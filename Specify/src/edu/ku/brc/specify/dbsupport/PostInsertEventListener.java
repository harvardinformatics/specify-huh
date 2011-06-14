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
package edu.ku.brc.specify.dbsupport;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.hibernate.event.PostInsertEvent;

import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.SpAuditLog;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;

/**
 * This class listens for Insert events from Hibernate so it can update the Lucene index.  This
 * mechanism is also being used by the AttachmentManager system as a trigger to copy the original
 * files into the storage storage.
 * 
 * @author rods
 *
 * @code_status Complete
 *
 * Created Date: Nov 2, 2006
 *
 */
public class PostInsertEventListener implements org.hibernate.event.PostInsertEventListener
{
    private static final Logger log = Logger.getLogger(PostInsertEventListener.class);
    
    private static boolean isAuditOn = true;
    
    public static final String DB_CMD_TYPE       = "Database"; //$NON-NLS-1$
    public static final String SAVE_CMD_ACT      = "Save"; //$NON-NLS-1$
    public static final String INSERT_CMD_ACT    = "Insert"; //$NON-NLS-1$
    public static final String DELETE_CMD_ACT    = "Delete"; //$NON-NLS-1$
    public static final String UPDATE_CMD_ACT    = "Update"; //$NON-NLS-1$
    
    /* (non-Javadoc)
     * @see org.hibernate.event.PostInsertEventListener#onPostInsert(org.hibernate.event.PostInsertEvent)
     */
    @Override
    public void onPostInsert(final PostInsertEvent obj)
    {
        if (obj.getEntity() instanceof FormDataObjIFace)
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    CommandDispatcher.dispatch(new CommandAction(PostInsertEventListener.DB_CMD_TYPE, PostInsertEventListener.INSERT_CMD_ACT, obj.getEntity())); 
                }
            });
            
            if (PostInsertEventListener.isAuditOn())
            {
                if (((FormDataObjIFace)obj.getEntity()).isChangeNotifier())
                {
                    saveOnAuditTrail((byte)0, obj.getEntity());
                }
            }
        }
    }
    
    /**
     * @param action
     * @param description
     * @param dObjArg
     */
    public static void saveOnAuditTrail(final Byte    action,
                                        final Object  dObjArg)
    {
        if (dObjArg instanceof FormDataObjIFace)
        {
            final FormDataObjIFace dObj    = (FormDataObjIFace)dObjArg;
            
            //javax.swing.SwingWorker<Integer, Integer> auditWorker = new javax.swing.SwingWorker<Integer, Integer>()
            //{
                //@Override
                //protected Integer doInBackground() throws Exception
                //{
                    DataProviderSessionIFace localSession = null;
                    try
                    {
                        localSession = DataProviderFactory.getInstance().createSession();
                        
                        localSession.beginTransaction();
                        
                        SpAuditLog spal = new SpAuditLog();
                        spal.initialize();
                        
                        spal.setRecordId(dObj.getId());
                        spal.setTableNum((short)dObj.getTableId());
                        
                        Integer pId           = dObj.getParentId();
                        Integer parentTableId = null;
                        spal.setParentRecordId(dObj.getParentId());
                        if (pId != null)
                        {
                            parentTableId = dObj.getParentTableId();
                        }
                        spal.setParentTableNum(parentTableId != null ? parentTableId.shortValue() : null);
                        
                        spal.setRecordVersion(dObj.getVersion() == null ? 0 : dObj.getVersion());
                        spal.setAction(action);
                        
                        
                        localSession.saveOrUpdate(spal);
                        localSession.commit();
                        
                    } catch (Exception ex)
                    {
                        localSession.rollback();
                        ex.printStackTrace();
                        log.error(ex);
                        
                    } finally
                    {
                        if (localSession != null)
                        {
                            localSession.close();
                        }
                    }
                    //return null;
                //}
            //};
            //auditWorker.execute();
            
        } else
        {
            log.error("Can't audit data object, not instanceof FormDataObjIFace: "+(dObjArg != null ? dObjArg.getClass().getSimpleName() : "null"));
        }
    }

    /**
     * @return the isAuditOn
     */
    public static boolean isAuditOn()
    {
        return isAuditOn;
    }

    /**
     * @param isAuditOn the isAuditOn to set
     */
    public static void setAuditOn(boolean isAuditOn)
    {
        PostInsertEventListener.isAuditOn = isAuditOn;
    }
    
    
}
