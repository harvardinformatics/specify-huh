package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
				log.warn("Couldn't insert address record", e);
			}
		}
	}

	private Organization parseOrganizationRecord(String[] columns) throws LocalException
	{
		if (columns.length < 8)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Botanist object
		Organization organization = new Organization();

		try {
			organization.setId(Integer.parseInt(StringUtils.trimToNull(columns[0])));
			organization.setName(               StringUtils.trimToNull(columns[1]));
			organization.setAcronym(            StringUtils.trimToNull(columns[2]));
			organization.setCity(               StringUtils.trimToNull(columns[3]));
			organization.setState(              StringUtils.trimToNull(columns[4]));
			organization.setCountry(            StringUtils.trimToNull(columns[5]));
			organization.setUri(                StringUtils.trimToNull(columns[6]));
			organization.setRemarks(            StringUtils.trimToNull(columns[7]));
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
		if (abbreviation.length() > 50) {
			log.warn("truncating acronym");
			abbreviation = abbreviation.substring(0, 50);
		}
		agent.setAbbreviation(abbreviation);

		// LastName
		String lastName = organization.getName();
		if ( lastName.length() > 50 ) {
			log.warn( "truncating last name" );
			lastName = lastName.substring( 0, 50);
		}
		agent.setLastName(lastName);

		// URL
		String url = organization.getUri();
		if (url.length() > 255) {
			log.warn("truncating acronym");
			url = url.substring(0, 255);
		}
		agent.setUrl(url);

		// Remarks
		String remarks = organization.getRemarks();
		if ( remarks != null ) {
			agent.setRemarks(SqlUtils.iso8859toUtf8(remarks));
		}

		return agent;
	}

	public String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, Abbreviation, LastName, TimestampCreated, Remarks";

		List<String> values = new ArrayList<String>(6);

		values.add(    String.valueOf(agent.getAgentType()   ));
		values.add(SqlUtils.sqlString(agent.getGuid()        ));
		values.add(SqlUtils.sqlString(agent.getAbbreviation()));
		values.add(SqlUtils.sqlString(agent.getLastName()    ));
		values.add("now()" );
		values.add(SqlUtils.sqlString(agent.getRemarks()     ));

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    public String getInsertSql(Address address) throws LocalException
    {
    	String fieldNames = "City, State, Country, AgentID";
    	
    	List<String> values = new ArrayList<String>(5);
    	
    	values.add(SqlUtils.sqlString(address.getCity()));
    	values.add(SqlUtils.sqlString(address.getState()));
    	values.add(SqlUtils.sqlString(address.getCountry()));
    	values.add(    String.valueOf(address.getAgent().getAgentId()));
    	values.add("now");
    	
    	return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
