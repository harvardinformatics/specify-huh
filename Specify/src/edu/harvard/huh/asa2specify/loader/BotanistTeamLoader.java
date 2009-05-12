package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistTeamMember;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.GroupPerson;

public class BotanistTeamLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(BotanistLoader.class);
	
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
		BotanistTeamMember botanistTeamMember = parseBotanistTeamRecord(columns);

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

        String teamGuid = SqlUtils.sqlString(teamBotanist.getGuid());

        String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", teamGuid);

        Integer groupAgentId = queryForId(sql);

        if (groupAgentId == null)
        {
        	throw new LocalException("Couldn't find AgentID with GUID " + teamGuid);
        }

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

        String guid = SqlUtils.sqlString(botanist.getGuid());

        sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

        Integer personAgentId = queryForId(sql);

        if (personAgentId == null)
        {
        	throw new LocalException("Couldn't find AgentID with GUID " + guid);
        }
        member.setAgentId(botanistId);
        groupPerson.setMember(member);
        
        if (groupAgentId != lastAgentId) {
            orderNumber = 1;
            lastAgentId = groupAgentId;
        }
        else {
            orderNumber++;
        }
        groupPerson.setOrderNumber((short) orderNumber);  // TODO: check sequencing

        // convert agentspecialty to sql and insert
        sql = getInsertSql(groupPerson);
        insert(sql);
	}

    private BotanistTeamMember parseBotanistTeamRecord(String[] columns) throws LocalException
    {
        if (columns.length < 4)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        BotanistTeamMember botanistTeamMember = new BotanistTeamMember();
        
        try {
        	botanistTeamMember.setTeamId(    Integer.parseInt(StringUtils.trimToNull(columns[0])));
        	botanistTeamMember.setBotanistId(Integer.parseInt(StringUtils.trimToNull(columns[1])));
        	botanistTeamMember.setOrdinal(   Integer.parseInt(StringUtils.trimToNull(columns[2])));
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistTeamMember;
    }
    
    public GroupPerson convert(BotanistTeamMember botanistTeamMember)
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
    	
    	List<String> values = new ArrayList<String>(5);
    	
    	values.add(String.valueOf(groupPerson.getGroup().getId()     ));
    	values.add(String.valueOf(groupPerson.getMember().getId()    ));
    	values.add(String.valueOf(groupPerson.getOrderNumber()       ));
    	values.add(String.valueOf(groupPerson.getCollectionMemberId()));
    	values.add("now()");
    	
    	return SqlUtils.getInsertSql("groupperson", fieldNames, values);
    }
}
