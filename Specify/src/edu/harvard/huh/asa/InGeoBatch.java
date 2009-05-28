package edu.harvard.huh.asa;

import edu.harvard.huh.asa.Transaction.TYPE;

public class InGeoBatch
{
	private Integer id;
	private Integer transactionId;
	private    TYPE transactionType;
	private  String geoUnit;
	private Integer itemCount;
	private Integer typeCount;
	private Integer nonSpecimenCount;
	private Integer discardCount;
	private Integer returnCount;
	private   Float cost;
	
	public Integer getId() { return id; }
	
	public Integer getTransactionId() { return transactionId; }
	
	public TYPE getTransactionType() { return transactionType; }
	
	public String getGeoUnit() { return geoUnit; }
	
	public Integer getItemCount() { return itemCount; }
	
	public Integer getTypeCount() { return typeCount; }
	
	public Integer getNonSpecimenCount() { return nonSpecimenCount; }
	
	public Integer getDiscardCount() { return discardCount; }
	
	public Integer getReturnCount() { return returnCount; }
	
	public Float getCost() { return cost; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
	
	public void setGeoUnit(String geoUnit) { this.geoUnit = geoUnit; }
	
	public void setType(TYPE type) { this.transactionType = type; }
	
	public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
	
	public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
	
	public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }
	
	public void setDiscardCount(Integer discardCount) { this.discardCount = discardCount; }
	
	public void setReturnCount(Integer returnCount) { this.returnCount = returnCount; }
	
	public void setCost(Float cost) { this.cost = cost; }
}
