package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.ReturnBatch;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;

public abstract class ReturnBatchLoader extends CountableBatchLoader
{
    protected ReturnBatchLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    protected int parse(String[] columns, ReturnBatch returnBatch) throws LocalException
    {        
        int i = super.parse(columns, returnBatch);

        if (columns.length < i + 3)
        {
            throw new LocalException("Not enough columns");
        }
        
        returnBatch.setBoxCount(                             columns[i + 0] );
        returnBatch.setIsAcknowledged( Boolean.parseBoolean( columns[i + 1] ));
        returnBatch.setActionDate(       SqlUtils.parseDate( columns[i + 2] ));
        
        return i + 3; // index of next column
    }
    
    protected String getRemarks(ReturnBatch returnBatch)
    {   
        String countNote = returnBatch.getItemCountNote();
        String boxNote = returnBatch.getBoxCountNote();
        String acknowledgedNote = returnBatch.getAcknowledgedNote();

        if (countNote != null || boxNote != null || acknowledgedNote != null)
        {
            if (countNote != null)
            {
                if (boxNote != null || acknowledgedNote != null)
                {
                    if (boxNote == null) return countNote + "  " + acknowledgedNote;
                    else return countNote + "  " + boxNote + "  " + acknowledgedNote;
                }
                else
                {
                    return countNote;
                }
            }
            else
            {
                if (boxNote == null) return acknowledgedNote;
                else return boxNote;
            }
        }

        return null;
    }
}
