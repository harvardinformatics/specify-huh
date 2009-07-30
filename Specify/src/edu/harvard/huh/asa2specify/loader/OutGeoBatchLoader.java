package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.OutGeoBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.ku.brc.specify.datamodel.Deaccession;
import edu.ku.brc.specify.datamodel.DeaccessionPreparation;

public class OutGeoBatchLoader extends CountableBatchLoader
{
    private static final Logger log  = Logger.getLogger(OutGeoBatchLoader.class);
    
    private OutgoingGiftLookup outGiftLookup;
    
	public OutGeoBatchLoader(File csvFile,
	                         Statement sqlStatement,
	                         OutgoingGiftLookup outGiftLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.outGiftLookup = outGiftLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		OutGeoBatch outGeoBatch = parse(columns);
		
		Integer outGeoBatchId = outGeoBatch.getId();
		
		setCurrentRecordId(outGeoBatchId);
		
		TYPE type = outGeoBatch.getType();
		checkNull(type, "transaction type");
		
		if (type.equals(TYPE.OutGift))
		{
		    DeaccessionPreparation deaccessionPrep = getDeaccessionPrep(outGeoBatch);
		    String sql = getInsertSql(deaccessionPrep);
		    insert(sql);
		}
		else
		{
		    throw new LocalException("Invalid out geo batch transaction type: " + type.name());
		}
	}

	public Logger getLogger()
    {
        return log;
    }
	
	private OutGeoBatch parse(String[] columns) throws LocalException
	{
	    OutGeoBatch outGeoBatch = new OutGeoBatch();
	    
	    int i = super.parse(columns, outGeoBatch);

        if (columns.length < i + 2)
        {
            throw new LocalException("Not enough columns");
        }
        
        try
        {
            outGeoBatch.setType( Transaction.parseType( columns[i + 0] ));
        }
        catch (AsaException e)
        {
            throw new LocalException("Couldn't parse transaction type", e);
        }
        outGeoBatch.setGeoUnit( columns[i + 1] );

		return outGeoBatch;
	}
	
	private DeaccessionPreparation getDeaccessionPrep(OutGeoBatch outGeoBatch) throws LocalException
	{
	    DeaccessionPreparation deaccessionPrep = new DeaccessionPreparation();
	    
	    // Deaccession
	    Integer transactionId = outGeoBatch.getTransactionId();
	    checkNull(transactionId, "transaction id");
	    
	    Deaccession deaccession = lookupOutGift(transactionId);
	    deaccessionPrep.setDeaccession(deaccession);
	    
	    // Quantity
	    short quantity = outGeoBatch.getBatchQuantity();
	    deaccessionPrep.setQuantity(quantity);
	    
	    // Remarks
	    String geoUnit = outGeoBatch.getGeoUnit();
	    String itemCountNote = outGeoBatch.getItemCountNote();
	    
	    String description = geoUnit + ".  " + itemCountNote;
	    
	    deaccessionPrep.setRemarks(description);
	    
	    return deaccessionPrep;
	}

	private Deaccession lookupOutGift(Integer transactionId) throws LocalException
	{
	    return outGiftLookup.getById(transactionId);
	}

	private String getInsertSql(DeaccessionPreparation deaccessionPrep)
	{
	    String fields = "DeaccessionID, Quantity, Remarks, TimestampCreated, Version";
	    
	    String[] values = new String[5];
	    
	    values[0] = SqlUtils.sqlString( deaccessionPrep.getDeaccession().getId());
	    values[1] = SqlUtils.sqlString( deaccessionPrep.getQuantity());
	    values[2] = SqlUtils.sqlString( deaccessionPrep.getRemarks());
	    values[3] = SqlUtils.now();
	    values[4] = SqlUtils.zero();
	    
	    return SqlUtils.getInsertSql("deaccessionpreparation", fields, values);
	}

}
