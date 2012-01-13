/*
 * Created on Nov 30, 2011
 *
 * Copyright © 2011 President and Fellows of Harvard College
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */

package edu.harvard.huh.specify.tests;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

import javax.swing.UIManager;

import org.hibernate.Session;
import org.junit.BeforeClass;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.Specify;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.SpecifyUser;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;

/**
 * This is the base for all the huh junit tests.
 * 
 * @author lowery
 * 
 */
public class BaseTest {
	protected static Session session = null;
	protected static Properties props;
	
    @BeforeClass
    public static void beforeClass() {
    	initializeDB();
    	initializeContext();
    }
    
	/**
	 * 
	 * This method initializes the database and application context before
	 * returning a hibernate session variable. 
	 * 
	 * @return Session
	 */
	protected static Session getSession() {
		try {
			UIManager.setLookAndFeel(
					UIManager.getCrossPlatformLookAndFeelClassName()); // Otherwise HibernateUtil will throw swing exception

			session = HibernateUtil.getNewSession();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return session;
	}
	
	protected static void initializeContext() {	
		System.setProperty(DataProviderFactory.factoryName, "edu.ku.brc.specify.dbsupport.HibernateDataProvider");
		System.setProperty(QueryAdjusterForDomain.factoryName, "edu.ku.brc.specify.dbsupport.SpecifyQueryAdjusterForDomain");
		System.setProperty(AppContextMgr.factoryName, "edu.ku.brc.specify.config.SpecifyAppContextMgr");
		AppContextMgr appCtxMgr = AppContextMgr.getInstance();
		
		String prop = props.getProperty("testing.specify.userid");

		SpecifyUser user = new SpecifyUser();
		int userId = Integer.parseInt(prop);
		user.setSpecifyUserId(userId);

		appCtxMgr.setContext(props.getProperty("testing.db.name"), props.getProperty("testing.db.name"), false, false);
		appCtxMgr.setHasContext(true);
		appCtxMgr.setClassObject(SpecifyUser.class, user);

		// The rest of this is needed for BaseTreeBusRulesTest
		Collection collection = new Collection();
		collection.setCollectionId(4);
		appCtxMgr.setClassObject(Collection.class, collection);

		TaxonTreeDef taxonTreeDef = new TaxonTreeDef();
		taxonTreeDef.setTaxonTreeDefId(1);
		appCtxMgr.setClassObject(TaxonTreeDef.class, taxonTreeDef);

		Specify.setUpSystemProperties();
	}
	
	protected static void initializeDB() {
		try {
			H2SpecifySchemaGenerator.generateFromProps();
			
			props = new Properties();
			props.load(new BaseTest().getClass().getResourceAsStream("testing.properties"));
			
			DBConnection dbConn = DBConnection.getInstance();

			String driver = props.getProperty("testing.db.driver");
			String connStr = props.getProperty("testing.db.connstr");
			String user = props.getProperty("testing.db.username");
			String pass = props.getProperty("testing.db.password");

			// If using in memory db, load the sql script specified in the properties file as the test data
			if (driver.equals("org.h2.Driver")) {
				Class.forName(driver);
				Connection conn = DriverManager.
				getConnection(connStr, user, pass);

				StringBuffer sb = new StringBuffer();

				InputStream in = new BaseTest().getClass().getResourceAsStream(props.getProperty("testing.db.sqldump"));
				Scanner sc = new Scanner(in);

				while (sc.hasNextLine()) {
					sb.append(sc.nextLine() + '\n');
				}
				sc.close();
				in.close();

				String[] statements = sb.toString().split(";\\n");
				Statement stmt = conn.createStatement();

				for (String sql : statements) {
					if(!sql.equals("")) {
						stmt.execute(sql);
						//System.out.println(sql);
					}
				}

				conn.close();
			}

			// Need to set up the db connection and get session for hibernate factory method
			dbConn.setDriver(driver);
			dbConn.setDialect(props.getProperty("testing.db.dialect"));
			dbConn.setDatabaseName(props.getProperty("testing.db.name"));
			dbConn.setConnectionStr(connStr);
			dbConn.setUsernamePassword(props.getProperty("testing.db.username"),
					props.getProperty("testing.db.password"));

			getSession();

			// Application context configuration
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
