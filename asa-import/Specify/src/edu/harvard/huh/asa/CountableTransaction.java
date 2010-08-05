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

public class CountableTransaction extends Transaction
{
    private Integer itemCount;
    private Integer typeCount;
    private Integer nonSpecimenCount;
    
    public Integer getItemCount() { return itemCount == null ? 0 : itemCount; }
    
    public Integer getTypeCount() { return typeCount == null ? 0 : typeCount; }
    
    public Integer getNonSpecimenCount() { return nonSpecimenCount == null ? 0 : nonSpecimenCount; }
    
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    
    public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
    
    public void setNonSpecimenCount(Integer nonSpecimenCount) { this.nonSpecimenCount = nonSpecimenCount; }

    /**
     * itemCount + typeCount + nonSpecimenCount
     */
    public short getBatchQuantity()
    {
        Integer itemCount = this.getItemCount();
        Integer typeCount = this.getTypeCount();
        Integer nonSpecimenCount = this.getNonSpecimenCount();

        if (itemCount == null) itemCount = 0;
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        return (short) (itemCount + typeCount + nonSpecimenCount);
    }
    
    /**
     * "Quantity contains [nonSpecimenCount] non-specimens and [typeCount] types."
     */
    public String getItemCountNote()
    {
        Integer typeCount = this.getTypeCount();
        Integer nonSpecimenCount = this.getNonSpecimenCount();
        
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        String nonSpecimenNote = nonSpecimenCount + " non-specimen" + (nonSpecimenCount == 1 ? "" : "s");
        String typeNote = typeCount + " type" + (typeCount == 1 ? "" : "s");
        
        return "Quantity contains " + nonSpecimenNote + " and " + typeNote + ".";
    }
}
