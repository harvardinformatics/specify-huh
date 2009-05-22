package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistRoleCountry;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentGeography;
import edu.ku.brc.specify.datamodel.Geography;

public class BotanistCountryLoader extends CsvToSqlLoader
{
	public BotanistCountryLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistRoleCountry botanistRoleCountry = parse(columns);

		// convert BotanistRoleCountry into AgentGeography
		AgentGeography agentGeography = convert(botanistRoleCountry);

		// find the matching agent record
		Agent agent = new Agent();
		Integer botanistId = botanistRoleCountry.getBotanistId();

		if (botanistId == null)
		{
			throw new LocalException("No botanist id");
		}
		Botanist botanist = new Botanist();
		botanist.setId(botanistId);

		String guid = botanist.getGuid();

		Integer agentId = getIntByField("agent", "AgentID", "GUID", guid);

		agent.setAgentId(agentId);
		agentGeography.setAgent(agent);

		// find the matching geography record
		Geography geography = new Geography();
		Integer geoUnitId = botanistRoleCountry.getGeoUnitId();

		if (geoUnitId == null)
		{
			throw new LocalException("No geo unit id");
		}
		guid = String.valueOf(geoUnitId);

		Integer geographyId = getIntByField("geography", "GeographyID", "GUID", guid);

		geography.setGeographyId(geographyId);
		agentGeography.setGeography(geography);

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
			botanistRoleCountry.setBotanistId( Integer.parseInt( StringUtils.trimToNull( columns[0] )));

			String role =                                        StringUtils.trimToNull( columns[1] );
			if (role == null) throw new LocalException("No type found in record ");

			botanistRoleCountry.setRole(role);

			botanistRoleCountry.setGeoUnitId( Integer.parseInt( StringUtils.trimToNull( columns[2] )));
			botanistRoleCountry.setOrdinal(   Integer.parseInt( StringUtils.trimToNull( columns[3] )));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return botanistRoleCountry;
	}


	private AgentGeography convert(BotanistRoleCountry botanistRoleCountry)
	{
		AgentGeography agentGeography = new AgentGeography();

		String role = botanistRoleCountry.getRole();
		if (role.length() > 64)
		{
			warn("Truncating botanist role", botanistRoleCountry.getBotanistId(), role);
			role = role.substring(0, 64);
		}
		agentGeography.setRole(role);

		Integer ordinal = botanistRoleCountry.getOrdinal();
		agentGeography.setRemarks(String.valueOf(ordinal));

		return agentGeography;
	}

	private String getInsertSql(AgentGeography agentGeography)
	{
		String fieldNames = "AgentId, GeographyID, Role, TimestampCreated, Remarks";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( agentGeography.getAgent().getAgentId());
		values[1] = SqlUtils.sqlString( agentGeography.getGeography().getGeographyId());
		values[2] = SqlUtils.sqlString( agentGeography.getRole());
		values[3] = SqlUtils.now();
		values[4] = SqlUtils.sqlString( agentGeography.getRemarks()); 

		return SqlUtils.getInsertSql("agentgeography", fieldNames, values);
	}

}
