package edu.harvard.huh.asa;

import java.util.Date;

import edu.harvard.huh.asa.Transaction.TYPE;

public class InReturnBatch
{
	private Integer id;
	private Integer transactionId;
	private TYPE type;
	private Integer itemCount;
	private String boxCount;
	private Boolean isAcknowledged;
	private Date actionDate;
	private String transferredTo;
	
	public Integer getId() { return id; }
	
	public Integer getTransactionId() { return transactionId; }
	
	public TYPE getTransactionType() { return type; }
	
	public Integer getItemCount() { return itemCount; }
	
	public String getBoxCount() { return boxCount; }
	
	public Boolean isAcknowledged() { return isAcknowledged; }
	
	public Date getActionDate() { return actionDate; }
	
	public String getTransferredTo() { return transferredTo; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
	
	public void setType(TYPE type) { this.type = type; }
	
	public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
	
	public void setBoxCount(String boxCount) { this.boxCount = boxCount; }
	
	public void setIsAcknowledged(Boolean isAcknowledged) { this.isAcknowledged = isAcknowledged; }
	
	public void setActionDate(Date actionDate) { this.actionDate = actionDate; }
	
	public void setTransferredTo(String transferredTo) { this.transferredTo = transferredTo; }
}