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
package edu.ku.brc.specify.datamodel.busrules;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Apr 1, 2008
 *
 */
public class TreeDefBusRules extends BaseBusRules
{
    protected int                   origDirection = -1;
    protected TreeDefIface<?, ?, ?> cachedTreeDef = null;
    
    /**
     * 
     */
    public TreeDefBusRules()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#initialize(edu.ku.brc.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        super.initialize(viewableArg);
        
        ValComboBox fnDirCBX = (ValComboBox)formViewObj.getControlByName("fnDirCBX");
        if (fnDirCBX != null)
        {
            DefaultComboBoxModel model = (DefaultComboBoxModel)fnDirCBX.getModel();
            model.addElement(UIRegistry.getResourceString("TTV_FORWARD"));
            model.addElement(UIRegistry.getResourceString("TTV_REVERSE"));
        
            fnDirCBX.getComboBox().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0)
                {
                    formViewObj.getValidator().setHasChanged(true);
                    formViewObj.getValidator().validateForm();
                    
                    checkForNumOfChanges();
                    
                    /*
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            checkForNumOfChanges();
                        }
                    });*/
                }
            });
        }
    }
    
    /**
     * 
     */
    protected void checkForNumOfChanges()
    {
        ValComboBox fnDirCBX = (ValComboBox)formViewObj.getControlByName("fnDirCBX");
        if (fnDirCBX != null)
        {
            int newDir = fnDirCBX.getComboBox().getSelectedIndex() == 0 ? TreeDefIface.FORWARD : TreeDefIface.REVERSE;
            if (newDir == origDirection)
            {
                return;
            }
        }
        
        SwingWorker workerThread = new SwingWorker()
        {
            @Override
            public Object construct()
            {
                DataProviderSessionIFace session = null;
                try
                {
                    Class<?> dataClass = cachedTreeDef.getNodeClass();
                    System.out.println(dataClass.getName());
                    String      sqlStr = "SELECT COUNT(id) FROM " + dataClass.getName();
                    DBTableInfo ti     = DBTableIdMgr.getInstance().getByClassName(dataClass.getName());
                    sqlStr = sqlStr + " WHERE " + QueryAdjusterForDomain.getInstance().getSpecialColumns(ti, true);
                    session = DataProviderFactory.getInstance().createSession();
                    Integer count = (Integer)session.getData(sqlStr);
                    
                    return count;
                    
                } catch (Exception ex)
                {
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(TreeDefBusRules.class, ex);
                    ex.printStackTrace();
                    
                } finally
                {
                    if (session != null)
                    {
                        session.close();
                    }
                }
                return null;
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public void finished()
            {
                //Object retVal = getValue();
                ///TreeHelper.f
                //System.out.println(retVal);
            }
        };
        
        // start the background task
        workerThread.start();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#afterFillForm(java.lang.Object)
     */
    @Override
    public void afterFillForm(final Object dataObj)
    {
        super.afterFillForm(dataObj);
        
        if (dataObj != null)
        {
            ValComboBox fnDirCBX = (ValComboBox)formViewObj.getControlByName("fnDirCBX");
            if (fnDirCBX != null)
            {
                TreeDefIface<?, ?, ?> treeDef = (TreeDefIface<?, ?, ?>)dataObj;
                origDirection = treeDef.getFullNameDirection();
                fnDirCBX.getComboBox().setSelectedIndex(origDirection == TreeDefIface.FORWARD ? 0 : 1);
                cachedTreeDef = treeDef;
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#afterSaveCommit(java.lang.Object)
     */
    @Override
    public boolean afterSaveCommit(final Object dataObj, final DataProviderSessionIFace session)
    {
        ValComboBox fnDirCBX = (ValComboBox)formViewObj.getControlByName("fnDirCBX");
        if (fnDirCBX != null)
        {
            int newDir = fnDirCBX.getComboBox().getSelectedIndex() == 0 ? TreeDefIface.FORWARD : TreeDefIface.REVERSE;
            if (newDir != origDirection)
            {
                TreeDefIface<?, ?, ?> treeDef = (TreeDefIface<?, ?, ?>)dataObj;
                treeDef.setFullNameDirection(newDir);
            }
        }
        return super.afterSaveCommit(dataObj, session);
    }
    
}
