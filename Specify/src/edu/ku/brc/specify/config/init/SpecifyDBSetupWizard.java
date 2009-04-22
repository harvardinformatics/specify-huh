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
package edu.ku.brc.specify.config.init;

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

import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.dbsupport.DBMSUserMgr;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.SpecifyUserTypes;
import edu.ku.brc.specify.config.DisciplineType;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.Institution;
import edu.ku.brc.specify.datamodel.StorageTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
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
public class SpecifyDBSetupWizard extends JPanel
{
    private static final Logger log = Logger.getLogger(SpecifyDBSetupWizard.class);
    
    public enum WizardType {Institution, Division, Discipline, Collection}
    
    protected WizardType             wizardType  = WizardType.Institution;
    protected WizardListener         listener;
    
    protected boolean                assumeDerby = false;
    protected final String           HOSTNAME    = "localhost";
    protected boolean                doLoginOnly = false;
    
    protected Properties             props       = new Properties();
    
    protected JButton                helpBtn;
    protected JButton                backBtn;
    protected JButton                nextBtn;
    protected JButton                cancelBtn;
    
    protected DisciplinePanel        disciplinePanel;
    protected DatabasePanel          dbPanel;
    protected TreeDefSetupPanel      storageTDPanel;
    protected TreeDefSetupPanel      taxonTDPanel;
    protected TreeDefSetupPanel      geoTDPanel;
    protected DBLocationPanel        locationPanel;
    protected UserInfoPanel          userInfoPanel;
    
    protected int                    step     = 0;
    protected int                    lastStep = 0;
    
    protected boolean                isCancelled;
    protected JPanel                 cardPanel;
    protected CardLayout             cardLayout = new CardLayout();
    protected Vector<BaseSetupPanel> panels     = new Vector<BaseSetupPanel>();
    
