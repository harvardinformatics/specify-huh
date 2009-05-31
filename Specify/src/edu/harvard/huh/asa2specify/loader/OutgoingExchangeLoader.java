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
import java.text.MessageFormat;
import java.util.Date;

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
        
        int i = parse(columns, outExchange);
        
        if (columns.length < i + 6)
        {
            throw new LocalException("Not enough columns");
        }
        
        outExchange.setOriginalDueDate( SqlUtils.parseDate( columns[i + 0] ));
        outExchange.setCurrentDueDate(  SqlUtils.parseDate( columns[i + 1] ));
        outExchange.setGeoUnit(                             columns[i + 2] );
        outExchange.setItemCount(        SqlUtils.parseInt( columns[i + 3] ));
        outExchange.setTypeCount(        SqlUtils.parseInt( columns[i + 4] ));
        outExchange.setNonSpecimenCount( SqlUtils.parseInt( columns[i + 5] ));
        
        return outExchange;
    }
    
    private ExchangeOut getExchangeOut(OutgoingExchange outgoingExchange) throws LocalException
    {
        ExchangeOut exchangeOut = new ExchangeOut();

        // TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = outgoingExchange.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        exchangeOut.setCreatedByAgent(createdByAgent);
        
        // CatalogedByID
        Agent agentCatalogedBy = lookupAffiliate(outgoingExchange);
        exchangeOut.setAgentCatalogedBy(agentCatalogedBy);

        // DescriptionOfMaterial
        String descriptionOfMaterial = outgoingExchange.getDescription();
        if (descriptionOfMaterial != null)
        {
            descriptionOfMaterial = truncate(descriptionOfMaterial, 120, "description");
        	exchangeOut.setDescriptionOfMaterial(descriptionOfMaterial);
        }
        
        // DivisionID
        exchangeOut.setDivision(getBotanyDivision());
        
        // ExchangeDate
        Date openDate = outgoingExchange.getOpenDate();
        if (openDate != null)
        {
            exchangeOut.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = outgoingExchange.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeOut.setNumber1((float) transactionId);
        
        // QuantityExchanged
        short quantity = getQuantity(outgoingExchange);
        exchangeOut.setQuantityExchanged(quantity);
        
        // Remarks
        String remarks = outgoingExchange.getRemarks();
        exchangeOut.setRemarks(remarks);
                
        // SentToOrganization
        Agent agentSentTo = lookupAgent(outgoingExchange);
        exchangeOut.setAgentSentTo(agentSentTo);
        
        // SrcGeography
        String geoUnit = outgoingExchange.getGeoUnit();
        checkNull(geoUnit, "src geography");
        geoUnit = truncate(geoUnit, 32, "src geography");
        exchangeOut.setSrcGeography(geoUnit);
        
        // SrcTaxonomy

        // Text1 (item, type, non-specimen counts; original date due)
        String description = getDescription(outgoingExchange);
        if (description != null)
        {
            description = truncate(description, 120, "description");
        }
        exchangeOut.setText1(description);
        
        // Text2 (forUseBy)
        String forUseBy = outgoingExchange.getForUseBy();
        exchangeOut.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = outgoingExchange.getDateCreated();
        exchangeOut.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = outgoingExchange.isAcknowledged();
        exchangeOut.setYesNo1(isAcknowledged);
        
        return exchangeOut;
    }
 
    private short getQuantity(OutgoingExchange outgoingExchange)
    {
        Integer itemCount = outgoingExchange.getItemCount();
        Integer typeCount = outgoingExchange.getTypeCount();
        Integer nonSpecimenCount = outgoingExchange.getNonSpecimenCount();
        
        return (short) (itemCount + typeCount + nonSpecimenCount);
    }
    
    private String getDescription(OutgoingExchange outgoingExchange)
    {
        Integer itemCount = outgoingExchange.getItemCount();
        Integer typeCount = outgoingExchange.getTypeCount();
        Integer nonSpecimenCount = outgoingExchange.getNonSpecimenCount();

        Date originalDateDue = outgoingExchange.getOriginalDueDate();
        
        Object[] args = { itemCount, typeCount, nonSpecimenCount, DateUtils.toString(originalDateDue) };
        String pattern = "{0} items, {1} types, {2} non-specimens{3}";

        return MessageFormat.format(pattern, args);
    }
    
    private String getInsertSql(ExchangeOut exchangeOut)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, " +
                            "ExchangeDate, Number1, QuantityExchanged, Remarks, SentToOrganizationID, " +
                            "SrcGeography, Text1, Text2, TimestampCreated, YesNo1";

        String[] values = new String[14];

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
        values[13] = SqlUtils.sqlString( exchangeOut.getYesNo1());
        
        return SqlUtils.getInsertSql("exchangeout", fieldNames, values);
    }
}
