package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after SeriesLoader.

public class OrganizationLoader extends AuditedObjectLoader
{    
    private OrganizationLookup organizationLookup;
	
	private String getGuid(Integer organizationId)
	{
		return organizationId + " organization";
	}
	
    private AsaIdMapper orgMapper;
	private BotanistLookup botanistLookup;
	
	public OrganizationLoader(File csvFile,
	                          Statement specifySqlStatement,
	                          File orgToBotanist,
	                          BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, specifySqlStatement);
		
		this.orgMapper = new AsaIdMapper(orgToBotanist);
		this.botanistLookup = botanistLookup;
	}

	public OrganizationLookup getOrganizationLookup()
	{
		if (organizationLookup == null)
		{
			organizationLookup = new OrganizationLookup() {
				public Agent getById(Integer organizationId) throws LocalException
				{
					Agent agent = new Agent();
					
					Integer botanistId = getBotanistId(organizationId);

					String guid = botanistId != null ? BotanistLoader.getGuid(botanistId) : getGuid(organizationId);
					
					Integer agentId = getId("agent", "AgentID", "GUID", guid);
					
					agent.setAgentId(agentId);
					
					return agent;
				}
			};
		}
		return organizationLookup;
	}
	
	private Agent lookup(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Organization organization = parse(columns);

		Integer organizationId = organization.getId();
		checkNull(organizationId, "id");
		setCurrentRecordId(organizationId);
		
		Agent agent = getAgent(organization);
        
        Integer botanistId = getBotanistId(organization.getId());
        
        if (botanistId != null) // retain botanist id in guid
        {
            Agent botanistAgent = lookup(botanistId);
            Integer agentId = botanistAgent.getId();
            agent.setAgentId(agentId);
            
        	if (agent.getRemarks() != null)
            {
                getLogger().warn(rec() + "Ignoring remarks: " + agent.getRemarks());
            }
        	
        	getLogger().warn(rec() + "Replacing url: " + agent.getUrl());
        	
            String sql = getUpdateSql(agent, agentId);
            update(sql);
        }
        else
        {
            String sql = getInsertSql(agent);
            Integer agentId = insert(sql);
            agent.setAgentId(agentId);
        }
        
        // Address
        Address address = getAddress(organization, agent);
        
        if (address != null)
        {
			String sql = getInsertSql(address);
			insert(sql);
        }

	}
	
	private Integer getBotanistId(Integer organizationId)
	{
	    return orgMapper.map(organizationId);
	}
	   
	private Organization parse(String[] columns) throws LocalException
	{
	    Organization organization = new Organization();
	    
	    int i = super.parse(columns, organization);
	    
		if (columns.length < i + 7)
		{
			throw new LocalException("Not enough columns");
		}
		
		try
		{
			organization.setName(                            columns[i + 0] );
			organization.setAcronym(                         columns[i + 1] );
			organization.setCity(                            columns[i + 2] );
			organization.setState(                           columns[i + 3] );
			organization.setCountry(                         columns[i + 4] );
			organization.setUri(                             columns[i + 5] );        
			organization.setRemarks( SqlUtils.iso8859toUtf8( columns[i + 6] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return organization;
	}

	private Agent getAgent(Organization organization) throws LocalException
	{		
		Agent agent = new Agent();

		// Abbreviation TODO: merge records based on abbreviation?
		String abbreviation = organization.getAcronym();
		if (abbreviation != null && !abbreviation.startsWith("*"))
		{
			abbreviation = truncate(abbreviation, 50, "acronym");
			agent.setAbbreviation(abbreviation);
		}
		
		// AgentType
		agent.setAgentType(Agent.ORG);
        
		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		Integer organizationId = organization.getId();
		checkNull(organizationId, "id");
		
		String guid = getGuid(organizationId);
		agent.setGuid(guid);

		// LastName
		String lastName = organization.getName();
		checkNull(lastName, "name");
		lastName = truncate(lastName, 200, "name");
		agent.setLastName(lastName);

		// Remarks
		String remarks = organization.getRemarks();
		agent.setRemarks((remarks));

        // URL
		String url = organization.getUri();
		if (url != null)
		{
			url = truncate(url, 255, "url");
			agent.setUrl(url);
		}
		
		setAuditFields(organization, agent);
		
		return agent;
	}

	private Address getAddress(Organization organization, Agent agent)
	{	
		String city = organization.getCity();
		String state = organization.getState();
		String country = organization.getCountry();
		
		if (city == null && state == null && country == null) return null;

		Address address = new Address();
		
		// Agent
		address.setAgent(agent);
		
		// City
		if (city != null)
		{
			city = truncate(city, 64, "city");
			address.setCity(city);
		}
		
		// State
		if (state != null)
		{
			state = truncate(state, 64, "state");
			address.setState(state);
		}
		
		// Country
		if (country != null)
		{
			country = truncate(country, 64, "country");
			address.setCountry(country);
		}
		
		return address;
	}
	
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "Abbreviation, AgentType, CreatedByAgentID, GUID, " +
				            "LastName, ModifiedByAgentID, Remarks, TimestampCreated, " +
				            "TimestampModified, URL, Version";

		String[] values = new String[11];

		values[0]  = SqlUtils.sqlString( agent.getAbbreviation());
		values[1]  = SqlUtils.sqlString( agent.getAgentType());
		values[2]  = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[3]  = SqlUtils.sqlString( agent.getGuid());
		values[4]  = SqlUtils.sqlString( agent.getLastName());
		values[5]  = SqlUtils.sqlString( agent.getModifiedByAgent().getId());
		values[6]  = SqlUtils.sqlString( agent.getRemarks());
		values[7]  = SqlUtils.sqlString( agent.getTimestampCreated());
		values[8]  = SqlUtils.sqlString( agent.getTimestampModified());
		values[9]  = SqlUtils.sqlString( agent.getUrl());
		values[10] = SqlUtils.one();
		
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
	private String getUpdateSql(Agent agent, Integer agentId) throws LocalException
	{
        String[] fieldNames = { "Abbreviation", "URL" };

        String[] values = new String[2];

        values[0] = SqlUtils.sqlString( agent.getAbbreviation());
        values[1] = SqlUtils.sqlString( agent.getUrl());
        
        return SqlUtils.getUpdateSql("agent", fieldNames, values, "AgentID", agentId);
	}

	private String getInsertSql(Address address) throws LocalException
    {
    	String fieldNames = "AgentID, City, State, Country, Ordinal, TimestampCreated, Version";
    	
    	String[] values = new String[7];
    	
    	values[0] = SqlUtils.sqlString( address.getAgent().getAgentId());
    	values[1] = SqlUtils.sqlString( address.getCity());
    	values[2] = SqlUtils.sqlString( address.getState());
    	values[3] = SqlUtils.sqlString( address.getCountry());
    	values[4] = SqlUtils.addressOrdinal( address.getAgent().getAgentId());
    	values[5] = SqlUtils.now();
    	values[6] = SqlUtils.one();
    	
    	return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
