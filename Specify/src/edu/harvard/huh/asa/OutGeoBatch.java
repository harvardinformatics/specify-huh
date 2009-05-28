package edu.harvard.huh.asa;

import edu.harvard.huh.asa.Transaction.TYPE;

public class OutGeoBatch
{	
	private Integer id;
	private Integer transactionId;
	private    TYPE type;
	private  String geoUnit;
	private Integer itemCount;
	private Integer typeCount;
	private Integer nonSpecimenCount;
	
	public Integer getId() { return id; }
	
	public Integer getTransactionId() { return transactionId; }
	
	public TYPE getTransactionType() { return type; }
	
	public String getGeoUnit() { return geoUnit; }
	
	public Integer getItemCount() { return itemCount; }
	
	public Integer getTypeCount() { return typeCount; }
	
	public Integer getNonSpecimenCount() { return nonSpecimenCount; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
	
	public void setGeoUnit(String geoUnit) { this.geoUnit = geoUnit; }
	
	public void setTransactionType(TYPE type) { this.type = type; }
	
	public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
	
	public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
	
	public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }
}
