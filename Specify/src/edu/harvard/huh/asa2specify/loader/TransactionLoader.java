package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AsaAgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
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
    
    // TODO: go through all the xAgents to determine if the affiliates/asaAgents match their assigned roles.

    // TODO: go through all Text1 fields to determine if there's a more appropriate field to put description into
    
    // TODO: go through all the date fields to determine if open/close dates have been appropriately applied
    
    // TODO: get list of conditions to raise warnings and exceptions
    
    // TODO: assign userType
    
    // TODO: assign boxCount
    
    protected static final String DEFAULT_ACCESSION_NUMBER   = "none";
	
	// config/common/picklist
	protected enum ROLE { borrower, benefactor, collector, contributor, donor, guest, lender, other, preparer, receiver, reviewer, sponsor, staff, student };
	
	protected enum ACCESSION_TYPE { gift, cln, disposal, field_work, lost, other, purchase };
	
	private AsaIdMapper botanistsByAffiliate;
	private AsaIdMapper botanistsByAgent;
	private BotanistLookup botanistLookup;
	private AsaAgentLookup agentLookup;
	private AffiliateLookup affiliateLookup;
	
	public TransactionLoader(File csvFile,
	                         Statement sqlStatement,
	                         File affiliateBotanists,
	                         File agentBotanists,
	                         BotanistLookup botanistLookup,
	                         AsaAgentLookup agentLookup,
	                         AffiliateLookup affiliateLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.botanistsByAffiliate = new AsaIdMapper(affiliateBotanists);
		this.botanistsByAgent     = new AsaIdMapper(agentBotanists);
		
		this.botanistLookup  = botanistLookup;
		this.agentLookup     = agentLookup;
		this.affiliateLookup = affiliateLookup;
	}

	protected int parse(String[] columns, Transaction transaction) throws LocalException
	{
    	if (columns.length < 22)
    	{
    		throw new LocalException("Wrong number of columns");
    	}
		
		try
		{
			transaction.setId(                     SqlUtils.parseInt( columns[0]  ));
			transaction.setType(               Transaction.parseType( columns[1]  ));
			transaction.setAgentId(                SqlUtils.parseInt( columns[2]  ));
			transaction.setLocalUnit(                                 columns[3]  );
			transaction.setRequestType( Transaction.parseRequestType( columns[4]  ));
			transaction.setPurpose(         Transaction.parsePurpose( columns[5]  ));
			transaction.setAffiliateId(            SqlUtils.parseInt( columns[6]  ));
			transaction.setUserType(       Transaction.parseUserType( columns[7]  ));
			transaction.setIsAcknowledged(      Boolean.parseBoolean( columns[8]  ));
			transaction.setOpenDate(              SqlUtils.parseDate( columns[9]  ));
			transaction.setCloseDate(             SqlUtils.parseDate( columns[10] ));			
			transaction.setTransactionNo(                             columns[11] );
			transaction.setForUseBy(                                  columns[12] );
			transaction.setBoxCount(                                  columns[13] );
			transaction.setDescription(                               columns[14] );
			transaction.setRemarks(                                   columns[15] );
			transaction.setCreatedById(             Integer.parseInt( columns[16] ));
			transaction.setDateCreated(           SqlUtils.parseDate( columns[17] ));        
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return 18; // index of next column
	}

	protected Agent getAffiliateAgent(Transaction transaction) throws LocalException
	{
        Agent agent = null;
        
	    Integer affiliateId = transaction.getAffiliateId();

	    if (affiliateId != null)
	    {
	    	Integer botanistId = getBotanistIdByAffiliateId(affiliateId);
	    	if (botanistId != null)
	    	{
	    		agent = lookupBotanist(botanistId);
	    	}
	    	else
	    	{
	    		agent = lookupAffiliate(affiliateId);
	    	}
	    }
	    
        return agent;
	}
	
	protected Agent getAsaAgentAgent(Transaction transaction) throws LocalException
	{
	    Agent agent = new Agent();
	    
	    Integer asaAgentId = transaction.getAgentId();

	    if (asaAgentId != null)
	    {
	        Integer botanistId = getBotanistIdByAgentId(asaAgentId);
	        if (botanistId != null)
	        {
	        	agent = lookupBotanist(botanistId);
	        }
	        else
	        {
	        	agent = lookupAgent(asaAgentId);
	        }
	    }
	    return agent;
	}
    
    private Agent lookupBotanist(Integer botanistId) throws LocalException
    {
        return botanistLookup.getByBotanistId(botanistId);
    }
       
    private Agent lookupAgent(Integer asaAgentId) throws LocalException
    {
        return agentLookup.getByAsaAgentId(asaAgentId);
    }

    private Agent lookupAffiliate(Integer affiliateId) throws LocalException
    {
        return affiliateLookup.getByAffiliateId(affiliateId);
    }
    
    private Integer getBotanistIdByAffiliateId(Integer affiliateId)
    {
        return botanistsByAffiliate.map(affiliateId);
    }
    
    private Integer getBotanistIdByAgentId(Integer agentId)
    {
        return botanistsByAgent.map(agentId);
    }
}
