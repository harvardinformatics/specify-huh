package edu.harvard.huh.oai.provider.dwc.dao;

import java.util.Date;
import java.util.List;

import edu.harvard.huh.dwc.model.Occurrence;

public interface OccurrenceDao {

	public Occurrence get(Long id);
	
	public List<Occurrence> getOccurrences(Date from, Date until);
}
