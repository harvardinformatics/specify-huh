/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.specify.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

/**

 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "accessionpreparation")
@org.hibernate.annotations.Table(appliesTo="accessionpreparation", indexes =
    {   @Index (name="AccPrepDspMemIDX", columnNames={"DisciplineID"})
    })
public class AccessionPreparation extends DisciplineMember implements java.io.Serializable, Comparable<AccessionPreparation>
{

    // Fields    

    protected Integer     accessionPreparationId;
    protected String      descriptionOfMaterial;
    protected String      outComments;          // Shipped Comments
    protected String      receivedComments;     // Received Comments
    protected Short       discardCount;
    protected Short       distributeCount;
    protected Short       itemCount;
    protected Short       nonSpecimenCount;
    protected Short       returnCount;
    protected Short       typeCount;

    protected Preparation preparation;
    protected Accession   accession;
    protected Geography   geography;
    protected Taxon       taxon;


    // Constructors

    /** default constructor */
    public AccessionPreparation() 
    {
        //
    }
    
    /** constructor with id */
    public AccessionPreparation(Integer loanPreparationId) 
    {
        this.accessionPreparationId = loanPreparationId;
    }

    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        accessionPreparationId = null;
        descriptionOfMaterial = null;
        outComments = null;
        receivedComments = null;
        discardCount = null;
        distributeCount = null;
        itemCount = null;
        nonSpecimenCount = null;
        returnCount = null;
        typeCount = null;
        preparation = null;
        accession = null;
        geography = null;
        taxon = null;
    }
    // End Initializer


    // Property accessors

    /**
     *      * PrimaryKey
     */
    @Id
    @GeneratedValue
    @Column(name = "AccessionPreparationID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getAccessionPreparationId() {
        return this.accessionPreparationId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.accessionPreparationId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return AccessionPreparation.class;
    }
    
    public void setAccessionPreparationId(Integer accessionPreparationId) {
        this.accessionPreparationId = accessionPreparationId;
    }

    /**
     *      * Description of loaned material (intended to be used for non-cataloged items, i.e. when PreparationID is null)
     */
    @Column(name = "DescriptionOfMaterial", unique = false, nullable = true, insertable = true, updatable = true, length=4096)
    public String getDescriptionOfMaterial() 
    {
        return this.descriptionOfMaterial;
    }
    
    public void setDescriptionOfMaterial(String descriptionOfMaterial) {
        this.descriptionOfMaterial = descriptionOfMaterial;
    }

    /**
     *      * Comments on item when loaned
     */
    @Lob
    @Column(name = "OutComments", unique = false, nullable = true, insertable = true, updatable = true, length = 1024)
    public String getOutComments() 
    {
        return this.outComments;
    }
    
    public void setOutComments(String outComments) 
    {
        this.outComments = outComments;
    }

    /**
     * @return the receivedComments
     */
    @Lob
    @Column(name = "ReceivedComments", unique = false, nullable = true, insertable = true, updatable = true, length = 1024)
    public String getReceivedComments()
    {
        return receivedComments;
    }

    /**
     * @param receivedComments the receivedComments to set
     */
    public void setReceivedComments(String receivedComments)
    {
        this.receivedComments = receivedComments;
    }
    
    /**
     * @return the geography.
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "GeographyID", unique = false, nullable = true, insertable = true, updatable = true)
    public Geography getGeography()
    {
        return this.geography;
    }

    /**
     * @param geography to set
     */
    public void setGeography(Geography geography)
    {
        this.geography = geography;
    }
    
    /**
     * @return the taxon.
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "TaxonID", unique = false, nullable = true, insertable = true, updatable = true)
    public Taxon getTaxon()
    {
        return this.taxon;
    }

    /**
     * @param higher taxon to set
     */
    public void setTaxon(Taxon taxon)
    {
        this.taxon = taxon;
    }
    

    /**
     *      * Number of specimens discarded
     */
    @Column(name = "DiscardCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getDiscardCount() {
        return this.discardCount;
    }
    
    public void setDiscardCount(Short discardCount) {
        this.discardCount = discardCount;
    }
    
    /**
     *      * Number of specimens distributed
     */
    @Column(name = "DistributeCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getDistributeCount() {
        return this.distributeCount;
    }
    
    public void setDistributeCount(Short distributeCount) {
        this.distributeCount = distributeCount;
    }
    
    /**
     *      * Number of nonspecimen received
     */
    @Column(name = "ItemCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getItemCount() {
        return this.itemCount;
    }
    
    public void setItemCount(Short itemCount) {
        this.itemCount = itemCount;
    }

    /**
     *      * Number of nonspecimen received
     */
    @Column(name = "NonSpecimenCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getNonSpecimenCount() {
        return this.nonSpecimenCount;
    }
    
    public void setNonSpecimenCount(Short nonSpecimenCount) {
        this.nonSpecimenCount = nonSpecimenCount;
    }
    
    /**
     *      * Number of specimens returned
     */
    @Column(name = "ReturnCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getReturnCount() {
        return this.returnCount;
    }
    
    public void setReturnCount(Short returnCount) {
        this.returnCount = returnCount;
    }
    
    /**
     *      * Number of types received
     */
    @Column(name = "TypeCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getTypeCount() {
        return this.typeCount;
    }
    
    public void setTypeCount(Short typeCount) {
        this.typeCount = typeCount;
    }

    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "PreparationID", unique = false, nullable = true, insertable = true, updatable = true)
    public Preparation getPreparation() {
        return this.preparation;
    }
    
    public void setPreparation(Preparation preparation) {
        this.preparation = preparation;
    }

    /**
     *      * Loan containing the Preparation
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "AccessionID", unique = false, nullable = false, insertable = true, updatable = true)
    public Accession getAccession() {
        return this.accession;
    }
    
    public void setAccession(Accession accession) {
        this.accession = accession;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Integer getParentTableId()
    {
        return Accession.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
   public Integer getParentId()
    {
        return accession != null ? accession.getId() : null;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getTableId()
     */
    @Override
    @Transient
    public int getTableId()
    {
        return getClassTableId();
    }
    
    /**
     * @return the Table ID for the class.
     */
    public static int getClassTableId()
    {
        return 144;
    }
    
    //----------------------------------------------------------------------
    //-- Comparable Interface
    //----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(AccessionPreparation obj)
    {
        return timestampCreated.compareTo(obj.timestampCreated);
    }
}
