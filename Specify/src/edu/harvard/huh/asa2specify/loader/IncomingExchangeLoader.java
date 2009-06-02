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

import edu.harvard.huh.asa.IncomingExchange;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeIn;

public class IncomingExchangeLoader extends TransactionLoader
{
    public IncomingExchangeLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
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
        
        int i = parse(columns, inExchange);
        
        if (columns.length < i + 10)
        {
            throw new LocalException("Not enough columns");
        }
        
        inExchange.setOriginalDueDate( SqlUtils.parseDate( columns[i + 0] ));
        inExchange.setCurrentDueDate(  SqlUtils.parseDate( columns[i + 1] ));
        inExchange.setGeoUnit(                             columns[i + 2] );
        inExchange.setItemCount(        SqlUtils.parseInt( columns[i + 3] ));
        inExchange.setTypeCount(        SqlUtils.parseInt( columns[i + 4] ));
        inExchange.setNonSpecimenCount( SqlUtils.parseInt( columns[i + 5] ));
        inExchange.setDiscardCount(     SqlUtils.parseInt( columns[i + 6] ));
        inExchange.setDistributeCount(  SqlUtils.parseInt( columns[i + 7] ));
        inExchange.setReturnCount(      SqlUtils.parseInt( columns[i + 8] ));
        inExchange.setCost(           SqlUtils.parseFloat( columns[i + 9] ));
        
        return inExchange;
    }
    
    private ExchangeIn getExchangeIn(IncomingExchange inExchange) throws LocalException
    {
        ExchangeIn exchangeIn = new ExchangeIn();

        // TODO: AddressOfRecord
        
        // CatalogedByID
        Integer creatorOptrId = inExchange.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);

        exchangeIn.setAgentCatalogedBy(createdByAgent);
        
        // CreatedByAgentID
        exchangeIn.setCreatedByAgent(createdByAgent);
        
        // DescriptionOfMaterial
        String descriptionOfMaterial = inExchange.getDescription();
        if (descriptionOfMaterial != null)
        {
            descriptionOfMaterial = truncate(descriptionOfMaterial, 120, "description");
            exchangeIn.setDescriptionOfMaterial(descriptionOfMaterial);
        }
        
        // DivisionID
        exchangeIn.setDivision(getBotanyDivision());
        
        // ExchangeDate
        Date openDate = inExchange.getOpenDate();
        if (openDate != null)
        {
            exchangeIn.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = inExchange.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeIn.setNumber1((float) transactionId);
        
        // QuantityExchanged
        short quantity = getQuantity(inExchange);
        exchangeIn.setQuantityExchanged(quantity);
        
        // ReceivedFromOrganization ("contact")
        Agent agentReceivedFrom = lookupAgent(inExchange);
        checkNull(agentReceivedFrom, "recipient");
        exchangeIn.setAgentReceivedFrom(agentReceivedFrom);
        
        // Remarks
        String remarks = inExchange.getRemarks();
        exchangeIn.setRemarks(remarks);
        
        // SrcGeography
        String geoUnit = inExchange.getGeoUnit();
        checkNull(geoUnit, "geo unit");
        geoUnit = truncate(geoUnit, 32, "geo unit");
        exchangeIn.setSrcGeography(geoUnit);
        
        // SrcTaxonomy
        
        // Text1 (description)
        String description = getDescription(inExchange);
        exchangeIn.setText1(description);
        
        // Text2 (forUseBy)
        String forUseBy = inExchange.getForUseBy();
        exchangeIn.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = inExchange.getDateCreated();
        exchangeIn.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = inExchange.isAcknowledged();
        exchangeIn.setYesNo1(isAcknowledged);
        
        return exchangeIn;
    }
    
    private short getQuantity(IncomingExchange inExchange)
    {
        Integer itemCount = inExchange.getItemCount();
        Integer typeCount = inExchange.getTypeCount();
        Integer nonSpecimenCount = inExchange.getNonSpecimenCount();

        Integer discardCount = inExchange.getDiscardCount();
        Integer distributeCount = inExchange.getDistributeCount();
        Integer returnCount = inExchange.getReturnCount();
        
        return (short) (itemCount + typeCount + nonSpecimenCount - discardCount - distributeCount - returnCount);
    }

    private String getDescription(IncomingExchange inExchange)
    {
        String geoUnit = inExchange.getGeoUnit();
        if (geoUnit == null) geoUnit = "";
        else geoUnit = geoUnit + ": ";

        Integer itemCount = inExchange.getItemCount();
        Integer typeCount = inExchange.getTypeCount();
        Integer nonSpecimenCount = inExchange.getNonSpecimenCount();

        Integer discardCount = inExchange.getDiscardCount();
        Integer distributeCount = inExchange.getDistributeCount();
        Integer returnCount = inExchange.getReturnCount();
        
        Float cost = inExchange.getCost();
        
        Object[] args = {geoUnit, itemCount, typeCount, nonSpecimenCount, discardCount, distributeCount, returnCount, cost };
        String pattern = "{0}{1} items, {2} types, {3} non-specimens; {4} discarded, {5} distributed, {6} returned; cost: {7}";

        return MessageFormat.format(pattern, args);
    }
    
    private String getInsertSql(ExchangeIn exchangeIn)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, ExchangeDate, " +
                            "Number1, ReceivedFromOrganizationID, Remarks, SrcGeography, Text1, Text2, " +
                            "TimestampCreated, YesNo1";
        
        String[] values = new String[13];
        
        values[0]  = SqlUtils.sqlString( exchangeIn.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( exchangeIn.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( exchangeIn.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( exchangeIn.getDivision().getId());
        values[4]  = SqlUtils.sqlString( exchangeIn.getExchangeDate());
        values[5]  = SqlUtils.sqlString( exchangeIn.getNumber1());
        values[6]  = SqlUtils.sqlString( exchangeIn.getAgentReceivedFrom().getId());
        values[7]  = SqlUtils.sqlString( exchangeIn.getRemarks());
        values[8]  = SqlUtils.sqlString( exchangeIn.getSrcGeography());
        values[9]  = SqlUtils.sqlString( exchangeIn.getText1());
        values[10] = SqlUtils.sqlString( exchangeIn.getText2());
        values[11] = SqlUtils.sqlString( exchangeIn.getTimestampCreated());
        values[10] = SqlUtils.sqlString( exchangeIn.getYesNo1());
        
        return SqlUtils.getInsertSql("exchangein", fieldNames, values);
    }
}
