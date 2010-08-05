package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface OrganizationLookup
{
	// TODO: create exception class for lookups
	public Agent getById(Integer organizationId) throws LocalException;
}
