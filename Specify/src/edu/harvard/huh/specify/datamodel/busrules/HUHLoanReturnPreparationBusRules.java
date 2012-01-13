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
import java.util.Set;

import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.harvard.huh.specify.plugins.ItemCountsLabel;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.TableViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValSpinner;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.busrules.LoanBusRules;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.Triple;

/**
 * This class duplicates the logic in HUHBorrowReturnMaterialBusRules; it should be merged
 * 
 * @author mmk
 * @author david lowery
 * 
 * @code_status Alpha
 *
 * May 22, 2010
 *
 */
public class HUHLoanReturnPreparationBusRules extends BaseBusRules
{
    private final String ITEM_COUNTS_LABEL = "itemcountslabel";
    
    /**
     * 
     */
    public HUHLoanReturnPreparationBusRules()
    {
        super(LoanReturnPreparation.class);
    }

   /**
    * Initialize the busrules for LoanReturnPreparation and add listeners to the item, type and non
    * specimen counts on the loanReturnPreparation form to update the item counts plugin.
    * @param viewableArg
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
        	
            Component itemCnt = formViewObj.getControlByName("itemCount");
            Component typeCnt = formViewObj.getControlByName("typeCount");
            Component nonSpecimenCnt = formViewObj.getControlByName("nonSpecimenCount");
            
            //if (loanForm != null) {
	            //final ItemCountsLabel itemCountsLabel = (ItemCountsLabel)formViewObj.getMVParent().getMultiViewParent().getCurrentViewAsFormViewObj().getControlById("itemcountslabel");
	            
	            //dl: detect changes made to the counts fields on the return form and update itemcounts plugin
	            ChangeListener countChangeListener = new ChangeListener() {
	                @Override
	                public void stateChanged(ChangeEvent e)
	                {
	                    FormViewObj prepForm = formViewObj.getMVParent().getMultiViewParent().getCurrentViewAsFormViewObj();
	                    if (prepForm != null) {
		                    ItemCountsLabel itemCountsLabel = (ItemCountsLabel)prepForm.getControlById("itemcountslabel");
		                    if ((LoanPreparation)formViewObj.getParentDataObj() != null) {
		                    	itemCountsLabel.updateCounts(((LoanPreparation)formViewObj.getParentDataObj()).getLoan());
		                    }
	                    }
	                }
	            };
	            
	            if (itemCnt instanceof ValSpinner)
	            	((ValSpinner)itemCnt).addChangeListener(countChangeListener);
	            if (typeCnt instanceof ValSpinner)
	            	((ValSpinner)typeCnt).addChangeListener(countChangeListener);
	            if (nonSpecimenCnt instanceof ValSpinner)
	            	((ValSpinner)nonSpecimenCnt).addChangeListener(countChangeListener);
        }
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
    
    @Override
    public boolean isOkToSave(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj instanceof LoanPreparation) {
        	formViewObj.getDataFromUI();
        }
        return true;
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
    
    @Override
    public void afterFillForm(Object dataObj) {
    	super.afterFillForm(dataObj);
    	
    	if (formViewObj != null && dataObj != null) {
    		FormViewObj prepForm = formViewObj.getMVParent().getMultiViewParent().getCurrentViewAsFormViewObj();
    		
	    	ItemCountsLabel itemCountsLabel = (ItemCountsLabel)prepForm.getControlById("itemcountslabel");
	        itemCountsLabel.updateCounts(((LoanReturnPreparation)dataObj).getLoanPreparation().getLoan());
    	}
    }
}
