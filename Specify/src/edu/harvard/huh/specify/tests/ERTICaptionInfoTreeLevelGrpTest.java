package edu.harvard.huh.specify.tests;

import java.sql.SQLException;

import org.junit.Test;

import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.tasks.subpane.qb.ERTICaptionInfoTreeLevelGrp;

public class ERTICaptionInfoTreeLevelGrpTest extends BaseTest {

    /**
     * ERTI affects the Query Builder. I had replaced the node number H/SQL
     * queries with a recursive getAncestors method. The only way to test the
     * change is through the UI, as the ERTI class provides no public methods. I
     * double-checked the replacement and ran a few of Gen's queries and
     * confirmed the results (regression test).
     */
    @Test
    public void testSetup() {
        getSession();
        ERTICaptionInfoTreeLevelGrp erti = new ERTICaptionInfoTreeLevelGrp(
                Taxon.class, 0, null, false, 0);

        try {
            erti.setUp();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
