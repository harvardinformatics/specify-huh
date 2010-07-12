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
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.ExchangeOutPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.ExchangeOutBusRules;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandListener;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author mmk
 *
 * @code_status Alpha
 *
 * July 10, 2010
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
            ExchangeOutPreparation eop = (ExchangeOutPreparation) dataObj;
            
            if (eop.getTypeCount() == null) eop.setTypeCount(0);
            if (eop.getNonSpecimenCount() == null) eop.setNonSpecimenCount(0);
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#beforeFormFill(edu.ku.brc.ui.forms.Viewable)
     */
    @Override
    public void beforeFormFill()
    {
        if (formViewObj != null && formViewObj.getDataObj() instanceof ExchangeOutPreparation)
        {
            ExchangeOutPreparation eop = (ExchangeOutPreparation) formViewObj.getDataObj();
            
            if (eop.getTypeCount() == null) eop.setTypeCount(0);
            if (eop.getNonSpecimenCount() == null) eop.setNonSpecimenCount(0);
        }
    }
    
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj != null && dataObj instanceof ExchangeOutPreparation)
        {
            ExchangeOutPreparation eop = (ExchangeOutPreparation) dataObj;

            Preparation p = eop.getPreparation();
         
            if (p != null)
            {                
                try
                {
                    if (p.getId() != null)
                    {
                        Preparation mergedP = session.merge(p);
                        eop.setPreparation(mergedP);
                    }
                    else
                    {
                        session.save(p);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHExchangeOutPreparationBusRules.class, ex);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.AttachmentOwnerBaseBusRules#beforeDeleteCommit(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public boolean beforeDeleteCommit(Object dataObj, DataProviderSessionIFace session) throws Exception
    {
        boolean ok = super.beforeDeleteCommit(dataObj, session);
        
        if (ok)
        {
            ExchangeOutPreparation eop = (ExchangeOutPreparation)dataObj;
            
            Preparation p = eop.getPreparation();
            if (p != null && p.getPrepType().getName().equalsIgnoreCase("lot"))
            {
                p.getExchangeOutPreparations().remove(eop);
                try
                {
                    session.delete(p);
                    return true;

                } catch (Exception ex)
                {
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHExchangeOutPreparationBusRules.class, ex);
                    ex.printStackTrace();
                    return false;
                }
            }

        }
        return ok;
    }
 
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#processBusinessRules(java.lang.Object)
     */
    public STATUS processBusinessRules(final Object dataObj)
    {
        STATUS status = super.processBusinessRules(dataObj);

        if (! STATUS.OK.equals(status))
        {
            return status;
        }

        if (dataObj != null && dataObj instanceof ExchangeOutPreparation)
        {
            ExchangeOutPreparation eop = (ExchangeOutPreparation) dataObj;

            if (eop.getPreparation() != null)
            {
                if ((eop.getItemCount() != null && eop.getItemCount() > 0) ||
                        (eop.getTypeCount() != null && eop.getTypeCount() > 0) ||
                        (eop.getNonSpecimenCount() != null && eop.getNonSpecimenCount() > 0))
                {
                    status = STATUS.OK;
                }
                else
                {
                    reasonList.add(UIRegistry.getLocalizedMessage("ZERO_COUNT"));
                    status = STATUS.Error;
                }
            }
        }

        return status;
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
