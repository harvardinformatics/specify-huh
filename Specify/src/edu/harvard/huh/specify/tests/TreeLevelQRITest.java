package edu.harvard.huh.specify.tests;

import org.junit.Ignore;
import org.junit.Test;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.specify.tasks.subpane.qb.TableQRI;
import edu.ku.brc.specify.tasks.subpane.qb.TableTree;
import edu.ku.brc.specify.tasks.subpane.qb.TreeLevelQRI;

public class TreeLevelQRITest extends BaseTest {

    /**
     * Work in progress...
     * 
     * @throws Exception
     */
    @Test
//    @Ignore
    public void testGetNodeNumberCriteria() throws Exception {
        System.setProperty(AppContextMgr.factoryName,                   "edu.ku.brc.specify.config.SpecifyAppContextMgr");
        
        DBFieldInfo info = new DBFieldInfo(new DBTableInfo(0, "edu.ku.brc.specify.datamodel.Taxon", "taxon",
                "taxonId", "e"), "taxonId", "taxonId", "", 0, false, false,
                false, false, false, null);
        
        DBTableInfo dbTableInfo = new DBTableInfo(0, "edu.ku.brc.specify.datamodel.Taxon", "Taxon", "taxonId", "e");
        
        TableQRI tableQri = new TableQRI(new TableTree("Taxon", "taxonId", "e", dbTableInfo));

        TreeLevelQRI qri = new TreeLevelQRI(tableQri, info, 0);

        qri.getNodeNumberCriteria("", null, "", false);
    }

}
