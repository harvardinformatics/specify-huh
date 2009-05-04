package edu.harvard.huh.asa;

public class GeoUnit {

	private Integer id;
	private Integer parentId;
	private  String name;
	private  String abbreviation;
	private  String isoCode;
	private  String vernacularName;
	private  String qualifiedName;
	private  String rank;
	private  String remarks;
	
	public Integer getId() { return id; }
	
	public Integer getParentId() { return parentId; }
	
	public String getName() { return name; }
	
	public String getAbbreviation() { return abbreviation; }
	
	public String getIsoCode() { return isoCode; }
	
	public String getVernacularName() { return vernacularName; }
	
	public String getQualifiedName() { return qualifiedName; }
	
	public String getRank() { return rank; }
	
	public String getRemarks() { return remarks; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setParentId(Integer parentId) { this.parentId = parentId; }
	
	public void setName(String name) { this.name = name; }
	
	public void setVernacularName(String vernacularName) { this.vernacularName = vernacularName; }
	
	public void setQualifiedname(String qualifiedName) { this.qualifiedName = qualifiedName; }
	
	public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
	
	public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
	
	public void setRank(String rank) { this.rank = rank; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
}
