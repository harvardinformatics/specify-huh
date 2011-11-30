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

import java.util.Properties;

import org.hibernate.Session;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.Specify;

public class BaseTest {
	protected Session session = null;
	
	protected Session getSession() {
		try {
			Properties props = new Properties();
			props.load(getClass().getResourceAsStream("testing.properties"));
		
			DBConnection dbConn = DBConnection.getInstance();
		
			dbConn.setDriver(props.getProperty("testing.db.driver"));
			dbConn.setDialect(props.getProperty("testing.db.dialect"));
			dbConn.setDatabaseName(props.getProperty("testing.db.name"));
			dbConn.setConnectionStr(props.getProperty("testing.db.connstr"));
			dbConn.setUsernamePassword(props.getProperty("testing.db.username"),
					props.getProperty("testing.db.password"));
		
			Specify.setUpSystemProperties(); // lchan: need this for a BaseTreeBusRulesTest
			
			session = HibernateUtil.getNewSession();
		} catch (Exception e) {
			e.printStackTrace();

		}
		return session;
	}
}
