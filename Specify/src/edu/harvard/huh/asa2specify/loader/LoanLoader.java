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
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.loader.TransactionLoader;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanAgent;

public class LoanLoader extends TransactionLoader
{
    private static final String DEFAULT_LOAN_NUMBER = "none";

    private LoanLookup loanLookup;
    
    public LoanLoader(File csvFile,
                      Statement sqlStatement,
                      File affiliateBotanists,
                      File agentBotanists,
                      BotanistLookup botanistLookup,
                      AgentLookup agentLookup,
                      AffiliateLookup affiliateLookup) throws LocalException
    {
        super(csvFile,
              sqlStatement,
              affiliateBotanists,
              agentBotanists,
              botanistLookup,
              agentLookup,
              affiliateLookup);
        
        // TODO Auto-generated constructor stub
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
        
        String sql = getInsertSql(loan);
        Integer loanId = insert(sql);
        loan.setLoanId(loanId);
        
        LoanAgent borrower = getLoanAgent(asaLoan, loan, ROLE.borrower, collectionMemberId); // "contact"
        if (borrower != null)
        {
            sql = getInsertSql(borrower);
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
                    
                    Integer loanId = getInt("loan", "LoanID", "Number1", transactionId);
                    
                    loan.setLoanId(loanId);
                    
                    return loan;
                }
            };
        }
        return loanLookup;
    }
    
    private AsaLoan parse(String[] columns) throws LocalException
    {        
        AsaLoan asaLoan = new AsaLoan();
        
        int i = parse(columns, asaLoan);
        
        if (columns.length < i + 4)
        {
            throw new LocalException("Not enough columns");
        }
        
        asaLoan.setOriginalDueDate( SqlUtils.parseDate( columns[i + 0] ));
        asaLoan.setCurrentDueDate(  SqlUtils.parseDate( columns[i + 1] ));           
        asaLoan.setHigherTaxon(                         columns[i + 2] );
        asaLoan.setTaxon(                               columns[i + 3] );

        return asaLoan;
    }
    
    private Loan getLoan(AsaLoan asaLoan) throws LocalException
    {
        Loan loan = new Loan();
        
        // TODO: their are some loans with forUseBy = ours, do we note that somewhere?
        
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

        // DateReceived

        // DisciplineID
        loan.setDiscipline(getBotanyDiscipline());

        // DivisionID
        loan.setDivision(getBotanyDivision());
        
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
        
        // SpecialConditions
        
        // TODO: SrcGeography
        
        // SrcTaxonomy
        String higherTaxon = asaLoan.getHigherTaxon();
        String taxon = asaLoan.getTaxon();
        
        String srcTaxonomy = null;
        if (higherTaxon != null)
        {
            if (taxon != null) srcTaxonomy = higherTaxon + " " + taxon;
            else srcTaxonomy = higherTaxon;
        }
        else
        {
        	srcTaxonomy = higherTaxon;
        }
        if (srcTaxonomy != null)
        {
        	srcTaxonomy = truncate(srcTaxonomy, 32, "src taxonomy");
        	loan.setSrcTaxonomy(srcTaxonomy);
        }
        
        // Text1 (description)
        String description = asaLoan.getDescription();
        loan.setText1(description);
        
        // Text2
        String forUseBy = asaLoan.getForUseBy();
        loan.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = asaLoan.getDateCreated();
        loan.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = asaLoan.isAcknowledged();
        loan.setYesNo1(isAcknowledged);
                        
        return loan;
    }
    
    private LoanAgent getLoanAgent(Transaction transaction, Loan loan, ROLE role, Integer collectionMemberId) throws LocalException
    {
        LoanAgent loanAgent = new LoanAgent();
        
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.preparer))
        {
            agent = getAffiliateAgent(transaction);
        }
        else if (role.equals(ROLE.borrower))
        {
            agent = getAsaAgentAgent(transaction);
        }

        if (agent.getId() == null) return null;
        
        loanAgent.setAgent(agent);
        
        // CollectionMemberID
        loanAgent.setCollectionMemberId(collectionMemberId);
        
        // LoanID
        loanAgent.setLoan(loan);
        
        // Remarks
        String forUseBy = transaction.getForUseBy();
        String userType = transaction.getUserType().name();
        
        if (role.equals(ROLE.borrower)) // "for use by"
        {
            String remarks = "For use by " + (forUseBy != null ? forUseBy : "") + "(" + userType + ")";
            loan.setRemarks(remarks);
        }
        
        // Role
        loanAgent.setRole(role.name());
        
        return loanAgent;
    }
    private String getInsertSql(Loan loan)
    {
        String fieldNames = "CreatedByAgentID, CurrentDueDate, DateClosed, DisciplineId, DivisionId, " +
                            "IsClosed, LoanDate, LoanNumber, Number1, OriginalDueDate, PurposeOfLoan, " +
                            "Remarks, SrcTaxonomy, Text1, Text2, TimestampCreated, YesNo1";
        
        String[] values = new String[17];
        
        values[0]  = SqlUtils.sqlString( loan.getCreatedByAgent().getId());
        values[1]  = SqlUtils.sqlString( loan.getCurrentDueDate());
        values[2]  = SqlUtils.sqlString( loan.getDateClosed());
        values[3]  = SqlUtils.sqlString( loan.getDiscipline().getId());
        values[4]  = SqlUtils.sqlString( loan.getDivision().getId());
        values[5]  = SqlUtils.sqlString( loan.getIsClosed());
        values[6]  = SqlUtils.sqlString( loan.getLoanDate());
        values[7]  = SqlUtils.sqlString( loan.getLoanNumber());
        values[8]  = SqlUtils.sqlString( loan.getNumber1());
        values[9]  = SqlUtils.sqlString( loan.getOriginalDueDate());
        values[10] = SqlUtils.sqlString( loan.getPurposeOfLoan());
        values[11] = SqlUtils.sqlString( loan.getRemarks());
        values[12] = SqlUtils.sqlString( loan.getSrcTaxonomy());
        values[13] = SqlUtils.sqlString( loan.getText1());
        values[14] = SqlUtils.sqlString( loan.getText2());
        values[15] = SqlUtils.sqlString( loan.getTimestampCreated());
        values[16] = SqlUtils.sqlString( loan.getYesNo1());
        
        return SqlUtils.getInsertSql("loan", fieldNames, values);
    }
    
    private String getInsertSql(LoanAgent loanAgent)
    {
        String fieldNames = "AgentID, CollectionMemberID, LoanID, Role, TimestampCreated";
            
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( loanAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( loanAgent.getCollectionMemberId());
        values[2] = SqlUtils.sqlString( loanAgent.getLoan().getId());
        values[3] = SqlUtils.sqlString( loanAgent.getRole());
        values[4] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("loanagent", fieldNames, values);
    }
}