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

import edu.harvard.huh.asa.OutgoingGift;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftAgent;

public class OutgoingGiftLoader extends TransactionLoader
{
    private static String DEFAULT_HERBARIUM = "A";
    
    private OutgoingGiftLookup outGiftLookup;
    
    public OutgoingGiftLoader(File csvFile,
                              Statement sqlStatement,
                              BotanistLookup botanistLookup,
                              File nameToBotanist) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        OutgoingGift outgoingGift = parse(columns);

        Integer transactionId = outgoingGift.getId();
        setCurrentRecordId(transactionId);
        
        Gift gift = getGift(outgoingGift);
        
        String sql = getInsertSql(gift);
        Integer giftId = insert(sql);
        gift.setGiftId(giftId);

        String herbariumCode = outgoingGift.getLocalUnit();
        if (herbariumCode == null)
        {
            getLogger().warn(rec() + " no herbarium code, using " + DEFAULT_HERBARIUM);
            herbariumCode = DEFAULT_HERBARIUM;
        }
        Integer collectionMemberId = getCollectionId(herbariumCode);

        Agent receiverAgent = lookupAgent(outgoingGift);
        if (receiverAgent != null)
        {
            GiftAgent receiver = getGiftAgent(receiverAgent, collectionMemberId, gift, ROLE.Receiver);

            sql = getInsertSql(receiver);
            insert(sql);
        }
    }
    
    public OutgoingGiftLookup getOutGoingGiftLookup()
    {
        if (outGiftLookup == null)
        {
            outGiftLookup = new OutgoingGiftLookup() {

                @Override
                public Gift getById(Integer transactionId) throws LocalException
                {
                    Gift gift = new Gift();
                    
                    Integer deaccessionId = getInt("gift", "GiftID", "Number1", transactionId);
                    
                    gift.setGiftId(deaccessionId);
                    
                    return gift;
                }  
            };
        }
        return outGiftLookup;
    }
    
    private OutgoingGift parse(String[] columns) throws LocalException
    {        
        OutgoingGift outgoingGift = new OutgoingGift();
        
        super.parse(columns, outgoingGift);
        
        return outgoingGift;
    }
    
    private Gift getGift(OutgoingGift outGift) throws LocalException
    {
        Gift gift = new Gift();
        
        // CreatedByAgentID
        Integer creatorOptrId = outGift.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        gift.setCreatedByAgent(createdByAgent);
        
        // DisciplineID
        gift.setDiscipline(getBotanyDiscipline());
        
        // GiftDate
        Date openDate = outGift.getOpenDate();
        if (openDate != null)
        {
            gift.setGiftDate(DateUtils.toCalendar(openDate));
        }
            
        // GiftNumber
        Integer transactionId = outGift.getId();
        checkNull(transactionId, "transaction id");
        gift.setGiftNumber(String.valueOf(transactionId));
        
        // Number1 (id) TODO: temporary!! remove when done!
        gift.setNumber1((float) transactionId);
        
        // PurposeOfGift
        String purpose = Transaction.toString(outGift.getPurpose());
        gift.setPurposeOfGift(purpose);
        
        // Remarks
        String remarks = outGift.getRemarks();
        gift.setRemarks(remarks);
        
        // SpecialConditions
        String specialConditions = getDescriptionOfMaterial(outGift);
        gift.setSpecialConditions(specialConditions);

        // Text1 (for use by)
        String forUseBy = outGift.getForUseBy();
        forUseBy = truncate(forUseBy, 300, "for use by");
        gift.setText1(forUseBy);
        
        // Text2  (local unit)
        String localUnit = outGift.getLocalUnit();
        gift.setText2(localUnit);
        
        // TimestampCreated
        Date dateCreated = outGift.getDateCreated();
        gift.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = outGift.isAcknowledged();
        gift.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(outGift.getRequestType());
        gift.setYesNo2(isTheirs);

        return gift;
    }
    
    private GiftAgent getGiftAgent(Agent agent, Integer collectionMemberId, Gift gift, ROLE role)
        throws LocalException
    {
        GiftAgent giftAgent = new GiftAgent();

        // Agent
        giftAgent.setAgent(agent);

        // Discipline
        giftAgent.setDiscipline(getBotanyDiscipline());
        
        // Gift
        giftAgent.setGift(gift);

        // Role
        giftAgent.setRole(Transaction.toString(role));
        
        return giftAgent;
    }

    private String getInsertSql(Gift gift)
    {
        String fieldNames = "CreatedByAgentID, DisciplineID, GiftDate, GiftNumber, Number1, " +
                            "PurposeOfGift, Remarks, Text1, Text2, TimestampCreated, Version, YesNo1";

        String[] values = new String[12];

        values[0]  = SqlUtils.sqlString( gift.getCreatedByAgent().getId());
        values[1]  = SqlUtils.sqlString( gift.getDiscipline().getId());
        values[2]  = SqlUtils.sqlString( gift.getGiftDate());
        values[3]  = SqlUtils.sqlString( gift.getGiftNumber());
        values[4]  = SqlUtils.sqlString( gift.getNumber1());
        values[5]  = SqlUtils.sqlString( gift.getPurposeOfGift());
        values[6]  = SqlUtils.sqlString( gift.getRemarks());
        values[7]  = SqlUtils.sqlString( gift.getText1());
        values[8]  = SqlUtils.sqlString( gift.getText2());
        values[9]  = SqlUtils.sqlString( gift.getTimestampCreated());
        values[10] = SqlUtils.zero();
        values[11] = SqlUtils.sqlString( gift.getYesNo1());
        
        return SqlUtils.getInsertSql("gift", fieldNames, values);
    }
    
    private String getInsertSql(GiftAgent giftAgent)
    {
        String fieldNames = "AgentID, DisciplineID, GiftID, Role, TimestampCreated, Version";

        String[] values = new String[6];

        values[0] = SqlUtils.sqlString( giftAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( giftAgent.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( giftAgent.getGift().getId());
        values[3] = SqlUtils.sqlString( giftAgent.getRole());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("giftagent", fieldNames, values);
    } 
}
