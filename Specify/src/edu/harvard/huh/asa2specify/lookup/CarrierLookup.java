package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface CarrierLookup
{
	public Agent getByName(String carrierName) throws LocalException;
	
	public Agent queryByName(String carrierName) throws LocalException;
}
