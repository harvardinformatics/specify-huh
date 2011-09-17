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
package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;

import edu.harvard.huh.asa.AsaBorrow;
import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa.Transaction.USER_TYPE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowMaterialLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowAgent;
import edu.ku.brc.specify.datamodel.BorrowMaterial;
import edu.ku.brc.specify.datamodel.Taxon;

public class BorrowLoader extends TaxonBatchTransactionLoader
{    
    private final static String BORROW_MAT_FMT = "00000";
    
    private BorrowLookup         borrowLookup;
    private BorrowMaterialLookup borrowMaterialLookup;
    
    public BorrowLoader(File csvFile,
                        Statement sqlStatement,
                        BotanistLookup botanistLookup,
                        AffiliateLookup affiliateLookup,
                        AgentLookup agentLookup,
                        OrganizationLookup organizationLookup,
                        TaxonLookup taxonLookup) throws LocalException
   {
        super(csvFile,
              sqlStatement,
              botanistLookup,
              affiliateLookup,
              agentLookup,
              organizationLookup,
              taxonLookup);
   }

    // Loads records from asa tables herb_transaction (type='borrow') and taxon_batch.
    // The complementary direction of these transactions are out_return batch records.
    // There are no associated shipment records.
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaBorrow asaBorrow = parse(columns);

        Integer transactionId = asaBorrow.getId();
        setCurrentRecordId(transactionId);
        
        String code = asaBorrow.getLocalUnit();
        checkNull(code, "local unit");
        
        Integer collectionMemberId = getCollectionId(code);
        
        Borrow borrow = getBorrow(asaBorrow, collectionMemberId);
        
        String sql = getInsertSql(borrow);
        Integer borrowId = insert(sql);
        borrow.setBorrowId(borrowId);
        
        BorrowAgent borrower = getBorrowAgent(lookupAffiliate(asaBorrow),
                borrow, ROLE.Borrower, collectionMemberId); // "for use by"
        if (borrower != null)
        {
            sql = getInsertSql(borrower);
            insert(sql);
        }
        
        int organizationId = asaBorrow.getOrganizationId();
        boolean isSelfOrganized = Organization.IsSelfOrganizing(organizationId);

        BorrowAgent lender = getBorrowAgent(isSelfOrganized ? lookupAgent(asaBorrow) : lookupOrganization(asaBorrow),
                borrow, ROLE.Lender, collectionMemberId); // "organization"
        if (lender != null)
        {
            sql = getInsertSql(lender);
            insert(sql);
        }
        
