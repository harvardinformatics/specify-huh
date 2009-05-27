package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Optr;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class first.
public class OptrLoader extends CsvToSqlLoader
{   
    private String getGuid(Integer optrId)
    {
    	return optrId + " optr";
    }
 
    private static Hashtable<Integer, Agent> agentsByOptrId = new Hashtable<Integer, Agent>();

    private static AsaIdMapper botanistsByOptr;

	public OptrLoader(File csvFile, Statement specifySqlStatement, File optrToBotanists) throws LocalException
	{
		super(csvFile, specifySqlStatement);

		botanistsByOptr = new AsaIdMapper(optrToBotanists);
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
			throw new LocalException("Wrong number of columns");
		}

		Optr optr = new Optr();
		try
		{
		    optr.setId(           SqlUtils.parseInt( StringUtils.trimToNull( columns[0] )));
		    optr.setUserName(                        StringUtils.trimToNull( columns[1] ));
		    optr.setFullName(                        StringUtils.trimToNull( columns[2] ));
		    optr.setRemarks( SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[3] )));
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
		String remarks = optr.getRemarks();
		agent.setRemarks(remarks);

		return agent;
	}

	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, FirstName, GUID, LastName, Remarks, TimestampCreated";

		String[] values = new String[6];

		values[0] = SqlUtils.sqlString( agent.getAgentType());
		values[1] = SqlUtils.sqlString( agent.getFirstName());
		values[2] = SqlUtils.sqlString( agent.getGuid());
		values[3] = SqlUtils.sqlString( agent.getLastName());
		values[4] = SqlUtils.sqlString( agent.getRemarks());
		values[5] = SqlUtils.now();

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
	// Agent records that represent Optrs who are also Botanists will first be loaded
    // as Optrs, which puts the Optr id in the Agent GUID field.  During Botanist
    // loading, the Agent records for Optr-Botanists are updated to put the Botanist
    // id in the Agent GUID field.  That means the guid-- the means by which we get
    // those Agent records-- may change during Botanist loading.
	Agent getAgentByOptrId(Integer optrId) throws LocalException
    {
        Agent agent = agentsByOptrId.get(optrId);

        if (agent == null)
        {
            String guid = getGuid(optrId);
            // TODO: move this to interface
            Integer agentId = queryForInt("agent", "AgentID", "GUID", guid);
            
            if (agentId == null)
            {
                Integer botanistId = getBotanistId(optrId);

                if (botanistId == null)
                {
                    throw new LocalException("Agent not found for optr id " + optrId);
                }
                guid = null; //BotanistLoader.getGuid(botanistId);
                agentId = getIntByField("agent", "AgentID", "GUID", guid);
            }
            agent = new Agent();
            agent.setAgentId(agentId);
            agentsByOptrId.put(optrId, agent);
        } 
        
        return agent;
    }
    
    private Integer getBotanistId(Integer optrId)
    {
        return botanistsByOptr.map(optrId);
    }
}
