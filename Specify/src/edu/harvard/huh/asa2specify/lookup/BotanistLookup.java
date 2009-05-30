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
package edu.harvard.huh.asa2specify.lookup;

import edu.harvard.huh.asa2specify.LocalException;
import edu.ku.brc.specify.datamodel.Agent;

public interface BotanistLookup
{
    /**
     * Create an Agent object with id found from database record in agent table
     * corresponding to a previously loaded Botanist with given botanistId.  Throws
     * an exception of the record was not found.
     * 
     * @param botanistId
     * @return
     * @throws LocalException
     */
    public Agent getById(Integer botanistId) throws LocalException;
}
