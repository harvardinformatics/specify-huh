package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.MessageFormat;

import edu.harvard.huh.asa.OutGeoBatch;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.ku.brc.specify.datamodel.Deaccession;
import edu.ku.brc.specify.datamodel.DeaccessionPreparation;

public class OutGeoBatchLoader extends CsvToSqlLoader
{
    OutgoingGiftLookup outGiftLookup;
    
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
		else if (type.equals(TYPE.OutExchange))
		{
		    ;// TODO: this needs to go into transaction loader
		}
		else
		{
		    throw new LocalException("Invalid out geo batch transaction type: " + type.name());
		}
	}

	private OutGeoBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 6)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		OutGeoBatch outGeoBatch = new OutGeoBatch();
		try
		{			
			outGeoBatch.setId(               SqlUtils.parseInt( columns[0] ));
			outGeoBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
			outGeoBatch.setGeoUnit(                             columns[2] );
			outGeoBatch.setItemCount(        SqlUtils.parseInt( columns[3] ));
			outGeoBatch.setTypeCount(        SqlUtils.parseInt( columns[4] ));
			outGeoBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[5] ));			
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		
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
	    short quantity = getQuantity(outGeoBatch);
	    deaccessionPrep.setQuantity(quantity);
	    
	    // Remarks
	    String description = getDescription(outGeoBatch);
	    deaccessionPrep.setRemarks(description);
	    
	    return deaccessionPrep;
	}

	private short getQuantity(OutGeoBatch outGeoBatch)
	{
	    Integer itemCount = outGeoBatch.getItemCount();
	    Integer typeCount = outGeoBatch.getTypeCount();
	    Integer nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
	    
	    return (short) (itemCount + typeCount + nonSpecimenCount);
	}
	
	private String getDescription(OutGeoBatch outGeoBatch)
	{
	    String geoUnit = outGeoBatch.getGeoUnit();
	    if (geoUnit == null) geoUnit = "";
	    else geoUnit = geoUnit + ": ";

	    Integer itemCount = outGeoBatch.getItemCount();
	    Integer typeCount = outGeoBatch.getTypeCount();
	    Integer nonSpecimenCount = outGeoBatch.getNonSpecimenCount();

	    Object[] args = {geoUnit, itemCount, typeCount, nonSpecimenCount };
	    String pattern = "{0}{1} items, {2} types, {3} non-specimens\n";

	    return MessageFormat.format(pattern, args);
	}

	private Deaccession lookupOutGift(Integer transactionId) throws LocalException
	{
	    return outGiftLookup.getById(transactionId);
	}

	private String getInsertSql(DeaccessionPreparation deaccessionPrep)
	{
	    String fields = "Deaccession, Quantity, Remarks, TimestampCreated";
	    
	    String[] values = new String[4];
	    
	    values[0] = SqlUtils.sqlString( deaccessionPrep.getDeaccession().getId());
	    values[1] = SqlUtils.sqlString( deaccessionPrep.getQuantity());
	    values[2] = SqlUtils.sqlString( deaccessionPrep.getRemarks());
	    values[3] = SqlUtils.now();
	    
	    return SqlUtils.getInsertSql("deaccessionpreparation", fields, values);
	}

}
