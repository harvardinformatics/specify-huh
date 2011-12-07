package edu.harvard.huh.specify.tests;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.busrules.TaxonBusRules;

/**
 * This class depends on the HUH database.
 * 
 * @author lchan
 * 
 */
public class BaseTreeBusRulesTest extends BaseTest {
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
            Taxon root = (Taxon) session.get(Taxon.class, 1);

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
            Taxon root = (Taxon) session.get(Taxon.class, 50906);

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
            Taxon root = (Taxon) session.get(Taxon.class, 108660);

            TaxonBusRules rules = new TaxonBusRules();
            boolean deletable = rules.okToDeleteNode(root);

            Assert.assertTrue(deletable);
        } finally {
            session.close();
        }
    }
}
