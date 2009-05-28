package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.BotanistRoleCountry;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.GeographyLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentGeography;
import edu.ku.brc.specify.datamodel.Geography;

// Run this class after BotanistLoader and GeoUnitLoader.

public class BotanistCountryLoader extends CsvToSqlLoader
{
    // lookups for geography
    private GeographyLookup geoLookup;
    private BotanistLookup  botanistLookup;
    
	public BotanistCountryLoader(File csvFile,
	                             Statement sqlStatement,
	                             GeographyLookup geoLookup,
	                             BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.geoLookup = geoLookup;
		this.botanistLookup = botanistLookup;
	}

	private Geography lookupGeography(Integer geoUnitId) throws LocalException
	{
	    return geoLookup.getByGeoUnitId(geoUnitId);
	}
    
	private Agent lookupBotanist(Integer botanistId) throws LocalException
    {
        return botanistLookup.getByBotanistId(botanistId);
    }
    
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistRoleCountry botanistRoleCountry = parse(columns);
		
		Integer botanistId = botanistRoleCountry.getBotanistId();
		setCurrentRecordId(botanistId);

		// convert BotanistRoleCountry into AgentGeography
		AgentGeography agentGeography = getAgentGeography(botanistRoleCountry);

		// convert agentgeography to sql and insert
		String sql = getInsertSql(agentGeography);
		insert(sql);
	}

	private BotanistRoleCountry parse(String[] columns) throws LocalException
	{
		if (columns.length < 4)
		{
			throw new LocalException("Wrong number of columns");
		}

		BotanistRoleCountry botanistRoleCountry = new BotanistRoleCountry();
		try
		{
			botanistRoleCountry.setBotanistId( SqlUtils.parseInt( columns[0] ));
			botanistRoleCountry.setRole(                          columns[1] );
			botanistRoleCountry.setGeoUnitId(  SqlUtils.parseInt( columns[2] ));
			botanistRoleCountry.setOrdinal(    SqlUtils.parseInt( columns[3] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		
		return botanistRoleCountry;
	}

	private AgentGeography getAgentGeography(BotanistRoleCountry botanistRoleCountry)
		throws LocalException
	{
		AgentGeography agentGeography = new AgentGeography();

		// Agent
		Integer botanistId = botanistRoleCountry.getBotanistId();
		checkNull(botanistId, "botanist id");

		Agent agent = lookupBotanist(botanistId);
		agentGeography.setAgent(agent);

		// Geography
		Integer geoUnitId = botanistRoleCountry.getGeoUnitId();
		checkNull(geoUnitId, "geo unit id");

		Geography geography = lookupGeography(geoUnitId);
		agentGeography.setGeography(geography);
		
		// Role
		String role = botanistRoleCountry.getRole();
		checkNull(role, "role");
		role = truncate(role, 64, "role");
		agentGeography.setRole(role);

		Integer ordinal = botanistRoleCountry.getOrdinal();
		if (ordinal != null)
		{
			String remarks = String.valueOf(ordinal);
			agentGeography.setRemarks(remarks);
		}

		return agentGeography;
	}

	private String getInsertSql(AgentGeography agentGeography)
	{
		String fieldNames = "AgentId, GeographyID, , Remarks, Role, TimestampCreated";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( agentGeography.getAgent().getAgentId());
		values[1] = SqlUtils.sqlString( agentGeography.getGeography().getGeographyId());
		values[2] = SqlUtils.sqlString( agentGeography.getRemarks()); 
		values[3] = SqlUtils.sqlString( agentGeography.getRole());
		values[4] = SqlUtils.now();

		return SqlUtils.getInsertSql("agentgeography", fieldNames, values);
	}

}
