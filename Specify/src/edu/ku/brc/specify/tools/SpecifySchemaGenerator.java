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
package edu.ku.brc.specify.tools;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.ui.UIRegistry;


/**
 * This class provides the ability to create a DB schema from a Hibernate OR mapping.
 *
 * @code_status Beta
 * @author jstewart
 */
public class SpecifySchemaGenerator
{
    protected static final Logger log = Logger.getLogger(SpecifySchemaGenerator.class);
            
    /**
     * Drops (or deletes) the database and creates a new one by generating the schema.
     * @param dbdriverInfo the driver info
     * @param hostname the host name ('localhost')
     * @param databaseName the database name
     * @param userName the username
     * @param password the password
     * @throws SQLException
     */
    public static void generateSchema(final DatabaseDriverInfo dbdriverInfo, 
                                      final String             hostname,
                                      final String             databaseName,
                                      final String             userName,
                                      final String             password) throws SQLException
    {
        log.debug("generateSchema hostname:" + hostname);
        log.debug("generateSchema databaseName:" + databaseName);
        log.debug("generateSchema userName:" + userName);
        log.debug("generateSchema password:" + password);
        boolean isDerby     = dbdriverInfo.getName().equals("Derby");
        //boolean isSQLServer = dbdriverInfo.getName().equals("SQLServer");
        
        // Get the Create OR the Open String
        // Note: Derby local databases have a different connection string for creating verses opening.
        // So we need to get the Create string if it has one (Derby) or the open string if it doesn't
        // Also notice that Derby wants a database name for the initial connection and the others do not
        String connectionStr = dbdriverInfo.getConnectionCreateOpenStr(hostname, isDerby ? databaseName : "", userName, password, dbdriverInfo.getName());
        log.debug("generateSchema connectionStr: " + connectionStr);
        
        // For derby the Connection String includes the "create" indicator
        // so we must first drop (delete) the Derby Database
        if (isDerby)
        {
            log.debug("database is derby database, running special drop method");
            dropDerbyDatabase(databaseName);
        }

        log.debug("Creating database connection to: " + connectionStr);
        // Now connect to other databases and "create" the Derby database
        DBConnection dbConn = DBConnection.createInstance(dbdriverInfo.getDriverClassName(), dbdriverInfo.getDialectClassName(), databaseName, connectionStr, userName, password);

        // Once connected drop the non-Derby databases
        if (!isDerby)
        {
            //log.debug("calling dropAndCreateDB(" + dbConn.toString() + ", " + databaseName +")");
            dropAndCreateDB(dbConn, databaseName);
            
            connectionStr = dbdriverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Open, hostname, databaseName, userName, password, dbdriverInfo.getName());
        }
        
        log.debug("Preparting to doGenSchema: " + connectionStr);
        
        // Generate the schema
        doGenSchema(dbdriverInfo,
                    connectionStr,
                    userName,
                    password);
        
        String       connStr           = dbdriverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Open, hostname, databaseName);
        DBConnection dbConnForDatabase = DBConnection.createInstance(dbdriverInfo.getDriverClassName(), dbdriverInfo.getDialectClassName(), databaseName, connStr, userName, password);
        fixFloatFields(dbConnForDatabase);
        
        // Now switch the Connection String back to Open from Create.
        if (isDerby)
        {
            dbConn.close(); // Derby Only
            
            connectionStr = dbdriverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Open, hostname, databaseName, userName, password, dbdriverInfo.getName());
            
            dbConn = DBConnection.createInstance(dbdriverInfo.getDriverClassName(), dbdriverInfo.getDialectClassName(), databaseName, connectionStr, userName, password);
        }

    }

    /**
     * Drops (deletes the directory) for a local Derby Database.
     * @param databaseName the database name (which is really the directory name).
     */
    protected static void dropDerbyDatabase(final String databaseName)
    {
        String derbyDatabasePath = UIRegistry.getJavaDBPath();
        if (StringUtils.isNotEmpty(derbyDatabasePath))
        {
            if (new File(derbyDatabasePath).exists())
            {
                File derbyDBDir = new File(derbyDatabasePath + File.separator + databaseName);
                if (derbyDBDir.exists())
                {
                    try
                    {
                        FileUtils.deleteDirectory(derbyDBDir);
                        return;
                        
                    } catch (IOException ex)
                    {
                        log.error(ex);
                    }
                    throw new RuntimeException("Error deleting directory["+derbyDBDir.getAbsolutePath()+"]");
                }
            }
        } else
        {
            throw new RuntimeException("JavaDB Path System property was null or empty and can't be!");
        }
    }
   
