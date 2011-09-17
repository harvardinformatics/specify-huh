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
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.harvard.huh.asa.AsaLoan;
import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.PURPOSE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa.Transaction.USER_TYPE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.lookup.LoanPreparationLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanAgent;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Taxon;

public class LoanLoader extends TaxonBatchTransactionLoader
{
    private final static Pattern YY_DASH_YY_NUMBER       = Pattern.compile("^(\\d\\d)-(\\d\\d)(\\d+)$");
    private final static Pattern YY_DASH_NUMBER_19XX     = Pattern.compile("^([6789]\\d)-(\\d\\d?\\d?\\d?)$");
    private final static Pattern YY_SLASH_YY_DASH_NUMBER = Pattern.compile("^(\\d\\d)/(\\d\\d)-(\\d+)$");
    private final static Pattern YY_DASH_NUMBER_20XX     = Pattern.compile("^(0\\d)-(\\d\\d\\d\\d\\d?)$");
    private final static Pattern NUMBER                  = Pattern.compile("^(\\d+)$");
    
    private final static String LOAN_NO_FMT = "00000";
    
    private LoanLookup loanLookup;
    private LoanPreparationLookup loanPrepLookup;
    
    public LoanLoader(File csvFile,
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
    
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaLoan asaLoan = parse(columns);

        Integer transactionId = asaLoan.getId();
        setCurrentRecordId(transactionId);
        
        String code = asaLoan.getLocalUnit();
        checkNull(code, "local unit");
        Loan loan = getLoan(asaLoan);

        int organizationId = asaLoan.getOrganizationId();
        boolean isSelfOrganized = Organization.IsSelfOrganizing(organizationId);
        
        Agent borrowerAgent = isSelfOrganized ? lookupAgent(asaLoan) : lookupOrganization(asaLoan);
        
        String sql = getInsertSql(loan);
        Integer loanId = insert(sql);
        loan.setLoanId(loanId);
        
        if (borrowerAgent != null)
        {
            LoanAgent borrower = getLoanAgent(loan, borrowerAgent, ROLE.Borrower);
            if (borrower != null)
            {
                sql = getInsertSql(borrower);
                insert(sql);
            }
        }
        
        LoanPreparation loanPrep = getLoanPreparation(asaLoan, loan);
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
                        getInt("select lp.LoanPreparationID from loanpreparation lp join preparation pp on lp.PreparationID=pp.PreparationID " +
                        	"where pp.PrepTypeID=(select PrepTypeID from preptype where Name='Lot') and lp.LoanID=(select LoanID from loan where Number1=" + transactionId + ")");
                    
                    loanPrep.setLoanPreparationId(loanPrepId);
                    
                    return loanPrep;
                }
            };
        }
        return loanPrepLookup;
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
        Calendar loanDate = DateUtils.toCalendar(openDate);
        loan.setLoanDate(loanDate);
        
        // LoanNumber
        Integer transactionId = asaLoan.getId();
        checkNull(transactionId, "transaction id");
        
        String transactionNo = asaLoan.getTransactionNo();
        
        int year = loanDate.get(Calendar.YEAR);
        
        String loanNumber = getLoanNumber(transactionNo, year, transactionId);
        
        loan.setLoanNumber(loanNumber);
        
        // Number1 (id) TODO: temporary!! remove when done!        
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
        
        // Remarks
        // ... remarks
        String remarks = asaLoan.getRemarks();
        
        // ... user type: staff/student/visitor/unknown
        String userType = null;
        if (asaLoan.getUserType().equals(USER_TYPE.Staff) || asaLoan.getUserType().equals(USER_TYPE.Student))
        {
            userType = denormalize("user type", Transaction.toString(asaLoan.getUserType()));
        }   
        
        // ... visitor?
        String visitor = denormalize("visitor?", asaLoan.getUserType().equals(Transaction.USER_TYPE.Visitor) ? "yes" : "no");
        
        asaLoan.setRemarks(concatenate(remarks, userType, visitor));
        
        // SrcGeography (transaction no)
        loan.setSrcGeography(transactionNo);
        
        // Text1 (for use by)
        String forUseBy = asaLoan.getForUseBy();
        loan.setText1(forUseBy);
        
        // Text2 (local unit)
        String localUnit = asaLoan.getLocalUnit();
        String account = "A".equals(localUnit) ? "GC" : "FH".equals(localUnit) ? "FC" : localUnit;
        loan.setText2(account);
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = asaLoan.isAcknowledged();
        loan.setYesNo1(isAcknowledged);
                       
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(asaLoan.getRequestType());
        loan.setYesNo2(isTheirs);
        
        setAuditFields(asaLoan, loan);
        
        return loan;
    }
    
    private LoanPreparation getLoanPreparation(AsaLoan asaLoan, Loan loan) throws LocalException
    {
        LoanPreparation loanPreparation = new LoanPreparation();
        
        // Description (description, box count)
        String taxonDescription = getDescriptionWithBoxCount(asaLoan);
        loanPreparation.setDescriptionOfMaterial(taxonDescription);
   
        // Discipline
        loanPreparation.setDiscipline(getBotanyDiscipline());
        
        // IsResolved
        boolean isClosed = asaLoan.getCloseDate() != null;
        loanPreparation.setIsResolved(isClosed);
        
        // Loan
        loanPreparation.setLoan(loan);
        
        // OutComments
        
        // ... higher taxon
        Integer taxonId = asaLoan.getHigherTaxonId();
        Taxon higherTaxon = null;
        if (taxonId != null) higherTaxon = lookupTaxon(taxonId);
        String higherTaxonName = denormalize("higher taxon", this.getString("taxon", "Name", "TaxonID", higherTaxon.getId()));
        
        // ... taxon
        String taxon = denormalize("taxon", asaLoan.getTaxon());
        
        // ... item count
        int itemCount = asaLoan.getItemCount();
        String items = denormalize("items", String.valueOf(itemCount));
        
        // ... type count
        int typeCount = asaLoan.getTypeCount();
        String types = denormalize("types", String.valueOf(typeCount));
        
        // ... non-specimen count
        int nonSpecimenCount = asaLoan.getNonSpecimenCount();
        String nonSpecimens = denormalize("non-specimens", String.valueOf(nonSpecimenCount));
        
        loanPreparation.setOutComments(concatenate(higherTaxonName, taxon, items, types, nonSpecimens));
        
        // Quantity
        loanPreparation.setQuantity(itemCount + typeCount + nonSpecimenCount);

        // ReceivedComments (transferred from)
        String transferredFrom = asaLoan.getReceivedComments();
        loanPreparation.setInComments(transferredFrom);

        return loanPreparation;        

    }

    private LoanAgent getLoanAgent(Loan loan, Agent agent, ROLE role) throws LocalException
    {
        LoanAgent loanAgent = new LoanAgent();
        
        // Agent
        loanAgent.setAgent(agent);
        
        // Discipline
        loanAgent.setDiscipline(getBotanyDiscipline());
        
        // LoanID
        loanAgent.setLoan(loan);
        
        // Role
        loanAgent.setRole(Transaction.toString(role));
        
        return loanAgent;
    }
    
    protected String getLoanNumber(String loanNo, int year, int id)
    {
        if (id == 1)     return "91-001375";//return "1991-001375";
        if (id == 21)    return "91-001376";//return "1991-001376";
        if (id == 636)   return "52-000279";//return "1952-000279";
        if (id == 848)   return "59-000436";//return "1959-000436";
        if (id == 997)   return "75-000210";//return "1975-000210";
        if (id == 1034)  return "77-000037";//return "1977-000037";
        if (id == 1665)  return "85-000277";//return "1985-000277";
        if (id == 2541)  return "90-000021";//return "1990-000021";
        if (id == 2662)  return "91-000090";//return "1991-000090";
        if (id == 2908)  return "90-000018";//return "1990-000018";
        if (id == 2927)  return "90-000173";//return "1990-000173";
        if (id == 4470)  return "52-000702";//return "1952-000702";
        if (id == 7008)  return "58-000301";//return "1958-000301";
        if (id == 13203) return "86-000198";//return "1986-000198";
        if (id == 13204) return "86-000199";//return "1986-000199";
        if (id == 13205) return "87-000053";//return "1987-000053";
        
      /*if (id == 1)     return "1991-001375";
        if (id == 21)    return "1991-001376";
        if (id == 997)   return "1975-000210";
        if (id == 1665)  return "1985-000277";
        if (id == 2541)  return "1990-000021";
        if (id == 2662)  return "1991-000090";
        if (id == 2908)  return "1990-000018";
        if (id == 2927)  return "1990-000173";
        if (id == 13203) return "1986-000198";
        if (id == 13204) return "1986-000199";
        if (id == 13205) return "1987-000053";*/
        
        // match ^([6-9]\d)-(\d{1,4})$ -> 19$1-$2
        Matcher yyDashNumber19xxMatcher = YY_DASH_NUMBER_19XX.matcher(loanNo);        
        if (yyDashNumber19xxMatcher.matches())
        {
            String s1 = yyDashNumber19xxMatcher.group(1);
            String s2 = yyDashNumber19xxMatcher.group(2);
            
            //return "19" + s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 ) );
            return s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 ) );
        }
                
        // match ^(0[0-9])-(\d{4,5})$ -> 20$1-$2
        Matcher yyDashNumber20xxMatcher = YY_DASH_NUMBER_20XX.matcher(loanNo);
        if (yyDashNumber20xxMatcher.matches())
        {
            String s1 = yyDashNumber20xxMatcher.group(1);
            String s2 = yyDashNumber20xxMatcher.group(2);
            
            int year1 = 2000 + Integer.parseInt(s1);
            
            if (Math.abs(year - year1) > 1) getLogger().warn(rec() + "loan number " + loanNo + " doesn't match year " + year);
            
            //return "20" + s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 ) );
            return s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 ) );
        }
        
        // match ^(\d\d)-(\d\d)(\d+)$  if $2-$1=1, 19$1-$3, else 19$1-$2$3
        Matcher yyNumberMatcher = YY_DASH_YY_NUMBER.matcher(loanNo);
        if (yyNumberMatcher.matches())
        {
            String s1 = yyNumberMatcher.group(1);
            String s2 = yyNumberMatcher.group(2);
            String s3 = yyNumberMatcher.group(3);
            
            int year1 = 1900 + Integer.parseInt(s1);
            int year2 = 1900 + Integer.parseInt(s2);

            if (year2 - year1 == 1)
            {
                if (Math.abs(year - year1) > 1) getLogger().warn(rec() + "loan number " + loanNo + " doesn't match year " + year);
                
                //return "19" + s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s3 ) );
                return s1 + "-" +(new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s3 ) );
            }
            else
            {
                //return "19" + s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 + s3 ) );
                return s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 + s3 ) );
            }
        }
        
        // match ^(\d\d)/(\d\d)-(\d{2,3}$) -> if $2-$1=1, 19$1-$3
        Matcher yySlashYYDashNumberMatcher = YY_SLASH_YY_DASH_NUMBER.matcher(loanNo);
        if (yySlashYYDashNumberMatcher.matches())
        {
            String s1 = yySlashYYDashNumberMatcher.group(1);
            String s2 = yySlashYYDashNumberMatcher.group(2);
            String s3 = yySlashYYDashNumberMatcher.group(3);
            
            int year1 = 1900 + Integer.parseInt(s1);
            int year2 = 1900 + Integer.parseInt(s2);

            if (year2 - year1 == 1)
            {
                if (Math.abs(year - year1) > 1) getLogger().warn(rec() + "loan number " + loanNo + " doesn't match year " + year);
                
                //return "19" + s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s3 ) );
                return s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s3 ) );
            }
            else
            {
                //return "19" + s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 + s3 ) );
                return s1 + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( s2 + s3 ) );
            }
        }

        // match ^(\d+)$ -> $year-$1
        Matcher numberMatcher = NUMBER.matcher(loanNo);
        if (numberMatcher.matches())
        {
            //return String.valueOf(year) + "-" + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( loanNo ) );
            return String.valueOf(year).substring(2) + (new DecimalFormat( LOAN_NO_FMT ) ).format( Integer.parseInt( loanNo ) );
        }
        
        getLogger().warn(rec() + "didn't match loan number: " + loanNo);
        
        return null;
    }

    private String getInsertSql(Loan loan)
    {
        String fieldNames = "CreatedByAgentID, CurrentDueDate, DateClosed, DisciplineId, " +
                            "IsClosed, LoanDate, LoanNumber, ModifiedByAgentID, Number1, " +
                            "OriginalDueDate, PurposeOfLoan, Remarks, SrcGeography, Text1, " +
                            "Text2, TimestampCreated, TimestampModified, Version, " +
                            "YesNo1, YesNo2";
        
        String[] values = {
        		SqlUtils.sqlString( loan.getCreatedByAgent().getId()),
        		SqlUtils.sqlString( loan.getCurrentDueDate()),
        		SqlUtils.sqlString( loan.getDateClosed()),
        		SqlUtils.sqlString( loan.getDiscipline().getId()),
        		SqlUtils.sqlString( loan.getIsClosed()),
        		SqlUtils.sqlString( loan.getLoanDate()),
        		SqlUtils.sqlString( loan.getLoanNumber()),
        		SqlUtils.sqlString( loan.getModifiedByAgent().getId()),
        		SqlUtils.sqlString( loan.getNumber1()),
        		SqlUtils.sqlString( loan.getOriginalDueDate()),
        		SqlUtils.sqlString( loan.getPurposeOfLoan()),
        		SqlUtils.sqlString( loan.getRemarks()),
        		SqlUtils.sqlString( loan.getSrcGeography()),
        		SqlUtils.sqlString( loan.getText1()),
        		SqlUtils.sqlString( loan.getText2()),
        		SqlUtils.sqlString( loan.getTimestampCreated()),
        		SqlUtils.sqlString( loan.getTimestampModified()),
        		SqlUtils.one(),
        		SqlUtils.sqlString( loan.getYesNo1()),
        		SqlUtils.sqlString( loan.getYesNo2())
        };
        
        return SqlUtils.getInsertSql("loan", fieldNames, values);
    }
    
    private String getInsertSql(LoanAgent loanAgent)
    {
        String fieldNames = "AgentID, DisciplineID, LoanID, Role, TimestampCreated, Version";
            
        String[] values = {
        		SqlUtils.sqlString( loanAgent.getAgent().getId()),
        		SqlUtils.sqlString( loanAgent.getDiscipline().getId()),
        		SqlUtils.sqlString( loanAgent.getLoan().getId()),
        		SqlUtils.sqlString( loanAgent.getRole()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("loanagent", fieldNames, values);
    }
    
    private String getInsertSql(LoanPreparation loanPreparation)
    {
        String fieldNames = "DescriptionOfMaterial, DisciplineID, IsResolved, " +
        		            "LoanID, OutComments, Quantity, ReceivedComments, " +
        		            "TimestampCreated, Version";
        
        String[] values = {
        		SqlUtils.sqlString( loanPreparation.getDescriptionOfMaterial()),
        		SqlUtils.sqlString( loanPreparation.getDiscipline().getId()),
        		SqlUtils.sqlString( loanPreparation.getIsResolved()),
        		SqlUtils.sqlString( loanPreparation.getLoan().getId()),
        		SqlUtils.sqlString( loanPreparation.getOutComments()),
        		SqlUtils.sqlString( loanPreparation.getQuantity()),
        		SqlUtils.sqlString( loanPreparation.getReceivedComments()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
    }
}
