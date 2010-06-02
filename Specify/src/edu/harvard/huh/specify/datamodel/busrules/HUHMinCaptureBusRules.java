package edu.harvard.huh.specify.datamodel.busrules;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;

public class HUHMinCaptureBusRules extends HUHFragmentBusRules implements BusinessRulesIFace
{
    public HUHMinCaptureBusRules()
    {
        super();
    }
    
    @Override
    public boolean beforeDeleteCommit(final Object dataObj, final DataProviderSessionIFace session) throws Exception
    {
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            
            CollectionObject collObj = fragment.getCollectionObject();
            if (collObj != null)
            {
                if (collObj.getFragments().size() <= 1)
                {
                    // check business rules
                    BusinessRulesIFace delBusRules = DBTableIdMgr.getInstance().getBusinessRule(collObj);
                    if (delBusRules != null)
                    {
                        if (!delBusRules.okToEnableDelete(collObj)) return false;
                        
                        delBusRules.beforeDelete(collObj, session);
                        session.delete(collObj);
                        delBusRules.beforeDeleteCommit(collObj, session);                        
                    }
                    session.delete(collObj);
                }
            }
            
            Preparation prep = fragment.getPreparation();
            if (prep != null)
            {
                if (prep.getFragments().size() <= 1)
                {
                    // check business rules
                    BusinessRulesIFace delBusRules = DBTableIdMgr.getInstance().getBusinessRule(prep);
                    if (delBusRules != null)
                    {
                        if (!delBusRules.okToEnableDelete(prep)) return false;
                        
                        delBusRules.beforeDelete(prep, session);
                        session.delete(prep);
                        delBusRules.beforeDeleteCommit(prep, session);                        
                    }
                    session.delete(prep);
                }
            }
        }
        return true;
    }
    
    @Override
    public void beforeFormFill()
    {
        ;
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
