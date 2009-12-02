/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.AsaShipment.CARRIER;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.CarrierLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;

import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.Shipment;

public class ShipmentLoader extends CsvToSqlLoader
{
    // Loan/Exchange/Gift
    
    private static final Logger log  = Logger.getLogger(ShipmentLoader.class);
            
    private static final String DEFAULT_SHIPPING_NUMBER = "none";
    
	private HashMap<String, Agent> shippers;
	
	private CarrierLookup carrierLookup;
	private LoanLookup loanLookup;
	private OutgoingExchangeLookup outExchangeLookup;
	private OutgoingGiftLookup outGiftLookup;
	
    public ShipmentLoader(File csvFile, 
    		              Statement sqlStatement,
    		              LoanLookup loanLookup,
    		              OutgoingExchangeLookup outExchangeLookup,
    		              OutgoingGiftLookup outGiftLookup) throws LocalException
    {
        super(csvFile, sqlStatement);


        this.loanLookup = loanLookup;
        
        this.shippers = new HashMap<String, Agent>();
        this.carrierLookup = getCarrierLookup();
        this.outExchangeLookup = outExchangeLookup;
        this.outGiftLookup = outGiftLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaShipment asaShipment = parse(columns);
        
        Integer asaShipmentId = asaShipment.getId();
        setCurrentRecordId(asaShipmentId);
        
        Shipment shipment = getShipment(asaShipment);
        
        String sql = getInsertSql(shipment);
        insert(sql);
    }
    
    public Logger getLogger()
    {
        return log;
    }
    
    public CarrierLookup getCarrierLookup()
    {
    	if (carrierLookup == null)
    	{
    		carrierLookup = new CarrierLookup() {
    			public Agent getByName(String carrierName) throws LocalException
    			{
    				Agent agent = new Agent();
    				
    				Integer agentId = getId("agent", "AgentID", "LastName", carrierName);
    				
    				agent.setAgentId(agentId);
    				
    				return agent;
    			}
    			
    			public Agent queryByName(String carrierName) throws LocalException
    			{
    				Agent agent = new Agent();
    				
    				Integer agentId = queryForInt("agent", "AgentID", "LastName", carrierName);
    				
    				if (agentId == null) return null;
    				
    				agent.setAgentId(agentId);
    				
    				return agent;
    			}
    		};
    	}
    	return carrierLookup;
    }
    
    // this is the last loader to create agent records; populate the agent_discipline table
    // when we're done
    @Override
    protected void postLoad() throws LocalException
    {
        String sql = "insert into agent_discipline(AgentID, DisciplineID) select a.AgentID, 3 from agent a where a.AgentID>1";
        execute(sql);
    }

