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

import java.util.Date;

public class TaxonBatchTransaction extends CountableTransaction
{
    private Date    originalDueDate;
    private Date    currentDueDate;
    private String  higherTaxon;
    private String  taxon;
    private String  transferredFrom;
    
    public Date getOriginalDueDate() { return originalDueDate; }
    
    public Date getCurrentDueDate() { return currentDueDate; }
    
    public String getHigherTaxon() { return higherTaxon; }
    
    public String getTaxon() { return taxon; }
    
    public String getTransferredFrom() { return transferredFrom; }
    
    public void setOriginalDueDate(Date originalDueDate) { this.originalDueDate = originalDueDate; }
    
    public void setCurrentDueDate(Date currentDueDate) { this.currentDueDate = currentDueDate; }
    
    public void setHigherTaxon(String higherTaxon) { this.higherTaxon = higherTaxon; }
    
    public void setTaxon(String taxon) { this.taxon = taxon; }
    
    public void setTransferredFrom(String transferredFrom) { this.transferredFrom = transferredFrom; }
}
