/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.harvard.huh.specify.tests;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.dbsupport.DBConnection;


/**
 * This class provides the ability to create a DB schema from a Hibernate OR mapping.
 *
 * @code_status Complete
 * @author rods
 * @author jstewart
 */
public class H2SpecifySchemaGenerator
{
    protected static final Logger log = Logger.getLogger(H2SpecifySchemaGenerator.class);
    
    /**
     * Drops (or deletes) the database and creates a new one by generating the schema.
     * @param dbdriverInfo the driver info
     * @param hostname the host name ('localhost')
     * @param databaseName the database name
     * @param userName the username
     * @param password the password
     * @param doUpdate tells it to update the schema instead of creating it
     * @throws SQLException
     */
    public static boolean generateSchema(String driver, String dialect, String dbName, String user, String password, String connStr) throws SQLException
    {
        log.debug("generateSchema connectionStr: " + connStr);
        
        log.debug("Creating database connection to: " + connStr);
        // Now connect to other databases and "create" the Derby database
        Connection dbConn = null;
        try
        {
            dbConn = createConnection(driver, dialect, dbName, connStr, user, password);
            if (dbConn != null)
            {
                log.debug("calling dropAndCreateDB(" + dbConn.toString() + ", " + dbName +")");

                log.debug("Preparing to doGenSchema: " + connStr);
                
                // Generate the schema
                doGenSchema(driver,
                			dialect,
                            connStr,
                            user,
                            password);
                
                Connection dbConnForDatabase = createConnection(driver, dialect, dbName, connStr, user, password);

                fixFloatFields(dbConnForDatabase);
                dbConnForDatabase.close();
                
            } else
            {
                return false;
            }
            return true;
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        }
        finally
        {
            if (dbConn != null)
            {
                dbConn.close();
            }
        }
    }
    
    private static Connection createConnection(String driver, String dialect,
			String dbName, String connStr, String user, String password) throws SQLException {
		return DriverManager.getConnection(connStr, user, password);
	}

	/**
     * @param dbConnection
     * @throws SQLException
     */
    protected static void fixFloatFields(final Connection connection) throws SQLException
    {
        if (connection != null)
        {
            Statement stmt = connection.createStatement();
            try
            {
                for (DBTableInfo tableInfo : DBTableIdMgr.getInstance().getTables())
                {
                    for (DBFieldInfo fldInfo : tableInfo.getFields())
                    {
                        if (fldInfo.getDataClass() == Float.class)
                        {
                            String sql;
                            sql = "ALTER TABLE "+tableInfo.getName()+" ALTER "+fldInfo.getColumn()+" FLOAT";
                          
                            stmt.executeUpdate(sql);
                            stmt.clearBatch();                           
                            log.info(sql);
                        }
                    }
                }
                
            } catch (SQLException ex)
            {
                //log.error("SQLException could not drop database ["+dbName+"]:" + "\n" + ex.toString());
                ex.printStackTrace();
            }        
            stmt.close();
            connection.close();
        } else
        {
            log.error("Error fixing float fields!");
        }
    } 
    
    /**
     * Creates a properties object with the necessary args for generating the schema.
     * @param driverInfo basic info about the DB driver to use
     * @param connectionStr the connection string for creating or opening a database
     * @param user the username
     * @param passwd the password (plaintext)
     * @param doUpdate tells it to update the schema instead of creating it
     * @return the generated Hibernate properties
     */
    protected static Properties getHibernateProperties(final String driver,
    												   final String dialect,
                                                       final String connectionStr, // might be a create or an open connection string
                                                       final String user,
                                                       final String passwd)
    {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", driver);
        props.setProperty("hibernate.dialect",                 dialect);
        props.setProperty("hibernate.connection.url",          connectionStr);
        props.setProperty("hibernate.connection.username",     user);
        props.setProperty("hibernate.connection.password",     passwd);
        props.setProperty("hibernate.max_fetch_depth",         "3");
        props.setProperty("hibernate.connection.pool_size",    "5");
        props.setProperty("hibernate.format_sql",              "true");
        
        log.debug("Hibernate Propereties: " + props.toString());
        return props;
    }

    /**generateSchema
     * Creates the Schema.
     * @param driverInfo the driver info to use
     * @param connectionStr the connection string for creating or opening a database
     * @param hostname the hostname (localhost)
     * @param databaseName the database name
     * @param user the username
     * @param passwd the password (clear text)
     * @param doUpdate tells it to update the schema instead of creating it
     */
    protected static void doGenSchema(final String driver,
    							      final String dialect,
                                      final String connectionStr, // might be a create or an open connection string
                                      final String user,
                                      final String passwd)
    {
        // setup the Hibernate configuration
        Configuration hibCfg = new AnnotationConfiguration();
        hibCfg.setProperties(getHibernateProperties(driver, dialect, connectionStr, user, passwd));
        hibCfg.configure();

        SchemaExport schemaExporter = new SchemaExport(hibCfg);
        schemaExporter.setDelimiter(";");

        log.info("Generating schema");
        //System.exit(0);
        boolean printToScreen = false;
        boolean exportToDb    = true;
        boolean justDrop      = false;
        boolean justCreate    = true;
        log.info("Creating the DB schema");
        schemaExporter.execute(printToScreen, exportToDb, justDrop, justCreate);

        log.info("DB schema creation completed");

        // log the exceptions that occurred
        List<?> exceptions = schemaExporter.getExceptions();
        for (Object o: exceptions)
        {
        	Exception e = (Exception)o;
        	log.error(e.getMessage());
        }
    }
    
    /**
     * Creates a {@link H2SpecifySchemaGenerator} and generates a test DB in a localhost MySQL server.
     * 
     * @param args ignored
     * @throws SQLException if any DB error occurs
     * @throws IOException 
     */
    public static void main(String[] args)
    {
    	try {
			generateFromProps();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public static void generateFromProps() throws IOException, SQLException {
		Properties props = new Properties();
		props.load(new BaseTest().getClass().getResourceAsStream("testing.properties"));
		String connStr = props.getProperty("testing.db.connstr");
		String dbName = props.getProperty("testing.db.name");
		String user = props.getProperty("testing.db.username");
		String pass = props.getProperty("testing.db.password");
		
        H2SpecifySchemaGenerator.generateSchema("org.h2.Driver", "org.hibernate.dialect.H2Dialect", dbName, user, pass, connStr);
	}
    
    
}
