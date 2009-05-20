package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistRoleSpecialty;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentSpecialty;

public class BotanistSpecialtyLoader extends CsvToSqlLoader
{
	private int lastAgentId;
	private int orderNumber;

	public BotanistSpecialtyLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
		lastAgentId = 0;
		orderNumber = 1;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistRoleSpecialty botanistRoleSpecialty = parse(columns);

		// convert BotanistRoleCountry into AgentSpecialty
		AgentSpecialty agentSpecialty = convert(botanistRoleSpecialty);

		// find the matching agent record
		Agent agent = new Agent();
		Integer botanistId = botanistRoleSpecialty.getBotanistId();

		if (botanistId == null)
		{
			throw new LocalException("No botanist id");
		}
		Botanist botanist = new Botanist();
		botanist.setId(botanistId);

		String guid = botanist.getGuid();

		String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

		Integer agentId = queryForId(sql);
		if (agentId == null)
		{
			throw new LocalException("Couldn't find AgentID with GUID " + guid);
		}
		agent.setAgentId(agentId);
		agentSpecialty.setAgent(agent);

		if (agentId != lastAgentId)
		{
			orderNumber = 1;
			lastAgentId = agentId;
		}
		else
		{
			orderNumber++;
		}
		agentSpecialty.setOrderNumber(orderNumber);

		// convert agentspecialty to sql and insert
		sql = getInsertSql(agentSpecialty);
		insert(sql);
	}

	private BotanistRoleSpecialty parse(String[] columns) throws LocalException
	{
		if (columns.length < 4)
		{
			throw new LocalException("Wrong number of columns");
		}

		BotanistRoleSpecialty botanistRoleSpecialty = new BotanistRoleSpecialty();
		try {
			botanistRoleSpecialty.setBotanistId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );

			String role = StringUtils.trimToNull( columns[1] );
			if (role == null) throw new LocalException("No type found in record");

			botanistRoleSpecialty.setRole(role);

			String specialty = StringUtils.trimToNull( columns[2] );
			botanistRoleSpecialty.setSpecialty(specialty);

			botanistRoleSpecialty.setOrdinal(Integer.parseInt(StringUtils.trimToNull( columns[3] ) ) );
		}
		catch (NumberFormatException e) {
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return botanistRoleSpecialty;
	}

	private AgentSpecialty convert(BotanistRoleSpecialty botanistRoleSpecialty) {

		AgentSpecialty agentSpecialty = new AgentSpecialty();

		String role = botanistRoleSpecialty.getRole();
		String specialty = botanistRoleSpecialty.getSpecialty();

		String specialtyName = specialty + " (" + role + ")";
		if (specialtyName.length() > 64)
		{
			warn("Truncating botanist specialty", botanistRoleSpecialty.getBotanistId(), specialtyName);
			specialtyName = specialtyName.substring(0, 64);
		}
		agentSpecialty.setSpecialtyName(specialtyName);

		Integer ordinal = botanistRoleSpecialty.getOrdinal();        
		agentSpecialty.setOrderNumber(ordinal);

		return agentSpecialty;
	}

	private String getInsertSql(AgentSpecialty agentSpecialty)
	{
		String fieldNames = "AgentId, SpecialtyName, OrderNumber, TimestampCreated";

		String[] values = new String[4];

		values[0] = SqlUtils.sqlString( agentSpecialty.getAgent().getAgentId());
		values[1] = SqlUtils.sqlString( agentSpecialty.getSpecialtyName());
		values[2] = SqlUtils.sqlString( agentSpecialty.getOrderNumber());
		values[3] = SqlUtils.now();

		return SqlUtils.getInsertSql("agentspecialty", fieldNames, values);
	}
}
