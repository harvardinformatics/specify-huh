package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface OptrLookup
{
	public Agent getByOptrId(Integer optrId) throws LocalException;
}
