/**
 * Created on Dec 7, 2011
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
 */
package edu.harvard.huh.specify.tests;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.ku.brc.dbsupport.HibernateUtil;

/**
 * Tests the integrity of a Treeable tree. May take a long time to run,
 * depending on the size of the tree.
 * 
 * @author lchan
 * 
 */
public class TreeIntegrityTest extends BaseTreeableTest {

    public TreeIntegrityTest() {
        super(0, null, 0, 0, 0);
    }

    @Test
    public void check() {
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            checkRecursive("Taxon", 1);
        } finally {
            session.close();
        }
    }

    private void checkRecursive(String entityClass, int treeId) {
        if (treeId % 1000 == 0) {
            System.out.println(treeId + " records processed.");
        }

        Integer rankId = (Integer) session.createQuery(
                "select e.rankId from " + entityClass + " e where e.id = "
                        + treeId).uniqueResult();

        @SuppressWarnings("unchecked")
        List<Object[]> children = session.createQuery(
                "select e.rankId, e.id from " + entityClass
                        + " e where e.parent.id = " + treeId).list();
        for (Object[] object : children) {
            Integer childRankId = (Integer) object[0];
            Integer childId = (Integer) object[1];

            Assert.assertTrue("Parent ID rankID " + treeId + " is >= child "
                    + childRankId, childRankId > rankId);

            checkRecursive(entityClass, childId);
        }
    }
}
