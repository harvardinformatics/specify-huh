package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.BotanistRoleCountry;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentGeography;
import edu.ku.brc.specify.datamodel.Geography;

// Run this class after BotanistLoader and GeoUnitLoader.

public class BotanistCountryLoader extends CsvToSqlLoader
{   
    // lookups for geography
    private GeoUnitLookup geoLookup;
    private BotanistLookup  botanistLookup;
    
	public BotanistCountryLoader(File csvFile,
	                             Statement sqlStatement,
	                             GeoUnitLookup geoLookup,
	                             BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.geoLookup = geoLookup;
		this.botanistLookup = botanistLookup;
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
			throw new LocalException("Not enough columns");
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

		// Remarks
		Integer ordinal = botanistRoleCountry.getOrdinal();
		if (ordinal != null)
		{
			String remarks = getOrdinalNote(ordinal);
			agentGeography.setRemarks(remarks);
		}
		
		// Role
		String role = botanistRoleCountry.getRole();
		checkNull(role, "role");
		role = truncate(role, 64, "role");
		agentGeography.setRole(role);

		return agentGeography;
	}

	private String getOrdinalNote(int i)
	{
		if (i == 1) return "primary";
		if (i == 2) return "secondary";
		if (i == 3) return "tertiary";
		if (i == 4) return "quaternary";
		if (i == 5) return "quinary";
		if (i == 6) return "senary";
		if (i == 7) return "septenary";
		if (i == 8) return "octonary";
		if (i == 9) return "nonary";
		if (i == 10) return "denary";
		
		return String.valueOf(i) + "th";
	}
	private Geography lookupGeography(Integer geoUnitId) throws LocalException
	{
	    return geoLookup.getById(geoUnitId);
	}
    
	private Agent lookupBotanist(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }
  
	private String getInsertSql(AgentGeography agentGeography)
	{
		String fieldNames = "AgentId, GeographyID, Remarks, Role, TimestampCreated, Version";

		String[] values = {
				SqlUtils.sqlString( agentGeography.getAgent().getId()),
				SqlUtils.sqlString( agentGeography.getGeography().getId()),
				SqlUtils.sqlString( agentGeography.getRemarks()), 
				SqlUtils.sqlString( agentGeography.getRole()),
				SqlUtils.now(),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("agentgeography", fieldNames, values);
	}

}
