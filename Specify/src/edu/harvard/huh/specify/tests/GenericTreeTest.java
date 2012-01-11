package edu.harvard.huh.specify.tests;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.treeutils.HibernateTreeDataServiceImpl;
import edu.ku.brc.specify.treeutils.TreeDataService;

public class GenericTreeTest extends BaseTest {

    protected TreeDataService treeService;

    /**
     * Class that contains a tree ID, parent ID, and comparator that returns
     * true when another class has the same tree and parent IDs. I'm using this
     * class because the Treeable subclass comparators don't compare the ID and
     * parent IDs.
     * 
     * @author lchan
     * 
     */
    private class TreeTestNode implements Comparable {
        private int id;

        private int parentId;

        public TreeTestNode(Treeable treeable) {
            this.id = treeable.getTreeId();
            if (treeable.getParent() == null) {
                this.parentId = -1;
            } else {
                this.parentId = treeable.getParent().getTreeId();
            }
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getParentId() {
            return parentId;
        }

        public void setParentId(int parentId) {
            this.parentId = parentId;
        }

        @Override
        public int compareTo(Object arg0) {
            if (!(arg0 instanceof TreeTestNode)) {
                return -1;
            }

            TreeTestNode other = (TreeTestNode) arg0;

            if (this.id == other.getId()
                    && this.parentId == other.getParentId()) {
                return 0;
            } else {
                return -1;
            }
        }
        
        @Override
        public String toString() {
            return "[" + id + "," + parentId + "]";
        }

    }

    private Taxon newTaxon(Session session) {
        TaxonTreeDef treeDef = (TaxonTreeDef) session.load(TaxonTreeDef.class,
                1);
        TaxonTreeDefItem treeDefItem = (TaxonTreeDefItem) session.load(
                TaxonTreeDefItem.class, 1);

        Taxon taxon = new Taxon("to-delete");
        taxon.setDefinition(treeDef);
        taxon.setDefinitionItem(treeDefItem);

        return taxon;
    }

    /**
     * Returns a treeable table's rows as a set of TreeTestNodes.
     * 
     * @param session
     * @return
     */
    private Set<TreeTestNode> getTreeNodeRepresentation() {
        HashSet<TreeTestNode> nodes = new HashSet<TreeTestNode>();
        
        Session sess = getSession();
        List<Treeable> treeables = session.createQuery("select e from Taxon e")
                .list();
        sess.close();
        
        for (Treeable treeable : treeables) {
            TreeTestNode node = new TreeTestNode(treeable);
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * @param session
     * @return
     */
    private int getRootNodeId(Session session) {
        int rootId = (Integer) session.createQuery(
                "select e.id from Taxon e where parent.id is null")
                .uniqueResult();
        return rootId;
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
//    @Test
    public void testFindByName() {
        TaxonTreeDef treeDef = new TaxonTreeDef();
        int numResults = treeService.findByName(treeDef, "1", true).size();
        Assert.assertTrue(numResults > 0);
    }

    /**
     * Tests the behavior of the TreeService method getNodeById.
     */
//    @Test
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

        Taxon root = (Taxon) session.get(Taxon.class, getRootNodeId(session1));
        Taxon taxon = newTaxon(session);
        taxon.setParent(root);

        session1.persist(taxon);
        session1.flush();
        tx.commit();
        session1.close();

        Set<TreeTestNode> before = getTreeNodeRepresentation();

        treeService.deleteTreeNode(taxon);

        Set<TreeTestNode> after = getTreeNodeRepresentation();

        Assert.assertTrue(before.equals(after));
    }

    /**
     * Tests the behavior of TreeService method MoveTreeNode by moving a tree
     * node from one parent to another then confirming that the moved node's
     * parent id is the same as the parent id that was specified by the
     * parameter.
     */
//    @Test
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
//    @Test
    public void testRootNode() {

        assertTrue(treeService.getRootNode(new TaxonTreeDef()) != null);
    }

    /**
     * Tests the behavior of the TreeService method getChildNodes with the
     * parameter values specified for lookup as the parent.
     */
//    @Test
    public void testGetChildNodes() {
        // Treeable parent = treeService.getNodeById(treeableClass, lookupId);
        // assertTrue(treeService.getChildNodes(parent) != null);
    }

}
