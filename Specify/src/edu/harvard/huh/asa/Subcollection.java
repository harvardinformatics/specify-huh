package edu.harvard.huh.asa;

import java.util.Date;

public class Subcollection
{
	private Integer id;
	private  String collectionCode;
	private  String taxonGroup;
	private  String name;
	private  String author;
	private  String specimenCount;
	private  String location;
	private  String cabinet;
	private  String remarks;
	private Integer createdById;
	private    Date dateCreated;
	
	private int[] exsiccataeIds = { 3091, 3093, 3260, 3274, 3414, 3454, 3456, 3461, 3507, 3517, 3534, 3539 };
	
	public Integer getId() { return id; }
	
	public String getGuid() { return id + " subcollection"; }

	public String getCollectionCode() { return collectionCode; }

	public String getTaxonGroup() { return taxonGroup; }

	public String getName() { return name; }
	
	public String getAuthor() { return author; }

	public String getSpecimenCount() { return specimenCount; }

	public String getLocation() { return location; }
	
	public String getCabinet() { return cabinet; }
	
	public String getRemarks() { return remarks; }
	
	public boolean isExsiccata()
	{
	    for (int i = 0; i < exsiccataeIds.length; i++)
	    {
	        if (id == exsiccataeIds[i]) return true;
	    }
	    return false;
	}

	public Integer getCreatedById() { return createdById; }

	public Date getDateCreated() { return dateCreated; }

	public void setId(Integer id) { this.id = id; }

    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }

    public void setTaxonGroup(String taxonGroup) { this.taxonGroup = taxonGroup; }
	
    public void setName(String name) { this.name = name; }
	
	public void setAuthor(String author) { this.author = author; }
	
	public void setSpecimenCount(String specimenCount) { this.specimenCount = specimenCount; }
	
	public void setLocation(String location) { this.location = location; }
	
	public void setCabinet(String cabinet) { this.cabinet = cabinet; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }

    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
    
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
