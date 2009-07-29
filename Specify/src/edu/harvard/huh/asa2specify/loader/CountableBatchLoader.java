package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.CountableBatch;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;

public abstract class CountableBatchLoader extends CsvToSqlLoader
{
    protected CountableBatchLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    protected int parse(String[] columns, CountableBatch countableBatch) throws LocalException
    {        

        if (columns.length < 6)
        {
            throw new LocalException("Not enough columns");
        }
        
        countableBatch.setId(               SqlUtils.parseInt( columns[0] ));
        countableBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
        countableBatch.setCollectionCode(                      columns[2] );
        countableBatch.setItemCount(        SqlUtils.parseInt( columns[3] ));
        countableBatch.setTypeCount(        SqlUtils.parseInt( columns[4] ));
        countableBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[5] ));
        
        return 6; // index of next column
    }
}
