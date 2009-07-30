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
import java.util.Date;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.OutgoingMisc;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.CarrierLookup;

import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Shipment;

public class OutgoingMiscLoader extends TransactionLoader
{
    private static final Logger log  = Logger.getLogger(OutgoingMiscLoader.class);
    
    private static final String DEFAULT_SHIPPING_NUMBER = "none";
    
    private CarrierLookup carrierLookup;
    
    public OutgoingMiscLoader(File csvFile,
                              Statement sqlStatement,
                              CarrierLookup carrierLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.carrierLookup = carrierLookup;
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        OutgoingMisc outMisc = parse(columns);

        Integer transactionId = outMisc.getId();
        setCurrentRecordId(transactionId);
        
        Shipment shipment = getShipment(outMisc);
        
        String sql = getInsertSql(shipment);
        insert(sql);
    }
    
    public Logger getLogger()
    {
        return log;
    }
    
    private OutgoingMisc parse(String[] columns) throws LocalException
    {        
        OutgoingMisc outMisc = new OutgoingMisc();
        
        int i = super.parse(columns, outMisc);
        
        if (columns.length < i + 0)
        {
            throw new LocalException("Not enough columns");
        }
        
        try
        {
            outMisc.setCarrier(     AsaShipment.parseCarrier( columns[i + 0] ));
            outMisc.setMethod(       AsaShipment.parseMethod( columns[i + 1] ));
            outMisc.setCost(             SqlUtils.parseFloat( columns[i + 2] ));
            outMisc.setIsEstimatedCost( Boolean.parseBoolean( columns[i + 3] ));
            outMisc.setIsInsured(       Boolean.parseBoolean( columns[i + 4] ));
            outMisc.setOrdinal(            SqlUtils.parseInt( columns[i + 5] ));
            outMisc.setTrackingNumber(                        columns[i + 6] );
            outMisc.setCustomsNumber(                         columns[i + 7] );
            outMisc.setShippingDesc(                          columns[i + 8] );
            outMisc.setBoxCount(                              columns[i + 9] );
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
            throw new LocalException("Couldn't parse carrier/method field", e);
        }
        return outMisc;
    }
    
    private Agent lookupCarrier(String carrierName) throws LocalException
    {
        return this.carrierLookup.getByName(carrierName);
    }

    private Shipment getShipment(OutgoingMisc outMisc) throws LocalException
    {
        Shipment shipment = new Shipment();
        
        // CreatedByAgentID
        Integer creatorOptrId = outMisc.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        shipment.setCreatedByAgent(createdByAgent);
                    
        // CollectionMemberId (transaction localUnit)
        String collectionCode = outMisc.getLocalUnit();
        Integer collectionMemberId = Integer.valueOf(0);

        if (collectionCode != null)
        {
            getLogger().warn(rec() + "No collection code");
            collectionMemberId = getCollectionId(collectionCode);
            shipment.setCollectionMemberId(collectionMemberId);
        }
        
        // InsuredForAmount (isInsured)
        Boolean isInsured = outMisc.isInsured();
        shipment.setInsuredForAmount(isInsured ? "insured" : "not insured");
        
        // NumberOfPackages (transaction boxCount)
        String boxCount = outMisc.getBoxCount();
        if (boxCount != null)
        {
            try
            {
                Short numberOfPackages = Short.parseShort(boxCount);
                shipment.setNumberOfPackages(numberOfPackages);
            }
            catch (NumberFormatException e)
            {
                getLogger().warn(rec() + "Couldn't parse box count");
            }
        }

        // Number1 (cost)
        Float cost = outMisc.getCost();
        shipment.setNumber1(cost);
        
        // Number2 (ordinal)
        Integer ordinal = outMisc.getOrdinal();
        if (ordinal != null)
        {
            shipment.setNumber2((float) ordinal);
        }
        
        // Remarks (purpose, forUseBy, shipment description, transaction description, transaction remarks)
        String remarks = getRemarks(outMisc);
        shipment.setRemarks(remarks);

        // ShipmentDate
        Date openDate = outMisc.getOpenDate();
        if (openDate != null)
        {
            shipment.setShipmentDate(DateUtils.toCalendar(openDate));
        }
        
        // ShipmentMethod (method)
        String method = outMisc.getMethod().name();
        shipment.setShipmentMethod(method);
        
        // ShipmentNumber (trackingNumber)
        String trackingNumber = outMisc.getTrackingNumber();
        if (trackingNumber == null)
        {
            trackingNumber = DEFAULT_SHIPPING_NUMBER;
        }
        trackingNumber = truncate(trackingNumber, 50, "tracking number");
        shipment.setShipmentNumber(trackingNumber);
        
        // ShippedTo
        Agent shippedTo = lookupAgent(outMisc);
        shipment.setShippedTo(shippedTo);
        
        // Shipper (carrier)
        String carrier = outMisc.getCarrier().name();
        Agent shipper = lookupCarrier(carrier);
        
        shipment.setShipper(shipper);
        
        // Text1 (customsNo)
        String customsNumber = outMisc.getCustomsNumber();
        shipment.setText1(customsNumber);
                
        // Text2 (transactionId)
        Integer transactionId = outMisc.getId();
        shipment.setText2("Asa transaction id: " + String.valueOf(transactionId));

        // TimestampCreated
        Date dateCreated = outMisc.getDateCreated();
        shipment.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = outMisc.isAcknowledged();
        shipment.setYesNo1(isAcknowledged);
        
        // YesNo2 (isCostEstimated)
        Boolean isEstimatedCost = outMisc.isEstimatedCost();
        shipment.setYesNo2(isEstimatedCost);
        
        return shipment;
    }
    
