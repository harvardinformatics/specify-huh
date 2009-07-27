package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.TaxonBatchTransaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;

// Run this class after AffiliateLoader, AgentLoader, and OrganizationLoader
public abstract class TaxonBatchTransactionLoader extends TransactionLoader
{
    public TaxonBatchTransactionLoader(File csvFile,
                             Statement sqlStatement,
                             BotanistLookup botanistLookup,
                             AffiliateLookup affiliateLookup,
                             AgentLookup agentLookup) throws LocalException
    {
        super(csvFile, sqlStatement, botanistLookup, affiliateLookup, agentLookup);
    }
        
    protected int parse(String[] columns, TaxonBatchTransaction tbTx) throws LocalException
    {        
        
        int i = super.parse(columns, tbTx);
        
        if (columns.length < i + 5)
        {
            throw new LocalException("Not enough columns");
        }
        
        tbTx.setOriginalDueDate( SqlUtils.parseDate( columns[i + 0] ));
        tbTx.setCurrentDueDate(  SqlUtils.parseDate( columns[i + 1] ));           
        tbTx.setHigherTaxon(                         columns[i + 2] );
        tbTx.setTaxon(                               columns[i + 3] );
        tbTx.setTransferredFrom(                     columns[i + 4] );
        
        return i + 5;
    }
}
