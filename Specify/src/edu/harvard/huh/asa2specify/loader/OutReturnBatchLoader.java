package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.OutReturnBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.CarrierLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonBatchLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowMaterial;
import edu.ku.brc.specify.datamodel.BorrowReturnMaterial;
import edu.ku.brc.specify.datamodel.Shipment;

// Run this class after TransactionLoader, ShipmentLoader, and TaxonBatchLoader

public class OutReturnBatchLoader extends CsvToSqlLoader
{
	private TaxonBatchLookup borrowMaterialLookup;
	private CarrierLookup carrierLookup;
	private BorrowLookup borrowLookup;
	
	private HashMap<String, Agent> shippers;
	
	public OutReturnBatchLoader(File csvFile,
			                    Statement sqlStatement,
			                    TaxonBatchLookup taxonBatchLookup,
			                    CarrierLookup carrierLookup,
			                    BorrowLookup borrowLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.borrowMaterialLookup = taxonBatchLookup;
		this.carrierLookup = carrierLookup;
		this.borrowLookup = borrowLookup;
		
		this.shippers = new HashMap<String, Agent>();
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		OutReturnBatch outReturnBatch = parse(columns);
		Integer outReturnBatchId = outReturnBatch.getId();
		setCurrentRecordId(outReturnBatchId);
		
		BorrowReturnMaterial borrowReturnMaterial = getBorrowReturnMaterial(outReturnBatch);
		String sql = getInsertSql(borrowReturnMaterial);
		insert(sql);
		
		Shipment shipment = getShipment(outReturnBatch);
		sql = getInsertSql(shipment);
		insert(sql);
	}

