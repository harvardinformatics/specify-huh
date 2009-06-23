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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.dbsupport.RecordSetItemIFace;
import edu.ku.brc.specify.Specify;

public class FilteredPushMgr
{
    protected static final Logger  log = Logger.getLogger(FilteredPushMgr.class);
            
    protected static FilteredPushMgr instance = null;
    
    protected static FilteredPushServer fpServer;
    
    public static final String factoryName = "edu.ku.brc.services.filteredpush.FilteredPushMgrFactory"; //$NON-NLS-1$

    private boolean isFpOn;
    private boolean hasNewMessages;

    private static Set<FilteredPushListenerIFace> listeners = new HashSet<FilteredPushListenerIFace>();

    private FilteredPushListenerIFace operator = new FilteredPushListenerIFace()
                                                 {
                                                    @Override
                                                    public void notification(FilteredPushEvent e)
                                                    {
                                                        receivedMessage(e);
                                                    }
                                                 };
    
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

    public boolean hasNewMessages()
    {
        return hasNewMessages;
    }

    public boolean connectToFilteredPush()  // this should happen as a result of an FP command so that listeners know
    {
        log.debug("FilteredPushMgr.connectToFilteredPush");

        isFpOn = getFilteredPushServer().connect();
        Specify.getSpecify().setFpConnectionStatus();
        
        return isFpOn;
    }
    
    public void disconnectFromFilteredPush() // this should happen as a result of an FP command so that listeners know
    {
        log.debug("FilteredPushMgr.disconnectFromFilteredPush");
        
        isFpOn = false;
        
        getFilteredPushServer().disconnect();
        Specify.getSpecify().setFpConnectionStatus();
    }
    
    public void registerListener(FilteredPushListenerIFace listener)
    {
        if (listener != null) this.listeners.add(listener);
    }

    private FilteredPushServer getFilteredPushServer()
    {
        if (fpServer == null)
        {
            fpServer = new FilteredPushServer(getOperator());
        }
        
        return fpServer;
    }
    
    public void publish(RecordSetIFace recordSet)
    {
        log.debug("FilteredPushMgr.publish");
    }
    
    public void query(RecordSetItemIFace recordSetItem)
    {
        log.debug("FilteredPushMgr.query");
    }
    
    private FilteredPushListenerIFace getOperator()
    {
        return operator;
    }

    private void receivedMessage(FilteredPushEvent e)
    {
        log.debug("FilteredPushMgr.receivedMessage: " + e.getMessage());
        
        hasNewMessages = true;
        
        for (FilteredPushListenerIFace listener : getListeners())
        {
            listener.notification(e);
        }
    }
    
    private Set<FilteredPushListenerIFace> getListeners()
    {
        return listeners;
    }
}
