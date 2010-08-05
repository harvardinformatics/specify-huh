package edu.harvard.huh.specify.datamodel.busrules;

import edu.ku.brc.specify.datamodel.Fragment;

public class HUHCollObjFragmentBusRules extends HUHFragmentBusRules
{
    public HUHCollObjFragmentBusRules()
    {
        super();
    }

    @Override
    public boolean okToEnableDelete(final Object dataObj)
    {
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            
            // In our collectionrelationship form, the user can associate a fragment
            // with the current object.  The associated fragment becomes the right side,
            // and the current becomes the left.  f.getRightSideRels() returns
            // CollectionRelationship objects for which fragment f is on the right side.
            if (fragment.getRightSideRels().size() > 0) return false;
            if (fragment.getLeftSideRels().size()  > 0) return false;
        }
        
        return true;
    }
}
