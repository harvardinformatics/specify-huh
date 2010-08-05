package edu.harvard.huh.asa;

public class SpecimenItem extends AuditedObject
{	
	// TODO: normalize alignment of variable declarations
	
	public static enum REPRO_STATUS           { Fruit,    Flower,   FlowerAndFruit,     Sterile,   Sporophyte,   NotDetermined   };
	public static String[] ReproStatusNames = { "fruit", "flower", "flower and fruit", "sterile", "sporophyte", "not determined" };

	// this is for Specify data model PrepType
	public static enum FORMAT               { OnSheet,    InPacket,    InBox,    InBag,    InJar,    InSpiritMedium,     Wood,   Fossil,   OnMicroscopeSlide,     DNAsample,    Photograph,   Drawing,   ProtologOnSheet,     Other   };
	public static String[] FormatNames =    { "on sheet", "in packet", "in box", "in bag", "in jar", "in spirit medium", "wood", "Fossil", "on microscope slide", "DNA sample", "photograph", "drawing", "protolog on sheet", "other" };

    // this is for Specify data model PrepType
    public static enum CONTAINER_TYPE       { Sheet,   Packet,   Box,  Bag,   Jar,   MicroscopeSlide,     Slide35mm,     Self,   Other,   Logical,   Lot   };
    public static String[] ContainerNames = { "Sheet", "Packet", "Box","Bag", "Jar", "Microscope slide",  "35 mm slide", "Self", "Other", "Logical", "Lot" };

	// this is for data model Fragment PrepMethod
	public static enum PREP_METHOD            { Pressed,    Dried,   Wood,   Drawing,   SpiritMedium,    Protolog,   Photograph,   Glycerine,   Fossil,   DNAsample,    Other   };
	public static String[] PrepMethodNames =  { "Pressed",  "Dried", "Wood", "Drawing", "Spirit medium", "Protolog", "Photograph", "Glycerine", "Fossil", "DNA sample", "Other" };


	public static REPRO_STATUS parseReproStatus(String string) throws AsaException
	{
	    if (string == null) return null;
	    
	    for (REPRO_STATUS reproStatus : REPRO_STATUS.values())
	    {
	        if (ReproStatusNames[reproStatus.ordinal()].equals(string)) return reproStatus;
	    }
	    throw new AsaException("Invalid transaction type: " + string);
	}
	
	public static FORMAT parseFormat(String string) throws AsaException
    {
        if (string == null) return null;
        
        for (FORMAT format : FORMAT.values())
        {
            if (FormatNames[format.ordinal()].equals(string)) return format;
        }
        throw new AsaException("Invalid transaction type: " + string);
    }
	
	public static String toString(REPRO_STATUS reproStatus)
	{
		return reproStatus.name();
	}
	
	public static String toString(FORMAT format)
    {
        return FormatNames[format.ordinal()];
    }
	
	public static String toString(CONTAINER_TYPE containerType)
	{
	    return ContainerNames[containerType.ordinal()];
	}
	
	public static String toString(PREP_METHOD prepMethod)
	{
	    return PrepMethodNames[prepMethod.ordinal()];
	}
	
	private Integer      specimenId;
	private Integer      barcode;
	private Integer      collectorId;
	private String       collectorNo;
	private String       herbariumCode;
	private Integer      siteId;
	private Integer      seriesId;
	private String       seriesName;
	private String       seriesNo;
	private String       seriesAbbrev;
	private FORMAT       format;
	private Integer      itemNo;
	private String       remarks;        // specimen.remarks
	private String       provenance;
	private String       voucher;
	private Boolean      isOversize;
	private Boolean      isCultivated;
	private REPRO_STATUS reproStatus;
	private String       sex;
	private String       accessionNo;
	private String       accessionStatus;
	private String       note;           // specimen_item.note
	private String       reference;
	private String       description;
	private String       habitat;
	private String       substrate;
	private String       container;
	private Integer      subcollectionId;
	private BDate        collDate;
	private Integer      replicates;
	private String       location;
	private String       vernacularName;
	private String       distribution;
	
	private Boolean hasCollectingTrip;
	
	public Integer getSpecimenId() { return this.specimenId; }
	
	public Integer getBarcode() { return this.barcode; }
	
	public Integer getCollectorId() { return this.collectorId; }
	
	public String getCollectorNo() { return this.collectorNo; }

	public BDate getCollDate() { return this.collDate; }
	
	public String getHerbariumCode() { return this.herbariumCode; }
	
