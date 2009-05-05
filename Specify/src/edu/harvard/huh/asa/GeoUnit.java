package edu.harvard.huh.asa;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GeoUnit {

	private Integer id;
	private Integer parentId;
	private      String name;
	private      String abbreviation;
	private      String isoCode;
	private      String vernacularName;
	private      String displayQualifier;
	private      String rank;
	private      String remarks;
	private     Integer createdById;
	private        Date dateCreated;
	private Set<String> variantNames = new HashSet<String>();
	
	public Integer getId() { return id; }
	
	public Integer getParentId() { return parentId; }
	
	public String getName() { return name; }
	
	public String getAbbreviation() { return abbreviation; }
	
	public String getIsoCode() { return isoCode; }
	
	public String getVernacularName() { return vernacularName; }
	
	public String getDisplayQualifier() { return displayQualifier; }
	
	public String getRank() { return rank; }
	
	public String getRemarks() { return remarks; }

	public Integer getCreatedById() { return createdById; }
	    
	public Date getDateCreated() { return dateCreated; }
	
	public Set<String> getVariantNames() { return variantNames; }
	    
	public void setId(Integer id) { this.id = id; }
	
	public void setParentId(Integer parentId) { this.parentId = parentId; }
	
	public void setName(String name) { this.name = name; }
	
	public void setVernacularName(String vernacularName) { this.vernacularName = vernacularName; }
	
	public void setDisplayQualifier(String displayQualifier) { this.displayQualifier = displayQualifier; }
	
	public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
	
	public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
	
	public void setRank(String rank) { this.rank = rank; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void setCreatedById(Integer createdById) { this.createdById = createdById; }
	    
	public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
	
	public void addVariantName(String variantName) { this.variantNames.add(variantName); }
}
