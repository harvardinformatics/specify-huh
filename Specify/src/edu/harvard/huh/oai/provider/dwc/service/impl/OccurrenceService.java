package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import edu.harvard.huh.dwc.model.Occurrence;
import edu.harvard.huh.oai.provider.dwc.dao.OccurrenceDao;

public class OccurrenceService implements NativeObjectService {

	// this class is entirely based on SampleDatasetService.  --mmk
	
	private OccurrenceDao occurrenceDao;
	
	@Override
	public Object getObject(Long id) {
		return occurrenceDao.get(id);
	}

	@Override
	public List<Object> getObjects(Date fromDate, Date toDate) {
		List<Object> objects = new ArrayList<Object>();
		List<Occurrence> occurrences = occurrenceDao.getOccurrences(fromDate, toDate); 
		for (Occurrence occurrence : occurrences) {
			objects.add(occurrence);
		}
		
		return objects; 
	}

}
