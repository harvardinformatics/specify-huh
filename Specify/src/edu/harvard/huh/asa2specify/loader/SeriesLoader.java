package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Series;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after BotanistLoader and before OrganizationLoader.

public class SeriesLoader extends CsvToSqlLoader
{
	private AsaIdMapper seriesToBotanistMapper;
    private BotanistLookup botanistLookup;
    
    public SeriesLoader(File csvFile,
                        Statement sqlStatement,
                        File seriesToBotanist,
                        BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.seriesToBotanistMapper = new AsaIdMapper(seriesToBotanist);
		this.botanistLookup = botanistLookup;
	}

    @Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Series series = parse(columns);

		Integer seriesId = series.getId();
		setCurrentRecordId(seriesId);
		
        Agent seriesAgent = getAgent(series);

        Integer botanistId = getBotanistId(series.getId());
        
        if (botanistId != null) // preserve botanist id in guid
        {
            Agent botanistAgent = lookup(botanistId);
            
            if (seriesAgent.getRemarks() != null)
            {
            	warn("Ignoring remarks", seriesAgent.getRemarks());
            }

            String sql = getUpdateSql(seriesAgent, botanistAgent.getId());
            update(sql);
        }
        else
        {
            String sql = getInsertSql(seriesAgent);
            insert(sql);
        }      
	}
    
    private Agent lookup(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }

	private Integer getBotanistId(Integer seriesId)
	{
	    return seriesToBotanistMapper.map(seriesId);
	}

    private Series parse(String[] columns) throws LocalException
    {
    	if (columns.length < 5)
    	{
    		throw new LocalException("Not enough columns");
    	}

    	Series series = new Series();
    	try
    	{
    		series.setId(            SqlUtils.parseInt( columns[0] ));
    		series.setName(                             columns[1] );
    		series.setAbbreviation(                     columns[2] );
    		series.setInstitutionId( SqlUtils.parseInt( columns[3] ));
    		series.setNote(                             columns[4] );
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	
    	return series;
    }
    

    private Agent getAgent(Series series) throws LocalException
    {
        Agent agent = new Agent();
		
		// Abbreviation
		String abbreviation = series.getAbbreviation();
		if (abbreviation != null)
		{
			abbreviation = truncate(abbreviation, 50, "abbreviation");
			agent.setAbbreviation(series.getAbbreviation());
		}
		
		// AgentType
        agent.setAgentType(Agent.ORG);

        // GUID
    	Integer seriesId = series.getId();
    	checkNull(seriesId, "id");
    	
        String guid = getGuid(seriesId);
        agent.setGuid(guid);
		
        // LastName
		String name = series.getName();
		checkNull(name, "name");
		
		name = truncate(name, 50, "name");
		agent.setLastName(name);

		// Remarks
		String note = series.getNote();
		agent.setRemarks(note);
		
		return agent;
	}
    
	private String getGuid(Integer seriesId)
	{
		return seriesId + " series";
	}
	
    private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "Abbreviation, AgentType, GUID, LastName, Remarks, TimestampCreated";

		String[] values = new String[6];

		values[0] = SqlUtils.sqlString( agent.getAbbreviation());
		values[1] = SqlUtils.sqlString( agent.getAgentType());
		values[2] = SqlUtils.sqlString( agent.getGuid());
		values[3] = SqlUtils.sqlString( agent.getLastName());
		values[4] = SqlUtils.sqlString( agent.getRemarks());
		values[5] = SqlUtils.now();

		return SqlUtils.getInsertSql("agent", fieldNames, values);    
	}
    
    private String getUpdateSql(Agent agent, Integer agentId) throws LocalException
    {
        String[] fieldNames = { "Abbreviation" };

        String[] values = new String[1];

        values[0] = SqlUtils.sqlString( agent.getAbbreviation());

        return SqlUtils.getUpdateSql("agent", fieldNames, values, "AgentID", String.valueOf(agentId));
    }
}
