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
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.TableViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.ExchangeOutPreparation;
import edu.ku.brc.specify.datamodel.busrules.ExchangeOutBusRules;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandListener;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Jan 29, 2007
 *
 */
public class HUHExchangeOutPreparationBusRules extends BaseBusRules implements CommandListener
{
    
    /**
     * 
     */
    public HUHExchangeOutPreparationBusRules()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        super.initialize(viewableArg);
    }
    
    @Override
    public void afterFillForm(Object dataObj)
    {
        if (dataObj != null && dataObj instanceof ExchangeOutPreparation)
        {
            ExchangeOutPreparation exchPrep = (ExchangeOutPreparation) dataObj;
            
            if (exchPrep.getTypeCount() == null) exchPrep.setTypeCount(0);
            if (exchPrep.getNonSpecimenCount() == null) exchPrep.setNonSpecimenCount(0);
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CommandListener#doCommand(edu.ku.brc.ui.CommandAction)
     */
    @Override
    public void doCommand(CommandAction cmdAction)
    {
        if (cmdAction.isType(ExchangeOutBusRules.CMDTYPE) && cmdAction.isAction("REFRESH_EXCH_OUT_PREPS"))
        {
            if (formViewObj != null)
            {
                MultiView giftMV = formViewObj.getMVParent().getMultiViewParent();
                if (giftMV != null)
                {
                    if (formViewObj.getValidator() != null)
                    {
                        ExchangeOut exchangeOut = (ExchangeOut)giftMV.getData();
                        formViewObj.setDataObj(exchangeOut.getExchangeOutPreparations());
                        formViewObj.getValidator().setHasChanged(true);
                        formViewObj.getValidator().validateRoot();
                    }
                }

            } else if (viewable instanceof TableViewObj)
            {
                TableViewObj tvo = (TableViewObj)viewable;
                // Make sure the Loan form knows there is a change
                MultiView giftMV = tvo.getMVParent().getMultiViewParent();
                giftMV.getCurrentValidator().setHasChanged(true);
                giftMV.getCurrentValidator().validateRoot();
                
                // Refresh list in the grid
                tvo.refreshDataList();
            }
        }
    }
}
