package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface AffiliateLookup
{
	// TODO: normalize interface names and method names
	public Agent getByAffiliateId(Integer affiliateId) throws LocalException;
}
