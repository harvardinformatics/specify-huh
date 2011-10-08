package edu.harvard.huh.specify.mapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Element;

import edu.harvard.huh.specify.mapper.SpecifyFieldMappingDesc.PathSegment;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.exceptions.ConfigurationException;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class SpecifyMapper {

	private JoinWalker joinWalker;

	private SimpleDateFormat yearMonthDayTimeFormat;
	private SimpleDateFormat yearMonthDayFormat;
	private SimpleDateFormat yearMonthFormat;
	private SimpleDateFormat yearFormat;
	private SimpleDateFormat timeFormat;
	
	/**
	 * Return a mapping of dwc terms (all lowercase) to String values found by traversing a CollectionObject
	 * according to the mappings given in a default list of SpecifyMapItems.
	 */
	public HashMap<String, String> map(CollectionObject collObj) {
		return map(collObj, getDefaultMappings());
	}

	/**
	 * Return a mapping of dwc terms (all lowercase) to String values found by traversing a CollectionObject
	 * according to the mappings given in the list of SpecifyMapItems.
	 */
	public HashMap<String, String> map(CollectionObject collObj, List<SpecifyMapItem> mapItems) {
		
		HashMap<String, String> dwcTermsToValues = new HashMap<String, String>();
		
		joinWalker = new JoinWalker(DBTableIdMgr.getInstance());
		joinWalker.setYearMonthDayTimeFormat(yearMonthDayTimeFormat);
		joinWalker.setYearMonthDayFormat(yearMonthDayFormat);
		joinWalker.setYearMonthFormat(yearMonthFormat);
		joinWalker.setYearFormat(yearFormat);
		joinWalker.setTimeFormat(timeFormat);
		
		for (SpecifyMapItem mapItem : mapItems) {
			
			String mappedValue = joinWalker.getPathValue(collObj, mapItem.getPathSegments(), mapItem.getFieldName(), false);
			
			String dwcTerm = mapItem.getName();
			
			dwcTermsToValues.put(dwcTerm, mappedValue);
		}
		return dwcTermsToValues;
	}
	
	/**
	 * Read SpecifyMapItems in from config/dwcdefaultmap.xml and return the list.
	 * The xml file is expected to match the xpath /default_mappings/default_mapping,
	 * with each <default_mapping> representing a SpecifyMapItem.
	 */
	public static List<SpecifyMapItem> getDefaultMappings() {

		List<SpecifyMapItem> list = new ArrayList<SpecifyMapItem>();

		// this is from ConceptMapUtils and MappedFieldInfo
		Element root = XMLHelper.readDOMFromConfigDir("dwcdefaultmap.xml");
		List<?> nodes = root.selectNodes("/default_mappings/default_mapping");

		for (Object obj : nodes) {
			SpecifyMapItem mapItem = parseMapItem((Element) obj);
			list.add(mapItem);
		}
		
		return list;
	}
	
	/**
	 * Marshall an xml element representation of a SpecifyMapItem.  Each element is
	 * expected to have the following attributes:
	 * 
	 * "name:" required; all lowercase, the darwin core concept name, e.g. "decimallongitude"
	 * 
	 * "table_path:" required; a comma-separated list of specify table ids that gives the join path.
	 * For example, "1,10,2" goes from collectionobject to collectingevent to locality (see the file
	 * config/specify_tableid_listing.xml for table ids).  When the relationship is ambiguous
	 * because there is more than one foreign key to the same table, the relationship name is appended
	 * with a preceding "-", e.g. "1,5-modifiedByAgent" goes from collectionobject to agent, and so
	 * does "1,5-createdByAgent"
	 * 
	 * "fieldname:" required; the name of the field from which to take the mapped value,
	 * from the last table in the join path.
	 * 
	 * "is_relationship:" required if true; when true, indicates that the concept may have
	 * multiple values; otherwise, only the first of multiple values will be used (objects are 
	 * sorted as DataModelObjBase objects by TimestampModified if not otherwise Comparable).  Note
	 * that if any of the joins previous to the final are n-to-many, only the first of these lists
	 * will be used to follow the remainder of the join path.
	 * 
	 * "is_active:" required if false; when false, indicates that the concept should not be used
	 * 
	 * @throws ConfigurationException
	 */
	public static SpecifyMapItem parseMapItem(Element element) throws ConfigurationException {
		
		SpecifyMapItem mapItem = new SpecifyMapItem();

		// required; all lowercase, the darwin core concept name, e.g. "decimallongitude"
		String name = XMLHelper.getAttr(element, "name", null);
		if (! name.matches("\\w+")) throw new ConfigurationException("Name attribute value must match '\\w+': " + name);
		mapItem.setName(name);

		// required; the Specify table's field name that contains the value for the concept, e.g. "longitude1"
		String fieldName = XMLHelper.getAttr(element, "fieldname", null);
		if (! fieldName.matches("\\w+")) throw new ConfigurationException("FieldName attribute value must match '\\w+': " + fieldName);
		mapItem.setFieldName(fieldName);

		// required; a comma-separated list of specify table ids that gives the join path, e.g. "1,10,2"
		// goes from collectionobject to collectingevent to locality.  When the relationship is ambiguous
		// because there is more than one foreign key to the same table, the relationship name is appended
		// with a preceding "-", e.g. "1,5-modifiedByAgent" goes from collectionobject to agent, and so
		// does "1,5-createdByAgent"
		String tablePath = XMLHelper.getAttr(element, "table_path", null);
		List<PathSegment> pathSegments = JoinWalker.parseTablePath(tablePath);
		mapItem.setPathSegments(pathSegments);

		// required if true; if this is "true," then the concept is at the "one" end of a "one-to-many" relationship
		mapItem.setIsRelationship(XMLHelper.getAttr(element, "is_relationship", false));

		// required if false; this is a way of saying "don't use this concept right now"
		mapItem.setIsActive(XMLHelper.getAttr(element, "active", true));
		
		return mapItem;
	}
	
	public SimpleDateFormat getYearMonthDayTimeFormat() {
		return yearMonthDayTimeFormat;
	}
	
	public void setYearMonthDayTimeFormat(SimpleDateFormat yearMonthDayTimeFormat) {
		this.yearMonthDayTimeFormat = yearMonthDayTimeFormat;
	}
	
	public SimpleDateFormat getYearMonthDayFormat() {
		return yearMonthDayFormat;
	}
	
	public void setYearMonthDayFormat(SimpleDateFormat yearMonthDayFormat) {
		this.yearMonthDayFormat = yearMonthDayFormat;
	}
	
	public SimpleDateFormat getYearMonthFormat() {
		return yearMonthFormat;
	}
	
	public void setYearMonthFormat(SimpleDateFormat yearMonthFormat) {
		this.yearMonthFormat = yearMonthFormat;
	}
	
	public SimpleDateFormat getYearFormat() {
		return yearFormat;
	}
	
	public void setYearFormat(SimpleDateFormat yearFormat) {
		this.yearFormat = yearFormat;
	}
	
	public SimpleDateFormat getTimeFormat() {
		return timeFormat;
	}
	
	public void setTimeFormat(SimpleDateFormat timeFormat) {
		this.timeFormat = timeFormat;
	}
}
