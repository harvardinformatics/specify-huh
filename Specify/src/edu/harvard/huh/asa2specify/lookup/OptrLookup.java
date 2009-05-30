package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface OptrLookup
{
	public Agent queryById(Integer optrId) throws LocalException;
}
