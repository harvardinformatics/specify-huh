package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace.PartialDateEnum;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after OptrLoader.

public class BotanistLoader extends AuditedObjectLoader
{	
    private BotanistLookup botanistLookup;
    private AsaIdMapper optrs;
    
    public BotanistLoader(File csvFile,
                          Statement sqlStatement,
                          File botanistOptrs,
                          File optrBotanists,
                          OptrLookup optrLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.optrs = new AsaIdMapper(botanistOptrs);
		
		setOptrBotanistMapper(new AsaIdMapper(optrBotanists));
		setOptrLookup(optrLookup);
        setBotanistLookup(getBotanistLookup());
	}
    
    public BotanistLookup getBotanistLookup()
    {
        if (botanistLookup == null)
        {
            botanistLookup = new BotanistLookup() {

                public Agent getById(Integer botanistId) throws LocalException
                {
                    Agent agent = new Agent();

                    String guid = getGuid(botanistId);

                    Integer agentId = getId("agent", "AgentID", "GUID", guid);

                    agent.setAgentId(agentId);
                    
                    return agent;
                }
            };
        }
        return botanistLookup;
    }
    
    public void loadRecord(String[] columns) throws LocalException
	{
		Botanist botanist = parse(columns);

		Integer botanistId = botanist.getId();
		setCurrentRecordId(botanistId);
		
        // convert botanist into agent ...
        Agent botanistAgent = getAgent(botanist);
        
        // convert agent to sql and insert or update (if there was an optr record for this botanist)
        Integer optrId = getOptrId(botanistId);
        
        if (optrId == null)
        {
            String sql = getInsertSql(botanistAgent);
            insert(sql);
        }
        else
        {
        	// merge the botanist and optr records; new guid as a botanist.
            Agent optrAgent = getAgentByOptrId(optrId);
            
            String sql = getUpdateSql(botanistAgent, optrAgent.getId());
            update(sql);
        }
	}

    protected static String getGuid(Integer botanistId)
    {
    	return botanistId + " botanist";
    }

	private Integer getOptrId(Integer botanistId)
	{
		return optrs.map(botanistId);
	}

