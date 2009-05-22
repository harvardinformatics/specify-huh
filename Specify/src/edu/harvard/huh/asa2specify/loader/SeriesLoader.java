package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Series;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;

public class SeriesLoader extends CsvToSqlLoader
{
    private AsaIdMapper seriesToBotanistMapper;
    private AsaIdMapper seriesToOrgMapper;
    
    public SeriesLoader(File csvFile, Statement sqlStatement, File seriesToBotanist, File seriesToOrg) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.seriesToBotanistMapper = new AsaIdMapper(seriesToBotanist);
		this.seriesToOrgMapper = new AsaIdMapper(seriesToOrg);
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Series series = parse(columns);

        // convert series into agent, type=org ...
        Agent agent = convert(series);

        // find matching botanist
        Integer seriesAgentId = null;
        Integer botanistId = getBotanistId(series.getId());
        Integer organizationId = getOrganizationId(series.getId());
        
        if (botanistId != null)
        {
            Botanist botanist = new Botanist();
            botanist.setId(botanistId);
            String guid = botanist.getGuid();

            seriesAgentId = queryForInt("agent", "AgentID", "GUID", guid);
        }
        else
        {
            if (organizationId != null)
            {
                Organization organization = new Organization();
                organization.setId(organizationId);
                String guid = organization.getGuid();

                seriesAgentId = queryForInt("agent", "AgentID", "GUID", guid);
            }
        }
        
        if (seriesAgentId == null)
        {
            String sql = getInsertSql(agent);
            seriesAgentId = insert(sql);
            agent.setAgentId(seriesAgentId);
        }
        else
        {
            if (botanistId != null)
            {
                if (agent.getRemarks() != null)
                {
                    warn("Ignoring remarks", series.getId(), agent.getRemarks());
                }
                String sql = getUpdateSql(agent, seriesAgentId);
                update(sql);
            }
            else if (organizationId != null)
            {
                warn("Series already present as organization", series.getId(), "org id: " + organizationId);
            }
        }        
	}

	private Integer getBotanistId(Integer seriesId)
	{
	    return seriesToBotanistMapper.map(seriesId);
	}
	
	private Integer getOrganizationId(Integer seriesId)
	{
	    return seriesToOrgMapper.map(seriesId);
	}

    private Series parse(String[] columns) throws LocalException
    {
    	if (columns.length < 5)
    	{
    		throw new LocalException("Wrong number of columns");
    	}

    	Series series = new Series();

    	try {
    		series.setId(            Integer.parseInt( StringUtils.trimToNull( columns[0] )));
    		series.setName(                            StringUtils.trimToNull( columns[1] ));
    		series.setAbbreviation(                    StringUtils.trimToNull( columns[2] ));
    		
    		String instIdStr =                         StringUtils.trimToNull( columns[3] );
    		if (instIdStr != null)
    		{
    		    series.setInstitutionId(Integer.parseInt(instIdStr));
    		}

    		series.setNote(                            StringUtils.trimToNull( columns[4] ));
    	}
    	catch (NumberFormatException e) {
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	
    	return series;
    }
    

    private Agent convert(Series series) throws LocalException {
        
        Agent agent = new Agent();
		
        agent.setAgentType(Agent.ORG);

        agent.setGuid(series.getGuid());
		
		String title = series.getName();
		if (title == null)
		{
			throw new LocalException("No title");
		}
		agent.setLastName(title);
		
		agent.setAbbreviation(series.getAbbreviation());

		agent.setRemarks(series.getNote());
		
		return agent;
	}
    
    private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "GUID, AgentType, LastName, Abbreviation, TimestampCreated, Remarks";

		String[] values = new String[6];

		values[0] = SqlUtils.sqlString( agent.getGuid());
		values[1] = SqlUtils.sqlString( agent.getAgentType());
		values[2] = SqlUtils.sqlString( agent.getLastName());
		values[3] = SqlUtils.sqlString( agent.getAbbreviation());
		values[4] = SqlUtils.now();
		values[5] = SqlUtils.sqlString( agent.getRemarks());

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
