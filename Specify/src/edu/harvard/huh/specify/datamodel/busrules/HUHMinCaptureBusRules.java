package edu.harvard.huh.specify.datamodel.busrules;

import edu.ku.brc.af.ui.forms.BusinessRulesIFace;

public class HUHMinCaptureBusRules extends HUHFragmentBusRules implements BusinessRulesIFace
{
    public HUHMinCaptureBusRules()
    {
        super();
    }
    
    @Override
    public boolean shouldCloneField(final String fieldName)
    {
        if ("preparation".equals(fieldName)) return true;
        
        return super.shouldCloneField(fieldName);
    }
    
}
