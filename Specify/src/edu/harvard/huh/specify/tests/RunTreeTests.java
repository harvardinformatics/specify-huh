package edu.harvard.huh.specify.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({BaseTreeBusRulesTest.class, ERTICaptionInfoTreeLevelGrpTest.class,
					 GeographyTreeTest.class, StorageTreeTest.class, TaxonTreeTest.class,
					 TreeLevelQRITest.class, TreeTableViewerTest.class})
public class RunTreeTests {
	
}