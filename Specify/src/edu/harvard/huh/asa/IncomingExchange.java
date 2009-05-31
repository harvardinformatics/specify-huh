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

public class IncomingExchange extends Transaction
{
    private String  geoUnit;
    private Integer itemCount;
    private Integer typeCount;
    private Integer nonSpecimenCount;
    private Integer discardCount;
    private Integer distributeCount;
    private Integer returnCount;
    private Float   cost;
    
    public String getGeoUnit() { return geoUnit; }
    
    public Integer getItemCount() { return itemCount; }
    
    public Integer getTypeCount() { return typeCount; }
    
    public Integer getNonSpecimenCount() { return nonSpecimenCount; }
    
    public Integer getDiscardCount() { return discardCount; }
    
    public Integer getDistributeCount() { return distributeCount; }
    
    public Integer getReturnCount() { return returnCount; }
    
    public Float getCost() { return cost; }
    
    public void setGeoUnit(String geoUnit) { this.geoUnit = geoUnit; }
    
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    
    public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
    
    public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }
    
    public void setDiscardCount(Integer discardCount) { this.discardCount = discardCount; }
    
    public void setDistributeCount(Integer distributeCount) { this.distributeCount = distributeCount; }
    
    public void setReturnCount(Integer returnCount) { this.returnCount = returnCount; }
    
    public void setCost(Float cost) { this.cost = cost; }
}
