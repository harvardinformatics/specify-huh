package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.util.HashMap;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import au.org.tern.ecoinformatics.oai.provider.service.IdentifierService;

public class SpecifyIdentifierService implements IdentifierService {

	private HashMap<Class<?>, String> tableAbbrevs = null;
	
	public SpecifyIdentifierService() {
		this(DBTableIdMgr.getInstance()); // TODO: get this from Spring?
	}
	
	public void setDBTableIdMgr(DBTableIdMgr tableMgr) {
		// TODO: remove this method
		this.tableAbbrevs = new HashMap<Class<?>, String>();
		
		for (DBTableInfo tableInfo : tableMgr.getTables()) {
			tableAbbrevs.put(tableInfo.getClassObj(), tableInfo.getAbbrev());
		}
	}

	public SpecifyIdentifierService(DBTableIdMgr tableMgr) {

		this.tableAbbrevs = new HashMap<Class<?>, String>();
		
		for (DBTableInfo tableInfo : tableMgr.getTables()) {
			tableAbbrevs.put(tableInfo.getClassObj(), tableInfo.getAbbrev());
		}
	}

	@Override
	public String createOaiIdentifier(Object object) {
		
		Class<?> clss = object.getClass();
		String tableAbbrev = tableAbbrevs.get(clss);
		
		// TODO: Can't call CollectionObject.toString without the ui framework being set up; this should probably change.
		if (tableAbbrev == null) throw new IllegalArgumentException("Object's class is not in Specify's data model: " + clss);
		
		DataModelObjBase specifyObj = (DataModelObjBase) object;
		Integer id = specifyObj.getId();
		
		if (id == null) throw new IllegalArgumentException("Object has no id");
		
		return tableAbbrev + "." + id;
	}

	@Override
	public String extractInternalIdentifier(String string) {
		
		if (! string.matches("\\w+\\.\\d+")) return null;
		
		int dotIndex = string.indexOf('.');
		
		String tableAbbrev = string.substring(0, dotIndex);
		
		if (! tableAbbrevs.containsValue(tableAbbrev)) return null;
		
		Integer id = Integer.parseInt(string.substring(dotIndex + 1));

		return String.valueOf(id);
	}

	@Override
	public String extractSetSpec(String arg0) {
		// as defined in the IdentifierService interface, this method presumes
		// the oai identifier contains semantics for set membership; there is
		// a better place to obtain set membership info from the object itself
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNamespace() {
		return "http://rs.tdwg.org/dwc/xsd/simpledarwincore/";
	}
}
