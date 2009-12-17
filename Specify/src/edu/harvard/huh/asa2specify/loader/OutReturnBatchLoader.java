package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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
		
		// NonSpecimenCount
		int nonSpecimenCount = outReturnBatch.getNonSpecimenCount();
		borrowReturnMaterial.setNonSpecimenCount((short) nonSpecimenCount);
		
		// Quantity (itemCount + typeCount + nonSpecimenCount)
		short quantity = outReturnBatch.getBatchQuantity();
		borrowReturnMaterial.setQuantity(quantity);
		
		// Remarks
		String note = outReturnBatch.getNote();
		borrowReturnMaterial.setRemarks(note);
		
		// ReturnedDate (actionDate)
		Date actionDate = outReturnBatch.getActionDate();
		if (actionDate != null)
		{
			Calendar returnedDate = DateUtils.toCalendar(actionDate);
			borrowReturnMaterial.setReturnedDate(returnedDate);
		}
		
		// TypeCount
		int typeCount = outReturnBatch.getTypeCount();
		borrowReturnMaterial.setTypeCount((short) typeCount);
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
    			if (outReturnBatch.getNote() == null)
    			{
    			    outReturnBatch.setNote(boxCount);
    			}
    			else
    			{
    			    getLogger().warn(rec() + "Couldn't parse box count, and didn't put it in Remarks");
    			}
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
		String fields = "BorrowMaterialID, CollectionMemberID, NonSpecimenCount, Quantity, " +
				        "Remarks, ReturnedDate, TimestampCreated, TypeCount, Version";
		
		String[] values = new String[9];
		
		values[0] = SqlUtils.sqlString( borrowReturnMaterial.getBorrowMaterial().getId());
		values[1] = SqlUtils.sqlString( borrowReturnMaterial.getCollectionMemberId());
		values[2] = SqlUtils.sqlString( borrowReturnMaterial.getNonSpecimenCount());
		values[3] = SqlUtils.sqlString( borrowReturnMaterial.getQuantity());
		values[4] = SqlUtils.sqlString( borrowReturnMaterial.getRemarks());
		values[5] = SqlUtils.sqlString( borrowReturnMaterial.getReturnedDate());
		values[6] = SqlUtils.now();
		values[7] = SqlUtils.sqlString( borrowReturnMaterial.getTypeCount());
		values[8] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("borrowreturnmaterial", fields, values);
	}
	
	private String getInsertSql(Shipment shipment)
	{
		String fields = "BorrowID, NumberOfPackages, " +
				        "Number1, Remarks, ShipperID, ShipmentDate, ShipmentMethod, " +
				        "ShipmentNumber, TimestampCreated, Version, YesNo1, YesNo2";
		
		String[] values = new String[12];
		
		values[0]  = SqlUtils.sqlString( shipment.getBorrow().getId());
		values[1]  = SqlUtils.sqlString( shipment.getNumberOfPackages());
		values[2]  = SqlUtils.sqlString( shipment.getNumber1());
		values[3]  = SqlUtils.sqlString( shipment.getRemarks());
		values[4]  = SqlUtils.sqlString( shipment.getShipper().getId());
		values[5]  = SqlUtils.sqlString( shipment.getShipmentDate());
		values[6]  = SqlUtils.sqlString( shipment.getShipmentMethod());
		values[7]  = SqlUtils.sqlString( shipment.getShipmentNumber());
		values[8]  = SqlUtils.now();
		values[9]  = SqlUtils.zero();
		values[10] = SqlUtils.sqlString( shipment.getYesNo1());
		values[11] = SqlUtils.sqlString( shipment.getYesNo2());
		
		return SqlUtils.getInsertSql("shipment", fields, values);
	}
}
