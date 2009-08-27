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

import org.hibernate.annotations.Index;

import edu.ku.brc.af.ui.forms.FormDataObjIFace;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Created Date: Aug 15, 2007
 *
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name="paleocontext")
@org.hibernate.annotations.Table(appliesTo="paleocontext", indexes =
    {   @Index (name="PaleoCxtColMemIDX", columnNames={"CollectionMemberID"})
    })
public class PaleoContext extends CollectionMember implements Cloneable
{
    protected Integer paleoContextId;
    protected Float   topDistance;
    protected Float   bottomDistance;
    protected String  distanceUnits; // "ft" or "m"
    protected String  direction;     // "up" or "down"
    protected String  positionState; // float or in-situ
    
    protected String  text1;
    protected String  text2;
    protected Boolean yesNo1;
    protected Boolean yesNo2;
    
    protected Set<CollectionObject> collectionObjects;
    
    protected LithoStrat            lithoStrat;
    protected GeologicTimePeriod    bioStrat;
    protected GeologicTimePeriod    chronosStrat;
    
    /**
     * Constructor.
     */
    public PaleoContext()
    {
        super();
    }
    
    /**
     * @return the paleoContextId
     */
    @Id
    @GeneratedValue
    @Column(name="PaleoContextID", unique=false, nullable=false, insertable=true, updatable=true)
    public Integer getPaleoContextId()
    {
        return paleoContextId;
    }

    /**
     * @param paleoContextId the paleoContextId to set
     */
    public void setPaleoContextId(Integer paleoContextId)
    {
        this.paleoContextId = paleoContextId;
    }
    

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#initialize()
     */
    @Override
    public void initialize()
    {
        super.init();
        
        paleoContextId = null;
        topDistance    = null;
        bottomDistance = null;
        distanceUnits  = null;
        direction      = null;
        positionState  = null;
        
        text1          = null;
        text2          = null;
        yesNo1         = null;
        yesNo2         = null;
        
        collectionObjects = new HashSet<CollectionObject>();

    }

    /**
     * @return the direction
     */
    @Column(name="Direction", unique=false, nullable=true, insertable=true, updatable=true, length=32)
    public String getDirection()
    {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    /**
     * @return the distanceUnits
     */
    @Column(name="DistanceUnits", unique=false, nullable=true, insertable=true, updatable=true, length=16)
    public String getDistanceUnits()
    {
        return distanceUnits;
    }

    /**
     * @param distanceUnits the distanceUnits to set
     */
    public void setDistanceUnits(String distanceUnits)
    {
        this.distanceUnits = distanceUnits;
    }

    /**
     * @return the topDistance
     */
    @Column(name="TopDistance", unique=false, nullable=true, insertable=true, updatable=true)
    public Float getTopDistance()
    {
        return topDistance;
    }

    /**
     * @param topDistance the topDistance to set
     */
    public void setTopDistance(Float topDistance)
    {
        this.topDistance = topDistance;
    }

    /**
     * @return the bottomDistance
     */
    @Column(name="BottomDistance", unique=false, nullable=true, insertable=true, updatable=true)
    public Float getBottomDistance()
    {
        return bottomDistance;
    }

    /**
     * @param bottomDistance the bottomDistance to set
     */
    public void setBottomDistance(Float bottomDistance)
    {
        this.bottomDistance = bottomDistance;
    }

    /**
     * @return the positionState
     */
    @Column(name="PositionState", unique=false, nullable=true, insertable=true, updatable=true, length=32)
    public String getPositionState()
    {
        return positionState;
    }

    /**
     * @param positionState the positionState to set
     */
    public void setPositionState(String positionState)
    {
        this.positionState = positionState;
    }

    public void setCollectionObjects(Set<CollectionObject> collectionObjects) 
    {
        this.collectionObjects = collectionObjects;
    }

    /**
     * @return the text1
     */
    @Column(name="Text1", unique=false, nullable=true, insertable=true, updatable=true, length=32)
    public String getText1()
    {
        return text1;
    }

    /**
     * @param text1 the text1 to set
     */
    public void setText1(String text1)
    {
        this.text1 = text1;
    }

    /**
     * @return the text2
     */
    @Column(name="Text2", unique=false, nullable=true, insertable=true, updatable=true, length=32)
    public String getText2()
    {
        return text2;
    }

    /**
     * @param text2 the text2 to set
     */
    public void setText2(String text2)
    {
        this.text2 = text2;
    }

    /**
     * @return the yesNo1
     */
    @Column(name="YesNo1", unique=false, nullable=true, insertable=true, updatable=true)
    public Boolean getYesNo1()
    {
        return yesNo1;
    }

    /**
     * @param yesNo1 the yesNo1 to set
     */
    public void setYesNo1(Boolean yesNo1)
    {
        this.yesNo1 = yesNo1;
    }

    /**
     * @return the yesNo2
     */
    @Column(name="YesNo2", unique=false, nullable=true, insertable=true, updatable=true)
    public Boolean getYesNo2()
    {
        return yesNo2;
    }

    /**
     * @param yesNo2 the yesNo2 to set
     */
    public void setYesNo2(Boolean yesNo2)
    {
        this.yesNo2 = yesNo2;
    }
    
    /**
     *
     */
    @OneToMany(cascade={}, fetch=FetchType.LAZY, mappedBy="paleoContext")
    public Set<CollectionObject> getCollectionObjects() 
    {
        return this.collectionObjects;
    }

    /**
     * @return the bioStrat
     */
    @ManyToOne
    @JoinColumn(name="BioStratID", unique=false, nullable=true, insertable=true, updatable=true)
    public GeologicTimePeriod getBioStrat()
    {
        return bioStrat;
    }

    /**
     * @param bioStrat the bioStrat to set
     */
    public void setBioStrat(GeologicTimePeriod bioStrat)
    {
        this.bioStrat = bioStrat;
    }

    /**
     * @return the chronosStrat
     */
    /**
    *
    */
    @ManyToOne
    @JoinColumn(name="ChronosStratID", unique=false, nullable=true, insertable=true, updatable=true)
    public GeologicTimePeriod getChronosStrat()
    {
        return chronosStrat;
    }

    /**
     * @param chronosStrat the chronosStrat to set
     */
    public void setChronosStrat(GeologicTimePeriod chronosStrat)
    {
        this.chronosStrat = chronosStrat;
    }

    /**
     * @return the lithoStrat
     */
    @ManyToOne
    @JoinColumn(name="LithoStratID", unique=false, nullable=true, insertable=true, updatable=true)
    public LithoStrat getLithoStrat()
    {
        return lithoStrat;
    }

    /**
     * @param lithoStrat the lithoStrat to set
     */
    public void setLithoStrat(LithoStrat lithoStrat)
    {
        this.lithoStrat = lithoStrat;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return PaleoContext.class;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getId()
     */
    @Transient
    @Override
    public Integer getId()
    {
        return paleoContextId;
    }
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Short getParentTableId()
    {
        return (short)CollectionObject.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        
        if (collectionObjects != null && collectionObjects.size() == 1)
        {
            return ((FormDataObjIFace)collectionObjects.toArray()[0]).getId();
        }
        return null;
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
        return 32;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        PaleoContext pc = (PaleoContext)super.clone();
        pc.init();
        
        pc.paleoContextId    = null;
        pc.collectionObjects = new HashSet<CollectionObject>();
        
        return pc;
    }

}
