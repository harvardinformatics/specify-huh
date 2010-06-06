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

import java.util.HashSet;
import java.util.Set;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;

/**
 * @author mkelly
 *
 * @code_status Alpha
 *
 * Created Date: May 4, 2010
 *
 */
public class HUHMixedPrepBusRules extends HUHCollectionObjectBusRules
{
    /**
     * Constructor.
     */
    public HUHMixedPrepBusRules()
    {
        super();
    }
    
    @Override
    public boolean beforeDeleteCommit(Object dataObj, DataProviderSessionIFace session) throws Exception
    {
        boolean ok = super.beforeDeleteCommit(dataObj, session);
        
        if (ok)
        {
            CollectionObject collObj = (CollectionObject)dataObj;
            if (collObj.getFragments().size() == 1)
            {
                Fragment fragment = collObj.getFragments().iterator().next();
                Preparation prep = fragment.getPreparation();
                
                if (prep != null && prep.getFragments().size() == 1)
                {
                    try
                    {
                        session.delete(prep);
                        return true;
                        
                    } catch (Exception ex)
                    {
                        edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                        ex.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return ok;
    }
    
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {        
        CollectionObject collObj = (CollectionObject) dataObj;
        
        if (collObj != null)
        {
            // temporarilty remove the collection object's fragments
            Set<Fragment> unsavedFragments = collObj.getFragments();
            Set<Fragment> savedFragments = new HashSet<Fragment>();
            collObj.setFragments(savedFragments);

            // save the collecting event
            CollectingEvent collEvt = collObj.getCollectingEvent();
            if (collEvt != null)
            {
                // save the locality
                Locality loc = collEvt.getLocality();
                if (loc != null)
                {
                    loc = (Locality) HUHFragmentBusRules.saveObject(loc, session);
                }
                collEvt = (CollectingEvent) HUHFragmentBusRules.saveObject(collEvt, session);
            }
            
            // save the collection object
            collObj = (CollectionObject) HUHFragmentBusRules.saveObject(collObj, session);

            // save the fragment/preparations
            for (Fragment fragment : unsavedFragments)
            {
                Preparation prep = fragment.getPreparation();

                if (prep != null)
                {
                    prep.setCountAmt(1);

                    // temporarily detach the preparation's fragments
                    Set<Fragment> prepFragments = prep.getFragments();
                    prep.setFragments(new HashSet<Fragment>());

                    // save the preparation
                    prep = (Preparation) HUHFragmentBusRules.saveObject(prep, session);

                    // reattach the preparation's fragments
                    prep.setFragments(prepFragments);
                    fragment.setPreparation(prep);
                }
                
                // save the fragment
                fragment = (Fragment) HUHFragmentBusRules.saveObject(fragment, session);
                savedFragments.add(fragment);
            }
            
            // reattach the collection object's fragments
            collObj.setFragments(savedFragments);
            
            // allow the merge to proceed with reattached fragments
        }
    }
}
