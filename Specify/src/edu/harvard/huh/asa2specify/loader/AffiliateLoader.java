package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Affiliate;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after OrganizationLoader and before AgentLoader.

public class AffiliateLoader extends AuditedObjectLoader
{
	private AffiliateLookup affiliateLookup;
	
    private BotanistLookup botanistLookup;
	private AsaIdMapper affiliates;

	static String getGuid(Integer affiliateId)
	{
		return affiliateId + " affiliate";
	}
    
	public AffiliateLoader(File csvFile,
	                       Statement specifySqlStatement,
	                       File affiliateBotanists,
	                       BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, specifySqlStatement);

		this.affiliates = new AsaIdMapper(affiliateBotanists);
		this.botanistLookup = botanistLookup;
	}

	public AffiliateLookup getAffiliateLookup()
	{
		if (affiliateLookup == null)
		{
			affiliateLookup = new AffiliateLookup() {
				public Agent getById(Integer affiliateId) throws LocalException
				{
					Agent agent = new Agent(); // TODO: this doesn't account for affiliate botanists
					
					Integer botanistId = getBotanistId(affiliateId);
					String guid = botanistId != null ? BotanistLoader.getGuid(botanistId) : AffiliateLoader.getGuid(affiliateId);
					
			        Integer agentId = getId("agent", "AgentID", "GUID", guid);

			        agent.setAgentId(agentId);
			        
			        return agent;
				}
			};
		}
		return affiliateLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Affiliate affiliate = parse(columns);

		Integer affiliateId = affiliate.getId();
		setCurrentRecordId(affiliateId);
		
		// convert affiliate into agent ...
        Agent agent = getAgent(affiliate);
                
        Integer botanistId = getBotanistId(affiliateId);

        if (botanistId != null) // retain botanist id in guid
        {
            Agent botanistAgent = lookup(botanistId);
            Integer agentId = botanistAgent.getId();
            agent.setAgentId(agentId);
            
        	if (agent.getRemarks() != null)
            {
        	    getLogger().warn(rec() + "Ignoring remarks: " + agent.getRemarks());
            }
        	
            String sql = getUpdateSql(agent, agentId);
            update(sql);
        }
        else
        {
            String sql = getInsertSql(agent);
            Integer agentId = insert(sql);
            agent.setAgentId(agentId);
        }

        Address address = getAddress(affiliate, agent);
        if (address != null)
		{
        	String sql = getInsertSql(address);
		    insert(sql);
		}
	}
    
	private Integer getBotanistId(Integer affiliateId)
	{
	    return affiliates.map(affiliateId);
	}
	
	private Agent lookup(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}

	private Affiliate parse(String[] columns) throws LocalException
	{
	    Affiliate affiliate = new Affiliate();
	    
	    int i = super.parse(columns, affiliate);
	    
		if (columns.length < i + 7)
		{
			throw new LocalException("Not enough columns");
		}
		
		try
		{
		    affiliate.setSurname(                         columns[i + 0] );
		    affiliate.setGivenName(                       columns[i + 1] );
		    affiliate.setPosition(                        columns[i + 2] );
		    affiliate.setPhone(                           columns[i + 3] );
		    affiliate.setEmail(                           columns[i + 4] );
		    affiliate.setAddress(                         columns[i + 5] );
		    affiliate.setRemarks( SqlUtils.iso8859toUtf8( columns[i + 6] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return affiliate;
	}

	private Agent getAgent(Affiliate affiliate) throws LocalException
	{
		Agent agent = new Agent();
		
		// AgentType
		agent.setAgentType(Agent.PERSON);
        
		// Division
		agent.setDivision(getBotanyDivision());
		  
        // Email
		String email = affiliate.getEmail();
		if (email != null)
		{
		    email = truncate(email, 50, "email");
			agent.setEmail(email);
		}
		
		// FirstName
		String firstName = affiliate.getGivenName();
		if (firstName != null)
		{
			firstName = truncate(firstName, 50, "first name");
			agent.setFirstName(firstName);
		}

		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		Integer affiliateId = affiliate.getId();
		checkNull(affiliateId, "id");
		
		String guid = AffiliateLoader.getGuid(affiliateId);
		agent.setGuid(guid);

		// JobTitle
		String position = affiliate.getPosition();
		if (position != null)
		{
			position = truncate(position, 50, "position");
			agent.setJobTitle(position);	
		}
		
		// LastName
		String lastName = affiliate.getSurname();
		checkNull(lastName, "lastName");
		lastName = truncate(lastName, 200, "last name");
		agent.setLastName(lastName);
		
        // Remarks
        String remarks = affiliate.getRemarks();
        agent.setRemarks(remarks);
        
        setAuditFields(affiliate, agent);

        return agent;
	}

	private Address getAddress(Affiliate affiliate, Agent agent) throws LocalException
	{
		Integer affiliateId = affiliate.getId();
		checkNull(affiliateId, "id");
		
        String phone = affiliate.getPhone();
        String addressString = affiliate.getAddress();
        
        if (phone == null && addressString == null) return null;
        
		Address address = new Address();
        
        // Phone
        if (phone != null)
        {
            phone = truncate(phone, 50, "phone number");
            address.setPhone1(phone);
        }

        // Address TODO: only 17 records have data, and it all needs post-import cleaning
        if (addressString != null)
        {
            addressString = truncate(addressString, 255, "address");
        }
        address.setAddress(addressString);
        
        // Agent
        address.setAgent(agent);
        
        // IsCurrent
        address.setIsCurrent(false);

        // IsPrimary
        address.setIsPrimary(false);

        // IsShipping
        address.setIsShipping(false);

        return address;
	}

	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "AgentType, CreatedByAgentID, DivisionID, Email, FirstName, " +
				            "GUID, JobTitle, LastName, ModifiedByAgentID, Remarks, " +
				            "TimestampCreated, TimestampModified, Version";

		String[] values = new String[13];

		values[0]  = SqlUtils.sqlString( agent.getAgentType());
        values[1]  = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[2]  = SqlUtils.sqlString( agent.getDivision().getId());
		values[3]  = SqlUtils.sqlString( agent.getEmail());
		values[4]  = SqlUtils.sqlString( agent.getFirstName());
		values[5]  = SqlUtils.sqlString( agent.getGuid());
		values[6]  = SqlUtils.sqlString( agent.getJobTitle());
		values[7]  = SqlUtils.sqlString( agent.getLastName());
		values[8]  = SqlUtils.sqlString( agent.getModifiedByAgent().getId());
		values[9]  = SqlUtils.sqlString( agent.getRemarks());
        values[10] = SqlUtils.sqlString( agent.getTimestampCreated());
        values[11] = SqlUtils.sqlString( agent.getTimestampModified());
        values[12] = SqlUtils.one();
        
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "Address, AgentID, IsCurrent, IsPrimary, IsShipping, Ordinal, " +
        		            "Phone1, TimestampCreated, Version";
        
        String[] values = new String[9];
        
        values[0] = SqlUtils.sqlString( address.getAddress());
        values[1] = SqlUtils.sqlString( address.getAgent().getId());
        values[2] = SqlUtils.sqlString( address.getIsCurrent());
        values[3] = SqlUtils.sqlString( address.getIsPrimary());
        values[4] = SqlUtils.sqlString( address.getIsShipping());
        values[5] = SqlUtils.addressOrdinal( address.getAgent().getId());
        values[6] = SqlUtils.sqlString( address.getPhone1());
        values[7] = SqlUtils.now();
        values[8] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
    
    private String getUpdateSql(Agent agent, Integer agentId) throws LocalException
    {
    	String[] fieldNames = { "Email", "JobTitle" };

        String[] values = new String[2];

        values[0] = SqlUtils.sqlString( agent.getEmail());
        values[1] = SqlUtils.sqlString( agent.getJobTitle());
        
        return SqlUtils.getUpdateSql("agent", fieldNames, values, "AgentID", agentId);
    }
}
