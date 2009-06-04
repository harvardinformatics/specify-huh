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

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Purchase;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class PurchaseLoader extends TransactionLoader
{
    private static final Logger log  = Logger.getLogger(PurchaseLoader.class);
    
    public PurchaseLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        Purchase purchase = parse(columns);

        Integer transactionId = purchase.getId();
        setCurrentRecordId(transactionId);
        
        Accession accession = getAccession(purchase);
        
        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        AccessionAgent preparer = getAccessionAgent(purchase, accession, ROLE.preparer);
        if (preparer != null)
        {
            sql = getInsertSql(preparer);
            insert(sql);
        }
        
        AccessionAgent contributor = getAccessionAgent(purchase, accession, ROLE.contributor);
        if (contributor != null)
        {
            sql = getInsertSql(contributor);
            insert(sql);
        }
    }

    public Logger getLogger()
    {
        return log;
    }
    
    private Accession getAccession(Purchase purchase) throws LocalException
    {
        Accession accession = new Accession();
        
        // TODO: AddressOfRecord
        
        // CreatedByAgent
        Integer creatorOptrId = purchase.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        accession.setCreatedByAgent(createdByAgent);
        
        // AccesionCondition
        String accessionCondition = getDescription(purchase);
        accessionCondition = truncate(accessionCondition, 255, "accession condition");
        accession.setAccessionCondition(accessionCondition);
        
        // AccessionNumber
        String transactionNo = purchase.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_ACCESSION_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "invoice number");
        accession.setAccessionNumber(transactionNo);
        
        // DateAccessioned
        Date openDate = purchase.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
        // Division
        accession.setDivision(getBotanyDivision());
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = purchase.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = accession.getRemarks();
        accession.setRemarks(remarks);
        
        // Text1 (description)
        String description = purchase.getDescription();
        accession.setText1(description);
        
        // Text2 (forUseBy)
        String forUseBy = purchase.getForUseBy();
        accession.setText2(forUseBy);
        
        // Text3 (boxCount)
        String boxCount = purchase.getBoxCount();
        accession.setText3(boxCount);
        
        // Type
        accession.setType(ACCESSION_TYPE.purchase.name());
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = purchase.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        return accession;
    }
    
    private Purchase parse(String[] columns) throws LocalException
    {        
        Purchase purchase = new Purchase();
        
        int i = parse(columns, purchase);
        
        if (columns.length < i + 10)
        {
            throw new LocalException("Not enough columns");
        }
        
        purchase.setOriginalDueDate( SqlUtils.parseDate( columns[i + 0] ));
        purchase.setCurrentDueDate(  SqlUtils.parseDate( columns[i + 1] ));
        purchase.setGeoUnit(                             columns[i + 2] );
        purchase.setItemCount(        SqlUtils.parseInt( columns[i + 3] ));
        purchase.setTypeCount(        SqlUtils.parseInt( columns[i + 4] ));
        purchase.setNonSpecimenCount( SqlUtils.parseInt( columns[i + 5] ));
        purchase.setDiscardCount(     SqlUtils.parseInt( columns[i + 6] ));
        purchase.setDistributeCount(  SqlUtils.parseInt( columns[i + 7] ));
        purchase.setReturnCount(      SqlUtils.parseInt( columns[i + 8] ));
        purchase.setCost(           SqlUtils.parseFloat( columns[i + 9] ));
        
        return purchase;
    }
    
    private AccessionAgent getAccessionAgent(Transaction transaction, Accession accession, ROLE role)
        throws LocalException
    {
           AccessionAgent accessionAgent = new AccessionAgent();

            // Agent
            Agent agent = null;

            if (role.equals(ROLE.preparer) || role.equals(ROLE.collector))
            {
                agent = lookupAffiliate(transaction);
            }
            else if (role.equals(ROLE.contributor))
            {
                agent = lookupAgent(transaction);
            }

            if (agent == null || agent.getId() == null) return null;

            accessionAgent.setAgent(agent);

            // Deaccession
            accessionAgent.setAccession(accession);

            // Remarks

            // Role
            accessionAgent.setRole(role.name());
            
            return accessionAgent;
    }

    private String getDescription(Purchase purchase)
    {
        String geoUnit = purchase.getGeoUnit();
        if (geoUnit == null) geoUnit = "";
        else geoUnit = geoUnit + ": ";

        Integer itemCount = purchase.getItemCount();
        Integer typeCount = purchase.getTypeCount();
        Integer nonSpecimenCount = purchase.getNonSpecimenCount();

        Integer discardCount = purchase.getDiscardCount();
        Integer distributeCount = purchase.getDistributeCount();
        Integer returnCount = purchase.getReturnCount();
        
        Float cost = purchase.getCost();
        
        Object[] args = {geoUnit, itemCount, typeCount, nonSpecimenCount, discardCount, distributeCount, returnCount, cost };
        String pattern = "{0}{1} items, {2} types, {3} non-specimens; {4} discarded, {5} distributed, {6} returned; cost: {7}";

        return MessageFormat.format(pattern, args);
    }
    
    private String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionCondition, AccessionNumber, CreatedByAgentID, DateAccessioned, " +
                            "DivisionID, Number1, Remarks, Text1, Text2, Text3, Type, TimestampCreated, " +
                            "Version, YesNo1";

        String[] values = new String[14];

        values[0]  = SqlUtils.sqlString( accession.getAccessionCondition());
        values[1]  = SqlUtils.sqlString( accession.getAccessionNumber());
        values[2]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[3]  = SqlUtils.sqlString( accession.getDateAccessioned());
        values[4]  = SqlUtils.sqlString( accession.getDivision().getId());
        values[5]  = SqlUtils.sqlString( accession.getNumber1());
        values[6]  = SqlUtils.sqlString( accession.getRemarks());
        values[7]  = SqlUtils.sqlString( accession.getText1());
        values[8]  = SqlUtils.sqlString( accession.getText2());
        values[9]  = SqlUtils.sqlString( accession.getText3());
        values[10] = SqlUtils.sqlString( accession.getType());
        values[11] = SqlUtils.now();
        values[12] = SqlUtils.one();
        values[13] = SqlUtils.sqlString( accession.getYesNo1());

        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    private String getInsertSql(AccessionAgent accessionAgent)
    {
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated, Version";

        String[] values = new String[5];

        values[0] = SqlUtils.sqlString( accessionAgent.getAccession().getId());
        values[1] = SqlUtils.sqlString( accessionAgent.getAgent().getId());
        values[2] = SqlUtils.sqlString( accessionAgent.getRole());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }
}
