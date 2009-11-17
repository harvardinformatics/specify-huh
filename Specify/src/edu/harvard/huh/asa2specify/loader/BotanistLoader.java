package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after OptrLoader.

public class BotanistLoader extends AuditedObjectLoader
{	
    private static final Logger log  = Logger.getLogger(BotanistLoader.class);
    
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

    public Logger getLogger()
    {
        return log;
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
        if (columns.length < 15)
        {
            throw new LocalException("Not enough columns");
        }

        Botanist botanist = new Botanist();        
        try
        {
            botanist.setId(           SqlUtils.parseInt( columns[0]  ));
            botanist.setIsTeam(      Boolean.parseBoolean( columns[1]  ));
            botanist.setIsCorporate( Boolean.parseBoolean( columns[2]  ));
            botanist.setName(                            columns[3]  );
            botanist.setDatesType(                       columns[4]  );
            botanist.setStartYear(    SqlUtils.parseInt( columns[5]  ));
            botanist.setStartPrecision(                  columns[6]  );
            botanist.setEndYear(      SqlUtils.parseInt( columns[7]  ));
            botanist.setEndPrecision(                    columns[8]  );
            botanist.setRemarks( SqlUtils.iso8859toUtf8( columns[9]  ));
            botanist.setCreatedById(  SqlUtils.parseInt( columns[10] ));
            botanist.setDateCreated( SqlUtils.parseDate( columns[11] ));
            botanist.setUri(                             columns[12] );
            botanist.setAuthorNote(                      columns[13] );
            botanist.setCollectorNote(                   columns[14] );
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
        
		// CreatedByAgent
        Integer creatorOptrId = botanist.getCreatedById();
        checkNull(creatorOptrId, "created by id");
        
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        agent.setCreatedByAgent(createdByAgent);
        
		// DateOfBirth: this is going to hold start dates no matter what the type for the time being
		Integer startYear = botanist.getStartYear();
		if (startYear != null)
		{
			Calendar dateOfBirth = DateUtils.toCalendar(startYear);
			agent.setDateOfBirth(dateOfBirth);
		}
		
		// DateOfDeath: this is going to hold end dates no matter what the type for the time being
        Integer endYear = botanist.getEndYear();
        if (endYear != null)
        {
            Calendar dateOfDeath = DateUtils.toCalendar(endYear);
            agent.setDateOfDeath(dateOfDeath);
        }
                
        // FirstName
        String name = botanist.getName();
        if (name == null)
        {
            getLogger().warn(rec() + "Empty name");
            name = EMPTY;
            botanist.setName(name);
        }

		String firstName = botanist.getFirstName();
		if (firstName != null)
		{
            firstName = truncate(firstName, 50, "first name");
            agent.setFirstName(firstName);
        }
		
		// GUID: temporarily hold asa botanist.id TODO: don't forget to unset this after migration
		Integer botanistId = botanist.getId();
		checkNull(botanistId, "id");
		
        String guid = getGuid(botanistId);
        agent.setGuid(guid);
        
        // Initials (note: we are using this for the dates type) TODO: enumeration
        String datesType = botanist.getDatesType();
        if (datesType.equals("birth/death"))             agent.setInitials("birth");
        else if (datesType.equals("flourished"))         agent.setInitials("fl");
        else if (datesType.equals("collected"))          agent.setInitials("coll");
        else if (datesType.equals("received specimens")) agent.setInitials("rec");
        
        // LastName
		String lastName = botanist.getLastName();
		lastName = truncate(lastName, 200, "last name");
        agent.setLastName(lastName);

		// Remarks
        String remarks = botanist.getRemarks();
        String authorNote = botanist.getAuthorNote();
        String collectorNote = botanist.getCollectorNote();
        //String datesType = "[dates type: " + botanist.getDatesType() + "]";
        
        if (remarks == null) remarks = "";
        
        if (authorNote != null) remarks = remarks + " [author note: " + authorNote + "]";
        
        if (collectorNote != null) remarks = remarks + " [collector note: " + collectorNote + "]";
        
        agent.setRemarks(remarks);

        // TimestampCreated
        Date dateCreated = botanist.getDateCreated();
        agent.setTimestampCreated(DateUtils.toTimestamp(dateCreated));

        // URL
        String url = botanist.getUri();
        agent.setUrl(url);

        return agent;
	}
	
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "AgentType, CreatedByAgentID, DateOfBirth, DateOfDeath, FirstName, " +
				            "GUID, Initials, LastName, Remarks, TimestampCreated, URL, Version";

		String[] values = new String[12];

		values[0]  = SqlUtils.sqlString( agent.getAgentType());
		values[1]  = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[2]  = SqlUtils.sqlString( agent.getDateOfBirth());
		values[3]  = SqlUtils.sqlString( agent.getDateOfDeath());
		values[4]  = SqlUtils.sqlString( agent.getFirstName());
		values[5]  = SqlUtils.sqlString( agent.getGuid());
		values[6]  = SqlUtils.sqlString( agent.getInitials());
	    values[7]  = SqlUtils.sqlString( agent.getLastName());
	    values[8]  = SqlUtils.sqlString( agent.getRemarks());
		values[9]  = SqlUtils.sqlString( agent.getTimestampCreated());
		values[10] = SqlUtils.sqlString( agent.getUrl());
		values[11] = SqlUtils.zero();

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}

	private String getUpdateSql(Agent agent, Integer agentId) throws LocalException
	{
	    String[] fieldNames = { "AgentType", "CreatedByAgentID","DateOfBirth", "DateOfDeath", "FirstName",
	    		                "GUID", "LastName", "Remarks", "TimestampCreated", "URL" };

	    String[] values = new String[10];

	    values[0] = SqlUtils.sqlString( agent.getAgentType());
	    values[1] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
	    values[2] = SqlUtils.sqlString( agent.getDateOfBirth());
	    values[3] = SqlUtils.sqlString( agent.getDateOfDeath());
	    values[4] = SqlUtils.sqlString( agent.getFirstName());
	    values[5] = SqlUtils.sqlString( agent.getGuid());
	    values[6] = SqlUtils.sqlString( agent.getLastName());
	    values[7] = SqlUtils.sqlString( agent.getRemarks());
	    values[8] = SqlUtils.sqlString( agent.getTimestampCreated());
	    values[9] = SqlUtils.sqlString( agent.getUrl());

	    return SqlUtils.getUpdateSql("agent", fieldNames, values, "AgentID", SqlUtils.sqlString(agentId));
	}
}
