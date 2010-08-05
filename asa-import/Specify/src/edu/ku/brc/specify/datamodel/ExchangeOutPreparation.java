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
@Table(name = "exchangeoutpreparation")
@org.hibernate.annotations.Table(appliesTo="exchangeoutpreparation", indexes =
    {   @Index (name="ExchOutPrepDspMemIDX", columnNames={"DisciplineID"})
    })
public class ExchangeOutPreparation extends DisciplineMember implements java.io.Serializable, Comparable<ExchangeOutPreparation>
{

    // Fields    

    protected Integer                       exchangeOutPreparationId;
    protected Integer                       itemCount;
    protected Integer                       nonSpecimenCount;
    protected Integer                       typeCount;
    protected String                        descriptionOfMaterial;
    protected String                        outComments;          // Shipped Comments
    protected String                        inComments;           // Returned Comments
    protected String                        receivedComments;     // Received Comments
    protected Preparation                   preparation;
    protected ExchangeOut                   exchangeOut;
    // Constructors

    /** default constructor */
    public ExchangeOutPreparation() {
        //
    }
    
    /** constructor with id */
    public ExchangeOutPreparation(Integer exchangeOutPreparationId) 
    {
        this.exchangeOutPreparationId = exchangeOutPreparationId;
    }
   
    
    

    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        exchangeOutPreparationId = null;
        itemCount = null;
        nonSpecimenCount = null;
        typeCount = null;
        descriptionOfMaterial = null;
        outComments = null;
        inComments = null;
        receivedComments = null;
        preparation = null;
        exchangeOut = null;
    }
    // End Initializer

    /**
     * PrimaryKey
     */
    @Id
    @GeneratedValue
    @Column(name = "ExchangeOutPreparationID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getExchangeOutPreparationId() {
        return this.exchangeOutPreparationId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.exchangeOutPreparationId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return ExchangeOutPreparation.class;
    }
    
    public void setExchangeOutPreparationId(Integer exchangeOutPreparationId) {
        this.exchangeOutPreparationId = exchangeOutPreparationId;
    }

    /**
     *      * Number of general collections items given
     */
    @Column(name = "ItemCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getItemCount() {
        return this.itemCount;
    }
    
    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }
    
    /**
     *      * Number of nonspecimens given
     */
    @Column(name = "NonSpecimenCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getNonSpecimenCount() {
        return this.nonSpecimenCount;
    }
    
    public void setNonSpecimenCount(Integer nonSpecimenCount) {
        this.nonSpecimenCount = nonSpecimenCount;
    }
    
    /**
     *      * Number of types given
     */
    @Column(name = "TypeCount", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getTypeCount() {
        return this.typeCount;
    }
    
    public void setTypeCount(Integer typeCount) {
        this.typeCount = typeCount;
    }
    
    /**
     * Description of exchanged material (intended to be used for non-cataloged items, i.e. when PreparationID is null)
     */
    @Column(name = "DescriptionOfMaterial", unique = false, nullable = true, insertable = true, updatable = true, length = 512)
    public String getDescriptionOfMaterial() {
        return this.descriptionOfMaterial;
    }
    
    public void setDescriptionOfMaterial(String descriptionOfMaterial) {
        this.descriptionOfMaterial = descriptionOfMaterial;
    }

    /**
     * Comments on item when exchanged
     */
    @Lob
    @Column(name = "OutComments", unique = false, nullable = true, insertable = true, updatable = true, length = 1024)
    public String getOutComments() {
        return this.outComments;
    }
    
    public void setOutComments(String outComments) {
        this.outComments = outComments;
    }

    /**
     * Comments on item when returned
     */
    @Lob
    @Column(name = "InComments", unique = false, nullable = true, insertable = true, updatable = true, length = 1024)
    public String getInComments() {
        return this.inComments;
    }
    
    public void setInComments(String inComments) {
        this.inComments = inComments;
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
     * 
     */
    @ManyToOne(cascade = {javax.persistence.CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name = "PreparationID", unique = false, nullable = true, insertable = true, updatable = true)
    public Preparation getPreparation() {
        return this.preparation;
    }
    
    public void setPreparation(Preparation preparation) {
        this.preparation = preparation;
    }

    /**
     * ExchangeOut containing the Preparation
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "ExchangeOutID", unique = false, nullable = true, insertable = true, updatable = true)
    public ExchangeOut getExchangeOut() {
        return this.exchangeOut;
    }
    
    public void setExchangeOut(ExchangeOut exchangeOut) 
    {
        this.exchangeOut = exchangeOut;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Integer getParentTableId()
    {
        return ExchangeOut.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        return exchangeOut != null ? exchangeOut.getId() : null;
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
        return 138;
    }
    
    //----------------------------------------------------------------------
    //-- Comparable Interface
    //----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ExchangeOutPreparation obj)
    {
        return timestampCreated.compareTo(obj.timestampCreated);
    }
}
