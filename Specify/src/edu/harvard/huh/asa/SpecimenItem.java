package edu.harvard.huh.asa;

import java.util.Calendar;

public class SpecimenItem {
	
	private  Integer id;
	private  Integer specimenId;
	private  Integer barcode;
	private  Integer botanistId;
	private   String collectorNo;
	private   String herbariumCode;
	private  Integer siteId;
	private  Integer seriesId;
	private   String seriesNo;
	private   String format;
	private  Integer itemNo;
	private   String remarks; // specimen.remarks
	private   String provenance;
	private   String voucher;
	private  Boolean isOversize;
	private  Boolean isCultivated;
	private   String reproStatus;
	private   String sex;
	private   String accessionNo;
	private   String note; // specimen_item.note
	private   String reference;
	private   String description;
	private   String habitat;
	private   String substrate;
	private Calendar catalogedDate;
	private   String createdBy;	
	private    BDate collDate;
	
	public SpecimenItem() {
	    ;
	}
	
	public Integer getId() { return this.id; }
	
	public Integer getSpecimenId() { return this.specimenId; }
	
	public Integer getBarcode() { return this.barcode; }
	
	public Integer getBotanistId() { return this.botanistId; }
	
	public String getCollectorNo() { return this.collectorNo; }

	public BDate getCollDate() { return this.collDate; }
	
	public String getHerbariumCode() { return this.herbariumCode; }
	
	public Integer getSiteId() { return this.siteId; }
	
	public String getFormat() { return this.format; }
	
	public Integer getItemNo() { return this.itemNo; }
	
	public String getRemarks() { return this.remarks; }
	
	public String getProvenance() { return this.provenance; }
	
	public String getVoucher() { return this.voucher; }
	
	public Boolean isOversize() { return this.isOversize; }

	public Boolean isCultivated() { return this.isCultivated; }
	
	public String getReproStatus() { return this.reproStatus; }
	
	public String getSex() { return this.sex; }
	
	public String getAccessionNo() { return this.accessionNo; }
	
	public String getNote() { return this.note; }
	
	public String getReference() { return this.reference; }
	
	public String getDescription() { return this.description; }
	
	public String getHabitat() { return this.habitat; }
	
	public String getSubstrate() { return this.substrate; }
	
	public Calendar getCatalogedDate() { return this.catalogedDate; }
	
	public Integer getSeriesId() { return this.seriesId; }
	
	public String getSeriesNo() { return this.seriesNo; }
	
	public String getCreatedBy() { return this.createdBy; }

	public void setId(Integer id) { this.id = id; }
	
	public void setSpecimenId(Integer specimenId) { this.specimenId = specimenId; }
	
	public void setBarcode(Integer barcode) { this.barcode = barcode; }
	
	public void setBotanistId(Integer botanistId) { this.botanistId = botanistId; }

	public void setCollectorNo(String collectorNo) { this.collectorNo = collectorNo; }
	
	public void setCollDate(BDate collDate) { this.collDate = collDate; }
	
	public void setHerbariumCode(String herbariumCode) { this.herbariumCode = herbariumCode; }

	public void setSiteId(Integer siteId) { this.siteId = siteId; }
	
	public void setFormat(String format) { this.format = format; }
	
	public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void setProvenance(String provenance) { this.provenance = provenance; }
	
	public void setVoucher(String voucher) { this.voucher = voucher; }
	
	public void setOversize(Boolean isOversize) { this.isOversize = isOversize; }
	
	public void setCultivated(Boolean isCultivated) { this.isCultivated = isCultivated; }
	
	public void setReproStatus(String reproStatus) { this.reproStatus = reproStatus; }
	
	public void setSex(String sex) { this.sex = sex; }
	
	public void setAccessionNo(String accessionNo) { this.accessionNo = accessionNo; }
	
	public void setNote(String note) { this.note = note; }
	
	public void setReference(String reference) { this.reference = reference; }
	
	public void setDescription(String description) { this.description = description; }
	
	public void setHabitat(String habitat) { this.habitat = habitat; }
	
	public void setSubstrate(String substrate) { this.substrate = substrate; }
	
	public void setCatalogedDate(Calendar catalogedDate) { this.catalogedDate = catalogedDate; }
	
	public void setSeriesId(Integer seriesId) { this.seriesId = seriesId; }
	
	public void setSeriesNo(String seriesNo) { this.seriesNo = seriesNo; }
	
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
