package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface SeriesLookup
{
	public Agent getById(Integer seriesId) throws LocalException;
}
