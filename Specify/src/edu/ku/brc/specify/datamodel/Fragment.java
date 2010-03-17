package edu.ku.brc.specify.datamodel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Index;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "fragment")
@org.hibernate.annotations.Table(appliesTo="fragment", indexes =
    {   @Index (name="FragColMemIDX", columnNames={"CollectionMemberID"}),
        @Index (name="CatalogNumberIDX", columnNames={"CatalogNumber"}) })
public class Fragment extends CollectionMember implements AttachmentOwnerIFace<FragmentAttachment>,
                                                          Serializable,
                                                          Comparable<Fragment>,
                                                          Cloneable
{
	// Fields 
	protected Integer fragmentId;
	protected String  accessionNumber;
	protected String  catalogNumber;
	protected String  description;
	protected String  distribution;
	protected String  phenology;
	protected String  prepMethod;
	protected String  provenance;
	protected String  remarks;
	protected String  sex;
	protected String  text1;   // herbarium (collection)
	protected String  text2;   // reference
	protected String  voucher;
	
	protected Integer number1;  // replicates
	protected Integer number2;
	
    protected Boolean deaccessioned;
	protected Boolean yesNo1;
	protected Boolean yesNo2;
	
	// Relations
	
	protected CollectionObject collectionObject;
	protected Preparation      preparation;
	
    protected Set<FragmentAttachment> fragmentAttachments;
    protected Set<FragmentCitation>   fragmentCitations;
	protected Set<Determination>      determinations;
	protected Set<ExsiccataItem>      exsiccataItems;
    protected Set<CollectionRelationship>   leftSideRels;
    protected Set<CollectionRelationship>   rightSideRels;

	// Transient
	
	// Constructors

    /** default constructor */
	public Fragment()
	{
		// do nothing
	}

	/** constructor with id */
    public Fragment(Integer fragmentId) 
    {
        this.fragmentId = fragmentId;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#initialize()
     */
    @Override
    public void initialize()
    {
        super.init();
        
        fragmentId       = null;
        accessionNumber  = null;
        catalogNumber    = null;
        description      = null;
        distribution     = null;
        phenology        = null;
        prepMethod       = null;
        provenance       = null;
        remarks          = null;
        sex              = null;
        text1            = null;
        text2            = null;
        voucher          = null;
        
        number1          = null;
        number2          = null;
        
        deaccessioned    = null;
        yesNo1           = null;
        yesNo2           = null;
        
        collectionObject = null;
        preparation      = null;
        
        fragmentAttachments = new HashSet<FragmentAttachment>();
        fragmentCitations   = new HashSet<FragmentCitation>();
        determinations      = new HashSet<Determination>();
        exsiccataItems      = new HashSet<ExsiccataItem>();
        leftSideRels        = new HashSet<CollectionRelationship>();
        rightSideRels       = new HashSet<CollectionRelationship>();
    }
    // End Initializer
    
    // Property accessors
    
    @Id
    @GeneratedValue
    @Column(name = "FragmentID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getFragmentId() {
        return this.fragmentId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getId()
     */
    @Override
    @Transient
	public Integer getId()
    {
		return this.fragmentId;
	}

    public void setFragmentId(Integer fragmentId) {
        this.fragmentId = fragmentId;
    }
    
    /**
     * @return the accessionNumber
     */
    @Column(name = "AccessionNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getAccessionNumber()
    {
        return accessionNumber;
    }

    /**
     * @param accessionNumber
     */
    public void setAccessionNumber(String accessionNumber)
    {
        this.accessionNumber = accessionNumber;
    }
    
    /**
     * @return the catalogNumber
     */
    @Column(name = "CatalogNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getCatalogNumber()
    {
        return catalogNumber;
    }

    /**
     * @param catalogNumber
     */
    public void setCatalogNumber(String catalogNumber)
    {
        this.catalogNumber = catalogNumber;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
	public Class<?> getDataClass()
	{
		return Fragment.class;
	}
    
    /**
     * @return the Table ID for the class.
     */
    public static int getClassTableId()
    {
        return 142;
    }
    
    /**
    *
    */
   @Column(name = "Deaccessioned", unique = false, nullable = true, insertable = true, updatable = true)
   public Boolean getDeaccessioned() {
       return this.deaccessioned;
   }

   public void setDeaccessioned(Boolean deaccessioned) {
       this.deaccessioned = deaccessioned;
   }
    
    /**
     * The relation of this fragment to others in a preparation, or to others on other preparations
     */
    @Column(name = "Description", unique = false, nullable = true, insertable = true, updatable = true, length = 255)
    public String getDescription() 
    {
        return this.description;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }
    
    /**
     * A list of other herbaria that were given duplicates of this fragment
     */
    @Column(name = "Distribution", unique = false, nullable = true, insertable = true, updatable = true, length = 100)
    public String getDistribution() 
    {
        return this.distribution;
    }

    public void setDistribution(String distribution) 
    {
        this.description = distribution;
    }
    
    /**
     * @return the number1
     */
    @Column(name = "Number1", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getNumber1()
    {
        return number1;
    }

    /**
     * @param number1
     */
    public void setNumber1(Integer number1)
    {
        this.number1 = number1;
    }

    /**
     * @return the number2
     */
    @Column(name = "Number2", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getNumber2()
    {
        return number2;
    }

    /**
     * @param number2
     */
    public void setNumber2(Integer number2)
    {
        this.number2 = number2;
    }
    
    /**
     * Phenology
     */
    @Column(name = "Phenology", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getPhenology() 
    {
        return this.phenology;
    }

    public void setPhenology(String phenology) 
    {
        this.phenology = phenology;
    }
    
    /**
     * PrepMethod
     */
    @Column(name = "PrepMethod", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getPrepMethod() 
    {
        return this.prepMethod;
    }

    public void setPrepMethod(String prepMethod) 
    {
        this.prepMethod = prepMethod;
    }
    
    /**
     * Previous owners of this fragment
     */
    @Column(name = "Provenance", unique = false, nullable = true, insertable = true, updatable = true, length = 255)
    public String getProvenance() 
    {
        return this.provenance;
    }
    
    public void setProvenance(String provenance) 
    {
        this.provenance = provenance;
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
     * Sex
     */
    @Column(name = "Sex", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getSex() 
    {
        return this.sex;
    }

    public void setSex(String sex) 
    {
        this.sex = sex;
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
     * Voucher
     */
    @Column(name = "Voucher", unique = false, nullable = true, insertable = true, updatable = true, length = 255)
    public String getVoucher() 
    {
        return this.voucher;
    }

    public void setVoucher(String voucher) 
    {
        this.voucher = voucher;
    }

    /**
     * @return the yesNo1
     */
    @Column(name = "YesNo1", unique = false, nullable = true, insertable = true, updatable = true)
    public Boolean getYesNo1()
    {
        return yesNo1;
    }

    /**
     * @param yesNo1
     */
    public void setYesNo1(Boolean yesNo1)
    {
        this.yesNo1 = yesNo1;
    }

    /**
     * @return the yesNo2
     */
    @Column(name = "YesNo2", unique = false, nullable = true, insertable = true, updatable = true)
    public Boolean getYesNo2()
    {
        return yesNo2;
    }

    /**
     * @param yesNo2
     */
    public void setYesNo2(Boolean yesNo2)
    {
        this.yesNo2 = yesNo2;
    }
    
    /**
     *       * CollectionObject
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "CollectionObjectID", unique = false, nullable = false, insertable = true, updatable = true)
    public CollectionObject getCollectionObject() {
        return this.collectionObject;
    }
    
    public void setCollectionObject(CollectionObject collectionObject) {
        this.collectionObject = collectionObject;
    }
    
    @OneToMany(mappedBy = "fragment")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OrderBy("ordinal ASC")
    public Set<FragmentAttachment> getFragmentAttachments()
    {
        return fragmentAttachments;
    }

    public void setFragmentAttachments(Set<FragmentAttachment> fragmentAttachments)
    {
        this.fragmentAttachments = fragmentAttachments;
    }
    
    @Transient
    public Set<FragmentAttachment> getAttachmentReferences()
    {
        return fragmentAttachments;
    }
    
    /**
    *
    */
   @OneToMany(cascade = { javax.persistence.CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "fragment")
   @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
   public Set<FragmentCitation> getFragmentCitations() {
       return this.fragmentCitations;
   }

   public void setFragmentCitations(Set<FragmentCitation> fragmentCitations) {
       this.fragmentCitations = fragmentCitations;
   }
    
    /**
     *      * Preparation
     */
    @ManyToOne(cascade = { javax.persistence.CascadeType.ALL }, fetch = FetchType.LAZY)
    @JoinColumn(name = "PreparationID", unique = false, nullable = true, insertable = true, updatable = true)
    public Preparation getPreparation() {
        return this.preparation;
    }

    public void setPreparation(Preparation preparation) {
        this.preparation = preparation;
    }
    
    /**
    *
    */
   @OneToMany(mappedBy = "fragment")
   @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
   public Set<Determination> getDeterminations() 
   {
       return this.determinations;
   }

   public void setDeterminations(Set<Determination> determinations) 
   {
       this.determinations = determinations;
   }
   
    @OneToMany(mappedBy = "fragment")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<ExsiccataItem> getExsiccataItems()
    {
        return exsiccataItems;
    }
    
    public void setExsiccataItems(Set<ExsiccataItem> exsiccataItems)
    {
        this.exsiccataItems = exsiccataItems;
    }
    
    /**
     * 
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "leftSide")
    @Cascade( { CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK })
    public Set<CollectionRelationship> getLeftSideRels() 
    {
        return this.leftSideRels;
    }
    
    public void setLeftSideRels(Set<CollectionRelationship> leftSideRels) 
    {
        this.leftSideRels = leftSideRels;
    }

    /**
     * 
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "rightSide")
    @Cascade( { CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK })
    public Set<CollectionRelationship> getRightSideRels() 
    {
        return this.rightSideRels;
    }
    
    public void setRightSideRels(Set<CollectionRelationship> rightSideRels) 
    {
        this.rightSideRels = rightSideRels;
    }
    
	@Override
	public int compareTo(Fragment o)
	{
	    // XXX TODO need to fix when Cat Nums change to Strings!
        if (catalogNumber != null && o != null && o.catalogNumber != null)
        {
            return catalogNumber.compareTo(o.catalogNumber);
        }
        // else
        return timestampCreated.compareTo(o.timestampCreated);
	}

}
