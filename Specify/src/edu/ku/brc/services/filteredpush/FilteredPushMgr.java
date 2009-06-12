/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ku.brc.services.filteredpush;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;

import edu.ku.brc.specify.Specify;

public class FilteredPushMgr
{
    protected static final Logger  log = Logger.getLogger(FilteredPushMgr.class);
            
    protected static FilteredPushMgr instance = null;
    
    protected static FilteredPushServiceProvider fpService;
    
    public static final String factoryName = "edu.ku.brc.services.filteredpush.FilteredPushMgrFactory"; //$NON-NLS-1$

    private boolean isFpOn;
    
    public static FilteredPushMgr getInstance()
    {
        if (instance != null)
        {
            return instance;
            
        }
        // else TODO: FP I don't know what this does or why 
        String factoryNameStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(factoryName);
                    }
                });
            
        if (factoryNameStr != null) 
        {
            try 
            {
                instance = (FilteredPushMgr)Class.forName(factoryNameStr).newInstance();
                return instance;
                 
            } catch (Exception e) 
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FilteredPushMgr.class, e);
                InternalError error = new InternalError("Can't instantiate FilteredPushMgr factory " + factoryNameStr); //$NON-NLS-1$
                error.initCause(e);
                throw error;
            }
        }
        return null;
    }
    
    public boolean isFpOn()
    {
        return isFpOn;
    }

    public boolean connectToFilteredPush()  // this should happen as a result of an FP command so that listeners know
    {
        log.debug("FilteredPushMgr.connectToFilteredPush");

        isFpOn = getFilteredPushService().connect();
        Specify.getSpecify().setFpConnectionStatus();
        
        return isFpOn;
    }
    
    public void disconnectFromFilteredPush() // this should happen as a result of an FP command so that listeners know
    {
        log.debug("FilteredPushMgr.disconnectFromFilteredPush");
        
        isFpOn = false;
        
        getFilteredPushService().disconnect();
        Specify.getSpecify().setFpConnectionStatus();
    }
    
    private FilteredPushServiceProvider getFilteredPushService()
    {
        if (fpService == null)
        {
            fpService = new FilteredPushServiceProvider();
        }
        
        return fpService;
    }
}
