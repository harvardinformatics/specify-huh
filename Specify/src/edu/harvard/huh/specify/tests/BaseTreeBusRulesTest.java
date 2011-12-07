package edu.harvard.huh.specify.tests;

import java.io.IOException;
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
 * This class depends on the HUH database.
 * 
 * @author lchan
 * 
 */

@RunWith(value = Parameterized.class)
public class BaseTreeBusRulesTest extends BaseTest {
	private int nodeIdHasChildren;
	private int nodeIdNoChildren;
	private int nodeIdHasReferences;
	
	public BaseTreeBusRulesTest(int nodeIdHasChildren, int nodeIdNoChildren, int nodeIdHasReferences) {
		this.nodeIdHasChildren = nodeIdHasChildren;
		this.nodeIdNoChildren = nodeIdNoChildren;
		this.nodeIdHasReferences = nodeIdHasReferences;
	}

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
     * I can't run this test, as the Specify code breaks somewhere and there are
     * no comments in the code to guide me to write the test in a way that will
     * work.clazz
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