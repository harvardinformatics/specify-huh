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
package edu.harvard.huh.specify.config.init;

import static edu.ku.brc.ui.UIHelper.createButton;
import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;
import java.util.prefs.BackingStoreException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.harvard.huh.specify.utilapps.LoadHUHdatabase;
import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.dbsupport.DBMSUserMgr;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.SpecifyUserTypes;
import edu.ku.brc.specify.config.init.BaseSetupPanel;
import edu.ku.brc.specify.config.init.DatabasePanel;
import edu.ku.brc.specify.config.init.GenericFormPanel;
import edu.ku.brc.specify.config.init.SetupPanelIFace;
import edu.ku.brc.specify.ui.HelpMgr;
import edu.ku.brc.specify.utilapps.BuildSampleDatabase;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.ProgressFrame;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Created Date: Oct 15, 2008
 *
 */
public class SpecifyHUHdbLoadWizard extends JPanel
{
    private static final Logger log = Logger.getLogger(SpecifyHUHdbLoadWizard.class);

    private WizardListener         listener;
    
    private Properties             props       = new Properties();
    
    private JButton                helpBtn;
    private JButton                backBtn;
    private JButton                nextBtn;
    private JButton                cancelBtn;

    private DatabasePanel          dbPanel;
    
    private int                    step     = 0;
    private int                    lastStep = 0;

    private JPanel                 cardPanel;
    private CardLayout             cardLayout = new CardLayout();
    private Vector<BaseSetupPanel> panels     = new Vector<BaseSetupPanel>();

    private JProgressBar           progressBar;
    
    
    /**
     * @param specify
     */
    public SpecifyHUHdbLoadWizard(final WizardListener listener)
    {
        super();

        this.listener   = listener;
        
        UIRegistry.loadAndPushResourceBundle("specifydbsetupwiz");
        UIRegistry.loadAndPushResourceBundle("huhloaddbwiz");

        HelpMgr.setLoadingPage("Load");
        
        cardPanel = new JPanel(cardLayout);
        
        cancelBtn  = createButton(UIRegistry.getResourceString("CANCEL"));
        helpBtn    = createButton(UIRegistry.getResourceString("HELP"));
        
        JPanel btnBar;
        backBtn    = createButton(UIRegistry.getResourceString("BACK"));
        nextBtn    = createButton(UIRegistry.getResourceString("NEXT"));
        
        CellConstraints cc = new CellConstraints();
        PanelBuilder bbpb = new PanelBuilder(new FormLayout("f:p:g,p,4px,p,4px,p,4px,p,4px", "p"));
        
        bbpb.add(helpBtn,   cc.xy(2,1));
        bbpb.add(backBtn,   cc.xy(4,1));
        bbpb.add(nextBtn,   cc.xy(6,1));
        bbpb.add(cancelBtn, cc.xy(8,1));
        
        btnBar = bbpb.getPanel();

        props.put("userType", SpecifyUserTypes.UserType.Manager.toString());
        
        UIFieldFormatterMgr.setDoingLocal(true);

        // Specify master username/password == the mysql username/password
        // The DatabasePanel grabs fields named IT_USERNAME and IT_PASSWORD,
        // but for our purposes we use them as SA_USERNAME and SA_PASSWORD
        dbPanel = new DatabasePanel(nextBtn, backBtn, "wizard_mysql_username", true);
        panels.add(dbPanel);
        HelpMgr.registerComponent(helpBtn, dbPanel.getHelpContext());

        // Locality
        panels.add(new GenericFormPanel("DIV", 
                                        "ENTER_DIV_INFO",
                                        "wizard_enter_division",
                                        new String[] { "NAME",    "ABBREV"}, 
                                        new String[] { "divName", "divAbbrev"}, 
                                        nextBtn, backBtn, true));
         
        lastStep = panels.size();
        
        panels.get(0).updateBtnUI();
        
        if (backBtn != null)
        {
            backBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    if (step > 0)
                    {
                        step--;
                        panels.get(step).doingPrev();
                        HelpMgr.registerComponent(helpBtn, panels.get(step).getHelpContext());
                        cardLayout.show(cardPanel, Integer.toString(step));
                    }
                    updateBtnBar();
                    if (listener != null)
                    {
                        listener.panelChanged(getResourceString(panels.get(step).getPanelName()+".TITLE"));
                    }
                }
            });
            
            backBtn.setEnabled(false);
        }
        
        nextBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
