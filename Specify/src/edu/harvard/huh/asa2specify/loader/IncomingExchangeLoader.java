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
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeIn;

public class IncomingExchangeLoader extends InGeoBatchTransactionLoader
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
        
        Agent affiliate = lookupAffiliate(incomingExchange);
        if (affiliate == null) affiliate = new Agent();
        
        Agent agent = lookupAgent(incomingExchange);
        if (agent == null) agent = new Agent();
        
        ExchangeIn exchangeIn = getExchangeIn(incomingExchange, affiliate, agent);
        
        String sql = getInsertSql(exchangeIn);
        insert(sql);
    }
    
    private IncomingExchange parse(String[] columns) throws LocalException
    {        
        IncomingExchange inExchange = new IncomingExchange();
        
        int i = super.parse(columns, inExchange);

        if (columns.length < i + 1)
        {
            throw new LocalException("Not enough columns");
        }
        
        inExchange.setAffiliateName( columns[i +0] );
        
        return inExchange;
    }
    
    private ExchangeIn getExchangeIn(IncomingExchange inExchange, Agent affiliate, Agent agent) throws LocalException
    {
        ExchangeIn exchangeIn = new ExchangeIn();
        
        // TODO: AddressOfRecord?
        
        // CatalogedBy (for use by in Asa -- affiliate)
        exchangeIn.setAgentCatalogedBy(affiliate);
        
        // CreatedByAgent
        Integer creatorOptrId = inExchange.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        exchangeIn.setCreatedByAgent(createdByAgent);
        
        // DescriptionOfMaterial (description + [boxCount])
        String description = getDescriptionOfMaterial(inExchange);
        if (description != null) description = truncate(description, 512, "description");
        exchangeIn.setDescriptionOfMaterial(description);
        
        // DiscardCount
        int discardCount = inExchange.getDiscardCount();
        exchangeIn.setDiscardCount((short) discardCount);
        
        // DistributeCount
        int distributeCount = inExchange.getDistributeCount();
        exchangeIn.setDistributeCount((short) distributeCount);
        
        // DivisionID
        exchangeIn.setDivision(getBotanyDivision());
        
        // ExchangeDate
        Date openDate = inExchange.getOpenDate();
        if (openDate != null)
        {
            exchangeIn.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // NonSpecimenCount
        int nonSpecimenCount = inExchange.getNonSpecimenCount();
        exchangeIn.setNonSpecimenCount((short) nonSpecimenCount);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = inExchange.getId();
        checkNull(transactionId, "transaction id");
        
        exchangeIn.setNumber1((float) transactionId);
        
        // QuantityExchanged (itemCount + typeCount + nonSpecimenCount - discardCount - distributeCount - returnCount)
        int quantity = inExchange.getBatchQuantity();
        exchangeIn.setQuantityExchanged((short) quantity);
        
        // ReceivedFromOrganization (Asa agent)
        exchangeIn.setAgentReceivedFrom(agent);
        
        // Remarks
        String remarks = inExchange.getRemarks();
        exchangeIn.setRemarks(remarks);
        
        // ReturnCount
        int returnCount = inExchange.getReturnCount();
        exchangeIn.setReturnCount((short) returnCount);
        
        // SrcGeography (geoUnit)
        String geoUnit = inExchange.getGeoUnit();
        geoUnit = truncate(geoUnit, 32, "geo unit");
        exchangeIn.setSrcGeography(geoUnit);
        
        // SrcTaxonomy (local unit)
        String localUnit = inExchange.getLocalUnit();
        exchangeIn.setSrcTaxonomy(localUnit);
        
        // Text1 (transaction no)
        String transactionNo = inExchange.getTransactionNo();
        exchangeIn.setText1(transactionNo);
        
        // Text2 (purpose)
        String purpose = Transaction.toString(inExchange.getPurpose());
        exchangeIn.setText2(purpose);
        
        // TypeCount
        int typeCount = inExchange.getTypeCount();
        exchangeIn.setTypeCount((short) typeCount);
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = inExchange.isAcknowledged();
        exchangeIn.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(inExchange.getRequestType());
        exchangeIn.setYesNo2(isTheirs);
        
        return exchangeIn;
    }

    private String getInsertSql(ExchangeIn accession)
    {
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, " +
                            "DiscardCount, DistributeCount, DivisionID, ExchangeDate, " +
                            "NonSpecimenCount, Number1, QuantityExchanged, Remarks, " +
                            "ReturnCount, SrcGeography, SrcTaxonomy, Text1, Text2, " +
                            "TypeCount, TimestampCreated, Version, YesNo1, YesNo2";

        String[] values = new String[21];

        values[0]  = SqlUtils.sqlString( accession.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( accession.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( accession.getDiscardCount());
        values[4]  = SqlUtils.sqlString( accession.getDistributeCount());
        values[5]  = SqlUtils.sqlString( accession.getDivision().getId());
        values[6]  = SqlUtils.sqlString( accession.getExchangeDate());
        values[7]  = SqlUtils.sqlString( accession.getNonSpecimenCount());
        values[8]  = SqlUtils.sqlString( accession.getNumber1());
        values[9]  = SqlUtils.sqlString( accession.getQuantityExchanged());
        values[10] = SqlUtils.sqlString( accession.getRemarks());
        values[11] = SqlUtils.sqlString( accession.getReturnCount());
        values[12] = SqlUtils.sqlString( accession.getSrcGeography());
        values[13] = SqlUtils.sqlString( accession.getSrcTaxonomy());
        values[14] = SqlUtils.sqlString( accession.getText1());
        values[15] = SqlUtils.sqlString( accession.getText2());
        values[16] = SqlUtils.sqlString( accession.getTypeCount());
        values[17] = SqlUtils.now();
        values[18] = SqlUtils.zero();
        values[19] = SqlUtils.sqlString( accession.getYesNo1());
        values[20] = SqlUtils.sqlString( accession.getYesNo2());
        
        return SqlUtils.getInsertSql("exchangein", fieldNames, values);
    }
    
/*    private String getInsertSql(AccessionAgent accessionAgent)
    {
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated, Version";

        String[] values = new String[5];

        values[0] = SqlUtils.sqlString( accessionAgent.getAccession().getId());
        values[1] = SqlUtils.sqlString( accessionAgent.getAgent().getId());
        values[2] = SqlUtils.sqlString( accessionAgent.getRole());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }*/
}