    protected String                 setupXMLPath;
    protected JProgressBar           progressBar;
    
    
    /**
     * @param specify
     */
    public SpecifyDBSetupWizard(final WizardType wizardType,
                                final WizardListener listener)
    {
        super();
        
        this.wizardType = wizardType;
        this.listener   = listener;
        
        UIRegistry.loadAndPushResourceBundle("specifydbsetupwiz");
        
        /*setupXMLPath = UIRegistry.getUserHomeAppDir() + File.separator + "setup_prefs.xml";
        try
        {
            props.loadFromXML(new FileInputStream(new File(setupXMLPath)));
            
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyDBSetupWizard.class, ex);
        }*/
        
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

        boolean doTesting = AppPreferences.getLocalPrefs().getBoolean("wizard.defaults", true);
        if (doTesting)
        {
            props.put("hostName",   "localhost");
            props.put("dbName",     "specify");
            props.put("dbUserName", "Specify");
            props.put("dbPassword", "Specify");
            
            props.put("saUserName", "Specify");
            props.put("saPassword", "Specify");
            
            props.put("firstName", "Maureen");
            props.put("lastName",  "Kelly");
            props.put("middleInitial", "M");
            props.put("email", "mkelly@oeb.harvard.edu");
            props.put("usrUsername", "maureen");
            props.put("usrPassword", "maureen");
    
            props.put("instName", "Harvard University Herbaria");
            props.put("instAbbrev", "HUH");
    
            props.put("divName", "Botany");
            props.put("divAbbrev", "bot");
    
            props.put("collName", "Gray Herbarium");
            props.put("collPrefix", "GH");
            
            // Address
            props.put("addr1", "22 DivinityAve");
            props.put("addr2", "");
            props.put("city", "Cambridge");
            props.put("state", "MA");
            props.put("country", "USA");
            props.put("zip", "02138");
            props.put("phone", "617-714-2365");
            
            props.put("addtaxon",   true);
        }

        props.put("userType", SpecifyUserTypes.UserType.Manager.toString());
        
        UIFieldFormatterMgr.setDoingLocal(true);
        
        if (wizardType == WizardType.Institution)
        {
            dbPanel = new DatabasePanel(nextBtn, "wizard_mysql_username", true);
            panels.add(dbPanel);
            HelpMgr.registerComponent(helpBtn, dbPanel.getHelpContext());
            
            panels.add(new GenericFormPanel("SA",
                    "ENTER_SA_INFO", 
                    "wizard_master_username",
                    new String[] { "SA_USERNAME", "SA_PASSWORD"}, 
                    new String[] { "saUserName", "saPassword"}, 
                    nextBtn, true));
            
            panels.add(new GenericFormPanel("SECURITY", 
                    "SECURITY_INFO",
                    "wizard_security_on",
                    new String[] { "SECURITY_ON"}, 
                    new String[] { "security_on"},
                    new String[] { "checkbox"},
                    nextBtn, true));

    
            userInfoPanel = new UserInfoPanel("AGENT", 
                    "ENTER_COLMGR_INFO", 
                    "wizard_create_it_user",
                    new String[] { "FIRSTNAME", "LASTNAME", "MIDNAME",       "EMAIL",  null,  "USERLOGININFO", "USERNAME",    "PASSWORD"}, 
                    new String[] { "firstName", "lastName", "middleInitial", "email",  " ",   "-",             "usrUsername",  "usrPassword"}, 
                    new boolean[] { true,       true,       false,            true,    true,  false,           true,           true},
                    nextBtn);
            panels.add(userInfoPanel);
            
            panels.add(new GenericFormPanel("INST", 
                    "ENTER_INST_INFO",
                    "wizard_create_institution",
                    new String[]  { "NAME",     "ABBREV",     null,  "INST_ADDR", "ADDR1", "ADDR2", "CITY",  "STATE", "COUNTRY", "ZIP", "PHONE"}, 
                    new String[]  { "instName", "instAbbrev", " ",   "-",         "addr1", "addr2", "city",  "state", "country", "zip", "phone"}, 
                    new boolean[] { true,       true,         false,  false,      true,    false,   true,    true,    true,      true,  true},
                    nextBtn, true));

            panels.add(new GenericFormPanel("ACCESSIONGLOBALLY", 
                    "ENTER_ACC_INFO",
                    "wizard_choose_accession_level",
                    new String[] { "ACCGLOBALLY"}, 
                    new String[] { "accglobal"},
                    new String[] { "checkbox"},
                    nextBtn, true));
            
            if (wizardType == WizardType.Institution)
            {
                panels.add(new FormatterPickerPanel("ACCNOFMT", "wizard_create_accession_number", nextBtn, false));
            }

            storageTDPanel = new TreeDefSetupPanel(StorageTreeDef.class, 
                                                    getResourceString("Storage"), 
                                                    "Storage", 
                                                    "wizard_configure_storage_tree",
                                                    "CONFIG_TREEDEF", 
                                                    nextBtn, 
                                                    null);
            panels.add(storageTDPanel);
            
        }
        
        /*
        target="wizard_mysql_username"
        target="wizard_master_username"
        target="wizard_security_on"
        target="wizard_create_it_user"
        target="wizard_create_institution"
        target="wizard_choose_accession_level"
          target="wizard_create_accession_number"
        target="wizard_configure_storage_tree"
        
        target="wizard_enter_division" url="
        target="wizard_choose_discipline_type"
        target="wizard_configure_taxon_tree"
        target="wizard_preload_taxon" url="
        target="wizard_configure_geography_tree"
        target="wizard_create_catalog_number"
         target="wizard_create_collection" url="
        target="wizard_summary" url=" 
                 */
        
        if (wizardType == WizardType.Institution ||
            wizardType == WizardType.Division)
        {
            panels.add(new GenericFormPanel("DIV", 
                "ENTER_DIV_INFO",
                "wizard_enter_division",
                new String[] { "NAME",    "ABBREV"}, 
                new String[] { "divName", "divAbbrev"}, 
                nextBtn, true));
        }

        if (wizardType == WizardType.Institution || 
            wizardType == WizardType.Division || 
            wizardType == WizardType.Discipline)
        {
            nextBtn.setEnabled(false);
            disciplinePanel = new DisciplinePanel("wizard_choose_discipline_type", nextBtn);
            panels.add(disciplinePanel);

            taxonTDPanel = new TreeDefSetupPanel(TaxonTreeDef.class, 
                                                 getResourceString("Taxon"), 
                                                 "Taxon", 
                                                 "wizard_configure_taxon_tree",
                                                 "CONFIG_TREEDEF", 
                                                 nextBtn, 
                                                 disciplinePanel);
            panels.add(taxonTDPanel);
            
            panels.add(new GenericFormPanel("PRELOADTXN", 
                    "PRELOADTXN_INFO",
                    "wizard_preload_taxon",
                    new String[] { "LOAD_TAXON"}, 
                    new String[] { "preloadtaxon"},
                    new String[] { "checkbox"},
                    nextBtn, true));
             
            geoTDPanel = new TreeDefSetupPanel(GeographyTreeDef.class, 
                                               getResourceString("Geography"), 
                                               "Geography", 
                                               "wizard_configure_geography_tree",
                                               "CONFIG_TREEDEF", 
                                               nextBtn, 
                                               disciplinePanel);
            panels.add(geoTDPanel);
        }

        panels.add(new GenericFormPanel("COLLECTION", 
                    "ENTER_COL_INFO",
                    "wizard_create_collection",
                    new String[] { "NAME",     "PREFIX", }, 
                    new String[] { "collName", "collPrefix", }, 
                    nextBtn, true));
        
        panels.add(new FormatterPickerPanel("CATNOFMT", "wizard_create_catalog_number", nextBtn, true));
        
        if (wizardType != WizardType.Institution)
        {
            Institution inst = AppContextMgr.getInstance().getClassObject(Institution.class);
            if (inst != null && !inst.getIsAccessionsGlobal())
            {
                panels.add(new FormatterPickerPanel("ACCNOFMT", "wizard_create_accession_number", nextBtn, false));
            }
        }
        
        panels.add(new SummaryPanel("SUMMARY", "wizard_summary", nextBtn, panels));
         
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
                if (step == lastStep-2)
                {
            		SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Component c = SpecifyDBSetupWizard.this.getParent();
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
                }

                if (step < lastStep-1)
                {
                    if (panels.get(step) == disciplinePanel)
                    {
                        DisciplineType disciplineType = disciplinePanel.getDisciplineType();
                        if (disciplineType.isPaleo() || disciplineType.getDisciplineType() == DisciplineType.STD_DISCIPLINES.fungi)
                        {
                            step += 2;
                        }
                    }
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
                    if (wizardType == WizardType.Institution)
                    {
                        createDBAndMaster();
                        
                    } else if (SpecifyDBSetupWizard.this.listener != null)
                    {
                        SpecifyDBSetupWizard.this.listener.hide();
                        SpecifyDBSetupWizard.this.listener.finished();
                    }
                }
            }
        });
        
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                if (SpecifyDBSetupWizard.this.listener != null)
                {
                    if (step == lastStep)
                    {
                        SpecifyDBSetupWizard.this.listener.finished();
                    } else
                    {
                        SpecifyDBSetupWizard.this.listener.cancelled();
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
     * @return
     */
    public DisciplineType getDisciplineType()
    {
        return disciplinePanel.getDisciplineType();
    }
    
    /**
     * @param listener the listener to set
     */
    public void setListener(WizardListener listener)
    {
        this.listener = listener;
    }

    /**
     * 
     */
    protected void updateBtnBar()
    {
        progressBar.setValue(step);
        progressBar.setString(String.format("%d", (int)(((step) * 100.0) / (lastStep-1)))+"% Complete"); // I18N

        if (step == lastStep-1)
        {
            nextBtn.setEnabled(panels.get(step).isUIValid());
            String key;
            switch (wizardType)
            {
                case Institution : key = "FINISHED"; break;
                case Division    : key = "FINISHED_DIV"; break;
                case Discipline  : key = "FINISHED_DISP"; break;
                case Collection  : key = "FINISHED_COL"; break;
                default          : key = "FINISHED"; break;
            }
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
    protected String stripSpecifyDir(final String path)
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
    protected boolean saveFormatters(final UIFieldFormatterIFace fmt, final String fileName)
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
    protected void configSetup()
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
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyDBSetupWizard.class, ex);
            
        }
        
        if (wizardType == WizardType.Institution)
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
    public void createDBAndMaster()
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
                
                String             dbName     = props.getProperty("dbName");
                String             hostName   = props.getProperty("hostName");
                
                String saUsername = props.getProperty("saUserName");
                String saPassword = props.getProperty("saPassword");
                
                String itUsername = props.getProperty("dbUserName");
                String itPassword = props.getProperty("dbPassword");
                
                if (mgr.connectToDBMS(itUsername, itPassword, hostName))
                {
                    if (!mgr.doesDBExists(dbName))
                    {
                        try
                        {
                            isOK = mgr.createDatabase(dbName);
                            
                        } catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    } else
                    {
                        isOK = true;
                    }
                    
                    if (!isOK)
                    {
                        mgr.close();
                        UIRegistry.showLocalizedMsg("You were unable to create the database, you must login as root.");
                        return null;
                    }
                    
                    if (!mgr.doesUserExists(saUsername))
                    {
                        try
                        {
                            isOK = mgr.createUser(saUsername, saPassword, dbName, DBMSUserMgr.PERM_ALL);
                            
                        } catch (Exception ex)
                        {
                            ex.printStackTrace();
                            
                        }
                        
                        if (!isOK)
                        {
                            mgr.close();
                            UIRegistry.showLocalizedMsg("You were unable to create the user you must be root.");
                            return null;
                        }
                    } else
                    {
                        isOK = true;
                    }
                } else
                {
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
                    configureDatabase();
                    
                    frame.setVisible(false); // Progress Dialog
                    frame.dispose();
                    
                    if (SpecifyDBSetupWizard.this.listener != null)
                    {
                        SpecifyDBSetupWizard.this.listener.hide();
                    } 
                } else
                {
                    frame.setVisible(false); // Progress Dialog
                    frame.dispose();
                    
                    if (SpecifyDBSetupWizard.this.listener != null)
                    {
                        SpecifyDBSetupWizard.this.listener.hide();
                        SpecifyDBSetupWizard.this.listener.cancelled();
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
     * Sets up initial preference settings.
     */
    protected void setupLoginPrefs()
    {
        String userName = props.getProperty("usrPassword");
        
        String encryptedMasterUP = UserAndMasterPasswordMgr.getInstance().encrypt(
                                       props.getProperty("saUserName"), 
                                       props.getProperty("saPassword"), 
                                       userName);

        DatabaseDriverInfo driverInfo = dbPanel.getDriver();
        AppPreferences ap = AppPreferences.getLocalPrefs();
        ap.put(userName+"_master.islocal",  "true");
        ap.put(userName+"_master.path",     encryptedMasterUP);
        ap.put("login.dbdriver_selected",  driverInfo.getName());
        ap.put("login.username",           props.getProperty("usrUsername"));
        ap.put("login.databases_selected", dbPanel.getDbName());
        ap.put("login.databases",          dbPanel.getDbName());
        ap.put("login.servers",            props.getProperty("hostName"));
        ap.put("login.servers_selected",   props.getProperty("hostName"));
        ap.put("login.rememberuser",       "true");
        
        try
        {
            ap.flush();
            
        } catch (BackingStoreException ex) {}
    }

    /**
     * @return the props
     */
    public Properties getProps()
    {
        return props;
    }
    
    public void processDataForNonBuild()
    {
        saveFormatters(); 
    }
    
    /**
     * 
     */
    protected void saveFormatters()
    {
        Object catNumFmtObj = props.get("catnumfmt");
        Object accNumFmtObj = props.get("accnumfmt");
        
        UIFieldFormatterIFace catNumFmt = catNumFmtObj instanceof UIFieldFormatterIFace ? (UIFieldFormatterIFace)catNumFmtObj : null;
        UIFieldFormatterIFace accNumFmt = accNumFmtObj instanceof UIFieldFormatterIFace ? (UIFieldFormatterIFace)accNumFmtObj : null;
        
        if (catNumFmt != null)
        {
            saveFormatters(catNumFmt, "catnumfmt.xml");
        }
        if (accNumFmt != null)
        {
            saveFormatters(accNumFmt, "accnumfmt.xml");
        }
    }

    /**
     * 
     */
    public void configureDatabase()
    {
        setupLoginPrefs();

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

                        BuildSampleDatabase builder = new BuildSampleDatabase();
                        
                        //builder.getFrame().setIconImage(IconManager.getImage("Specify16", IconManager.IconSize.Std16).getImage());
                        
                        boolean proceed = true;
                        if (checkForDatabase(props))
                        {
                            Object[] options = { 
                                    getResourceString("PROCEED"), 
                                    getResourceString("CANCEL")
                                  };
                            int userChoice = JOptionPane.showOptionDialog(UIRegistry.getTopWindow(), 
                                                                         UIRegistry.getLocalizedMessage("DEL_CUR_DB", dbPanel.getDbName()), 
                                                                         getResourceString("DEL_CUR_DB_TITLE"), 
                                                                         JOptionPane.YES_NO_OPTION,
                                                                         JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                            proceed = userChoice == JOptionPane.YES_OPTION;
                            
                        } 

                        if (proceed)
                        {
                            isOK = builder.buildEmptyDatabase(props);
                            
                            if (isOK)
                            {
                                saveFormatters();
                            }

                            JOptionPane.showMessageDialog(UIRegistry.getTopWindow(), 
                                    getLocalizedMessage("BLD_DONE", getResourceString(isOK ? "BLD_OK" :"BLD_NOTOK")),
                                    getResourceString("COMPLETE"), JOptionPane.INFORMATION_MESSAGE);                                
                        }
                        
                        UIRegistry.popResourceBundle();
                            
                        } catch (Exception ex)
                        {
                            //edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                            //edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyDBSetupWizard.class, ex);
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
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyDBSetupWizard.class, ex);
            }
    }
    
    /**
     * @param properties
     * @return
     */
    private boolean checkForDatabase(final Properties properties)
    {
        final String dbName = properties.getProperty("dbName");
        
        DBMSUserMgr mgr = null;
        try
        {
            
            String itUsername = properties.getProperty("dbUserName");
            String itPassword = properties.getProperty("dbPassword");
            String hostName   = properties.getProperty("hostName");
            
            mgr = DBMSUserMgr.getInstance();
            
            if (mgr.connectToDBMS(itUsername, itPassword, hostName))
            {
                if (mgr.doesDBExists(dbName))
                {
                    mgr.close();
                    
                    if (mgr.connect(itUsername, itPassword, hostName, dbName))
                    {
                        return mgr.doesDBHaveTables();
                    }
                    
                }
            }
            
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyDBSetupWizard.class, ex);
            
        } finally
        {
            if (mgr != null)
            {
                mgr.close();
            }
        }
        return false;
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
