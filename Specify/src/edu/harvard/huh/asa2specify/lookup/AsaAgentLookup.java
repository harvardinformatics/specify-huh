package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface AsaAgentLookup
{
	public Agent getByAsaAgentId(Integer asaAgentId) throws LocalException;
}
