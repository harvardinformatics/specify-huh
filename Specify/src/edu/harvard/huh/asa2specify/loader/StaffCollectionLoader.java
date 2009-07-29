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

import edu.harvard.huh.asa.StaffCollection;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class StaffCollectionLoader extends InGeoBatchTransactionLoader
{
    private static final Logger log  = Logger.getLogger(StaffCollectionLoader.class);
    
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
        
        AccessionAgent collector = getAccessionAgent(staffCollection, accession, ROLE.Collector);
        if (collector != null)
        {
            sql = getInsertSql(collector);
            insert(sql);
        }
    }
    
    public Logger getLogger()
    {
        return log;
    }
    
    private StaffCollection parse(String[] columns) throws LocalException
    {        
        StaffCollection staffCollection = new StaffCollection();
        
        super.parse(columns, staffCollection);

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
        String description = staffCollection.getDescription();
        if (description != null) description = truncate(description, 255, "accession condition");
        accession.setAccessionCondition(description);
        
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
        String remarks = staffCollection.getRemarks();
        accession.setRemarks(remarks);
        
        // Text1 (itemCount, typeCount, nonSpecimenCount)
        String itemCountNote = staffCollection.getItemCountNote();
        accession.setText1(itemCountNote);
        
        // Text2 (purpose)
        String purpose = Transaction.toString(staffCollection.getPurpose());
        accession.setText2(purpose);
        
        // Text3 (geoUnit)
        String geoUnit = staffCollection.getGeoUnit();
        accession.setText3(geoUnit);
        
        // Type
        accession.setType(TransactionLoader.toString(ACCESSION_TYPE.Collection));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = staffCollection.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
        
        // YesNo2 (requestType = "theirs")
        Boolean isTheirs = isTheirs(staffCollection.getRequestType());
        accession.setYesNo2(isTheirs);
        
        return accession;
    }
    
    private AccessionAgent getAccessionAgent(Transaction transaction, Accession accession, ROLE role)
        throws LocalException
    {
           AccessionAgent accessionAgent = new AccessionAgent();

            // Agent
            Agent agent = null;

            if (role.equals(ROLE.Preparer) || role.equals(ROLE.Collector))
            {
                agent = lookupAffiliate(transaction);
            }
            else if (role.equals(ROLE.Contributor))
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
    
    private String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionCondition, AccessionNumber, CreatedByAgentID, DateAccessioned, DivisionID, " +
        		            "Number1, Remarks, Text1, Text2, Text3, Type, TimestampCreated, Version, YesNo1, YesNo2";

        String[] values = new String[15];

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
        values[12] = SqlUtils.zero();
        values[13] = SqlUtils.sqlString( accession.getYesNo1());
        values[14] = SqlUtils.sqlString( accession.getYesNo2());
        
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
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
    }
}
