package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.BotanistRoleCitation;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ReferenceWork;

// Run this class after BotanistLoader and GeoUnitLoader.

public class BotanistCitationLoader extends CsvToSqlLoader
{
    // lookups for geography
    private PublicationLookup pubLookup;
    private BotanistLookup  botanistLookup;
    
	public BotanistCitationLoader(File csvFile,
	                             Statement sqlStatement,
	                             PublicationLookup pubLookup,
	                             BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.pubLookup = pubLookup;
		this.botanistLookup = botanistLookup;
	}
  
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistRoleCitation botanistRoleCitation = parse(columns);
		
		Integer botanistId = botanistRoleCitation.getBotanistId();
		setCurrentRecordId(botanistId);

		checkNull(botanistId, "botanist id");

		Agent agent = lookupBotanist(botanistId);

		// convert BotanistRoleCountry into AgentGeography
		String agentCitation = getAgentCitation(botanistRoleCitation);

		// convert agentgeography to sql and insert
		String sql = getUpdateSql(agent, agentCitation);
		insert(sql);
	}

	private BotanistRoleCitation parse(String[] columns) throws LocalException
	{
		if (columns.length < 7)
		{
			throw new LocalException("Not enough columns");
		}

		BotanistRoleCitation botanistRoleCitation = new BotanistRoleCitation();
		try
		{
		    botanistRoleCitation.setBotanistId(    SqlUtils.parseInt( columns[0] ));
		    botanistRoleCitation.setRole(                             columns[1] );
		    botanistRoleCitation.setPublicationId( SqlUtils.parseInt( columns[2] ));
		    botanistRoleCitation.setCollation(                        columns[3] );
		    botanistRoleCitation.setPublDate(                         columns[4] );
		    botanistRoleCitation.setAltSource(                        columns[5] );
		    botanistRoleCitation.setOrdinal(       SqlUtils.parseInt( columns[6] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		
		return botanistRoleCitation;
	}

	private String getAgentCitation(BotanistRoleCitation botanistRoleCitation)
		throws LocalException
	{
		// ReferenceWork
		String title = "";
		
		Integer publicationId = botanistRoleCitation.getPublicationId();
		if (publicationId != null)
		{
		    ReferenceWork referenceWork = lookupReferenceWork(publicationId);
		    title = this.getString("referencework", "Title", "ReferenceWorkID", referenceWork.getId());
		}

		String collation = botanistRoleCitation.getCollation();
		String publDate = botanistRoleCitation.getPublDate();
		String altSource = botanistRoleCitation.getAltSource();
		String role = "(" + botanistRoleCitation.getRole() + ")";
		
		return denormalize("cited in", concatenate(title, collation, publDate, altSource, role));
	}

	private ReferenceWork lookupReferenceWork(Integer publicationId) throws LocalException
	{
	    return pubLookup.getById(publicationId);
	}
    
	private Agent lookupBotanist(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }
  
	private String getUpdateSql(Agent agent, String agentCitation)
    {        
    	return SqlUtils.getAppendUpdateSql("agent", "Remarks", agentCitation, "AgentID", agent.getId());
    }

}
