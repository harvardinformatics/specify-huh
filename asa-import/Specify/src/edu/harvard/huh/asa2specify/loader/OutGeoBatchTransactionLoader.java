package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.OutGeoBatchTransaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;

public abstract class OutGeoBatchTransactionLoader extends CountableTransactionLoader
{
    public OutGeoBatchTransactionLoader(File csvFile,
                             Statement sqlStatement,
                             BotanistLookup botanistLookup,
                             AffiliateLookup affiliateLookup,
                             AgentLookup agentLookup,
                             OrganizationLookup organizationLookup) throws LocalException
    {
        super(csvFile, sqlStatement, botanistLookup, affiliateLookup, agentLookup, organizationLookup);
    }
        
    protected OutGeoBatchTransactionLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    protected int parse(String[] columns, OutGeoBatchTransaction outGeoBatchTx) throws LocalException
    {        
        int i = super.parse(columns, outGeoBatchTx);
        
        if (columns.length < i + 1)
        {
            throw new LocalException("Not enough columns");
        }
        
        outGeoBatchTx.setGeoUnit( columns[i + 0] );
        
        return i + 1;
    }
}
