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

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.PreparationBusRules;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author mkelly
 *
 * @code_status Alpha
 *
 * June 4, 2010
 *
 */
public class HUHPreparationBusRules extends PreparationBusRules
{
	// these are keys in resources_en.properties
	private static final String DELETE_PREP_ANYWAY = "DELETE_PREP_ANYWAY";
	private static final String DONT_DELETE_PREP = "DONT_DELETE_PREP";
	
    public HUHPreparationBusRules()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#beforeDeleteCommit(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public boolean beforeDeleteCommit(Object dataObj, DataProviderSessionIFace session) throws Exception
    {
        boolean ok = super.beforeDeleteCommit(dataObj, session);

        if (ok)
        {
            if (dataObj instanceof Preparation)
            {
                Preparation prep = (Preparation) dataObj;

                for (Fragment fragment : prep.getFragments())
                {
                    fragment.setPreparation(null);
                    prep.getFragments().remove(fragment);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj != null)
        {
            if (dataObj instanceof Preparation)
            {
                Preparation prep = (Preparation) dataObj;
                
                /* dl: if the preparation is of type "Lot" and it has a value of null, default to 0 for a count
                   otherwise use the count specified. If it is not a lot, it should contain at least one item,
                   set the count to default to 1 if 0 or null is found. */
                if (prep.getPrepType().getName().equals("Lot")) {
                	if (prep.getCountAmt() == null)
                		prep.setCountAmt(0);
                } else if (prep.getCountAmt() == null || prep.getCountAmt() == 0) {
                    prep.setCountAmt(1);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#okToDelete(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace, edu.ku.brc.ui.forms.BusinessRulesOkDeleteIFace)
     */
    @Override
    public void okToDelete(final Object dataObj,
                           final DataProviderSessionIFace session,
                           final BusinessRulesOkDeleteIFace deletable)
    {
        reasonList.clear();
        
        boolean isOK = false;
        if (deletable != null)
        {
            FormDataObjIFace dbObj = (FormDataObjIFace)dataObj;
            
            Integer id = dbObj.getId();
            if (id == null)
            {
                isOK = true;
                
            } else
            {
                DBTableInfo tableInfo      = DBTableIdMgr.getInstance().getInfoById(Preparation.getClassTableId());
                String[]    tableFieldList = gatherTableFieldsForDelete(new String[] {"preparation", "fragment", "loanpreparation"}, tableInfo);
                isOK = okToDelete(tableFieldList, dbObj.getId());
                
                // The above listing of preparation, fragment, and loanpreparation indicates that
                // we ignore references from those tables if they exist and delete anyway.  But we
                // probably still want to warn, at least on loanpreparation.  So we check again,
                // but this time we don't ignore references to loanpreparation.
                if (isOK) {
                	tableFieldList = gatherTableFieldsForDelete(new String[] {"preparation", "fragment"}, tableInfo);
                	boolean noLoanPrepsHere = okToDelete( tableFieldList, dbObj.getId() );
                	if (! noLoanPrepsHere)  {
                		boolean iWantToIgnoreThem = UIRegistry.displayConfirmLocalized(getResourceString("WARNING"),
                				"There are loans associated with this preparation.  If you delete them, they will disappear from the loans.\n\n"
                				+ "If you are trying to merge preparations, you should come back to these loans and re-add the merged preparation.\n\n"
                				+ "To find a list of loans associated with this preparation, make sure 'Loans by Barcode' is active in the\n"
                				+ "Simple Search config and perform a search over 'All' BEFORE YOU DELETE THIS PREPARATION.\n\n" 
                				+ "Ignore them and delete anyway?", DELETE_PREP_ANYWAY, // TODO: localization
                					DONT_DELETE_PREP, JOptionPane.WARNING_MESSAGE);

                		if (! iWantToIgnoreThem) isOK = false;
                	}
                }
            }
            deletable.doDeleteDataObj(dataObj, session, isOK);
            
        } else
        {
            super.okToDelete(dataObj, session, deletable);
        }
    }
    
    public boolean okToEnableDelete(Object dataObj)
    {
        if (dataObj != null)
        {
            if (dataObj instanceof Preparation)
            {
                reasonList.clear();

                Preparation prep = (Preparation) dataObj;
                if (prep.getFragments().size() > 1)
                {
                    reasonList.add(getResourceString("PreparationBusRules.ATTACHED_ITEMS"));
                    return false;
                }
            }
        }
        return super.okToEnableDelete(dataObj);
    }
    
    @Override
    public boolean isOkToSave(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj instanceof Preparation)
        {
            Preparation prep = (Preparation) dataObj;
            
            boolean hasCycle = hasCycle(prep);
            boolean hasFragment = hasFragment(prep);
            boolean isLot = isLot(prep);
            
            if (hasCycle)
            {
                reasonList.add(getLocalizedMessage("PreparationBusRules.ANCESTOR_ERR"));
                return false;
            }

            if (!hasFragment && StringUtils.isEmpty(prep.getIdentifier()) && !isLot)
            {
                reasonList.add(getLocalizedMessage("PreparationBusRules.NO_ITEMS"));
                return false;
            }
            
            if (hasFragment || !StringUtils.isEmpty(prep.getIdentifier()))
            {
                String barcodeError = HUHFragmentBusRules.checkForBarcodeError(prep);

                if (barcodeError != null)
                {
                    reasonList.add(barcodeError);
                    return false;
                }
            }
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(Object dataObj)
    {
        STATUS status = super.processBusinessRules(dataObj);
        
        if (!STATUS.OK.equals(status)) return status;
        
        Preparation prep = (Preparation) dataObj;
        
        boolean hasCycle = hasCycle(prep);
        
        if (hasCycle)
        {
            reasonList.add(getLocalizedMessage("PreparationBusRules.ANCESTOR_ERR"));
            status = STATUS.Error;
        }
        if (!STATUS.OK.equals(status)) return status;
        
        boolean hasFragment = hasFragment(prep);
        boolean isLot = isLot(prep);
        
        if (!hasFragment && StringUtils.isEmpty(prep.getIdentifier()) && !isLot)
        {
            reasonList.add(getLocalizedMessage("PreparationBusRules.NO_ITEMS"));
            status = STATUS.Error;
        }
        if (!STATUS.OK.equals(status)) return status;
        
        if (hasFragment || !StringUtils.isEmpty(prep.getIdentifier()))
        {
            String barcodeError = HUHFragmentBusRules.checkForBarcodeError(prep);

            if (barcodeError != null)
            {
                reasonList.add(barcodeError);
                status = STATUS.Error;
            }        
        }
        return status;
    }
    
    protected static boolean hasCycle(final Preparation prep)
    {
        Preparation parentPrep = prep.getParent();
        
        Vector<Integer> descendantIds = new Vector<Integer>();
        
        while (parentPrep != null)
        {
            Integer parentId = parentPrep.getPreparationId();
            
            for (Integer descendantId : descendantIds)
            {
                if (parentId.equals(descendantId)) return true;
            }
            parentPrep = parentPrep.getParent();
            descendantIds.add(parentId);
        }
        return false;
    }
    
    protected boolean hasFragment(Preparation prep)
    {
        if (prep != null)
        {
            if (prep.getFragments() == null || prep.getFragments().size() < 1) return false;
        }
        return true;
    }
    
    protected boolean isLot(Preparation prep)
    {
        PrepType prepType = prep.getPrepType();
        if (prepType != null && "lot".equalsIgnoreCase(prepType.getName()))
        {
            return true;
        }
        return false;
    }
}
