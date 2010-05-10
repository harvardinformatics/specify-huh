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

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
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
public class HUHMixedCollBusRules extends HUHPreparationBusRules
{
    public HUHMixedCollBusRules()
    {
        super();
    }

    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        Preparation prep = (Preparation) dataObj;
        
        for (Fragment fragment : prep.getFragments())
        {
            CollectionObject collObj = fragment.getCollectionObject();
            if (collObj.getCollection() == null)
            {
                Collection catSeries = AppContextMgr.getInstance().getClassObject(Collection.class);
                collObj.setCollection(catSeries); 
            }
            
            CollectingEvent collEvt = collObj.getCollectingEvent();
            if (collEvt != null)
            {
                Locality loc = collEvt.getLocality();
                if (loc != null)
                {
                    loc = (Locality) HUHFragmentBusRules.saveObject(loc, session);
                }
                collEvt = (CollectingEvent) HUHFragmentBusRules.saveObject(collEvt, session);                
            }

            collObj = (CollectionObject) HUHFragmentBusRules.saveObject(collObj, session);
        }
    }
}
