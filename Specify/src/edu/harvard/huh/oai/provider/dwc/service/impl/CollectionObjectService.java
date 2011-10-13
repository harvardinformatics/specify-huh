package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import edu.harvard.huh.oai.provider.dwc.dao.CollectionObjectDao;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class CollectionObjectService implements NativeObjectService {

	// this class is entirely based on SampleDatasetService.  --mmk
	
	private CollectionObjectDao collectionObjectDao;
	
	@Override
	public Object getObject(Long id) {
		return collectionObjectDao.get(id);
	}

	@Override
	public List<Object> getObjects(Date fromDate, Date toDate) {
		List<Object> objects = new ArrayList<Object>();
		List<CollectionObject> collectionObjects = collectionObjectDao.getCollectionObjects(fromDate, toDate); 
		for (CollectionObject collectionObject : collectionObjects) {
			objects.add(collectionObject);
		}
		
		return objects; 
	}

	public void setCollectionObjectDao(CollectionObjectDao collectionObjectDao) {
		this.collectionObjectDao = collectionObjectDao;
	}
}
