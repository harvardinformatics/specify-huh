package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BotanistName;
import edu.harvard.huh.asa.BotanistName.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentVariant;

// Run this class after BotanistLoader.

public class BotanistNameLoader extends CsvToSqlLoader
{
    private BotanistLookup botanistLookup;
    
	public BotanistNameLoader(File csvFile, Statement sqlStatement, BotanistLookup botanistLookup)
	    throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.botanistLookup = botanistLookup;
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistName botanistName = parse(columns);

		Integer botanistId = botanistName.getBotanistId();
		setCurrentRecordId(botanistId);
		
        // convert BotanistName into AgentVariant
        AgentVariant agentVariant = getAgentVariant(botanistName);

        // convert agentvariant to sql and insert
        String sql = getInsertSql(agentVariant);
        insert(sql);
	}
	
    // botanistId, nameType, name
    private BotanistName parse(String[] columns) throws LocalException
    {
        if (columns.length < 3)
        {
            throw new LocalException("Not enough columns");
        }

        BotanistName botanistName = new BotanistName();        
        try
        {
            botanistName.setBotanistId( SqlUtils.parseInt( columns[0] ));
            botanistName.setType(  BotanistName.parseType( columns[1] ));
            botanistName.setName(                          columns[2] );

        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
        	throw new LocalException("Couldn't parse name type", e);
        }

        return botanistName;
    }

    private AgentVariant getAgentVariant(BotanistName botanistName) throws LocalException
    {
        AgentVariant agentVariant = new AgentVariant();
        
     	// Agent
        Integer botanistId = botanistName.getBotanistId();
        checkNull(botanistId, "id");

        Agent agent = lookup(botanistId);
        agentVariant.setAgent(agent);

        // Name
        String name = botanistName.getName();
        checkNull(name, "name");

        name = truncate(name, 255, "name");
        agentVariant.setName(botanistName.getName());
        
        // Type
        Byte varType;
        
        TYPE nameType = botanistName.getType();
        checkNull(nameType, "type");
        
        if      (nameType == TYPE.AuthorName  ) varType = AgentVariant.AUTHOR;
        else if (nameType == TYPE.AuthorAbbrev) varType = AgentVariant.AUTHOR_ABBREV;
        else if (nameType == TYPE.Collector   ) varType = AgentVariant.LABLELNAME;
        else if (nameType == TYPE.Variant     ) varType = AgentVariant.VARIANT;
        
        else throw new IllegalArgumentException("Invalid BotanistName type");

        agentVariant.setVarType(varType);
        
        return agentVariant;
    }
	
    private Agent lookup(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }

    private String getInsertSql(AgentVariant agentVariant)
    {
        String fieldNames = "AgentID, Name, VarType, TimestampCreated";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( agentVariant.getAgent().getId());
        values[1] = SqlUtils.sqlString( agentVariant.getName());
        values[2] = SqlUtils.sqlString( agentVariant.getVarType());
        values[3] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("agentvariant", fieldNames, values);
    }
 
}
