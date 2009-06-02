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

import edu.harvard.huh.asa.StaffCollection;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class StaffCollectionLoader extends TransactionLoader
{
    public StaffCollectionLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        StaffCollection staffCollection = parse(columns);

        Integer transactionId = staffCollection.getId();
        setCurrentRecordId(transactionId);
        
        Accession accession = getAccession(staffCollection);
        
        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        AccessionAgent collector = getAccessionAgent(staffCollection, accession, ROLE.collector);
        if (collector != null)
        {
            sql = getInsertSql(collector);
            insert(sql);
        }
    }
    
    private StaffCollection parse(String[] columns) throws LocalException
    {        
        StaffCollection staffCollection = new StaffCollection();
        
        int i = parse(columns, staffCollection);
        
        if (columns.length < i + 10)
        {
            throw new LocalException("Not enough columns");
        }
        
        staffCollection.setOriginalDueDate( SqlUtils.parseDate( columns[i + 0] ));
        staffCollection.setCurrentDueDate(  SqlUtils.parseDate( columns[i + 1] ));
        staffCollection.setGeoUnit(                             columns[i + 2] );
        staffCollection.setItemCount(        SqlUtils.parseInt( columns[i + 3] ));
        staffCollection.setTypeCount(        SqlUtils.parseInt( columns[i + 4] ));
        staffCollection.setNonSpecimenCount( SqlUtils.parseInt( columns[i + 5] ));
        staffCollection.setDiscardCount(     SqlUtils.parseInt( columns[i + 6] ));
        staffCollection.setDistributeCount(  SqlUtils.parseInt( columns[i + 7] ));
        staffCollection.setReturnCount(      SqlUtils.parseInt( columns[i + 8] ));
        staffCollection.setCost(           SqlUtils.parseFloat( columns[i + 9] ));
        
        return staffCollection;
    }
    
    private Accession getAccession(StaffCollection staffCollection) throws LocalException
    {
        Accession accession = new Accession();
        
        // TODO: AddressOfRecord
        
        // CreatedByAgent
        Integer creatorOptrId = staffCollection.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        accession.setCreatedByAgent(createdByAgent);
        
        // AccesionCondition
        String accessionCondition = getDescription(staffCollection);
        accessionCondition = truncate(accessionCondition, 255, "accession condition");
        accession.setAccessionCondition(accessionCondition);
        
        // AccessionNumber
        String transactionNo = staffCollection.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_ACCESSION_NUMBER;
        }
        transactionNo = truncate(transactionNo, 50, "invoice number");
        accession.setAccessionNumber(transactionNo);
        
        // DateAccessioned
        Date openDate = staffCollection.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
        // Division
        accession.setDivision(getBotanyDivision());
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = staffCollection.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
        
        // Remarks
        String remarks = accession.getRemarks();
        accession.setRemarks(remarks);
        
        // Text1 (description)
        String description = staffCollection.getDescription();
        accession.setText1(description);
        
        // Text2 (forUseBy)
        String forUseBy = staffCollection.getForUseBy();
        accession.setText2(forUseBy);
        
        // Text3 (boxCount)
        String boxCount = staffCollection.getBoxCount();
        accession.setText3(boxCount);
        
        // Type
        accession.setType(ACCESSION_TYPE.cln.name());
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = staffCollection.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        return accession;
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

    private String getDescription(StaffCollection staffCollection)
    {
        String geoUnit = staffCollection.getGeoUnit();
        if (geoUnit == null) geoUnit = "";
        else geoUnit = geoUnit + ": ";

        Integer itemCount = staffCollection.getItemCount();
        Integer typeCount = staffCollection.getTypeCount();
        Integer nonSpecimenCount = staffCollection.getNonSpecimenCount();

        Integer discardCount = staffCollection.getDiscardCount();
        Integer distributeCount = staffCollection.getDistributeCount();
        Integer returnCount = staffCollection.getReturnCount();
        
        Float cost = staffCollection.getCost();
        
        Object[] args = {geoUnit, itemCount, typeCount, nonSpecimenCount, discardCount, distributeCount, returnCount, cost };
        String pattern = "{0}{1} items, {2} types, {3} non-specimens; {4} discarded, {5} distributed, {6} returned; cost: {7}";

        return MessageFormat.format(pattern, args);
    }
    
    private String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionCondition, AccessionNumber, CreatedByAgentID, DateAccessioned, DivisionID, " +
        		            "Number1, Remarks, Text1, Text2, Text3, Type, TimestampCreated, YesNo1";

        String[] values = new String[13];

        values[0]  = SqlUtils.sqlString( accession.getAccessionCondition());
        values[1]  = SqlUtils.sqlString( accession.getAccessionNumber());
        values[2]  = SqlUtils.sqlString( accession.getCreatedByAgent().getId());
        values[3]  = SqlUtils.sqlString( accession.getDateAccessioned());
        values[4]  = SqlUtils.sqlString( accession.getDivision().getId());
        values[4]  = SqlUtils.sqlString( accession.getNumber1());
        values[5]  = SqlUtils.sqlString( accession.getRemarks());
        values[6]  = SqlUtils.sqlString( accession.getText1());
        values[7]  = SqlUtils.sqlString( accession.getText2());
        values[8]  = SqlUtils.sqlString( accession.getText3());
        values[9]  = SqlUtils.sqlString( accession.getType());
        values[10] = SqlUtils.now();
        values[11] = SqlUtils.sqlString( accession.getYesNo1());

        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    private String getInsertSql(AccessionAgent accessionAgent)
    {
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated";

        String[] values = new String[4];

        values[0] = SqlUtils.sqlString( accessionAgent.getAccession().getId());
        values[1] = SqlUtils.sqlString( accessionAgent.getAgent().getId());
        values[2] = SqlUtils.sqlString( accessionAgent.getRole());
        values[3] = SqlUtils.now();

        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }
}
