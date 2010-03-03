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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Index;

/**

 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "logicalcontainer")
@org.hibernate.annotations.Table(appliesTo="logicalcontainer", indexes =
    {   @Index (name="LogicalContainerNameIDX", columnNames={"Name"}),
        @Index (name="CollectionMemIDX", columnNames={"CollectionMemberID"})
    })
public class LogicalContainer extends CollectionMember implements java.io.Serializable 
{
     // Fields

     protected Integer               logicalContainerId;
     protected Short                 type;
     protected String                name;
     protected String                description;
     protected Integer               number;
     protected Set<CollectionObject> collectionObjects;

     // Tree
     protected LogicalContainer             parent;
     protected Set<LogicalContainer>        children;

    // Constructors

    /** default constructor */
    public LogicalContainer() 
    {
        //
    }

    /** constructor with id */
    public LogicalContainer(Integer logicalContainerId) 
    {
        this.logicalContainerId = logicalContainerId;
    }

    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        logicalContainerId     = null;
        type                   = null;
        name                   = null;
        description            = null;
        number                 = null;
        parent                 = null;
        collectionObjects      = new HashSet<CollectionObject>();
        children               = new HashSet<LogicalContainer>();
    }
    // End Initializer

    // Property accessors

    /**
     *
     */
    @Id
    @GeneratedValue
    @Column(name = "LogicalContainerID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getLogicalContainerId() 
    {
        return this.logicalContainerId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.logicalContainerId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return LogicalContainer.class;
    }

    public void setLogicalContainerId(Integer logicalContainerId) 
    {
        this.logicalContainerId = logicalContainerId;
    }

    /**
     *
     */
    @Column(name = "Type", unique = false, nullable = true, insertable = true, updatable = true)
    public Short getType() 
    {
        return this.type;
    }

    public void setType(Short type) 
    {
        this.type = type;
    }

    /**
     *
     */
    @Column(name = "Name", unique = false, nullable = true, insertable = true, updatable = true, length = 200)
    public String getName() 
    {
        return this.name;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    /**
     *
     */
    @Column(name = "Description", unique = false, nullable = true, insertable = true, updatable = true, length = 512)
    public String getDescription() 
    {
        return this.description;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    /**
     *
     */
    @Column(name = "Number", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getNumber() 
    {
        return this.number;
    }

    public void setNumber(Integer number) 
    {
        this.number = number;
    }

    /**
     *
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "logicalContainer")
    @Cascade( { CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK })
    public Set<CollectionObject> getCollectionObjects() 
    {
        return this.collectionObjects;
    }

    public void setCollectionObjects(Set<CollectionObject> collectionObjects) 
    {
        this.collectionObjects = collectionObjects;
    }

    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentID")
    public LogicalContainer getParent()
    {
        return this.parent;
    }

    public void setParent(LogicalContainer parent)
    {
        this.parent = parent;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Integer getParentTableId()
    {
        return LogicalContainer.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        return parent != null ? parent.getId() : null;
    }

    
    @OneToMany(mappedBy = "parent")
    @Cascade( { CascadeType.ALL })
    public Set<LogicalContainer> getChildren()
    {
        return this.children;
    }

    public void setChildren(Set<LogicalContainer> children)
    {
        this.children = children;
    }
    
    // Add Methods

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
        return 143;
    }

}