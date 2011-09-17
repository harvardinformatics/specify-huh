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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Index;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "collobjbarcode")
@org.hibernate.annotations.Table(appliesTo="collobjbarcode", indexes =
    {   @Index (name="BarcodeIDX", columnNames={"Barcode"})
    })
public class CollObjBarcode extends DataModelObjBase implements java.io.Serializable {

    // Fields    

     protected Integer collObjBarcodeId;
     protected String  barcode;
     protected String  remarks;
     
     protected Set<CollectionObject> collectionObjects;

    // Constructors

    /** default constructor */
    public CollObjBarcode() 
    {
        //
    }
    
    /** constructor with id */
    public CollObjBarcode(Integer collObjBarcodeId) {
        this.collObjBarcodeId = collObjBarcodeId;
    }
   
    
    

    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        collObjBarcodeId = null;
        barcode = null;
        remarks = null;
        
        collectionObjects = new HashSet<CollectionObject>();
    }
    // End Initializer

    // Property accessors

    @Id
    @GeneratedValue
    @Column(name = "CollObjBarcodeID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getCollObjBarcodeId() {
        return this.collObjBarcodeId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.collObjBarcodeId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return CollObjBarcode.class;
    }
    
    public void setCollObjBarcodeId(Integer collObjBarcodeId) {
        this.collObjBarcodeId = collObjBarcodeId;
    }

    @Column(name = "Barcode", unique = true, nullable = false, insertable = true, updatable = true, length = 64)
    public String getBarcode() {
        return this.barcode;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @Lob
    @Column(name = "Remarks", length = 4096)
    public String getRemarks() {
        return this.remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @ManyToMany(mappedBy="barcodes")
    @Cascade( {CascadeType.ALL, CascadeType.DELETE_ORPHAN} )
    public Set<CollectionObject> getCollectionObjects()
    {
        return collectionObjects;
    }
    
    public void setCollectionObjects(Set<CollectionObject> collectionObjects)
    {
        this.collectionObjects = collectionObjects;
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
    
    public static int getClassTableId()
    {
        return 146;
    }


}
