package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.ACCESSION_TYPE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AccessionLookup;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public abstract class InGeoBatchTransactionLoader extends TransactionLoader
{
    private final static Pattern NUMBER  = Pattern.compile("^(A|FH|GH)-(\\d+)$");
    
    private final static String ACC_NO_FMT = "00000";

    private AccessionLookup accessionLookup;
    
    public InGeoBatchTransactionLoader(File csvFile,
                                       Statement sqlStatement,
                                       BotanistLookup botanistLookup,
                                       AffiliateLookup affiliateLookup,
                                       AgentLookup agentLookup,
                                       OrganizationLookup organizationLookup) throws LocalException
    {
        super(csvFile, sqlStatement, botanistLookup, affiliateLookup, agentLookup, organizationLookup);
    }
        
    public AccessionLookup getAccessionLookup()
    {
        if (accessionLookup == null)
        {
            accessionLookup = new AccessionLookup() {

                @Override
                public Accession getById(Integer transactionId) throws LocalException
                {
                    Accession accession = new Accession();
                    
                    Integer accessionId = getInt("accession", "AccessionID", "Number1", transactionId);
                    
                    accession.setAccessionId(accessionId);
                    
                    return accession;
                }
            };
        }
        return accessionLookup;
    }
    
    protected InGeoBatchTransactionLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    protected Accession getAccession(Transaction transaction, ACCESSION_TYPE type) throws LocalException
    {
        Accession accession = new Accession();

        // TODO: AddressOfRecord
        
        // AccessionCondition
        String description = getDescriptionOfMaterial(transaction);
        if (description != null) description = truncate(description, 255, "accession condition");
        accession.setAccessionCondition(description);
        
        // AccessionNumber
        String transactionNo = transaction.getTransactionNo();
        String accessionNumber = getAccessionNumber(transactionNo);
        accession.setAccessionNumber(accessionNumber);

        // DateAccessioned
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
        // Division
        accession.setDivision(getBotanyDivision());
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = transaction.getRemarks();
        accession.setRemarks(remarks);
        
        // Text1 (local unit)
        String localUnit = transaction.getLocalUnit();
        accession.setText1(localUnit);
        
        // Text2 (purpose)
        String purpose = Transaction.toString(transaction.getPurpose());
        accession.setText2(purpose);
        
        // Text3 (transaction no)
        accession.setText3(transactionNo);
        
        // Type
        accession.setType(Transaction.toString(type));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(transaction.getRequestType());
        accession.setYesNo2(isTheirs);
        
        setAuditFields(transaction, accession);
        
        return accession;
    }
    
    protected AccessionAgent getAccessionAgent(Accession accession, Agent agent, ROLE role)
        throws LocalException
    {
        AccessionAgent accessionAgent = new AccessionAgent();

        // Accession
        accessionAgent.setAccession(accession);
        
        // Agent
        accessionAgent.setAgent(agent);

        // Remarks

        // Role
        accessionAgent.setRole(Transaction.toString(role));

        return accessionAgent;
    }
    
    protected String getAccessionNumber(String accNo) throws LocalException
    {
        // match ^(A|FH|GH)-(\d+)$
        Matcher numberMatcher = NUMBER.matcher(accNo);
        if (numberMatcher.matches())
        {
            /*String s1 = numberMatcher.group(1);
            
            if (s1.equals("A"))       s1 = "   A";
            else if (s1.equals("FH")) s1 = "  FH";
            else if (s1.equals("GH")) s1 = "  GH";*/
            
            String s2 = numberMatcher.group(2);
            return  "HUH-" + (new DecimalFormat( ACC_NO_FMT ) ).format( Integer.parseInt( s2 ) );
        }
        
        throw new LocalException(rec() + "didn't match accession number: " + accNo);
    }
    
    protected String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionCondition, AccessionNumber, CreatedByAgentID, DateAccessioned, " +
                            "DivisionID, ModifiedByAgentID, Number1, Remarks, Text1, Text2, Text3, " +
                            "Type, TimestampCreated, TimestampModified, Version, YesNo1, YesNo2";

        String[] values = {
        		SqlUtils.sqlString( accession.getAccessionCondition()),
        		SqlUtils.sqlString( accession.getAccessionNumber()),
        		SqlUtils.sqlString( accession.getCreatedByAgent().getId()),
        		SqlUtils.sqlString( accession.getDateAccessioned()),
        		SqlUtils.sqlString( accession.getDivision().getId()),
        		SqlUtils.sqlString( accession.getModifiedByAgent().getId()),
        		SqlUtils.sqlString( accession.getNumber1()),
        		SqlUtils.sqlString( accession.getRemarks()),
        		SqlUtils.sqlString( accession.getText1()),
        		SqlUtils.sqlString( accession.getText2()),
        		SqlUtils.sqlString( accession.getText3()),
        		SqlUtils.sqlString( accession.getType()),
        		SqlUtils.sqlString( accession.getTimestampCreated()),
        		SqlUtils.sqlString( accession.getTimestampModified()),
        		SqlUtils.one(),
        		SqlUtils.sqlString( accession.getYesNo1()),
        		SqlUtils.sqlString( accession.getYesNo1())
        };
        
        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    protected String getInsertSql(AccessionAgent accessionAgent)
    {
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated, Version";

        String[] values = {
        		SqlUtils.sqlString( accessionAgent.getAccession().getId()),
        		SqlUtils.sqlString( accessionAgent.getAgent().getId()),
        		SqlUtils.sqlString( accessionAgent.getRole()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }
}
