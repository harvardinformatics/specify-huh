package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaAgent;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AsaAgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

//Run this class after AffiliateLoader.

public class AgentLoader extends CsvToSqlLoader
{
	private AsaAgentLookup agentLookup;
	
	private OrganizationLookup organizationLookup;
	
	private String getGuid(Integer agentId)
	{
		return agentId + " agent";
	}

	private AsaIdMapper agents;
	private BotanistLookup botanistLookup;
	
	public AgentLoader(File csvFile,
	                   Statement specifySqlStatement,
	                   File agentBotanists,
	                   BotanistLookup botanistLookup,
	                   OrganizationLookup organizationLookup) throws LocalException
	{
		super(csvFile, specifySqlStatement);
		
		this.agents = new AsaIdMapper(agentBotanists);

		this.botanistLookup = botanistLookup;
		this.organizationLookup = organizationLookup;
	}

	public AsaAgentLookup getAgentLookup()
	{
		if (agentLookup == null)
		{
			agentLookup = new AsaAgentLookup() {
				public Agent getByAsaAgentId(Integer asaAgentId) throws LocalException
				{
					Agent agent = new Agent();  // TODO: this doesn't take into account agent botanists

					String guid = getGuid(asaAgentId);

					Integer agentId = getInt("agent", "AgentID", "GUID", guid);

					agent.setAgentId(agentId);

					return agent;
				}
			};
		}
		return agentLookup;
	}
	
