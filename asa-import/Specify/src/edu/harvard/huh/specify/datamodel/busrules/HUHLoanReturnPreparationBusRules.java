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
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.ui.UIRegistry;

/**
 * This class duplicates the logic in HUHBorrowReturnMaterialBusRules; it should be merged
 * 
 * @author mmk
 * 
 * @code_status Alpha
 *
 * May 22, 2010
 *
 */
public class HUHLoanReturnPreparationBusRules extends BaseBusRules
{
    /**
     * 
     */
    public HUHLoanReturnPreparationBusRules()
    {
        super(LoanReturnPreparation.class);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#processBusiessRules(java.lang.Object)
     */
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof LoanReturnPreparation))
        {
            return STATUS.Error;
        }
        
        LoanReturnPreparation returnPrep = (LoanReturnPreparation) dataObj;

        // Check counts to make sure they don't exceed what's available.
        LoanPreparation loanPrep = returnPrep.getLoanPreparation();

        if (loanPrep != null)
        {
            Integer itemCount        = loanPrep.getItemCount();
            Integer nonSpecimenCount = loanPrep.getNonSpecimenCount();
            Integer typeCount        = loanPrep.getTypeCount();
            
            Integer returnItemCount        = returnPrep.getItemCount();
            Integer returnNonSpecimenCount = returnPrep.getNonSpecimenCount();
            Integer returnTypeCount        = returnPrep.getTypeCount();
        
            if (! countIsOK(itemCount, returnItemCount))
            {
                reasonList.add(UIRegistry.getLocalizedMessage("LRP_ITEM_COUNT_ERR"));
                return STATUS.Error;
            }
            
            if (! countIsOK(nonSpecimenCount, returnNonSpecimenCount))
            {
                reasonList.add(UIRegistry.getLocalizedMessage("LRP_NONSP_COUNT_ERR"));
                return STATUS.Error;
            }
            
            if (! countIsOK(typeCount, returnTypeCount))
            {
                reasonList.add(UIRegistry.getLocalizedMessage("LRP_TYPE_COUNT_ERR"));
                return STATUS.Error;
            }
        }
        return STATUS.OK;
    }

    private boolean countIsOK(Integer borrowCount, Integer returnCount)
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
