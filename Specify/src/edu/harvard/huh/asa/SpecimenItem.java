package edu.harvard.huh.asa;

public class SpecimenItem {
	
	private Integer id;
	private Integer barcode;
	private Integer botanistId;
	private  String collectorNo;
	
	private BDate collDate;
	
	public SpecimenItem() {
	    ;
	}
	
	public Integer getId() { return this.id; }
	
	public Integer getBarcode() { return this.barcode; }
	
	public Integer getBotanistId() { return this.botanistId; }
	
	public String getCollectorNo() { return this.collectorNo; }

	public BDate getCollDate() { return this.collDate; }

	public void setId(Integer id) { this.id = id; }
	
	public void setBarcode(Integer barcode) { this.barcode = barcode; }
	
	public void setBotanistId(Integer botanistId) { this.botanistId = botanistId; }

	public void setCollectorNo(String collectorNo) { this.collectorNo = collectorNo; }
	
	public void setCollDate(BDate collDate) { this.collDate = collDate; }

}
