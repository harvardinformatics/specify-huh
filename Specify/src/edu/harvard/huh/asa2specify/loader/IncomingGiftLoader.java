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

import edu.harvard.huh.asa.IncomingGift;
import edu.harvard.huh.asa.Transaction.ACCESSION_TYPE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

public class IncomingGiftLoader extends InGeoBatchTransactionLoader
{    
    public IncomingGiftLoader(File csvFile,  Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }
    
    public void loadRecord(String[] columns) throws LocalException
    {
        IncomingGift incomingGift = parse(columns);

        Integer transactionId = incomingGift.getId();
        setCurrentRecordId(transactionId);
        
        Accession accession = getAccession(incomingGift, ACCESSION_TYPE.Gift);
        
        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        Agent receiverAgent = lookupAffiliate(incomingGift);
        if (receiverAgent != null)
        {
            AccessionAgent receiver = getAccessionAgent(accession, receiverAgent, ROLE.Receiver);
            sql = getInsertSql(receiver);
            insert(sql);
        }
        
        Agent contactAgent = lookupAgent(incomingGift);
        if (contactAgent != null)
        {
            AccessionAgent contact = getAccessionAgent(accession, contactAgent, ROLE.Donor);
            sql = getInsertSql(contact);
            insert(sql);
        }

    }
    
    private IncomingGift parse(String[] columns) throws LocalException
    {        
        IncomingGift inGift = new IncomingGift();
        
        super.parse(columns, inGift);
        
        return inGift;
    }
}
