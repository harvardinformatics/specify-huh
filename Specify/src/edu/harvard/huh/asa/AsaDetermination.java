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

public class AsaDetermination
{
    private Integer id;
    private Integer specimenId;
    private Integer taxonId;
    private  String qualifier;
    private   BDate date;
    private Boolean isCurrent;
    private Boolean isLabel;
    private  String determinedBy;
    private  String labelText;
    private Integer ordinal;
    private  String remarks;
    
    public Integer getId() { return id; }
    
    public Integer getSpecimenId() { return specimenId; }
    
    public Integer getTaxonId() { return taxonId; }
    
    public String getQualifier() { return qualifier; }
    
    public BDate getDate() { return date; }
    
    public Boolean isCurrent() { return isCurrent; }
    
    public Boolean isLabel() { return isLabel; }
    
    public String getDeterminedBy() { return determinedBy; }
    
    public String getLabelText() { return labelText; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public String getRemarks() { return remarks; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setSpecimenId(Integer specimenId) { this.specimenId = specimenId; }
    
    public void setTaxonId(Integer taxonId) { this.taxonId = taxonId; }

    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    
    public void setDate(BDate date) { this.date = date; }
    
    public void setCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
    
    public void setIsLabel(Boolean isLabel) { this.isLabel = isLabel; }
    
    public void setDeterminedBy(String determinedBy) { this.determinedBy = determinedBy; }
    
    public void setLabelText(String labelText) { this.labelText = labelText; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
