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
package edu.ku.brc.specify.prefs;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.GenericLSIDGeneratorFactory;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.TaskMgr;
import edu.ku.brc.af.core.Taskable;
import edu.ku.brc.af.core.GenericLSIDGeneratorFactory.CATEGORY_TYPE;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.prefs.GenericPrefsPanel;
import edu.ku.brc.af.ui.forms.validation.DataChangeNotifier;
import edu.ku.brc.af.ui.forms.validation.FormValidator;
import edu.ku.brc.af.ui.forms.validation.UIValidatable;
import edu.ku.brc.af.ui.forms.validation.ValCheckBox;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.dnd.GhostGlassPane;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Jul 7, 2009
 *
 */
public class LSIDPrefsPanel extends GenericPrefsPanel
{
    protected static String PREF_NAME_PREFIX = "Prefs.LSID.";
    
    //Specimen, Taxonomy, Geography, LithoStrat, Locality, Person, Publication, Media
    protected Hashtable<CATEGORY_TYPE, ValCheckBox> checkBoxes = new Hashtable<CATEGORY_TYPE, ValCheckBox>();
    protected ValCheckBox   useVersioning;
    protected FormValidator validator      = new FormValidator(null);
    
    /**
     * 
     */
    public LSIDPrefsPanel()
    {
        super();
        
        title    = "LSIDPrefsPanel";
        name     = "LSIDPrefsPanel";
        hContext = "LSIDPrefs";
        
        validator.setName("LSID Validator");
        validator.setNewObj(true);
        
        UIRegistry.loadAndPushResourceBundle("preferences");
        
        createUI();
        
        UIRegistry.popResourceBundle();
    }

