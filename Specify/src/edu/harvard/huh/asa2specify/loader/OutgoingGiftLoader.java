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

import edu.harvard.huh.asa.OutgoingGift;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Deaccession;
import edu.ku.brc.specify.datamodel.DeaccessionAgent;

public class OutgoingGiftLoader extends TransactionLoader
{
    private static final Logger log  = Logger.getLogger(OutgoingGiftLoader.class);
    
    private static final String DEFAULT_DEACCESSION_NUMBER = "none";
    
    private OutgoingGiftLookup outGiftLookup;
    
    public OutgoingGiftLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        OutgoingGift outgoingGift = parse(columns);

        Integer transactionId = outgoingGift.getId();
        setCurrentRecordId(transactionId);
        
        Deaccession deaccession = getDeaccession(outgoingGift, ACCESSION_TYPE.Gift);
        
        String sql = getInsertSql(deaccession);
        Integer deaccessionId = insert(sql);
        deaccession.setDeaccessionId(deaccessionId);
        
        DeaccessionAgent donor = getDeaccessionAgent(outgoingGift, deaccession, ROLE.Donor);
        if (donor != null)
        {
            sql = getInsertSql(donor);
            insert(sql);
        }

        DeaccessionAgent receiver = getDeaccessionAgent(outgoingGift, deaccession, ROLE.Receiver);
        if (receiver != null)
        {
            sql = getInsertSql(receiver);
            insert(sql);
        }
    }
    
    public Logger getLogger()
    {
        return log;
    }
    
    public OutgoingGiftLookup getOutGoingGiftLookup()
    {
        if (outGiftLookup == null)
        {
            outGiftLookup = new OutgoingGiftLookup() {

                @Override
                public Deaccession getById(Integer transactionId) throws LocalException
                {
                    Deaccession deaccession = new Deaccession();
                    
                    Integer deaccessionId = getInt("deaccession", "DeaccessionID", "Number1", transactionId);
                    
                    deaccession.setDeaccessionId(deaccessionId);
                    
                    return deaccession;
                }  
            };
        }
        return outGiftLookup;
    }
    
    private OutgoingGift parse(String[] columns) throws LocalException
    {        
        OutgoingGift outgoingGift = new OutgoingGift();
        
        parse(columns, outgoingGift);

        return outgoingGift;
    }
    
    private Deaccession getDeaccession(OutgoingGift outGift, ACCESSION_TYPE type) throws LocalException
    {
        Deaccession deaccession = new Deaccession();
        
        // CreatedByAgentID
        Integer creatorOptrId = outGift.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        deaccession.setCreatedByAgent(createdByAgent);
        
        // DeaccessionDate
        Date openDate = outGift.getOpenDate();
        if (openDate != null)
        {
            deaccession.setDeaccessionDate(DateUtils.toCalendar(openDate));
        }
            
        // DeaccessionNumber
        String transactionNo = outGift.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_DEACCESSION_NUMBER;
        }
        
        transactionNo = truncate(transactionNo, 50, "loan number");    
        deaccession.setDeaccessionNumber(transactionNo);
            
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = outGift.getId();
        checkNull(transactionId, "transaction id");
        deaccession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = outGift.getRemarks();
        deaccession.setRemarks(remarks);
        
        // Text1 (description)
        String description = outGift.getDescription();
        deaccession.setText1(description);
        
        // Text2 (localUnit, forUseBy, userType, purpose)
        String usage = getUsageWithLocalUnit(outGift);
        deaccession.setText2(usage);
        
        // TimestampCreated
        Date dateCreated = outGift.getDateCreated();
        deaccession.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // Type
        deaccession.setType(type.name());

        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = outGift.isAcknowledged();
        deaccession.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(outGift.getRequestType());
        deaccession.setYesNo2(isTheirs);
        
        return deaccession;
    }

    private DeaccessionAgent getDeaccessionAgent(Transaction transaction, Deaccession deaccession, ROLE role)
        throws LocalException
    {
        DeaccessionAgent deaccessionAgent = new DeaccessionAgent();

        // Agent
        Agent agent = null;

        if (role.equals(ROLE.Donor))
        {
            agent = lookupAffiliate(transaction);
        }
        else if (role.equals(ROLE.Receiver))
        {
            agent = lookupAgent(transaction);
        }

        if (agent == null || agent.getId() == null) return null;

        deaccessionAgent.setAgent(agent);

        // Deaccession
        deaccessionAgent.setDeaccession(deaccession);

        // Remarks

        // Role
        deaccessionAgent.setRole(role.name());
        
        return deaccessionAgent;
    }

    private String getInsertSql(Deaccession deaccession)
    {
        String fieldNames = "CreatedByAgentID, DeaccessionDate, DeaccessionNumber, Number1, " +
                            "Remarks, Text1, Text2, Type, TimestampCreated, Version, YesNo1, YesNo2";

        String[] values = new String[12];

        values[0]  = SqlUtils.sqlString( deaccession.getCreatedByAgent().getId());
        values[1]  = SqlUtils.sqlString( deaccession.getDeaccessionDate());
        values[2]  = SqlUtils.sqlString( deaccession.getDeaccessionNumber());
        values[3]  = SqlUtils.sqlString( deaccession.getNumber1());
        values[4]  = SqlUtils.sqlString( deaccession.getRemarks());
        values[5]  = SqlUtils.sqlString( deaccession.getText1());
        values[6]  = SqlUtils.sqlString( deaccession.getText2());
        values[7]  = SqlUtils.sqlString( deaccession.getType());
        values[8]  = SqlUtils.sqlString( deaccession.getTimestampCreated());
        values[9]  = SqlUtils.one();
        values[10] = SqlUtils.sqlString( deaccession.getYesNo1());
        values[11] = SqlUtils.sqlString( deaccession.getYesNo2());
        
        return SqlUtils.getInsertSql("deaccession", fieldNames, values);
    }
    
    private String getInsertSql(DeaccessionAgent deaccessionAgent)
    {
        String fieldNames = "AgentID, DeaccessionID, Role, TimestampCreated, Version";

        String[] values = new String[5];

        values[0] = SqlUtils.sqlString( deaccessionAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( deaccessionAgent.getDeaccession().getId());
        values[2] = SqlUtils.sqlString( deaccessionAgent.getRole());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("deaccessionagent", fieldNames, values);
    } 
}
