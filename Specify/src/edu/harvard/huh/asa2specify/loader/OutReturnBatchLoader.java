package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.OutReturnBatch;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BorrowMaterialLookup;
import edu.harvard.huh.asa2specify.lookup.CarrierLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowMaterial;
import edu.ku.brc.specify.datamodel.BorrowReturnMaterial;
import edu.ku.brc.specify.datamodel.Shipment;

// Run this class after BorrowLoader, LoanLoader, and ShipmentLoader

public class OutReturnBatchLoader extends ReturnBatchLoader
{
    private static final Logger log  = Logger.getLogger(OutReturnBatchLoader.class);
    
    private static final String DEFAULT_SHIPPING_NUMBER = "none";
    
	private BorrowMaterialLookup borrowMaterialLookup;
	private CarrierLookup carrierLookup;
	private BorrowLookup borrowLookup;
	
	private HashMap<String, Agent> shippers;
	
	public OutReturnBatchLoader(File csvFile,
			                    Statement sqlStatement,
			                    BorrowMaterialLookup borrowMaterialLookup,
			                    CarrierLookup carrierLookup,
			                    BorrowLookup borrowLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.borrowMaterialLookup = borrowMaterialLookup;
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

	public Logger getLogger()
    {
        return log;
    }
	
	private OutReturnBatch parse(String[] columns) throws LocalException
	{
        OutReturnBatch outReturnBatch = new OutReturnBatch();
	 
        int i = super.parse(columns, outReturnBatch);
		
        if (columns.length < i + 5)
		{
			throw new LocalException("Not enough columns");
		}

		try
		{
			outReturnBatch.setCarrier(     AsaShipment.parseCarrier( columns[i + 0] ));
			outReturnBatch.setMethod(       AsaShipment.parseMethod( columns[i + 1] ));
			outReturnBatch.setCost(             SqlUtils.parseFloat( columns[i + 2] ));
			outReturnBatch.setIsEstimatedCost( Boolean.parseBoolean( columns[i + 3] ));
			outReturnBatch.setNote(                                  columns[i + 4] );
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
		
		BorrowMaterial borrowMaterial = lookupBorrowMaterial(transactionId);
		borrowReturnMaterial.setBorrowMaterial(borrowMaterial);

		// CollectionMemberID (collectionCode)
		String collectionCode = outReturnBatch.getCollectionCode();
		checkNull(collectionCode, "collection code");
		
		Integer collectionMemberId = getCollectionId(collectionCode);
		borrowReturnMaterial.setCollectionMemberId(collectionMemberId);
		
		// Quantity
		short quantity = outReturnBatch.getBatchQuantity();
		borrowReturnMaterial.setQuantity(quantity);
		
		// Remarks (type and non-specimen count)
		String remarks = getRemarks(outReturnBatch);
		borrowReturnMaterial.setRemarks(remarks);
		
		// ReturnedDate (actionDate)
		Date actionDate = outReturnBatch.getActionDate();
		if (actionDate != null)
		{
			Calendar returnedDate = DateUtils.toCalendar(actionDate);
			borrowReturnMaterial.setReturnedDate(returnedDate);
		}
		
		return borrowReturnMaterial;
	}
	
	private BorrowMaterial lookupBorrowMaterial(Integer transactionId) throws LocalException
	{
		return borrowMaterialLookup.getById(transactionId);
	}
	
	private Borrow lookupBorrow(Integer transactionId) throws LocalException
	{
		return borrowLookup.getById(transactionId);
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
    			;
    		}
    	}

    	// Number1 (cost)
        Float cost = outReturnBatch.getCost();
        shipment.setNumber1(cost);
    	
    	// Shipper (carrier)
    	String carrier = AsaShipment.toString(outReturnBatch.getCarrier());
    	Agent shipper = lookupCarrier(carrier);
    	
    	shipment.setShipper(shipper);
    	
    	// ShipmentDate (actionDate)
    	Date actionDate = outReturnBatch.getActionDate();
    	if (actionDate != null) shipment.setShipmentDate(DateUtils.toCalendar(actionDate));

    	// ShipmentMethod (method)
    	String method = AsaShipment.toString(outReturnBatch.getMethod());
    	shipment.setShipmentMethod(method);
    	
    	// ShipmentNumber (shipment.trackingNumber)
    	String shipmentNumber = DEFAULT_SHIPPING_NUMBER;
    	
    	shipmentNumber = truncate(shipmentNumber, 50, "transaction number");
    	shipment.setShipmentNumber(shipmentNumber);
    	
    	// Remarks (shipment.description)
    	String note = outReturnBatch.getNote();
    	shipment.setRemarks(note);

    	// Text1 (shipment.customsNo)
    	    	
    	// YesNo1 (acknowledgedFlag)
        Boolean isAcknowledged = outReturnBatch.isAcknowledged();
        shipment.setYesNo1(isAcknowledged);
    	
    	// YesNo2 (isCostEstimated)
        Boolean isEstimatedCost = outReturnBatch.isEstimatedCost();
        shipment.setYesNo2(isEstimatedCost);
    	
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

	private String getInsertSql(BorrowReturnMaterial borrowReturnMaterial)
	{
		String fields = "BorrowMaterialID, CollectionMemberID, Quantity, " +
				        "Remarks, ReturnedDate, TimestampCreated, Version";
		
		String[] values = new String[7];
		
		values[0] = SqlUtils.sqlString( borrowReturnMaterial.getBorrowMaterial().getId());
		values[1] = SqlUtils.sqlString( borrowReturnMaterial.getCollectionMemberId());
		values[2] = SqlUtils.sqlString( borrowReturnMaterial.getQuantity());
		values[3] = SqlUtils.sqlString( borrowReturnMaterial.getRemarks());
		values[4] = SqlUtils.sqlString( borrowReturnMaterial.getReturnedDate());
		values[5] = SqlUtils.now();
		values[6] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("borrowreturnmaterial", fields, values);
	}
	
	private String getInsertSql(Shipment shipment)
	{
		String fields = "BorrowID, CollectionMemberID, NumberOfPackages, " +
				        "Number1, Remarks, ShipperID, ShipmentDate, ShipmentMethod, " +
				        "ShipmentNumber, TimestampCreated, Version, YesNo1, YesNo2";
		
		String[] values = new String[13];
		
		values[0]  = SqlUtils.sqlString( shipment.getBorrow().getId());
		values[1]  = SqlUtils.sqlString( shipment.getCollectionMemberId());
		values[2]  = SqlUtils.sqlString( shipment.getNumberOfPackages());
		values[3]  = SqlUtils.sqlString( shipment.getNumber1());
		values[4]  = SqlUtils.sqlString( shipment.getRemarks());
		values[5]  = SqlUtils.sqlString( shipment.getShipper().getId());
		values[6]  = SqlUtils.sqlString( shipment.getShipmentDate());
		values[7]  = SqlUtils.sqlString( shipment.getShipmentMethod());
		values[8]  = SqlUtils.sqlString( shipment.getShipmentNumber());
		values[9]  = SqlUtils.now();
		values[10] = SqlUtils.zero();
		values[11] = SqlUtils.sqlString( shipment.getYesNo1());
		values[12] = SqlUtils.sqlString( shipment.getYesNo2());
		
		return SqlUtils.getInsertSql("shipment", fields, values);
	}
}
