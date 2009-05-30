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

import edu.harvard.huh.asa.OutgoingExchange;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeOut;

public class OutgoingExchangeLoader extends TransactionLoader
{
    private OutgoingExchangeLookup outExchangeLookup;
    
    public OutgoingExchangeLoader(File csvFile,
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
        OutgoingExchange outgoingExchange = parse(columns);

        Integer transactionId = outgoingExchange.getId();
        setCurrentRecordId(transactionId);
        
        ExchangeOut exchangeOut = getExchangeOut(outgoingExchange);
        
        String sql = getInsertSql(exchangeOut);
        insert(sql);
    }
    
    public OutgoingExchangeLookup getOutgoingExchangeLookup()
    {
        if (outExchangeLookup == null)
        {
            outExchangeLookup = new OutgoingExchangeLookup()
            {
                @Override
                public ExchangeOut getById(Integer transactionId) throws LocalException
                {
                    ExchangeOut exchangeOut = new ExchangeOut();
                    
                    Integer exchangeOutId = getInt("exchangeout", "ExchangeOutID", "Number1", transactionId);
                    
                    exchangeOut.setExchangeOutId(exchangeOutId);
                    
                    return exchangeOut;
                }   
            };
        }
        return outExchangeLookup;
    }

    private OutgoingExchange parse(String[] columns) throws LocalException
    {        
        OutgoingExchange outExchange = new OutgoingExchange();
        
        parse(columns, outExchange);
        
        return outExchange;
    }
    
    private ExchangeOut getExchangeOut(Transaction transaction) throws LocalException
    {
        ExchangeOut exchangeOut = new ExchangeOut();

        // TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        exchangeOut.setCreatedByAgent(createdByAgent);
        
        // CatalogedByID
        Agent agentCatalogedBy = getAffiliateAgent(transaction);
        exchangeOut.setAgentCatalogedBy(agentCatalogedBy);

        // DescriptionOfMaterial
        String description = transaction.getDescription();
        if (description != null)
        {
        	description = truncate(description, 120, "description");
        	exchangeOut.setDescriptionOfMaterial(description);
        }
        
        // DivisionID
        exchangeOut.setDivision(getBotanyDivision());
        
        // ExchangeDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            exchangeOut.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeOut.setNumber1((float) transactionId);
        
        // QuantityExchanged
        
        // Remarks
        String remarks = transaction.getRemarks();
        exchangeOut.setRemarks(remarks);
                
        // SentToOrganization
        Agent agentSentTo = getAsaAgentAgent(transaction);
        exchangeOut.setAgentSentTo(agentSentTo);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text1 (description)
        
        // Text2 (forUseBy)
        String forUseBy = transaction.getForUseBy();
        exchangeOut.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        exchangeOut.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        exchangeOut.setYesNo1(isAcknowledged);
        
        return exchangeOut;
    }
 
    private String getInsertSql(ExchangeOut exchangeOut)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, " +
                            "ExchangeDate, Number1, Remarks, SentToOrganizationID, Text2, " +
                            "TimestampCreated, YesNo1";

        String[] values = new String[11];

        values[0]  = SqlUtils.sqlString( exchangeOut.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( exchangeOut.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( exchangeOut.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( exchangeOut.getDivision().getId());
        values[4]  = SqlUtils.sqlString( exchangeOut.getExchangeDate());
        values[5]  = SqlUtils.sqlString( exchangeOut.getNumber1());
        values[6]  = SqlUtils.sqlString( exchangeOut.getRemarks());
        values[7]  = SqlUtils.sqlString( exchangeOut.getAgentSentTo().getId());
        values[8]  = SqlUtils.sqlString( exchangeOut.getText2());
        values[9]  = SqlUtils.sqlString( exchangeOut.getTimestampCreated());
        values[10] = SqlUtils.sqlString( exchangeOut.getYesNo1());
        
        return SqlUtils.getInsertSql("exchangeout", fieldNames, values);
    }
}
