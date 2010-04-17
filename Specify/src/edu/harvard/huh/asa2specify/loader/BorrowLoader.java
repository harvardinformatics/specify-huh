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
import java.util.Date;

import edu.harvard.huh.asa.AsaBorrow;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.ROLE;
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
    private static final String DEFAULT_BORROW_NUMBER = "none";
    
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
    // The complimentary direction of these transactions are out_return batch records.
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
        
        BorrowAgent borrower = getBorrowAgent(asaBorrow, borrow, ROLE.Borrower, collectionMemberId); // "for use by"
        if (borrower != null)
        {
            sql = getInsertSql(borrower);
            insert(sql);
        }
        
        BorrowAgent contact = getBorrowAgent(asaBorrow, borrow, ROLE.Contact, collectionMemberId); // "contact"
        if (contact != null)
        {
            sql = getInsertSql(contact);
            insert(sql);
        }
        
        BorrowAgent lender = getBorrowAgent(asaBorrow, borrow, ROLE.Lender, collectionMemberId); // "contact"
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
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_BORROW_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "invoice number");
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
        String remarks = asaBorrow.getRemarks();
        borrow.setRemarks(remarks);
                
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
    
    private BorrowAgent getBorrowAgent(Transaction transaction, Borrow borrow, ROLE role, Integer collectionMemberId) throws LocalException
    {
        BorrowAgent borrowAgent = new BorrowAgent();
        
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.Borrower))
        {
            agent = lookupAffiliate(transaction);
        }
        else if (role.equals(ROLE.Contact))
        {
            agent = lookupAgent(transaction);
        }
        else if (role.equals(ROLE.Lender))
        {
            agent = lookupOrganization(transaction);
        }

        if (agent ==  null || agent.getId() == null) return null;
        
        borrowAgent.setAgent(agent);
        
        // CollectionMemberID
        borrowAgent.setCollectionMemberId(collectionMemberId);
        
        // LoanID
        borrowAgent.setBorrow(borrow);
        
        // Remarks (borrow.userType)
        if (role.equals(ROLE.Borrower))
        {
            String userType = Transaction.toString(transaction.getUserType());
            borrowAgent.setRemarks(userType);
        }
        
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
        
        // MaterialNumber (transaction no)
        String transactionNo = asaBorrow.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_BORROW_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "material number");
        
        borrowMaterial.setMaterialNumber(transactionNo);
        
        // NonSpecimenCount
        int nonSpecimenCount = asaBorrow.getNonSpecimenCount();
        borrowMaterial.setNonSpecimenCount((short) nonSpecimenCount);
        
        // Quantity (itemCount + typeCount + nonSpecimenCount)
        int quantity = asaBorrow.getBatchQuantity();
        borrowMaterial.setQuantity((short) quantity);
        
        // QuantityResolved (quantity returned)
        int quantityReturned = asaBorrow.getBatchQuantityReturned() == null ? 0 : asaBorrow.getBatchQuantityReturned();
        borrowMaterial.setQuantityResolved((short) quantityReturned);
        
        // QuantityReturned
        borrowMaterial.setQuantityReturned((short) quantityReturned);
        
        // SrcTaxonomy
        String taxon = asaBorrow.getTaxon();
        taxon = truncate(taxon, 512, "taxon");
        borrowMaterial.setSrcTaxonomy(taxon);
        
        // Taxon
        Integer taxonId = asaBorrow.getHigherTaxonId();
        Taxon higherTaxon = NullTaxon;
        
        if (taxonId != null) higherTaxon = lookupTaxon(taxonId);
        borrowMaterial.setTaxon(higherTaxon);
        
        // TypeCount
        int typeCount = asaBorrow.getTypeCount();
        borrowMaterial.setTypeCount((short) typeCount);
        
        return borrowMaterial;
    }
    
    private String getInsertSql(Borrow borrow)
    {
        String fieldNames = "CollectionMemberID, CreatedByAgentID, CurrentDueDate, DateClosed, " +
                            "InvoiceNumber, IsClosed, ModifiedByAgentID, Number1, OriginalDueDate, " +
                            "ReceivedDate, Remarks, Text1, Text2, TimestampCreated, TimestampModified, " +
                            "Version, YesNo1, YesNo2";
        
        String[] values = new String[18];
        
        values[0]  = SqlUtils.sqlString( borrow.getCollectionMemberId());
        values[1]  = SqlUtils.sqlString( borrow.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( borrow.getCurrentDueDate());
        values[3]  = SqlUtils.sqlString( borrow.getDateClosed());
        values[4]  = SqlUtils.sqlString( borrow.getInvoiceNumber());
        values[5]  = SqlUtils.sqlString( borrow.getIsClosed());
        values[6]  = SqlUtils.sqlString( borrow.getModifiedByAgent().getId());
        values[7]  = SqlUtils.sqlString( borrow.getNumber1());
        values[8]  = SqlUtils.sqlString( borrow.getOriginalDueDate());
        values[9]  = SqlUtils.sqlString( borrow.getReceivedDate());
        values[10] = SqlUtils.sqlString( borrow.getRemarks());
        values[11] = SqlUtils.sqlString( borrow.getText1());
        values[12] = SqlUtils.sqlString( borrow.getText2());
        values[13] = SqlUtils.sqlString( borrow.getTimestampCreated());
        values[14] = SqlUtils.sqlString( borrow.getTimestampModified());
        values[15] = SqlUtils.zero();
        values[16] = SqlUtils.sqlString( borrow.getYesNo1());
        values[17] = SqlUtils.sqlString( borrow.getYesNo2());
        
        return SqlUtils.getInsertSql("borrow", fieldNames, values);
    }
    
    private String getInsertSql(BorrowAgent borrowAgent)
    {
        String fieldNames = "AgentID, BorrowID, CollectionMemberID, Remarks, Role, " +
        		            "TimestampCreated, Version";

        String[] values = new String[7];

        values[0] = SqlUtils.sqlString( borrowAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( borrowAgent.getBorrow().getId());
        values[2] = SqlUtils.sqlString( borrowAgent.getCollectionMemberId());
        values[3] = SqlUtils.sqlString( borrowAgent.getRemarks());
        values[4] = SqlUtils.sqlString( borrowAgent.getRole());
        values[5] = SqlUtils.now();
        values[6] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("borrowagent", fieldNames, values);
    }
    
    private String getInsertSql(BorrowMaterial borrowMaterial)
    {
        String fields = "BorrowID, CollectionMemberID, Description, MaterialNumber, " +
        		        "NonSpecimenCount, SrcTaxonomy, Quantity, QuantityResolved, " +
        		        "QuantityReturned, TaxonID, TimestampCreated, TypeCount, Version";
            
        String[] values = new String[13];
        
        values[0]  = SqlUtils.sqlString( borrowMaterial.getBorrow().getId());
        values[1]  = SqlUtils.sqlString( borrowMaterial.getCollectionMemberId());
        values[2]  = SqlUtils.sqlString( borrowMaterial.getDescription());
        values[3]  = SqlUtils.sqlString( borrowMaterial.getMaterialNumber());
        values[4]  = SqlUtils.sqlString( borrowMaterial.getNonSpecimenCount());
        values[5]  = SqlUtils.sqlString( borrowMaterial.getSrcTaxonomy());
        values[6]  = SqlUtils.sqlString( borrowMaterial.getTaxon().getId());
        values[7]  = SqlUtils.sqlString( borrowMaterial.getQuantity());
        values[8]  = SqlUtils.sqlString( borrowMaterial.getQuantityResolved());
        values[9]  = SqlUtils.sqlString( borrowMaterial.getQuantityReturned());
        values[10] = SqlUtils.now();
        values[11] = SqlUtils.sqlString( borrowMaterial.getTypeCount());
        values[12] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("borrowmaterial", fields, values);
    }
    
}
