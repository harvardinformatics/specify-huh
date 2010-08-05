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
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.GiftPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.GiftPreparationBusRules;
import edu.ku.brc.ui.CommandListener;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author mmk
 *
 * @code_status Alpha
 *
 * July 12, 2010
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
            GiftPreparation gp = (GiftPreparation) dataObj;
            
            if (gp.getTypeCount() == null) gp.setTypeCount(0);
            if (gp.getNonSpecimenCount() == null) gp.setNonSpecimenCount(0);
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#beforeFormFill(edu.ku.brc.ui.forms.Viewable)
     */
    @Override
    public void beforeFormFill()
    {
        if (formViewObj != null && formViewObj.getDataObj() instanceof GiftPreparation)
        {
            GiftPreparation gp = (GiftPreparation) formViewObj.getDataObj();
            
            if (gp.getTypeCount() == null) gp.setTypeCount(0);
            if (gp.getNonSpecimenCount() == null) gp.setNonSpecimenCount(0);
        }
    }
    
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj != null && dataObj instanceof GiftPreparation)
        {
            GiftPreparation gp = (GiftPreparation) dataObj;

            Preparation p = gp.getPreparation();
         
            if (p != null)
            {                
                try
                {
                    if (p.getId() != null)
                    {
                        Preparation mergedP = session.merge(p);
                        gp.setPreparation(mergedP);
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
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHGiftPreparationBusRules.class, ex);
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
            GiftPreparation gp = (GiftPreparation)dataObj;
            
            Preparation p = gp.getPreparation();
            if (p != null && p.getPrepType().getName().equalsIgnoreCase("lot"))
            {
                p.getGiftPreparations().remove(gp);
                try
                {
                    session.delete(p);
                    return true;

                } catch (Exception ex)
                {
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHGiftPreparationBusRules.class, ex);
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

        if (dataObj != null && dataObj instanceof GiftPreparation)
        {
            GiftPreparation gp = (GiftPreparation) dataObj;

            if (gp.getPreparation() != null)
            {
                if ((gp.getItemCount() != null && gp.getItemCount() > 0) ||
                    (gp.getTypeCount() != null && gp.getTypeCount() > 0) ||
                    (gp.getNonSpecimenCount() != null && gp.getNonSpecimenCount() > 0))
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
}
