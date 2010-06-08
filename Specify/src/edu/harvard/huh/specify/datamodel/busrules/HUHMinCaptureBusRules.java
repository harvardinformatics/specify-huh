package edu.harvard.huh.specify.datamodel.busrules;

import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;

public class HUHMinCaptureBusRules extends HUHFragmentBusRules implements BusinessRulesIFace
{
    public HUHMinCaptureBusRules()
    {
        super();
    }
    
    @Override
    public void beforeFormFill()
    {
        ; // don't create the collection object
    }

    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        Fragment fragment = (Fragment) dataObj;
        
        // save the preparation
        Preparation prep = fragment.getPreparation();

        if (prep != null)
        {
            if (prep.getCountAmt() == null) prep.setCountAmt(1);

            // save the preparation
            prep = (Preparation) HUHFragmentBusRules.saveObject(prep, session);

            fragment.setPreparation(prep);
            prep.getFragments().add(fragment);
        }
    }

    @Override
    public boolean okToEnableDelete(final Object dataObj)
    {
        boolean ok = super.okToEnableDelete(dataObj);
        
        if (!ok) return false;
        
        if (dataObj instanceof Preparation)
        {
            Preparation prep = (Preparation) dataObj;
            
            if (prep.getFragments().size() > 1) return false;
        }
        
        return true;
    }

    @Override
    public boolean shouldCloneField(final String fieldName)
    {
        if ("preparation".equals(fieldName)) return true;
        
        return super.shouldCloneField(fieldName);
    }
    
}
