/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import edu.ku.brc.specify.datamodel.Agent;

/**
 * This alters the UI depending on which type of agent is set.
 * 
 * @author rods
 *
 * @code_status Complete
 *
 * Created Date: Jan 24, 2007
 *
 */
public class HUHAgentBusRules extends AttachmentOwnerBaseBusRules
{    
    /**
     * Constructor.
     */
    public HUHAgentBusRules()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        STATUS status =  super.processBusinessRules(dataObj);
        
        if (status == STATUS.OK)
        {
            status = checkVariants((Agent) dataObj);
        }
        return status;
    }
    
    /**
     * Process business rules specifically related to determinations.  Return OK status, or add error messages
     * to reasonList and return Error status.
     * @param dataObj
     * @return 
     */
    protected STATUS checkVariants(final Agent agent)
    {
        // check that at least one variant exists
        if (agent.getVariants().size() < 1)
        {
            reasonList.add(getResourceString("AgentBusRules.NO_VARIANTS"));

            return STATUS.Warning;
        }

        return STATUS.OK;
    }
}
