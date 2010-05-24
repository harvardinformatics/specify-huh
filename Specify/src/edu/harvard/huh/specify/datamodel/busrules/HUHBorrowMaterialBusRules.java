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

import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.BorrowMaterial;

/**
 * @author mmk
 *
 * @code_status Alpha
 *
 * May 22, 2010
 *
 */
public class HUHBorrowMaterialBusRules extends BaseBusRules
{

    public HUHBorrowMaterialBusRules()
    {
        super(BorrowMaterial.class);
    }
    
    @Override
    public void beforeMerge(final Object dataObj, DataProviderSessionIFace session)
    {
        BorrowMaterial borrowMaterial = (BorrowMaterial) dataObj;
        
        if (borrowMaterial.getItemCount() == null) borrowMaterial.setItemCount((short) 0);
        if (borrowMaterial.getTypeCount() == null) borrowMaterial.setTypeCount((short) 0);
        if (borrowMaterial.getNonSpecimenCount() == null) borrowMaterial.setNonSpecimenCount((short) 0);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#processBusiessRules(java.lang.Object)
     */
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof BorrowMaterial))
        {
            return STATUS.Error;
        }
        
        return STATUS.OK;
    }
}