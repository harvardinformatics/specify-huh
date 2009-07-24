package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Optr;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class first.
public class OptrLoader extends CsvToSqlLoader
{    
    private static final Logger log  = Logger.getLogger(OptrLoader.class);
    
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

    public Logger getLogger()
    {
        return log;
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
			"AgentType, FirstName, GUID, LastName, Remarks, TimestampCreated, Version";

		String[] values = new String[7];

		values[0] = SqlUtils.sqlString( agent.getAgentType());
		values[1] = SqlUtils.sqlString( agent.getFirstName());
		values[2] = SqlUtils.sqlString( agent.getGuid());
		values[3] = SqlUtils.sqlString( agent.getLastName());
		values[4] = SqlUtils.sqlString( agent.getRemarks());
		values[5] = SqlUtils.now();
		values[6] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
}
