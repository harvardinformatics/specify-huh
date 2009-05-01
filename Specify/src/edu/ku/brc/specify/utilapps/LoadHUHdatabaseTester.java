package edu.ku.brc.specify.utilapps;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistName;
import edu.harvard.huh.asa.Site;
import edu.harvard.huh.asa2specify.BotanistConverter;
import edu.harvard.huh.asa2specify.BotanistNameConverter;
import edu.harvard.huh.asa2specify.CsvToSqlMgr;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SiteConverter;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentVariant;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.utilapps.LoadHUHdatabase;
import edu.ku.brc.util.LatLonConverter.FORMAT;


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
    private static final byte srcLatLongUnit = (byte) FORMAT.DDDDDD.ordinal();
    

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

        try
        {
            test.loadBotanistSpecialties();
        } catch (LocalException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    void setUpConnection() {
        String dbName = "specifybeta";

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