    /**
     * "[purpose].  For use by [[forUseBy].  [shippingDesc].  [transactionDesc].  [transactionRemarks]."
     */
    private String getRemarks(OutgoingMisc outMisc)
    {        
        String purpose = Transaction.toString(outMisc.getPurpose());
        String forUseBy = outMisc.getForUseBy();
        String shippingDesc = outMisc.getShippingDesc();
        String transactionDesc = outMisc.getDescription();
        String transactionRemarks = outMisc.getRemarks();
        
        purpose = purpose.substring(0, 1).toUpperCase() + purpose.substring(1) + ".";
        if (forUseBy != null) forUseBy = "For use by " + forUseBy + ".";
        if (shippingDesc != null & ! shippingDesc.endsWith(".")) shippingDesc = shippingDesc + ".";
        if (transactionDesc != null & ! transactionDesc.endsWith(".")) transactionDesc = transactionDesc + ".";
        if (transactionRemarks != null & ! transactionRemarks.endsWith(".")) transactionRemarks = transactionRemarks + ".";
        
        String remarks = purpose;
        if (forUseBy != null) remarks = remarks + "  " + forUseBy;
        if (shippingDesc != null) remarks = remarks + "  " + shippingDesc;
        if (transactionDesc != null) remarks = remarks + "  " + transactionDesc;
        if (transactionRemarks != null) remarks = remarks + "  " + transactionRemarks;
        
        return remarks;
    }
    
    private String getInsertSql(Shipment shipment)
    {
        String fields = "CollectionMemberID, ExchangeOutID, InsuredForAmount, LoanID, NumberOfPackages, " +
                        "Number1, Number2, Remarks, ShippedToID, ShipperID, ShipmentMethod, ShipmentNumber, " +
                        "Text1, Text2, TimestampCreated, Version, YesNo2";

        String[] values = new String[17];

        values[0]  = SqlUtils.sqlString( shipment.getCollectionMemberId());
        values[1]  = SqlUtils.sqlString( shipment.getExchangeOut().getId());
        values[2]  = SqlUtils.sqlString( shipment.getInsuredForAmount());
        values[3]  = SqlUtils.sqlString( shipment.getLoan().getId());
        values[4]  = SqlUtils.sqlString( shipment.getNumberOfPackages());
        values[5]  = SqlUtils.sqlString( shipment.getNumber1());
        values[6]  = SqlUtils.sqlString( shipment.getNumber2());
        values[7]  = SqlUtils.sqlString( shipment.getRemarks());
        values[8]  = SqlUtils.sqlString( shipment.getShippedTo().getId());
        values[9]  = SqlUtils.sqlString( shipment.getShipper().getId());
        values[10] = SqlUtils.sqlString( shipment.getShipmentMethod());
        values[11] = SqlUtils.sqlString( shipment.getShipmentNumber());
        values[12] = SqlUtils.sqlString( shipment.getText1());
        values[13] = SqlUtils.sqlString( shipment.getText2());
        values[14] = SqlUtils.now();
        values[15] = SqlUtils.zero();
        values[16] = SqlUtils.sqlString( shipment.getYesNo1());
        
        return SqlUtils.getInsertSql("shipment", fields, values);
    } 
}
