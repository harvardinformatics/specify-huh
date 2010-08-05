package edu.harvard.huh.asa;

import edu.harvard.huh.asa.Transaction.TYPE;

public class TaxonBatch
{	
	private Integer id;
	private Integer transactionId;
	private String  collectionCode;
	private TYPE    type;
	private String  higherTaxon;
	private Integer itemCount;
	private Integer typeCount;
	private Integer nonSpecimenCount;
	private String  taxon;
	private String  transferredFrom;
	private Integer qtyReturned;
	
	public Integer getId() { return id; }
	
	public Integer getTransactionId() { return transactionId; }
	
	public String getCollectionCode() { return collectionCode; }
	
	public TYPE getType() { return type; }
	
	public String getHigherTaxon() { return higherTaxon; }
	
	public Integer getItemCount() { return itemCount; }
	
	public Integer getTypeCount() { return typeCount; }
	
	public Integer getNonSpecimenCount() { return nonSpecimenCount; }
	
	public String getTaxon() { return taxon; }
	
	public Integer getQtyReturned() { return qtyReturned; }
	
	public String getTransferredFrom() { return transferredFrom; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
	
	public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
	
	public void setType(TYPE type) { this.type = type; }
	
	public void setHigherTaxon(String higherTaxon) { this.higherTaxon = higherTaxon; }
	
	public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
	
	public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
	
	public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }
	
	public void setTaxon(String taxon) { this.taxon = taxon; }
	
	public void setTransferredFrom(String transferredFrom) { this.transferredFrom = transferredFrom; }
	
	public void setQtyReturned(Integer qtyReturned) { this.qtyReturned = qtyReturned; }
}
