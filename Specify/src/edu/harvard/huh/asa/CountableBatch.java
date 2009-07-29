package edu.harvard.huh.asa;

public class CountableBatch 
{
    private Integer id;
    private Integer transactionId;
    private String  collectionCode;
    private Integer itemCount;
    private Integer typeCount;
    private Integer nonSpecimenCount;
	
    public Integer getId() { return id; }
    
    public Integer getTransactionId() { return transactionId; }
    
    public String getCollectionCode() { return collectionCode; }
    
    public Integer getItemCount() { return itemCount; }
    
    public Integer getTypeCount() { return typeCount; }
    
    public Integer getNonSpecimenCount() { return nonSpecimenCount; }

    public void setId(Integer id) { this.id = id; }
    
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
    
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
    
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    
    public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
    
    public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }

    /**
     * itemCount + typeCount + nonSpecimenCount
     */
    public short getBatchQuantity()
    {
        Integer itemCount = this.getItemCount();
        Integer typeCount = this.getTypeCount();
        Integer nonSpecimenCount = this.getNonSpecimenCount();

        if (itemCount == null) itemCount = 0;
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        return (short) (itemCount + typeCount + nonSpecimenCount);
    }
    
    /**
     * "Quantity contains [nonSpecimenCount] non-specimens and [typeCount] types."
     */
    public String getItemCountNote()
    {
        Integer typeCount = this.getTypeCount();
        Integer nonSpecimenCount = this.getNonSpecimenCount();
        
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        String nonSpecimenNote = nonSpecimenCount + " non-specimen" + (nonSpecimenCount == 1 ? "" : "s");
        String typeNote = typeCount + " type" + (typeCount == 1 ? "" : "s");
        
        return "Quantity contains " + nonSpecimenNote + " and " + typeNote + ".";
    }

}
