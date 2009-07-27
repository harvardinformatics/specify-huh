package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.CountableTransaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;

public abstract class CountableTransactionLoader extends TransactionLoader
{
    public CountableTransactionLoader(File csvFile,
                                      Statement sqlStatement,
                                      BotanistLookup botanistLookup,
                                      AffiliateLookup affiliateLookup,
                                      AgentLookup agentLookup) throws LocalException
    {
        super(csvFile, sqlStatement, botanistLookup, affiliateLookup, agentLookup);
    }
        
    protected CountableTransactionLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    protected int parse(String[] columns, CountableTransaction countableTx) throws LocalException
    {        
        int i = super.parse(columns, countableTx);
        
        if (columns.length < i + 3)
        {
            throw new LocalException("Not enough columns");
        }
        
        countableTx.setItemCount(        SqlUtils.parseInt( columns[i + 0] ));
        countableTx.setTypeCount(        SqlUtils.parseInt( columns[i + 1] ));
        countableTx.setNonSpecimenCount( SqlUtils.parseInt( columns[i + 2] ));
        
        return i + 3;
    }
}
