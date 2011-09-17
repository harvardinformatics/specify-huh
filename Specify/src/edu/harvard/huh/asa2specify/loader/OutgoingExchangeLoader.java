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

import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.OutgoingExchange;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeOut;

public class OutgoingExchangeLoader extends TransactionLoader
{
    private OutgoingExchangeLookup outExchangeLookup;
    
    public OutgoingExchangeLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
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
                    
                    Integer exchangeOutId = getId("exchangeout", "ExchangeOutID", "Number1", String.valueOf(transactionId));
                    
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
        
        super.parse(columns, outExchange);
        
        return outExchange;
    }
    
    private ExchangeOut getExchangeOut(OutgoingExchange outExchange) throws LocalException
    {
        ExchangeOut exchangeOut = new ExchangeOut();

        // TODO: AddressOfRecord
        
        setAuditFields(outExchange, exchangeOut);
        
        // CatalogedByID
        Agent createdByAgent = exchangeOut.getCreatedByAgent();
        exchangeOut.setAgentCatalogedBy(createdByAgent);
        
        // DescriptionOfMaterial (description + [boxCount])
        String description = getDescriptionOfMaterial(outExchange);
        if (description != null) description = truncate(description, 255, "description");
        exchangeOut.setDescriptionOfMaterial(description);
        
        // DivisionID
        exchangeOut.setDivision(getBotanyDivision());
        
        // ExchangeDate
        Date openDate = outExchange.getOpenDate();
        if (openDate != null)
        {
            exchangeOut.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id)
        Integer transactionId = outExchange.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeOut.setNumber1((float) (transactionId + 0.0));
        
        // QuantityExchanged
        
        // Remarks
        String remarks = outExchange.getRemarks();
        String purpose = denormalize("purpose", outExchange.getPurpose().name());
        
        exchangeOut.setRemarks(concatenate(remarks, purpose));

        // SentToOrganization
        int organizationId = outExchange.getOrganizationId();
        boolean isSelfOrganized = Organization.IsSelfOrganizing(organizationId);
        
        Agent agentSentTo = isSelfOrganized ? lookupAgent(outExchange) : lookupOrganization(outExchange);
        
        checkNull(agentSentTo, "recipient");
        exchangeOut.setAgentSentTo(agentSentTo);
        
        // Text1 (for use by)
        String forUseBy = outExchange.getForUseBy();
        exchangeOut.setText1(forUseBy);
        
        // Text2 (local unit)
        String localUnit = outExchange.getLocalUnit();
        exchangeOut.setText2(localUnit);
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = outExchange.isAcknowledged();
        exchangeOut.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(outExchange.getRequestType());
        exchangeOut.setYesNo2(isTheirs);
        
        return exchangeOut;
    }
      
    private String getInsertSql(ExchangeOut exchangeOut)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, " +
                            "ExchangeDate, ModifiedByAgentID, Number1, Remarks, SentToOrganizationID, " +
                            "Text1, Text2, TimestampCreated, TimestampModified, Version, " +
                            "YesNo1, YesNo2";

        String[] values = {
        		SqlUtils.sqlString( exchangeOut.getAgentCatalogedBy().getId()),
        		SqlUtils.sqlString( exchangeOut.getCreatedByAgent().getId()),
        		SqlUtils.sqlString( exchangeOut.getDescriptionOfMaterial()),
        		SqlUtils.sqlString( exchangeOut.getDivision().getId()),
        		SqlUtils.sqlString( exchangeOut.getExchangeDate()),
        		SqlUtils.sqlString( exchangeOut.getModifiedByAgent().getId()),
        		SqlUtils.sqlString( exchangeOut.getNumber1()),
        		SqlUtils.sqlString( exchangeOut.getRemarks()),
        		SqlUtils.sqlString( exchangeOut.getAgentSentTo().getId()),
        		SqlUtils.sqlString( exchangeOut.getText1()),
        		SqlUtils.sqlString( exchangeOut.getText2()),
        		SqlUtils.sqlString( exchangeOut.getTimestampCreated()),
        		SqlUtils.sqlString( exchangeOut.getTimestampModified()),
        		SqlUtils.one(),
        		SqlUtils.sqlString( exchangeOut.getYesNo1()),
        		SqlUtils.sqlString( exchangeOut.getYesNo2())
        };
        
        return SqlUtils.getInsertSql("exchangeout", fieldNames, values);
    }
}