	private OutReturnBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 15)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		OutReturnBatch outReturnBatch = new OutReturnBatch();
		try
		{
			outReturnBatch.setId(                 SqlUtils.parseInt( columns[0]  ));
			outReturnBatch.setTransactionId(      SqlUtils.parseInt( columns[1]  ));
			outReturnBatch.setCollectionCode(                        columns[2]  );
			outReturnBatch.setType(           Transaction.parseType( columns[3]  ));
			outReturnBatch.setItemCount(          SqlUtils.parseInt( columns[4]  ));
			outReturnBatch.setTypeCount(          SqlUtils.parseInt( columns[5]  ));
			outReturnBatch.setNonSpecimenCount(   SqlUtils.parseInt( columns[6]  ));	
			outReturnBatch.setBoxCount(                              columns[7]  );
			outReturnBatch.setIsAcknowledged(  Boolean.parseBoolean( columns[8]  ));
			outReturnBatch.setActionDate(        SqlUtils.parseDate( columns[9]  ));
			outReturnBatch.setCarrier(     AsaShipment.parseCarrier( columns[10] ));
			outReturnBatch.setMethod(       AsaShipment.parseMethod( columns[11] ));
			outReturnBatch.setCost(             SqlUtils.parseFloat( columns[12] ));
			outReturnBatch.setIsEstimatedCost( Boolean.parseBoolean( columns[13] ));
			outReturnBatch.setNote(                                  columns[14] );
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return outReturnBatch;
	}
	
	private BorrowReturnMaterial getBorrowReturnMaterial(OutReturnBatch outReturnBatch) throws LocalException
	{
		BorrowReturnMaterial borrowReturnMaterial = new BorrowReturnMaterial();
		
		// BorrowMaterial
		Integer transactionId = outReturnBatch.getTransactionId();
		checkNull(transactionId, "transaction id");
		
		BorrowMaterial borrowMaterial = lookupTaxonBatch(transactionId);
		borrowReturnMaterial.setBorrowMaterial(borrowMaterial);

		// CollectionMemberID (collectionCode)
		String collectionCode = outReturnBatch.getCollectionCode();
		checkNull(collectionCode, "collection code");
		
		Integer collectionMemberId = getCollectionId(collectionCode);
		borrowReturnMaterial.setCollectionMemberId(collectionMemberId);
		
		// Quantity
		short quantity = getQuantity(outReturnBatch);
		borrowReturnMaterial.setQuantity(quantity);
		
		// Remarks (note)
		String note = outReturnBatch.getNote();
		borrowReturnMaterial.setRemarks(note);
		
		// ReturnedDate (actionDate)
		Date actionDate = outReturnBatch.getActionDate();
		if (actionDate != null)
		{
			Calendar returnedDate = DateUtils.toCalendar(actionDate);
			borrowReturnMaterial.setReturnedDate(returnedDate);
		}
		
		return borrowReturnMaterial;
	}
	
	private BorrowMaterial lookupTaxonBatch(Integer transactionId) throws LocalException
	{
		return borrowMaterialLookup.getBorrowMaterial(transactionId);
	}
	
	private Borrow lookupBorrow(Integer transactionId) throws LocalException
	{
		return borrowLookup.getById(transactionId);
	}
	
	private short getQuantity(OutReturnBatch outReturnBatch)
	{
	    Integer itemCount = outReturnBatch.getItemCount();
	    Integer typeCount = outReturnBatch.getTypeCount();
	    Integer nonSpecimenCount = outReturnBatch.getNonSpecimenCount();

	    return (short) (itemCount + typeCount + nonSpecimenCount);
	}
	
	private Shipment getShipment(OutReturnBatch outReturnBatch) throws LocalException
	{
		Shipment shipment = new Shipment();
		
	   	// Borrow
    	Integer transactionId = outReturnBatch.getTransactionId();
    	checkNull(transactionId, "transaction id");
    	    	
    	Borrow borrow = lookupBorrow(transactionId);
    	shipment.setBorrow(borrow);
    	
    	// CollectionMemberId (transaction localUnit)
    	String collectionCode = outReturnBatch.getCollectionCode();
    	checkNull(collectionCode, "collection code");
    	
    	Integer collectionMemberId = getCollectionId(collectionCode);
    	shipment.setCollectionMemberId(collectionMemberId);
    	
    	// InsuredForAmount (shipment.isInsured)
    	
    	// NumberOfPackages (transaction boxCount)
    	String boxCount = outReturnBatch.getBoxCount();
    	if (boxCount != null)
     	{
    		try
    		{
    			Short numberOfPackages = Short.parseShort(boxCount);
    			shipment.setNumberOfPackages(numberOfPackages);
    		}
    		catch (NumberFormatException e)
    		{
    			; // this field was already saved in TransactionLoader
    		}
    	}

    	// Number1 (shipment.ordinal)
    	shipment.setNumber1((float) 1.0);
    	
    	// Number2 (cost)
    	Float cost = outReturnBatch.getCost();
    	shipment.setNumber2(cost);
    	
    	// Remarks (shipment.description)
    	String description = getDescription(outReturnBatch);
    	shipment.setRemarks(description);
    	
    	// Shipper (carrier)
    	String carrier = outReturnBatch.getCarrier().name();
    	Agent shipper = lookupCarrier(carrier);
    	
    	shipment.setShipper(shipper);
    	
    	// ShipmentMethod (method)
    	String method = outReturnBatch.getMethod().name();
    	shipment.setShipmentMethod(method);
    	
    	// ShipmentNumber (shipment.trackingNumber)
    	
    	// Text1 (shipment.customsNo)
    	    	
    	// YesNo1 (isEstimatedCost)
    	Boolean isEstimatedCost = outReturnBatch.isEstimatedCost();
    	shipment.setYesNo1(isEstimatedCost);
    	
    	// YesNo2 (acknowledgedFlag)
    	Boolean isAcknowledged = outReturnBatch.isAcknowledged();
    	shipment.setYesNo2(isAcknowledged);
    	
		return shipment;
	}
	
    private Agent lookupCarrier(String carrierName) throws LocalException
    {
    	Agent carrier = shippers.get(carrierName);
    	
    	if (carrier == null)
    	{
    		carrier = carrierLookup.getByName(carrierName);
    		shippers.put(carrierName, carrier);
    	}
    	return carrier;
    }
    
	private String getDescription(OutReturnBatch taxonBatch)
	{
		String boxCount = taxonBatch.getBoxCount();
		if (boxCount == null) boxCount = "0";
		
	    Integer itemCount = taxonBatch.getItemCount();
	    Integer typeCount = taxonBatch.getTypeCount();
	    Integer nonSpecimenCount = taxonBatch.getNonSpecimenCount();

	    Object[] args = {boxCount, itemCount, typeCount, nonSpecimenCount };
	    String pattern = "{0} box(es): {1} items, {2} types, {3} non-specimens";
	    
	    return MessageFormat.format(pattern, args);
	}
	
	private String getInsertSql(BorrowReturnMaterial borrowReturnMaterial)
	{
		String fields = "BorrowMaterialID, CollectionMemberID, Quantity, " +
				        "Remarks, ReturnedDate, TimestampCreated";
		
		String[] values = new String[6];
		
		values[0] = SqlUtils.sqlString( borrowReturnMaterial.getBorrowMaterial().getId());
		values[1] = SqlUtils.sqlString( borrowReturnMaterial.getCollectionMemberId());
		values[2] = SqlUtils.sqlString( borrowReturnMaterial.getQuantity());
		values[3] = SqlUtils.sqlString( borrowReturnMaterial.getRemarks());
		values[4] = SqlUtils.sqlString( borrowReturnMaterial.getReturnedDate());
		values[5] = SqlUtils.now();
		
		return SqlUtils.getInsertSql("borrowreturnmaterial", fields, values);
	}
	
	private String getInsertSql(Shipment shipment)
	{
		String fields = "BorrowID, CollectionMemberID, NumberOfPackages, " +
				        "Number1, Number2, Remarks, Shipper, ShipmentMethod, " +
				        "TimestampCreated, YesNo1, YesNo2";
		
		String[] values = new String[11];
		
		values[0]  = SqlUtils.sqlString( shipment.getBorrow().getId());
		values[1]  = SqlUtils.sqlString( shipment.getCollectionMemberId());
		values[2]  = SqlUtils.sqlString( shipment.getNumberOfPackages());
		values[3]  = SqlUtils.sqlString( shipment.getNumber1());
		values[4]  = SqlUtils.sqlString( shipment.getNumber2());
		values[5]  = SqlUtils.sqlString( shipment.getRemarks());
		values[6]  = SqlUtils.sqlString( shipment.getShipper().getId());
		values[7]  = SqlUtils.sqlString( shipment.getShipmentMethod());
		values[8]  = SqlUtils.now();
		values[9]  = SqlUtils.sqlString( shipment.getYesNo1());
		values[10] = SqlUtils.sqlString( shipment.getYesNo2());
		
		return SqlUtils.getInsertSql("shipment", fields, values);
	}
}
