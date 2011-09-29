package edu.harvard.huh.specify.mapper;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import edu.harvard.huh.dwc.model.Occurrence;
import edu.harvard.huh.specify.mapper.SpecifyFieldMappingDesc.PathSegment;
import edu.ku.brc.exceptions.ConfigurationException;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class SpecifyMapper {

	public Occurrence map(CollectionObject collObj) {
		return map(collObj, getDefaultMappings());
	}

	public Occurrence map(CollectionObject collObj, List<SpecifyMapItem> mapItems) {
/*
 * private Long   id;
	private String catalogNumber;
	private String occurrenceDetails;
	private String occurrenceRemarks;
	private String recordNumber;
	private String recordedBy;
	private String individualId;
	private String individualCuunt;
	private String sex;
	private String lifeStage;
	private String reproductiveCondition;
	private String behavior;
	private String establishmentMeans;
	private String occurrenceStatus;
	private String preparations;
	private String disposition;
	private String otherCatalogNumbers;
	private String previousIdentifications;
	private String associatedMedia;
	private String associatedReferences;
	private String associatedOccurrences;
	private String associatedSequences;
	private String associatedTaxa;
 */
		return null;
	}
	
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
}
