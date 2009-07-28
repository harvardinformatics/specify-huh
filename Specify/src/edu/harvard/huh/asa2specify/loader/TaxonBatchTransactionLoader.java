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
public abstract class TaxonBatchTransactionLoader extends CountableTransactionLoader
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
        
        if (columns.length < i + 6)
        {
            throw new LocalException("Not enough columns");
        }
        
        tbTx.setOriginalDueDate( SqlUtils.parseDate(      columns[i + 0] ));
        tbTx.setCurrentDueDate(  SqlUtils.parseDate(      columns[i + 1] ));           
        tbTx.setHigherTaxon(                              columns[i + 2] );
        tbTx.setTaxon(                                    columns[i + 3] );
        tbTx.setTransferredFrom(                          columns[i + 4] );
        tbTx.setBatchQuantityReturned( SqlUtils.parseInt( columns[i + 5] ));
        
        return i + 6;
    }
    
    protected String getTaxonDescription(TaxonBatchTransaction tbTx)
    {
        String description = null;
        
        String higherTaxon = tbTx.getHigherTaxon();
        String taxon       = tbTx.getTaxon();

        if (higherTaxon != null || taxon != null)
        {   
            if (higherTaxon == null) description = taxon;
            else if (taxon == null) description = higherTaxon;
            else description = higherTaxon + " " + taxon;
        }
        
        return description;
    }
    
    protected String getCountsDescription(TaxonBatchTransaction tbTx)
    {
        String inComments = null;
        
        String description = tbTx.getDescription();
        String boxCount = tbTx.getBoxCount();
        Integer typeCount = tbTx.getTypeCount();
        Integer nonSpecimenCount = tbTx.getNonSpecimenCount();
        
        if (description != null || boxCount != null || (typeCount != null && typeCount > 0) || (nonSpecimenCount != null && nonSpecimenCount > 0))
        {
            String counts = null;

            if (boxCount != null || typeCount != null || nonSpecimenCount != null)
            {
                String itemCounts = tbTx.getItemCountNote();
                
                if (boxCount != null)
                {
                    try
                    {
                        int boxes = Integer.parseInt(boxCount);
                        boxCount = boxCount + " box" + (boxes == 1 ? "" : "es");
                    }
                    catch (NumberFormatException nfe)
                    {
                        ;
                    }
                    boxCount = boxCount + ".";
                }
                
                if (boxCount == null) counts =  itemCounts;
                else if (itemCounts == null) counts = boxCount;
                else counts = boxCount + "  " + itemCounts;
            }

            if (counts == null) inComments = description;
            else if (description == null) inComments = counts;
            else inComments = counts + "  " + description;
        }
        
        return inComments;
    }
}
