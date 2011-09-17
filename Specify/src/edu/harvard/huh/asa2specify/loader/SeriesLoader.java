package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Series;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.SeriesLookup;
import edu.ku.brc.specify.datamodel.Agent;

// Run this class after BotanistLoader and before OrganizationLoader.

public class SeriesLoader extends CsvToSqlLoader
{
    private SeriesLookup seriesLookup;
        
    static String getGuid(Integer seriesId)
    {
        return seriesId + " series";
    }
    
    public SeriesLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
		super(csvFile, sqlStatement);
	}

    public SeriesLookup getSeriesLookup()
    {
        if (seriesLookup == null)
        {
            seriesLookup = new SeriesLookup() {
                public Agent getById(Integer seriesId) throws LocalException
                {
                    Agent agent = new Agent(); // TODO: this doesn't account for affiliate botanists
                    
                    String guid = getGuid(seriesId);
                    
                    Integer agentId = getId("agent", "AgentID", "GUID", guid);

                    agent.setAgentId(agentId);
                    
                    return agent;
                }
            };
        }
        return seriesLookup;
    }
    @Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Series series = parse(columns);

		Integer seriesId = series.getId();
		setCurrentRecordId(seriesId);
		
        Agent seriesAgent = getAgent(series);

        String sql = getInsertSql(seriesAgent);
        insert(sql);   
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
		
		name = truncate(name, 120, "name");
		agent.setLastName(name);

		// MiddleInitial
		agent.setMiddleInitial(AgentType.series.name());
		
		// Remarks
		String note = series.getNote();
		agent.setRemarks(note);
		
		return agent;
	}
	
    private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "Abbreviation, AgentType, GUID, LastName, MiddleInitial, Remarks, TimestampCreated, Version";

		String[] values = {
				SqlUtils.sqlString( agent.getAbbreviation()),
				SqlUtils.sqlString( agent.getAgentType()),
				SqlUtils.sqlString( agent.getGuid()),
				SqlUtils.sqlString( agent.getLastName()),
				SqlUtils.sqlString( agent.getRemarks()),
				SqlUtils.now(),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("agent", fieldNames, values);    
	}
}
