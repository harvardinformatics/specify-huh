package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import edu.harvard.huh.asa.Affiliate;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Division;

// Run this class after OrganizationLoader and before AgentLoader.

public class AffiliateLoader extends AuditedObjectLoader
{
    private BotanistLookup botanistLookup;
    
	private Division division;
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
		
		this.division = getBotanyDivision();
		this.affiliates = new AsaIdMapper(affiliateBotanists);
		this.botanistLookup = botanistLookup;
	}

	private Agent lookup(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getByBotanistId(botanistId);
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
                warn("Ignoring remarks", agent.getRemarks());
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

	private Affiliate parse(String[] columns) throws LocalException
	{
		if (columns.length < 10)
		{
			throw new LocalException("Wrong number of columns");
		}

		Affiliate affiliate = new Affiliate();
		try
		{
		    affiliate.setId(           SqlUtils.parseInt( columns[0] ));
		    affiliate.setSurname(                         columns[1] );
		    affiliate.setGivenName(                       columns[2] );
		    affiliate.setPosition(                        columns[3] );
		    affiliate.setPhone(                           columns[4] );
		    affiliate.setEmail(                           columns[5] );
		    affiliate.setAddress(                         columns[6] );
		    affiliate.setCreatedById(  SqlUtils.parseInt( columns[7] ));
            affiliate.setDateCreated( SqlUtils.parseDate( columns[8] ));
		    affiliate.setRemarks( SqlUtils.iso8859toUtf8( columns[9] ));
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
		
		// CreatedBy
        Integer creatorOptrId = affiliate.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        agent.setCreatedByAgent(createdByAgent);
        
		// Division
		agent.setDivision(division);
		  
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
		lastName = truncate(lastName, 50, "last name");
		agent.setLastName(lastName);
		
        // Remarks
        String remarks = affiliate.getRemarks();
        agent.setRemarks(remarks);
        
        // TimestampCreated
        Date dateCreated = affiliate.getDateCreated();
        agent.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
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

        return address;
	}

	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "AgentType, CreatedByAgentID, DivisionID, Email, FirstName, " +
				            "GUID, JobTitle, LastName, TimestampCreated, Remarks";

		String[] values = new String[10];

		values[0] = SqlUtils.sqlString( agent.getAgentType());
        values[1] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[2] = SqlUtils.sqlString( agent.getDivision().getDivisionId());
		values[3] = SqlUtils.sqlString( agent.getEmail());
		values[4] = SqlUtils.sqlString( agent.getFirstName());
		values[5] = SqlUtils.sqlString( agent.getGuid());
		values[6] = SqlUtils.sqlString( agent.getJobTitle());
		values[7] = SqlUtils.sqlString( agent.getLastName());
		values[8] = SqlUtils.sqlString( agent.getRemarks());
        values[9] = SqlUtils.sqlString( agent.getTimestampCreated());

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "Address, AgentID,  Phone1, TimestampCreated";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( address.getAddress());
        values[1] = SqlUtils.sqlString( address.getAgent().getAgentId());
        values[2] = SqlUtils.sqlString( address.getPhone1());
        values[3] = SqlUtils.now();
        
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
