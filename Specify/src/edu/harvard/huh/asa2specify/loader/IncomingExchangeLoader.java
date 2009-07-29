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

import edu.harvard.huh.asa.IncomingExchange;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeIn;

public class IncomingExchangeLoader extends InGeoBatchTransactionLoader
{
    private static final Logger log  = Logger.getLogger(IncomingExchangeLoader.class);
    
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
    
    public Logger getLogger()
    {
        return log;
    }
    
    private IncomingExchange parse(String[] columns) throws LocalException
    {        
        IncomingExchange inExchange = new IncomingExchange();
        
        int i = super.parse(columns, inExchange);

        if (columns.length < i + i)
        {
            throw new LocalException("Not enough columns");
        }
        
        inExchange.setAffiliateName( columns[i +0] );
        
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
        
        // Number2 (cost)
        Float cost = inExchange.getCost();
        exchangeIn.setNumber2(cost);
        
        // QuantityExchanged
        short quantity = inExchange.getBatchQuantity();
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
        
        // Text1 (boxCount, typeCount, nonSpecimenCount, discardCount, distributeCount, returnCount)
        String description = inExchange.getItemCountNote();
        exchangeIn.setText1(description);
        
        // Text2 (localUnit, transactionNo, affiliate)
        String transactionNo = inExchange.getTransactionNo();
        String affiliateName = inExchange.getAffiliateName();
        
        String text2 = transactionNo + (affiliateName == null ? "" : ".  For use by " + affiliateName);
 
        exchangeIn.setText2(text2);
        
        // TimestampCreated
        Date dateCreated = inExchange.getDateCreated();
        exchangeIn.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = inExchange.isAcknowledged();
        exchangeIn.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(inExchange.getRequestType());
        exchangeIn.setYesNo2(isTheirs);
        
        return exchangeIn;
    }
    
    private String getInsertSql(ExchangeIn exchangeIn)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, ExchangeDate, " +
                            "Number1, Number2, ReceivedFromOrganizationID, Remarks, SrcGeography, Text1, Text2, " +
                            "TimestampCreated, Version, YesNo1, YesNo2";
        
        String[] values = new String[16];
        
        values[0]  = SqlUtils.sqlString( exchangeIn.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( exchangeIn.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( exchangeIn.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( exchangeIn.getDivision().getId());
        values[4]  = SqlUtils.sqlString( exchangeIn.getExchangeDate());
        values[5]  = SqlUtils.sqlString( exchangeIn.getNumber1());
        values[6]  = SqlUtils.sqlString( exchangeIn.getNumber2());
        values[7]  = SqlUtils.sqlString( exchangeIn.getAgentReceivedFrom().getId());
        values[8]  = SqlUtils.sqlString( exchangeIn.getRemarks());
        values[9]  = SqlUtils.sqlString( exchangeIn.getSrcGeography());
        values[10] = SqlUtils.sqlString( exchangeIn.getText1());
        values[11] = SqlUtils.sqlString( exchangeIn.getText2());
        values[12] = SqlUtils.sqlString( exchangeIn.getTimestampCreated());
        values[13] = SqlUtils.zero();
        values[14] = SqlUtils.sqlString( exchangeIn.getYesNo1());
        values[15] = SqlUtils.sqlString( exchangeIn.getYesNo2());
        
        return SqlUtils.getInsertSql("exchangein", fieldNames, values);
    }
}
