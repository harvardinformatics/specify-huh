package edu.harvard.huh.oai.specify;

import java.util.List;

import edu.harvard.huh.oai.specify.JoinWalker.PathSegment;

public class SpecifyMapItem {

	// see Specify's MappedFieldInfo

	private String stringId        = null;
	private String name            = null;
	private String fieldName       = null; 
	private boolean isRelationship = false;
	private boolean isActive       = true;
	
	private List<PathSegment> pathSegments  = null;

	public SpecifyMapItem() {
		;
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
