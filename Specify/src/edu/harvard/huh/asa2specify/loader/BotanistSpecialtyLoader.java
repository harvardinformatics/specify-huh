package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BotanistRoleSpecialty;
import edu.harvard.huh.asa.BotanistRoleSpecialty.SPECIALTY;
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
			botanistRoleSpecialty.setBotanistId(                   SqlUtils.parseInt( columns[0] ));
			botanistRoleSpecialty.setRole(                                            columns[1] );
			botanistRoleSpecialty.setSpecialty( BotanistRoleSpecialty.parseSpecialty( columns[2] ));
			botanistRoleSpecialty.setOrdinal(                      SqlUtils.parseInt( columns[3] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
        {
            throw new LocalException("Couldn't parse specialty", e);
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
		SPECIALTY specialty = botanistRoleSpecialty.getSpecialty();
		checkNull(specialty, "specialty");
		
        String role = botanistRoleSpecialty.getRole();
        checkNull(role, "role");
        role = truncate(role, 64, "specialty role");
        
		agentSpecialty.setSpecialtyName(BotanistRoleSpecialty.toString(specialty) + "(" + role + ")");
		
		return agentSpecialty;
	}

    private Agent lookup(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }

	private String getInsertSql(AgentSpecialty agentSpecialty)
	{
		String fieldNames = "AgentId, OrderNumber, SpecialtyName, TimestampCreated, Version";

		String[] values = {
				SqlUtils.sqlString( agentSpecialty.getAgent().getAgentId()),
				SqlUtils.sqlString( agentSpecialty.getOrderNumber()),
				SqlUtils.sqlString( agentSpecialty.getSpecialtyName()),
				SqlUtils.now(),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("agentspecialty", fieldNames, values);
	}
}
