package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Organization;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

public class OrganizationLoader extends CsvToSqlLoader {

	private final Logger log  = Logger.getLogger(OrganizationLoader.class);

	public OrganizationLoader(File csvFile, Statement specifySqlStatement)
	{
		super(csvFile, specifySqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Organization organization = parseOrganizationRecord(columns);

		// convert organization into agent ...
		Agent agent = convert(organization);

        // find matching record creator
        Integer creatorOptrId = organization.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        agent.setCreatedByAgent(createdByAgent);
        
		// convert organization to sql and insert
		String sql = getInsertSql(agent);
		Integer agentId = insert(sql);
		agent.setAgentId(agentId);

		String city = organization.getCity();
		String state = organization.getState();
		String country = organization.getCountry();

		if (city != null || state != null || country != null) {
			Address address = new Address();

			address.setCity(city);
			address.setState(state);
			address.setCountry(country);
			address.setAgent(agent);

			try {
				sql = getInsertSql(address);
				insert(sql);
			}
			catch (LocalException e) {
				warn("Couldn't insert address record", organization.getId(), e.getMessage());
			}
		}
	}

	private Organization parseOrganizationRecord(String[] columns) throws LocalException
	{
		if (columns.length < 10)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Botanist object
		Organization organization = new Organization();

		try {
			organization.setId(Integer.parseInt(StringUtils.trimToNull( columns[0] )));
			organization.setName(               StringUtils.trimToNull( columns[1] ));
			organization.setAcronym(            StringUtils.trimToNull( columns[2] ));
			organization.setCity(               StringUtils.trimToNull( columns[3] ));
			organization.setState(              StringUtils.trimToNull( columns[4] ));
			organization.setCountry(            StringUtils.trimToNull( columns[5] ));
			organization.setUri(                StringUtils.trimToNull( columns[6] ));
			
            Integer optrId =   Integer.parseInt(StringUtils.trimToNull( columns[7] ));
            organization.setCreatedById(optrId);
            
            String createDateString =           StringUtils.trimToNull( columns[8] );
            Date createDate = SqlUtils.parseDate(createDateString);
            organization.setDateCreated(createDate);
            
			organization.setRemarks(            StringUtils.trimToNull (columns[9] ));
		}
		catch (NumberFormatException e) {
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return organization;
	}

	public Agent convert(Organization organization) throws LocalException
	{

		Agent agent = new Agent();

		// AgentType
		agent.setAgentType(Agent.ORG);

		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		agent.setGuid(organization.getGuid());

		// Abbreviation
		String abbreviation = organization.getAcronym();
		if (abbreviation != null && !abbreviation.startsWith("*"))
		{
		    if (abbreviation.length() > 50)
		    {
		        warn("Truncating acronym", organization.getId(), abbreviation);
		        abbreviation = abbreviation.substring(0, 50);
		    }
		      agent.setAbbreviation(abbreviation);
		}

		// LastName
		String lastName = organization.getName();
		if ( lastName.length() > 50 ) {
		    warn("Truncating last name", organization.getId(), lastName);
			lastName = lastName.substring( 0, 50);
		}
		agent.setLastName(lastName);

		// URL
		String url = organization.getUri();
		if (url != null && url.length() > 255) {
		    warn("Truncating url", organization.getId(), url);
			url = url.substring(0, 255);
		}
		agent.setUrl(url);

		// Remarks
		String remarks = organization.getRemarks();
		if ( remarks != null ) {
			agent.setRemarks(SqlUtils.iso8859toUtf8(remarks));
		}

        // TimestampCreated
        Date dateCreated = organization.getDateCreated();
        agent.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		return agent;
	}

	public String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, Abbreviation, LastName, CreatedByAgentID, TimestampCreated, Remarks";

		String[] values = new String[7];

		values[0] =     String.valueOf( agent.getAgentType());
		values[1] = SqlUtils.sqlString( agent.getGuid());
		values[2] = SqlUtils.sqlString( agent.getAbbreviation());
		values[3] = SqlUtils.sqlString( agent.getLastName());
		values[4] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[5] = SqlUtils.sqlString( agent.getTimestampCreated());
		values[6] = SqlUtils.sqlString( agent.getRemarks());

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    public String getInsertSql(Address address) throws LocalException
    {
    	String fieldNames = "City, State, Country, AgentID, TimestampCreated";
    	
    	String[] values = new String[5];
    	
    	values[0] = SqlUtils.sqlString( address.getCity());
    	values[1] = SqlUtils.sqlString( address.getState());
    	values[2] = SqlUtils.sqlString( address.getCountry());
    	values[3] =     String.valueOf( address.getAgent().getAgentId());
    	values[4] = "now()";
    	
    	return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
