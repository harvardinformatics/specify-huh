package edu.harvard.huh.specify.tests;

import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;
import edu.ku.brc.specify.treeutils.HibernateTreeDataServiceImpl;
import edu.ku.brc.specify.treeutils.TreeDataService;

public class GenericTreeTest extends BaseTest {

    protected TreeDataService treeService;
    
    private Taxon newTaxon(Session session) {
        TaxonTreeDef treeDef = (TaxonTreeDef) session.load(TaxonTreeDef.class, 1);
        TaxonTreeDefItem treeDefItem = (TaxonTreeDefItem) session.load(TaxonTreeDefItem.class, 1);
        
        Taxon taxon = new Taxon("to-delete");
        taxon.setDefinition(treeDef);
        taxon.setDefinitionItem(treeDefItem);
        
        return taxon;
    }

    @Before
    public void beforeClass() {
        getSession();
        // this.treeDefClass = treeDefClass;
        // this.treeableClass = treeableClass;
        // currentDef = (TreeDefIface) session.load(treeDefClass,
        // DEFAULT_TREE_DEF_ID);
        // AppContextMgr.getInstance().setClassObject(treeDefClass, currentDef);
        treeService = new HibernateTreeDataServiceImpl();
        session.close();

        // root = treeService.getRootNode(currentDef);
    }

    /**
     * Tests the behavior of the TreeService method findByName.
     */
    @Test
    public void testFindByName() {
        TaxonTreeDef treeDef = new TaxonTreeDef();
        int numResults = treeService.findByName(treeDef, "1", true).size();
        Assert.assertTrue(numResults > 0);
    }

    /**
     * Tests the behavior of the TreeService method getNodeById.
     */
    @Test
    public void testGetNodeById() {
        assertTrue(treeService.getNodeById(Taxon.class, 167876) != null);
    }

    /**
     * Tests the behavior of TreeService method deleteTreeNode by deleting the
     * node then running a query to confirm that it has been deleted.
     */
    @Test
    public void testDeleteTreeNode() {
        Session session1 = getSession();
        Transaction tx = session1.beginTransaction();
        
        Taxon taxon = newTaxon(session);

        session1.persist(taxon);
        session1.flush();
        tx.commit();
        session1.close();
        
        // Record tree state

        treeService.deleteTreeNode(taxon);
        
        // Update and confirm tree state
    }

    /**
     * Tests the behavior of TreeService method MoveTreeNode by moving a tree
     * node from one parent to another then confirming that the moved node's
     * parent id is the same as the parent id that was specified by the
     * parameter.
     */
    @Test
    public void testMoveTreeNode() {
        // Treeable from = treeService.getNodeById(treeableClass, moveFrom);
        // Treeable to = treeService.getNodeById(treeableClass, moveTo);
        //
        // treeService.moveTreeNode(from, to);
        //
        // Treeable child = treeService.getNodeById(treeableClass, moveFrom);
        // assertTrue(child.getParent().getTreeId() == moveTo);
    }

    /**
     * Tests the behavior of the TreeService method getNodeById.
     */
    @Test
    public void testRootNode() {

        assertTrue(treeService.getRootNode(new TaxonTreeDef()) != null);
    }

    /**
     * Tests the behavior of the TreeService method getChildNodes with the
     * parameter values specified for lookup as the parent.
     */
    @Test
    public void testGetChildNodes() {
        // Treeable parent = treeService.getNodeById(treeableClass, lookupId);
        // assertTrue(treeService.getChildNodes(parent) != null);
    }

}
