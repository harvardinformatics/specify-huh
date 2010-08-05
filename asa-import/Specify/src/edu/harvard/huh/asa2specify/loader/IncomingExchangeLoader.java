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

import edu.harvard.huh.asa.IncomingExchange;
import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Transaction.ACCESSION_TYPE;
import edu.harvard.huh.asa.Transaction.ROLE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;

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
        
        Accession accession = getAccession(incomingExchange, ACCESSION_TYPE.Exchange);

        String sql = getInsertSql(accession);
        Integer accessionId = insert(sql);
        accession.setAccessionId(accessionId);
        
        Agent receiverAgent = lookupAffiliate(incomingExchange);
        if (receiverAgent != null)
        {
            AccessionAgent receiver = getAccessionAgent(accession, receiverAgent, ROLE.Receiver);
            sql = getInsertSql(receiver);
            insert(sql);
        }
        
        int organizationId = incomingExchange.getOrganizationId();
        boolean isSelfOrganized = Organization.IsSelfOrganizing(organizationId);
        
        if (!isSelfOrganized)
        {
            Agent contactAgent = lookupAgent(incomingExchange);
            if (contactAgent != null)
            {
                AccessionAgent contact = getAccessionAgent(accession, contactAgent, ROLE.Contact);
                sql = getInsertSql(contact);
                insert(sql);
            }
        }

        Agent contributorAgent = isSelfOrganized ? lookupAgent(incomingExchange) : lookupOrganization(incomingExchange);

        if (contributorAgent != null)
        {
            AccessionAgent contributor = getAccessionAgent(accession, contributorAgent, ROLE.Contributor);
            sql = getInsertSql(contributor);
            insert(sql);
        }

    }
    
    private IncomingExchange parse(String[] columns) throws LocalException
    {        
        IncomingExchange inExch = new IncomingExchange();
        
        super.parse(columns, inExch);
        
        return inExch;
    }
}
