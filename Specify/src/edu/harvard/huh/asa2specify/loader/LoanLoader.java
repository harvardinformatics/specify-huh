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

import edu.harvard.huh.asa.AsaLoan;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.PURPOSE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.AsaStringMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.lookup.LoanPreparationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanAgent;
import edu.ku.brc.specify.datamodel.LoanPreparation;

public class LoanLoader extends TaxonBatchTransactionLoader
{
    private static final String DEFAULT_LOAN_NUMBER = "none";

    private AsaStringMapper nameToBotanistMapper;
    private BotanistLookup botanistLookup;
    
    private LoanLookup loanLookup;
    private LoanPreparationLookup loanPrepLookup;
    
    public LoanLoader(File csvFile,
                      Statement sqlStatement,
                      BotanistLookup botanistLookup,
                      AffiliateLookup affiliateLookup,
                      AgentLookup agentLookup,
                      File nameToBotanist) throws LocalException
    {
        super(csvFile,
              sqlStatement,
              botanistLookup,
              affiliateLookup,
              agentLookup);
        
        this.nameToBotanistMapper = new AsaStringMapper(nameToBotanist);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaLoan asaLoan = parse(columns);

        Integer transactionId = asaLoan.getId();
        setCurrentRecordId(transactionId);
        
        String code = asaLoan.getLocalUnit();
        checkNull(code, "local unit");
        
        Integer collectionMemberId = getCollectionId(code);
        
        Loan loan = getLoan(asaLoan);
        
        String forUseBy = asaLoan.getForUseBy();
        Agent borrowerAgent = null;
        Agent contactAgent = null;
        
        if (forUseBy != null)
        {
            contactAgent = lookupAgent(asaLoan);
            
            Integer botanistId = getBotanistId(forUseBy);
            
            if (botanistId != null)
            { 
                borrowerAgent = lookup(botanistId);
            }
        }
        else
        {
            borrowerAgent = lookupAgent(asaLoan);
        }

        String sql = getInsertSql(loan);
        Integer loanId = insert(sql);
        loan.setLoanId(loanId);
        
        if (contactAgent != null)
        {
            LoanAgent contact = getLoanAgent(loan, contactAgent, ROLE.Contact, null);
            
            if (contact != null)
            {
                sql = getInsertSql(contact);
                insert(sql);
            }
        }
        
        if (borrowerAgent != null)
        {
            LoanAgent borrower = getLoanAgent(loan, borrowerAgent, ROLE.Borrower, Transaction.toString(asaLoan.getUserType()));
            if (borrower != null)
            {
                sql = getInsertSql(borrower);
                insert(sql);
            }
        }

        LoanPreparation loanPrep = getLoanPreparation(asaLoan, loan, collectionMemberId);
        if (loanPrep != null)
        {
            sql = getInsertSql(loanPrep);
            insert(sql);
        }
    }
    
    public LoanLookup getLoanLookup()
    {
        if (loanLookup == null)
        {
            loanLookup = new LoanLookup() {

                @Override
                public Loan getById(Integer transactionId) throws LocalException
                {
                    Loan loan = new Loan();
                    
                    // if this changes, you will have to change getLoanPrepLookup as well.
                    Integer loanId = getInt("loan", "LoanID", "Number1", transactionId);
                    
                    loan.setLoanId(loanId);
                    
                    return loan;
                }
            };
        }
        return loanLookup;
    }
    
    public LoanPreparationLookup getLoanPrepLookup()
    {
        if (loanPrepLookup == null)
        {
            loanPrepLookup = new LoanPreparationLookup()
            {
                @Override
                public LoanPreparation getLotById(Integer transactionId) throws LocalException
                {
                    LoanPreparation loanPrep = new LoanPreparation();
              
                    Integer loanPrepId =
                        getInt("select LoanPreparationID from loanpreparation where " +
                        	"PreparationID is null and LoanID=(select LoanID from loan where Number1=" + transactionId + ")");
                    
                    loanPrep.setLoanPreparationId(loanPrepId);
                    
                    return loanPrep;
                }
            };
        }
        return loanPrepLookup;
    }

    private Integer getBotanistId(String name)
    {
        return nameToBotanistMapper.map(name);
    }
    
    private Agent lookup(Integer botanistId) throws LocalException
    {
        return botanistLookup.getById(botanistId);
    }
    
