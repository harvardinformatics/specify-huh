package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.BotanistRoleCitation;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentCitation;
import edu.ku.brc.specify.datamodel.ReferenceWork;

// Run this class after BotanistLoader and GeoUnitLoader.

public class BotanistCitationLoader extends CsvToSqlLoader
{
    // lookups for geography
    private PublicationLookup pubLookup;
    private BotanistLookup  botanistLookup;
    
    private static final ReferenceWork NullRefWork = new ReferenceWork();
    
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

		// convert BotanistRoleCountry into AgentGeography
		AgentCitation agentCitation = getAgentCitation(botanistRoleCitation);

		// convert agentgeography to sql and insert
		String sql = getInsertSql(agentCitation);
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

	private AgentCitation getAgentCitation(BotanistRoleCitation botanistRoleCitation)
		throws LocalException
	{
		AgentCitation agentCitation = new AgentCitation();

		// Agent
		Integer botanistId = botanistRoleCitation.getBotanistId();
		checkNull(botanistId, "botanist id");

		Agent agent = lookupBotanist(botanistId);
		agentCitation.setAgent(agent);

		// ReferenceWork
		ReferenceWork referenceWork = NullRefWork;
		
		Integer publicationId = botanistRoleCitation.getPublicationId();
		if (publicationId != null)
		{
		    referenceWork = lookupReferenceWork(publicationId);
		}
		
		agentCitation.setReferenceWork(referenceWork);
		
		// Role
		String role = botanistRoleCitation.getRole();
		checkNull(role, "role");
		role = truncate(role, 64, "role");
		agentCitation.setText1(role);

		// Text1 (collation)
		String collation = botanistRoleCitation.getCollation();
		agentCitation.setText1(collation);
		
		// Text2 (publ date)
		String publDate = botanistRoleCitation.getPublDate();
		agentCitation.setText2(publDate);
		
		// Remarks (alt source)
		String altSource = botanistRoleCitation.getAltSource();
		agentCitation.setRemarks(altSource);

		return agentCitation;
	}

	private ReferenceWork lookupReferenceWork(Integer publicationId) throws LocalException
	{
	    return pubLookup.getById(publicationId);
	}
    
	private Agent lookupBotanist(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }
  
	private String getInsertSql(AgentCitation agentCitation)
	{
		String fieldNames = "AgentId, ReferenceWorkID, Remarks, Role, Text1, Text2, " +
				            "TimestampCreated, Version";

		String[] values = new String[8];

		values[0] = SqlUtils.sqlString( agentCitation.getAgent().getId());
		values[1] = SqlUtils.sqlString( agentCitation.getReferenceWork().getId());
		values[2] = SqlUtils.sqlString( agentCitation.getRemarks());
		values[3] = SqlUtils.sqlString( agentCitation.getRole());
		values[4] = SqlUtils.sqlString( agentCitation.getText1());
		values[5] = SqlUtils.sqlString( agentCitation.getText2());
		values[6] = SqlUtils.now();
		values[7] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("agentcitation", fieldNames, values);
	}

}
