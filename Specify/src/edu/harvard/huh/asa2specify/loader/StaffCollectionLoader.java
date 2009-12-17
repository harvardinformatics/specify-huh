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

import edu.harvard.huh.asa.StaffCollection;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class StaffCollectionLoader extends InGeoBatchTransactionLoader
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
        
        Agent receiverAgent = lookupAffiliate(staffCollection);
        if (receiverAgent != null)
        {
            AccessionAgent receiver = getAccessionAgent(accession, receiverAgent, ROLE.Collector);
            sql = getInsertSql(receiver);
            insert(sql);
        }
        
        Agent donorAgent = lookupAgent(staffCollection);
        if (donorAgent != null)
        {
            AccessionAgent donor = getAccessionAgent(accession, donorAgent, ROLE.Donor);
            sql = getInsertSql(donor);
            insert(sql);
        }
    }
    
    private StaffCollection parse(String[] columns) throws LocalException
    {        
        StaffCollection staffCollection = new StaffCollection();
        
        super.parse(columns, staffCollection);

        return staffCollection;
    }
}
