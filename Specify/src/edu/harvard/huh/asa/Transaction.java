package edu.harvard.huh.asa;

import java.util.Date;

public class Transaction extends AuditedObject
{
	// from st_lookup category 150
	public static enum TYPE             { Loan,    Borrow,   InExchange,          OutExchange,         InGift,          OutGift,         InSpecial,               OutSpecial,              Purchase,   StaffCollection,    OutMiscellaneous        };
	private static String[] TypeNames = { "loan", "borrow", "incoming exchange", "outgoing exchange", "incoming gift", "outgoing gift", "incoming special exch", "outgoing special exch", "purchase", "staff collection", "outgoing miscellaneous" };

	// from st_lookup category 152
	public static enum REQUEST_TYPE            {  Theirs,   Ours,   Unknown  };
	private static String[] RequestTypeNames = { "theirs", "ours", "unknown" };
	
	// from st_lookup category 151, 154
	public static enum PURPOSE             {  ForID,    Unrestricted,   ForStudy,    ForDnaSampling,     ForExhibition   };
	private static String[] PurposeNames = { "for id", "unrestricted", "for study", "for DNA sampling", "for exhibition" };
	
	// from st_lookup category 153
	public static enum USER_TYPE            {  Staff,   Student,   Visitor,   Unknown  };
	private static String[] UserTypeNames = { "staff", "student", "visitor", "unknown" };
		

    // config/common/picklist
    public enum ACCESSION_TYPE { Gift, Collection, Disposal, Exchange, FieldWork, Lost, Other, Purchase };
    
    public enum ROLE { Borrower, Benefactor, Collector, Contact, Contributor, Donor, Guest, Lender, Other, Preparer, Receiver, Reviewer, Sponsor, Staff, Student };
	
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

    public static String toString(REQUEST_TYPE requestType)
    {
        return RequestTypeNames[requestType.ordinal()];
    }
    
    public static String toString(PURPOSE purpose)
    {
        return purpose.name();
    }
    
    public static String toString(USER_TYPE userType)
    {
        return UserTypeNames[userType.ordinal()];
    }
   
    public static String toString(ACCESSION_TYPE accessionType)
    {
        return accessionType.name();
    }
    
    public static String toString(ROLE role)
    {
        return role.name();
    }
    
    /**
     * boxCount is an integer? [boxCount] boxes. : [boxCount]
     */
    public String getBoxCountNote() throws NumberFormatException
    {
        String boxCount = getBoxCount();
        
        if (boxCount != null)
        {
            try
            {
                int boxes = Integer.parseInt(boxCount);
                boxCount = boxCount + " box" + (boxes == 1 ? "" : "es");
            }
            catch (NumberFormatException e)
            {
                ;
            }
            return boxCount + ".";
        }
        return null;
    }
    
	private TYPE         type;
	private Integer      agentId;
	private String       localUnit;
	private REQUEST_TYPE requestType;
	private PURPOSE      purpose;
	private Integer      affiliateId;
	private USER_TYPE    userType;
	private Boolean      isAcknowledged;
	private Date         openDate;
	private Date         closeDate;
	private String       transactionNo;
	private String       forUseBy;
	private String       boxCount;
	private String       description;
	private String       remarks;
    
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
}
