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

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.util.Calendar;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;

/**
 * @author mkelly
 *
 * @code_status Alpha
 *
 * Created Date: Mar 0, 2010
 *
 */
public class HUHCollectionObjectBusRules extends CollectionObjectBusRules
{
    /**:
     * Constructor.
     */
    public HUHCollectionObjectBusRules()
    {
        super();
    }

    @Override
    public void addChildrenToNewDataObjects(Object newDataObj)
    { 
        CollectionObject collectionObject = (CollectionObject) newDataObj;

        fillNewCollectionObject(collectionObject);
    }
    
    private void fillNewCollectionObject(CollectionObject collectionObject)
    {
        if (collectionObject != null)
        {
            if (collectionObject.getCataloger() == null)
            {
                Agent agent = Agent.getUserAgent();
                collectionObject.setCataloger(agent);
                collectionObject.setCatalogedDate(Calendar.getInstance());
                collectionObject.setCatalogedDatePrecision((byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal());
            }
            
            CollectingEvent collectingEvent = collectionObject.getCollectingEvent();
            if (collectingEvent == null)
            {
                collectingEvent = new CollectingEvent();
                collectingEvent.initialize();
                collectionObject.addReference(collectingEvent, "collectingEvent");
            }

            Locality locality = collectingEvent.getLocality();
            if (locality == null)
            {
                locality = new Locality();
                locality.initialize();
                collectingEvent.addReference(locality, "locality");
            }
            //collectionObject.getOtherIdentifiers().size();
        }
    }
    
/*    @Override
    public void afterDeleteCommit(final Object dataObj)
    {
        CollectionObject collObj = (CollectionObject) dataObj;
        
        if (collObj != null)
        {
            for (Fragment fragment : collObj.getFragments())
            {
                collObj.getFragments().remove(fragment);
            }
        }
    }*/
    
    @Override
    public void beforeFormFill()
    {
        if (formViewObj != null)
        {
            CollectionObject collObj = (CollectionObject) formViewObj.getDataObj();
            
            fillNewCollectionObject(collObj);
        }
    }
    
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        CollectionObject colObj = (CollectionObject)dataObj;
        if (AppContextMgr.getInstance().getClassObject(Collection.class).getIsEmbeddedCollectingEvent())
        {
            CollectingEvent ce = colObj.getCollectingEvent();
            if (ce != null)
            {
                try
                {
                    Locality loc = ce.getLocality();
                    if (loc != null)
                    {
                        if (loc.getId() != null)
                        {
                            Locality mergedLoc = session.merge(ce.getLocality());
                            ce.setLocality(mergedLoc);
                        }
                        else
                        {
                            session.save(ce.getLocality());
                        }
                    }
                    if (ce.getId() != null)
                    {
                        CollectingEvent mergedCE = session.merge(colObj.getCollectingEvent());
                        colObj.setCollectingEvent(mergedCE);
                    } else
                    {
                        session.save(colObj.getCollectingEvent());
                    }
                    
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.datamodel.busrules.COllectionObjectBusRules(java.lang.Object)
     */
    protected STATUS checkDeterminations(final Object dataObj)
    {
        /*// check that a current determination exists
        if (((CollectionObject) dataObj).getFragments().size() > 0)
        {
            for (Fragment fragment : ((CollectionObject) dataObj).getFragments())
            {
                if (fragment.getDeterminations().size() > 0)
                {
                    int currents = 0;
                    for (Determination det : fragment.getDeterminations())
                    {
                        if (det.isCurrentDet())
                        {
                            currents++;
                        }
                    }
                    if (currents != 1)
                    {
                        if (currents == 0)
                        {
                            reasonList.add(getResourceString("CollectionObjectBusRules.CURRENT_DET_REQUIRED"));
                        }
                        else
                        {
                            reasonList.add(getResourceString("CollectionObjectBusRules.ONLY_ONE_CURRENT_DET"));
                        }
                        return STATUS.Warning;
                    }
                }
            }
        }*/
        return STATUS.OK;
    }
    
    public boolean okToEnableDelete(Object dataObj)
    {
        if (dataObj != null)
        {
            if (dataObj instanceof CollectionObject)
            {
                reasonList.clear();

                CollectionObject collObj = (CollectionObject) dataObj;
                if (collObj.getFragments().size() > 1)
                {
                    return false;
                }
            }
        }
        return super.okToEnableDelete(dataObj);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object, java.lang.Object, boolean)
     */
    @Override
    public STATUS processBusinessRules(final Object parentDataObj, final Object dataObj, final boolean isEdit)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof CollectionObject))
        {
            return STATUS.Error;
        }
        
        return STATUS.OK;
    }
}
