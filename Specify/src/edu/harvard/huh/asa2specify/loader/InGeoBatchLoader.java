package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.InGeoBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.IncomingGiftLookup;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftPreparation;

public class InGeoBatchLoader extends CsvToSqlLoader
{
    private static final Logger log  = Logger.getLogger(InGeoBatchLoader.class);
    
    private IncomingGiftLookup inGiftLookup;
    
	public InGeoBatchLoader(File csvFile,
	                        Statement sqlStatement,
	                        IncomingGiftLookup inGiftLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.inGiftLookup = inGiftLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{	
	    InGeoBatch inGeoBatch = parse(columns);
	    
	    Integer inGeoBatchId = inGeoBatch.getId();
	    setCurrentRecordId(inGeoBatchId);
	    
	    TYPE type = inGeoBatch.getType();
	    
	    if (type.equals(TYPE.InGift))
	    {
	        GiftPreparation giftPreparation = getGiftPreparation(inGeoBatch);
	        String sql = getInsertSql(giftPreparation);
	        insert(sql);
	    }
	    else
	    {
	        throw new LocalException("Invalid out in batch transaction type: " + type.name());
	    }
	}

	public Logger getLogger()
	{
	    return log;
	}
	
	private InGeoBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 12)
		{
			throw new LocalException("Not enough columns");
		}
		
		InGeoBatch inGeoBatch = new InGeoBatch();
		try
		{
			inGeoBatch.setId(               SqlUtils.parseInt( columns[0]  ));
			inGeoBatch.setTransactionId(    SqlUtils.parseInt( columns[1]  ));
			inGeoBatch.setCollectionCode(                      columns[2]  );
			inGeoBatch.setType(         Transaction.parseType( columns[3]  ));
			inGeoBatch.setGeoUnit(                             columns[4]  );
			inGeoBatch.setItemCount(        SqlUtils.parseInt( columns[5]  ));
			inGeoBatch.setTypeCount(        SqlUtils.parseInt( columns[6]  ));
			inGeoBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[7]  ));
			inGeoBatch.setDiscardCount(     SqlUtils.parseInt( columns[8]  ));
			inGeoBatch.setDistributeCount(  SqlUtils.parseInt( columns[9]  ));
			inGeoBatch.setReturnCount(      SqlUtils.parseInt( columns[10] ));
			inGeoBatch.setCost(           SqlUtils.parseFloat( columns[11] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return inGeoBatch;
	}
	
	private GiftPreparation getGiftPreparation(InGeoBatch inGeoBatch) throws LocalException
	{
	    GiftPreparation giftPreparation = new GiftPreparation();

	    // CollectionMemberID
	    String collectionCode = inGeoBatch.getCollectionCode();
	    checkNull(collectionCode, "collection code");
	    
	    Integer collectionMemberId = getCollectionId(collectionCode);
	    giftPreparation.setCollectionMemberId(collectionMemberId);
	    
	    // DescriptionOfMaterial
	    String description = getDescription(inGeoBatch);
	    description = truncate(description, 255, "description");
	    giftPreparation.setDescriptionOfMaterial(description);
	    
	    // Gift
	    Integer transactionId = inGeoBatch.getTransactionId();
	    checkNull(transactionId, "transaction id");
	    
	    Gift gift = lookupGift(transactionId);
	    giftPreparation.setGift(gift);
	    
	    // Quantity
	    int quantity = getQuantity(inGeoBatch);
	    giftPreparation.setQuantity(quantity);
	    
	    return giftPreparation;
	}

	private Gift lookupGift(Integer transactionId) throws LocalException
	{
	    return inGiftLookup.getById(transactionId);
	}

	private int getQuantity(InGeoBatch inGeoBatch)
	{
	    Integer itemCount = inGeoBatch.getItemCount();
	    Integer typeCount = inGeoBatch.getTypeCount();
	    Integer nonSpecimenCount = inGeoBatch.getNonSpecimenCount();

        Integer discardCount = inGeoBatch.getDiscardCount();
        Integer distributeCount = inGeoBatch.getDistributeCount();
        Integer returnCount = inGeoBatch.getReturnCount();
        
	    return (itemCount + typeCount + nonSpecimenCount - discardCount - distributeCount - returnCount);
	}

	private String getDescription(InGeoBatch inGeoBatch)
	{
	    String geoUnit = inGeoBatch.getGeoUnit();
	    if (geoUnit == null) geoUnit = "";
	    else geoUnit = geoUnit + ": ";

	    Integer itemCount = inGeoBatch.getItemCount();
	    Integer typeCount = inGeoBatch.getTypeCount();
	    Integer nonSpecimenCount = inGeoBatch.getNonSpecimenCount();

	    Integer discardCount = inGeoBatch.getDiscardCount();
	    Integer distributeCount = inGeoBatch.getDistributeCount();
	    Integer returnCount = inGeoBatch.getReturnCount();
	    
	    Float cost = inGeoBatch.getCost();
	    
	    Object[] args = {geoUnit, itemCount, typeCount, nonSpecimenCount, discardCount, distributeCount, returnCount, cost };
	    String pattern = "{0}{1} items, {2} types, {3} non-specimens; {4} discarded, {5} distributed, {6} returned; cost: {7}";

	    return MessageFormat.format(pattern, args);
	}

	private String getInsertSql(GiftPreparation giftPreparation)
	{
	    String fields = "CollectionMemberID, DescriptionOfMaterial, GiftID, Quantity, " +
	    		        "TimestampCreated, Version";
	    
	    String[] values = new String[6];
	    
	    values[0] = SqlUtils.sqlString( giftPreparation.getCollectionMemberId());
	    values[1] = SqlUtils.sqlString( giftPreparation.getDescriptionOfMaterial());
	    values[2] = SqlUtils.sqlString( giftPreparation.getGift().getId());
	    values[3] = SqlUtils.sqlString( giftPreparation.getQuantity());
	    values[4] = SqlUtils.now();
	    values[5] = SqlUtils.one();
	    
	    return SqlUtils.getInsertSql("giftpreparation", fields, values);
	}
}
