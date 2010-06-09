package edu.harvard.huh.asa;

public class MichaelaSubcollection
{
    private Integer id;
    private boolean isExsiccata;
	private String  taxonGroup;
	private String  name;
	private String  author;
	private String  specimenCount;
	private String  location;
	private String  cabinet;
	
	public Integer getId() { return id; }

	public boolean isExsiccata() { return isExsiccata; }
	
	public String getTaxonGroup() { return taxonGroup; }

	public String getName() { return name; }
	
	public String getAuthor() { return author; }

	public String getSpecimenCount() { return specimenCount; }

	public String getLocation() { return location; }
	
	public String getCabinet() { return cabinet; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setExsiccata(boolean isExsiccata) { this.isExsiccata = isExsiccata; }
	
    public void setTaxonGroup(String taxonGroup) { this.taxonGroup = taxonGroup; }
	
    public void setName(String name) { this.name = name; }
	
	public void setAuthor(String author) { this.author = author; }
	
	public void setSpecimenCount(String specimenCount) { this.specimenCount = specimenCount; }
	
	public void setLocation(String location) { this.location = location; }
	
	public void setCabinet(String cabinet) { this.cabinet = cabinet; }	
}