    // id, isTeam, isCorporate, name, datesType, startYear, startPrecision, endYear, endPrecision, remarks
    private Botanist parse(String[] columns) throws LocalException
    {
        Botanist botanist = new Botanist();
        
        int i = super.parse(columns, botanist);
        
        if (columns.length < i + 12)
        {
            throw new LocalException("Not enough columns");
        }
        
        try
        {
            botanist.setIsTeam(      Boolean.parseBoolean( columns[i + 0]  ));
            botanist.setIsCorporate( Boolean.parseBoolean( columns[i + 1]  ));
            botanist.setName(                              columns[i + 2]  );
            botanist.setDatesType(                         columns[i + 3]  );
            botanist.setStartYear(      SqlUtils.parseInt( columns[i + 4]  ));
            botanist.setStartPrecision(                    columns[i + 5]  );
            botanist.setEndYear(        SqlUtils.parseInt( columns[i + 6]  ));
            botanist.setEndPrecision(                      columns[i + 7]  );
            botanist.setRemarks(   SqlUtils.iso8859toUtf8( columns[i + 8]  ));
            botanist.setUri(                               columns[i + 9]  );
            botanist.setAuthorNote(                        columns[i + 10] );
            botanist.setCollectorNote(                     columns[i + 11] );
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanist;
    }

    private Agent getAgent(Botanist botanist) throws LocalException
    {    
		Agent agent = new Agent();

		// NOTE: as of 11:46 am Feb 27 2009, each botanist record that lacks a full name
		// has both an author name and collector name and they are equal; further, the
		// author/collector name contains an ampersand (and a positive team flag)
        
		// AgentType
        if (botanist.isOrganization()) agent.setAgentType( Agent.ORG );
        else if (botanist.isGroup())   agent.setAgentType( Agent.GROUP );
        else                           agent.setAgentType( Agent.PERSON );
                
		// DateOfBirth: this is going to hold start dates no matter what the type for the time being
		Integer startYear = botanist.getStartYear();
		if (startYear != null)
		{
			Calendar dateOfBirth = DateUtils.toCalendar(startYear);
			agent.setDateOfBirth(dateOfBirth);
		}
		
		// DateOfBirthConfidence
		String startConfidence = botanist.getStartPrecision();
		agent.setDateOfBirthConfidence(startConfidence);
		
		// DateOfBirthPrecision
		byte preciseToYear = (byte) PartialDateEnum.Year.ordinal();
		if (startYear != null) agent.setDateOfBirthPrecision(preciseToYear);
		
		// DateOfDeath: this is going to hold end dates no matter what the type for the time being
        Integer endYear = botanist.getEndYear();
        if (endYear != null)
        {
            Calendar dateOfDeath = DateUtils.toCalendar(endYear);
            agent.setDateOfDeath(dateOfDeath);
        }
                
        // DateOfDeathConfidence
        String endConfidence = botanist.getEndPrecision();
        agent.setDateOfDeathConfidence(endConfidence);
        
        // DateOfDeathPrecision
        if (endYear != null) agent.setDateOfDeathPrecision(preciseToYear);
        
        // DatesType
        String datesType = botanist.getDatesType();
        if (datesType.equals("birth/death"))             agent.setDatesType(Agent.BIRTH);
        else if (datesType.equals("flourished"))         agent.setDatesType(Agent.FLOURISHED);
        else if (datesType.equals("collected"))          agent.setDatesType(Agent.COLLECTED);
        else if (datesType.equals("received specimens")) agent.setDatesType(Agent.RECEIVED_SPECIMENS);
        
		// GUID: temporarily hold asa botanist.id TODO: don't forget to unset this after migration
		Integer botanistId = botanist.getId();
		checkNull(botanistId, "id");
		
        String guid = getGuid(botanistId);
        agent.setGuid(guid);
        
		// Remarks
        String remarks = botanist.getRemarks();
        String authorNote = botanist.getAuthorNote();
        String collectorNote = botanist.getCollectorNote();
        //String datesType = "[dates type: " + botanist.getDatesType() + "]";
        
        if (remarks == null) remarks = "";
        
        if (authorNote != null) remarks = remarks + " [author note: " + authorNote + "]";
        
        if (collectorNote != null) remarks = remarks + " [collector note: " + collectorNote + "]";
        
        agent.setRemarks(remarks);
        
        // URL
        String url = botanist.getUri();
        agent.setUrl(url);

        setAuditFields(botanist, agent);

        return agent;
	}
	
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "AgentType, CreatedByAgentID, DateOfBirth, DateOfBirthConfidence, " +
				            "DateOfBirthPrecision, DateOfDeath, DateOfDeathConfidence, DateOfDeathPrecision, " +
				            "DatesType, GUID, ModifiedByAgentID, Remarks, TimestampCreated, " +
				            "TimestampModified, URL, Version";

		String[] values = new String[16];

		values[0]  = SqlUtils.sqlString( agent.getAgentType());
		values[1]  = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[2]  = SqlUtils.sqlString( agent.getDateOfBirth());
		values[3]  = SqlUtils.sqlString( agent.getDateOfBirthConfidence());
		values[4]  = SqlUtils.sqlString( agent.getDateOfBirthPrecision());
		values[5]  = SqlUtils.sqlString( agent.getDateOfDeath());
		values[6]  = SqlUtils.sqlString( agent.getDateOfDeathConfidence());
		values[7]  = SqlUtils.sqlString( agent.getDateOfDeathPrecision());
		values[8]  = SqlUtils.sqlString( agent.getDatesType());
		values[9]  = SqlUtils.sqlString( agent.getGuid());
	    values[10] = SqlUtils.sqlString( agent.getModifiedByAgent().getId());
	    values[11] = SqlUtils.sqlString( agent.getRemarks());
		values[12] = SqlUtils.sqlString( agent.getTimestampCreated());
		values[13] = SqlUtils.sqlString( agent.getTimestampModified());
		values[14] = SqlUtils.sqlString( agent.getUrl());
		values[15] = SqlUtils.one();

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}

	private String getUpdateSql(Agent agent, Integer agentId) throws LocalException
	{
	    String[] fieldNames = { "AgentType", "CreatedByAgentID", "DateOfBirth", "DateOfBirthConfidence",
	                            "DateOfBirthPrecision", "DateOfDeath", "DateOfDeathConfidence",
	                            "DateOfDeathPrecision", "DatesType", "GUID", "ModifiedByAgentID",
	                            "Remarks", "TimestampCreated", "TimestampModified", "URL" };

	    String[] values = new String[15];

	    values[0]  = SqlUtils.sqlString( agent.getAgentType());
	    values[1]  = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
	    values[2]  = SqlUtils.sqlString( agent.getDateOfBirth());
	    values[3]  = SqlUtils.sqlString( agent.getDateOfBirthConfidence());
	    values[4]  = SqlUtils.sqlString( agent.getDateOfBirthPrecision());
	    values[5]  = SqlUtils.sqlString( agent.getDateOfDeath());
	    values[6]  = SqlUtils.sqlString( agent.getDateOfDeathConfidence());
	    values[7]  = SqlUtils.sqlString( agent.getDateOfDeathPrecision());
	    values[8]  = SqlUtils.sqlString( agent.getDatesType());
	    values[9]  = SqlUtils.sqlString( agent.getGuid());
	    values[10] = SqlUtils.sqlString( agent.getModifiedByAgent().getId());
	    values[11] = SqlUtils.sqlString( agent.getRemarks());
	    values[12] = SqlUtils.sqlString( agent.getTimestampCreated());
	    values[13] = SqlUtils.sqlString( agent.getTimestampModified());
	    values[14] = SqlUtils.sqlString( agent.getUrl());

	    return SqlUtils.getUpdateSql("agent", fieldNames, values, "AgentID", SqlUtils.sqlString(agentId));
	}
}
