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

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Purchase;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.ACCESSION_TYPE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class PurchaseLoader extends InGeoBatchTransactionLoader
{
    private static final Logger log  = Logger.getLogger(PurchaseLoader.class);
    
    public PurchaseLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        Purchase purchase = parse(columns);

        Integer transactionId = purchase.getId();
        setCurrentRecordId(transactionId);
        
        Accession accession = getAccession(purchase);
        //Accession accession = getById(purchase.getId());
        
        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        Agent receiverAgent = lookupAffiliate(purchase);
        
        if (receiverAgent != null)
        {
            AccessionAgent preparer = getAccessionAgent(receiverAgent, accession, ROLE.Receiver);
            sql = getInsertSql(preparer);
            insert(sql);
        }
        
        Agent contributorAgent = lookupAgent(purchase);
        if (contributorAgent != null)
        {
            AccessionAgent contributor = getAccessionAgent(contributorAgent, accession, ROLE.Contributor);
            sql = getInsertSql(contributor);
            insert(sql);
        }
    }

    private Accession getById(Integer transactionId) throws LocalException
    {
        Accession accession = new Accession();
        
        Integer accessionId = getInt("accession", "AccessionID", "Number1", transactionId);
        
        accession.setAccessionId(accessionId);
        
        return accession;
    } 
    
    public Logger getLogger()
    {
        return log;
    }
    
    private Accession getAccession(Purchase purchase) throws LocalException
    {
        Accession accession = new Accession();
        
        // TODO: AddressOfRecord
        
        // CreatedByAgent
        Integer creatorOptrId = purchase.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        accession.setCreatedByAgent(createdByAgent);
        
        // AccesionCondition
        String description = purchase.getDescription();
        if (description != null) description = truncate(description, 255, "accession condition");
        accession.setAccessionCondition(description);
        
        // AccessionNumber
        String transactionNo = purchase.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_ACCESSION_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "invoice number");
        accession.setAccessionNumber(transactionNo);
        
        // DateAccessioned
        Date openDate = purchase.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
        // Division
        accession.setDivision(getBotanyDivision());
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = purchase.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = purchase.getRemarks();
        accession.setRemarks(remarks);
        
        // Text1 (itemCount, typeCount, nonSpecimenCount)
        String itemCountNote = purchase.getItemCountNote();
        accession.setText1(itemCountNote);
        
        // Text2 (purpose)
        String purpose = Transaction.toString(purchase.getPurpose());
        accession.setText2(purpose);
        
        // Text3 (geoUnit)
        String geoUnit = purchase.getGeoUnit();
        accession.setText3(geoUnit);
        
        // Type
        accession.setType(Transaction.toString(ACCESSION_TYPE.Purchase));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = purchase.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(purchase.getRequestType());
        accession.setYesNo2(isTheirs);
        
        return accession;
    }
    
    private Purchase parse(String[] columns) throws LocalException
    {        
        Purchase purchase = new Purchase();
        
        super.parse(columns, purchase);

        return purchase;
    }
    
    private AccessionAgent getAccessionAgent(Agent agent, Accession accession, ROLE role)
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
    
    private String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionCondition, AccessionNumber, CreatedByAgentID, DateAccessioned, " +
                            "DivisionID, Number1, Remarks, Text1, Text2, Text3, Type, TimestampCreated, " +
                            "Version, YesNo1, YesNo2";

        String[] values = new String[15];

        values[0]  = SqlUtils.sqlString( accession.getAccessionCondition());
        values[1]  = SqlUtils.sqlString( accession.getAccessionNumber());
        values[2]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[3]  = SqlUtils.sqlString( accession.getDateAccessioned());
        values[4]  = SqlUtils.sqlString( accession.getDivision().getId());
        values[5]  = SqlUtils.sqlString( accession.getNumber1());
        values[6]  = SqlUtils.sqlString( accession.getRemarks());
        values[7]  = SqlUtils.sqlString( accession.getText1());
        values[8]  = SqlUtils.sqlString( accession.getText2());
        values[9]  = SqlUtils.sqlString( accession.getText3());
        values[10] = SqlUtils.sqlString( accession.getType());
        values[11] = SqlUtils.now();
        values[12] = SqlUtils.zero();
        values[13] = SqlUtils.sqlString( accession.getYesNo1());
        values[14] = SqlUtils.sqlString( accession.getYesNo2());
        
        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    private String getInsertSql(AccessionAgent accessionAgent)
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
