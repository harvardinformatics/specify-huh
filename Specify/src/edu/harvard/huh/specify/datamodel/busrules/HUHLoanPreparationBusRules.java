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

import java.awt.Component;

import javax.swing.JTextField;

import edu.harvard.huh.specify.plugins.ItemCountsLabel;
import edu.harvard.huh.specify.plugins.PrepItemTable;
import edu.harvard.huh.specify.util.LoanPrepUtil;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.SubViewBtn;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValSpinner;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.busrules.LoanPreparationBusRules;
import edu.ku.brc.ui.CommandListener;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Jan 29, 2007
 *
 */
public class HUHLoanPreparationBusRules extends LoanPreparationBusRules implements CommandListener
{
    private SubViewBtn loanRetBtn       = null;
    
    private final String PREPARATION  = "preparation";
    private final String DESCRIPTION  = "descriptionOfMaterial";
    private final String HIGHER_TAXON = "higherTaxon";
    private final String SRC_TAXONOMY = "srcTaxonomy";
    private final String TYPE_COUNT   = "typeCount";
    private final String ITEM_COUNT   = "itemCount";
    private final String PREP_ITEM_TABLE = "prepitemtable";
    private final String ITEM_COUNTS_LABEL = "itemcountslabel";
    private final String IS_RESOLVED = "isResolved";
    /**
     * Constructor.
     */
    public HUHLoanPreparationBusRules()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        viewable = viewableArg;
        if (viewable instanceof FormViewObj)
        {
            formViewObj = (FormViewObj)viewable;
        }
        
        if (formViewObj != null)
        {
            formViewObj.setSkippingAttach(true);

            Component comp = formViewObj.getControlById("loanReturnPreparations");
            if (comp instanceof SubViewBtn)
            {
                loanRetBtn = (SubViewBtn)comp;
                loanRetBtn.getBtn().setIcon(null);
                loanRetBtn.getBtn().setText(UIRegistry.getResourceString("LOAN_RET_PREP"));
            }
            
        }
    }

    @Override
    public void afterFillForm(Object dataObj) {
    	super.afterFillForm(dataObj);
    	
    	if (formViewObj != null && dataObj != null) {
    		ItemCountsLabel itemCountsLabel = (ItemCountsLabel)formViewObj.getControlById(ITEM_COUNTS_LABEL);
    		PrepItemTable prepItemTable = (PrepItemTable)formViewObj.getControlById(PREP_ITEM_TABLE);
    		
    		Component typeCountComp = formViewObj.getControlById(TYPE_COUNT);
    		Component itemCountComp = formViewObj.getControlById(ITEM_COUNT);
    		//Component descriptionComp = formViewObj.getControlByName(DESCRIPTION);
    		Component isResolvedComp = formViewObj.getControlByName(IS_RESOLVED);

    		LoanPreparation loanPreparation = (LoanPreparation)dataObj;
    		itemCountsLabel.updateCounts(loanPreparation.getLoan());
    		LoanPrepUtil lpUtil = new LoanPrepUtil(loanPreparation);
    		
    		int itemTotal = lpUtil.getItemCount();
    		int typeTotal = lpUtil.getTypeCount();
    		
            Component srcTaxonComp    = formViewObj.getControlById(SRC_TAXONOMY);

            if (srcTaxonComp != null && srcTaxonComp instanceof JTextField) {
            	((JTextField) srcTaxonComp).setText(lpUtil.getTaxonName());
            }
                  
    		
    		if (itemCountComp instanceof ValSpinner)
    			((ValSpinner)itemCountComp).setValue(itemTotal);
    		if (itemCountComp instanceof ValSpinner)
    			((ValSpinner)typeCountComp).setValue(typeTotal);
    		
    		boolean enabled = false;
    		// TODO: Maybe instead of null, isLot() can return false
    		if (lpUtil.isLot() != null && lpUtil.isLot()) {
    			enabled = true;
    		}

    		itemCountComp.setEnabled(enabled);
    		typeCountComp.setEnabled(enabled);
    	}
    }
}
