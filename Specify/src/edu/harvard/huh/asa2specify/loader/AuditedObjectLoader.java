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
import java.util.Hashtable;

import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.ku.brc.specify.datamodel.Agent;

public abstract class AuditedObjectLoader extends CsvToSqlLoader
{
    private static Hashtable<Integer, Agent> AgentsByOptrId = new Hashtable<Integer, Agent>();
    private static AsaIdMapper               BotanistsByOptr;
    private static OptrLookup                OptrLookup;
    private static BotanistLookup            BotanistLookup;
    
    public AuditedObjectLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }

    public static void setOptrLookup(OptrLookup optrLookup)
    {
        OptrLookup = optrLookup;
    }

    public static void setBotanistLookup(BotanistLookup botanistLookup)
    {
    	BotanistLookup = botanistLookup;
    }

    // Agent records that represent Optrs who are also Botanists will first be loaded
    // as Optrs, which puts the Optr id in the Agent GUID field.  During Botanist
    // loading, the Agent records for Optr-Botanists are updated to put the Botanist
    // id in the Agent GUID field.  That means the guid-- the means by which we get
    // those Agent records-- may change during Botanist loading.
    protected Agent getAgentByOptrId(Integer optrId) throws LocalException
    {
        Agent agent = AgentsByOptrId.get(optrId);

        if (agent == null && OptrLookup != null)
        {
        	agent = OptrLookup.queryById(optrId);
        	
        	if (agent == null && BotanistsByOptr != null)
        	{
	        	Integer botanistId = BotanistsByOptr.map(optrId);
	        	
	        	if (botanistId != null)
	        	{
	        		agent = BotanistLookup.getById(botanistId);
	        	}
        	}

        	if (agent == null) throw new LocalException("Couldn't find agent for optr id: " + optrId);
        	
            AgentsByOptrId.put(optrId, agent);
        }
    	return agent;
    }
}
