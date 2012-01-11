/*
 * Created on Nov 30, 2011
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

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Run all the tree tests. Add more tests to Suite.SuiteClasses annotation.
 * Cointains no methods.
 * 
 * lchan: BaseTreeBusRulesTest should be run first, before mutative tests
 * (TaxonTreeTest) run.
 * 
 * @author lowery, lchan
 * 
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ BaseTreeBusRulesTest.class, GeographyTreeTest.class,
        StorageTreeTest.class, TaxonTreeTest.class, TreeIntegrityTest.class })
public class RunTreeTests {
    
    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("src/edu/harvard/huh/specify/tests/load-sample-db.sh");
        Process process = builder.start();
        process.waitFor();
    }
}