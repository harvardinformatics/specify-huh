package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.BotanistRoleSpecialty;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentSpecialty;

// Run this class after BotanistLoader.

public class BotanistSpecialtyLoader extends CsvToSqlLoader
{
    private BotanistLookup botanistLookup;
    
	private int lastAgentId;
	private int orderNumber;

	public BotanistSpecialtyLoader(File csvFile,
	                               Statement sqlStatement,
	                               BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.botanistLookup = botanistLookup;
		
		lastAgentId = 0;
		orderNumber = 1;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistRoleSpecialty botanistRoleSpecialty = parse(columns);

		Integer botanistId = botanistRoleSpecialty.getBotanistId();
		setCurrentRecordId(botanistId);
		
		// convert BotanistRoleCountry into AgentSpecialty
		AgentSpecialty agentSpecialty = getAgentSpecialty(botanistRoleSpecialty);

		// convert agentspecialty to sql and insert
		String sql = getInsertSql(agentSpecialty);
		insert(sql);
	}
	
	private BotanistRoleSpecialty parse(String[] columns) throws LocalException
	{
		if (columns.length < 4)
		{
			throw new LocalException("Not enough columns");
		}

		BotanistRoleSpecialty botanistRoleSpecialty = new BotanistRoleSpecialty();
		try
		{
			botanistRoleSpecialty.setBotanistId( SqlUtils.parseInt( columns[0] ));
			botanistRoleSpecialty.setRole(                          columns[1] );
			botanistRoleSpecialty.setSpecialty(                     columns[2] );
			botanistRoleSpecialty.setOrdinal(    SqlUtils.parseInt( columns[3] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return botanistRoleSpecialty;
	}

	private AgentSpecialty getAgentSpecialty(BotanistRoleSpecialty botanistRoleSpecialty) throws LocalException
	{

		AgentSpecialty agentSpecialty = new AgentSpecialty();

		// Agent
		Integer botanistId = botanistRoleSpecialty.getBotanistId();
		checkNull(botanistId, "botanist id");
		
		Agent agent = lookup(botanistId);
		agentSpecialty.setAgent(agent);

		// OrderNumber
		if (agent.getId() != lastAgentId)
		{
			orderNumber = 1;
			lastAgentId = agent.getId();
		}
		else
		{
			orderNumber++;
		}
		agentSpecialty.setOrderNumber(orderNumber);
		
        // SpecialtyName
		String role = botanistRoleSpecialty.getRole();
        checkNull(role, "role");
        
		String specialty = botanistRoleSpecialty.getSpecialty();
		checkNull(specialty, "specialty");
		
		String specialtyName = specialty + " (" + role + ")";
		specialtyName = truncate(specialtyName, 64, "specialty name");
		agentSpecialty.setSpecialtyName(specialtyName);

		return agentSpecialty;
	}

    private Agent lookup(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }

	private String getInsertSql(AgentSpecialty agentSpecialty)
	{
		String fieldNames = "AgentId, OrderNumber, SpecialtyName, TimestampCreated, Version";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( agentSpecialty.getAgent().getAgentId());
		values[1] = SqlUtils.sqlString( agentSpecialty.getOrderNumber());
		values[2] = SqlUtils.sqlString( agentSpecialty.getSpecialtyName());
		values[3] = SqlUtils.now();
		values[4] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("agentspecialty", fieldNames, values);
	}
}
