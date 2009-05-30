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

import edu.harvard.huh.asa.IncomingExchange;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeIn;

public class IncomingExchangeLoader extends TransactionLoader
{
    public IncomingExchangeLoader(File csvFile,
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
        IncomingExchange incomingExchange = parse(columns);

        Integer transactionId = incomingExchange.getId();
        setCurrentRecordId(transactionId);
        
        ExchangeIn exchangeIn = getExchangeIn(incomingExchange);
        
        String sql = getInsertSql(exchangeIn);
        insert(sql);
    }
    
    private IncomingExchange parse(String[] columns) throws LocalException
    {        
        IncomingExchange inExchange = new IncomingExchange();
        
        parse(columns, inExchange);
        
        return inExchange;
    }
    
    private ExchangeIn getExchangeIn(Transaction transaction) throws LocalException
    {
        ExchangeIn exchangeIn = new ExchangeIn();

        // TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        exchangeIn.setCreatedByAgent(createdByAgent);
        
        // CatalogedByID ("for use by")
        Agent agentCatalogedBy = getAffiliateAgent(transaction);
        exchangeIn.setAgentCatalogedBy(agentCatalogedBy);

        // DivisionID
        exchangeIn.setDivision(getBotanyDivision());
        
        // DescriptionOfMaterial
        String description = transaction.getDescription();
        if (description != null)
        {
        	description = truncate(description, 120, "description");
        	exchangeIn.setDescriptionOfMaterial(description);
        }
        
        // ExchangeDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            exchangeIn.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeIn.setNumber1((float) transactionId);
        
        // QuantityExchanged
        
        // ReceivedFromOrganization ("contact")
        Agent agentReceivedFrom = getAsaAgentAgent(transaction);
        exchangeIn.setAgentReceivedFrom(agentReceivedFrom);
        
        // Remarks
        String remarks = transaction.getRemarks();
        exchangeIn.setRemarks(remarks);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text1 (description)
        
        // Text2 (forUseBy)
        String forUseBy = transaction.getForUseBy();
        exchangeIn.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        exchangeIn.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        exchangeIn.setYesNo1(isAcknowledged);
        
        return exchangeIn;
    }
    
    private String getInsertSql(ExchangeIn exchangeIn)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, ExchangeDate, " +
                            "Number1, ReceivedFromOrganizationID, Remarks, Text2, TimestampCreated, YesNo1";
        
        String[] values = new String[11];
        
        values[0]  = SqlUtils.sqlString( exchangeIn.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( exchangeIn.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( exchangeIn.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( exchangeIn.getDivision().getId());
        values[4]  = SqlUtils.sqlString( exchangeIn.getExchangeDate());
        values[5]  = SqlUtils.sqlString( exchangeIn.getNumber1());
        values[6]  = SqlUtils.sqlString( exchangeIn.getAgentReceivedFrom().getId());
        values[7]  = SqlUtils.sqlString( exchangeIn.getRemarks());
        values[8]  = SqlUtils.sqlString( exchangeIn.getText2());
        values[9]  = SqlUtils.sqlString( exchangeIn.getTimestampCreated());
        values[10] = SqlUtils.sqlString( exchangeIn.getYesNo1());
        
        return SqlUtils.getInsertSql("exchangein", fieldNames, values);
    }
}
