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

import edu.harvard.huh.asa.Purchase;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class PurchaseLoader extends TransactionLoader
{
    public PurchaseLoader(File csvFile,
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
   }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        Purchase purchase = parse(columns);

        Integer transactionId = purchase.getId();
        setCurrentRecordId(transactionId);
        
        Accession accession = getAccession(purchase, ACCESSION_TYPE.purchase);
        
        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        AccessionAgent preparer = getAccessionAgent(purchase, accession, ROLE.preparer);
        if (preparer != null)
        {
            sql = getInsertSql(preparer);
            insert(sql);
        }
        
        AccessionAgent contributor = getAccessionAgent(purchase, accession, ROLE.contributor);
        if (contributor != null)
        {
            sql = getInsertSql(contributor);
            insert(sql);
        }
    }

    private Accession getAccession(Transaction transaction, ACCESSION_TYPE type) throws LocalException
    {
        Accession accession = new Accession();
        
        // TODO: AddressOfRecord
        
        // CreatedByAgent
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        accession.setCreatedByAgent(createdByAgent);
        
        // AccessionNumber
        String transactionNo = transaction.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_ACCESSION_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "invoice number");
        accession.setAccessionNumber(transactionNo);
        
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
        String remarks = accession.getRemarks();
        accession.setRemarks(remarks);
        
        // Text1 (description)
        String description = transaction.getDescription();
        accession.setText1(description);
        
        // Text2 (forUseBy)
        String forUseBy = transaction.getForUseBy();
        accession.setText2(forUseBy);
        
        // Text3 (boxCount)
        String boxCount = transaction.getBoxCount();
        accession.setText3(boxCount);
        
        // Type
        accession.setType(type.name());
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        return accession;
    }
    
    private Purchase parse(String[] columns) throws LocalException
    {        
        Purchase purchase = new Purchase();
        
        parse(columns, purchase);

        return purchase;
    }
    
    private AccessionAgent getAccessionAgent(Transaction transaction, Accession accession, ROLE role)
        throws LocalException
    {
           AccessionAgent accessionAgent = new AccessionAgent();

            // Agent
            Agent agent = null;

            if (role.equals(ROLE.preparer) || role.equals(ROLE.collector))
            {
                agent = getAffiliateAgent(transaction);
            }
            else if (role.equals(ROLE.contributor))
            {
                agent = getAsaAgentAgent(transaction);
            }

            if (agent.getId() == null) return null;

            accessionAgent.setAgent(agent);

            // Deaccession
            accessionAgent.setAccession(accession);

            // Remarks

            // Role
            accessionAgent.setRole(role.name());
            
            return accessionAgent;
    }
    
    private String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionNumber, CreatedByAgentID, DateAccessioned, Number1, " +
                            "Remarks, Text1, Text2, Text3, Type, TimestampCreated, YesNo1";

        String[] values = new String[11];

        values[0]  = SqlUtils.sqlString( accession.getAccessionNumber());
        values[1]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( accession.getDateAccessioned());
        values[3]  = SqlUtils.sqlString( accession.getNumber1());
        values[4]  = SqlUtils.sqlString( accession.getRemarks());
        values[5]  = SqlUtils.sqlString( accession.getText1());
        values[6]  = SqlUtils.sqlString( accession.getText2());
        values[7]  = SqlUtils.sqlString( accession.getText3());
        values[8]  = SqlUtils.sqlString( accession.getType());
        values[9]  = SqlUtils.sqlString( accession.getTimestampCreated());
        values[10] = SqlUtils.sqlString( accession.getYesNo1());

        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    private String getInsertSql(AccessionAgent accessionAgent)
    {
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated";

        String[] values = new String[4];

        values[0] = SqlUtils.sqlString( accessionAgent.getAccession().getId());
        values[1] = SqlUtils.sqlString( accessionAgent.getAgent().getId());
        values[2] = SqlUtils.sqlString( accessionAgent.getRole());
        values[3] = SqlUtils.now();

        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }
}
