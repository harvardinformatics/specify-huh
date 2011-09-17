package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.OutReturnBatch;
import edu.harvard.huh.asa.AsaShipment.CARRIER;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
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
	private BorrowMaterialLookup borrowMaterialLookup;
	private AgentLookup agentLookup;
	private CarrierLookup carrierLookup;
	private BorrowLookup borrowLookup;
	
	private HashMap<String, Agent> shippers;
	
	public OutReturnBatchLoader(File csvFile,
			                    Statement sqlStatement,
			                    BorrowMaterialLookup borrowMaterialLookup,
			                    AgentLookup agentLookup,
			                    CarrierLookup carrierLookup,
			                    BorrowLookup borrowLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.borrowMaterialLookup = borrowMaterialLookup;
		this.agentLookup = agentLookup;
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
		
        Shipment shipment = getShipment(outReturnBatch);
        String sql = getInsertSql(shipment);
        insert(sql);
        
        BorrowReturnMaterial borrowReturnMaterial = getBorrowReturnMaterial(outReturnBatch, shipment.getBorrow().getId());
		sql = getInsertSql(borrowReturnMaterial);
		insert(sql);

	}
	
	private OutReturnBatch parse(String[] columns) throws LocalException
	{
        OutReturnBatch outReturnBatch = new OutReturnBatch();
	 
        int i = super.parse(columns, outReturnBatch);
		
        if (columns.length < i + 7)
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
			outReturnBatch.setAgentId(            SqlUtils.parseInt( columns[i + 5] ));
			outReturnBatch.setOrganizationId(     SqlUtils.parseInt( columns[i + 6] ));
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
	
	private BorrowReturnMaterial getBorrowReturnMaterial(OutReturnBatch outReturnBatch, Integer borrowId) throws LocalException
	{
		BorrowReturnMaterial borrowReturnMaterial = new BorrowReturnMaterial();
		
		// BorrowMaterial
		Integer transactionId = outReturnBatch.getTransactionId();
		checkNull(transactionId, "transaction id");
		
		BorrowMaterial borrowMaterial = lookupBorrowMaterial(borrowId);
		borrowReturnMaterial.setBorrowMaterial(borrowMaterial);

		// CollectionMemberID (collectionCode)
		String collectionCode = outReturnBatch.getCollectionCode();
		checkNull(collectionCode, "collection code");
		
		Integer collectionMemberId = getCollectionId(collectionCode);
		borrowReturnMaterial.setCollectionMemberId(collectionMemberId);
        
		// Remarks
		// ... note
		String note = outReturnBatch.getNote();
		
		// ... item count
        int itemCount = outReturnBatch.getItemCount();
        String items = denormalize("items", String.valueOf(itemCount));
        
        // ... type count
        int typeCount = outReturnBatch.getTypeCount();
        String types = denormalize("types", String.valueOf(typeCount));
        
        // ... non-specimen count
        int nonSpecimenCount = outReturnBatch.getNonSpecimenCount();
        String nonSpecimens = denormalize("non-specimens", String.valueOf(nonSpecimenCount));
		
		borrowReturnMaterial.setRemarks(concatenate(note, items, types, nonSpecimens));
		
		// Quantity
		borrowReturnMaterial.setQuantity((short) (itemCount + typeCount + nonSpecimenCount));
		
		// ReturnedDate (actionDate)
		Date actionDate = outReturnBatch.getActionDate();
		if (actionDate != null)
		{
			Calendar returnedDate = DateUtils.toCalendar(actionDate);
			borrowReturnMaterial.setReturnedDate(returnedDate);
		}
		
		return borrowReturnMaterial;
	}
	
	private BorrowMaterial lookupBorrowMaterial(Integer borrowId) throws LocalException
	{
		return borrowMaterialLookup.getByBorrowId(borrowId);
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
    	
    	// Discipline
    	shipment.setDiscipline(getBotanyDiscipline());
    	
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
    	
        // ShippedTo
        Integer asaAgentId = outReturnBatch.getAgentId();
        checkNull(asaAgentId, "agent id");
        
        Agent shippedTo = lookupAgent(asaAgentId);
        shipment.setShippedTo(shippedTo);

        // Shipper (carrier)
        Agent shipper = null;

        CARRIER carrier = outReturnBatch.getCarrier();
        
        if (! CARRIER.Unknown.equals(carrier))
        {
            shipper = lookupCarrier(AsaShipment.toString(carrier));
        }
        else
        {
            shipper = new Agent();
        }
        shipment.setShipper(shipper);

    	
    	// ShipmentDate (actionDate)
    	Date actionDate = outReturnBatch.getActionDate();
    	if (actionDate != null) shipment.setShipmentDate(DateUtils.toCalendar(actionDate));

    	// ShipmentMethod (method)
    	String method = AsaShipment.toString(outReturnBatch.getMethod());
    	shipment.setShipmentMethod(method);
    	
    	// ShipmentNumber (shipment.trackingNumber)
    	if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
    	String shipmentNumber = ShipmentLoader.getShipmentNumber(transactionId);
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
    
    private Agent lookupAgent(Integer asaAgentId) throws LocalException
    {
        return agentLookup.getById(asaAgentId);
    }

	private String getInsertSql(BorrowReturnMaterial borrowReturnMaterial)
	{
		String fields = "BorrowMaterialID, CollectionMemberID, Quantity, " +
				        "Remarks, ReturnedDate, TimestampCreated, Version";
		
		String[] values = {
				SqlUtils.sqlString( borrowReturnMaterial.getBorrowMaterial().getId()),
				SqlUtils.sqlString( borrowReturnMaterial.getCollectionMemberId()),
				SqlUtils.sqlString( borrowReturnMaterial.getQuantity()),
				SqlUtils.sqlString( borrowReturnMaterial.getRemarks()),
				SqlUtils.sqlString( borrowReturnMaterial.getReturnedDate()),
				SqlUtils.now(),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("borrowreturnmaterial", fields, values);
	}
	
	private String getInsertSql(Shipment shipment)
	{
		String fields = "BorrowID, DisciplineID, NumberOfPackages, Number1, " +
				        "Remarks, ShippedToID, ShipperID, ShipmentDate, ShipmentMethod, " +
				        "ShipmentNumber, TimestampCreated, Version, YesNo1, YesNo2";
		
		String[] values = {
				SqlUtils.sqlString( shipment.getBorrow().getId()),
				SqlUtils.sqlString( shipment.getDiscipline().getId()),
				SqlUtils.sqlString( shipment.getNumberOfPackages()),
				SqlUtils.sqlString( shipment.getNumber1()),
				SqlUtils.sqlString( shipment.getRemarks()),
				SqlUtils.sqlString( shipment.getShippedTo().getId()),
				SqlUtils.sqlString( shipment.getShipper().getId()),
				SqlUtils.sqlString( shipment.getShipmentDate()),
				SqlUtils.sqlString( shipment.getShipmentMethod()),
				SqlUtils.sqlString( shipment.getShipmentNumber()),
				SqlUtils.now(),
				SqlUtils.one(),
				SqlUtils.sqlString( shipment.getYesNo1()),
				SqlUtils.sqlString( shipment.getYesNo2())
		};
		
		return SqlUtils.getInsertSql("shipment", fields, values);
	}
}
