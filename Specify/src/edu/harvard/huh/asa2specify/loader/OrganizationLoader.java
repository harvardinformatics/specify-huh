package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after SeriesLoader.

public class OrganizationLoader extends AuditedObjectLoader
{
    private static final Logger log  = Logger.getLogger(OrganizationLoader.class);
	
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

	public Logger getLogger()
    {
        return log;
    }
	
	private Integer getBotanistId(Integer organizationId)
	{
	    return orgMapper.map(organizationId);
	}
	   
	private Organization parse(String[] columns) throws LocalException
	{
		if (columns.length < 10)
		{
			throw new LocalException("Not enough columns");
		}
		
		Organization organization = new Organization();
		try
		{
			organization.setId(           SqlUtils.parseInt( columns[0] ));
			organization.setName(                            columns[1] );
			organization.setAcronym(                         columns[2] );
			organization.setCity(                            columns[3] );
			organization.setState(                           columns[4] );
			organization.setCountry(                         columns[5] );
			organization.setUri(                             columns[6] );
			organization.setCreatedById(  SqlUtils.parseInt( columns[7] ));
            organization.setDateCreated( SqlUtils.parseDate( columns[8] ));            
			organization.setRemarks( SqlUtils.iso8859toUtf8( columns[9] ));
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

        // CreatedByAgent
        Integer creatorOptrId = organization.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        agent.setCreatedByAgent(createdByAgent);
        
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

        // TimestampCreated
        Date dateCreated = organization.getDateCreated();
        agent.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
		
        // URL
		String url = organization.getUri();
		if (url != null)
		{
			url = truncate(url, 255, "url");
			agent.setUrl(url);
		}
		
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
				            "LastName, Remarks, TimestampCreated, URL, Version";

		String[] values = new String[9];

		values[0] = SqlUtils.sqlString( agent.getAbbreviation());
		values[1] = SqlUtils.sqlString( agent.getAgentType());
		values[2] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[3] = SqlUtils.sqlString( agent.getGuid());
		values[4] = SqlUtils.sqlString( agent.getLastName());
		values[5] = SqlUtils.sqlString( agent.getRemarks());
		values[6] = SqlUtils.sqlString( agent.getTimestampCreated());
		values[7] = SqlUtils.sqlString( agent.getUrl());
		values[8] = SqlUtils.zero();
		
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
    	String fieldNames = "AgentID, City, State, Country, TimestampCreated, Version";
    	
    	String[] values = new String[6];
    	
    	values[0] = SqlUtils.sqlString( address.getAgent().getAgentId());
    	values[1] = SqlUtils.sqlString( address.getCity());
    	values[2] = SqlUtils.sqlString( address.getState());
    	values[3] = SqlUtils.sqlString( address.getCountry());
    	values[4] = SqlUtils.now();
    	values[5] = SqlUtils.zero();
    	
    	return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
