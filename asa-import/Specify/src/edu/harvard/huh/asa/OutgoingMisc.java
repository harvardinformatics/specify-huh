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

import edu.harvard.huh.asa.AsaShipment.CARRIER;
import edu.harvard.huh.asa.AsaShipment.METHOD;

public class OutgoingMisc extends Transaction
{
    private CARRIER carrier;
    private  METHOD method;
    private   Float cost;
    private Boolean isEstimatedCost;
    private Boolean isInsured;
    private Integer ordinal;
    private  String trackingNo;
    private  String customsNo;
    private  String shippingDesc;
    private  String boxCount;
    
    public CARRIER getCarrier() { return carrier; }
    
    public METHOD getMethod() { return method; }
    
    public Float getCost() { return cost; }
    
    public Boolean isEstimatedCost() { return isEstimatedCost; }
    
    public Boolean isInsured() { return isInsured; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public String getTrackingNumber() { return trackingNo; }
    
    public String getCustomsNumber() { return customsNo; }
    
    public String getShippingDesc() { return shippingDesc; }
    
    public String getBoxCount() { return boxCount; }

    public void setCarrier(CARRIER carrier) { this.carrier = carrier; }
    
    public void setMethod(METHOD method) { this.method = method; }
    
    public void setCost(Float cost) { this.cost = cost; }
    
    public void setIsEstimatedCost(Boolean isEstimatedCost) { this.isEstimatedCost = isEstimatedCost; }
    
    public void setIsInsured(Boolean isInsured) { this.isInsured = isInsured; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
    
    public void setTrackingNumber(String trackingNo) { this.trackingNo = trackingNo; }
    
    public void setCustomsNumber(String customsNo) { this.customsNo = customsNo; }
    
    public void setShippingDesc(String shippingDesc) { this.shippingDesc = shippingDesc; }
    
    public void setBoxCount(String boxCount) { this.boxCount = boxCount; }
}
