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

public class TypeSpecimen
{
    private Integer id;
    private Integer specimenId;
    private  String collectionCode;
    private Integer taxonId;
    private  String typeStatus;
    private  String conditionality;
    private Boolean isFragment;
    private   BDate date;
    private  String verifiedBy;
    private  String nle1Designator;
    private Integer nle1PublicationId;
    private  String nle1Collation;
    private  String nle1Date;
    private  String nle2Designator;
    private Integer nle2PublicationId;
    private  String nle2Collation;
    private  String nle2Date;
    private  String remarks;
    
    public Integer getId() { return id; }
    
    public Integer getSpecimenId() { return specimenId; }
    
    public String getCollectionCode() { return collectionCode; }
    
    public Integer getTaxonId() { return taxonId; }
    
    public String getTypeStatus() { return typeStatus; }
    
    public String getConditionality() { return conditionality; }
    
    public Boolean isFragment() { return isFragment; }
    
    public BDate getDate() { return date; }
    
    public String getVerifiedBy() { return verifiedBy; }
    
    public String getNle1Designator() { return nle1Designator; }
    
    public Integer getNle1PublicationId() { return nle1PublicationId; }
    
    public String getNle1Collation() { return nle1Collation; }
    
    public String getNle1Date() { return nle1Date; }
    
    public String getNle2Designator() { return nle2Designator; }
    
    public Integer getNle2PublicationId() { return nle2PublicationId; }
    
    public String getNle2Collation() { return nle2Collation; }
    
    public String getNle2Date() { return nle2Date; }
    
    public String getRemarks() { return remarks; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setSpecimenId(Integer specimenId) { this.specimenId = specimenId; }
    
    public void setTaxonId(Integer taxonId) { this.taxonId = taxonId; }

    public void setTypeStatus(String typeStatus) { this.typeStatus = typeStatus; }
    
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
    
    public void setConditionality(String conditionality) { this.conditionality = conditionality; }
    
    public void setIsFragment(Boolean isFragment) { this.isFragment = isFragment; }
    
    public void setDate(BDate date) { this.date = date; }
    
    public void setVerifiedBy(String determinedBy) { this.verifiedBy = determinedBy; }
    
    public void setNle1Designator(String nle1Designator) { this.nle1Designator = nle1Designator; }
    
    public void setNle1PublicationId(Integer nle1PublicationId) { this.nle1PublicationId = nle1PublicationId; }
    
    public void setNle1Collation(String nle1Collation) { this.nle1Collation = nle1Collation; }
    
    public void setNle1Date(String nle1Date) { this.nle1Date = nle1Date; }
    
    public void setNle2Designator(String nle2Designator) { this.nle2Designator = nle2Designator; }
    
    public void setNle2PublicationId(Integer nle2PublicationId) { this.nle2PublicationId = nle2PublicationId; }
    
    public void setNle2Collation(String nle2Collation) { this.nle2Collation = nle2Collation; }
    
    public void setNle2Date(String nle2Date) { this.nle2Date = nle2Date; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
