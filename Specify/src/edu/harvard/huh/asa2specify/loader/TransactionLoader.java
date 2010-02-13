package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.REQUEST_TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after AffiliateLoader, AgentLoader, and OrganizationLoader
public abstract class TransactionLoader extends AuditedObjectLoader
{
    //       in_geo_batch     -> in.gift/in.exch/sp.exch/staff.coll/purchase
    //       out_geo_batch    -> out.gift/out.exch
    //       in_return_batch  -> loan
    //       out_return_batch -> borrow
    //       shipment         -> out.gift/out.misc/out.exch/loan
    //       loan_item        -> loan
    //       taxon_batch      -> borrow/loan
    //       due_date         -> borrow/loan

    // TODO: get list of conditions to raise warnings and exceptions

    protected static final String DEFAULT_ACCESSION_NUMBER   = "none";
    
	private static BotanistLookup BotanistLookup;
	private static AgentLookup AgentLookup;
	private static AffiliateLookup AffiliateLookup;
	private static OrganizationLookup OrganizationLookup;
	
	public TransactionLoader(File csvFile,
	                         Statement sqlStatement,
	                         BotanistLookup botanistLookup,
	                         AffiliateLookup affiliateLookup,
	                         AgentLookup agentLookup,
	                         OrganizationLookup organizationLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		if (BotanistLookup == null) BotanistLookup  = botanistLookup;
		if (AgentLookup == null) AgentLookup = agentLookup;
		if (AffiliateLookup == null) AffiliateLookup = affiliateLookup;
		if (OrganizationLookup == null) OrganizationLookup = organizationLookup;
	}

	protected TransactionLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
	    super(csvFile, sqlStatement);
	}

	protected int parse(String[] columns, Transaction transaction) throws LocalException
	{
    	if (columns.length < 21)
    	{
    		throw new LocalException("Not enough columns");
    	}
		
		try
		{
		    transaction.setId(                     SqlUtils.parseInt( columns[0]  ));
		    transaction.setCreatedById(             Integer.parseInt( columns[1]  ));
		    transaction.setCreateDate(            SqlUtils.parseDate( columns[2]  ));
		    transaction.setUpdatedById(             Integer.parseInt( columns[3]  ));
		    transaction.setUpdateDate(            SqlUtils.parseDate( columns[4]  ));
			transaction.setType(               Transaction.parseType( columns[5]  ));
			transaction.setAgentId(                SqlUtils.parseInt( columns[6]  ));
			transaction.setOrganizationId(         SqlUtils.parseInt( columns[7]  ));
			transaction.setLocalUnit(                                 columns[8]  );
			transaction.setRequestType( Transaction.parseRequestType( columns[9]  ));
			transaction.setPurpose(         Transaction.parsePurpose( columns[10]  ));
			transaction.setAffiliateId(            SqlUtils.parseInt( columns[11] ));
			transaction.setUserType(       Transaction.parseUserType( columns[12] ));
			transaction.setIsAcknowledged(      Boolean.parseBoolean( columns[13] ));
			transaction.setOpenDate(              SqlUtils.parseDate( columns[14] ));
			transaction.setCloseDate(             SqlUtils.parseDate( columns[15] ));			
			transaction.setTransactionNo(                             columns[16] );
			transaction.setForUseBy(                                  columns[17] );
			transaction.setBoxCount(                                  columns[18] );
			transaction.setDescription(                               columns[19] );
			transaction.setRemarks(                                   columns[20] );
        
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return 20; // index of next column
	}
     
    protected Agent lookupAgent(Transaction transaction) throws LocalException
    {
        Integer asaAgentId = transaction.getAgentId();
        
        if (asaAgentId == null) return null;
        
        return AgentLookup.getById(asaAgentId);
    }

    protected Agent lookupAffiliate(Transaction transaction) throws LocalException
    {
        Integer affiliateId = transaction.getAffiliateId();
        
        if (affiliateId == null) return null;
        
        else return AffiliateLookup.getById(affiliateId);
    }
    
    protected Agent lookupOrganization(Transaction transaction) throws LocalException
    {
        Integer organizationId = transaction.getOrganizationId();
        
        if (organizationId == null) return null;
        
        else return OrganizationLookup.getById(organizationId);
    }

    /**
     * (user: [for_use_by], type: [user_type], purpose: [purpose])
     */
    protected String getUsage(Transaction transaction)
    {
        String user = transaction.getForUseBy();
        if (user == null) user = "?";
        
        String userType = Transaction.toString(transaction.getUserType());
        
        String purpose  = Transaction.toString(transaction.getPurpose());
        
        return "(user: " + user + ", " + "type: " + userType + ", " + "purpose: " + purpose + ")";
    }

    /**
     * description + [boxCount]
     * @param description
     * @param boxCount
     * @return
     */
    protected String getDescriptionOfMaterial(Transaction transaction)
    {
        String boxCount = transaction.getBoxCount();
        String description = transaction.getDescription();
        
        if (boxCount == null) return description;

        boxCount = "[box count: " + boxCount + "]";
   
        if (description == null) return boxCount;
        
        return description + " " + boxCount;
    }
    
    protected Boolean isTheirs(REQUEST_TYPE requestType)
    {
        if (requestType.equals(REQUEST_TYPE.Theirs)) return true;
        else if (requestType.equals(REQUEST_TYPE.Ours)) return false;
        else return null;
    }
}