        BorrowMaterial borrowMaterial = getBorrowMaterial(asaBorrow, borrow, collectionMemberId);
        sql = getInsertSql(borrowMaterial);
        insert(sql);
    }

    public BorrowLookup getBorrowLookup()
    {
        if (borrowLookup == null)
        {
            borrowLookup = new BorrowLookup() {

                @Override
                public Borrow getById(Integer transactionId) throws LocalException
                {
                    Borrow borrow = new Borrow();
                    
                    Integer borrowId = getInt("borrow", "BorrowID", "Number1", transactionId);
                    
                    borrow.setBorrowId(borrowId);
                    
                    return borrow;
                }
            };
        }
        return borrowLookup;
    }
    
    public BorrowMaterialLookup getBorrowMaterialLookup()
    {
        if (borrowMaterialLookup == null)
        {
            borrowMaterialLookup = new BorrowMaterialLookup()
            {
                @Override
                public BorrowMaterial getByBorrowId(Integer borrowId) throws LocalException
                {
                    BorrowMaterial borrowMaterial = new BorrowMaterial();
                    
                    Integer borrowMaterialId = getInt("borrowmaterial", "BorrowMaterialID", "BorrowID", borrowId);
                    
                    borrowMaterial.setBorrowMaterialId(borrowMaterialId);
                    
                    return borrowMaterial;
                }
            };
        }
        return borrowMaterialLookup;
    }

    private AsaBorrow parse(String[] columns) throws LocalException
    {        
        AsaBorrow asaBorrow = new AsaBorrow();
        
        super.parse(columns, asaBorrow);
        
        return asaBorrow;
    }
    
    private String getBorrowMaterialNumber(Integer id)
    {
        return (new DecimalFormat( BORROW_MAT_FMT ) ).format( id );
    }
    
    private Borrow getBorrow(AsaBorrow asaBorrow, Integer collectionMemberId) throws LocalException
    {
        Borrow borrow = new Borrow();
        
        // TODO: AddressOfRecord
        
        // CollectionMemberID
        borrow.setCollectionMemberId(collectionMemberId);
        
        // CurrentDueDate
        Date currentDueDate = asaBorrow.getCurrentDueDate();
        if (currentDueDate != null)
        {
            borrow.setCurrentDueDate(DateUtils.toCalendar(currentDueDate));
        }
        
        // DateClosed
        Date closeDate = asaBorrow.getCloseDate();
        if (closeDate != null)
        {
            borrow.setDateClosed(DateUtils.toCalendar(closeDate));
        }

        // InvoiceNumber
        String transactionNo = asaBorrow.getTransactionNo();
        if (transactionNo != null) transactionNo = truncate(transactionNo, 50, "invoice number");
        borrow.setInvoiceNumber(transactionNo);
        
        // IsClosed
        borrow.setIsClosed(closeDate != null);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = asaBorrow.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        borrow.setNumber1((float) transactionId);
        
        // OriginalDueDate
        Date originalDueDate = asaBorrow.getOriginalDueDate();
        if (originalDueDate != null)
        {
            borrow.setOriginalDueDate(DateUtils.toCalendar(originalDueDate));
        }
        
        // ReceivedDate
        Date openDate = asaBorrow.getOpenDate();
        if (openDate != null)
        {
            borrow.setReceivedDate(DateUtils.toCalendar(openDate));
        }

        // Remarks
        // ... remarks
        String remarks = asaBorrow.getRemarks();
        
        // ... user type: staff/student/visitor/unknown
        String userType = null;
        if (asaBorrow.getUserType().equals(USER_TYPE.Staff) || asaBorrow.getUserType().equals(USER_TYPE.Student))
        {
            userType = denormalize("user type", Transaction.toString(asaBorrow.getUserType()));
        }   
        
        // ... visitor?
        String visitor = denormalize("visitor?", asaBorrow.getUserType().equals(Transaction.USER_TYPE.Visitor) ? "yes" : "no");
        
        borrow.setRemarks(concatenate(remarks, userType, visitor));
                
        // Text1 (purpose)
        String description = Transaction.toString(asaBorrow.getPurpose());
        borrow.setText1(description);
        
        // Text2 (local unit)
        String localUnit = asaBorrow.getLocalUnit();
        borrow.setText2(localUnit);     

        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = asaBorrow.isAcknowledged();
        borrow.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(asaBorrow.getRequestType());
        borrow.setYesNo2(isTheirs);
        
        setAuditFields(asaBorrow, borrow);
        
        return borrow;
    }
    
    private BorrowAgent getBorrowAgent(Agent agent, Borrow borrow, ROLE role, Integer collectionMemberId) throws LocalException
    {
        BorrowAgent borrowAgent = new BorrowAgent();
        
        // Agent
        if (agent ==  null || agent.getId() == null) return null;
        
        borrowAgent.setAgent(agent);
        
        // CollectionMemberID
        borrowAgent.setCollectionMemberId(collectionMemberId);
        
        // LoanID
        borrowAgent.setBorrow(borrow);

        // Role
        borrowAgent.setRole(Transaction.toString(role));
        
        return borrowAgent;
    }

    private BorrowMaterial getBorrowMaterial(AsaBorrow asaBorrow, Borrow borrow, Integer collectionMemberId) throws LocalException
    {
        BorrowMaterial borrowMaterial = new BorrowMaterial();
        
        // Borrow
        borrowMaterial.setBorrow(borrow);
        
        // CollectionMemberID (collectionCode)
        borrowMaterial.setCollectionMemberId(collectionMemberId);
        
        // Description
        String descWithBoxCount = getDescriptionWithBoxCount(asaBorrow);
        descWithBoxCount = truncate(descWithBoxCount, 255, "description");
        borrowMaterial.setDescription(descWithBoxCount);
                
        // InComments
        // ... taxon
        String taxon = denormalize("taxon", asaBorrow.getTaxon());
        
        // ... higher taxon
        Integer taxonId = asaBorrow.getHigherTaxonId();
        Taxon higherTaxon = null;
        if (taxonId != null) higherTaxon = lookupTaxon(taxonId);
        String higherTaxonName = denormalize("higher taxon", this.getString("taxon", "Name", "TaxonID", higherTaxon.getId()));
        
        // ... item count
        String itemCount = denormalize("items", String.valueOf(asaBorrow.getItemCount()));
        
        // ... type count
        String typeCount = denormalize("items", String.valueOf(asaBorrow.getTypeCount()));
        
        // ... non-specimen count
        String nonSpecimenCount = denormalize("items", String.valueOf(asaBorrow.getNonSpecimenCount()));
        
        borrowMaterial.setInComments(concatenate(higherTaxonName, taxon, itemCount, typeCount, nonSpecimenCount));

        // MaterialNumber (transaction no)
        Integer borrowId = asaBorrow.getId();
        borrowMaterial.setMaterialNumber(getBorrowMaterialNumber(borrowId));
        
        return borrowMaterial;
    }
    
    private String getInsertSql(Borrow borrow)
    {
        String fieldNames = "CollectionMemberID, CreatedByAgentID, CurrentDueDate, DateClosed, " +
                            "InvoiceNumber, IsClosed, ModifiedByAgentID, Number1, OriginalDueDate, " +
                            "ReceivedDate, Remarks, Text1, Text2, TimestampCreated, " +
                            "TimestampModified, Version, YesNo1, YesNo2";
        
        String[] values = {
        		SqlUtils.sqlString( borrow.getCollectionMemberId()),
        		SqlUtils.sqlString( borrow.getCreatedByAgent().getId()),
        		SqlUtils.sqlString( borrow.getCurrentDueDate()),
        		SqlUtils.sqlString( borrow.getDateClosed()),
        		SqlUtils.sqlString( borrow.getInvoiceNumber()),
        		SqlUtils.sqlString( borrow.getIsClosed()),
        		SqlUtils.sqlString( borrow.getModifiedByAgent().getId()),
        		SqlUtils.sqlString( borrow.getNumber1()),
        		SqlUtils.sqlString( borrow.getOriginalDueDate()),
        		SqlUtils.sqlString( borrow.getReceivedDate()),
        		SqlUtils.sqlString( borrow.getRemarks()),
        		SqlUtils.sqlString( borrow.getText1()),
        		SqlUtils.sqlString( borrow.getText2()),
        		SqlUtils.sqlString( borrow.getTimestampCreated()),
        		SqlUtils.sqlString( borrow.getTimestampModified()),
        		SqlUtils.one(),
        		SqlUtils.sqlString( borrow.getYesNo1()),
        		SqlUtils.sqlString( borrow.getYesNo2())
        };
        
        return SqlUtils.getInsertSql("borrow", fieldNames, values);
    }
    
    private String getInsertSql(BorrowAgent borrowAgent)
    {
        String fieldNames = "AgentID, BorrowID, CollectionMemberID, Role, " +
        		            "TimestampCreated, Version";

        String[] values = {
        		SqlUtils.sqlString( borrowAgent.getAgent().getId()),
        		SqlUtils.sqlString( borrowAgent.getBorrow().getId()),
        		SqlUtils.sqlString( borrowAgent.getCollectionMemberId()),
        		SqlUtils.sqlString( borrowAgent.getRole()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("borrowagent", fieldNames, values);
    }
    
    private String getInsertSql(BorrowMaterial borrowMaterial)
    {
        String fields = "BorrowID, CollectionMemberID, Description, InComments, MaterialNumber, " +
        		        "TimestampCreated, Version";
            
        String[] values = {        
        		SqlUtils.sqlString( borrowMaterial.getBorrow().getId()),
        		SqlUtils.sqlString( borrowMaterial.getCollectionMemberId()),
        		SqlUtils.sqlString( borrowMaterial.getDescription()),
        		SqlUtils.sqlString( borrowMaterial.getInComments()),
        		SqlUtils.sqlString( borrowMaterial.getMaterialNumber()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("borrowmaterial", fields, values);
    }
    
}
