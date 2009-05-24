package edu.harvard.huh.asa;

import java.util.Date;

public class Transaction
{
	// from st_lookup category 150
	public static enum TYPE             { Loan,    Borrow,   InExchange,          OutExchange,         InGift,          OutGift,         InSpecial,               OutSpecial,              Purchase,   StaffCollection   };
	private static String[] TypeNames = { "loan", "borrow", "incoming exchange", "outgoing exchange", "incoming gift", "outgoing gift", "incoming special exch", "outgoing special exch", "purchase", "staff collection" };

	// from st_lookup category...
	public static enum REQUEST_TYPE { };
	private static String[] RequestTypeNames = { };
	
	// from st_lookup category...
	public static enum PURPOSE { };
	private static String[] PurposeNames = { };
	
	// from st_lookup category...
	public static enum USER_TYPE { };
	private static String[] UserTypeNames = { };
		
	public static TYPE parseType(String string) throws AsaException
	{
	    for (TYPE type : TYPE.values())
	    {
	        if (TypeNames[type.ordinal()].equals(string)) return type;
	    }
	    throw new AsaException("Invalid transaction type: " + string);
	}
	
	public static REQUEST_TYPE parseRequestType(String string) throws AsaException
	{
	    for (REQUEST_TYPE requestType : REQUEST_TYPE.values())
	    {
	        if (RequestTypeNames[requestType.ordinal()].equals(string)) return requestType;
	    }
	    throw new AsaException("Invalid transaction request type: " + string);
	}
	
	public static PURPOSE parsePurpose(String string) throws AsaException
	{
	    for (PURPOSE purpose : PURPOSE.values())
	    {
	        if (PurposeNames[purpose.ordinal()].equals(string)) return purpose;
	    }
	    throw new AsaException("Invalid transaction purpose: " + string);
	}
	
	public static USER_TYPE parseUserType(String string) throws AsaException
	{
	    for (USER_TYPE userType : USER_TYPE.values())
	    {
	        if (UserTypeNames[userType.ordinal()].equals(string)) return userType;
	    }
	    throw new AsaException("Invalid transaction user type: " + string);
	}
	
    public static String toString(TYPE type)
    {
        return TypeNames[type.ordinal()];
    }

    public static String toString(REQUEST_TYPE type)
    {
        return RequestTypeNames[type.ordinal()];
    }
    
    public static String toString(PURPOSE purpose)
    {
        return PurposeNames[purpose.ordinal()];
    }
    
    public static String toString(USER_TYPE userType)
    {
        return UserTypeNames[userType.ordinal()];
    }
    
    private       Integer id;
	private          TYPE type;
	private       Integer agentId;
	private        String localUnit;
	private  REQUEST_TYPE requestType;
	private       PURPOSE purpose;
	private       Integer affiliateId;
	private     USER_TYPE userType;
	private       Boolean isAcknowledged;
	private          Date openDate;
	private          Date closeDate;
	private        String transactionNo;
	private        String forUseBy;
	private        String boxCount;
	private        String description;
	private        String remarks;
	private       Integer createdById;
	private          Date dateCreated;
	private          Date originalDueDate;
	private          Date currentDueDate;
	
	public Integer getId() { return id; }
	
	public TYPE getType() { return type; }
	
	public Integer getAgentId() { return agentId; }
	
	public String getLocalUnit() { return localUnit; }
	
	public REQUEST_TYPE getRequestType() { return requestType; }
	
	public PURPOSE getPurpose() { return purpose; }
	
	public Integer getAffiliateId() { return affiliateId; }
	
	public USER_TYPE getUserType() { return userType; }
	
	public Boolean isAcknowledged() { return isAcknowledged; }
	
	public Date getOpenDate() { return openDate; }
	
	public Date getCloseDate() { return closeDate; }
	
	public String getTransactionNo() { return transactionNo; }
	
	public String getForUseBy() { return forUseBy; }
	
	public String getBoxCount() { return boxCount; }
	
	public String getDescription() { return description; }
	
	public String getRemarks() { return remarks; }
	
	public Integer getCreatedById() { return createdById; }

	public Date getDateCreated() { return dateCreated; }
	
	public Date getOriginalDueDate() { return originalDueDate; }
	
	public Date getCurrentDueDate() { return currentDueDate; }
		
	public void setId(Integer id) { this.id = id; }
	
	public void setType(TYPE type) { this.type = type; }
	
	public void setAgentId(Integer agentId) { this.agentId = agentId; }
	
	public void setLocalUnit(String localUnit) { this.localUnit = localUnit; }
	
	public void setRequestType(REQUEST_TYPE requestType) { this.requestType = requestType; }
	
	public void setPurpose(PURPOSE purpose) { this.purpose = purpose; }
	
	public void setAffiliateId(Integer affiliateId) { this.affiliateId = affiliateId; }
	
	public void setUserType(USER_TYPE userType) { this.userType = userType; }
	
	public void setIsAcknowledged(Boolean isAcknowledged) { this.isAcknowledged = isAcknowledged; }
	
	public void setOpenDate(Date openDate) { this.openDate = openDate; }
	
	public void setCloseDate(Date closeDate) { this.closeDate = closeDate; }
	
	public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
	
	public void setForUseBy(String forUseBy) { this.forUseBy = forUseBy; }
	
	public void setBoxCount(String boxCount) { this.boxCount = boxCount; }
	
	public void setDescription(String description) { this.description = description; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
    
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
    
    public void setOriginalDueDate(Date originalDueDate) { this.originalDueDate = originalDueDate; }
    
    public void setCurrentDueDate(Date currentDueDate) { this.currentDueDate = currentDueDate; }
}
