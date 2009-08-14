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
    // from st_lookup category 128
    public static enum QUALIFIER              {  Affine, Compare, InQuestion, Not  };
    private static String[] QualifierNames  = { "aff.",  "cf.",   "?",        "not" };

    public static QUALIFIER parseQualifier(String string) throws AsaException
    {
        if (string == null) return null;
        
        for (QUALIFIER qualifier : QUALIFIER.values())
        {
            if (QualifierNames[qualifier.ordinal()].equals(string)) return qualifier;
        }
        throw new AsaException("Invalid taxon status: " + string);
    }
    
    public static String toString(QUALIFIER qualifier)
    {
        return qualifier.name();
    }
    
    private Integer   id;
    private Integer   specimenId;
    private String    collectionCode;
    private Integer   taxonId;
    private QUALIFIER qualifier;
    private BDate     date;
    private Boolean   isCurrent;
    private Boolean   isLabel;
    private String    determinedBy;
    private String    labelText;
    private Integer   ordinal;
    private String    remarks;
    
    public Integer getId() { return id; }
    
    public Integer getSpecimenId() { return specimenId; }
    
    public String getCollectionCode() { return collectionCode; }
    
    public Integer getTaxonId() { return taxonId; }
    
    public QUALIFIER getQualifier() { return qualifier; }
    
    public BDate getDate() { return date; }
    
    public Boolean isCurrent() { return isCurrent; }
    
    public Boolean isLabel() { return isLabel; }
    
    public String getDeterminedBy() { return determinedBy; }
    
    public String getLabelText() { return labelText; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public String getRemarks() { return remarks; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setSpecimenId(Integer specimenId) { this.specimenId = specimenId; }
    
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
    
    public void setTaxonId(Integer taxonId) { this.taxonId = taxonId; }

    public void setQualifier(QUALIFIER qualifier) { this.qualifier = qualifier; }
    
    public void setDate(BDate date) { this.date = date; }
    
    public void setCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
    
    public void setIsLabel(Boolean isLabel) { this.isLabel = isLabel; }
    
    public void setDeterminedBy(String determinedBy) { this.determinedBy = determinedBy; }
    
    public void setLabelText(String labelText) { this.labelText = labelText; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
