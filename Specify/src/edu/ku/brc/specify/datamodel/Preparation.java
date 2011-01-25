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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
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

import edu.ku.brc.af.core.UsageTracker;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.AttributeIFace;
import edu.ku.brc.dbsupport.AttributeProviderIFace;
import edu.ku.brc.dbsupport.DBConnection;

/**

 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "preparation", uniqueConstraints = {
	@UniqueConstraint(columnNames={"Identifier"} )
} )
@org.hibernate.annotations.Table(appliesTo="preparation", indexes =
    {   @Index (name="PreparedDateIDX", columnNames={"preparedDate"}),
        @Index (name="PrepColMemIDX", columnNames={"CollectionMemberID"}),
        @Index (name="IdentifierIDX", columnNames={"Identifier"}) })
public class Preparation extends CollectionMember implements AttachmentOwnerIFace<PreparationAttachment>, 
                                                             AttributeProviderIFace, 
                                                             java.io.Serializable, 
                                                             Comparable<Preparation>,
                                                             Cloneable
{

    // Fields    

    protected Integer                     preparationId;
    protected String                      identifier;
    protected String                      text1;
    protected String                      text2;
    protected Integer                     countAmt;
    protected String                      storageLocation;
    protected String                      remarks;
    protected Calendar                    preparedDate;
    protected Byte                        preparedDatePrecision;   // Accurate to Year, Month, Day
    protected String                      status;
    protected String                      sampleNumber;
    protected String                      description;             // from Specify 5
    
    protected Float                       number1;
    protected Float                       number2;
    protected Boolean                     yesNo1;
    protected Boolean                     yesNo2;
    protected Boolean                     yesNo3;
    
    // from CollectionObject
    protected Appraisal                   appraisal;
    protected Set<ConservDescription>     conservDescriptions;
	protected Calendar                    inventoryDate;
	protected String                      objectCondition;
    protected Set<TreatmentEvent>         treatmentEvents;
    
    // Fragment
    protected Set<Fragment>               fragments;
    
    protected Set<AccessionPreparation>   accessionPreparations;
    protected Set<ExchangeOutPreparation> exchangeOutPreparations;
    protected Set<GiftPreparation>        giftPreparations;
    protected Set<LoanPreparation>        loanPreparations;
    protected PrepType                    prepType;
    protected Agent                       preparedByAgent;
    protected Storage                     storage;
    protected Set<DeaccessionPreparation> deaccessionPreparations;

    protected PreparationAttribute        preparationAttribute;    // Specify 5 Attributes table
    protected Set<PreparationAttr>        preparationAttrs;        // Generic Expandable Attributes
    protected Set<PreparationAttachment>  preparationAttachments;
    
    // Unbarcoded Lots
    protected Geography   geography;
    protected Taxon       taxon;
    
    // Tree
    protected Preparation                 parent;
    protected Set<Preparation>            children;
    
    // Transient
    protected Boolean                     isOnLoan = null;
    
    // Constructors

    /** default constructor */
    public Preparation() 
    {
        // do nothing
    }
    
    /** constructor with id */
    public Preparation(Integer preparationId) 
    {
        this.preparationId = preparationId;
    }
   
    
    

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#initialize()
     */
    @Override
    public void initialize()
    {
        super.init();
        
        preparationId = null;
        identifier = null;
        text1         = null;
        text2         = null;
        countAmt      = null;
        storageLocation = null;
        remarks       = null;
        preparedDate  = null;
        preparedDatePrecision = null;
        status        = null;
        sampleNumber  = null;
        description   = null;
        
        number1       = null;
        number2       = null;
        yesNo1        = null;
        yesNo2        = null;
        yesNo3        = null;
        
        // fragments
        fragments = new HashSet<Fragment>();
        
        // from collection object
        appraisal           = null;
        conservDescriptions = new HashSet<ConservDescription>();
        inventoryDate       = null;
        objectCondition     = null;
        treatmentEvents     = new HashSet<TreatmentEvent>();
        
        fragments = new HashSet<Fragment>();
        
        geography = null;
        taxon = null;
        
        accessionPreparations = new HashSet<AccessionPreparation>();
        exchangeOutPreparations = new HashSet<ExchangeOutPreparation>();
        giftPreparations = new HashSet<GiftPreparation>();
        loanPreparations = new HashSet<LoanPreparation>();
        prepType = null;
        preparedByAgent = null;
        storage = null;
        deaccessionPreparations = new HashSet<DeaccessionPreparation>();
        
        preparationAttribute   = null;
        preparationAttrs       = new HashSet<PreparationAttr>();
        preparationAttachments = new HashSet<PreparationAttachment>();
        
        // tree
        parent   = null;
        children = new HashSet<Preparation>();
    }
    // End Initializer

    // Property accessors

    /**
     * 
     */
    @Id
    @GeneratedValue
    @Column(name = "PreparationID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getPreparationId() {
        return this.preparationId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getId()
     */
    @Override
    @Transient
    public Integer getId()
    {
        return this.preparationId;
    }
    

    /** dl: The if statement inserted in setIdentifier addresses issues regarding multiple
     *  preparations per loan that are lots which have an identifier that is an empty string
     *  and which throw a ConstraintViolation due to "duplicate keys". This workaround 
     *  replaces that empty string with a null value. Note: There is a unique constraint in both
     *  the hibernate mapping and in the database table on the identifier field. Additionally, 
     *  all lots have an identifier value assigned as an empty string.
     */
    public void setIdentifier(String identifier) {

 	   if (identifier != null && identifier.equals(""))
 		   this.identifier = null;
 	   else
 		   this.identifier = identifier;
    }
    
    /** dl: The if statement has been inserted in getIdentifier due to changes made in setIdentifier 
     * regarding multiple lot preparations per loan and duplicate key errors due to lots being 
     * assigned an identifier which is an empty string. The database value of null is different 
     * than the original empty string value of identifier as a side consequence of converting the 
     * strings in setIdentifier. The if statement in the get method will convert the null value 
     * retrieved from the database back into an empty string. What is unclear is whether or not a 
     * null identifier assigned to a preparation holds any meaning in specify code. A concern is 
     * that an identifier which is originally assigned a null value could mistakenly be converted 
     * to an empty string by the alterations to this getter method. 
     */
   @Column(name = "Identifier", unique = true, nullable = true, insertable = true, updatable = true, length = 32)
   public String getIdentifier() {
	   /*
	   if (this.identifier == null)
		   return "";
	   else */
		   return this.identifier;
   }

    /**
     * 
     */
    @OneToMany(cascade = { javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST }, mappedBy = "preparation", fetch = FetchType.EAGER)
    @org.hibernate.annotations.Cascade( { CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK })
    public Set<Fragment> getFragments() {
        return this.fragments;
    }
    
    public void setFragments(Set<Fragment> fragments) {
        this.fragments = fragments;
    }

    // from CollectionObject
    
    /**
     * @return the appraisal
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    //@Cascade( { CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK })
    @JoinColumn(name = "AppraisalID", unique = false, nullable = true, insertable = true, updatable = true)
    public Appraisal getAppraisal()
    {
        return appraisal;
    }

    /**
     * @param appraisal the appraisal to set
     */
    public void setAppraisal(Appraisal appraisal)
    {
        this.appraisal = appraisal;
    }
    
    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentID")
    public Preparation getParent()
    {
        return this.parent;
    }

    public void setParent(Preparation parent)
    {
        this.parent = parent;
    }

    /**
     *
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<ConservDescription> getConservDescriptions()
    {
        return this.conservDescriptions;
    }

    public void setConservDescriptions(final Set<ConservDescription> conservDescriptions)
    {
        this.conservDescriptions = conservDescriptions;
    }
    
    /**
     * @return the inventoryDate
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "InventoryDate", unique = false, nullable = true, insertable = true, updatable = true)
    public Calendar getInventoryDate()
    {
        return inventoryDate;
    }

    /**
     * @param inventoryDate the inventoryDate to set
     */
    public void setInventoryDate(Calendar inventoryDate)
    {
        this.inventoryDate = inventoryDate;
    }

    /**
     * @return the condition
     */
    @Column(name = "ObjectCondition", unique = false, nullable = true, insertable = true, updatable = true, length=64)
    public String getObjectCondition()
    {
        return objectCondition;
    }

    /**
     * @param condition the condition to set
     */
    public void setObjectCondition(String objectCondition)
    {
        this.objectCondition = objectCondition;
    }
    
    /**
     * @return the treatmentEvents
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<TreatmentEvent> getTreatmentEvents()
    {
        return treatmentEvents;
    }

    /**
     * @param treatmentEvents the treatmentEvents to set
     */
    public void setTreatmentEvents(Set<TreatmentEvent> treatmentEvents)
    {
        this.treatmentEvents = treatmentEvents;
    }
    
    // end methods from CollectionObject
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return Preparation.class;
    }
    
    public void setPreparationId(Integer preparationId) {
        this.preparationId = preparationId;
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
     *      * The number of objects (specimens, slides, pieces) prepared
     */
    @Column(name = "CountAmt", unique = false, nullable = true, insertable = true, updatable = true)
    public Integer getCountAmt() 
    {
        return this.countAmt;
    }
    
    public void setCountAmt(Integer countAmt) 
    {
        this.countAmt = countAmt;
    }
    
    @Transient
    public int getLoanAvailable()
    {
        int cnt = this.countAmt != null ? this.countAmt : 0;
        return cnt - getLoanQuantityOut();
    }

    /**
     * calculates the number of preparations already loaned out.
     * @return the (calculated) number of preps out on loan.
     */
    @Transient
    public int getLoanQuantityOut()
    {
        int stillOut = 0;
        for (LoanPreparation lpo : getLoanPreparations())
        {
            int quantityLoaned   = lpo.getItemCount() != null ? lpo.getItemCount() : 0;
            int quantityResolved = lpo.getQuantityResolved() != null ? lpo.getQuantityResolved() : 0;
            
            stillOut += (quantityLoaned - quantityResolved);
        }
        return stillOut;
    }
    
    /**
     * @return
     */
    @Transient
    public Boolean getIsOnLoan()
    {
        if (isOnLoan == null)
        {
            Connection conn = null;
            Statement  stmt = null;
            try
            {
                conn = DBConnection.getInstance().createConnection();
                if (conn != null)
                {
                    stmt = conn.createStatement();
                    String sql = "SELECT p.CountAmt, lp.ItemCount, lp.QuantityResolved, lp.QuantityReturned, lp.IsResolved FROM preparation p " +
                                 "INNER JOIN loanpreparation lp ON p.PreparationID = lp.PreparationID WHERE p.PreparationID = "+getId();
                    ResultSet rs = stmt.executeQuery(sql);
                    
                    int     totalAvail = 0;
                    Integer prepQty    = null;
                    
                    while (rs.next())
                    {
                        prepQty = rs.getObject(1) != null ? rs.getInt(1) : 0;
                        //System.err.print("\nprepQty "+prepQty);
                        
                        boolean isResolved = rs.getObject(5) != null ? rs.getBoolean(5) : false;
                        
                        int loanQty = rs.getObject(2) != null ? rs.getInt(2) : 0;
                        int qtyRes  = rs.getObject(3) != null ? rs.getInt(3) : 0;
                        //int qtyRtn  = rs.getObject(4) != null ? rs.getInt(4) : 0;
                        
                        if (isResolved && qtyRes != loanQty) // this shouldn't happen
                        {
                            qtyRes = loanQty;
                        }
                        
                        totalAvail += qtyRes - loanQty;
                    }
                    rs.close();
                    
                    if (prepQty == null)
                    {
                        return false;
                    }
                        
                    isOnLoan = totalAvail < prepQty;
                } else
                {
                    UsageTracker.incrNetworkUsageCount();
                }
                
            } catch (SQLException ex)
            {
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(Preparation.class, ex);
                UsageTracker.incrSQLUsageCount();
                ex.printStackTrace();
                
            } finally
            {
                if (stmt != null)
                {
                    try
                    {
                        stmt.close();
                    } catch (SQLException ex) {}
                }
                if (conn != null)
                {
                    try
                    {
                        conn.close();
                    } catch (SQLException ex) {}
                }
            }
        }
        return isOnLoan == null ? false : true;
    }
    
    /**
     * @param isOnLoan
     */
    public void setIsOnLoan(final Boolean isOnLoan)
    {
        this.isOnLoan = isOnLoan;
    }

    // HUH mmk: increased length of StorageLocation to 100
    /**
     * 
     */
    @Column(name = "StorageLocation", unique = false, nullable = true, insertable = true, updatable = true, length = 100)
    public String getStorageLocation() {
        return this.storageLocation;
    }
    
    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
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
    @Temporal(TemporalType.DATE)
    @Column(name = "PreparedDate", unique = false, nullable = true, insertable = true, updatable = true)
    public Calendar getPreparedDate() {
        return this.preparedDate;
    }
    
    public void setPreparedDate(Calendar preparedDate) {
        this.preparedDate = preparedDate;
    }

    /**
     * @return the preparedDatePrecision
     */
    @Column(name = "PreparedDatePrecision", unique = false, nullable = true, insertable = true, updatable = true)
    public Byte getPreparedDatePrecision()
    {
        return this.preparedDatePrecision != null ? this.preparedDatePrecision : (byte)UIFieldFormatterIFace.PartialDateEnum.Full.ordinal();
    }

    /**
     * @param preparedDatePrecision the preparedDatePrecision to set
     */
    public void setPreparedDatePrecision(Byte preparedDatePrecision)
    {
        this.preparedDatePrecision = preparedDatePrecision;
    }

    /**
     * @return the status
     */
    @Column(name = "Status", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getStatus()
    {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * @return the sampleNumber
     */
    @Column(name = "SampleNumber", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getSampleNumber()
    {
        return sampleNumber;
    }

    /**
     * @param sampleNumber the sampleNumber to set
     */
    public void setSampleNumber(String sampleNumber)
    {
        this.sampleNumber = sampleNumber;
    }

    /**
     * Image, Sound, Preparation, Container(Container Label?)
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
     * @return the number1
     */
    @Column(name = "Number1", unique = false, nullable = true, insertable = true, updatable = true)
    public Float getNumber1()
    {
        return number1;
    }

    /**
     * @param number1 the number1 to set
     */
    public void setNumber1(Float number1)
    {
        this.number1 = number1;
    }

    /**
     * @return the number2
     */
    @Column(name = "Number2", unique = false, nullable = true, insertable = true, updatable = true)
    public Float getNumber2()
    {
        return number2;
    }

    /**
     * @param number2 the number2 to set
     */
    public void setNumber2(Float number2)
    {
        this.number2 = number2;
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
     * @param yesNo1 the yesNo1 to set
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
     * @param yesNo2 the yesNo2 to set
     */
    public void setYesNo2(Boolean yesNo2)
    {
        this.yesNo2 = yesNo2;
    }

    /**
     * @return the yesNo3
     */
    @Column(name = "YesNo3", unique = false, nullable = true, insertable = true, updatable = true)
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
     * 
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<AccessionPreparation> getAccessionPreparations() {
        return this.accessionPreparations;
    }
    
    public void setAccessionPreparations(Set<AccessionPreparation> accessionPreparations) {
        this.accessionPreparations = accessionPreparations;
    }
    
    /**
     * 
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<LoanPreparation> getLoanPreparations() {
        return this.loanPreparations;
    }
    
    public void setLoanPreparations(Set<LoanPreparation> loanPreparations) {
        this.loanPreparations = loanPreparations;
    }

    /**
     * 
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<ExchangeOutPreparation> getExchangeOutPreparations() {
        return this.exchangeOutPreparations;
    }
    
    public void setExchangeOutPreparations(Set<ExchangeOutPreparation> exchangeOutPreparations) {
        this.exchangeOutPreparations = exchangeOutPreparations;
    }
    
    /**
     * 
     */
    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<GiftPreparation> getGiftPreparations() {
        return this.giftPreparations;
    }
    
    public void setGiftPreparations(Set<GiftPreparation> giftPreparations) {
        this.giftPreparations = giftPreparations;
    }

   /**
     * @return the preparationAttrs
     */
    @OneToMany(targetEntity=PreparationAttr.class, cascade = {}, fetch = FetchType.LAZY, mappedBy="preparation")
    @Cascade( { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    public Set<PreparationAttr> getPreparationAttrs()
    {
        return preparationAttrs;
    }

    /**
     * @param preparationAttrs the preparationAttrs to set
     */
    public void setPreparationAttrs(Set<PreparationAttr> preparationAttrs)
    {
        this.preparationAttrs = preparationAttrs;
    }

   /**
    *
    */
   @Transient
   public Set<AttributeIFace> getAttrs() 
   {
       return new HashSet<AttributeIFace>(this.preparationAttrs);
   }

   public void setAttrs(Set<AttributeIFace> preparationAttrs) 
   {
       this.preparationAttrs.clear();
       for (AttributeIFace a : preparationAttrs)
       {
           if (a instanceof PreparationAttr)
           {
               this.preparationAttrs.add((PreparationAttr)a);
           }
       }
   }
    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "PrepTypeID", unique = false, nullable = false, insertable = true, updatable = true)
    public PrepType getPrepType() {
        return this.prepType;
    }
    
    public void setPrepType(PrepType prepType) {
        this.prepType = prepType;
    }

    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "PreparedByID", unique = false, nullable = true, insertable = true, updatable = true)
    public Agent getPreparedByAgent() {
        return this.preparedByAgent;
    }
    
    public void setPreparedByAgent(Agent preparedByAgent) {
        this.preparedByAgent = preparedByAgent;
    }

    //@OneToMany(cascade = {javax.persistence.CascadeType.ALL}, mappedBy = "preparation")
    @OneToMany(mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OrderBy("ordinal ASC")
    public Set<PreparationAttachment> getPreparationAttachments()
    {
        return preparationAttachments;
    }

    public void setPreparationAttachments(Set<PreparationAttachment> preparationAttachments)
    {
        this.preparationAttachments = preparationAttachments;
    }

    /**
     * 
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "StorageID", unique = false, nullable = true, insertable = true, updatable = true)
    public Storage getStorage() {
        return this.storage;
    }
    
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
    *
    */
   @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "preparation")
    @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
   public Set<DeaccessionPreparation> getDeaccessionPreparations() {
       return this.deaccessionPreparations;
   }

   public void setDeaccessionPreparations(Set<DeaccessionPreparation> deaccessionPreparations) {
       this.deaccessionPreparations = deaccessionPreparations;
   }
   
   /**
   *
   */
   @ManyToOne(cascade = {javax.persistence.CascadeType.ALL}, fetch = FetchType.LAZY)
   @org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
   @JoinColumn(name = "PreparationAttributeID", unique = false, nullable = true, insertable = true, updatable = true)
   public PreparationAttribute getPreparationAttribute() {
       return this.preparationAttribute;
   }

   public void setPreparationAttribute(PreparationAttribute preparationAttribute) {
       this.preparationAttribute = preparationAttribute;
   }
   
   /* (non-Javadoc)
    * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
    */
   @Override
   @Transient
   public Integer getParentTableId()
   {
       return Preparation.getClassTableId();
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
   public Set<Preparation> getChildren()
   {
       return this.children;
   }

   public void setChildren(Set<Preparation> children)
   {
       this.children = children;
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
        return 63;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.AttachmentOwnerIFace#getAttachmentReferences()
     */
    @Transient
    public Set<PreparationAttachment> getAttachmentReferences()
    {
        return preparationAttachments;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        Preparation obj = (Preparation)super.clone();
        obj.init();
        
        obj.preparationId           = null;
        obj.loanPreparations        = new HashSet<LoanPreparation>();
        obj.accessionPreparations   = new HashSet<AccessionPreparation>();
        obj.deaccessionPreparations = new HashSet<DeaccessionPreparation>();
        obj.giftPreparations        = new HashSet<GiftPreparation>();
        obj.preparationAttachments  = new HashSet<PreparationAttachment>();
        obj.conservDescriptions     = new HashSet<ConservDescription>();
        obj.treatmentEvents         = new HashSet<TreatmentEvent>();
        obj.fragments               = new HashSet<Fragment>();
        obj.children                = new HashSet<Preparation>();
        
        // Clone Attributes
        obj.parent = this.parent;
        obj.preparationAttribute    = preparationAttribute != null ? (PreparationAttribute)preparationAttribute.clone() : null;
        obj.preparationAttrs        = new HashSet<PreparationAttr>();
        for (PreparationAttr pa : preparationAttrs)
        {
            obj.preparationAttrs.add((PreparationAttr)pa.clone());
        }
         
        return obj;
    }

    //----------------------------------------------------------------------
    //-- Comparable Interface
    //----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Preparation obj)
    {
        if (prepType != null && obj != null && StringUtils.isNotEmpty(prepType.name) && StringUtils.isNotEmpty(obj.prepType.name))
        {
            return prepType.name.toLowerCase().compareTo(obj.prepType.name.toLowerCase());
        }
        // else
        return timestampCreated.compareTo(obj.timestampCreated);
    }


}