    private AsaShipment parse(String[] columns) throws LocalException
    {
    	if (columns.length < 14)
    	{
    		throw new LocalException("Not enough columns");
    	}
    	
    	AsaShipment asaShipment = new AsaShipment();
    	try
    	{
    		asaShipment.setId(                 SqlUtils.parseInt( columns[0]  ));
    		asaShipment.setTransactionId(      SqlUtils.parseInt( columns[1]  ));
    		asaShipment.setType(           Transaction.parseType( columns[2]  ));
    		asaShipment.setCarrier(     AsaShipment.parseCarrier( columns[3]  ));
    		asaShipment.setMethod(       AsaShipment.parseMethod( columns[4]  ));
    		asaShipment.setCost(             SqlUtils.parseFloat( columns[5]  ));
    		asaShipment.setIsEstimatedCost( Boolean.parseBoolean( columns[6]  ));
    		asaShipment.setIsInsured(       Boolean.parseBoolean( columns[7]  ));
    		asaShipment.setOrdinal(            SqlUtils.parseInt( columns[8]  ));
    		asaShipment.setTrackingNumber(                        columns[9] );
    		asaShipment.setCustomsNumber(                         columns[10] );
    		asaShipment.setDescription(                           columns[11] );
    		asaShipment.setBoxCount(                              columns[12] );
            asaShipment.setCollectionCode(                        columns[13] );
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	catch (AsaException e)
    	{
    		throw new LocalException("Couldn't parse carrier/method field", e);
    	}
    	
    	return asaShipment;
    }
    
    private Shipment getShipment(AsaShipment asaShipment) throws LocalException
    {
    	Shipment shipment = new Shipment();
    	
    	// ExchangeOut/Gift/Loan (Loan, OutExchange, OutGift, OutMiscExch, OutSpecialExch)
    	Integer transactionId = asaShipment.getTransactionId();
    	checkNull(transactionId, "transaction id");
    	
    	TYPE type = asaShipment.getType();
    	
    	if (type.equals(TYPE.OutExchange) || type.equals(TYPE.OutSpecial))
    	{
    		ExchangeOut exchangeOut = lookupExchangeOut(transactionId);
    		shipment.setExchangeOut(exchangeOut);
    		shipment.setGift(new Gift());
    		shipment.setLoan(new Loan());
    	}
    	else if (type.equals(TYPE.OutGift))
    	{
    		Gift gift = lookupGift(transactionId);
    		shipment.setGift(gift);
    		shipment.setExchangeOut(new ExchangeOut());
    		shipment.setLoan(new Loan());
    	}
    	else if (type.equals(TYPE.Loan))
    	{
    		Loan loan = lookupLoan(transactionId);
    		shipment.setLoan(loan);
    		shipment.setExchangeOut(new ExchangeOut());
    		shipment.setGift(new Gift());
    	}
    	else
    	{
    		throw new LocalException("Invalid transaction type " + Transaction.toString(type));
    	}
    	
    	// InsuredForAmount (isInsured)
    	Boolean isInsured = asaShipment.isInsured();
    	shipment.setInsuredForAmount(isInsured ? "insured" : "not insured");
    	
    	// NumberOfPackages (transaction boxCount)
    	String boxCount = asaShipment.getBoxCount();
    	if (boxCount != null)
     	{
    		try
    		{
    			Short numberOfPackages = Short.parseShort(boxCount);
    			shipment.setNumberOfPackages(numberOfPackages);
    		}
    		catch (NumberFormatException e)
    		{
    			; // this field, from herb_transaction, is also saved in TransactionLoader
    		}
    	}

        
        // Number1 (cost)
        Float cost = asaShipment.getCost();
        shipment.setNumber1(cost);
        
        // Number2 (ordinal)
    	Integer ordinal = asaShipment.getOrdinal();
    	if (ordinal != null)
    	{
    		shipment.setNumber2((float) ordinal);
    	}
    	
    	// Remarks (description)
    	String description = asaShipment.getDescription();
    	shipment.setRemarks(description);
    	
    	// Shipper (carrier)
    	Agent shipper = null;

    	CARRIER carrier = asaShipment.getCarrier();
    	
    	if (! CARRIER.Unknown.equals(carrier))
    	{
    	    shipper = getAgentByCarrierName(AsaShipment.toString(carrier));
    	}
    	else
    	{
    	    shipper = new Agent();
    	}
    	shipment.setShipper(shipper);
    	
    	// ShipmentMethod (method)
    	String method = AsaShipment.toString(asaShipment.getMethod());
    	shipment.setShipmentMethod(method);
    	
    	// ShipmentNumber (trackingNumber)
    	String trackingNumber = asaShipment.getTrackingNumber();
    	if (trackingNumber == null)
    	{
    	    trackingNumber = DEFAULT_SHIPPING_NUMBER;
    	}
    	trackingNumber = truncate(trackingNumber, 50, "tracking number");
    	shipment.setShipmentNumber(trackingNumber);
    	
    	// Text1 (customsNo)
    	String customsNumber = asaShipment.getCustomsNumber();
    	shipment.setText1(customsNumber);
    	    	
    	// Text2 (transactionId)
    	shipment.setText2("Asa transaction id: " + String.valueOf(transactionId));

    	// YesNo1 (acknowledgedFlag)
        
        // YesNo2 (isCostEstimated)
        Boolean isEstimatedCost = asaShipment.isEstimatedCost();
        shipment.setYesNo2(isEstimatedCost);
    	
    	return shipment;
    }
    
    private Agent lookupCarrier(String carrierName) throws LocalException
    {
    	return carrierLookup.queryByName(carrierName);
    }
    
    private Agent getAgentByCarrierName(String carrier) throws LocalException
    {
    	Agent agent = shippers.get(carrier);
    	
    	if (agent == null)
    	{
    		agent = lookupCarrier(carrier);
    		
    		if (agent == null)
    		{
    			agent = getCarrierAgent(carrier);
    			String sql = getInsertSql(agent);
    			Integer agentId = insert(sql);
    			agent.setAgentId(agentId);
    		}
    		
    		shippers.put(carrier, agent);
    	}
    	
    	return agent;
	}
    
    private Agent getCarrierAgent(String name) throws LocalException
    {
    	Agent carrierAgent = new Agent();
    	
    	// AgentType
    	carrierAgent.setAgentType(Agent.ORG);
    	
    	// GUID
    	checkNull(name, "carrier name");
    	carrierAgent.setGuid(name);
    	
    	// LastName
    	carrierAgent.setLastName(name);
    	
    	return carrierAgent;
    }
    
    private ExchangeOut lookupExchangeOut(Integer transactionId) throws LocalException
    {
    	return outExchangeLookup.getById(transactionId);
    }

    private Gift lookupGift(Integer transactionId) throws LocalException
    {
        return outGiftLookup.getById(transactionId);
    }
    
    private Loan lookupLoan(Integer transactionId) throws LocalException
    {
    	return loanLookup.getById(transactionId);
    }

	private String getInsertSql(Shipment shipment)
    {
		String fields = "ExchangeOutID, GiftID, InsuredForAmount, LoanID, " +
                        "NumberOfPackages, Number1, Number2, Remarks, ShipperID, ShipmentMethod, " +
                        "ShipmentNumber, Text1, Text2, TimestampCreated, Version, YesNo2";

		String[] values = new String[16];

		values[0]  = SqlUtils.sqlString( shipment.getExchangeOut().getId());
		values[1]  = SqlUtils.sqlString( shipment.getGift().getId());
		values[2]  = SqlUtils.sqlString( shipment.getInsuredForAmount());
		values[3]  = SqlUtils.sqlString( shipment.getLoan().getId());
		values[4]  = SqlUtils.sqlString( shipment.getNumberOfPackages());
		values[5]  = SqlUtils.sqlString( shipment.getNumber1());
		values[6]  = SqlUtils.sqlString( shipment.getNumber2());
		values[7]  = SqlUtils.sqlString( shipment.getRemarks());
		values[8]  = SqlUtils.sqlString( shipment.getShipper().getId());
		values[9]  = SqlUtils.sqlString( shipment.getShipmentMethod());
		values[10] = SqlUtils.sqlString( shipment.getShipmentNumber());
		values[11] = SqlUtils.sqlString( shipment.getText1());
		values[12] = SqlUtils.sqlString( shipment.getText2());
		values[13] = SqlUtils.now();
		values[14] = SqlUtils.zero();
		values[15] = SqlUtils.sqlString( shipment.getYesNo2());
    	
    	return SqlUtils.getInsertSql("shipment", fields, values);
    }

	private String getInsertSql(Agent agent)
    {
    	String fieldNames = "AgentType, GUID, LastName, TimestampCreated, Version";
    	
    	String[] values = new String[5];
    	
    	values[0] = SqlUtils.sqlString( agent.getAgentType());
    	values[1] = SqlUtils.sqlString( agent.getGuid());
    	values[2] = SqlUtils.sqlString( agent.getLastName());
    	values[3] = SqlUtils.now();
    	values[4] = SqlUtils.zero();
    	
    	return SqlUtils.getInsertSql("agent", fieldNames, values);
    }
}
