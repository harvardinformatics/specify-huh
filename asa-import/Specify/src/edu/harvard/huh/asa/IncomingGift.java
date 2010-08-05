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

public class IncomingGift extends InGeoBatchTransaction
{
    /**
     * "[boxCount] boxes.  Gift includes [itemCount] general collections, [nonSpecimenCount] non-specimens,
     *  and [typeCount] types.  Quantity calculated before [discardCount] discarded, [distributeCount]
     *  distributed, and [returnCount] returned."
     */
    @Override
    public String getItemCountNote()
    {
        String boxCountNote = getBoxCountNote();
        
        Integer itemCount = getItemCount();
        Integer typeCount = getTypeCount();
        Integer nonSpecimenCount = getNonSpecimenCount();
        
        if (itemCount == null) itemCount = 0;
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        String itemCountNote = itemCount + " general collection" + (itemCount == 1 ? "" : "s");
        String nonSpecimenNote = nonSpecimenCount + " non-specimen" + (nonSpecimenCount == 1 ? "" : "s");
        String typeNote = typeCount + " type" + (typeCount == 1 ? "" : "s");
        
        Integer discardCount = getDiscardCount();
        Integer distributeCount = getDistributeCount();
        Integer returnCount = getReturnCount();
        
        if (discardCount == null) discardCount = 0;
        if (distributeCount == null) distributeCount = 0;
        if (returnCount == null) returnCount = 0;
        
        String discardNote = discardCount + " discarded";
        String distributeNote = distributeCount + " distributed";
        String returnNote = returnCount + " returned";
        
        return boxCountNote + "  Gift includes " +  itemCountNote + ", " + nonSpecimenNote + ", and " +
               typeNote + ".  Quantity calculated before " + discardNote + ", " + distributeNote + ", and " + returnNote + ".";
    }
    
}
