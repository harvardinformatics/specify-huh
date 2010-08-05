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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author mkelly
 *
 * @code_status Alpha
 *
 * Created Date: Feb 22, 2010
 *
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "referenceworkidentifier")
public class ReferenceWorkIdentifier extends DataModelObjBase implements java.io.Serializable {

    // Fields    

     protected Integer          referenceWorkIdentifierId;
     protected String           identifier;
     protected String           type;
     protected ReferenceWork    referenceWork;


    // Constructors

    /** default constructor */
    public ReferenceWorkIdentifier() 
    {
        //
    }
    
    /** constructor with id */
    public ReferenceWorkIdentifier(Integer referenceWorkIdentifierId) {
        this.referenceWorkIdentifierId = referenceWorkIdentifierId;
    }
   
    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        referenceWorkIdentifierId = null;
        identifier = null;
        type = null;
        referenceWork = null;
    }
    // End Initializer

    // Property accessors

    /**
     * 
     */
    @Id
    @GeneratedValue
    @Column(name = "ReferenceWorkIdentifierID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getReferenceWorkIdentifierId() {
        return this.referenceWorkIdentifierId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.referenceWorkIdentifierId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return ReferenceWorkIdentifier.class;
    }
    
    public void setReferenceWorkIdentifierId(Integer referenceWorkIdentifierId) {
        this.referenceWorkIdentifierId = referenceWorkIdentifierId;
    }

    /**
     * 
     */
    @Column(name = "Identifier", unique = false, nullable = false, insertable = true, updatable = true, length = 32)
    public String getIdentifier() {
        return this.identifier;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     *      * ID of object identified by Identifier
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "ReferenceWorkID", unique = false, nullable = false, insertable = true, updatable = true)
    public ReferenceWork getReferenceWork() {
        return this.referenceWork;
    }
    
    public void setReferenceWork(ReferenceWork referenceWork) {
        this.referenceWork = referenceWork;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Integer getParentTableId()
    {
        return ReferenceWork.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        return referenceWork != null ? referenceWork.getId() : null;
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
        return 140;
    }

    /**
     * @return the institution
     */
    @Column(name = "Type", unique = false, insertable = true, updatable = true, length = 12)
    public String getType()
    {
        return this.type;
    }

    /**
     * @param institution the institution to set
     */
    public void setType(String type)
    {
        this.type = type;
    }

}
