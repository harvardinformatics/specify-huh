package edu.harvard.huh.oai.specify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Element;

import edu.ku.brc.exceptions.ConfigurationException;
import edu.ku.brc.helpers.XMLHelper;

public class SpecifyFieldMappingDesc {

	// see Specify's MappedFieldInfo

	private String stringId        = null;
	private String name            = null;
	private String fieldName       = null; 
	private boolean isRelationship = false;
	private boolean isActive       = true;
	
	private List<PathSegment> pathSegments  = null;

	public SpecifyFieldMappingDesc() {
		;
	}

	public SpecifyFieldMappingDesc(String fieldName, String tablePath, boolean isRelationship) {
		this.fieldName = fieldName;
		this.pathSegments = parseTablePath(tablePath);
		this.isRelationship = isRelationship;
	}

	public String getStringId() {
		return stringId;
	}
	public String getName() {
		return name;
	}
	public String getFieldName() {
		return fieldName;
	}
	public List<PathSegment> getPathSegments() {
		return pathSegments;
	}
	public boolean getIsRelationship() {
		return isRelationship;
	}
	public boolean getIsActive() {
		return isActive;
	}
	public void setStringId(String stringId) {
		this.stringId = stringId;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public void setPathSegments(List<PathSegment> pathSegments) {
		this.pathSegments = pathSegments;
	}
	public void setIsRelationship(boolean isRelationship) {
		this.isRelationship = isRelationship;
	}
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public static HashMap<String, SpecifyFieldMappingDesc> getDefaultMappings() {

		HashMap<String, SpecifyFieldMappingDesc> map = new HashMap<String, SpecifyFieldMappingDesc>();

		// this is from ConceptMapUtils and MappedFieldInfo
		Element root = XMLHelper.readDOMFromConfigDir("dwcdefaultmap.xml");
		List<?> nodes = root.selectNodes("/default_mappings/default_mapping");

		for (Object obj : nodes) {
			SpecifyFieldMappingDesc mapItem = parse((Element) obj);
			map.put(mapItem.getName(), mapItem);
		}
		
		return map;
	}
	
	public static SpecifyFieldMappingDesc parse(Element element) throws ConfigurationException {
		
		SpecifyFieldMappingDesc mapItem = new SpecifyFieldMappingDesc();

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
		List<PathSegment> pathSegments = parseTablePath(tablePath);
		mapItem.setPathSegments(pathSegments);

		// required if true; if this is "true," then the concept is at the "one" end of a "one-to-many" relationship
		mapItem.setIsRelationship(XMLHelper.getAttr(element, "is_relationship", false));

		// required if false; this is a way of saying "don't use this concept right now"
		mapItem.setIsActive(XMLHelper.getAttr(element, "active", true));
		
		return mapItem;
	}

	public static List<PathSegment> parseTablePath(String tablePath) throws ConfigurationException {

		if (tablePath == null || !tablePath.matches("\\d+(-\\w+)?(,\\d+(-\\w+)?)*")) {
			throw new ConfigurationException("TablePath must match '\\d+(-\\w+)?(,\\d+(-\\w+)?)*': " + tablePath);
		}

		String[] pathSegmentStrings = tablePath.split(",");
		List<PathSegment> pathSegments = new ArrayList<PathSegment>(pathSegmentStrings.length);

		for (String pathSegmentString : pathSegmentStrings) {

			Integer tableId = null;
			String relationshipName = null;

			int i = pathSegmentString.indexOf('-');

			if (i >= 0) {
				tableId = Integer.parseInt(pathSegmentString.substring(0, i));
				relationshipName = pathSegmentString.substring(i + 1);
			}
			else {
				tableId = Integer.parseInt(pathSegmentString);
			}
			pathSegments.add(new PathSegment(tableId, relationshipName));
		}
		
		return pathSegments;
	}
	
	public static class PathSegment {
		private Integer tableId;
		private String relationshipName;
		
		public PathSegment(Integer tableId, String relationshipName) {
			this.tableId = tableId;
			this.relationshipName = relationshipName;
		}
		public Integer getTableId() { return this.tableId; }
		public void setTableId(Integer tableId) { this.tableId = tableId; }

		public String getRelationshipName() { return this.relationshipName; }
		public void setRelationshipName(String relationshipName) { this.relationshipName = relationshipName; }
	}
}
