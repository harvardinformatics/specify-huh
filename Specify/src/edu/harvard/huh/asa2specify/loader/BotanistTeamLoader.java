package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistTeamMember;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.GroupPerson;

public class BotanistTeamLoader extends CsvToSqlLoader
{
	private int lastAgentId;
	private int orderNumber;
	
	public BotanistTeamLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
		lastAgentId = 0;
		orderNumber = 1;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		BotanistTeamMember botanistTeamMember = parse(columns);

		// convert botanist_team to groupperson
        GroupPerson groupPerson = convert(botanistTeamMember);
        
        // find the matching agent record for the team
        Agent team = new Agent();
        Integer teamId = botanistTeamMember.getTeamId();

        if (teamId == null)
        {
        	throw new LocalException("No team");
        }
        Botanist teamBotanist = new Botanist();
        teamBotanist.setId(teamId);

        String teamGuid = teamBotanist.getGuid();

        Integer groupAgentId = getIntByField("agent", "AgentID", "GUID", teamGuid);

        team.setAgentId(groupAgentId);
        groupPerson.setGroup(team);
        
        // find the matching agent record for the member
        Agent member = new Agent();
        Integer botanistId = botanistTeamMember.getBotanistId();
        
        if (botanistId == null)
        {
        	throw new LocalException("No botanist");
        }
        Botanist botanist = new Botanist();
        botanist.setId(botanistId);

        String guid = botanist.getGuid();

        Integer personAgentId = getIntByField("agent", "AgentID", "GUID", guid);

        member.setAgentId(personAgentId);
        groupPerson.setMember(member);
        
        if (groupAgentId != lastAgentId)
        {
            orderNumber = 1;
            lastAgentId = groupAgentId;
        }
        else
        {
            orderNumber++;
        }
        groupPerson.setOrderNumber((short) orderNumber);  // TODO: check sequencing

        // CollectionMemberID  TODO: this is a problem; (see botanist id 152426, e.g.)
        // we have more than one group with specimens in no fewer than five different collections
        groupPerson.setCollectionMemberId(Integer.valueOf(0));
        
        // convert agentspecialty to sql and insert
        String sql = getInsertSql(groupPerson);
        insert(sql);
	}

    private BotanistTeamMember parse(String[] columns) throws LocalException
    {
        if (columns.length < 3)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        BotanistTeamMember botanistTeamMember = new BotanistTeamMember();
        
        try
        {
        	botanistTeamMember.setTeamId(    Integer.parseInt(StringUtils.trimToNull(columns[0])));
        	botanistTeamMember.setBotanistId(Integer.parseInt(StringUtils.trimToNull(columns[1])));
        	botanistTeamMember.setOrdinal(   Integer.parseInt(StringUtils.trimToNull(columns[2])));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistTeamMember;
    }
    
    private GroupPerson convert(BotanistTeamMember botanistTeamMember)
    {
        GroupPerson groupPerson = new GroupPerson();
        
        Integer ordinal = botanistTeamMember.getOrdinal();
               
        if (ordinal != null)
        {
        	groupPerson.setOrderNumber((short) ordinal.shortValue());
        }

        return groupPerson;
    }
    
    private String getInsertSql(GroupPerson groupPerson)
    {
    	String fieldNames = "GroupID, MemberID, OrderNumber, CollectionMemberID, TimestampCreated";
    	
    	String[] values = new String[5];
    	
    	values[0] = SqlUtils.sqlString( groupPerson.getGroup().getId());
    	values[1] = SqlUtils.sqlString( groupPerson.getMember().getId());
    	values[2] = SqlUtils.sqlString( groupPerson.getOrderNumber());
    	values[3] = SqlUtils.sqlString( groupPerson.getCollectionMemberId());
    	values[4] = SqlUtils.now();
    	
    	return SqlUtils.getInsertSql("groupperson", fieldNames, values);
    }
}
