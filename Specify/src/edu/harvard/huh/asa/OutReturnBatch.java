package edu.harvard.huh.asa;

import java.util.Date;

import edu.harvard.huh.asa.AsaShipment.CARRIER;
import edu.harvard.huh.asa.AsaShipment.METHOD;
import edu.harvard.huh.asa.Transaction.TYPE;

public class OutReturnBatch
{
	private Integer id;
	private Integer transactionId;
	private String  collectionCode;
	private TYPE    type;
	private Integer itemCount;
	private Integer typeCount;
	private Integer nonSpecimenCount;
	private String  boxCount;
	private Boolean isAcknowledged;
	private Date    actionDate;
	private CARRIER carrier;
	private METHOD  method;
	private Float   cost;
	private Boolean isEstimatedCost;
	private String  note;
	
	public Integer getId() { return id; }
	
	public Integer getTransactionId() { return transactionId; }
	
	public String getCollectionCode() { return collectionCode; }
	
	public TYPE getTransactionType() { return type; }
	
	public Integer getItemCount() { return itemCount; }
	
	public Integer getTypeCount() { return typeCount; }
	
	public Integer getNonSpecimenCount() { return nonSpecimenCount; }
	
	public String getBoxCount() { return boxCount; }
	
	public Boolean isAcknowledged() { return isAcknowledged; }
	
	public Date getActionDate() { return actionDate; }

	public CARRIER getCarrier() { return carrier; }
    
    public METHOD getMethod() { return method; }
    
    public Float getCost() { return cost; }
    
    public Boolean isEstimatedCost() { return isEstimatedCost; }
	
	public String getNote() { return note; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
	
	public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
	
	public void setType(TYPE type) { this.type = type; }
	
	public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
	
	public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
	
	public void setBoxCount(String boxCount) { this.boxCount = boxCount; }
	
	public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }
	
	public void setIsAcknowledged(Boolean isAcknowledged) { this.isAcknowledged = isAcknowledged; }
	
	public void setActionDate(Date actionDate) { this.actionDate = actionDate; }
    
    public void setCarrier(CARRIER carrier) { this.carrier = carrier; }
    
    public void setMethod(METHOD method) { this.method = method; }
    
    public void setCost(Float cost) { this.cost = cost; }
    
    public void setIsEstimatedCost(Boolean isEstimatedCost) { this.isEstimatedCost = isEstimatedCost; }
	
	public void setNote(String note) { this.note = note; }
}
