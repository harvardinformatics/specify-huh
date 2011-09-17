package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Optr;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class first.
public class OptrLoader extends CsvToSqlLoader
{    
    private OptrLookup optrLookup;
    
	public OptrLoader(File csvFile, Statement specifySqlStatement) throws LocalException
	{
		super(csvFile, specifySqlStatement);
	}

	public OptrLookup getOptrLookup()
	{
		if (optrLookup == null)
		{
			optrLookup = new OptrLookup() {
				public Agent queryById(Integer optrId) throws LocalException
				{
					String guid = getGuid(optrId);

					Integer agentId = queryForInt("agent", "AgentID", "GUID", guid);

					if (agentId == null) return null;

					Agent agent = new Agent();
					agent.setAgentId(agentId);
			        
			        return agent;
				}
			};
		}
		return optrLookup;
	}
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Optr optr = parse(columns);

		Integer optrId = optr.getId();
		setCurrentRecordId(optrId);
		
		// convert optr into agent ...
		Agent agent = getAgent(optr);

		// convert organization to sql and insert
		String sql = getInsertSql(agent);
		insert(sql);
	}
    
	private Optr parse(String[] columns) throws LocalException
	{
		if (columns.length < 4)
		{
			throw new LocalException("Not enough columns");
		}

		Optr optr = new Optr();
		try
		{
		    optr.setId(        SqlUtils.parseInt( columns[0] ));
		    optr.setUserName(                     columns[1] );
		    optr.setFullName(                     columns[2] );
		    optr.setNote( SqlUtils.iso8859toUtf8( columns[3] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		
		return optr;
	}

	private Agent getAgent(Optr optr) throws LocalException
	{
		Agent agent = new Agent();

		Integer optrId = optr.getId();
		checkNull(optrId, "id");
		
		// AgentType
		agent.setAgentType(Agent.PERSON);

		// Division
		agent.setDivision(getBotanyDivision());

		// FirstName
		String firstName = optr.getFirstName();
		if (firstName != null)
		{
		    firstName = truncate(firstName, 50, "first name");
			agent.setFirstName(firstName);
		}
		
		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		String guid = getGuid(optrId);
		agent.setGuid(guid);

		// LastName
		String lastName = optr.getLastName();
		checkNull(lastName, "last name");
		lastName = truncate(lastName, 50, "last name");
		agent.setLastName(lastName);
	        
		// MiddleInitial (asa record type)
		agent.setMiddleInitial(AgentType.user.name());
		
		// Remarks
		String remarks = optr.getNote();
		agent.setRemarks(remarks);

		return agent;
	}

    private String getGuid(Integer optrId)
    {
    	return optrId + " optr";
    }
    
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, DivisionID, FirstName, GUID, LastName, MiddleInitial, Remarks, TimestampCreated, Version";

		String[] values = {
				SqlUtils.sqlString( agent.getAgentType()),
				SqlUtils.sqlString( agent.getDivision().getId()),
				SqlUtils.sqlString( agent.getFirstName()),
				SqlUtils.sqlString( agent.getGuid()),
				SqlUtils.sqlString( agent.getLastName()),
				SqlUtils.sqlString( agent.getMiddleInitial()),
				SqlUtils.sqlString( agent.getRemarks()),
				SqlUtils.now(),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
}
