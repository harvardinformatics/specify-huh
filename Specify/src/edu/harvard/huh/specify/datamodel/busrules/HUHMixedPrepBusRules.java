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

import java.util.Calendar;

import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Preparation;

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
    public void addChildrenToNewDataObjects(Object newDataObj)
    {
        CollectionObject collObj = (CollectionObject) newDataObj;

        if (collObj != null)
        {
            Agent agent = Agent.getUserAgent();
            collObj.setCataloger(agent);
            collObj.setCatalogedDate(Calendar.getInstance());
            collObj.setCatalogedDatePrecision((byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal());
        }
        
        CollectingEvent collEvt = collObj.getCollectingEvent();
        if (collEvt == null)
        {
            collEvt = new CollectingEvent();
            collEvt.initialize();
            collObj.addReference(collEvt, "collectingEvent");
        }

        Locality loc = collEvt.getLocality();
        if (loc == null)
        {
            loc = new Locality();
            loc.initialize();
            collEvt.addReference(loc, "locality");
        }
    }

    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {        
        CollectionObject collObj = (CollectionObject) dataObj;
        
        if (collObj != null)
        {
            for (Fragment fragment : collObj.getFragments())
            {

                HUHFragmentBusRules.saveObject(fragment, session);
                
                Preparation prep = fragment.getPreparation();
                if (prep != null)
                {
                    prep.setCountAmt(1);
                    prep = (Preparation) HUHFragmentBusRules.saveObject(prep, session);
                    fragment.setPreparation(prep);
                }
            }
            
            CollectingEvent collEvt = collObj.getCollectingEvent();
            if (collEvt != null)
            {
                collEvt = (CollectingEvent) HUHFragmentBusRules.saveObject(collEvt, session);
                collObj.setCollectingEvent(collEvt);
                
                Locality loc = collEvt.getLocality();
                loc = (Locality) HUHFragmentBusRules.saveObject(loc, session);
                collEvt.setLocality(loc);
            }
        }

    }
}
