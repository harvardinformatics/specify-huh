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

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public abstract class AuditedObjectLoader extends CsvToSqlLoader
{
    private OptrLoader optrLoader;
    
    public AuditedObjectLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
        // TODO Auto-generated constructor stub
    }

    public void setOptrLoader(OptrLoader optrLoader)
    {
        this.optrLoader = optrLoader;
    }

    protected Agent getAgentByOptrId(Integer optrId) throws LocalException
    {
        if (optrLoader != null)
        {
            return optrLoader.getAgentByOptrId(optrId);
        }
        else 
        {
            return null;
        }
    }
}