	public Integer getSiteId() { return this.siteId; }
	
	public FORMAT getFormat() { return this.format; }
	
	public Integer getItemNo() { return this.itemNo; }
	
	public String getRemarks() { return this.remarks; }
	
	public String getProvenance() { return this.provenance; }
	
	public String getVoucher() { return this.voucher; }
	
	public Boolean isOversize() { return this.isOversize; }

	public Boolean isCultivated() { return this.isCultivated; }
	
	public REPRO_STATUS getReproStatus() { return this.reproStatus; }
	
	public String getSex() { return this.sex; }
	
	public String getAccessionNo() { return this.accessionNo; }
	
	public String getAccessionStatus() { return this.accessionStatus; }
	
	public String getNote() { return this.note; }
	
	public String getReference() { return this.reference; }
	
	public String getDescription() { return this.description; }
	
	public String getHabitat() { return this.habitat; }
	
	public String getSubstrate() { return this.substrate; }
	
	public String getContainer() { return this.container; }
	
	public Integer getSeriesId() { return this.seriesId; }
	
	public String getSeriesName() { return this.seriesName; }
	
	public String getSeriesNo() { return this.seriesNo; }
	
	public String getSeriesAbbrev() { return this.seriesAbbrev; }
	
	public Integer getSubcollectionId() { return this.subcollectionId; }
	
	public Integer getReplicates() { return this.replicates; }
	
	public String getLocation() { return this.location; }
	
	public String getVernacularName() { return this.vernacularName; }
	
	public String getDistribution() { return this.distribution; }
	
	public boolean hasCollectingTrip()
	{
	    if (this.hasCollectingTrip ==  null)
	    {
	        this.hasCollectingTrip = isCollectingTrip(this.container);
	    }
	    
	    return this.hasCollectingTrip;
	}
	
	public void setSpecimenId(Integer specimenId) { this.specimenId = specimenId; }
	
	public void setBarcode(Integer barcode) { this.barcode = barcode; }
	
	public void setCollectorId(Integer collectorId) { this.collectorId = collectorId; }

	public void setCollectorNo(String collectorNo) { this.collectorNo = collectorNo; }
	
	public void setCollDate(BDate collDate) { this.collDate = collDate; }
	
	public void setHerbariumCode(String herbariumCode) { this.herbariumCode = herbariumCode; }

	public void setSiteId(Integer siteId) { this.siteId = siteId; }
	
	public void setFormat(FORMAT format) { this.format = format; }
	
	public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void setProvenance(String provenance) { this.provenance = provenance; }
	
	public void setVoucher(String voucher) { this.voucher = voucher; }
	
	public void setOversize(Boolean isOversize) { this.isOversize = isOversize; }
	
	public void setCultivated(Boolean isCultivated) { this.isCultivated = isCultivated; }
	
	public void setReproStatus(REPRO_STATUS reproStatus) { this.reproStatus = reproStatus; }
	
	public void setSex(String sex) { this.sex = sex; }
	
	public void setAccessionNo(String accessionNo) { this.accessionNo = accessionNo; }
	
	public void setAccessionStatus(String accessionStatus) { this.accessionStatus = accessionStatus; }
	   
	public void setNote(String note) { this.note = note; }
	
	public void setReference(String reference) { this.reference = reference; }
	
	public void setDescription(String description) { this.description = description; }
	
	public void setHabitat(String habitat) { this.habitat = habitat; }
	
	public void setSubstrate(String substrate) { this.substrate = substrate; }
	
	public void setContainer(String container) { this.container = container; }
	
	public void setSeriesId(Integer seriesId) { this.seriesId = seriesId; }
	
	public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
	
	public void setSeriesNo(String seriesNo) { this.seriesNo = seriesNo; }
	
	public void setSeriesAbbrev(String seriesAbbrev) { this.seriesAbbrev = seriesAbbrev; }
	
	public void setSubcollectionId(Integer subcollectionId) { this.subcollectionId = subcollectionId; }
	
	public void setReplicates(Integer replicates) { this.replicates = replicates; }
	
	public void setLocation(String location) { this.location = location; }
	
	public void setVernacularName(String vernacularName) { this.vernacularName = vernacularName; }
	
	public void setDistribution(String distribution) { this.distribution = distribution; }
	
    private boolean isCollectingTrip(String string)
    {
        if (string == null) return false;
        
        return string.toLowerCase().matches(".*(cruise|xpedit|xpedic|xplora|xcurs|iter|itinera|journey|survey).*");
    }
}
