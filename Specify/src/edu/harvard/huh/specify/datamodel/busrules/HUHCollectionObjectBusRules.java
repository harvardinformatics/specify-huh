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
package edu.harvard.huh.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Created Date: Jan 24, 2007
 *
 */
public class HUHCollectionObjectBusRules extends CollectionObjectBusRules
{
    /**
     * Constructor.
     */
    public HUHCollectionObjectBusRules()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.datamodel.busrules.COllectionObjectBusRules(java.lang.Object)
     */
    protected STATUS checkDeterminations(final Object dataObj)
    {
        // check that a current determination exists
        if (((CollectionObject) dataObj).getFragments().size() > 0)
        {
            for (Fragment fragment : ((CollectionObject) dataObj).getFragments())
            {
                if (fragment.getDeterminations().size() > 0)
                {
                    int currents = 0;
                    for (Determination det : fragment.getDeterminations())
                    {
                        if (det.isCurrentDet())
                        {
                            currents++;
                        }
                    }
                    if (currents != 1)
                    {
                        if (currents == 0)
                        {
                            reasonList.add(getResourceString("CollectionObjectBusRules.CURRENT_DET_REQUIRED"));
                        }
                        else
                        {
                            reasonList.add(getResourceString("CollectionObjectBusRules.ONLY_ONE_CURRENT_DET"));
                        }
                        return STATUS.Warning;
                    }
                }
            }
        }
        return STATUS.OK;
    }
}
