package edu.harvard.huh.asa;

import java.util.Date;

public class LoanItem
{
	private Integer id;
	private Integer loanId;
	private    Date returnDate;
	private Integer barcode;
	private  String transferredFrom;
	private  String transferredTo;
	private  String collection;
	private  String localUnit;
	
	public int getId() { return id; }
	
	public Integer getLoanId() { return loanId; }
	
	public Date getReturnDate() { return returnDate; }
	
	public Integer getBarcode() { return barcode; }
	
	public String getTransferredFrom() { return transferredFrom; }
	
	public String getTransferredTo() { return transferredTo; }
	
	public String getCollection() { return collection; }
	
	public String getLocalUnit() { return localUnit; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setLoanId(Integer loanId) { this.loanId = loanId; }
	
	public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
	
	public void setBarcode(Integer barcode) { this.barcode = barcode; }
	
	public void setTransferredFrom(String transferredFrom) { this.transferredFrom = transferredFrom; }
	
	public void setTransferredTo(String transferredTo) { this.transferredTo = transferredTo; }
	
	public void setCollection(String collection) { this.collection = collection; }
	
	public void setLocalUnit(String localUnit) { this.localUnit = localUnit; }
}
