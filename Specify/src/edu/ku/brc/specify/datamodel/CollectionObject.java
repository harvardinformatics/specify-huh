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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Index;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.AttributeIFace;
import edu.ku.brc.dbsupport.AttributeProviderIFace;

/**

 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "collectionobject", uniqueConstraints = {
        @UniqueConstraint(columnNames={"CollectionID", "CatalogNumber"} ) 
        } 
)
@org.hibernate.annotations.Table(appliesTo="collectionobject", indexes =
    {   @Index (name="FieldNumberIDX", columnNames={"FieldNumber"}),
        @Index (name="CatalogedDateIDX", columnNames={"CatalogedDate"}),
        @Index (name="CatalogNumberIDX", columnNames={"CatalogNumber"}),
        @Index (name="ColObjGuidIDX", columnNames={"GUID"}),
        @Index (name="COColMemIDX", columnNames={"CollectionmemberID"})
    })
public class CollectionObject extends CollectionMember implements AttachmentOwnerIFace<CollectionObjectAttachment>, 
                                                                  java.io.Serializable, 
                                                                  AttributeProviderIFace, 
                                                                  Comparable<CollectionObject>
{

    // Fields

    protected Integer                       collectionObjectId;
    protected String                        fieldNumber;
    protected String                        description;
    protected String                        projectNumber;
    protected String                        text1;
    protected String                        text2;
    protected String                        text3;
    protected String                        text4;
    protected Float                         number1;
    protected Float                         number2;
    protected Boolean                       yesNo1;
    protected Boolean                       yesNo2;
    protected Boolean                       yesNo3;
    protected Boolean                       yesNo4;
    protected Boolean                       yesNo5;
    protected Boolean                       yesNo6;
    protected Integer                       countAmt;
    protected String                        internalRemarks;
    protected String                        remarks;
    protected String                        name;
    protected String                        modifier;
    protected Calendar                      catalogedDate;
    protected Byte                          catalogedDatePrecision;   // Accurate to Year, Month, Day
    protected String                        catalogedDateVerbatim;
    protected String                        guid;
    protected String                        altCatalogNumber;
    protected String                        catalogNumber;
    protected String                        availability;
    protected String                        restrictions;
    protected String                        notifications;
    protected BigDecimal                    totalValue;
    
    // Security
    protected Byte                          visibility;
    protected SpecifyUser                   visibilitySetBy;
    
    // Relationships
    protected Container                     container;
    protected CollectingEvent               collectingEvent;
    protected Set<Fragment>                 fragments;
    protected Set<Project>                  projects;
    // protected Set<DeaccessionPreparation> deaccessionPreparations;
    protected Set<OtherIdentifier>          otherIdentifiers;
    protected Collection                    collection;
    protected Agent                         cataloger;
    protected CollectionObjectAttribute     collectionObjectAttribute; // Specify 5 Attributes table
    protected Set<CollectionObjectAttr>     collectionObjectAttrs;      // Generic Expandable Attributes
    protected PaleoContext                  paleoContext;
    protected Set<DNASequence>              dnaSequences;
    protected FieldNotebookPage             fieldNotebookPage;
    
    
    protected Set<CollectionObjectAttachment> collectionObjectAttachments;

    
    // Constructors

    /** default constructor */
    public CollectionObject()
    {
        // do nothing
    }

    /** constructor with id */
    public CollectionObject(Integer collectionObjectId) 
    {
        this.collectionObjectId = collectionObjectId;
    }

    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        collectionObjectId    = null;
        fieldNumber           = null;
        description           = null;
        text1                 = null;
        text2                 = null;
        text3                 = null;
        text4                 = null;
        number1               = null;
        number2               = null;
        yesNo1                = null;
        yesNo2                = null;
        yesNo3                = null;
        yesNo4                = null;
        yesNo5                = null;
        yesNo6                = null;
        countAmt              = null;
        internalRemarks       = null;
        remarks               = null;
        name                  = null;
        modifier              = null;
        catalogedDate         = null;
        catalogedDateVerbatim = null;
        guid                  = null;
        altCatalogNumber      = null;
        catalogNumber         = null;
        availability          = null;
        restrictions          = null;
        notifications         = null;
        totalValue            = null;
        visibility            = null;
        visibilitySetBy       = null; 
        
        container             = null;
        collectingEvent       = null;
        collectionObjectAttrs = new HashSet<CollectionObjectAttr>();
        fragments             = new HashSet<Fragment>();
        projects              = new HashSet<Project>();
        //deaccessionPreparations = new HashSet<DeaccessionPreparation>();
        otherIdentifiers      = new HashSet<OtherIdentifier>();
        collection            = null;
        cataloger             = null;
        paleoContext          = null;
        dnaSequences          = new HashSet<DNASequence>();
        fieldNotebookPage     = null;
        
        collectionObjectAttachments = new HashSet<CollectionObjectAttachment>();
        
    }
    // End Initializer
    
    public void initForSearch()
    {
        collection = new Collection();
        collection.initialize();
        
        cataloger  = new Agent();
        cataloger.initialize();
    }

    // Property accessors

    /**
     *
     */
    @Id
    @GeneratedValue
    @Column(name = "CollectionObjectID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getCollectionObjectId() {
        return this.collectionObjectId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.collectionObjectId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return CollectionObject.class;
    }

    public void setCollectionObjectId(Integer collectionObjectId) {
        this.collectionObjectId = collectionObjectId;
    }

    /**
     *      * BiologicalObject (Bird, Fish, etc)
     */
    @Column(name = "FieldNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 50)
    public String getFieldNumber()
    {
        return this.fieldNumber;
    }

    public void setFieldNumber(String fieldNumber)
    {
        this.fieldNumber = fieldNumber;
    }

    /**
     * Image, Sound, Preparation, Container(Container Label?) - this was suppose to be in Preparation
     */
    @Lob
    @Column(name = "Description", unique = false, nullable = true, insertable = true, updatable = true, length = 1024)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *      * User definable
     */
    @Column(name = "Text1", length=300, unique = false, nullable = true, insertable = true, updatable = true)
    public String getText1() {
        return this.text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    /**
     *      * User definable
     */
    @Column(name = "Text2", length=300, unique = false, nullable = true, insertable = true, updatable = true)
    public String getText2() {
        return this.text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }

    /**
     *      * User definable
     */
    @Column(name = "Text3", length=300, unique = false, nullable = true, insertable = true, updatable = true)
    public String getText3() {
        return this.text3;
    }

    public void setText3(String text3) {
        this.text3 = text3;
    }
    
    /**
     *      * User definable
     */
    @Column(name = "Text4", length=50, unique = false, nullable = true, insertable = true, updatable = true)
    public String getText4() {
        return this.text4;
    }

    public void setText4(String text4) {
        this.text4 = text4;
    }
    
    /**
     *      * User definable
     */
    @Column(name = "Number1", unique = false, nullable = true, insertable = true, updatable = true, length = 24)
    public Float getNumber1() {
        return this.number1;
    }

    public void setNumber1(Float number1) {
        this.number1 = number1;
    }

    /**
     *      * User definable
     */
    @Column(name = "Number2", unique = false, nullable = true, insertable = true, updatable = true, length = 24)
    public Float getNumber2() {
        return this.number2;
    }

    public void setNumber2(Float number2) {
        this.number2 = number2;
    }

    /**
     *      * User definable
     */
    @Column(name="YesNo1",unique=false,nullable=true,updatable=true,insertable=true)
    public Boolean getYesNo1() {
        return this.yesNo1;
    }

    public void setYesNo1(Boolean yesNo1) {
        this.yesNo1 = yesNo1;
    }

    /**
     *      * User definable
     */
    @Column(name="YesNo2",unique=false,nullable=true,updatable=true,insertable=true)
    public Boolean getYesNo2() {
        return this.yesNo2;
    }

    public void setYesNo2(Boolean yesNo2) {
        this.yesNo2 = yesNo2;
    }

    /**
     * @return the yesNo3
     */
    @Column(name="YesNo3",unique=false,nullable=true,updatable=true,insertable=true)
    public Boolean getYesNo3()
    {
        return yesNo3;
    }

    /**
     * @param yesNo3 the yesNo3 to set
     */
    public void setYesNo3(Boolean yesNo3)
    {
        this.yesNo3 = yesNo3;
    }

    /**
     * @return the yesNo4
     */
    @Column(name="YesNo4",unique=false,nullable=true,updatable=true,insertable=true)
    public Boolean getYesNo4()
    {
        return yesNo4;
    }

    /**
     * @param yesNo4 the yesNo4 to set
     */
    public void setYesNo4(Boolean yesNo4)
    {
        this.yesNo4 = yesNo4;
    }

    /**
     * @return the yesNo5
     */
    @Column(name="YesNo5",unique=false,nullable=true,updatable=true,insertable=true)
    public Boolean getYesNo5()
    {
        return yesNo5;
    }

    /**
     * @param yesNo5 the yesNo5 to set
     */
    public void setYesNo5(Boolean yesNo5)
    {
        this.yesNo5 = yesNo5;
    }

    /**
     * @return the yesNo6
     */
    @Column(name="YesNo6",unique=false,nullable=true,updatable=true,insertable=true)
    public Boolean getYesNo6()
    {
        return yesNo6;
    }

    /**
     * @param yesNo6 the yesNo6 to set
     */
    public void setYesNo6(Boolean yesNo6)
    {
        this.yesNo6 = yesNo6;
    }

    /**
     *
     */
    @Column(name = "CountAmt", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getCountAmt() {
        return this.countAmt;
    }

    public void setCountAmt(Integer countAmt) {
        this.countAmt = countAmt;
    }

    /**
     *
     */
    @Lob
    @Column(name = "InternalRemarks", length = 4096)
    public String getInternalRemarks() {
        return this.internalRemarks;
    }

    public void setInternalRemarks(String internalRemarks) {
        this.internalRemarks = internalRemarks;
    }

    /**
     *
     */
    @Lob
    @Column(name = "Remarks", length = 4096)
    public String getRemarks() {
        return this.remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    /**
     *
     */
    @Column(name = "Name", unique = false, nullable = true, insertable = true, updatable = true, length = 64)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     */
    @Column(name = "Modifier", unique = false, nullable = true, insertable = true, updatable = true, length = 50)
    public String getModifier() {
        return this.modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    /**
     *
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "CatalogedDate", unique = false, nullable = true, insertable = true, updatable = true)
    public Calendar getCatalogedDate() {
        return this.catalogedDate;
    }

    public void setCatalogedDate(Calendar catalogedDate) {
        this.catalogedDate = catalogedDate;
    }

    /**
     *
     */
    @Column(name = "CatalogedDateVerbatim", length = 32, unique = false, nullable = true, insertable = true, updatable = true)
    public String getCatalogedDateVerbatim() {
        return this.catalogedDateVerbatim;
    }

    public void setCatalogedDateVerbatim(String catalogedDateVerbatim) {
        this.catalogedDateVerbatim = catalogedDateVerbatim;
    }

    /**
     *
     */
    @Column(name = "GUID", unique = false, nullable = true, insertable = true, updatable = true, length = 128)
    public String getGuid() {
        return this.guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    /**
     *
     */
    @Column(name = "AltCatalogNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getAltCatalogNumber() 
    {
        return this.altCatalogNumber;
    }

    public void setAltCatalogNumber(String altCatalogNumber) 
    {
        this.altCatalogNumber = altCatalogNumber;
    }

    /**
     *
     */
    @Column(name = "CatalogNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getCatalogNumber() {
        return this.catalogNumber;
    }

    public void setCatalogNumber(String catalogNumber) {
        this.catalogNumber = catalogNumber;
    }
    
    /**
     * @return the catalogedDatePrecision
     */
    @Column(name = "CatalogedDatePrecision", unique = false, nullable = true, insertable = true, updatable = true)
    public Byte getCatalogedDatePrecision()
    {
        return catalogedDatePrecision != null ? this.catalogedDatePrecision : (byte)UIFieldFormatterIFace.PartialDateEnum.Full.ordinal();
    }

    /**
     * @param catalogedDatePrecision the catalogedDatePrecision to set
     */
    public void setCatalogedDatePrecision(Byte catalogedDatePrecision)
    {
        this.catalogedDatePrecision = catalogedDatePrecision;
    }

    /**
     * @return the availability
     */
    @Column(name = "Availability", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getAvailability()
    {
        return availability;
    }

    /**
     * @param availability the availability to set
     */
    public void setAvailability(String availability)
    {
        this.availability = availability;
    }

    /**
     * @return the restrictions
     */
    @Column(name = "Restrictions", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getRestrictions()
    {
        return restrictions;
    }

    /**
     * @param restrictions the restrictions to set
     */
    public void setRestrictions(String restrictions)
    {
        this.restrictions = restrictions;
    }

    /**
     * @return the notifications
     */
    @Column(name = "Notifications", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getNotifications()
    {
        return notifications;
    }

    /**
     * @param notifications the notifications to set
     */
    public void setNotifications(String notifications)
    {
        this.notifications = notifications;
    }
    /**
     * @return the totalValue
     */
    @Column(name = "TotalValue", unique = false, nullable = true, insertable = true, updatable = true, precision = 12, scale = 2)
    public BigDecimal getTotalValue()
    {
        return totalValue;
    }

    /**
     * @param totalValue the totalValue to set
     */
    public void setTotalValue(BigDecimal totalValue)
    {
        this.totalValue = totalValue;
    }


    /**
     * @return the projectNumber
     */
    @Column(name = "ProjectNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 64)
    public String getProjectNumber()
    {
        return projectNumber;
    }

    /**
     * @param projectNumber the projectNumber to set
     */
    public void setProjectNumber(String projectNumber)
    {
        this.projectNumber = projectNumber;
    }

    /**
     *      * Indicates whether this record can be viewed - by owner, by institution, or by all
     */
    @Column(name = "Visibility", unique = false, nullable = true, insertable = true, updatable = true, length = 10)
    public Byte getVisibility() {
        return this.visibility;
    }
    
    public void setVisibility(Byte visibility) {
        this.visibility = visibility;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#isRestrictable()
     */
    @Transient
    @Override
    public boolean isRestrictable()
    {
        return true;
    }   
    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "VisibilitySetByID", unique = false, nullable = true, insertable = true, updatable = true)
    public SpecifyUser getVisibilitySetBy() {
        return this.visibilitySetBy;
    }
    
    public void setVisibilitySetBy(SpecifyUser visibilitySetBy) {
        this.visibilitySetBy = visibilitySetBy;
    }
    
    /**
     *      * Container
     */
    @ManyToOne(cascade = { javax.persistence.CascadeType.ALL }, fetch = FetchType.LAZY)
    @JoinColumn(name = "ContainerID", unique = false, nullable = true, insertable = true, updatable = true)
    public Container getContainer() {
        return this.container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }
    
    /**
     *      * BiologicalObject (Bird, Fish, etc)
     */
    @ManyToOne(cascade = {}, fetch = FetchType.EAGER)
    //@Cascade( { CascadeType.MERGE, CascadeType.LOCK })
    @Cascade( { CascadeType.LOCK })
    @JoinColumn(name = "CollectingEventID", unique = false, nullable = true, insertable = true, updatable = true)
    public CollectingEvent getCollectingEvent() {
        return this.collectingEvent;
    }

    public void setCollectingEvent(CollectingEvent collectingEvent) {
        this.collectingEvent = collectingEvent;
    }

    /**
     *
     */
    @ManyToOne(cascade = {javax.persistence.CascadeType.ALL}, fetch = FetchType.LAZY)
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name = "CollectionObjectAttributeID", unique = false, nullable = true, insertable = true, updatable = true)
    public CollectionObjectAttribute getCollectionObjectAttribute() 
    {
        return this.collectionObjectAttribute;
    }

    public void setCollectionObjectAttribute(CollectionObjectAttribute colObjAttribute) 
    {
        this.collectionObjectAttribute = colObjAttribute;
    }

    /**
     *
     */
    @Transient
    public Set<AttributeIFace> getAttrs() 
    {
        return new HashSet<AttributeIFace>(this.collectionObjectAttrs);
    }

    public void setAttrs(Set<AttributeIFace> collectionObjectAttrs) 
    {
        this.collectionObjectAttrs.clear();
        for (AttributeIFace a : collectionObjectAttrs)
        {
            if (a instanceof CollectionObjectAttr)
            {
                this.collectionObjectAttrs.add((CollectionObjectAttr)a);
            }
        }
    }

    /**
     * @return the collectionObjectAttrs
     */
    @OneToMany(targetEntity=CollectionObjectAttr.class,
            cascade = {}, fetch = FetchType.LAZY, mappedBy="collectionObject")
    @Cascade( { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    public Set<CollectionObjectAttr> getCollectionObjectAttrs()
    {
        return collectionObjectAttrs;
    }

    /**
     * @param collectionObjectAttrs the collectionObjectAttrs to set
     */
    public void setCollectionObjectAttrs(Set<CollectionObjectAttr> collectionObjectAttrs)
    {
        this.collectionObjectAttrs = collectionObjectAttrs;
    }

    /**
     *
     */
    @OneToMany(mappedBy = "collectionObject")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<Fragment> getFragments() {
        return this.fragments;
    }

    public void setFragments(Set<Fragment> fragments) {
        this.fragments = fragments;
    }

    /**
     *
     */
    @ManyToMany(cascade={}, fetch=FetchType.LAZY, mappedBy="collectionObjects")
    @Cascade( {CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK} )
    public Set<Project> getProjects() 
    {
        return this.projects;
    }

    public void setProjects(Set<Project> projects) 
    {
        this.projects = projects;
    }
    
    /**
     *
     */
    @ManyToOne(cascade = {javax.persistence.CascadeType.ALL}, fetch = FetchType.LAZY)
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name = "PaleoContextID", unique = false, nullable = true, insertable = true, updatable = true)
    public PaleoContext getPaleoContext()
    {
        return this.paleoContext;
    }

    public void setPaleoContext(PaleoContext paleoContext)
    {
        this.paleoContext = paleoContext;
    }


//    /**
//     *
//     */
//    public Set<DeaccessionPreparation> getDeaccessionPreparations() {
//        return this.deaccessionPreparations;
//    }
//
//    public void setDeaccessionPreparations(Set<DeaccessionPreparation> deaccessionPreparations) {
//        this.deaccessionPreparations = deaccessionPreparations;
//    }

    /**
     * @return the collectionObjects
     */
    @OneToMany(mappedBy = "collectionObject")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<DNASequence> getDnaSequences()
    {
        return dnaSequences;
    }
    
    /**
     * @param dnaSequences the dnaSequences to set
     */
    public void setDnaSequences(Set<DNASequence> dnaSequences)
    {
        this.dnaSequences = dnaSequences;
    }

    /**
     * @return the fieldNotebookPage
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "FieldNotebookPageID", unique = false, nullable = true, insertable = true, updatable = true)
    public FieldNotebookPage getFieldNotebookPage()
    {
        return fieldNotebookPage;
    }

    /**
     * @param fieldNotebookPage the fieldNotebookPage to set
     */
    public void setFieldNotebookPage(FieldNotebookPage fieldNotebookPage)
    {
        this.fieldNotebookPage = fieldNotebookPage;
    }

    /**
     *
     */
    @OneToMany(cascade = { javax.persistence.CascadeType.ALL }, fetch = FetchType.EAGER, mappedBy = "collectionObject")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<OtherIdentifier> getOtherIdentifiers() {
        return this.otherIdentifiers;
    }

    public void setOtherIdentifiers(Set<OtherIdentifier> otherIdentifiers) {
        this.otherIdentifiers = otherIdentifiers;
    }

    /**
     *
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "CollectionID", unique = false, nullable = false, insertable = true, updatable = true)
    public Collection getCollection() {
        return this.collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    /**
     *
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "CatalogerID", unique = false, nullable = true, insertable = true, updatable = true)
    public Agent getCataloger() {
        return this.cataloger;
    }

    public void setCataloger(Agent cataloger) {
        this.cataloger = cataloger;
    }

    @OneToMany(mappedBy = "collectionObject")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OrderBy("ordinal ASC")
    public Set<CollectionObjectAttachment> getCollectionObjectAttachments()
    {
        return collectionObjectAttachments;
    }

    public void setCollectionObjectAttachments(Set<CollectionObjectAttachment> collectionObjectAttachments)
    {
        this.collectionObjectAttachments = collectionObjectAttachments;
    }
    
   //---------------------------------------------------------------------------
    // Overrides DataModelObjBase
    //---------------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getIdentityTitle()
     */
    @Override
    @Transient
    public String getIdentityTitle()
    {
        if (StringUtils.isNotEmpty(catalogNumber))
        {
            UIFieldFormatterIFace fmt = AppContextMgr.getInstance().getFormatter("CollectionObject", "CatalogNumber");
            if (fmt != null)
            {
                return fmt.formatToUI(catalogNumber).toString();
            }
        }
        return fieldNumber != null ? fieldNumber : super.getIdentityTitle();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Integer getParentTableId()
    {
        return Collection.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        return collection != null ? collection.getId() : null;
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
        return 1;
    }
    
    @Transient
    public Set<CollectionObjectAttachment> getAttachmentReferences()
    {
        return collectionObjectAttachments;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#forceLoad()
     */
    @Override
    public void forceLoad()
    {
        fragments.size();
        projects.size();
        if (collection != null)
        {
            collection.getId();
        }
    }
    
    //----------------------------------------------------------------------
    //-- Comparable Interface
    //----------------------------------------------------------------------


    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(CollectionObject obj)
    {
        // XXX TODO need to fix when Cat Nums change to Strings!
        if (catalogNumber != null && obj != null && obj.catalogNumber != null)
        {
            return catalogNumber.compareTo(obj.catalogNumber);
        }
        // else
        return timestampCreated.compareTo(obj.timestampCreated);
    }

}