//    /**
//     * Drops (deletes the directory) for a local Derby Database.
//     * @param databaseName the database name (which is really the directory name).
//     */
//    protected static void dropSQLServerDatabase(final String databaseName)
//    {
//
//    }    
    /**
     * Drop the database via it's connection.
     * @param dbConnection the connection 
     * @param dbName the database name
     * @throws SQLException if any DB errors occur
     */
    protected static void dropAndCreateDB(final DBConnection dbConnection, final String dbName) throws SQLException
    {
        dropDB(dbConnection,dbName );
        createDB(dbConnection,dbName );
    }
    /**
     * Drop the database via it's connection.
     * @param dbConnection the connection 
     * @param dbName the database name
     * @throws SQLException if any DB errors occur
     */
    protected static void dropDB(final DBConnection dbConnection, final String dbName) throws SQLException
    {
        Connection connection = dbConnection.createConnection();
        if (connection != null)
        {
            Statement  stmt       = connection.createStatement();
            try
            {
                log.info("Dropping database "+dbName);
                //String myStatemt
                stmt.execute("drop database "+ dbName);
                
                log.info("Dropped database "+dbName);
                
                stmt.close();
                
                
            } catch (SQLException ex)
            {
                log.error("SQLException could not drop database ["+dbName+"]:" + "\n" + ex.toString());
                ex.toString();
            }        
            stmt.close();
            connection.close();
        } else
        {
            log.error(dbConnection.getErrorMsg());
        }
    } 
    
    /**
     * @param dbConnection
     * @throws SQLException
     */
    protected static void fixFloatFields(final DBConnection dbConnection) throws SQLException
    {
        Connection connection = dbConnection.createConnection();
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
                            String sql = "ALTER TABLE "+tableInfo.getName()+" MODIFY "+fldInfo.getColumn()+" FLOAT(20,10)";
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
            log.error(dbConnection.getErrorMsg());
        }
    } 

    
    /**
     * Drop the database via it's connection.
     * @param dbConnection the connection 
     * @param dbName the database name
     * @throws SQLException if any DB errors occur
     */
    protected static void createDB(final DBConnection dbConnection, final String dbName) throws SQLException
    {
        Connection connection = dbConnection.createConnection();
        if (connection != null)
        {
            Statement  stmt       = connection.createStatement();
            log.info("Creating database "+dbName);        
            stmt.execute("create database "+dbName+" default charset utf8;");        
            log.info("Created database "+dbName);        
            stmt.close();
            connection.close();            
        } else
        {
            log.error(dbConnection.getErrorMsg());
        }

        //System.exit(0);
    }
    /**
     * Creates a properties object with the necessary args for generating the schema.
     * @param driverInfo basic info about the DB driver to use
     * @param connectionStr the connection string for creating or opening a database
     * @param user the username
     * @param passwd the password (plaintext)
     * @return the generated Hibernate properties
     */
    protected static Properties getHibernateProperties(final DatabaseDriverInfo driverInfo,
                                                       final String connectionStr, // might be a create or an open connection string
                                                       final String user,
                                                       final String passwd)
    {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", driverInfo.getDriverClassName());
        props.setProperty("hibernate.dialect",                 driverInfo.getDialectClassName());
        props.setProperty("hibernate.connection.url",          connectionStr);
        props.setProperty("hibernate.connection.username",     user);
        props.setProperty("hibernate.connection.password",     passwd);
        props.setProperty("hibernate.max_fetch_depth",         "3");
        props.setProperty("hibernate.connection.pool_size",    "5");
        props.setProperty("hibernate.format_sql",              "true");
        log.debug("Hibernate Propereties: " + props.toString());
        return props;
    }

    /**
     * Creates the Schema.
     * @param driverInfo the driver info to use
     * @param connectionStr the connection string for creating or opening a database
     * @param hostname the hostname (localhost)
     * @param databaseName the database name
     * @param user the username
     * @param passwd the password (clear text)
     */
    protected static void doGenSchema(final DatabaseDriverInfo driverInfo,
                                      final String connectionStr, // might be a create or an open connection string
                                      final String user,
                                      final String passwd)
    {
        // setup the Hibernate configuartion
        Configuration hibCfg = new AnnotationConfiguration();
        hibCfg.setProperties(getHibernateProperties(driverInfo, connectionStr, user, passwd));
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
     * Creates a {@link SpecifySchemaGenerator} and generates a test DB in a localhost MySQL server.
     * 
     * @param args ignored
     * @throws SQLException if any DB error occurs
     */
    public static void main(String[] args) throws SQLException
    {
        DatabaseDriverInfo dbdriverInfo = DatabaseDriverInfo.getDriver("SQLServer");
        SpecifySchemaGenerator.generateSchema(dbdriverInfo, "localhost", "Fish_sp6", "sa", "Re4a22jiu");
   
        //DatabaseDriverInfo dbdriverInfo = DatabaseDriverInfo.getDriver("MySQL");
       // SpecifySchemaGenerator.generateSchema(dbdriverInfo, "localhost", "testdb", "rods", "rods");
    }
}
