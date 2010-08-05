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

import org.apache.log4j.Logger;

import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.AccessionPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author mmk
 *
 * @code_status Alpha
 *
 * Jul 10, 2010
 *
 */
public class HUHAccessionPreparationBusRules extends BaseBusRules
{
    protected Logger log = Logger.getLogger(HUHAccessionPreparationBusRules.class);
    
    /**
     * 
     */
    public HUHAccessionPreparationBusRules()
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
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#addChildrenToNewDataObjects(java.lang.Object)
     */
    @Override
    public void addChildrenToNewDataObjects(final Object newDataObj)
    {
        AccessionPreparation ap = (AccessionPreparation) newDataObj;
        
        if (ap.getDiscardCount() == null) ap.setDiscardCount((short) 0);
        if (ap.getDistributeCount() == null) ap.setDistributeCount((short) 0);
        if (ap.getReturnCount() == null) ap.setReturnCount((short) 0);
    }
    
    @Override
    public void afterFillForm(Object dataObj)
    {
        if (dataObj != null && dataObj instanceof AccessionPreparation)
        {
            AccessionPreparation ap = (AccessionPreparation) dataObj;
            
            if (ap.getTypeCount() == null) ap.setTypeCount((short) 0);
            if (ap.getNonSpecimenCount() == null) ap.setNonSpecimenCount((short) 0);
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#beforeFormFill(edu.ku.brc.ui.forms.Viewable)
     */
    @Override
    public void beforeFormFill()
    {
        if (formViewObj != null && formViewObj.getDataObj() instanceof AccessionPreparation)
        {
            AccessionPreparation ap = (AccessionPreparation) formViewObj.getDataObj();
            
            if (ap.getTypeCount() == null) ap.setTypeCount((short) 0);
            if (ap.getNonSpecimenCount() == null) ap.setNonSpecimenCount((short) 0);
        }
    }
    
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj != null && dataObj instanceof AccessionPreparation)
        {
            AccessionPreparation ap = (AccessionPreparation) dataObj;
            
            if (ap.getDiscardCount() == null) ap.setDiscardCount((short) 0);
            if (ap.getDistributeCount() == null) ap.setDistributeCount((short) 0);
            if (ap.getReturnCount() == null) ap.setReturnCount((short) 0);

            Preparation p = ap.getPreparation();
         
            if (p != null)
            {                
                try
                {
                    if (p.getId() != null)
                    {
                        Preparation mergedP = session.merge(p);
                        ap.setPreparation(mergedP);
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
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHAccessionPreparationBusRules.class, ex);
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
            AccessionPreparation ap = (AccessionPreparation)dataObj;
            
            Preparation p = ap.getPreparation();
            if (p != null && p.getPrepType().getName().equalsIgnoreCase("lot"))
            {
                p.getAccessionPreparations().remove(ap);
                try
                {
                    session.delete(p);
                    return true;

                } catch (Exception ex)
                {
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHAccessionPreparationBusRules.class, ex);
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

        if (dataObj != null && dataObj instanceof AccessionPreparation)
        {
            AccessionPreparation ap = (AccessionPreparation) dataObj;

            if (ap.getPreparation() != null)
            {
                if ((ap.getItemCount() != null && ap.getItemCount() > 0) ||
                    (ap.getTypeCount() != null && ap.getTypeCount() > 0) ||
                    (ap.getNonSpecimenCount() != null && ap.getNonSpecimenCount() > 0))
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