/*                if (step == lastStep-2)
                {
            		SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Component c = SpecifyHUHdbLoadWizard.this.getParent();
		                	while (!(c instanceof Window) && c != null)
		                	{
		                		c = c.getParent();
		                	}
		                	if (c != null)
		                	{
		                		((Window)c).pack();
		                	}
						}
            		});
                }*/

                if (step < lastStep-1)
                {

                    step++;
                    HelpMgr.registerComponent(helpBtn, panels.get(step).getHelpContext());
                    panels.get(step).doingNext();
                    cardLayout.show(cardPanel, Integer.toString(step));

                    updateBtnBar();
                    if (listener != null)
                    {
                        listener.panelChanged(getResourceString(panels.get(step).getPanelName()+".TITLE"));
                    }
                    
                } else
                {
                    nextBtn.setEnabled(false);
                    
                    configSetup();

                    loadDatabase();

                }
            }
        });
        
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                if (SpecifyHUHdbLoadWizard.this.listener != null)
                {
                    if (step == lastStep)
                    {
                        SpecifyHUHdbLoadWizard.this.listener.finished();
                    } else
                    {
                        SpecifyHUHdbLoadWizard.this.listener.cancelled();
                    }
                }
            }
         });

        for (int i=0;i<panels.size();i++)
        {   
            cardPanel.add(Integer.toString(i), panels.get(i));
            panels.get(i).setValues(props);
        }
        cardLayout.show(cardPanel, "0");
        
        if (dbPanel != null)
        {
            dbPanel.updateBtnUI();
        }

        PanelBuilder    builder = new PanelBuilder(new FormLayout("f:p:g", "f:p:g,10px,p"));
        builder.add(cardPanel, cc.xy(1, 1));
        builder.add(btnBar, cc.xy(1, 3));
        
        builder.setDefaultDialogBorder();
        
        setLayout(new BorderLayout());
        PanelBuilder  iconBldr  = new PanelBuilder(new FormLayout("20px, f:p:g,p,f:p:g,8px", "20px,t:p,f:p:g, 8px"));
        JLabel        iconLbl   = new JLabel(IconManager.getIcon("WizardIcon"));
        iconLbl.setVerticalAlignment(SwingConstants.TOP);
        iconBldr.add(iconLbl, cc.xy(2, 3));
        add(iconBldr.getPanel(), BorderLayout.WEST);
        add(builder.getPanel(), BorderLayout.CENTER);
        
        progressBar = new JProgressBar(0, lastStep-1);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);
    }
    
    /**
     * 
     */
    private void updateBtnBar()
    {
        progressBar.setValue(step);
        progressBar.setString(String.format("%d", (int)(((step) * 100.0) / (lastStep-1)))+"% Complete"); // I18N

        if (step == lastStep-1)
        {
            nextBtn.setEnabled(panels.get(step).isUIValid());
            String key = "FINISHED";
            nextBtn.setText(getResourceString(key));
            
        } else
        {
            nextBtn.setEnabled(panels.get(step).isUIValid());
            nextBtn.setText(getResourceString("NEXT"));
        }
        backBtn.setEnabled(step > 0); 
    }

    /**
     * @param path
     * @return
     */
    private String stripSpecifyDir(final String path)
    {
        String appPath = path;
        int endInx = appPath.indexOf("Specify.app");
        if (endInx > -1)
        {
            appPath = appPath.substring(0, endInx-1);
        }
        return appPath;
    }
    
    /**
     * @param fmt
     * @param fileName
     */
    private boolean saveFormatters(final UIFieldFormatterIFace fmt, final String fileName)
    {
        if (fmt != null)
        {
            StringBuilder sb = new StringBuilder();
            fmt.toXML(sb);
            
            String path = UIRegistry.getAppDataDir() + File.separator + fileName;
            try
            {
                FileUtils.writeStringToFile(new File(path), sb.toString());
                return true;
                
            } catch (IOException ex)
            {
                
            }
        } else
        {
            return true; // null fmtr doesn't mean an error
        }
        return false;
    }
    
    /**
     * Get the values form the panels.
     */
    private void configSetup()
    {
        try
        {
            for (SetupPanelIFace panel : panels)
            {
                panel.getValues(props);
            }
            //props.storeToXML(new FileOutputStream(new File(setupXMLPath)), "SetUp Props");
            
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyHUHdbLoadWizard.class, ex);
            
        }
        
        if (true) //wizardType == WizardType.Institution
        {
            // Clear and Reset Everything!
            //AppPreferences.shutdownLocalPrefs();
            //UIRegistry.setDefaultWorkingPath(null);
            
            log.debug("********** WORK["+UIRegistry.getDefaultWorkingPath()+"]");
            log.debug("********** USER LOC["+stripSpecifyDir(UIRegistry.getAppDataDir())+"]");
            
            String baseAppDir;
            if (UIHelper.getOSType() == UIHelper.OSTYPE.MacOSX)
            {
                baseAppDir = stripSpecifyDir(UIRegistry.getAppDataDir());
                
            } else
            {
                baseAppDir = UIRegistry.getDefaultWorkingPath();
            }
            
            baseAppDir = UIHelper.stripSubDirs(baseAppDir, 1);
            UIRegistry.setDefaultWorkingPath(baseAppDir);
            
            log.debug("********** Working path for App ["+baseAppDir+"]");
        }
    }

    /**
     * 
     */
    private void loadDatabase()
    {
        final ProgressFrame frame = new ProgressFrame("Creating master user ...", "SpecifyLargeIcon");
        //frame.getCloseBtn().setVisible(false);
        frame.pack();
        Dimension size = frame.getSize();
        size.width = Math.max(size.width, 500);
        frame.setSize(size);
        UIHelper.centerAndShow(frame);
        
        final SwingWorker worker = new SwingWorker()
        {
            protected boolean isOK = false;
            
            public Object construct()
            {
                System.setProperty(DBMSUserMgr.factoryName, "edu.ku.brc.dbsupport.MySQLDMBSUserMgr");
                DBMSUserMgr mgr = DBMSUserMgr.getInstance();
                // need to call a factory here based on the type of DBMS we are using.
                
                String dbName     = props.getProperty("dbName");
                String hostName   = props.getProperty("hostName");
                String saUsername = props.getProperty("dbUserName");
                String saPassword = props.getProperty("dbPassword");
                
                isOK = mgr.connectToDBMS(saUsername, saPassword, hostName);

                if (isOK)
                {
                    isOK = mgr.doesDBExists(dbName);
                    if (! isOK)
                    {
                        // No Database Error
                        UIRegistry.showLocalizedMsg("The database was not found.");
                    }
                }
                else {
                    // No Connect Error
                    UIRegistry.showLocalizedMsg("You were unable to to connect to the database.");
                }

                mgr.close();
                
                return null;
            }

            //Runs on the event-dispatching thread.
            public void finished()
            {
                if (isOK)
                {
                    loadLocalityData();
                    
                    frame.setVisible(false); // Progress Dialog
                    frame.dispose();
                    
                    if (SpecifyHUHdbLoadWizard.this.listener != null)
                    {
                        SpecifyHUHdbLoadWizard.this.listener.hide();
                    } 
                } else
                {
                    frame.setVisible(false); // Progress Dialog
                    frame.dispose();
                    
                    if (SpecifyHUHdbLoadWizard.this.listener != null)
                    {
                        SpecifyHUHdbLoadWizard.this.listener.hide();
                        SpecifyHUHdbLoadWizard.this.listener.cancelled();
                    } 
                }
            }
        };
        SwingUtilities.invokeLater(new Runnable() {

            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run()
            {
                worker.start();
            }
        });
    }
  
    /**
     * 
     */
    private void loadLocalityData()
    {

       //System.err.println(UIRegistry.getDefaultWorkingPath() + File.separator + "DerbyDatabases");
        try
        {
            final SwingWorker worker = new SwingWorker()
            {
                protected boolean isOK = false;
                
                public Object construct()
                {
                    try
                    {
                        DatabaseDriverInfo driverInfo = dbPanel.getDriver();
                        props.put("driver", driverInfo);
                        
                        if (driverInfo == null)
                        {
                            throw new RuntimeException("Couldn't find driver by name ["+driverInfo+"] in driver list.");
                        }

                        LoadHUHdatabase loader = new LoadHUHdatabase();
                        
                        boolean proceed = true;

                        if (proceed)
                        {
                            isOK = loader.loadHUHdatabase(props);
                            
                            JOptionPane.showMessageDialog(UIRegistry.getTopWindow(), 
                                    getLocalizedMessage("BLD_DONE", getResourceString(isOK ? "BLD_OK" :"BLD_NOTOK")),
                                    getResourceString("COMPLETE"), JOptionPane.INFORMATION_MESSAGE);                                
                        }
                        
                        UIRegistry.popResourceBundle();
                            
                        } catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                        return null;
                    }

                    //Runs on the event-dispatching thread.
                    public void finished()
                    {
                        if (isOK)
                        {
                            HibernateUtil.shutdown();
                        }
                        if (listener != null)
                        {
                            listener.hide();
                            listener.finished();
                        }
                    }
                };
                worker.start();
            
            } catch (Exception ex)
            {
                ex.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyHUHdbLoadWizard.class, ex);
            }
    }
    
    //-------------------------------------------------
    public interface WizardListener
    {
        public abstract void panelChanged(String title);
        
        public abstract void cancelled();
        
        public abstract void hide();
        
        public abstract void finished();
    }
    
}
