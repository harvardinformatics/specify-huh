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
 * @Author: Lawrence Chan  lchan@indigocube.net
 */
package edu.harvard.huh.specify.tests;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.hibernate.HibernateException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.StorageTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.treeutils.HibernateTreeDataServiceImpl;
import edu.ku.brc.specify.treeutils.TreeDataService;

/**
 * Tests the integrity of a Treeable tree. May take a long time to run,
 * depending on the size of the tree.
 * 
 * @author lowery, lchan
 * 
 */
@SuppressWarnings("unchecked")
@RunWith(value = Parameterized.class)
public class TreeIntegrityTest extends BaseTest {

    private Set<Integer> visited = new HashSet<Integer>();

    @SuppressWarnings("rawtypes")
    private TreeDataService treeService;

    private String treeDefClass;

    @SuppressWarnings("rawtypes")
    private Treeable root;

    @SuppressWarnings("rawtypes")
    private TreeDefIface currentDef;

    private String treeableClass;
    
    private Integer size;

    public TreeIntegrityTest(String treeDefClass, String treeableClass) {
        this.treeDefClass = treeDefClass;
        this.treeableClass = treeableClass;
    }

    /**
     * This method initializes the parameters to be used by each run of all the
     * tests in the testcase. In this case, a tree definition and corresponding
     * treeable object.
     * 
     * @return parameters
     */
    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                { TaxonTreeDef.class.getName(), "Taxon" },
                { GeographyTreeDef.class.getName(), "Geography" },
                { StorageTreeDef.class.getName(), "Storage" } };
        return Arrays.asList(data);
    }

    /**
     * Takes care of setting up the tree definition and the TreeService before
     * tests are run.
     * 
     * @throws HibernateException
     * @throws ClassNotFoundException
     */
    @Before
    public void setUp() throws HibernateException, ClassNotFoundException {
        session = getSession();
        currentDef = (TreeDefIface)session.load(Class.forName(treeDefClass), 1);
        session.close();

        treeService = new HibernateTreeDataServiceImpl();
        root = treeService.getRootNode(currentDef);
        
        session = getSession();
    }

    @After
    public void after() {
        session.close();
    }

    /**
     * Tests that there are no cycles in a tree and that the rank IDs of
     * children are larger than those of their parents. Executes HQL statements
     * directly and does not use the HibernateTreeServices methods.
     * 
     * Tests tree traversal recursively without calls to tree service methods.
     * Specifically, the rankid is checked and count of nodes traversed is
     * compared against the count of rows in the database.
     */
    @Test
    public void testVisitationAndRankIds() {
        // try {
        // session = HibernateUtil.getSessionFactory().openSession();
        size = 0;
        StringBuilder sb = new StringBuilder();
        testVisitationAndRankIdsRecursive(treeableClass,
                root.getTreeId(), sb);
        Integer dbCount = (Integer) session.createQuery(
                "select count(e) from " + treeableClass + " e").uniqueResult();

        // Essentially the same as testing the treeService
        // getDescendantCount() method against count of db rows.
        if (size != dbCount) {
            sb.append("Nodes traversed (" + size
                    + ") is different than database row count (" + dbCount
                    + ")");
        }

        String errors = sb.toString();
        assertTrue(errors, errors.length() == 0);
        // } finally {
        // session.close();
        // }
    }

    /**
     * This is the recursive method for testVisitationAndRankId.
     * 
     * @param entityClass
     * @param treeId
     * @param sb
     * @return int size
     */
    private void testVisitationAndRankIdsRecursive(String entityClass,
            int treeId, StringBuilder sb) {
        size++;
        if (size % 1000 == 0) {
            System.out.println(size + " records processed.");
        }

        // First, check for cycles by checking if this node has been visited
        // more than once
        if (visited.contains(treeId)) {
            Assert.fail("Already visited tree ID " + treeId);

        }
        visited.add(treeId);

        Integer rankId = (Integer) session.createQuery(
                "select e.rankId from " + entityClass + " e where e.id = "
                        + treeId).uniqueResult();

        List<Object[]> children = session.createQuery(
                "select e.rankId, e.id from " + entityClass
                        + " e where e.parent.id = " + treeId).list();
        for (Object[] row : children) {
            Integer childRankId = (Integer) row[0];
            Integer childId = (Integer) row[1];

            // Second, check that the child's rank ID is greater than the
            // parent's.
            if (rankId > childRankId) {
                // Is this necessarily true for geographic hierarchies, such as
                // municipalites, towns, and shorelines?
                String s = "Parent ID :1 rank ID :2 is >= child ID :3 rankId :4\n";
                s = s.replaceAll(":1", String.valueOf(treeId))
                        .replaceAll(":2", String.valueOf(rankId))
                        .replaceAll(":3", String.valueOf(childId))
                        .replaceAll(":4", String.valueOf(childRankId));
                sb.append(s);
            }
            testVisitationAndRankIdsRecursive(entityClass, childId, sb);
        }
    }

    /**
     * Calls checkTreeService method to test the integrity of the tree when it
     * is accessed via calls to the TreeService. Checks for cycles, and correct
     * parent ids.
     * @throws ClassNotFoundException 
     * @throws HibernateException 
     */
    @Test
    public void testTreeService() throws HibernateException, ClassNotFoundException {
        String errors = checkTreeService();
        assertTrue(errors, errors.length() == 0);
    }

    /**
     * This method does the checking for testTreeService. It will traverse all
     * nodes before returning a StringBuilder object that contains all errors
     * found, if any.
     * 
     * @return StringBuilder
     */
    public String checkTreeService() {
        StringBuilder sb = new StringBuilder();

        Queue<Treeable> nodes = new LinkedList<Treeable>();
        Set<Integer> visited = new HashSet<Integer>();
        nodes.offer(root);

        while (!nodes.isEmpty()) {
            Treeable parent = nodes.poll();
            for (Treeable node : (Set<Treeable>) treeService
                    .getChildNodes(parent)) {

                // First, check the parent object reference and parent id
                if (node.getParent() != null) {
                    if (node.getParent().getTreeId() != parent.getTreeId()) {
                        sb.append(node.getTreeId()
                                + ": "
                                + node.getFullName()
                                + "(The child does not point back to the correct parent!\n)");
                    }
                } else {
                    sb.append(node.getTreeId() + ": " + node.getFullName()
                            + "(This node has no parent and isn't the root!)\n");
                }

                // Second, check for cycles by checking if this node has been
                // visited more than once
                int id = node.getTreeId();
                if (visited.contains(id)) {
                    Assert.fail(node.getTreeId() + ": " + node.getFullName()
                            + "This node has been visited already!");
                }

                // Enqueue current node
                nodes.offer(node);
                visited.add(id);
            }
        }

        return sb.toString();
    }

}
