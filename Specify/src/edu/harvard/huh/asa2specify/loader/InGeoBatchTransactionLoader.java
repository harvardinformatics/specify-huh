package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

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
    
    protected Accession getAccession(InGeoBatchTransaction inGift) throws LocalException
    {
        Accession accession = new Accession();

        // TODO: AddressOfRecord
        
        // CreatedByAgent
        Integer creatorOptrId = inGift.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        accession.setCreatedByAgent(createdByAgent);
        
        // AccessionCondition
        String description = getDescriptionOfMaterial(inGift);
        if (description != null) description = truncate(description, 255, "accession condition");
        accession.setAccessionCondition(description);
        
        // AccessionNumber
        String transactionNo = inGift.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_ACCESSION_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "invoice number");
        accession.setAccessionNumber(transactionNo);
        
        // DateAccessioned
        Date openDate = inGift.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
        // DiscardCount
        int discardCount = inGift.getDiscardCount();
        accession.setDiscardCount((short) discardCount);
        
        // DistributeCount
        int distributeCount = inGift.getDistributeCount();
        accession.setDistributeCount((short) distributeCount);
        
        // Division
        accession.setDivision(getBotanyDivision());
        
        // ItemCount
        int itemCount = inGift.getItemCount();
        accession.setItemCount((short) itemCount);
        
        // NonSpecimenCount
        int nonSpecimenCount = inGift.getNonSpecimenCount();
        accession.setNonSpecimenCount((short) nonSpecimenCount);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = inGift.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = inGift.getRemarks();
        accession.setRemarks(remarks);
        
        // ReturnCount
        int returnCount = inGift.getReturnCount();
        accession.setReturnCount((short) returnCount);
        
        // Text1 (local unit)
        String localUnit = inGift.getLocalUnit();
        accession.setText1(localUnit);
        
        // Text2 (purpose)
        String purpose = Transaction.toString(inGift.getPurpose());
        accession.setText2(purpose);
        
        // Text3 (geoUnit)
        String geoUnit = inGift.getGeoUnit();
        accession.setText3(geoUnit);
        
        // Type
        accession.setType(Transaction.toString(ACCESSION_TYPE.Gift));
        
        // TypeCount
        int typeCount = inGift.getTypeCount();
        accession.setTypeCount((short) typeCount);
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = inGift.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(inGift.getRequestType());
        accession.setYesNo2(isTheirs);
        
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
    
    protected String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionCondition, AccessionNumber, CreatedByAgentID, DateAccessioned, " +
                            "DiscardCount, DistributeCount, DivisionID, ItemCount, NonSpecimenCount, " +
                            "Number1, Remarks, ReturnCount, Text1, Text2, Text3, Type, TypeCount, " +
                            "TimestampCreated, Version, YesNo1, YesNo2";

        String[] values = new String[21];

        values[0]  = SqlUtils.sqlString( accession.getAccessionCondition());
        values[1]  = SqlUtils.sqlString( accession.getAccessionNumber());
        values[2]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[3]  = SqlUtils.sqlString( accession.getDateAccessioned());
        values[4]  = SqlUtils.sqlString( accession.getDiscardCount());
        values[5]  = SqlUtils.sqlString( accession.getDistributeCount());
        values[6]  = SqlUtils.sqlString( accession.getDivision().getId());
        values[7]  = SqlUtils.sqlString( accession.getItemCount());
        values[8]  = SqlUtils.sqlString( accession.getNonSpecimenCount());
        values[9]  = SqlUtils.sqlString( accession.getNumber1());
        values[10] = SqlUtils.sqlString( accession.getRemarks());
        values[11] = SqlUtils.sqlString( accession.getReturnCount());
        values[12] = SqlUtils.sqlString( accession.getText1());
        values[13] = SqlUtils.sqlString( accession.getText2());
        values[14] = SqlUtils.sqlString( accession.getText3());
        values[15] = SqlUtils.sqlString( accession.getType());
        values[16] = SqlUtils.sqlString( accession.getTypeCount());
        values[17] = SqlUtils.now();
        values[18] = SqlUtils.zero();
        values[19] = SqlUtils.sqlString( accession.getYesNo1());
        values[20] = SqlUtils.sqlString( accession.getYesNo1());
        
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
