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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.Test;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.treeutils.HibernateTreeDataServiceImpl;
import edu.ku.brc.specify.treeutils.TreeDataService;

public class BaseTreeableTest extends BaseTest {
	protected  Class treeDefClass;
	protected  Class treeableClass;
	protected  TreeDefIface currentDef;
	protected  TreeDataService treeService;
	protected  Treeable root;
	protected  final int DEFAULT_TREE_DEF_ID = 1;
	
	protected static List<String[]> propsList = new LinkedList<String[]>();
	
	protected int lookupId;
	protected String lookupName;
	protected int moveFrom;
	protected int moveTo;
	protected int deleteId;
	
	public BaseTreeableTest(int lookupId, String lookupName, int moveFrom, int moveTo, int deleteId) {
		this.lookupId = lookupId;
		this.lookupName = lookupName;
		this.moveFrom = moveFrom;
		this.moveTo = moveTo;
		this.deleteId = deleteId;
	}

	public void initialize(Class treeDefClass, Class treeableClass) {
		getSession();
		this.treeDefClass = treeDefClass;
		this.treeableClass = treeableClass;
		currentDef = (TreeDefIface)session.load(treeDefClass, DEFAULT_TREE_DEF_ID);
		AppContextMgr.getInstance().setClassObject(treeDefClass, currentDef);
		treeService = new HibernateTreeDataServiceImpl();
		session.close();
		
		root = treeService.getRootNode(currentDef);
	}
	
	protected static Collection<Object[]> getTreeableParams(String lookupProp, String moveProp, String deleteProp) {
		TestParameter lookupParams = new TestParameter(lookupProp);
		TestParameter moveParams = new TestParameter(moveProp);
		TestParameter deleteParams = new TestParameter(deleteProp);
		
		Collection<Object[]> params = new LinkedList<Object[]>();

		// {lookupId, lookupName, moveFrom, moveTo, deleteId }
		while (lookupParams.hasNext() && moveParams.hasNext() && deleteParams.hasNext()) {
			params.add(new Object[] { Integer.parseInt(lookupParams.getParam()),
								  lookupParams.getParam(),
								  Integer.parseInt(moveParams.getParam()),
								  Integer.parseInt(moveParams.getParam()),
								  Integer.parseInt(deleteParams.getParam()) });
		}
		
		return params;
	}

	@Test public void testFindByName() {
		assertTrue(treeService.findByName(currentDef, lookupName, true).size() > 0);
	}

	@Test public void testGetNodeById() {
		assertTrue(treeService.getNodeById(treeableClass, lookupId) != null);
	}

	@Test public void testDeleteTreeNode() {
		assertTrue(treeService.getNodeById(treeableClass, deleteId) != null);
		Treeable node = treeService.getNodeById(treeableClass, deleteId);
		treeService.deleteTreeNode(node);
		assertTrue(treeService.getNodeById(treeableClass, deleteId) == null);
	}

	@Test public void testMoveTreeNode() {
		Treeable from = treeService.getNodeById(treeableClass, moveFrom);
		Treeable to = treeService.getNodeById(treeableClass, moveTo);
		treeService.moveTreeNode(from, to);
		
		Treeable child = treeService.getNodeById(new Taxon().getClass(), moveFrom);
		assertTrue(child.getParent().getTreeId() == moveTo);
	}

	@Test public void testRootNode() 		
	{
		assertTrue(treeService.getRootNode(currentDef) != null);
	}
	
	@Test public void testRootNodeChildren() {
		assertTrue(treeService.getChildNodes(root) != null);
	}
	
	@Test public void testTreeIntegrity() {
		assertFalse(checkTreeIntegrity());
	}
	
	@Test public void testGetDescendantCount() {
		assertTrue(treeService.getDescendantCount(root) == getSize(root)-1);
	}

	public boolean checkTreeIntegrity() {	
		Queue<Treeable> nodes = new LinkedList<Treeable>();
		Set<Integer> visited = new HashSet<Integer>();
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
				int id = node.getTreeId();
				if (visited.contains(id)) {
					errStr = node.getTreeId() + ": " + node.getFullName() + "This node has been visited already!";
					error = true;
				}

				// Enqueue current node and increment the count
				nodes.offer(node);
				visited.add(id);
			}
		}
		System.out.println(errStr);
		return error;
	}
	
	public int getSize(Treeable parent) {
		int size = 1;
		for (Treeable child : (Set<Treeable>)treeService.getChildNodes(parent)) {
			size += getSize(child);
		}
		return size;
	}
}
