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

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.PreparationBusRules;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Feb 11, 2008
 *
 */
public class HUHPreparationBusRules extends PreparationBusRules
{
    public HUHPreparationBusRules()
    {
        super();
    }
    
    @Override
    public void beforeDelete(final Object dataObj, final DataProviderSessionIFace session)
    {
        if (dataObj instanceof Preparation)
        {
            Preparation prep = (Preparation) dataObj;
            
            for (Fragment fragment : prep.getFragments())
            {
                fragment.setPreparation(null);
            }
            prep.setFragments(null);
            prep.getPreparationAttachments().size();
        }
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
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(Object dataObj)
    {
        STATUS status = super.processBusinessRules(dataObj);
        
        if (!STATUS.OK.equals(status)) return status;
        
        status = isParentOK((FormDataObjIFace) dataObj);
        
        if (!STATUS.OK.equals(status)) return status;
        
        return isBarcodeOK((FormDataObjIFace) dataObj);
    }
    
    /** Either the Fragment or its Preparation must have a barcode, and the barcode
     *  must be unique across the union of all Fragment and Preparation objects .
     * @param dataObj the Fragment to check
     * @return whether the Fragment or its Preparation has a unique barcode
     */
    protected STATUS isBarcodeOK(final FormDataObjIFace dataObj)
    {
        if (dataObj instanceof Preparation)
        {
            Preparation preparation = (Preparation) dataObj;

            String fieldName = "identifier";
            String barcode = (String) FormHelper.getValue(dataObj, fieldName);

            Integer prepCount = 0;
            Integer fragmentCount = 0;
            if (!StringUtils.isEmpty(barcode))
            {
                // count preparations with this barcode
                prepCount =
                    HUHFragmentBusRules.getCountSql(Preparation.class, "preparationId", fieldName, barcode, preparation.getId());
                
                // count fragment records with this barcode
                fragmentCount =
                    HUHFragmentBusRules.getCountSql(Fragment.class, "fragmentId", fieldName, barcode, null);
            }

            if (fragmentCount + prepCount == 0)
            {
                // prep has the barcode
                return STATUS.OK;
            }
            else
            {
                reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Preparation.class, fieldName, barcode));
                return STATUS.Error;
            }
        }
        
        throw new IllegalArgumentException();
    }
    
    protected STATUS isParentOK(final FormDataObjIFace dataObj)
    {
        if (dataObj instanceof Preparation)
        {
            Preparation formPrep = (Preparation) dataObj;
            Preparation parentPrep = formPrep.getParent();
            
            while (parentPrep != null)
            {
                if (parentPrep.getPreparationId().equals(formPrep.getPreparationId()))
                {
                    reasonList.add(getLocalizedMessage("ANCESTOR_ERR"));
                    return STATUS.Error;
                }
                parentPrep = parentPrep.getParent();
            }
        }
        return STATUS.OK;
    }
}
