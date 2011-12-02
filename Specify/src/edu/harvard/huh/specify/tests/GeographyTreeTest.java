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

import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.Storage;
import edu.ku.brc.specify.datamodel.StorageTreeDef;

@RunWith(value = Parameterized.class)
public class GeographyTreeTest extends BaseTreeableTest {
	
	public GeographyTreeTest(int lookupId, String lookupName, int moveFrom, int moveTo, int deleteId) {
		super(lookupId, lookupName, moveFrom, moveTo, deleteId);
	}

	@Parameters public static Collection<Object[]> data() throws IOException {
		Properties props = new Properties();
		props.load(TaxonTreeTest.class.getResourceAsStream("testing.properties"));

		propsList.add(props.getProperty("testing.geography.lookup").split(","));
		propsList.add(props.getProperty("testing.geography.move").split(","));
		propsList.add(props.getProperty("testing.geography.delete").split(","));

		return getParams(propsList);
	}

	@Before public void setTreeable() {
		initialize(new GeographyTreeDef().getClass(), new Geography().getClass());
	}
}