    private AsaLoan parse(String[] columns) throws LocalException
    {        
        AsaLoan asaLoan = new AsaLoan();
        
        super.parse(columns, asaLoan);
        
        return asaLoan;
    }
    
    private Loan getLoan(AsaLoan asaLoan) throws LocalException
    {
        Loan loan = new Loan();
        
        // TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = asaLoan.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        loan.setCreatedByAgent(createdByAgent);
        
        // CurrentDueDate
        Date currentDueDate = asaLoan.getCurrentDueDate();
        if (currentDueDate != null)
        {
            loan.setCurrentDueDate(DateUtils.toCalendar(currentDueDate));
        }
        
        // DateClosed
        Date closeDate = asaLoan.getCloseDate();
        if (closeDate != null)
        {
            loan.setDateClosed(DateUtils.toCalendar(closeDate));
        }

        // DisciplineID
        loan.setDiscipline(getBotanyDiscipline());
        
        // IsClosed
        loan.setIsClosed(closeDate != null);
        
        // LoanDate
        Date openDate = asaLoan.getOpenDate();
        if (openDate != null)
        {
            loan.setLoanDate(DateUtils.toCalendar(openDate));
        }
        
        // LoanNumber
        String transactionNo = asaLoan.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_LOAN_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "transaction number");
        loan.setLoanNumber(transactionNo);

        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = asaLoan.getId();
        checkNull(transactionId, "transaction id");
        
        loan.setNumber1((float) transactionId);
        
        // OriginalDueDate
        Date originalDueDate = asaLoan.getOriginalDueDate();
        if (originalDueDate != null)
        {
            loan.setOriginalDueDate(DateUtils.toCalendar(originalDueDate));
        }
        
        // PurposeOfLoan
        PURPOSE purpose = asaLoan.getPurpose();
        String purposeOfLoan = Transaction.toString(purpose);
        loan.setPurposeOfLoan(purposeOfLoan);
        
        // ReceivedComments
        
        // Remarks
        String remarks = asaLoan.getRemarks();
        loan.setRemarks(remarks);
        
        // Text1 (local unit)
        String localUnit = asaLoan.getLocalUnit();
        loan.setText1(localUnit);
        
        // Text2 (for use by)
        String forUseBy = asaLoan.getForUseBy();
        loan.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = asaLoan.getDateCreated();
        loan.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = asaLoan.isAcknowledged();
        loan.setYesNo1(isAcknowledged);
                       
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(asaLoan.getRequestType());
        loan.setYesNo2(isTheirs);
        