	private Agent lookup(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getByBotanistId(botanistId);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		AsaAgent asaAgent = parse(columns);

		Integer asaAgentId = asaAgent.getId();
		setCurrentRecordId(asaAgentId);
		
		Agent agent = getAgent(asaAgent);

		Integer botanistId = getBotanistId(asaAgentId);
		
        if (botanistId != null) // retain botanist id in guid
        {
            Agent botanistAgent = lookup(botanistId);
            Integer agentId = botanistAgent.getId();
            agent.setAgentId(agentId);
            
        	if (agent.getRemarks() != null)
            {
                warn("Ignoring remarks", asaAgent.getRemarks());
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

        // Correspondence address
        Address correspAddress = getCorrespAddress(asaAgent, agent);
        if (correspAddress != null)
        {
		    String sql = getInsertSql(correspAddress);
		    insert(sql);
        }
		
        // Shipping address
        Address shippingAddress = getShippingAddress(asaAgent, agent);
        if (shippingAddress != null)
        {
        	String sql = getInsertSql(shippingAddress);
        	insert(sql);
        }
	}

	private Integer getBotanistId(Integer agentId)
	{
	    return agents.map(agentId);
	}
	
	private AsaAgent parse(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		AsaAgent agent = new AsaAgent();
		try
		{
		    agent.setId(             SqlUtils.parseInt( columns[0]  ));
		    agent.setOrganizationId( SqlUtils.parseInt( columns[1]  ));
		    agent.setActive(      Boolean.parseBoolean( columns[2]  ));
		    agent.setPrefix(                            columns[3]  );
		    agent.setName(                              columns[4]  );
		    agent.setTitle(                             columns[5]  );
		    agent.setSpecialty(                         columns[6]  );
		    agent.setCorrespAddress(                    columns[7]  );
		    agent.setShippingAddress(                   columns[8]  );
            agent.setEmail(                             columns[9]  );
		    agent.setPhone(                             columns[10] );
            agent.setFax(                               columns[11] );
		    agent.setUri(                               columns[12] );
		    agent.setRemarks(   SqlUtils.iso8859toUtf8( columns[13] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return agent;
	}

	private Agent getAgent(AsaAgent asaAgent) throws LocalException
	{		
		Agent agent = new Agent();
		
		// AgentType
		agent.setAgentType(Agent.PERSON);

        // Email
		String email = asaAgent.getEmail();
		if (email != null )
		{
		    email = truncate(email, 50, "email");
			agent.setEmail(email);
		}

		// FirstName TODO: parse name?
		String firstName = asaAgent.getName();
		if (firstName != null)
		{
		    firstName = truncate(firstName, 50, "first name");
			agent.setFirstName(firstName);
		}
	    
		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration		Integer asaAgentId = asaAgent.getId();
		Integer asaAgentId = asaAgent.getId();
		checkNull(asaAgentId, "id");

		String guid = getGuid(asaAgentId);
		agent.setGuid(guid);

		// Interests
		String specialty = asaAgent.getSpecialty();
		if (specialty != null)
		{
			specialty = truncate(specialty, 255, "interests");
			agent.setInterests(specialty);
		}
		
		// JobTitle
		String position = asaAgent.getTitle();
		if (position != null)
		{
		    position = truncate(position, 50, "position");
			agent.setJobTitle(position);
		}
		
		// LastName
		String lastName = asaAgent.getName();
		checkNull(lastName, "last name");
		lastName = truncate(lastName, 50, "last name");
		agent.setLastName(lastName);
  
		// ParentOrganziation
		Integer organizationId = asaAgent.getOrganizationId();
		checkNull(organizationId, "organization id");
		
		Agent organization = lookupOrganization(organizationId);
		agent.setOrganization(organization);
		
        // Remarks
        String remarks = asaAgent.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks(remarks);
        }
		
		// Title
		String prefix = asaAgent.getPrefix();
		if (prefix != null && prefix.length() > 50)
		{
		    warn("Truncating prefix", prefix);
		    prefix = prefix.substring(0, 50);
		}
		agent.setTitle(prefix);
		
        // URL
		String uri = asaAgent.getUri();
		agent.setUrl(uri);
		
		return agent;
	}

	private Agent lookupOrganization(Integer organizationId) throws LocalException
	{
		return organizationLookup.getById(organizationId);
	}
	
	private Address getCorrespAddress(AsaAgent asaAgent, Agent agent)
	{
		String addressString = asaAgent.getCorrespAddress();
		String fax = asaAgent.getFax();
		String phone = asaAgent.getPhone();
		
		if (addressString == null && fax == null && phone == null) return null;
		
	    Address address = new Address();
   
        // Address TODO: parse address string
        if (addressString != null)
        {
            addressString = truncate(addressString, 255, "corresp address");
            address.setAddress(addressString);
        }
        
        // Agent
        address.setAgent(agent);
        
	    // Fax
        if (fax != null)
        {
        	fax = truncate(fax, 50, "fax");
        	address.setFax(fax);
        }
        
        // IsShipping
        address.setIsShipping(false);
        
        // Phone
        if (phone != null)
        {
            phone = truncate(phone, 50, "phone");
            address.setPhone1(phone);
        }
        
        return address;
	}

	private Address getShippingAddress(AsaAgent asaAgent, Agent agent)
	{
		String addressString = asaAgent.getShippingAddress();
		
		if (addressString == null) return null;
		
	    Address address = new Address();
	    
	    // Address
	    addressString = truncate(addressString, 255, "shipping address");
	    address.setAddress(addressString);

	    // Agent
	    address.setAgent(agent);
	    
	    // Fax
	    
	    // IsShipping
	    address.setIsShipping(true);

	    // Phone
	    
	    return address;
	}

	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "AgentType, Email, FirstName, GUID, Interests, JobTitle" +
				            "LastName, ParentOrganizationID, Remarks, TimestampCreated, " +
				            "Title, URL";

		String[] values = new String[12];

		values[0]  = SqlUtils.sqlString( agent.getAgentType());
		values[1]  = SqlUtils.sqlString( agent.getEmail());
		values[2]  = SqlUtils.sqlString( agent.getFirstName());
		values[3]  = SqlUtils.sqlString( agent.getGuid());
		values[4]  = SqlUtils.sqlString( agent.getInterests());
		values[5]  = SqlUtils.sqlString( agent.getJobTitle());
		values[6]  = SqlUtils.sqlString( agent.getLastName());
		values[7]  = SqlUtils.sqlString( agent.getOrganization().getId());
		values[8]  = SqlUtils.sqlString( agent.getRemarks());
		values[9]  = SqlUtils.now();
		values[10] = SqlUtils.sqlString( agent.getTitle());
		values[10] = SqlUtils.sqlString( agent.getUrl());

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
	private String getUpdateSql(Agent agent, Integer agentId)
	{
		String[] fields = { "Email", "Interests", "JobTitle", "ParentOrganizationID",
				            "Title", "URL" };
		
		String[] values = new String[6];
		
		values[0] = SqlUtils.sqlString( agent.getEmail());
		values[1] = SqlUtils.sqlString( agent.getInterests());
		values[2] = SqlUtils.sqlString( agent.getJobTitle());
		values[3] = SqlUtils.sqlString( agent.getOrganization().getId());
		values[4] = SqlUtils.sqlString( agent.getTitle());
		values[5] = SqlUtils.sqlString( agent.getUrl());
		
		return SqlUtils.getUpdateSql("agent", fields, values, "AgentID", agentId);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "AgentID, Address, Fax, IsShipping, Phone1,TimestampCreated";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( address.getAddress());
        values[1] = SqlUtils.sqlString( address.getAgent().getAgentId());
        values[2] = SqlUtils.sqlString( address.getFax());
        values[3] = SqlUtils.sqlString( address.getIsShipping());
        values[4] = SqlUtils.sqlString( address.getPhone1());
        values[5] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
