package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.harvard.huh.asa.InGeoBatchTransaction;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.ACCESSION_TYPE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public abstract class InGeoBatchTransactionLoader extends CountableTransactionLoader
{
    private final static Pattern NUMBER  = Pattern.compile("^(A|FH|GH)-(\\d+)$");
    
    private final static String ACC_NO_FMT = "000000";

    public InGeoBatchTransactionLoader(File csvFile,
                                       Statement sqlStatement,
                                       BotanistLookup botanistLookup,
                                       AffiliateLookup affiliateLookup,
                                       AgentLookup agentLookup) throws LocalException
    {
        super(csvFile, sqlStatement, botanistLookup, affiliateLookup, agentLookup);
    }
        
    protected InGeoBatchTransactionLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    protected int parse(String[] columns, InGeoBatchTransaction inGeoBatchTx) throws LocalException
    {        
        int i = super.parse(columns, inGeoBatchTx);
        
        if (columns.length < i + 5)
        {
            throw new LocalException("Not enough columns");
        }
        
        inGeoBatchTx.setGeoUnit(                             columns[i + 0] );
        inGeoBatchTx.setDiscardCount(     SqlUtils.parseInt( columns[i + 1] ));
        inGeoBatchTx.setDistributeCount(  SqlUtils.parseInt( columns[i + 2] ));
        inGeoBatchTx.setReturnCount(      SqlUtils.parseInt( columns[i + 3] ));
        inGeoBatchTx.setCost(           SqlUtils.parseFloat( columns[i + 4] ));
        
        return i + 5;
    }
    
    protected Accession getAccession(InGeoBatchTransaction transaction, ACCESSION_TYPE type) throws LocalException
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
        
        // AltAccessionNumber
        accession.setAltAccessionNumber(transactionNo);

        // DateAccessioned
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
        // DiscardCount
        int discardCount = transaction.getDiscardCount();
        accession.setDiscardCount((short) discardCount);
        
        // DistributeCount
        int distributeCount = transaction.getDistributeCount();
        accession.setDistributeCount((short) distributeCount);
        
        // Division
        accession.setDivision(getBotanyDivision());
        
        // ItemCount
        int itemCount = transaction.getItemCount();
        accession.setItemCount((short) itemCount);
        
        // NonSpecimenCount
        int nonSpecimenCount = transaction.getNonSpecimenCount();
        accession.setNonSpecimenCount((short) nonSpecimenCount);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = transaction.getRemarks();
        accession.setRemarks(remarks);
        
        // ReturnCount
        int returnCount = transaction.getReturnCount();
        accession.setReturnCount((short) returnCount);
        
        // Text1 (local unit)
        String localUnit = transaction.getLocalUnit();
        accession.setText1(localUnit);
        
        // Text2 (purpose)
        String purpose = Transaction.toString(transaction.getPurpose());
        accession.setText2(purpose);
        
        // Text3 (geoUnit)
        String geoUnit = transaction.getGeoUnit();
        accession.setText3(geoUnit);
        
        // Type
        accession.setType(Transaction.toString(type));
        
        // TypeCount
        int typeCount = transaction.getTypeCount();
        accession.setTypeCount((short) typeCount);
        
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
        String fieldNames = "AccessionCondition, AccessionNumber, AltAccessionNumber, CreatedByAgentID, " +
                            "DateAccessioned, DiscardCount, DistributeCount, DivisionID, ItemCount, " +
                            "ModifiedByAgentID, NonSpecimenCount, Number1, Remarks, ReturnCount, Text1, " +
                            "Text2, Text3, Type, TypeCount, TimestampCreated, TimestampModified, Version, " +
                            "YesNo1, YesNo2";

        String[] values = new String[24];

        values[0]  = SqlUtils.sqlString( accession.getAccessionCondition());
        values[1]  = SqlUtils.sqlString( accession.getAccessionNumber());
        values[2]  = SqlUtils.sqlString( accession.getAltAccessionNumber());
        values[3]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[4]  = SqlUtils.sqlString( accession.getDateAccessioned());
        values[5]  = SqlUtils.sqlString( accession.getDiscardCount());
        values[6]  = SqlUtils.sqlString( accession.getDistributeCount());
        values[7]  = SqlUtils.sqlString( accession.getDivision().getId());
        values[8]  = SqlUtils.sqlString( accession.getItemCount());
        values[9]  = SqlUtils.sqlString( accession.getModifiedByAgent().getId());
        values[10] = SqlUtils.sqlString( accession.getNonSpecimenCount());
        values[11]  = SqlUtils.sqlString( accession.getNumber1());
        values[12] = SqlUtils.sqlString( accession.getRemarks());
        values[13] = SqlUtils.sqlString( accession.getReturnCount());
        values[14] = SqlUtils.sqlString( accession.getText1());
        values[15] = SqlUtils.sqlString( accession.getText2());
        values[16] = SqlUtils.sqlString( accession.getText3());
        values[17] = SqlUtils.sqlString( accession.getType());
        values[18] = SqlUtils.sqlString( accession.getTypeCount());
        values[19] = SqlUtils.sqlString( accession.getTimestampCreated());
        values[20] = SqlUtils.sqlString( accession.getTimestampModified());
        values[21] = SqlUtils.zero();
        values[22] = SqlUtils.sqlString( accession.getYesNo1());
        values[23] = SqlUtils.sqlString( accession.getYesNo1());
        
        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    protected String getInsertSql(AccessionAgent accessionAgent)
    {
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated, Version";

        String[] values = new String[5];

        values[0] = SqlUtils.sqlString( accessionAgent.getAccession().getId());
        values[1] = SqlUtils.sqlString( accessionAgent.getAgent().getId());
        values[2] = SqlUtils.sqlString( accessionAgent.getRole());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }
}
