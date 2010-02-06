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
package edu.ku.brc.specify.datamodel.busrules;

import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.specify.datamodel.Preparation;

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

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
        Preparation preparation = (Preparation) dataObj;
        if (preparation.getCountAmt() == null) preparation.setCountAmt(1);
        
        reasonList.clear();
        STATUS status =  super.processBusinessRules(dataObj);

        if (status == STATUS.OK)
        {
            status = processBusinessRules(null, dataObj, true);
        }
        return status;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object, java.lang.Object, boolean)
     */
    @Override
    public STATUS processBusinessRules(final Object parentDataObj, final Object dataObj, final boolean isEdit)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof Preparation))
        {
            return STATUS.Error;
        }
        
        STATUS duplicateNumberStatus = isCheckDuplicateNumberOK("sampleNumber", 
                                                                (FormDataObjIFace)dataObj, 
                                                                Preparation.class, 
                                                                "preparationId");
        
        return duplicateNumberStatus;
    }
}
