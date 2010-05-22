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
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowMaterial;
import edu.ku.brc.specify.datamodel.BorrowReturnMaterial;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author mmk
 *
 * @code_status Alpha
 *
 * May 22, 2010
 *
 */
public class HUHBorrowReturnMaterialBusRules extends BaseBusRules
{

    public HUHBorrowReturnMaterialBusRules()
    {
        super(Borrow.class);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#processBusiessRules(java.lang.Object)
     */
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof BorrowReturnMaterial))
        {
            return STATUS.Error;
        }
        
        BorrowReturnMaterial returnMaterial = (BorrowReturnMaterial) dataObj;

        // Check counts to make sure they don't exceed what's available.
        BorrowMaterial borrowMaterial = returnMaterial.getBorrowMaterial();

        if (borrowMaterial != null)
        {
            Short itemCount        = borrowMaterial.getItemCount();
            Short nonSpecimenCount = borrowMaterial.getNonSpecimenCount();
            Short typeCount        = borrowMaterial.getTypeCount();
            
            Short returnItemCount        = returnMaterial.getItemCount();
            Short returnNonSpecimenCount = returnMaterial.getNonSpecimenCount();
            Short returnTypeCount        = returnMaterial.getTypeCount();
        
            if (! countIsOK(itemCount, returnItemCount))
            {
                reasonList.add(UIRegistry.getLocalizedMessage("BRM_ITEM_COUNT_ERR"));
                return STATUS.Error;
            }
            
            if (! countIsOK(nonSpecimenCount, returnNonSpecimenCount))
            {
                reasonList.add(UIRegistry.getLocalizedMessage("BRM_NONSP_COUNT_ERR"));
                return STATUS.Error;
            }
            
            if (! countIsOK(typeCount, returnTypeCount))
            {
                reasonList.add(UIRegistry.getLocalizedMessage("BRM_TYPE_COUNT_ERR"));
                return STATUS.Error;
            }
        }
        return STATUS.OK;
    }

    private boolean countIsOK(Short borrowCount, Short returnCount)
    {
        if (borrowCount != null)
        {
            if (returnCount != null)
            {
                if (returnCount > borrowCount) return false;
            }
        }
        else
        {
            if (returnCount != null)
            {
                if (returnCount != 0) return false;
            }
        }
        return true;
    }
}
