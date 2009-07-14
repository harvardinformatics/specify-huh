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

import edu.harvard.huh.asa.IncomingGift;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.PURPOSE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.IncomingGiftLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftAgent;

public class IncomingGiftLoader extends TransactionLoader
{
    private static final Logger log  = Logger.getLogger(IncomingGiftLoader.class);
    
    private static final String DEFAULT_GIFT_NUMBER = "none";
    
    private IncomingGiftLookup inGiftLookup;
    
    public IncomingGiftLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
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
        
        GiftAgent receiver = getGiftAgent(incomingGift, gift, ROLE.Receiver, collectionMemberId);
        if (receiver != null)
        {
            sql = getInsertSql(receiver);
            insert(sql);
        }
        GiftAgent donor = getGiftAgent(incomingGift, gift, ROLE.Donor, collectionMemberId);
        if (donor != null)
        {
            sql = getInsertSql(donor);
            insert(sql);
        }
    }
 
    public Logger getLogger()
    {
        return log;
    }
    
    public IncomingGiftLookup getIncomingGiftLookup()
    {
        if (inGiftLookup == null)
        {
            inGiftLookup = new IncomingGiftLookup() {

                public Gift getById(Integer transactionId) throws LocalException
                {
                    Gift gift = new Gift();
                    
                    Integer giftId = getInt("gift", "GiftID", "Number1", transactionId);
                    
                    gift.setGiftId(giftId);
                    
                    return gift;
                }
                
            };
        }
        return inGiftLookup;
    }

    private IncomingGift parse(String[] columns) throws LocalException
    {        
        IncomingGift incomingGift = new IncomingGift();
        
        parse(columns, incomingGift);

        return incomingGift;
    }
    
    private Gift getGift(IncomingGift inGift) throws LocalException
    {
        Gift gift = new Gift();

        // TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = inGift.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        gift.setCreatedByAgent(createdByAgent);
        
        // DisciplineID
        gift.setDiscipline(getBotanyDiscipline());
        
        // DivisionID
        gift.setDivision(getBotanyDivision());
        
        // GiftDate
        Date openDate = inGift.getOpenDate();
        if (openDate != null)
        {
            gift.setGiftDate(DateUtils.toCalendar(openDate));
        }
        
        // GiftNumber
        String transactionNo = inGift.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_GIFT_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "transaction number");
        gift.setGiftNumber(transactionNo);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = inGift.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        gift.setNumber1((float) transactionId);
        
        // PurposeOfGift
        PURPOSE purpose = inGift.getPurpose();
        String purposeOfGift = Transaction.toString(purpose);
        purposeOfGift = truncate(purposeOfGift, 64, "purpose");
        gift.setPurposeOfGift(purposeOfGift);
        
        // Remarks
        String remarks = inGift.getRemarks();
        gift.setRemarks(remarks);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text1 (description)
        String description = inGift.getDescription();
        gift.setText1(description);
        
        // Text2
        String forUseBy = inGift.getForUseBy();
        gift.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = inGift.getDateCreated();
        gift.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = inGift.isAcknowledged();
        gift.setYesNo1(isAcknowledged);
        
        return gift;
    }
    
    private GiftAgent getGiftAgent(Transaction transaction, Gift gift, ROLE role, Integer collectionMemberId) throws LocalException
    {
        GiftAgent giftAgent = new GiftAgent();
        
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.Receiver))
        {
            agent = lookupAffiliate(transaction);
        }
        else if (role.equals(ROLE.Donor))
        {
            agent = lookupAgent(transaction);
        }
        
        if (agent == null || agent.getId() == null) return null;
        
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
                            "Number1, PurposeOfGift, Remarks, Text1, Text2, TimestampCreated, " +
                            "Version, YesNo1";
        
        String[] values = new String[13];
        
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
        values[11] = SqlUtils.one();
        values[12] = SqlUtils.sqlString( gift.getYesNo1());
        
        return SqlUtils.getInsertSql("gift", fieldNames, values);
    }
    
    private String getInsertSql(GiftAgent borrowAgent)
    {
        String fieldNames = "AgentID, CollectionMemberID, GiftID, Role, TimestampCreated, Version";

        String[] values = new String[6];

        values[0] = SqlUtils.sqlString( borrowAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( borrowAgent.getCollectionMemberId());
        values[2] = SqlUtils.sqlString( borrowAgent.getGift().getId());
        values[3] = SqlUtils.sqlString( borrowAgent.getRole());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("giftagent", fieldNames, values);
    }
}
