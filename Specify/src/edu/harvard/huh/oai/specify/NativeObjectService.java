package edu.harvard.huh.oai.specify;

import java.util.Date;
import java.util.List;

public interface NativeObjectService {
	
	public Object getObject(Long id);
	
	public List<Object> getObjects(Date fromDate, Date toDate);
}