        return loan;
    }
    
    private LoanPreparation getLoanPreparation(AsaLoan asaLoan, Loan loan, Integer collectionMemberId) throws LocalException
    {
        LoanPreparation loanPreparation = new LoanPreparation();
        
        // Description (description, box count)
        String taxonDescription = getDescriptionOfMaterial(asaLoan);
        loanPreparation.setDescriptionOfMaterial(taxonDescription);
   
        // Discipline
        loanPreparation.setDiscipline(getBotanyDiscipline());
        
        // HigherTaxon
        String higherTaxon = asaLoan.getHigherTaxon();
        higherTaxon = truncate(higherTaxon, 32, "higher taxon");
        loanPreparation.setHigherTaxon(higherTaxon);
        
        // IsResolved
        boolean isClosed = asaLoan.getCloseDate() != null;
        
        int quantity = asaLoan.getBatchQuantity();
        int quantityReturned = asaLoan.getBatchQuantityReturned() == null ? 0 : asaLoan.getBatchQuantityReturned();

        // yes, that's a >=. grumble, grumble.
        loanPreparation.setIsResolved(quantityReturned >= quantity && isClosed);

        if (quantityReturned > quantity)
        {
            getLogger().warn(rec() + "More items returned than sent.");
        }

        // Loan
        loanPreparation.setLoan(loan);
        
        // NonSpecimenCount
        int nonSpecimenCount = asaLoan.getNonSpecimenCount();
        loanPreparation.setNonSpecimenCount(nonSpecimenCount);
                
        // Quantity (itemCount + typeCount + nonSpecimenCount)
        loanPreparation.setQuantity(quantity);
        
        // QuantityReturned
        loanPreparation.setQuantityReturned(quantityReturned);

        // SrcTaxonomy
        String srcTaxonomy = asaLoan.getTaxon();
        loanPreparation.setSrcTaxonomy(srcTaxonomy);
        
        // TypeCount
        int typeCount = asaLoan.getTypeCount();
        loanPreparation.setTypeCount(typeCount);

        return loanPreparation;
    }

    private LoanAgent getLoanAgent(Loan loan, Agent agent, ROLE role, String userType) throws LocalException
    {
        LoanAgent loanAgent = new LoanAgent();
        
        // Agent
        loanAgent.setAgent(agent);
        
        // Discipline
        loanAgent.setDiscipline(getBotanyDiscipline());
        
        // LoanID
        loanAgent.setLoan(loan);
        
        // Remarks
        loan.setRemarks(userType);
        
        // Role
        loanAgent.setRole(Transaction.toString(role));
        
        return loanAgent;
    }
    
    private String getInsertSql(Loan loan)
    {
        String fieldNames = "CreatedByAgentID, CurrentDueDate, DateClosed, DisciplineId, " +
                            "IsClosed, LoanDate, LoanNumber, Number1, OriginalDueDate, PurposeOfLoan, " +
                            "Remarks, Text1, Text2, TimestampCreated, Version, YesNo1, YesNo2";
        
        String[] values = new String[17];
        
        values[0]  = SqlUtils.sqlString( loan.getCreatedByAgent().getId());
        values[1]  = SqlUtils.sqlString( loan.getCurrentDueDate());
        values[2]  = SqlUtils.sqlString( loan.getDateClosed());
        values[3]  = SqlUtils.sqlString( loan.getDiscipline().getId());
        values[4]  = SqlUtils.sqlString( loan.getIsClosed());
        values[5]  = SqlUtils.sqlString( loan.getLoanDate());
        values[6]  = SqlUtils.sqlString( loan.getLoanNumber());
        values[7]  = SqlUtils.sqlString( loan.getNumber1());
        values[8]  = SqlUtils.sqlString( loan.getOriginalDueDate());
        values[9]  = SqlUtils.sqlString( loan.getPurposeOfLoan());
        values[10] = SqlUtils.sqlString( loan.getRemarks());
        values[11] = SqlUtils.sqlString( loan.getText1());
        values[12] = SqlUtils.sqlString( loan.getText2());
        values[13] = SqlUtils.sqlString( loan.getTimestampCreated());
        values[14] = SqlUtils.zero();
        values[15] = SqlUtils.sqlString( loan.getYesNo1());
        values[16] = SqlUtils.sqlString( loan.getYesNo2());
        
        return SqlUtils.getInsertSql("loan", fieldNames, values);
    }
    
    private String getInsertSql(LoanAgent loanAgent)
    {
        String fieldNames = "AgentID, DisciplineID, LoanID, Remarks, Role, TimestampCreated, Version";
            
        String[] values = new String[7];
        
        values[0] = SqlUtils.sqlString( loanAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( loanAgent.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( loanAgent.getLoan().getId());
        values[3] = SqlUtils.sqlString( loanAgent.getRemarks());
        values[4] = SqlUtils.sqlString( loanAgent.getRole());
        values[5] = SqlUtils.now();
        values[6] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("loanagent", fieldNames, values);
    }
    
    private String getInsertSql(LoanPreparation loanPreparation)
    {
        String fieldNames = "DescriptionOfMaterial, DisciplineID, HigherTaxon, IsResolved, LoanID, " +
        		            "NonSpecimenCount, Quantity, QuantityReturned, SrcTaxonomy, " +
        		            "TimestampCreated, TypeCount, Version";
        
        String[] values = new String[12];
        
        values[0]  = SqlUtils.sqlString( loanPreparation.getDescriptionOfMaterial());
        values[1]  = SqlUtils.sqlString( loanPreparation.getDiscipline().getId());
        values[2]  = SqlUtils.sqlString( loanPreparation.getHigherTaxon());
        values[3]  = SqlUtils.sqlString( loanPreparation.getIsResolved());
        values[4]  = SqlUtils.sqlString( loanPreparation.getLoan().getId());
        values[5]  = SqlUtils.sqlString( loanPreparation.getNonSpecimenCount());
        values[6]  = SqlUtils.sqlString( loanPreparation.getQuantity());
        values[7]  = SqlUtils.sqlString( loanPreparation.getQuantityReturned());
        values[8]  = SqlUtils.sqlString( loanPreparation.getSrcTaxonomy());
        values[9]  = SqlUtils.now();
        values[10] = SqlUtils.sqlString( loanPreparation.getTypeCount());
        values[11] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
    }
}
