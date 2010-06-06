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

import java.util.HashSet;
import java.util.List;

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
    
/*    @Override
    public void beforeDelete(final Object dataObj, final DataProviderSessionIFace session)
    {
        if (dataObj instanceof Preparation)
        {
            Preparation prep = (Preparation) dataObj;
            
            for (Fragment fragment : prep.getFragments())
            {
                fragment.setPreparation(null);
            }
            prep.setFragments(new HashSet<Fragment>());
            prep.getPreparationAttachments().size();
        }
    }*/
    
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
                    return false;
                }
            }
        }
        return super.okToEnableDelete(dataObj);
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
        
        status = isParentOK(prep, reasonList);
        
        if (prep.getFragments().size() > 0)
        {
            status = HUHFragmentBusRules.checkBarcode(prep, reasonList);
        }
        
        return status;
    }
    
    protected static STATUS isParentOK(final Preparation prep, List<String> reasonList)
    {

        Preparation parentPrep = prep.getParent();

        while (parentPrep != null)
        {
            if (parentPrep.getPreparationId().equals(prep.getPreparationId()))
            {
                reasonList.add(getLocalizedMessage("ANCESTOR_ERR"));
                return STATUS.Error;
            }
            parentPrep = parentPrep.getParent();
        }
        return STATUS.OK;
    }
}
