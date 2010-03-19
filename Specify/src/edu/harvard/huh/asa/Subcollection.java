package edu.harvard.huh.asa;

public class Subcollection extends AuditedObject
{
	private  String collectionCode;
	private  String taxonGroup;
	private  String name;
	private  String author;
	private  String specimenCount;
	private  String location;
	private  String cabinet;
	private  String remarks;
	
	private static final int[] exsiccataeIds = { 3091, 3093, 3260, 3274, 3414, 3454, 3456, 3461, 3507, 3517, 3534, 3539, 3672 };
	
	public String getGuid() { return getId() + " subcollection"; }

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
	        if (getId() == exsiccataeIds[i]) return true;
	    }
	    return false;
	}

	public static boolean IsExsiccata(int subcollectionId)
	{
	       for (int i = 0; i < exsiccataeIds.length; i++)
	        {
	            if (subcollectionId == exsiccataeIds[i]) return true;
	        }
	        return false;
	}

    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }

    public void setTaxonGroup(String taxonGroup) { this.taxonGroup = taxonGroup; }
	
    public void setName(String name) { this.name = name; }
	
	public void setAuthor(String author) { this.author = author; }
	
	public void setSpecimenCount(String specimenCount) { this.specimenCount = specimenCount; }
	
	public void setLocation(String location) { this.location = location; }
	
	public void setCabinet(String cabinet) { this.cabinet = cabinet; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
}
