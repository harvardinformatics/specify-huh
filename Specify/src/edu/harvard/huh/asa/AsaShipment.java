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
package edu.harvard.huh.asa;

import edu.harvard.huh.asa.Transaction.TYPE;

public class AsaShipment
{
    // from st_lookup category 156
    public static enum CARRIER             {  USPS,   FedEx,   UPS,   Unknown     };
    private static String[] CarrierNames = { "USPS", "FedEx", "UPS", "NA/Unknown" };

    // from st_lookup category 157
    public static enum METHOD             {  Air,   Express,   LibraryRate,    SeaMail,    FirstClass,    Ground,   Priority,   Unknown     };
    private static String[] MethodNames = { "Air", "Express", "Library rate", "Sea mail", "First class", "Ground", "Priority", "NA/Unknown" };

    public static CARRIER parseCarrier(String string) throws AsaException
    {
        for (CARRIER carrier : CARRIER.values())
        {
            if (CarrierNames[carrier.ordinal()].equals(string)) return carrier;
        }
        throw new AsaException("Invalid shipment carrier: " + string);
    }
    
    public static METHOD parseMethod(String string) throws AsaException
    {
        for (METHOD method : METHOD.values())
        {
            if (MethodNames[method.ordinal()].equals(string)) return method;
        }
        throw new AsaException("Invalid shipment method: " + string);
    }
    
    public static String toString(CARRIER carrier)
    {
        return carrier.name();
    }

    public static String toString(METHOD method)
    {
        return method.name();
    }
    
    private Integer id;
    private Integer transactionId;
    private    TYPE type;
    private CARRIER carrier;
    private  METHOD method;
    private   Float cost;
    private Boolean isEstimatedCost;
    private Boolean isInsured;
    private Integer ordinal;
    private  String trackingNo;
    private  String customsNo;
    private  String description;
    private  String boxCount;
    private  String collectionCode;
    
    public Integer getId() { return id; }
    
    public Integer getTransactionId() { return transactionId; }
    
    public CARRIER getCarrier() { return carrier; }
    
    public METHOD getMethod() { return method; }
    
    public TYPE getType() { return type; }
    
    public Float getCost() { return cost; }
    
    public Boolean isEstimatedCost() { return isEstimatedCost; }
    
    public Boolean isInsured() { return isInsured; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public String getTrackingNumber() { return trackingNo; }
    
    public String getCustomsNumber() { return customsNo; }
    
    public String getDescription() { return description; }
    
    public String getBoxCount() { return boxCount; }
    
    public String getCollectionCode() { return collectionCode; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
    
    public void setCarrier(CARRIER carrier) { this.carrier = carrier; }
    
    public void setMethod(METHOD method) { this.method = method; }
    
    public void setType(TYPE type) { this.type = type; }
    
    public void setCost(Float cost) { this.cost = cost; }
    
    public void setIsEstimatedCost(Boolean isEstimatedCost) { this.isEstimatedCost = isEstimatedCost; }
    
    public void setIsInsured(Boolean isInsured) { this.isInsured = isInsured; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
    
    public void setTrackingNumber(String trackingNo) { this.trackingNo = trackingNo; }
    
    public void setCustomsNumber(String customsNo) { this.customsNo = customsNo; }
    
    public void setDescription(String description) { this.description = description; }
    
    public void setBoxCount(String boxCount) { this.boxCount = boxCount; }
    
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
}
