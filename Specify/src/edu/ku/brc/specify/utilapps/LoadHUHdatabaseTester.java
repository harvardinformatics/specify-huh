package edu.ku.brc.specify.utilapps;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa2specify.loader.*;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.specify.datamodel.Discipline;


/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

public class LoadHUHdatabaseTester extends LoadHUHdatabase
{
    private static final Logger log = Logger.getLogger(LoadHUHdatabase.class);

    protected static Vector<DatabaseDriverInfo> driverList;
   
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        log.setLevel(Level.ALL);

        log.debug("debug");
        log.info("info");
        log.warn("warn");
        log.error("error");
 
        LoadHUHdatabaseTester test = new LoadHUHdatabaseTester();
        test.setUpConnection();

        // set up a database connection for direct sql TODO: manage statement, connection?
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DBConnection.getInstance().createConnection();
            statement  = connection.createStatement();
        }
        catch (SQLException e)
        {
            System.err.println("Couldn't create Statement: " + e.getMessage());
            return;
        }
        
        int records = 0;
        try
        {
            File dir = new File("/home/maureen/load");
            
            //TaxonLoader taxonLoader = new TaxonLoader(new File(dir, "taxon.csv"), statement);
            //records += taxonLoader.loadRecords();

            //System.out.println("Numbering Taxonomy Tree...");
            //taxonLoader.numberNodes();
            //log.info("Numbered taxon tree");

            //GeoUnitLoader geoUnitLoader = new GeoUnitLoader(new File(dir, "geo_unit.csv"), statement);
            //records = geoUnitLoader.loadRecords();
            //System.out.println("Numbering Geography Tree...");
            //geoUnitLoader.numberNodes();
            //log.info("Numbered geography tree");

            //LocalityLoader siteLoader = new LocalityLoader(new File(dir, "site.csv"), statement);
            //records += siteLoader.loadRecords();
            
            //OrganizationLoader organizationLoader = new OrganizationLoader(new File(dir, "organization_test.csv"), statement);
            //records += organizationLoader.loadRecords();
            
            //AffiliateLoader affiliateLoader = new AffiliateLoader(new File(dir, "affiliate.csv"), statement, new File(dir, "affiliate_botanist.csv"));
            //records += affiliateLoader.loadRecords();
            
            BotanistNameLoader botanistNameLoader = new BotanistNameLoader(new File(dir, "botanist_name.csv"), statement);
            records += botanistNameLoader.loadRecords();
            
            //PublAuthorLoader publAuthorLoader = new PublAuthorLoader(new File("demo_files/publ_author.csv"), statement);
            //records = publAuthorLoader.loadRecords();
            
            log.info("Loaded " + records + " records");
        }
        catch (LocalException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Processed " + records + " records");
    }
    
    void setUpConnection() {
        String dbName = "specify";

        Vector<DatabaseDriverInfo> driverList = DatabaseDriverInfo.getDriversList();
        DatabaseDriverInfo driverInfo = DatabaseDriverInfo.getInfoByName(driverList, "MySQL");

        String userName = "Specify";
        String password = "Specify";
        String hostname = "localhost";

        String connStr = driverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Create, 
                hostname, 
                dbName);

        if (connStr == null)
        {
            connStr = driverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Open, hostname,  dbName);
        }
        else {
            System.err.println("Couldn't create connection");
        }

        DBConnection dbConn = DBConnection.getInstance();

        dbConn.setDriver(driverInfo.getDriverClassName());
        dbConn.setDialect(driverInfo.getDialectClassName());
        dbConn.setDatabaseName(dbName);
        dbConn.setConnectionStr(connStr);
        dbConn.setUsernamePassword(userName, password);

        Connection connection = dbConn.createConnection();
        if (connection != null)
        {
            try
            {
                connection.close();

            } catch (SQLException ex)
            {
                // do nothing
            }
        }
        else {
            System.err.println("Couldn't open connection");
        }
    }

    Discipline getBotanyDiscipline() {
        Discipline discipline = new Discipline();
        discipline.setDisciplineId(3);
        
        return discipline;
    }
    
}