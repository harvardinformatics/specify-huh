package edu.harvard.huh.oai.provider.dwc.dao;

import java.util.Date;
import java.util.List;

import edu.ku.brc.specify.datamodel.CollectionObject;

public interface CollectionObjectDao {

	public CollectionObject get(Long id);
	
	public List<CollectionObject> getCollectionObjects(Date from, Date until);
}