    /**
     * 
     */
    protected void createUI()
    {
        ArrayList<String> list = new ArrayList<String>(CATEGORY_TYPE.values().length);
        for (CATEGORY_TYPE cat : CATEGORY_TYPE.values())
        {
            list.add(cat.toString());
        }
        Collections.sort(list);
        
        CellConstraints cc = new CellConstraints();
        
        AppPreferences remote = AppPreferences.getRemote();
        
        String rowDef = UIHelper.createDuplicateJGoodiesDef("p", "2px", CATEGORY_TYPE.values().length+1);
        PanelBuilder pb = new PanelBuilder(new FormLayout("10px,p,f:p:g", rowDef + ",10px,p,10px,p"), this);
        pb.addSeparator(getResourceString("LSSEP"), cc.xyw(1, 1, 3));
        int y = 3;
        for (CATEGORY_TYPE cat : CATEGORY_TYPE.values())
        {
            String pName = PREF_NAME_PREFIX + cat.toString();
            
            ValCheckBox vcb = new ValCheckBox(cat.toString(), false, false);
            checkBoxes.put(cat, vcb);
            
            vcb.setSelected(remote.getBoolean(pName, false));
            pb.add(vcb, cc.xy(2, y));
            DataChangeNotifier dcn = validator.createDataChangeNotifer(pName, vcb, null);
            vcb.addActionListener(dcn);
            y += 2;
        }
        
        pb.addSeparator(getResourceString("LSADD"), cc.xyw(1, y, 3));
        y += 2;
        
        String pName = PREF_NAME_PREFIX + "UseVersioning";
        useVersioning = new ValCheckBox("Use Versioning", false, false); // I18N
        useVersioning.setSelected(remote.getBoolean(pName, false));
        
        DataChangeNotifier dcn = validator.createDataChangeNotifer(pName, useVersioning, null);
        useVersioning.addActionListener(dcn);
        
        JButton updateLSIDsBtn = UIHelper.createI18NButton("CREATE_LSIDS");
        updateLSIDsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                savePrefs();
                createLSIDs();
            }
        });
        
        PanelBuilder pbInner = new PanelBuilder(new FormLayout("p,20px,p", "p"));
        //pbInner.add(useVersioning, cc.xy(1, 1));
        pbInner.add(updateLSIDsBtn, cc.xy(1, 1));
        
        pb.add(pbInner.getPanel(), cc.xyw(1, y, 3));
        y += 2;
        
        pb.setDefaultDialogBorder();
    }
    
    /**
     * 
     */
    protected void createLSIDs()
    {
        if (mgr == null || mgr.closePrefs())
        {
            final String COUNT = "COUNT";
            
            final GhostGlassPane glassPane = UIRegistry.writeGlassPaneMsg(getResourceString("SETTING_LSIDS"), UIRegistry.STD_FONT_SIZE);
            glassPane.setProgress(0);
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    if (SubPaneMgr.getInstance().aboutToShutdown())
                    {
                        Taskable task = TaskMgr.getTask("Startup");
                        if (task != null)
                        {
                            SubPaneIFace splash = edu.ku.brc.specify.tasks.StartUpTask.createFullImageSplashPanel(task.getTitle(), task);
                            SubPaneMgr.getInstance().addPane(splash);
                            SubPaneMgr.getInstance().showPane(splash);
                        }
                        
                        final LSIDWorker worker = new LSIDWorker()
                        {
                            protected Integer doInBackground() throws Exception
                            {
                                GenericLSIDGeneratorFactory.getInstance().buildLSIDs(this);                           
                                return null;
                            }
                            
                            @Override
                            public void propertyChange(PropertyChangeEvent evt)
                            {
                                if (evt.getPropertyName().equals("COUNT"))
                                {
                                    firePropertyChange("COUNT", 0, evt.getNewValue());
                                }
                            }

                            @Override
                            protected void done()
                            {
                                glassPane.setProgress(100);
                                UIRegistry.clearGlassPaneMsg();
                            }
                        };
                        
                        
                        worker.addPropertyChangeListener(
                                new PropertyChangeListener() {
                                    public  void propertyChange(final PropertyChangeEvent evt) {
                                        if (COUNT.equals(evt.getPropertyName())) 
                                        {
                                            glassPane.setProgress((int)((Integer)evt.getNewValue() * 100.0 / CATEGORY_TYPE.values().length));
                                        }
                                    }
                                });
                        worker.execute();
                    }
                }
            });
        }
    }
    
    class LSIDWorker extends javax.swing.SwingWorker<Integer, Integer> implements PropertyChangeListener
    {
        /* (non-Javadoc)
         * @see javax.swing.SwingWorker#doInBackground()
         */
        @Override
        protected Integer doInBackground() throws Exception
        {
            return null;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            
        }
        
        @Override
        protected void done()
        {
            super.done();
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.prefs.PrefsPanelIFace#getValidator()
     */
    public FormValidator getValidator()
    {
        return validator;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.prefs.GenericPrefsPanel#getChangedFields(java.util.Properties)
     */
    @Override
    public void getChangedFields(final Properties changeHash)
    {
        for (String key : validator.getDCNs().keySet())
        {
            String[]    toks = StringUtils.split(key, ".");
            ValCheckBox chkbx;
            if (toks[2].startsWith("Use"))
            {
                chkbx = useVersioning;
            } else
            {
                chkbx = checkBoxes.get(CATEGORY_TYPE.valueOf(toks[2]));
            }
            
            if (((UIValidatable)chkbx).isChanged())
            {
                Object value = chkbx.getValue();
                if (value != null)
                {
                    changeHash.put(key, value); //$NON-NLS-1$
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.prefs.PrefsPanelIFace#isFormValid()
     */
    public boolean isFormValid()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.prefs.GenericPrefsPanel#savePrefs()
     */
    @Override
    public void savePrefs()
    {
        AppPreferences remote = AppPreferences.getRemote();
        for (String key : validator.getDCNs().keySet())
        {
            String[]    toks = StringUtils.split(key, ".");
            ValCheckBox chkbx;
            if (toks[2].indexOf("Use") > -1)
            {
                chkbx = useVersioning;
            } else
            {
                chkbx = checkBoxes.get(CATEGORY_TYPE.valueOf(toks[2]));
            }
            
            if (((UIValidatable)chkbx).isChanged())
            {
                Object value = chkbx.getValue();
                if (value != null)
                {
                    remote.putBoolean(key, chkbx.isSelected());
                }
            }
        }
    }


}
