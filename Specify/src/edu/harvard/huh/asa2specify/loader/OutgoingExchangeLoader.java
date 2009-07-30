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

import edu.harvard.huh.asa.OutgoingExchange;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeOut;

public class OutgoingExchangeLoader extends OutGeoBatchTransactionLoader
{
    private static final Logger log  = Logger.getLogger(OutgoingExchangeLoader.class);
            
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
    
    public Logger getLogger()
    {
        return log;
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
        
        int i = super.parse(columns, outExchange);
        
        if (columns.length < i + 1)
        {
            throw new LocalException("Not enough columns");
        }
        
        outExchange.setGeoUnit( columns[i + 0] );
        
        return outExchange;
    }
    
    private ExchangeOut getExchangeOut(OutgoingExchange outExchange) throws LocalException
    {
        ExchangeOut exchangeOut = new ExchangeOut();

        TYPE type = outExchange.getType();

        // TODO: AddressOfRecord
        
        // CatalogedByID
        Integer creatorOptrId = outExchange.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);

        exchangeOut.setAgentCatalogedBy(createdByAgent);
        
        // CreatedByAgentID
        exchangeOut.setCreatedByAgent(createdByAgent);
        
        // DescriptionOfMaterial
        String descriptionOfMaterial = outExchange.getDescription();
        if (descriptionOfMaterial != null)
        {
            descriptionOfMaterial = truncate(descriptionOfMaterial, 120, "description");
        	exchangeOut.setDescriptionOfMaterial(descriptionOfMaterial);
        }
        
        // DivisionID
        exchangeOut.setDivision(getBotanyDivision());
        
        // ExchangeDate
        Date openDate = outExchange.getOpenDate();
        if (openDate != null)
        {
            exchangeOut.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = outExchange.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeOut.setNumber1((float) transactionId);
        
        // QuantityExchanged
        short quantity = outExchange.getBatchQuantity();
        exchangeOut.setQuantityExchanged(quantity);
        
        // Remarks
        String remarks = outExchange.getRemarks();
        exchangeOut.setRemarks(remarks);
                
        // SentToOrganization
        Agent agentSentTo = lookupAgent(outExchange);
        checkNull(agentSentTo, "recipient");
        exchangeOut.setAgentSentTo(agentSentTo);
        
        // SrcGeography
        if (type.equals(TYPE.OutExchange))
        {
            String geoUnit = outExchange.getGeoUnit();
            checkNull(geoUnit, "src geography");
            geoUnit = truncate(geoUnit, 32, "src geography");
            exchangeOut.setSrcGeography(geoUnit);
        }
        
        // SrcTaxonomy

        // Text1 (boxCount, typeCount, nonSpecimenCount)
        String description = outExchange.getItemCountNote();
        exchangeOut.setText1(description);
        
        // Text2 (localUnit, agent)
        String localUnit = outExchange.getLocalUnit();
        String agentName = outExchange.getForUseBy();
        
        String text2 = localUnit + (agentName == null ? "" : ".  For use by " + agentName);
 
        exchangeOut.setText2(text2);
        
        // TimestampCreated
        Date dateCreated = outExchange.getDateCreated();
        exchangeOut.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
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
                            "ExchangeDate, Number1, QuantityExchanged, Remarks, SentToOrganizationID, " +
                            "SrcGeography, Text1, Text2, TimestampCreated, Version, YesNo1, YesNo2";

        String[] values = new String[16];

        values[0]  = SqlUtils.sqlString( exchangeOut.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( exchangeOut.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( exchangeOut.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( exchangeOut.getDivision().getId());
        values[4]  = SqlUtils.sqlString( exchangeOut.getExchangeDate());
        values[5]  = SqlUtils.sqlString( exchangeOut.getNumber1());
        values[6]  = SqlUtils.sqlString( exchangeOut.getQuantityExchanged());
        values[7]  = SqlUtils.sqlString( exchangeOut.getRemarks());
        values[8]  = SqlUtils.sqlString( exchangeOut.getAgentSentTo().getId());
        values[9]  = SqlUtils.sqlString( exchangeOut.getSrcGeography());
        values[10] = SqlUtils.sqlString( exchangeOut.getText1());
        values[11] = SqlUtils.sqlString( exchangeOut.getText2());
        values[12] = SqlUtils.sqlString( exchangeOut.getTimestampCreated());
        values[13] = SqlUtils.zero();
        values[14] = SqlUtils.sqlString( exchangeOut.getYesNo1());
        values[15] = SqlUtils.sqlString( exchangeOut.getYesNo2());
        
        return SqlUtils.getInsertSql("exchangeout", fieldNames, values);
    }
}
