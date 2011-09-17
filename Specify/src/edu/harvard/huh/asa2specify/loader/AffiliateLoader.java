package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Affiliate;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after OrganizationLoader and before AgentLoader.

public class AffiliateLoader extends AuditedObjectLoader
{
	private AffiliateLookup affiliateLookup;
	
	static String getGuid(Integer affiliateId)
	{
		return affiliateId + " affiliate";
	}
    
	public AffiliateLoader(File csvFile, Statement specifySqlStatement) throws LocalException
	{
		super(csvFile, specifySqlStatement);
	}

	public AffiliateLookup getAffiliateLookup()
	{
		if (affiliateLookup == null)
		{
			affiliateLookup = new AffiliateLookup() {
				public Agent getById(Integer affiliateId) throws LocalException
				{
					Agent agent = new Agent();
					
					String guid = getGuid(affiliateId);
					
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
                
        String sql = getInsertSql(agent);
        Integer agentId = insert(sql);
        agent.setAgentId(agentId);

        Address address = getAddress(affiliate, agent);
        if (address != null)
		{
        	sql = getInsertSql(address);
		    insert(sql);
		}
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
		
		// MiddleInitial
		agent.setMiddleInitial(AgentType.affiliate.name());
		
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

		String[] values = {
				SqlUtils.sqlString( agent.getAgentType()),
				SqlUtils.sqlString( agent.getCreatedByAgent().getId()),
				SqlUtils.sqlString( agent.getDivision().getId()),
				SqlUtils.sqlString( agent.getEmail()),
				SqlUtils.sqlString( agent.getFirstName()),
				SqlUtils.sqlString( agent.getGuid()),
				SqlUtils.sqlString( agent.getJobTitle()),
				SqlUtils.sqlString( agent.getLastName()),
				SqlUtils.sqlString( agent.getModifiedByAgent().getId()),
				SqlUtils.sqlString( agent.getRemarks()),
				SqlUtils.sqlString( agent.getTimestampCreated()),
				SqlUtils.sqlString( agent.getTimestampModified()),
				SqlUtils.one()
		};
        
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "Address, AgentID, IsCurrent, IsPrimary, IsShipping, Ordinal, " +
        		            "Phone1, TimestampCreated, Version";
        
        String[] values = {
        		SqlUtils.sqlString( address.getAddress()),
        		SqlUtils.sqlString( address.getAgent().getId()),
        		SqlUtils.sqlString( address.getIsCurrent()),
        		SqlUtils.sqlString( address.getIsPrimary()),
        		SqlUtils.sqlString( address.getIsShipping()),
        		SqlUtils.addressOrdinal( address.getAgent().getId()),
        		SqlUtils.sqlString( address.getPhone1()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
