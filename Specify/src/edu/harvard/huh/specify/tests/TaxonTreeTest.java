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
import java.util.Collection;
import java.util.Properties;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;

/**
 * Taxon tree test. Runs all tests in BaseTreeableTest with parameters from
 * the testing properties file for taxon.
 * 
 * @author lowery
 *
 */
@RunWith(value = Parameterized.class)
public class TaxonTreeTest extends BaseTreeableTest {

	/**
	 * Constructor calls superclass with parameters.
	 * 
	 * @param lookupId
	 * @param lookupName
	 * @param moveFrom
	 * @param moveTo
	 * @param deleteId
	 */
	public TaxonTreeTest(int lookupId, String lookupName, int moveFrom, int moveTo, int deleteId) {
		super(lookupId, lookupName, moveFrom, moveTo, deleteId);
	}

	/**
	 * Obtains parameters from the testing properties file and calls
	 * getTreeableParams to obtain a Collection<Object[]> that
	 * represents a collection of sets of parameters for each
	 * testing run.
	 * 
	 * @return
	 * @throws IOException
	 */
	@Parameters public static Collection<Object[]> data() throws IOException {
		Properties props = new Properties();
		props.load(TaxonTreeTest.class.getResourceAsStream("testing.properties"));

		String lookupProp = props.getProperty("testing.taxon.lookup");
		String moveProp = props.getProperty("testing.taxon.move");
		String deleteProp = props.getProperty("testing.taxon.delete");

		return getTreeableParams(lookupProp, moveProp, deleteProp);
	}

	/**
	 * Initializes the test with TaxonTreeDef class and the Taxon class.
	 */
	@Before public void setTreeable(){
		initialize(new TaxonTreeDef().getClass(), new Taxon().getClass());
	}
}
