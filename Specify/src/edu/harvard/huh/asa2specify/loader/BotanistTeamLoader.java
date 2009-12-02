package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.BotanistTeamMember;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.GroupPerson;

// Run this class after BotanistLoader.

public class BotanistTeamLoader extends CsvToSqlLoader
{
    private static final Logger log  = Logger.getLogger(BotanistTeamLoader.class);
    
    private BotanistLookup botanistLookup;
    
	private int lastAgentId;
	private int orderNumber;
	
	public BotanistTeamLoader(File csvFile,
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
		BotanistTeamMember botanistTeamMember = parse(columns);

		Integer botanistId = botanistTeamMember.getBotanistId();
		setCurrentRecordId(botanistId);
		
		// convert botanist_team to groupperson
        GroupPerson groupPerson = getGroupPerson(botanistTeamMember);
        
        // convert agentspecialty to sql and insert
        String sql = getInsertSql(groupPerson);
        insert(sql);
	}

	public Logger getLogger()
	{
	    return log;
	}

	private BotanistTeamMember parse(String[] columns) throws LocalException
    {
        if (columns.length < 3)
        {
            throw new LocalException("Not enough columns");
        }
        
        BotanistTeamMember botanistTeamMember = new BotanistTeamMember();
        try
        {
        	botanistTeamMember.setTeamId(     SqlUtils.parseInt( columns[0] ));
        	botanistTeamMember.setBotanistId( SqlUtils.parseInt( columns[1] ));
        	botanistTeamMember.setOrdinal(    SqlUtils.parseInt( columns[2] ));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistTeamMember;
    }
    
    private GroupPerson getGroupPerson(BotanistTeamMember botanistTeamMember) throws LocalException
    {
        GroupPerson groupPerson = new GroupPerson();
 
        // Group
        Integer teamId = botanistTeamMember.getTeamId();
        checkNull(teamId, "team id");
        
        Agent group = lookup(teamId);
        groupPerson.setGroup(group);
        
        // Member
        Integer botanistId = botanistTeamMember.getBotanistId();
        checkNull(botanistId, "botanist id");
        
        Agent member = lookup(botanistId);
        groupPerson.setMember(member);
        
        // Ordinal
        if (group.getId() != lastAgentId)
        {
            orderNumber = 1;
            lastAgentId = group.getId();
        }
        else
        {
            orderNumber++;
        }
        groupPerson.setOrderNumber((short) orderNumber);

        return groupPerson;
    }

    private Agent lookup(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }
   
    private String getInsertSql(GroupPerson groupPerson)
    {
    	String fieldNames = "GroupID, MemberID, OrderNumber, TimestampCreated, Version";
    	
    	String[] values = new String[5];
    	
    	values[0] = SqlUtils.sqlString( groupPerson.getGroup().getId());
    	values[1] = SqlUtils.sqlString( groupPerson.getMember().getId());
    	values[2] = SqlUtils.sqlString( groupPerson.getOrderNumber());
    	values[3] = SqlUtils.now();
    	values[4] = SqlUtils.zero();
    	
    	return SqlUtils.getInsertSql("groupperson", fieldNames, values);
    }
}
