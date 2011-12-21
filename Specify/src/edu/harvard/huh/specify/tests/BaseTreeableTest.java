/*
 * Created on Nov 21, 2011
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

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.treeutils.HibernateTreeDataServiceImpl;
import edu.ku.brc.specify.treeutils.TreeDataService;

/**
 * This is the base class to be used for parameterized testing of each of the
 * Treeable interface's implementing classes.
 * 
 * @author lowery, lchan
 * 
 */
@SuppressWarnings("unchecked")
public class BaseTreeableTest extends BaseTest {

    protected Class treeDefClass;
    protected Class treeableClass;
    protected TreeDefIface currentDef;
    protected TreeDataService treeService;
    protected Treeable root;

    protected static List<String[]> propsList = new LinkedList<String[]>();

    protected int lookupId;
    protected String lookupName;
    protected int moveFrom;
    protected int moveTo;
    protected int deleteId;

    /**
     * The constructor accepts arguments from the test parameters (initialized
     * in getTreeableParams). Each test case will have a unique set of
     * parameters and will run all tests once using these values.
     * 
     * @param lookupId
     * @param lookupName
     * @param moveFrom
     * @param moveTo
     * @param deleteId
     */
    public BaseTreeableTest(int lookupId, String lookupName, int moveFrom,
            int moveTo, int deleteId) {
        this.lookupId = lookupId;
        this.lookupName = lookupName;
        this.moveFrom = moveFrom;
        this.moveTo = moveTo;
        this.deleteId = deleteId;
    }

    /**
     * Class that contains a tree ID, parent ID, and comparator that returns
     * true when another class has the same tree and parent IDs. I'm using this
     * class because the Treeable subclass comparators don't compare the ID and
     * parent IDs.
     * 
     * @author lchan
     * 
     */
    private class TreeTestNode {
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
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + id;
            result = prime * result + parentId;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TreeTestNode other = (TreeTestNode) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (id != other.id)
                return false;
            if (parentId != other.parentId)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[" + id + "," + parentId + "]";
        }

        private BaseTreeableTest getOuterType() {
            return BaseTreeableTest.this;
        }

    }

    /**
     * Returns a treeable table's rows as a set of TreeTestNodes.
     * 
     * @param session
     * @return
     */
    private HashSet<TreeTestNode> getTreeNodeRepresentation(Class type) {
        Session sess = getSession();
        List<Treeable> treeables = session.createQuery(
                "select e from " + type.getSimpleName() + " e").list();
        sess.close();

        HashSet<TreeTestNode> nodes = new HashSet<TreeTestNode>();
        for (Treeable treeable : treeables) {
            TreeTestNode node = new TreeTestNode(treeable);
            nodes.add(node);
        }

        return nodes;
    }

    private void moveTreeNode(Set<TreeTestNode> nodes, int nodeId, int parentId) {
        for (TreeTestNode node : nodes) {
            if (node.getId() == nodeId) {
                node.setParentId(parentId);
                return;
            }
        }
    }

    /**
     * This method must be called by it's subclass. The subclass deals with one
     * specific implementing class of the Treeable interface. The corresponding
     * tree definition class and tree object class must be initialized (as
     * arguments to this method) before these tests can be run.
     * 
     * @param treeDefClass
     * @param treeableClass
     */
    public void initialize(Class treeDefClass, Class treeableClass,
            int treeDefId) {
        getSession();
        this.treeDefClass = treeDefClass;
        this.treeableClass = treeableClass;
        currentDef = (TreeDefIface) session.load(treeDefClass, treeDefId);
        AppContextMgr.getInstance().setClassObject(treeDefClass, currentDef);
        treeService = new HibernateTreeDataServiceImpl();
        session.close();

        root = treeService.getRootNode(currentDef);
    }

    /**
     * A subclass method annotated with "@Parameters" must call this to obtain
     * the Collection<Object[]> it is required to return for parameterized
     * testing to work. This method obtains the parameter lists from the
     * properties file, parses them, and composes a set of properties in an
     * Object[] to be applied during one run of testing.
     * 
     * @param lookupProp
     * @param moveProp
     * @param deleteProp
     * @return params
     */
    protected static Collection<Object[]> getTreeableParams(String lookupProp,
            String moveProp, String deleteProp) {
        TestParameter lookupParams = new TestParameter(lookupProp);
        TestParameter moveParams = new TestParameter(moveProp);
        TestParameter deleteParams = new TestParameter(deleteProp);

        Collection<Object[]> params = new LinkedList<Object[]>();

        while (lookupParams.hasNext() && moveParams.hasNext()
                && deleteParams.hasNext()) {
            params.add(new Object[] {
                    Integer.parseInt(lookupParams.getParam()),
                    lookupParams.getParam(),
                    Integer.parseInt(moveParams.getParam()),
                    Integer.parseInt(moveParams.getParam()),
                    Integer.parseInt(deleteParams.getParam()) });
        }

        return params;
    }

    /**
     * Tests the behavior of the TreeService method findByName.
     */
    @Test
    public void testFindByName() {
        assertTrue(treeService.findByName(currentDef, lookupName, true).size() > 0);
    }

    /**
     * Tests the behavior of the TreeService method getNodeById.
     */
    @Test
    public void testGetNodeById() {
        assertTrue(treeService.getNodeById(treeableClass, lookupId) != null);
    }

    /**
     * Tests the behavior of TreeService method deleteTreeNode by deleting the
     * node then running a query to confirm that it has been deleted.
     */
    @Test
    public void testDeleteTreeNode() {
        Treeable node = treeService.getNodeById(treeableClass, deleteId);
        treeService.deleteTreeNode(node);

        Session session = getSession();
        Boolean deleted = (Integer) session.createQuery(
                "select count(e) from " + treeableClass.getName()
                        + " e where e.id = " + deleteId).uniqueResult() == 0;
        assertTrue(deleted);
        session.close();
    }

    /**
     * Tests the behavior of TreeService method MoveTreeNode by moving a tree
     * node from one parent to another then confirming that the moved node's
     * parent id is the same as the parent id that was specified by the
     * parameter. Also checks the database to confirm that the parent IDs are
     * correct by comparing the table pre and post move.
     */
    @Test
    public void testMoveTreeNode() {
        Treeable from = treeService.getNodeById(treeableClass, moveFrom);
        Treeable to = treeService.getNodeById(treeableClass, moveTo);

        HashSet<TreeTestNode> before = getTreeNodeRepresentation(treeableClass);
        moveTreeNode(before, moveFrom, moveTo);

        treeService.moveTreeNode(from, to);

        HashSet<TreeTestNode> after = getTreeNodeRepresentation(treeableClass);

        // I'm using hashCode because equals doesn't work for some strange
        // reason.
        Assert.assertTrue(before.hashCode() == after.hashCode());

        Treeable child = treeService.getNodeById(treeableClass, moveFrom);
        assertTrue(child.getParent().getTreeId() == moveTo);
    }

    /**
     * Tests the behavior of the TreeService method getNodeById.
     */
    @Test
    public void testRootNode() {
        assertTrue(treeService.getRootNode(currentDef) != null);
    }

    /**
     * Tests the behavior of the TreeService method getChildNodes with the
     * parameter values specified for lookup as the parent.
     */
    @Test
    public void testRootNodeChildren() {
        Treeable parent = treeService.getNodeById(treeableClass, lookupId);
        assertTrue(treeService.getChildNodes(parent) != null);
    }
}
