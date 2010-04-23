package edu.harvard.huh.specify.datamodel.busrules;

import org.apache.log4j.Logger;

import edu.ku.brc.af.ui.forms.BusinessRulesIFace;

public class HUHBatchFragmentBusRules extends HUHFragmentBusRules implements BusinessRulesIFace
{
    private static final Logger  log   = Logger.getLogger(HUHBatchFragmentBusRules.class);

	@Override
	public boolean shouldCloneField(String fieldName)
	{
		if ("preparation".equals(fieldName)) return true;
		
		return super.shouldCloneField(fieldName);
	}
}
