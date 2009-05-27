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

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.Shipment;

public class ShipmentLoader extends CsvToSqlLoader
{
    // Loan/Exchange/Gift
    
	private HashMap<String, Agent> shippers;
	
    public ShipmentLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);

        this.shippers = new HashMap<String, Agent>();
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
    
    private AsaShipment parse(String[] columns) throws LocalException
    {
    	if (columns.length < 13)
    	{
    		throw new LocalException("Wrong number of columns");
    	}
    	
    	AsaShipment asaShipment = new AsaShipment();
    	try
    	{
    		asaShipment.setId(                  SqlUtils.parseInt( StringUtils.trimToNull( columns[0]  )));
    		asaShipment.setTransactionId(       SqlUtils.parseInt( StringUtils.trimToNull( columns[1]  )));
    		asaShipment.setTransactionType( Transaction.parseType( StringUtils.trimToNull( columns[2]  )));
    		asaShipment.setCarrier(      AsaShipment.parseCarrier( StringUtils.trimToNull( columns[3]  )));
    		asaShipment.setMethod(        AsaShipment.parseMethod( StringUtils.trimToNull( columns[4]  )));
    		asaShipment.setCost(              SqlUtils.parseFloat( StringUtils.trimToNull( columns[5]  )));
    		asaShipment.setIsEstimatedCost(  Boolean.parseBoolean( StringUtils.trimToNull( columns[6]  )));
    		asaShipment.setIsInsured(        Boolean.parseBoolean( StringUtils.trimToNull( columns[7]  )));
    		asaShipment.setOrdinal(             SqlUtils.parseInt( StringUtils.trimToNull( columns[8]  )));
    		asaShipment.setTrackingNumber(                         StringUtils.trimToNull( columns[9]  ));
    		asaShipment.setCustomsNumber(                          StringUtils.trimToNull( columns[9]  ));
    		asaShipment.setDescription(                            StringUtils.trimToNull( columns[10] ));
    		asaShipment.setBoxCount(                               StringUtils.trimToNull( columns[11] ));
    		asaShipment.setBoxCount(                               StringUtils.trimToNull( columns[12] ));
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	catch (AsaException e)
    	{
    		throw new LocalException("Couldn't parse field", e);
    	}
    	
    	return asaShipment;
    }
    
    private Shipment getShipment(AsaShipment asaShipment) throws LocalException
    {
    	Shipment shipment = new Shipment();
    	
    	// Borrow/ExchangeOut/Gift/Loan TODO: check which transaction types exist in asa shipments
    	Integer transactionId = asaShipment.getTransactionId();
    	checkNull(transactionId, "transaction id");
    	
    	TYPE type = asaShipment.getTransactionType();
    	
    	if (type.equals(TYPE.OutExchange) || type.equals(TYPE.OutSpecial))
    	{
    		ExchangeOut exchangeOut = getExchangeOutByTransactionId(transactionId);
    		shipment.setExchangeOut(exchangeOut);
    	}
    	else if (type.equals(TYPE.OutGift))
    	{
    		warn("No link to deaccession", null);
    	}
    	else if (type.equals(TYPE.Loan))
    	{
    		Loan loan = getLoanByTransactionId(transactionId);
    		shipment.setLoan(loan);
    	}
    	else
    	{
    		throw new LocalException("Invalid transaction type " + type.name());
    	}
    	
    	// CollectionMemberId (transaction localUnit)
    	String collectionCode = asaShipment.getCollectionCode();
    	checkNull(collectionCode, "collection code");
    	
    	Integer collectionMemberId = getCollectionId(collectionCode);
    	shipment.setCollectionMemberId(collectionMemberId);
    	
    	// InsuredForAmount (isInsured)
    	Boolean isInsured = asaShipment.isInsured();
    	
    	if (isInsured)
    	{
    		shipment.setInsuredForAmount("insured");
    	}
    	
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
    			; // this field was already saved in TransactionLoader
    		}
    	}

    	// Number1 (ordinal)
    	Integer ordinal = asaShipment.getOrdinal();
    	if (ordinal != null)
    	{
    		shipment.setNumber1((float) ordinal);
    	}
    	
    	// Number2 (cost)
    	Float cost = asaShipment.getCost();
    	shipment.setNumber2(cost);
    	
    	// Remarks (description)
    	String description = asaShipment.getDescription();
    	shipment.setRemarks(description);
    	
    	// Shipper (carrier)
    	String carrier = asaShipment.getCarrier().name();
    	Agent shipper = getAgentByCarrierName(carrier);
    	
    	shipment.setShipper(shipper);
    	
    	// ShipmentMethod (method)
    	String method = asaShipment.getMethod().name();
    	shipment.setShipmentMethod(method);
    	
    	// ShipmentNumber (trackingNumber)
    	String trackingNumber = asaShipment.getTrackingNumber();
    	checkNull(trackingNumber, "tracking number");
    	
    	shipment.setShipmentNumber(trackingNumber);
    	
    	// Text1 (customsNo)
    	String customsNumber = asaShipment.getCustomsNumber();
    	shipment.setText1(customsNumber);
    	    	
    	// YesNo1 (isEstimatedCost)
    	Boolean isEstimatedCost = asaShipment.isCostEstimated();
    	shipment.setYesNo1(isEstimatedCost);
    	
    	return shipment;
    }
    
    private Agent getAgentByCarrierName(String carrier) throws LocalException
    {
    	Agent agent = shippers.get(carrier);
    	
    	if (agent == null)
    	{
    	    // TODO: move this to interface
    		Integer agentId = queryForInt("agent", "AgentId", "GUID", carrier);
    		
    		if (agentId != null)
    		{
    			agent = new Agent();
    			agent.setAgentId(agentId);
    		}
    		else
    		{
    			agent = getCarrierAgent(carrier);
    			String sql = getInsertSql(agent);
    			agentId = insert(sql);
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

    private ExchangeOut getExchangeOutByTransactionId(Integer transactionId) throws LocalException
    {
    	ExchangeOut exchangeOut = new ExchangeOut();
    	
    	// TODO: move this to interface
    	Integer exchangeOutId = getInt("exchangeout", "ExchangeOutId", "Number1", transactionId);
    	
    	exchangeOut.setExchangeOutId(exchangeOutId);
    	
    	return exchangeOut;
    }
    
    private Loan getLoanByTransactionId(Integer transactionId) throws LocalException
    {
    	Loan loan = new Loan();
    	
        // TODO: move this to interface
    	Integer loanId = getInt("loan", "LoanID", "Number1", transactionId);
    	
    	loan.setLoanId(loanId);
    	
    	return loan;
    }
    
	private String getInsertSql(Shipment shipment)
    {
    	String fieldNames = "";
    	
    	String[] values = new String[0];
    	
    	values[0] = SqlUtils.now();
    	return SqlUtils.getInsertSql("shipment", fieldNames, values);
    }

	private String getInsertSql(Agent agent)
    {
    	String fieldNames = "AgentType, GUID, LastName";
    	
    	String[] values = new String[3];
    	
    	values[0] = SqlUtils.sqlString( agent.getAgentType());
    	values[1] = SqlUtils.sqlString( agent.getGuid());
    	values[2] = SqlUtils.sqlString( agent.getLastName());

    	return SqlUtils.getInsertSql("shipment", fieldNames, values);
    }
}
