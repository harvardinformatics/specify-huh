package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistName;
import edu.harvard.huh.asa.BotanistName.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentVariant;

public class BotanistNameLoader extends CsvToSqlLoader
{
	public BotanistNameLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		BotanistName botanistName = parse(columns);

        // convert BotanistName into AgentVariant
        AgentVariant agentVariant = convert(botanistName);
        
        // find the matching agent record
        Agent agent = new Agent();
        Integer botanistId = botanistName.getBotanistId();

        if (botanistId == null) {
        	throw new LocalException("No botanist id");
        }

        Botanist botanist = new Botanist();
        botanist.setId(botanistId);

        String guid = botanist.getGuid();

        Integer agentId = getIdByField("agent", "AgentID", "GUID", guid);

        agent.setAgentId(agentId);
        agentVariant.setAgent(agent);

        // convert agentvariant to sql and insert
        String sql = getInsertSql(agentVariant);
        insert(sql);
	}
	
    // botanistId, nameType, name
    private BotanistName parse(String[] columns) throws LocalException
    {
        if (columns.length < 3)
        {
            throw new LocalException("Wrong number of columns");
        }

        // assign values to Botanist object
        BotanistName botanistName = new BotanistName();
        
        try {
            botanistName.setBotanistId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
            
            String type = StringUtils.trimToNull( columns[1] );
            if (type == null) throw new LocalException("No type found in record ");

            BotanistName.TYPE nameType;

            if      (type.equals("author name")   ) nameType = BotanistName.TYPE.Author;
            else if (type.equals("author abbrev") ) nameType = BotanistName.TYPE.AuthorAbbrev;
            else if (type.equals("collector name")) nameType = BotanistName.TYPE.Collector;
            else if (type.equals("variant")       ) nameType = BotanistName.TYPE.Variant;
            
            else throw new LocalException("Unrecognized botanist name type: " + type);

            botanistName.setType(nameType);

            String name = StringUtils.trimToNull( columns[2] );
            if (name != null)
            {
                botanistName.setName( name );
            }
            else {
                throw new LocalException("No name found in record");
            }
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistName;
    }

    private AgentVariant convert(BotanistName botanistName) {

        AgentVariant variant = new AgentVariant();
        
        String name = botanistName.getName();
        if (name.length() > 255)
        {
            warn("Truncating botanist name variant", null, name);
            name = name.substring(0, 255);
        }
        variant.setName(botanistName.getName());
        
        Byte varType;
        TYPE nameType = botanistName.getType();
        
        if      (nameType == TYPE.Author      ) varType = AgentVariant.AUTHOR;
        else if (nameType == TYPE.AuthorAbbrev) varType = AgentVariant.AUTHOR_ABBREV;
        else if (nameType == TYPE.Collector   ) varType = AgentVariant.LABLELNAME;
        else if (nameType == TYPE.Variant     ) varType = AgentVariant.VARIANT;
        
        else throw new IllegalArgumentException("Unrecognized BotanistName type");

        variant.setVarType(varType);
        
        return variant;
    }
    
    private String getInsertSql(AgentVariant agentVariant)
    {
        String fieldNames = "AgentID, VarType, Name, TimestampCreated";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( agentVariant.getAgent().getId());
        values[1] = SqlUtils.sqlString( agentVariant.getVarType());
        values[2] = SqlUtils.sqlString( agentVariant.getName());
        values[3] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("agentvariant", fieldNames, values);
    }
 
}
