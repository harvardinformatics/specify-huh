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

import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Purchase;
import edu.harvard.huh.asa.Transaction.ACCESSION_TYPE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class PurchaseLoader extends InGeoBatchTransactionLoader
{
    public PurchaseLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        Purchase purchase = parse(columns);

        Integer transactionId = purchase.getId();
        setCurrentRecordId(transactionId);
        
        Accession accession = getAccession(purchase, ACCESSION_TYPE.Purchase);
        //Accession accession = getById(purchase.getId());
        
        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        Agent receiverAgent = lookupAffiliate(purchase);
        if (receiverAgent != null)
        {
            AccessionAgent receiver = getAccessionAgent(accession, receiverAgent, ROLE.ForUseBy);
            sql = getInsertSql(receiver);
            insert(sql);
        }

        Agent collectorAgent = lookupAgent(purchase);
        if (collectorAgent != null)
        {
            AccessionAgent seller = getAccessionAgent(accession, collectorAgent, ROLE.Collector);
            sql = getInsertSql(seller);
            insert(sql);
        }
    }
    
    private Purchase parse(String[] columns) throws LocalException
    {        
        Purchase purchase = new Purchase();
        
        super.parse(columns, purchase);

        return purchase;
    }
}
