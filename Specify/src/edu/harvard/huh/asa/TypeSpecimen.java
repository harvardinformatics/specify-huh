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
    // from st_lookup category 111
    public static enum STATUS             {  Epitype,   Isoepitype,   Holotype,   Isotype,   Lectotype,   Isolectotype,   Neotype,   Isoneotype,   Syntype,   Isosyntype,    Neosyntype,    Type,   TypeMaterial,    DrawingOfType,     PhotoOfType         };
    private static String[] StatusNames = { "Epitype", "Isoepitype", "Holotype", "Isotype", "Lectotype", "Isolectotype", "Neotype", "Isoneotype", "Syntype", "Isosyntype", "[Neosyntype]", "Type", "Type material", "Drawing of type", "Photograph of type" };
    
    // from st_lookup category 112
    public static enum CONDITIONALITY              { Possible,   Probable };
    private static String[] ConditionalityNames = { "possible", "probable" };
    
    private Integer        id;
    private Integer        specimenId;
    private String         collectionCode;
    private Integer        taxonId;
    private String         taxon;
    private STATUS         typeStatus;
    private CONDITIONALITY conditionality;
    private Boolean        isFragment;
    private BDate          date;
    private String         verifiedBy;
    private Integer        nle1DesignatorId;
    private Integer        nle1PublicationId;
    private String         nle1Collation;
    private String         nle1Date;
    private Integer        nle2DesignatorId;
    private Integer        nle2PublicationId;
    private String         nle2Collation;
    private String         nle2Date;
    private String         remarks;
    private Integer        ordinal;
    
    public static STATUS parseStatus(String string) throws AsaException
    {
        for (STATUS status : STATUS.values())
        {
            if (StatusNames[status.ordinal()].equals(string)) return status;
        }
        throw new AsaException("Invalid type status: " + string);
    }
    
    public static String toString(STATUS status)
    {
        return status.name();
    }
    
    public static CONDITIONALITY parseConditionality(String string) throws AsaException
    {
        if (string == null) return null;
        
        for (CONDITIONALITY conditionality: CONDITIONALITY.values())
        {
            if (ConditionalityNames[conditionality.ordinal()].equals(string)) return conditionality;
        }
        throw new AsaException("Invalid conditionality status: " + string);
    }

    public static String toString(CONDITIONALITY conditionality)
    {
        return conditionality.name();
    }

    public Integer getId() { return id; }
    
    public Integer getSpecimenId() { return specimenId; }
    
    public String getCollectionCode() { return collectionCode; }
    
    public Integer getTaxonId() { return taxonId; }
    
    public String getTaxon() { return taxon; }
    
    public STATUS getTypeStatus() { return typeStatus; }
    
    public CONDITIONALITY getConditionality() { return conditionality; }
    
    public Boolean isFragment() { return isFragment; }
    
    public BDate getDate() { return date; }
    
    public String getVerifiedBy() { return verifiedBy; }
    
    public Integer getNle1DesignatorId() { return nle1DesignatorId; }
    
    public Integer getNle1PublicationId() { return nle1PublicationId; }
    
    public String getNle1Collation() { return nle1Collation; }
    
    public String getNle1Date() { return nle1Date; }
    
    public Integer getNle2DesignatorId() { return nle2DesignatorId; }
    
    public Integer getNle2PublicationId() { return nle2PublicationId; }
    
    public String getNle2Collation() { return nle2Collation; }
    
    public String getNle2Date() { return nle2Date; }
    
    public String getRemarks() { return remarks; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setSpecimenId(Integer specimenId) { this.specimenId = specimenId; }
    
    public void setTaxonId(Integer taxonId) { this.taxonId = taxonId; }

    public void setTaxon(String taxon) { this.taxon = taxon; }
    
    public void setTypeStatus(STATUS typeStatus) { this.typeStatus = typeStatus; }
    
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
    
    public void setConditionality(CONDITIONALITY conditionality) { this.conditionality = conditionality; }
    
    public void setIsFragment(Boolean isFragment) { this.isFragment = isFragment; }
    
    public void setDate(BDate date) { this.date = date; }
    
    public void setVerifiedBy(String determinedBy) { this.verifiedBy = determinedBy; }
    
    public void setNle1DesignatorId(Integer nle1DesignatorId) { this.nle1DesignatorId = nle1DesignatorId; }
    
    public void setNle1PublicationId(Integer nle1PublicationId) { this.nle1PublicationId = nle1PublicationId; }
    
    public void setNle1Collation(String nle1Collation) { this.nle1Collation = nle1Collation; }
    
    public void setNle1Date(String nle1Date) { this.nle1Date = nle1Date; }
    
    public void setNle2DesignatorId(Integer nle2DesignatorId) { this.nle2DesignatorId = nle2DesignatorId; }
    
    public void setNle2PublicationId(Integer nle2PublicationId) { this.nle2PublicationId = nle2PublicationId; }
    
    public void setNle2Collation(String nle2Collation) { this.nle2Collation = nle2Collation; }
    
    public void setNle2Date(String nle2Date) { this.nle2Date = nle2Date; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
}
