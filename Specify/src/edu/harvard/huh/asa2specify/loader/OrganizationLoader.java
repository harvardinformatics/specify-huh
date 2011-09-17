package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
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
	
	public OrganizationLoader(File csvFile, Statement specifySqlStatement) throws LocalException
	{
		super(csvFile, specifySqlStatement);
	}

	public OrganizationLookup getOrganizationLookup()
	{
		if (organizationLookup == null)
		{
			organizationLookup = new OrganizationLookup() {
				public Agent getById(Integer organizationId) throws LocalException
				{
					Agent agent = new Agent();
					
					String guid = getGuid(organizationId);
					
					Integer agentId = getId("agent", "AgentID", "GUID", guid);
					
					agent.setAgentId(agentId);
					
					return agent;
				}
			};
		}
		return organizationLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Organization organization = parse(columns);

		Integer organizationId = organization.getId();
		checkNull(organizationId, "id");
		setCurrentRecordId(organizationId);
		
		Agent agent = getAgent(organization);
        
		String sql = getInsertSql(agent);
		Integer agentId = insert(sql);
		agent.setAgentId(agentId);
        
        // Address
        Address address = getAddress(organization, agent);
        
        if (address != null)
        {
			sql = getInsertSql(address);
			insert(sql);
        }
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
		lastName = truncate(lastName, 120, "name");
		agent.setLastName(lastName);

		// MiddleInitial
		agent.setMiddleInitial(AgentType.organization.name());
		
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
		
		// IsCurrent
        address.setIsCurrent(false);

        // IsPrimary
        address.setIsPrimary(false);

        // IsShipping
        address.setIsShipping(false);
        
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
				            "LastName, MiddleInitial, ModifiedByAgentID, Remarks, TimestampCreated, " +
				            "TimestampModified, URL, Version";

		String[] values = {
				SqlUtils.sqlString( agent.getAbbreviation()),
				SqlUtils.sqlString( agent.getAgentType()),
				SqlUtils.sqlString( agent.getCreatedByAgent().getId()),
				SqlUtils.sqlString( agent.getGuid()),
				SqlUtils.sqlString( agent.getLastName()),
				SqlUtils.sqlString( agent.getMiddleInitial()),
				SqlUtils.sqlString( agent.getModifiedByAgent().getId()),
				SqlUtils.sqlString( agent.getRemarks()),
				SqlUtils.sqlString( agent.getTimestampCreated()),
				SqlUtils.sqlString( agent.getTimestampModified()),
				SqlUtils.sqlString( agent.getUrl()),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}

	private String getInsertSql(Address address) throws LocalException
    {
    	String fieldNames = "AgentID, City, Country, IsCurrent, IsPrimary, IsShipping, " +
    			            "Ordinal, State, TimestampCreated, Version";
    	
    	String[] values = {
    			SqlUtils.sqlString( address.getAgent().getAgentId()),
    			SqlUtils.sqlString( address.getCity()),
    			SqlUtils.sqlString( address.getCountry()),
    			SqlUtils.sqlString( address.getIsCurrent()),
    			SqlUtils.sqlString( address.getIsPrimary()),
    			SqlUtils.sqlString( address.getIsShipping()),
    			SqlUtils.addressOrdinal( address.getAgent().getId()),
    			SqlUtils.sqlString( address.getState()),
    			SqlUtils.now(),
    			SqlUtils.one()
    	};
    	
    	return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
