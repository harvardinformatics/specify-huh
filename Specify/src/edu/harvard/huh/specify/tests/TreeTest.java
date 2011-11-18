package edu.harvard.huh.specify.tests;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.swing.UIManager;

import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.treeutils.HibernateTreeDataServiceImpl;
import edu.ku.brc.specify.treeutils.TreeDataService;


public class TreeTest {
	private Treeable taxon;
	private TreeDefIface currentDef;
	private TreeDataService treeService;
	private Treeable root;
	private final int TAXON_TREE_DEF_ID = 1;
	private Session session;

	@Before public void setUp() {
		try {

	        UIManager.setLookAndFeel(
	            UIManager.getCrossPlatformLookAndFeelClassName()); // Otherwise HibernateUtil will throw swing exception
			
			DBConnection dbConn = DBConnection.getInstance();

	        dbConn.setDriver("com.mysql.jdbc.Driver");
	        dbConn.setDialect("org.hibernate.dialect.MySQLDialect");
	        dbConn.setDatabaseName("specify");
	        dbConn.setConnectionStr("jdbc:mysql://localhost/specify?characterEncoding=UTF-8&autoReconnect=true");
	        dbConn.setUsernamePassword("root", "password");

			session = HibernateUtil.getSessionFactory().openSession();
			
		    currentDef = (TreeDefIface<Taxon, TaxonTreeDef, TaxonTreeDefItem>)session.load(new TaxonTreeDef().getClass(), TAXON_TREE_DEF_ID);
			treeService = new HibernateTreeDataServiceImpl<Taxon, TaxonTreeDef, TaxonTreeDefItem>();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();        
	        }
		}
		
		root = treeService.getRootNode(currentDef);
	}
	
	@Test public void testRootNode() {
		assertTrue(treeService.getRootNode(currentDef) != null);
	}
	@Test public void testRootNodeChildren() {
		assertTrue(treeService.getChildNodes(root) != null);
	}
	@Test public void testSize() {
		//System.out.println(size(root));
		System.out.println(treeService.getDescendantCount(root));
	}
	
	private int size(Treeable parent) {
		int i = 1;
		for (Treeable t: (Set<Treeable>)treeService.getChildNodes(parent)) {
			if (t != null) {
				i += size(t);
			}
		}
		return i;
	}
}
