package edu.harvard.huh.specify.mapper;

import java.util.List;

public class SpecifyMapItem {

	// see Specify's MappedFieldInfo

	private String stringId        = null;
	private String name            = null; // all-lowercase version of the dwc term
	private String fieldName       = null; // the specify table field name
	private boolean isRelationship = false;
	private boolean isActive       = true;
	
	private List<PathSegment> pathSegments  = null;

	public SpecifyMapItem() {
		;
	}

	public String toString(SpecifyMapItem mapItem) {
 		return name + ":" + fieldName + ":" + toString(mapItem.getPathSegments());
 	}

 	private String toString(List<PathSegment> pathSegments) {
 		StringBuffer sb = new StringBuffer();
 		for (PathSegment segment : pathSegments) {
 			String name = segment.getRelationshipName();
 			Integer tableId = segment.getTableId();
 			if (name == null) name = Integer.toString(tableId);
 			sb.append(name);
 			sb.append(",");
 		}
 		return sb.toString().replaceAll(",$", "");
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
	
	public SpecifyMapItem(String fieldName, String tablePath, boolean isRelationship) {
		this.fieldName = fieldName;
		this.pathSegments = JoinWalker.parseTablePath(tablePath);
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
}
