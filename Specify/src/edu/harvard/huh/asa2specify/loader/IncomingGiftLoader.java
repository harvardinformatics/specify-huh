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

import edu.harvard.huh.asa.IncomingGift;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.PURPOSE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AsaAgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftAgent;

public class IncomingGiftLoader extends TransactionLoader
{
    private static final String DEFAULT_GIFT_NUMBER = "none";
    
    public IncomingGiftLoader(File csvFile,
                              Statement sqlStatement,
                              File affiliateBotanists,
                              File agentBotanists,
                              BotanistLookup botanistLookup,
                              AsaAgentLookup agentLookup,
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
        IncomingGift incomingGift = parse(columns);

        Integer transactionId = incomingGift.getId();
        setCurrentRecordId(transactionId);
        
        String code = incomingGift.getLocalUnit();
        checkNull(code, "local unit");
        
        Integer collectionMemberId = getCollectionId(code);
        
        Gift gift = getGift(incomingGift);
        
        String sql = getInsertSql(gift);
        Integer giftId = insert(sql);
        gift.setGiftId(giftId);
        
        GiftAgent receiver = getGiftAgent(incomingGift, gift, ROLE.receiver, collectionMemberId);
        if (receiver != null)
        {
            sql = getInsertSql(receiver);
            insert(sql);
        }
        GiftAgent donor = getGiftAgent(incomingGift, gift, ROLE.donor, collectionMemberId);
        if (donor != null)
        {
            sql = getInsertSql(donor);
            insert(sql);
        }
    }
 
    private IncomingGift parse(String[] columns) throws LocalException
    {        
        IncomingGift incomingGift = new IncomingGift();
        
        parse(columns, incomingGift);

        return incomingGift;
    }
    
    private Gift getGift(Transaction transaction) throws LocalException
    {
        Gift gift = new Gift();

        // TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        gift.setCreatedByAgent(createdByAgent);
        
        // DisciplineID
        gift.setDiscipline(getBotanyDiscipline());
        
        // DivisionID
        gift.setDivision(getBotanyDivision());
        
        // GiftDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            gift.setGiftDate(DateUtils.toCalendar(openDate));
        }
        
        // GiftNumber
        String transactionNo = transaction.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_GIFT_NUMBER;
        }
        if (transactionNo.length() > 50)
        {
            warn("Truncating invoice number", transactionNo);
            transactionNo = transactionNo.substring(0, 50);
        }
        gift.setGiftNumber(transactionNo);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        gift.setNumber1((float) transactionId);
        
        // PurposeOfGift
        PURPOSE purpose = transaction.getPurpose();
        String purposeOfGift = Transaction.toString(purpose);
        gift.setPurposeOfGift(purposeOfGift);
        
        // Remarks
        String remarks = transaction.getRemarks();
        gift.setRemarks(remarks);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text1 (description)
        String description = transaction.getDescription();
        gift.setText1(description);
        
        // Text2
        String forUseBy = transaction.getForUseBy();
        gift.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        gift.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        gift.setYesNo1(isAcknowledged);
        
        return gift;
    }
    
    private GiftAgent getGiftAgent(Transaction transaction, Gift gift, ROLE role, Integer collectionMemberId) throws LocalException
    {
        GiftAgent giftAgent = new GiftAgent();
        
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.receiver))
        {
            agent = getAffiliateAgent(transaction);
        }
        else if (role.equals(ROLE.donor))
        {
            agent = getAsaAgentAgent(transaction);
        }
        
        if (agent.getId() == null) return null;
        
        giftAgent.setAgent(agent);
        
        // CollectionMemberID
        giftAgent.setCollectionMemberId(collectionMemberId);
        
        // GiftID
        giftAgent.setGift(gift);
        
        // Remarks
        
        // Role
        giftAgent.setRole(role.name());
        
        return giftAgent;
    }
    
    private String getInsertSql(Gift gift)
    {
        String fieldNames = "CreatedByAgentID, DisciplineID, DivisionID, GiftDate, GiftNumber, " +
                            "Number1, PurposeOfGift, Remarks, Text1, Text2, TimestampCreated, YesNo1";
        
        String[] values = new String[12];
        
        values[0]  = SqlUtils.sqlString( gift.getCreatedByAgent().getId());
        values[1]  = SqlUtils.sqlString( gift.getDiscipline().getId());
        values[2]  = SqlUtils.sqlString( gift.getDivision().getId());
        values[3]  = SqlUtils.sqlString( gift.getGiftDate());
        values[4]  = SqlUtils.sqlString( gift.getGiftNumber());
        values[5]  = SqlUtils.sqlString( gift.getNumber1());
        values[6]  = SqlUtils.sqlString( gift.getPurposeOfGift());
        values[7]  = SqlUtils.sqlString( gift.getRemarks());
        values[8]  = SqlUtils.sqlString( gift.getText1());
        values[9]  = SqlUtils.sqlString( gift.getText2());
        values[10] = SqlUtils.sqlString( gift.getTimestampCreated());
        values[11] = SqlUtils.sqlString( gift.getYesNo1());
        
        return SqlUtils.getInsertSql("gift", fieldNames, values);
    }
    
    private String getInsertSql(GiftAgent borrowAgent)
    {
        String fieldNames = "AgentID, CollectionMemberID, GiftID, Role, TimestampCreated";

        String[] values = new String[5];

        values[0] = SqlUtils.sqlString( borrowAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( borrowAgent.getCollectionMemberId());
        values[2] = SqlUtils.sqlString( borrowAgent.getGift().getId());
        values[3] = SqlUtils.sqlString( borrowAgent.getRole());
        values[4] = SqlUtils.now();

        return SqlUtils.getInsertSql("giftagent", fieldNames, values);
    }
}
