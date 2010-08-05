package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface AffiliateLookup
{
	public Agent getById(Integer affiliateId) throws LocalException;
}
