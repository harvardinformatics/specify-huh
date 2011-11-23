package edu.harvard.huh.specify.tests;

import static org.junit.Assert.*;

import org.hibernate.Session;
import org.junit.Test;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.Specify;
import edu.ku.brc.specify.datamodel.busrules.TaxonBusRules;

/**
 * Tests the miscellaneous changed methods in the tree by parent upgrade. Tests
 * the recursive query replacements for the original S/HQL statements.
 * 
 * @author lchan
 * 
 */
public class BaseTest {

    protected Session getSession() {
        DBConnection dbc = DBConnection.getInstance();
        dbc.setConnectionStr("jdbc:mysql://127.0.0.1:3307/specify?characterEncoding=UTF-8&autoReconnect=true");
        dbc.setDatabaseName("specify");
        dbc.setDialect("org.hibernate.dialect.MySQLDialect");
        dbc.setDriver("com.mysql.jdbc.Driver");
        dbc.setServerName("127.0.0.1");
        dbc.setUsernamePassword("lchan", "1579ASDF");

        Specify.setUpSystemProperties();

        Session session = HibernateUtil.getNewSession();
        
        return session;
    }



    @Test
    public void testErtiSetup() {

    }

    @Test
    public void testGetNodeNumberCriteria() {

    }

    @Test
    public void testHibernateTreeDataServiceImplCreateNode() {

    }

    @Test
    public void testGetDescendantCount() {

    }

    @Test
    public void testTreeTableViewerCreateNode() {

    }

}
