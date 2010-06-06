package edu.harvard.huh.specify.datamodel.busrules;

import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;

public class HUHPrepFragmentBusRules extends HUHFragmentBusRules implements BusinessRulesIFace
{
    public HUHPrepFragmentBusRules()
    {
        super();
    }

    @Override
    public void addChildrenToNewDataObjects(Object newDataObj)
    {
        ;
    }
        
    @Override
    public void okToDelete(final Object dataObj,
                           final DataProviderSessionIFace session,
                           final BusinessRulesOkDeleteIFace deletable)
    {
        // if is a fragment and has a singleton preparation, get rid of the preparation, too
        // the preparation is not a FormViewObj, just a data obj; if we put it on the same
        // MultiView objects-to-delete list, it will get deleted for us on the save of the
        // parent CollectionObject
        
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            Preparation prep = fragment.getPreparation();
            
            if (prep != null && prep.getFragments().size() == 1)
            {
                MultiView mv = formViewObj.getMVParent();
                mv.getTopLevel().addDeletedItem(prep);
            }
        }
        super.okToDelete(dataObj, session, deletable);
    }
}
