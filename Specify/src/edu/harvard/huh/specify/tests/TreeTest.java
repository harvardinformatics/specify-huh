package edu.harvard.huh.specify.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;
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
	
	@Test public void testRootNode() 		
	{
		assertTrue(treeService.getRootNode(currentDef) != null);
	}
	@Test public void testRootNodeChildren() {
		assertTrue(treeService.getChildNodes(root) != null);
	}

	@Test public void checkTreeIntegrity() {	
		Queue<Treeable> nodes = new LinkedList<Treeable>();
		int count = 0;
		boolean[] visited = new boolean[1];
		boolean error = false;
		String errStr = "No Errors!";
		nodes.offer(root);
		
		while (!error && !nodes.isEmpty()) {
			Treeable parent = nodes.poll();
			for (Treeable node : (Set<Treeable>)treeService.getChildNodes(parent)) {
				
				// First, check the parent object reference and parent id
				if (node.getParent() != null) {
					if(node.getParent().getTreeId() != parent.getTreeId()) {
						errStr = node.getTreeId() + ": " + node.getFullName() + "(The child does not point back to the correct parent!)";
						error = true;
					}
				} else {
					errStr = node.getTreeId() + ": " + node.getFullName() + "(This node has no parent and isn't the root!)";
					error = true;
				}
				
				// Second, check for cycles by checking if this node has been visited more than once
				
				// TODO: implement this using a map
				int id = node.getTreeId();
				if (id > visited.length-1) {
					boolean[] newVisited = new boolean[id+1];
					for (int i = 0; i < visited.length; i++) {
						newVisited[i] = visited[i];
					}
					visited = newVisited;
				}
				if (visited[id]) {
					errStr = node.getTreeId() + ": " + node.getFullName() + "This node has been visited already!";
					error = true;
				}
				visited[id] = true;
				
				// Enqueue current node and increment the count
				nodes.offer(node);
				count++;
			}
		}
		assertFalse(error);
		System.out.println(errStr);
	}
}
