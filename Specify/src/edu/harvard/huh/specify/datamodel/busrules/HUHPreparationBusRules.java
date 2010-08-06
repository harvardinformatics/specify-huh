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

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.PreparationBusRules;

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
                
                if (prep.getCountAmt() == null || prep.getCountAmt() == 0)
                {
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
                String[]    tableFieldList = gatherTableFieldsForDelete(new String[] {"preparation", "fragment"}, tableInfo);
                isOK = okToDelete(tableFieldList, dbObj.getId());
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
            boolean hasFragment = hasFragment(prep);

            if (hasFragment)
            {
                return true;
            }
            else
            {
                if (StringUtils.isEmpty(prep.getIdentifier()))
                {
                    reasonList.add(getLocalizedMessage("PreparationBusRules.NO_ITEMS"));
                    return false;
                }
                else return true;
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
        
        if (!hasFragment && StringUtils.isEmpty(prep.getIdentifier()))
        {
            reasonList.add(getLocalizedMessage("PreparationBusRules.NO_ITEMS"));
            status = STATUS.Error;
        }
        if (!STATUS.OK.equals(status)) return status;
        
        String barcodeError = HUHFragmentBusRules.checkForBarcodeError(prep);
        
        if (barcodeError != null)
        {
            reasonList.add(barcodeError);
            status = STATUS.Error;
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
}
