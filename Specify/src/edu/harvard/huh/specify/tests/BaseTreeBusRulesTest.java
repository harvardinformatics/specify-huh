/*
 * Created on Dec 8, 2011
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
 * @Author: Lawrence Chan  lchan@indigocube.net
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */

package edu.harvard.huh.specify.tests;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.busrules.TaxonBusRules;

/**
 * This class contains the test case for the methods in BaseTreeBusRules.
 * 
 * @author lchan, lowery
 * 
 */

@RunWith(value = Parameterized.class)
public class BaseTreeBusRulesTest extends BaseTest {
	private int nodeIdHasChildren;
	private int nodeIdNoChildren;
	private int nodeIdHasReferences;

	/**
	 * The constructor is used by the parameterized testing to obtain node
	 * ids for each test.
	 * 
	 * @param nodeIdHasChildren
	 * @param nodeIdNoChildren
	 * @param nodeIdHasReferences
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public BaseTreeBusRulesTest(int nodeIdHasChildren, int nodeIdNoChildren, int nodeIdHasReferences) throws ClassNotFoundException, SQLException, IOException {
		this.nodeIdHasChildren = nodeIdHasChildren;
		this.nodeIdNoChildren = nodeIdNoChildren;
		this.nodeIdHasReferences = nodeIdHasReferences;
	}

	/**
	 * This method gathers parameters for each of the variable fields from the testing
	 * properties file.
	 */
	@Parameters public static Collection<Object[]> data() throws IOException {
		Properties props = new Properties();
		props.load(TaxonTreeTest.class.getResourceAsStream("testing.properties"));

		TestParameter hasChildren = new TestParameter(props.getProperty("testing.busrules.haschildren"));
		TestParameter noChildren = new TestParameter(props.getProperty("testing.busrules.nochildren"));
		TestParameter hasReferences = new TestParameter(props.getProperty("testing.busrules.hasreferences"));

		Collection<Object[]> params = new LinkedList<Object[]>();

		while (hasChildren.hasNext() && noChildren.hasNext() && hasReferences.hasNext()) {
			params.add(new Object[] { Integer.parseInt(hasChildren.getParam()),
					Integer.parseInt(noChildren.getParam()),
					Integer.parseInt(hasReferences.getParam()) });
		}
		
		initializeDB();
		initializeContext();

		return params;
	}

	/**
	 * edu.ku.brc.specify.datamodel.busrules.BaseTreeBusRules: okToDeleteNode:
	 * Only allow deletion when: 1) A node has no children 2) There are no
	 * references to the node. I scrapped the original behavior.
	 */
	@Test
	public void testOkToDeleteNodeHasChildren() {
		Session session = null;
		try {
			session = getSession();
			Taxon root = (Taxon) session.get(Taxon.class, nodeIdHasChildren);

			TaxonBusRules rules = new TaxonBusRules();
			boolean deletable = rules.okToDeleteNode(root);

			Assert.assertFalse(deletable);
		} finally {
			session.close();
		}
	}

	/**
	 * A node should not be deletable if it has references. Taxon ID 50906 has
	 * determination references.
	 */
	@Test
	public void testOkToDeleteNodeHasReferences() {
		Session session = null;
		try {
			session = getSession();
			Taxon root = (Taxon) session.get(Taxon.class, nodeIdHasReferences);

			TaxonBusRules rules = new TaxonBusRules();
			boolean deletable = rules.okToDeleteNode(root);

			Assert.assertFalse(deletable);
		} finally {
			session.close();
		}
	}

	/**
	 * A node should be deletable only when it has no children.
	 */			 
	@Test
	public void testOkToDeleteNodeNoChildren() {
		Session session = null;
		try {
			session = getSession();
			Taxon root = (Taxon) session.get(Taxon.class, nodeIdNoChildren);

			TaxonBusRules rules = new TaxonBusRules();
			boolean deletable = rules.okToDeleteNode(root);

			Assert.assertTrue(deletable);
		} finally {
			session.close();
		}
	}
}