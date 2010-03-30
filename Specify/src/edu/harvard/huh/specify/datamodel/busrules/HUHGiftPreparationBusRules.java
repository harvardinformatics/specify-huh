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

import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.specify.datamodel.GiftPreparation;
import edu.ku.brc.specify.datamodel.busrules.GiftPreparationBusRules;
import edu.ku.brc.ui.CommandListener;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Jan 29, 2007
 *
 */
public class HUHGiftPreparationBusRules extends GiftPreparationBusRules implements CommandListener
{
    
    /**
     * 
     */
    public HUHGiftPreparationBusRules()
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
    }
    
    @Override
    public void afterFillForm(Object dataObj)
    {
        if (dataObj != null && dataObj instanceof GiftPreparation)
        {
            GiftPreparation giftPrep = (GiftPreparation) dataObj;
            
            if (giftPrep.getTypeCount() == null) giftPrep.setTypeCount(0);
            if (giftPrep.getNonSpecimenCount() == null) giftPrep.setNonSpecimenCount(0);
        }
    }
}
