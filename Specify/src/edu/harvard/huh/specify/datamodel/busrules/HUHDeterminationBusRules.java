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

import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.busrules.DeterminationBusRules;

/**
 * @author mkelly
 *
 * @code_status Alpha
 *
 * Created Date: May 9, 2010
 *
 */
public class HUHDeterminationBusRules extends DeterminationBusRules
{
    protected Determination determination = null;

    public HUHDeterminationBusRules()
    {
        super();
    }
/*
    @Override
    public boolean afterSaveCommit(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj instanceof Determination && formViewObj != null)
        {
            Determination determination = (Determination) dataObj;
            
            Fragment fragment = determination.getFragment();

            formViewObj.setParentDataObj(fragment);
            formViewObj.setCellName("determinations");
        }
        
        return true;
    }

    @Override
    public boolean shouldCloneField(final String fieldName)
    {
        if ("fragment".equals(fieldName)) return false;
        
        return super.shouldCloneField(fieldName);
    }
*/
}
