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
package edu.ku.brc.specify.tasks;

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.thoughtworks.xstream.XStream;

import edu.ku.brc.af.auth.BasicPermisionPanel;
import edu.ku.brc.af.auth.PermissionEditorIFace;
import edu.ku.brc.af.auth.PermissionSettings;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.AppResourceIFace;
import edu.ku.brc.af.core.ContextMgr;
import edu.ku.brc.af.core.NavBox;
import edu.ku.brc.af.core.NavBoxAction;
import edu.ku.brc.af.core.NavBoxButton;
import edu.ku.brc.af.core.NavBoxIFace;
import edu.ku.brc.af.core.NavBoxItemIFace;
import edu.ku.brc.af.core.NavBoxMgr;
import edu.ku.brc.af.core.RecordSetFactory;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.TaskMgr;
import edu.ku.brc.af.core.Taskable;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.core.UsageTracker;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.prefs.PreferencesDlg;
import edu.ku.brc.af.tasks.subpane.FormPane;
import edu.ku.brc.af.ui.db.CommandActionForDB;
import edu.ku.brc.af.ui.db.ViewBasedDisplayDialog;
import edu.ku.brc.af.ui.db.ViewBasedDisplayIFace;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.TableViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.persist.ViewIFace;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.dbsupport.TableModel2Excel;
import edu.ku.brc.dbsupport.DataProviderSessionIFace.QueryIFace;
import edu.ku.brc.helpers.EMailHelper;
import edu.ku.brc.helpers.Encryption;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.config.SpecifyAppContextMgr;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.ExchangeIn;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftPreparation;
import edu.ku.brc.specify.datamodel.InfoRequest;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Permit;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.PreparationsProviderIFace;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.specify.datamodel.RepositoryAgreement;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.SpAppResource;
import edu.ku.brc.specify.datamodel.SpReport;
import edu.ku.brc.specify.datamodel.busrules.LoanBusRules;
import edu.ku.brc.specify.tasks.subpane.qb.QueryBldrPane;
import edu.ku.brc.specify.tasks.subpane.wb.wbuploader.Uploader;
import edu.ku.brc.specify.ui.LoanReturnDlg;
import edu.ku.brc.specify.ui.LoanReturnDlg.LoanReturnInfo;
import edu.ku.brc.ui.ChooseFromListDlg;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.DataFlavorTableExt;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.RolloverCommand;
import edu.ku.brc.ui.ToolBarDropDownBtn;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.dnd.Trash;
import edu.ku.brc.util.Pair;


/**
 * This task manages Loans, Gifts, Exchanges and provide actions and forms to do the interactions.
 *
 * @code_status Beta
 *
 * @author rods
 *
 */
public class InteractionsTask extends BaseTask
{
    private static final Logger log = Logger.getLogger(InteractionsTask.class);

    protected static final String resourceName = "InteractionsTaskInit";
    
    public static final String             INTERACTIONS        = "Interactions";
    public static final DataFlavorTableExt INTERACTIONS_FLAVOR = new DataFlavorTableExt(DataEntryTask.class, INTERACTIONS);
    public static final DataFlavorTableExt INFOREQUEST_FLAVOR  = new DataFlavorTableExt(InfoRequest.class, "InfoRequest", new int[] {50});
    public static final DataFlavorTableExt LOAN_FLAVOR         = new DataFlavorTableExt(Loan.class, "Loan", new int[] {52});
    public static final DataFlavorTableExt GIFT_FLAVOR         = new DataFlavorTableExt(Loan.class, "Gift");
    public static final DataFlavorTableExt EXCHGIN_FLAVOR      = new DataFlavorTableExt(ExchangeIn.class, "ExchangeIn");
    public static final DataFlavorTableExt EXCHGOUT_FLAVOR     = new DataFlavorTableExt(ExchangeOut.class, "ExchangeOut");
    
    public static final  String   IS_USING_INTERACTIONS_PREFNAME = "Interactions.Using.Interactions.";

    protected static final String InfoRequestName      = "InfoRequest";
    protected static final String NEW_LOAN             = "NEW_LOAN";
    protected static final String NEW_ACCESSION        = "NEW_ACCESSION";
    protected static final String NEW_PERMIT           = "NEW_PERMIT";
    protected static final String NEW_GIFT             = "NEW_GIFT";
    protected static final String NEW_EXCHANGE_OUT     = "NEW_EXCHANGE_OUT";
    protected static final String PRINT_LOAN           = "PRINT_LOAN";
    protected static final String PRINT_INVOICE        = "PRINT_INVOICE";
    protected static final String INFO_REQ_MESSAGE     = "Specify Info Request";
    protected static final String CREATE_MAILMSG       = "CreateMailMsg";
    protected static final String ADD_TO_LOAN          = "AddToLoan";
    protected static final String ADD_TO_GIFT          = "AddToGift";
    protected static final String OPEN_NEW_VIEW        = "OpenNewView";
    protected static final String LN_NO_PRP            = "LN_NO_PRP";
    
    protected static final int    loanTableId;
    protected static final int    infoRequestTableId;
    protected static final int    colObjTableId;

    // Data Members
    protected NavBox                  infoRequestNavBox;
    protected Vector<NavBoxIFace>     extendedNavBoxes = new Vector<NavBoxIFace>();
    protected Vector<NavBoxItemIFace> invoiceList      = new Vector<NavBoxItemIFace>();
    protected NavBoxItemIFace         exchgNavBtn      = null; 
    protected NavBoxItemIFace         giftsNavBtn      = null; 
    protected NavBox                  actionsNavBox;
    
    protected boolean                 isUsingInteractions = true;
    protected ToolBarDropDownBtn      toolBarBtn       = null;
    protected int                     indexOfTBB       = -1;
    
    protected Vector<InteractionEntry>  entries = new Vector<InteractionEntry>();
    
    protected Vector<Integer> printableInvoiceTblIds = new Vector<Integer>();
    
    InteractionsProcessor<Gift> giftProcessor = new InteractionsProcessor<Gift>(this, false, Gift.getClassTableId());
    InteractionsProcessor<Loan> loanProcessor = new InteractionsProcessor<Loan>(this, true,  Loan.getClassTableId());
    
    static 
    {
        loanTableId        = DBTableIdMgr.getInstance().getIdByClassName(Loan.class.getName());
        infoRequestTableId = DBTableIdMgr.getInstance().getIdByClassName(InfoRequest.class.getName());
        colObjTableId      = DBTableIdMgr.getInstance().getIdByClassName(CollectionObject.class.getName());
        
        INFOREQUEST_FLAVOR.addTableId(50);
    }

   /**
     * Default Constructor
     *
     */
    public InteractionsTask()
    {
        super(INTERACTIONS, getResourceString("Interactions"));
        
        CommandDispatcher.register(INTERACTIONS, this);
        CommandDispatcher.register(RecordSetTask.RECORD_SET, this);
        CommandDispatcher.register(DB_CMD_TYPE, this);
        CommandDispatcher.register(DataEntryTask.DATA_ENTRY, this);
        CommandDispatcher.register(PreferencesDlg.PREFERENCES, this);
        
        readEntries();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.Taskable#initialize()
     */
    @Override
    public void initialize()
    {
        if (!isInitialized)
        {
            super.initialize(); // sets isInitialized to false
            
            setUpCachedPrefs();
            
            extendedNavBoxes.clear();
            invoiceList.clear();

            actionsNavBox = new NavBox(getResourceString("CreateAndUpdate"));
            
            SpecifyAppContextMgr acm = (SpecifyAppContextMgr)AppContextMgr.getInstance();
            
            boolean showIRBox = true;
            for (InteractionEntry entry : entries)
            {
                DBTableInfo tableInfo = DBTableIdMgr.getInstance().getInfoByTableName(entry.getTableName());
                if (!AppContextMgr.isSecurityOn() || tableInfo.getPermissions().canView())
                {
                    String label;
                    if (StringUtils.isNotEmpty(entry.getLabelKey()))
                    {
                        label = getResourceString(entry.getLabelKey());
                    } else
                    {
                        label = tableInfo.getTitle();
                    }
                    
                    String tooltip = "";
                    if (StringUtils.isEmpty(entry.getTooltip()) && StringUtils.isNotEmpty(entry.getViewName()))
                    {
                        ViewIFace view = acm.getView(entry.getViewName());
                        if (view != null)
                        {
                            tooltip = getLocalizedMessage("DET_OPEN_VIEW", view.getObjTitle());
                        }
                    } else
                    {
                        tooltip = getLocalizedMessage(entry.getTooltip());
                    }
                    
                    entry.setTitle(label);
                    entry.setI18NTooltip(tooltip);

                    if (entry.isOnLeft())
                    {
                        NavBoxButton navBtn = addCommand(actionsNavBox, tableInfo, entry);
                        navBtn.setToolTip(entry.getI18NTooltip());
                    }
                    
                    if (StringUtils.isNotEmpty(entry.getViewName()) && entry.isSearchService())
                    {
                        CommandAction cmdAction = createCmdActionFromEntry(entry, tableInfo);
                        ContextMgr.registerService(10, entry.getViewName(), tableInfo.getTableId(), cmdAction, this, "Data_Entry", tableInfo.getTitle(), true); // the Name gets Hashed
                    }
                } else if (tableInfo.getTableId() == infoRequestTableId)
                {
                    showIRBox = false;
                }
            }
            navBoxes.add(actionsNavBox);
            
            infoRequestNavBox  = new NavBox(getResourceString("InfoRequest"));
            loadNavBox(infoRequestNavBox, InfoRequest.class, INFOREQUEST_FLAVOR, showIRBox);
        }
        isShowDefault = true;
    }

    
    /* (non-Javadoc)
	 * @see edu.ku.brc.af.tasks.BaseTask#doProcessAppCommands(edu.ku.brc.ui.CommandAction)
	 */
	@Override
	protected void doProcessAppCommands(CommandAction cmdAction)
	{
		// TODO Auto-generated method stub
		super.doProcessAppCommands(cmdAction);
        
        if (cmdAction.isAction(APP_RESTART_ACT) ||
            cmdAction.isAction(APP_START_ACT))
        {
            this.isInitialized = false;
            initialize();
        }
	}

	/**
     * Returns whether a table id if considered to be an Interaction.
     * @param tableId the table ID in question
     * @return true if it is a table that is handled by Interactions
     */
    public static boolean isInteractionTable(final int tableId)
    {
        return tableId == Loan.getClassTableId() ||
               tableId == Gift.getClassTableId() ||
               tableId == Accession.getClassTableId() ||
               tableId == Permit.getClassTableId() ||
               tableId == RepositoryAgreement.getClassTableId() ||
               tableId == ExchangeIn.getClassTableId() ||
               tableId == ExchangeOut.getClassTableId();
    }
    
    /**
     * Reads the entries set up information from the database.
     */
    @SuppressWarnings("unchecked")
    private void readEntries()
    {
        try
        {
            XStream xstream = new XStream();
            
            InteractionEntry.config(xstream);
            EntryFlavor.config(xstream);
            
            String           xmlStr = null;
            AppResourceIFace appRes = AppContextMgr.getInstance().getResourceFromDir(SpecifyAppContextMgr.PERSONALDIR, resourceName);
            if (appRes != null)
            {
                xmlStr = appRes.getDataAsString();
                
            } else
            {
                // Get the default resource by name and copy it to a new User Area Resource
                AppResourceIFace newAppRes = AppContextMgr.getInstance().copyToDirAppRes(SpecifyAppContextMgr.PERSONALDIR, resourceName);
                if (newAppRes != null)
                {
                    // Save it in the User Area
                    //AppContextMgr.getInstance().saveResource(newAppRes);
                    xmlStr = newAppRes.getDataAsString();
                } else
                {
                    return;
                }
            }
            
            //log.debug(xmlStr);
            entries = (Vector<InteractionEntry>)xstream.fromXML(xmlStr); // Describes the definitions of the full text search);
            
        } catch (Exception ex)
        {
            log.error(ex);
            ex.printStackTrace();
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);
        }
    }
    
    /**
     * Writes the entries set up information to the database.
     */
    private void writeEntries()
    {
        XStream xstream = new XStream();
        InteractionEntry.config(xstream);
        EntryFlavor.config(xstream);
        
        AppResourceIFace appRes = AppContextMgr.getInstance().getResourceFromDir(SpecifyAppContextMgr.PERSONALDIR, resourceName);
        if (appRes != null)
        {
            appRes.setDataAsString(xstream.toXML(entries));
            AppContextMgr.getInstance().saveResource(appRes);
            
        } else
        {
            AppContextMgr.getInstance().putResourceAsXML(resourceName, xstream.toXML(entries));     
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#isConfigurable()
     */
    @Override
    public boolean isConfigurable()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#doConfigure()
     */
    @Override
    public void doConfigure()
    {
        
        boolean isEmpty = infoRequestNavBox.getItems().size() == 0;
        
        Vector<TaskConfigItemIFace> stdList  = new Vector<TaskConfigItemIFace>();
        Vector<TaskConfigItemIFace> miscList = new Vector<TaskConfigItemIFace>();
        Vector<TaskConfigItemIFace> srvList  = new Vector<TaskConfigItemIFace>();
        
        boolean showIRBox = true;
        for (InteractionEntry entry : entries)
        {
            DBTableInfo tableInfo = DBTableIdMgr.getInstance().getInfoByTableName(entry.getTableName());
            if (!AppContextMgr.isSecurityOn() || tableInfo.getPermissions().canView())
            {
                //System.err.println(entry.getName()+"\t  "+entry.isSearchService()+"   "+entry.isOnLeft()+"   "+entry.isVisible());
                entry.setEnabled(true);
                if (entry.isVisible())
                {
                    Vector<TaskConfigItemIFace> list = entry.isOnLeft() ? stdList : miscList;
                    // Clone for undo (Cancel)
                    try
                    {
                        list.add((TaskConfigItemIFace)entry.clone());
                        
                    } catch (CloneNotSupportedException ex) {/* ignore */}
                } else
                {
                    srvList.add(entry);
                }
            } else
            {
                entry.setEnabled(false);
                
                if (tableInfo.getTableId() == infoRequestTableId)
                {
                    showIRBox = false;
                }
            }
        }
        
        if (showIRBox)
        {
            navBoxes.add(infoRequestNavBox);
        } else
        {
            navBoxes.remove(infoRequestNavBox);
        }
        
        int origNumStd = stdList.size();
        
        TaskConfigureDlg dlg = new TaskConfigureDlg(stdList, miscList, false,
                "InteractionsConfig",
                "IAT_TITLE",
                "IAT_AVAIL_ITEMS",
                "IAT_HIDDEN_ITEMS",
                "IAT_MAKE_AVAIL",
                "IAT_MAKE_HIDDEN");
        dlg.setVisible(true);
        if (!dlg.isCancelled())
        {
            entries.clear();
            for (TaskConfigItemIFace ie : stdList)
            {
                ((InteractionEntry)ie).setOnLeft(true);
                entries.add((InteractionEntry)ie);
            }
            
            for (TaskConfigItemIFace ie : miscList)
            {
                ((InteractionEntry)ie).setOnLeft(false);
                entries.add((InteractionEntry)ie);
            }
            
            for (TaskConfigItemIFace ie : srvList)
            {
                entries.add((InteractionEntry)ie);
            }
            
            writeEntries();
            
            actionsNavBox.clear();
            
            Collections.sort(entries);
            
            for (InteractionEntry entry : entries)
            {
                DBTableInfo tableInfo = DBTableIdMgr.getInstance().getInfoByTableName(entry.getTableName());
                if (entry.isEnabled())
                {
                    if (entry.isOnLeft())
                    {
                        NavBoxButton navBtn = addCommand(actionsNavBox, tableInfo, entry);
                        navBtn.setToolTip(entry.getI18NTooltip());
                        
                    } else if (StringUtils.isNotEmpty(entry.getViewName()) && !entry.isSearchService())
                    {
                        CommandAction cmdAction = createCmdActionFromEntry(entry, tableInfo);
                        ContextMgr.registerService(10, entry.getViewName(), tableInfo.getTableId(), cmdAction, this, "Data_Entry", tableInfo.getTitle(), true); // the Name gets Hashed
                    }
                }
            }
            
            NavBoxMgr.getInstance().validate();
            NavBoxMgr.getInstance().invalidate();
            NavBoxMgr.getInstance().doLayout();
            
            if (stdList.size() == 0)
            {
                String ds = AppContextMgr.getInstance().getClassObject(Discipline.class).getType();
                AppPreferences.getRemote().putBoolean(IS_USING_INTERACTIONS_PREFNAME+ds, false);
                JToolBar toolBar = (JToolBar)UIRegistry.get(UIRegistry.TOOLBAR);
                indexOfTBB = toolBar.getComponentIndex(toolBarBtn);
                if (indexOfTBB > -1)
                {
                    TaskMgr.removeToolbarBtn(toolBarBtn);
                    toolBar.validate();
                    toolBar.repaint();
                }
                
            } else if (isEmpty && stdList.size() > 0)
            {
                String ds = AppContextMgr.getInstance().getClassObject(Discipline.class).getType();
                AppPreferences.getRemote().putBoolean(IS_USING_INTERACTIONS_PREFNAME+ds, true);
                prefsChanged(new CommandAction(null, null, AppPreferences.getRemote()));
                
                if (origNumStd == 0)
                {
                    JToolBar toolBar = (JToolBar)UIRegistry.get(UIRegistry.TOOLBAR);
                    int inx = indexOfTBB != -1 ? indexOfTBB : 4;
                    TaskMgr.addToolbarBtn(toolBarBtn, inx);
                    toolBar.validate();
                    toolBar.repaint();  
                }
            }
        }
    }

    /**
     * Retrieves the prefs that we cache.
     */
    protected void setUpCachedPrefs()
    {
        AppPreferences remotePrefs = AppPreferences.getRemote();
        String ds = AppContextMgr.getInstance().getClassObject(Discipline.class).getType();
        isUsingInteractions = remotePrefs.getBoolean(IS_USING_INTERACTIONS_PREFNAME+ds, true);
    }
    
    /**
     * @return
     */
    protected List<AppResourceIFace> getInvoiceAppResources()
    {
        Vector<AppResourceIFace> ars = new Vector<AppResourceIFace>();
        for (AppResourceIFace ap : AppContextMgr.getInstance().getResourceByMimeType("jrxml/report"))
        {
            Properties params = ap.getMetaDataMap();
            if (params.getProperty("reporttype", "").equals("Invoice"))
            {
                ars.add(ap);
            }
        }
        return ars;
    }
  
    /**
     * @param entry
     * @param tableInfo
     * @return
     */
    private CommandAction createCmdActionFromEntry(final InteractionEntry entry, final DBTableInfo tableInfo)
    {
        CommandAction cmdAction = new CommandAction(entry.getCmdType(), entry.getAction(), tableInfo.getTableId());
        if (StringUtils.isNotEmpty(entry.getViewName()))
        {
            cmdAction.setProperty("view", entry.getViewName());
            cmdAction.setProperty(NavBoxAction.ORGINATING_TASK, this);
        }
        return cmdAction;
    }
            
    /**
     * @param navBox
     * @param tableInfo
     * @param label
     * @param cmdType
     * @param action
     * @param viewName
     * @param iconName
     * @param tableIds
     * @return
     */
    protected NavBoxButton addCommand(final NavBox navBox, 
                                      final DBTableInfo tableInfo,
                                      final InteractionEntry entry)
    {
        CommandAction cmdAction = createCmdActionFromEntry(entry, tableInfo);
        if (StringUtils.isNotEmpty(entry.getViewName()) && entry.isSearchService())
        {
            ContextMgr.registerService(10, entry.getViewName(), tableInfo.getTableId(), cmdAction, this, "Data_Entry", tableInfo.getTitle(), true); // the Name gets Hashed
        }
        
        NavBoxButton roc = (NavBoxButton)makeDnDNavBtn(navBox, entry.getTitle(), entry.getIconName(), cmdAction, null, true, false);// true means make it draggable
        
        for (EntryFlavor ef : entry.getDraggableFlavors())
        {
            if (cmdAction.getAction().equals(PRINT_INVOICE))
            {
            	//this.printableInvoiceTblIds.add(ef.getDataFlavorClass().
            	System.out.println(ef);
            }
            DataFlavorTableExt dfx = new DataFlavorTableExt(ef.getDataFlavorClass(), ef.getHumanReadable(), ef.getTableIdsAsArray());
            roc.addDragDataFlavor(dfx);
        }
        
        for (EntryFlavor ef : entry.getDroppableFlavors())
        {
            if (cmdAction.getAction().equals(PRINT_INVOICE))
            {
            	if (ef.getClassName().equals(RecordSetTask.class.getName()))
            	{
            		this.printableInvoiceTblIds.addAll(ef.getTableIds());
            	}
            }
        	DataFlavorTableExt dfx = new DataFlavorTableExt(ef.getDataFlavorClass(), ef.getHumanReadable(), ef.getTableIdsAsArray());
            roc.addDropDataFlavor(dfx);
        }
        return roc;
    }
            
    /**
     * Helper function to load all the items for a class of data objects (i.e. Loans, Gifts)
     * @param navBox the parent
     * @param dataClass the class to be loaded
     * @param dragFlav the drag flavor of the item
     */
    protected void loadNavBox(final NavBox     navBox, 
                              final Class<?>   dataClass,
                              final DataFlavor dragFlav,
                              final boolean    addBox)
    {
        DBTableInfo ti = DBTableIdMgr.getInstance().getByShortClassName(dataClass.getSimpleName());
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            
            String sql = "FROM " + ti.getClassName() + " WHERE collectionMemberId = COLMEMID";
            List<?> list = session.getDataList(QueryAdjusterForDomain.getInstance().adjustSQL(sql));
            for (Iterator<?> iter=list.iterator();iter.hasNext();)
            {
                FormDataObjIFace   dataObj = (FormDataObjIFace)iter.next();
                createNavBtn(navBox, dragFlav, dataObj, ti);
            }
            if (addBox)
            {
                navBoxes.add(navBox);
            }
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);
            
        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
    }
    
    /**
     * @param navBox
     * @param dragFlav
     * @param dataObj
     * @param ti
     * @return
     */
    protected NavBoxItemIFace createNavBtn(final NavBox     navBox, 
                                           final DataFlavor dragFlav,
                                           final FormDataObjIFace dataObj, 
                                           final DBTableInfo ti)
    {
        RecordSet rs = new RecordSet();
        rs.initialize();
        rs.set(dataObj.getIdentityTitle(), ti.getTableId(), RecordSet.HIDDEN);
        rs.addItem(dataObj.getId());
        
        CommandAction      cmd     = new CommandAction("Data_Entry", "Edit", rs);
        cmd.setProperty(NavBoxAction.ORGINATING_TASK, this);
        
        CommandActionForDB delCmd  = new CommandActionForDB(INTERACTIONS, DELETE_CMD_ACT, ti.getTableId(), dataObj.getId());
        NavBoxItemIFace    nbi     = makeDnDNavBtn(navBox, dataObj.getIdentityTitle(), ti.getShortClassName(), cmd, delCmd, true, true);
        RolloverCommand    roc     = (NavBoxButton)nbi;
        nbi.setData(rs);
        roc.addDragDataFlavor(dragFlav);
        roc.addDragDataFlavor(Trash.TRASH_FLAVOR);
        return nbi;
    }

    /**
     * Helper method for registering a NavBoxItem as a GhostMouseDropAdapter
     * @param navBox the parent box for the nbi to be added to
     * @param navBoxItemDropZone the nbi in question
     * @param params
     * @return returns the new NavBoxItem
     */
    protected NavBoxItemIFace addToNavBoxAndRegisterAsDroppable(final NavBox              navBox,
                                                                final NavBoxItemIFace     nbi,
                                                                final Properties params)
    {
        NavBoxButton roc = (NavBoxButton)nbi;
        roc.setData(params);

        // When Being Dragged
        roc.addDragDataFlavor(Trash.TRASH_FLAVOR);
        roc.addDragDataFlavor(INTERACTIONS_FLAVOR);

        // When something is dropped on it
        roc.addDropDataFlavor(RecordSetTask.RECORDSET_FLAVOR);

        navBox.add(nbi);
        //labelsList.add(nbi);
        return nbi;
    }
    
    /*
     *  (non-Javadoc)
     * @see edu.ku.brc.specify.core.Taskable#getNavBoxes()
     */
    @Override
    public java.util.List<NavBoxIFace> getNavBoxes()
    {
        initialize();

        extendedNavBoxes.clear();
        extendedNavBoxes.addAll(navBoxes);

        RecordSetTask rsTask = (RecordSetTask)ContextMgr.getTaskByClass(RecordSetTask.class);
        List<NavBoxIFace> nbs = rsTask.getNavBoxes();
        if (nbs != null)
        {
            extendedNavBoxes.addAll(nbs);
        }
        return extendedNavBoxes;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.BaseTask#getStarterPane()
     */
    @Override
    public SubPaneIFace getStarterPane()
    {
        if (starterPane != null)
        {
            return starterPane;
        }
        
        return starterPane = StartUpTask.createFullImageSplashPanel(title, this);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getToolBarItems()
     */
    @Override
    public List<ToolBarItemDesc> getToolBarItems()
    {
        toolbarItems       = new Vector<ToolBarItemDesc>();
        String label       = getResourceString(name);
        String iconNameStr = name;
        String hint = getResourceString("interactions_hint");
        toolBarBtn = createToolbarButton(label, iconNameStr, hint);

        AppPreferences remotePrefs = AppPreferences.getRemote();
        if (remotePrefs == AppPreferences.getRemote())
        {
            String ds = AppContextMgr.getInstance().getClassObject(Discipline.class).getType();
            isUsingInteractions = remotePrefs.getBoolean(IS_USING_INTERACTIONS_PREFNAME+ds, true);
        }

        if (isUsingInteractions)
        {
            toolbarItems.add(new ToolBarItemDesc(toolBarBtn));
        }

        return toolbarItems;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getTaskClass()
     */
    @Override
    public Class<? extends BaseTask> getTaskClass()
    {
        return this.getClass();
    }
    
    
    /**
     * Creates a new loan from a RecordSet.
     * @param fileNameArg the filename of the report (Invoice) to use (can be null)
     * @param recordSets the recordset to use to create the loan
     */
    protected void printInvoice(final String fileNameArg, final Object data)
    {
        if (data instanceof RecordSetIFace)
        {
            RecordSetIFace rs = (RecordSetIFace)data;
            
            InvoiceInfo invoiceInfo = getInvoiceInfo(rs.getDbTableId());
            if (invoiceInfo != null)
            {
            	launchInvoice(invoiceInfo, rs);
            }
        }
    }
    
    /**
     * @param list
     */
    protected void showMissingDetsDlg(final List<CollectionObject> noCurrDetList)
    {
        StringBuilder sb = new StringBuilder();
        for (CollectionObject co : noCurrDetList)
        {
            if (sb.length() > 0) sb.append(", ");
            sb.append(co.getIdentityTitle());
        }
        
        PanelBuilder    pb     = new PanelBuilder(new FormLayout("p:g", "p,2px,p"));
        CellConstraints cc     = new CellConstraints();
        JTextArea       ta     = UIHelper.createTextArea(5, 40);
        JScrollPane     scroll = UIHelper.createScrollPane(ta);
        JLabel          lbl    = UIHelper.createLabel(getResourceString("InteractionsTask.MISSING_DET"));
        
        pb.add(lbl,    cc.xy(1, 1));
        pb.add(scroll, cc.xy(1, 3));
        
        ta.setText(sb.toString());
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        
        pb.setDefaultDialogBorder();
        
        CustomDialog dlg = new CustomDialog((Frame)UIRegistry.getTopWindow(), 
                getResourceString("InteractionsTask.MISSING_DET_TITLE"), 
                true, 
                CustomDialog.OK_BTN, 
                pb.getPanel());
        //dlg.setOkLabel(getResourceString(key))
        dlg.setVisible(true);
    }
    
    /**
     * @return returns a list of RecordSets of InfoRequests
     */
    protected Vector<RecordSetIFace> getInfoReqRecordSetsFromSideBar()
    {
        Vector<RecordSetIFace> rsList = new Vector<RecordSetIFace>();
        for (NavBoxItemIFace nbi : infoRequestNavBox.getItems())
        {
            rsList.add((RecordSet)nbi.getData());
        }
        return rsList;
    }
    /**
     * @param existingLoan
     * @param infoRequest
     * @param prepsHash
     */
    protected void addPrepsToLoan(final PreparationsProviderIFace   existingLoanArg, 
                                  final InfoRequest                 infoRequest,
                                  final Hashtable<Integer, Integer> prepsHash,
                                  final Viewable                    srcViewable)
    {
        Loan existingLoan = (Loan)existingLoanArg;
        Loan loan;
        
        if (existingLoan == null)
        {
            loan = new Loan();
            loan.initialize();
            
            Calendar dueDate = Calendar.getInstance();
            dueDate.add(Calendar.MONTH, 6);                 // XXX PREF Due Date
            loan.setCurrentDueDate(dueDate);
            
            Shipment shipment = new Shipment();
            shipment.initialize();
            
            // Get Defaults for Certain fields
            //SpecifyAppContextMgr appContextMgr     = (SpecifyAppContextMgr)AppContextMgr.getInstance();
            
            // Comment out defaults for now until we can manage them
            //PickListItemIFace    defShipmentMethod = appContextMgr.getDefaultPickListItem("ShipmentMethod", getResourceString("SHIPMENT_METHOD"));
            //if (defShipmentMethod != null)
            //{
            //     shipment.setShipmentMethod(defShipmentMethod.getValue());
            //}
            
            //FormDataObjIFace shippedBy = appContextMgr.getDefaultObject(Agent.class, "ShippedBy", getResourceString("SHIPPED_BY"), true, false);
            //if (shippedBy != null)
            //{
            //    shipment.setShippedBy((Agent)shippedBy);
            //}
            
            if (infoRequest != null && infoRequest.getAgent() != null)
            {
                shipment.setShippedTo(infoRequest.getAgent());
            }
            
            loan.addReference(shipment, "shipments");
        } else
        {
            loan = existingLoan;
        }
        
        Hashtable<Integer, LoanPreparation> prepToLoanPrepHash = null;
        if (existingLoan != null)
        {
            prepToLoanPrepHash = new Hashtable<Integer, LoanPreparation>();
            for (LoanPreparation lp : existingLoan.getLoanPreparations())
            {
                prepToLoanPrepHash.put(lp.getPreparation().getId(), lp);
            }
        }
        
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            
            for (Integer prepId : prepsHash.keySet())
            {
                Preparation prep  = session.get(Preparation.class, prepId);
                Integer     count = prepsHash.get(prepId);
                if (prepToLoanPrepHash != null)
                {
                    LoanPreparation lp = prepToLoanPrepHash.get(prep.getId());
                    if (lp != null)
                    {
                        int lpCnt = lp.getQuantity();
                        lpCnt += count;
                        lp.setQuantity(lpCnt);
                        continue;
                    }
                }
                
                LoanPreparation lpo = new LoanPreparation();
                lpo.initialize();
                lpo.setPreparation(prep);
                lpo.setQuantity(count);
                lpo.setLoan(loan);
                loan.getLoanPreparations().add(lpo);
            }
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);

        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
        
        if (existingLoan == null)
        {
            if (srcViewable != null)
            {
                srcViewable.setNewObject(loan);
                
            } else
            {
                DataEntryTask dataEntryTask = (DataEntryTask)TaskMgr.getTask(DataEntryTask.DATA_ENTRY);
                if (dataEntryTask != null)
                {
                    DBTableInfo loanTableInfo = DBTableIdMgr.getInstance().getInfoById(loan.getTableId());
                    dataEntryTask.openView(this, null, loanTableInfo.getDefaultFormName(), "edit", loan, true);
                }
            }
        } else 
        {
            CommandDispatcher.dispatch(new CommandAction(INTERACTIONS, "REFRESH_LOAN_PREPS", loan));
        }
    }
    
    /**
     * @param existingLoan
     * @param infoRequest
     * @param prepsHash
     */
    protected void addPrepsToGift(final PreparationsProviderIFace existingGiftArg, 
                                  final InfoRequest               infoRequest,
                                  final Hashtable<Integer, Integer> prepsHash,
                                  final Viewable                    srcViewable)
    {
        Gift existingGift = (Gift)existingGiftArg;
        Gift gift;
        
        if (existingGift == null)
        {
            gift = new Gift();
            gift.initialize();
            
            Calendar dueDate = Calendar.getInstance();
            dueDate.add(Calendar.MONTH, 6);                 // XXX PREF Due Date
            
            Shipment shipment = new Shipment();
            shipment.initialize();
            
            // Get Defaults for Certain fields
            //SpecifyAppContextMgr appContextMgr     = (SpecifyAppContextMgr)AppContextMgr.getInstance();
            
            // Comment out defaults for now until we can manage them
            //PickListItemIFace    defShipmentMethod = appContextMgr.getDefaultPickListItem("ShipmentMethod", getResourceString("SHIPMENT_METHOD"));
            //if (defShipmentMethod != null)
            //{
            //     shipment.setShipmentMethod(defShipmentMethod.getValue());
            //}
            
            //FormDataObjIFace shippedBy = appContextMgr.getDefaultObject(Agent.class, "ShippedBy", getResourceString("SHIPPED_BY"), true, false);
            //if (shippedBy != null)
            //{
            //    shipment.setShippedBy((Agent)shippedBy);
            //}
            
            if (infoRequest != null && infoRequest.getAgent() != null)
            {
                shipment.setShippedTo(infoRequest.getAgent());
            }
            
            gift.addReference(shipment, "shipments");
        } else
        {
            gift = existingGift;
        }
        
        Hashtable<Integer, GiftPreparation> prepToGiftPrepHash = null;
        if (existingGift != null)
        {
            prepToGiftPrepHash = new Hashtable<Integer, GiftPreparation>();
            for (GiftPreparation lp : existingGift.getGiftPreparations())
            {
                prepToGiftPrepHash.put(lp.getPreparation().getId(), lp);
            }
        }
        
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            
            for (Integer prepId : prepsHash.keySet())
            {
                Preparation prep  = session.get(Preparation.class, prepId);
                Integer     count = prepsHash.get(prepId);
                if (prepToGiftPrepHash != null)
                {
                    GiftPreparation gp = prepToGiftPrepHash.get(prep.getId());
                    if (gp != null)
                    {
                        int lpCnt = gp.getQuantity();
                        lpCnt += count;
                        gp.setQuantity(lpCnt);
                        //System.err.println("Adding "+count+"  to "+lp.hashCode());
                        continue;
                    }
                }
                
                GiftPreparation gpo = new GiftPreparation();
                gpo.initialize();
                gpo.setPreparation(prep);
                gpo.setQuantity(count);
                gpo.setGift(gift);
                gift.getGiftPreparations().add(gpo);
            }
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);

        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
        
        if (existingGift == null)
        {
            if (srcViewable != null)
            {
                srcViewable.setNewObject(gift);
                
            } else
            {
                DataEntryTask dataEntryTask = (DataEntryTask)TaskMgr.getTask(DataEntryTask.DATA_ENTRY);
                if (dataEntryTask != null)
                {
                    DBTableInfo giftTableInfo = DBTableIdMgr.getInstance().getInfoById(gift.getTableId());
                    dataEntryTask.openView(this, null, giftTableInfo.getDefaultFormName(), "edit", gift, true);
                }
            }
        } else 
        {
            CommandDispatcher.dispatch(new CommandAction(INTERACTIONS, "REFRESH_GIFT_PREPS", gift));
        }
    }
    
    /**
     * Displays UI that asks the user to select a predefined label.
     * @return the name of the label file or null if canceled
     */
    protected String askForInvoiceName()
    {
        List<AppResourceIFace> invoiceReports = getInvoiceAppResources();
        
        if (invoiceReports.size() == 1)
        {
            return (String)invoiceReports.get(0).getMetaDataMap().get("file");

        }
        
        ChooseFromListDlg<AppResourceIFace> dlg = new ChooseFromListDlg<AppResourceIFace>((Frame)UIRegistry.getTopWindow(),
                                                                                        getResourceString("ChooseInvoice"), 
                                                                                        invoiceReports, 
                                                                                        IconManager.getIcon(name, IconManager.IconSize.Std24));
        dlg.setMultiSelect(false);
        dlg.setModal(true);
        dlg.setVisible(true);
        if (!dlg.isCancelled())
        {
            return  (String)dlg.getSelectedObject().getMetaDataMap().get("file");
        }
        return null;
    }
    
    /**
     * Creates a new InfoRequest from a RecordSet.
     * @param recordSet the recordSet to use to create the InfoRequest
     */
    protected void createInfoRequest(final RecordSetIFace recordSetArg)
    {
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getInfoById(InfoRequest.getClassTableId());
        
        ViewIFace view = AppContextMgr.getInstance().getView(tableInfo.getDefaultFormName());

        InfoRequest infoRequest = new InfoRequest();
        infoRequest.initialize();
        
        RecordSetTask          rsTask       = (RecordSetTask)TaskMgr.getTask(RecordSetTask.RECORD_SET);
        List<RecordSetIFace>   colObjRSList = rsTask.getRecordSets(CollectionObject.getClassTableId());

        RecordSetIFace recordSetFromDB = getRecordSetOfColObj(recordSetArg, colObjRSList.size());
        
        if (recordSetFromDB != null)
        {
            RecordSet recordSet = RecordSetTask.copyRecordSet(recordSetFromDB);
            if (recordSet == null)
            {
                UIRegistry.showLocalizedError("ERROR_COPYING_RS");
                return;
            }
            recordSet.setName(recordSet.getName() + "_IT");
            
            infoRequest.addReference(recordSet, "recordSets");
            
            if (recordSet.getOrderedItems() != null && recordSet.getNumItems() > 0)
            {
                Taskable irTask = TaskMgr.getTask("InfoRequest");
                createFormPanel(irTask.getTitle(), view.getViewSetName(), view.getName(), "edit", infoRequest, MultiView.IS_NEW_OBJECT, 
                                irTask.getIcon(16));
                
            } else
            {
                UIRegistry.displayErrorDlgLocalized("ERROR_MISSING_RS_OR_NOITEMS");  
            }
        }
    }

    
    /**
     * @param cmdAction
     */
    protected void checkToPrintLoan(final CommandAction cmdAction)
    {
        if (cmdAction.getData() instanceof Loan)
        {
            Loan loan = (Loan)cmdAction.getData();
            
            Boolean     printLoan   = null;
            FormViewObj formViewObj = getCurrentFormViewObj();
            if (formViewObj != null)
            {
                Component comp = formViewObj.getControlByName("generateInvoice");
                if (comp instanceof JCheckBox)
                {
                    printLoan = ((JCheckBox)comp).isSelected();
                }
            }
            
            if (printLoan == null)
            {
                Object[] options = {getResourceString("CreateLoanInvoice"), getResourceString("CANCEL")};
                int n = JOptionPane.showOptionDialog(UIRegistry.get(UIRegistry.FRAME),
                                                    String.format(getResourceString("CreateLoanInvoiceForNum"), new Object[] {(loan.getLoanNumber())}),
                                                    getResourceString("CreateLoanInvoice"),
                                                    JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.QUESTION_MESSAGE,
                                                    null,     //don't use a custom Icon
                                                    options,  //the titles of buttons
                                                    options[0]); //default button title
                printLoan = n == 0;
            }
            
            // XXX DEBUG
            //printLoan = false;
            if (printLoan)
            {
                InvoiceInfo invoice = getLoanReport();
                
                if (invoice == null)
                {
                    return;
                }
                
                
                DataProviderSessionIFace session = null;
                try
                {
                    session = DataProviderFactory.getInstance().createSession();
                    //session.attach(loan);
                    loan = (Loan)session.getData("From Loan where loanId = "+loan.getLoanId());
                    
//                    if (loan.getShipments().size() == 0)
//                    {
//                        UIRegistry.displayErrorDlg(getResourceString("NO_SHIPMENTS_ERROR"));
//                        
//                    } else if (loan.getShipments().size() > 1)
//                    {
//                        // XXX Do we allow them to pick a shipment or print all?
//                        UIRegistry.displayErrorDlg(getResourceString("MULTI_SHIPMENTS_NOT_SUPPORTED"));
//                        
//                    } else
//                    {
//                        // XXX At the moment this is just checking to see if there is at least one "good/valid" shipment
//                        // but the hard part will be sending the correct info so the report can be printed
//                        // using bouth a Loan Id and a Shipment ID, and at some point distinguishing between using
//                        // the shipped by versus the shipper.
//                        Shipment shipment = loan.getShipments().iterator().next();
//                        if (shipment.getShippedBy() == null)
//                        {
//                            UIRegistry.displayErrorDlg(getResourceString("SHIPMENT_MISSING_SHIPPEDBY"));
//                        } else if (shipment.getShippedBy().getAddresses().size() == 0)
//                        {
//                            UIRegistry.displayErrorDlg(getResourceString("SHIPPEDBY_MISSING_ADDR"));
//                        } else if (shipment.getShippedTo() == null)
//                        {
//                            UIRegistry.displayErrorDlg(getResourceString("SHIPMENT_MISSING_SHIPPEDTO"));
//                        } else if (shipment.getShippedTo().getAddresses().size() == 0)
//                        {
//                            UIRegistry.displayErrorDlg(getResourceString("SHIPPEDTO_MISSING_ADDR"));
//                        } else
//                        {
                            //session.close();
                            //session = null;
                            
                            RecordSet rs = new RecordSet();
                            rs.initialize();
                            rs.setName(loan.getIdentityTitle());
                            rs.setDbTableId(loan.getTableId());
                            rs.addItem(loan.getId());
                            
                            launchInvoice(invoice, rs);
                } finally
                {
                    if (session != null)
                    {
                        session.close();
                    }
                }
            }
        }
    }
    
    /**
     * @param invoice
     * @param rs
     * 
     * Builds and dispatches command to launch invoice
     */
    protected void launchInvoice(final InvoiceInfo invoice, final RecordSetIFace rs)
    {
        if (invoice.getSpReport() != null)
        {
            SpReport spRep = invoice.getSpReport();
            QueryBldrPane.runReport(spRep, UIRegistry.getResourceString("LoanInvoice"),
                    rs);
        }
        else
        {
            SpAppResource rep = invoice.getSpAppResource();
            CommandAction cmd = new CommandAction(ReportsBaseTask.REPORTS, ReportsBaseTask.PRINT_REPORT,
                    rs);
            cmd.getProperties().put("title", rep.getName());
            cmd.getProperties().put("file", rep.getFileName());
            cmd.getProperties().put("reporttype", "report");
            cmd.getProperties().put("name", rep.getName());
            CommandDispatcher.dispatch(cmd);
        }
    }
    
    public InvoiceInfo getLoanReport()
    {
    	return getInvoiceInfo(DBTableIdMgr.getInstance().getIdByShortName("Loan"));
    }
    
    /**
     * @return a loan invoice if one exists.
     * 
     * If more than one report is defined for loan then user must choose.
     * 
     * Fairly goofy code. Eventually may want to add ui to allow labeling resources as "invoice" (see printLoan()).
     */
    public InvoiceInfo getInvoiceInfo(final int invoiceTblId)
    {
        DataProviderSessionIFace session = null;
        ChooseFromListDlg<InvoiceInfo> dlg = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            List<AppResourceIFace> reps = AppContextMgr.getInstance().getResourceByMimeType(ReportsBaseTask.LABELS_MIME);
            reps.addAll(AppContextMgr.getInstance().getResourceByMimeType(ReportsBaseTask.REPORTS_MIME));
            Vector<InvoiceInfo> repInfo = new Vector<InvoiceInfo>();
            
            for (AppResourceIFace rep : reps)
            {
                Properties params = rep.getMetaDataMap();
                String tableid = params.getProperty("tableid"); 
                SpReport spReport = null;
                boolean includeIt = false;
                try
                {
                    Integer tblId = null;
                	try
                    {
                    	tblId = Integer.valueOf(tableid);
                    }
                    catch (NumberFormatException ex)
                    {
                    	//continue;
                    }
                    if (tblId == null)
                    {
                    	continue;
                    }
                    
                    if (tblId.equals(invoiceTblId))
                    {
                        includeIt = true;
                    }
                    else if (tblId.equals(-1))
                    {
                        QueryIFace q = session.createQuery("from SpReport spr join spr.appResource apr "
                              + " join spr.query spq "
                              + "where apr.id = " + ((SpAppResource )rep).getId() 
                              + " and spq.contextTableId = " + invoiceTblId, false);
                        List<?> spReps = q.list();
                        if (spReps.size() > 0)
                        {
                            includeIt = true;
                            spReport = (SpReport )((Object[] )spReps.get(0))[0];
                            spReport.forceLoad();
                            if (spReps.size() > 1)
                            {
                                //should never happen
                                log.error("More than SpReport exists for " + rep.getName());
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);
                    //skip this res
                }
                if (includeIt)
                {
                    repInfo.add(new InvoiceInfo((SpAppResource )rep, spReport));
                }
            }
            
            if (repInfo.size() == 0)
            {
            	UIRegistry.displayInfoMsgDlgLocalized("InteractionsTask.NoInvoiceFound", 
                			DBTableIdMgr.getInstance().getTitleForId(invoiceTblId));
                return null;
            }
            
            if (repInfo.size() == 1)
            {
                return repInfo.get(0);
            }
            
            dlg = new ChooseFromListDlg<InvoiceInfo>((Frame) UIRegistry
                    .getTopWindow(), UIRegistry.getResourceString("REP_CHOOSE_INVOICE"),
                    repInfo);
            dlg.setVisible(true);
            if (dlg.isCancelled()) 
            { 
                return null; 
            }
            return dlg.getSelectedObject();
            
//            for (AppResourceIFace res : AppContextMgr.getInstance().getResourceByMimeType(ReportsBaseTask.LABELS_MIME))
//            {
//                Properties params = res.getMetaDataMap();
//                String tableid = params.getProperty("tableid"); 
//                boolean includeIt = false;
//                try
//                {
//                    includeIt = Integer.valueOf(tableid).equals(loanTblId);
//                }
//                catch (Exception ex)
//                {
//                    //skip this res
//                }
//                if (includeIt)
//                {
//                    aprs.add((SpAppResource )res);
//                }
//            }
//            for (AppResourceIFace res : AppContextMgr.getInstance().getResourceByMimeType(ReportsBaseTask.REPORTS_MIME))
//            {
//                Properties params = res.getMetaDataMap();
//                String tableid = params.getProperty("tableid"); 
//                boolean includeIt = false;
//                try
//                {
//                    includeIt = Integer.valueOf(tableid).equals(loanTblId);
//                }
//                catch (Exception ex)
//                {
//                    // skip this res
//                }
//                if (includeIt)
//                {
//                    aprs.add((SpAppResource) res);
//                }
//            }
//            
//            if (aprs.size() == 0)
//            {
//                return null;
//            }
//            
//            if (aprs.size() == 1)
//            {
//                return aprs.get(0);
//            }
            
            
//            Vector<String> reportNames = new Vector<String>();
//            for (Integer id : ids)
//            {
//                QueryIFace q = session.createQuery("from SpReport spr join spr.appResource apr "
//                    + "where apr.id = " + id.toString(), false);
//                List<?> reports = q.list();
//                for (Object repObj : reports)
//                {
//                    reportNames.add(((SpReport )repObj).getName());
//                }
//            if (reports.size() == 0)
//            {
//                return null;
//            }
//            if (reports.size() == 1)
//            {
//                SpReport result = (SpReport )((Object[] )reports.get(0))[0];
//                result.forceLoad();
//                return result;
//            }
//
//            Vector<SpReport> reps = new Vector<SpReport>(reports.size());
//            for (Object rep : reports)
//            {
//                reps.add((SpReport )((Object[] )rep)[0]);
//            }
            
//            dlg = new ChooseFromListDlg<SpAppResource>((Frame) UIRegistry
//                    .getTopWindow(), UIRegistry.getResourceString("REP_CHOOSE_SP_REPORT"),
//                    aprs);
//            dlg.setVisible(true);
//            if (dlg.isCancelled()) 
//            { 
//                return null; 
//            }
//            return dlg.getSelectedObject();
//            result.forceLoad();
//            return result;
        }
        finally
        {
            session.close();
            if (dlg != null)
            {
                dlg.dispose();
            }
        }
    }
    /**
     * Creates an Excel SpreadSheet or CVS file and attaches it to an email and send it to an agent.
     */
    public void createAndSendEMail()
    {
        FormViewObj formViewObj = getCurrentFormViewObj();
        if (formViewObj != null && formViewObj.getDataObj() instanceof InfoRequest) 
        {
            InfoRequest infoRequest = (InfoRequest)formViewObj.getDataObj();
            Agent       toAgent     = infoRequest.getAgent();
            
            boolean   sendEMail = true; // default to true
            Component comp      = formViewObj.getControlByName("sendEMail");
            if (comp instanceof JCheckBox)
            {
                sendEMail = ((JCheckBox)comp).isSelected();
            }
            
            MultiView mv = formViewObj.getSubView("InfoRequestColObj");
            if (mv != null && sendEMail)
            {
                final Viewable viewable = mv.getCurrentView();
                if (viewable instanceof TableViewObj)
                {
                    final Hashtable<String, String> emailPrefs = new Hashtable<String, String>();
                    if (!EMailHelper.isEMailPrefsOK(emailPrefs))
                    {
                        JOptionPane.showMessageDialog(UIRegistry.getTopWindow(), 
                                getResourceString("NO_EMAIL_PREF_INFO"), 
                                getResourceString("NO_EMAIL_PREF_INFO_TITLE"), JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    final File tempExcelFileName = TableModel2Excel.getTempExcelName();
                    
                    emailPrefs.put("to", toAgent.getEmail() != null ? toAgent.getEmail() : "");
                    emailPrefs.put("from", emailPrefs.get("email"));
                    emailPrefs.put("subject", String.format(getResourceString("INFO_REQUEST_SUBJECT"), new Object[] {infoRequest.getIdentityTitle()}));
                    emailPrefs.put("bodytext", "");
                    emailPrefs.put("attachedFileName", tempExcelFileName.getName());
                    
                    final Frame topFrame = (Frame)UIRegistry.getTopWindow();
                    final ViewBasedDisplayDialog dlg = new ViewBasedDisplayDialog(topFrame,
                                                  "SystemSetup",
                                                  "SendMail",
                                                  null,
                                                  getResourceString("SEND_MAIL_TITLE"),
                                                  getResourceString("SEND_BTN"),
                                                  null, // className,
                                                  null, // idFieldName,
                                                  true, // isEdit,
                                                  0);
                    dlg.setData(emailPrefs);
                    dlg.setModal(true);
                    dlg.setVisible(true);
                    
                    if (dlg.getBtnPressed() == ViewBasedDisplayIFace.OK_BTN)
                    {
                        dlg.getMultiView().getDataFromUI();
                        
                        //System.out.println("["+emailPrefs.get("bodytext")+"]");
                        
                        TableViewObj  tblViewObj = (TableViewObj)viewable;
                        File          excelFile  = TableModel2Excel.convertToExcel(tempExcelFileName, 
                                                                                   getResourceString("CollectionObject"), 
                                                                                   tblViewObj.getTable().getModel());
                        StringBuilder sb         = TableModel2Excel.convertToHTML(getResourceString("CollectionObject"), 
                                                                                  tblViewObj.getTable().getModel());
                        
                        //EMailHelper.setDebugging(true);
                        String text = emailPrefs.get("bodytext").replace("\n", "<br>") + "<BR><BR>" + sb.toString();
                        UIRegistry.displayLocalizedStatusBarText("SENDING_EMAIL");
                        
                        String password = Encryption.decrypt(emailPrefs.get("password"));
                        if (StringUtils.isEmpty(password))
                        {
                            password = EMailHelper.askForPassword(topFrame);
                        }
                        
                        if (StringUtils.isNotEmpty(password))
                        {
                            final EMailHelper.ErrorType status = EMailHelper.sendMsg(emailPrefs.get("smtp"), 
                                                                                       emailPrefs.get("username"), 
                                                                                       password, 
                                                                                       emailPrefs.get("email"), 
                                                                                       emailPrefs.get("to"), 
                                                                                       emailPrefs.get("subject"), 
                                                                                       text, 
                                                                                       EMailHelper.HTML_TEXT, 
                                                                                       emailPrefs.get("port"), 
                                                                                       emailPrefs.get("security"), 
                                                                                       excelFile);
                            if (status != EMailHelper.ErrorType.Cancel)
                            {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        UIRegistry.displayLocalizedStatusBarText(status == EMailHelper.ErrorType.Error ? "EMAIL_SENT_ERROR" : "EMAIL_SENT_OK");
                                    }
                                });
                            }
                        }
                    }
                }
            }
        } else
        {
            log.error("Why doesn't the current SubPane have a main FormViewObj?");
        }
    }
    
    /**
     * Delete a InfoRequest..
     * @param infoRequest the infoRequest to be deleted
     */
    protected InfoRequest deleteInfoRequest(final int infoReqId)
    {
        InfoRequest infoRequest = null;
        // delete from database
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        try
        {
            infoRequest = session.get(InfoRequest.class, infoReqId);
            if (infoRequest != null)
            {
                session.beginTransaction();
                session.delete(infoRequest);
                session.commit();
            }
            
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);
            ex.printStackTrace();
            log.error(ex);
            
        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
        return infoRequest;
    }
    
    /**
     * Delete the InfoRequest from the UI, which really means remove the NavBoxItemIFace. 
     * This method first checks to see if the boxItem is not null and uses that, if
     * it is null then it looks the box up by name ans used that
     * @param boxItem the box item to be deleted
     * @param infoRequest the infoRequest that is "owned" by some UI object that needs to be deleted (used for secodary lookup
     */
    protected void deleteInfoRequestFromUI(final NavBoxItemIFace boxItem, final InfoRequest infoRequest)
    {
        deleteDnDBtn(infoRequestNavBox, boxItem != null ? boxItem : getBoxByTitle(infoRequest.getIdentityTitle()));
    }
    
    /**
     * Starts process to return a loan.
     * @param multiView the current form doing the return
     * @param loan the loan being returned
     * @param agent the agent doing the return
     * @param returns the list of items being returned
     */
    protected void doReturnLoan(final MultiView            multiView,
                                final Loan                 loanArg,
                                final Agent                agent, 
                                final List<LoanReturnInfo> returns)
    {
        final JStatusBar statusBar = UIRegistry.getStatusBar();
        statusBar.setIndeterminate(INTERACTIONS, true);
        
        String msg = getResourceString("ReturningLoanItems");
        statusBar.setText(msg);
        UIRegistry.writeSimpleGlassPaneMsg(msg, 24);
        
        final SwingWorker worker = new SwingWorker()
        {
            protected int  numLPR = 0;
            protected Loan loan   = null;
            
            @Override
            public Object construct()
            {
                DataProviderSessionIFace session = null;
                try
                {
                    session = DataProviderFactory.getInstance().createSession();
                    
                    session.beginTransaction();
                    
                    loan = session.merge(loanArg);
                    
                    for (LoanReturnInfo loanRetInfo : returns)
                    {   
                        LoanPreparation loanPrep = loanRetInfo.getLoanPreparation();
                        for (LoanPreparation lp : loan.getLoanPreparations())
                        {
                            if (loanPrep.getId().equals(lp.getId()))
                            {
                                loanPrep = lp;
                            }
                        }
                        
                        // The loanRetInfo contains the total number of Resolved and Returned
                        // so we need to go get the number already resolved/returned and subtract it
                        // to get the remaining difference for this last LoanReturnPrep
                        int qtyRes = 0;
                        int qtyRet = 0;
                        for (LoanReturnPreparation lrp : loanPrep.getLoanReturnPreparations())
                        {
                            qtyRes += lrp.getQuantityResolved();
                            qtyRet += lrp.getQuantityReturned();
                        }
                        
                        LoanReturnPreparation loanRetPrep = new LoanReturnPreparation();
                        loanRetPrep.initialize();
                        loanRetPrep.setReceivedBy(agent);
                        loanRetPrep.setModifiedByAgent(Agent.getUserAgent());
                        loanRetPrep.setReturnedDate(Calendar.getInstance());
                        loanRetPrep.setQuantityResolved(loanRetInfo.getResolvedQty() - qtyRes);
                        loanRetPrep.setQuantityReturned(loanRetInfo.getReturnedQty() - qtyRet);
                        
                        loanRetPrep.setRemarks(loanRetInfo.getRemarks());
                        
                        loanPrep.setIsResolved(loanRetInfo.isResolved());
                        loanPrep.setQuantityResolved(loanRetInfo.getResolvedQty());
                        loanPrep.setQuantityReturned(loanRetInfo.getReturnedQty());
                        loanPrep.addReference(loanRetPrep, "loanReturnPreparations");
                        
                        session.save(loanRetPrep);
                        session.saveOrUpdate(loanPrep);
                        session.saveOrUpdate(loan);
                    }
                    
                    boolean isClosed = true;
                    for (LoanPreparation lp : loan.getLoanPreparations())
                    {
                        if (lp.getQuantityResolved().equals(lp.getQuantity()))
                        {
                            if (!lp.getIsResolved())
                            {
                                lp.setIsResolved(true);
                            }
                        } else
                        {
                            isClosed = false;
                        }
                    }
                    loan.setIsClosed(isClosed);
                    session.saveOrUpdate(loan);
                    
                    session.commit();
                    
                    numLPR = returns.size();
                    
                } catch (Exception ex)
                {
                    // Error Dialog
                    ex.printStackTrace();
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);
                    
                } finally
                {
                    if (session != null)
                    {
                        session.close();
                    }
                }
                return null;
            }

            //Runs on the event-dispatching thread.
            @Override
            public void finished()
            {
                statusBar.setProgressDone(INTERACTIONS);
                statusBar.setText("");
                multiView.setIsNewForm(false, true);
                multiView.setData(null);
                multiView.setData(loan);
                UIRegistry.clearSimpleGlassPaneMsg();
                
                UIRegistry.showLocalizedMsg(JOptionPane.INFORMATION_MESSAGE, "InteractionsTask.LN_RET_TITLE", "InteractionsTask.RET_LN_SV", numLPR);
            }
        };
        worker.start();
    }
    
    /**
     * Starts process to return a loan
     * @param doPartial true means show dialog and do partial, false means just return the loan
     */
    protected void returnLoan()
    {
        Loan         loan    = null;
        MultiView    mv      = null;
        SubPaneIFace subPane = SubPaneMgr.getInstance().getCurrentSubPane();
        if (subPane != null)
        {
            mv = subPane.getMultiView();
            if (mv != null)
            {
                if (mv.getData() instanceof Loan)
                {
                    loan = (Loan)mv.getData();
                }
            }
        }
        
        if (mv != null && loan != null)
        {
            LoanReturnDlg dlg = new LoanReturnDlg(loan);
            if (dlg.createUI())
            {
                dlg.setModal(true);
                UIHelper.centerAndShow(dlg);
                dlg.dispose();
                
                if (!dlg.isCancelled())
                {
                    FormViewObj fvp = mv.getCurrentViewAsFormViewObj();
                    fvp.setHasNewData(true);
                    // 03/04/09 Commented out the two lines below so the form doesn't get enabled for saving.
                    //fvp.getValidator().setHasChanged(true);
                    //fvp.validationWasOK(fvp.getValidator().getState() == UIValidatable.ErrorType.Valid);
                   
                    List<LoanReturnInfo> returns = dlg.getLoanReturnInfo();
                    if (returns.size() > 0)
                    {
                        doReturnLoan(mv, loan, dlg.getAgent(), returns);
                    }
                }
            }
            
        } else
        {
            // XXX Show some kind of error dialog
        }
    }
    
    /**
     * 
     */
    protected void createLoanNoPreps(final Viewable srcViewable)
    {
        Loan loan = new Loan();
        loan.initialize();
        
        Calendar dueDate = Calendar.getInstance();
        dueDate.add(Calendar.MONTH, 6);                 // XXX PREF Due Date
        loan.setCurrentDueDate(dueDate);
        
        Shipment shipment = new Shipment();
        shipment.initialize();
        
        loan.addReference(shipment, "shipments");
        
        if (srcViewable != null)
        {
            srcViewable.setNewObject(loan);
            
        } else
        {
            DataEntryTask dataEntryTask = (DataEntryTask)TaskMgr.getTask(DataEntryTask.DATA_ENTRY);
            if (dataEntryTask != null)
            {
                DBTableInfo loanTableInfo = DBTableIdMgr.getInstance().getInfoById(loan.getTableId());
                FormPane formPane = dataEntryTask.openView(this, null, loanTableInfo.getDefaultFormName(), "edit", loan, true);
                if (formPane != null)
                {
                    MultiView mv = formPane.getMultiView();
                    if (mv != null)
                    {
                        FormViewObj fvo = mv.getCurrentViewAsFormViewObj();
                        if (fvo != null && fvo.getBusinessRules() != null && fvo.getBusinessRules() instanceof LoanBusRules)
                        {
                            ((LoanBusRules)fvo.getBusinessRules()).setDoCreateLoanNoPreps(true);
                        }
                    }
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#subPaneRemoved(edu.ku.brc.af.core.SubPaneIFace)
     */
    @Override
    public void subPaneRemoved(final SubPaneIFace subPane)
    {
        super.subPaneRemoved(subPane);
        
        if (subPane == starterPane)
        {
            starterPane = null;
        }
    }
    
    //-------------------------------------------------------
    // CommandListener Interface
    //-------------------------------------------------------
    /**
     * 
     */
    protected void prefsChanged(final CommandAction cmdAction)
    {
        AppPreferences remotePrefs = (AppPreferences)cmdAction.getData();
        
        if (remotePrefs == AppPreferences.getRemote())
        {
            String ds = AppContextMgr.getInstance().getClassObject(Discipline.class).getType();
            isUsingInteractions = remotePrefs.getBoolean(IS_USING_INTERACTIONS_PREFNAME+ds, true);
            
            JToolBar toolBar = (JToolBar)UIRegistry.get(UIRegistry.TOOLBAR);
            if (!isUsingInteractions)
            {
                indexOfTBB = toolBar.getComponentIndex(toolBarBtn);
                TaskMgr.removeToolbarBtn(toolBarBtn);
                toolBar.validate();
                toolBar.repaint();
                
            } else
            {
                int curInx = toolBar.getComponentIndex(toolBarBtn);
                if (curInx == -1)
                {
                    int inx = indexOfTBB != -1 ? indexOfTBB : 4;
                    TaskMgr.addToolbarBtn(toolBarBtn, inx);
                    toolBar.validate();
                    toolBar.repaint();
                }
            }
        }
    }
    
    /**
     * Processes all Commands of type RECORD_SET.
     * @param cmdAction the command to be processed
     */
    protected void processRecordSetCommands(final CommandAction cmdAction)
    {
        Object data = cmdAction.getData();
        UsageTracker.incrUsageCount("IN."+cmdAction.getType()+(data != null ? ("."+data.getClass().getSimpleName()) : ""));

        if (cmdAction.isAction("Clicked"))
        {
            Object srcObj = cmdAction.getSrcObj();
            Object dstObj = cmdAction.getDstObj();
            
            log.debug("********* In Labels doCommand src["+srcObj+"] dst["+dstObj+"] data["+data+"] context["+ContextMgr.getCurrentContext()+"]");
             
            if (ContextMgr.getCurrentContext() == this)
            {
                if (dstObj instanceof RecordSetIFace)
                {
                    RecordSetIFace rs = (RecordSetIFace)dstObj;
                    if (rs.getDbTableId() == Loan.getClassTableId())
                    {
                        DBTableInfo ti = DBTableIdMgr.getInstance().getInfoById(rs.getDbTableId());
                        
                        super.createFormPanel(ti.getTitle(), "view", rs, IconManager.getIcon(ti.getShortClassName(), IconManager.IconSize.Std16));
                    }
                }
            }
        }
    }
    
    /**
     * Processes all Commands of type DB_CMD_TYPE.
     * @param cmdAction the command to be processed
     */
    protected void processDatabaseCommands(final CommandAction cmdAction)
    {
        if (cmdAction.getData() instanceof InfoRequest)
        {
            // NOTE: An Update message is sent right after an Insert.
            // So we can ignore the Insert
            System.out.println(cmdAction);
            if (cmdAction.isAction(UPDATE_CMD_ACT))
            {
                // Create Specify Application
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        CommandDispatcher.dispatch(new CommandAction(INTERACTIONS, CREATE_MAILMSG, SubPaneMgr.getInstance().getCurrentSubPane()));
                    }
                });
                
            } else if (cmdAction.isAction(INSERT_CMD_ACT))
            {
                InfoRequest infoRequest = (InfoRequest)cmdAction.getData();
                createNavBtn(infoRequestNavBox, INFOREQUEST_FLAVOR, infoRequest, DBTableIdMgr.getInstance().getInfoById(InfoRequest.getClassTableId()));
            }
        }
    }
    
    /**
     * Loads a InfoRequest into a form
     * @param cmdAction the command action containing the InfoRequest
     */
    private void showInfoReqForm(final CommandActionForDB cmdAction, 
                                 final RecordSetIFace irRS)
    {
        // Launch Info Req Form
        DataEntryTask dataEntryTask = (DataEntryTask)TaskMgr.getTask(DataEntryTask.DATA_ENTRY);
        if (dataEntryTask != null)
        {
            InfoRequest              infoReq   = null;
            DBTableInfo              tableInfo = DBTableIdMgr.getInstance().getInfoById(InfoRequest.getClassTableId());
            DataProviderSessionIFace session   = DataProviderFactory.getInstance().createSession();
            try
            {
                infoReq = session.get(InfoRequest.class, cmdAction != null ? cmdAction.getId() : irRS.getItems().iterator().next().getRecordId());
                
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsTask.class, ex);
                ex.printStackTrace();
                log.error(ex);
            } finally
            {
                session.close();
            }
            
            if (infoReq != null)
            {
                dataEntryTask.openView(this, null, tableInfo.getDefaultFormName(), "edit", infoReq, true);
            }
        }
    }
    
    /**
     * Asks if they want to delete and then deletes it
     * @param cmdActionDB the delete command
     */
    private void deleteInfoRequest(final CommandActionForDB cmdActionDB)
    {
        NavBoxButton nb = (NavBoxButton)cmdActionDB.getSrcObj();
        int option = JOptionPane.showOptionDialog(UIRegistry.getMostRecentWindow(), 
                String.format(UIRegistry.getResourceString("InteractionsTask.CONFIRM_DELETE_IR"), nb.getName()),
                UIRegistry.getResourceString("InteractionsTask.CONFIRM_DELETE_TITLE_IR"), 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, JOptionPane.NO_OPTION); // I18N
        
        if (option == JOptionPane.YES_OPTION)
        {
            InfoRequest infoRequest = deleteInfoRequest(cmdActionDB.getId());
            deleteInfoRequestFromUI(null, infoRequest);
        }
    }
    
    /**
     * Processes all Commands of type INTERACTIONS.
     * @param cmdAction the command to be processed
     */
    protected void processInteractionsCommands(final CommandAction cmdAction)
    {
        boolean isNewLoan        = cmdAction.isAction(NEW_LOAN);
        boolean isNewLoanNoPreps = cmdAction.isAction(LN_NO_PRP);
        boolean isNewGift        = cmdAction.isAction(NEW_GIFT);
        boolean isInfoReq        = cmdAction.isAction(INFO_REQ_MESSAGE);

        boolean isOKToAdd;
        if (AppContextMgr.isSecurityOn())
        {
            PermissionSettings loanPerms = DBTableIdMgr.getInstance().getInfoById(Loan.getClassTableId()).getPermissions();
            PermissionSettings giftPerms = DBTableIdMgr.getInstance().getInfoById(Gift.getClassTableId()).getPermissions();
            PermissionSettings irPerms   = DBTableIdMgr.getInstance().getInfoById(InfoRequest.getClassTableId()).getPermissions();
            
            isOKToAdd = ((isNewLoan || isNewLoanNoPreps) && (loanPerms == null || loanPerms.canAdd())) ||
                        (isNewGift && (giftPerms == null || giftPerms.canAdd())) ||
                        (isInfoReq && (irPerms == null || irPerms.canAdd()));
        } else
        {
            isOKToAdd = isNewLoan || isNewGift || isInfoReq || isNewLoanNoPreps;
        }
        
        UsageTracker.incrUsageCount("IN."+cmdAction.getType());

        if (cmdAction.isAction(CREATE_MAILMSG))
        {
            createAndSendEMail();
            
        } else if (cmdAction.isAction(PRINT_INVOICE))
        {
            if (cmdAction.getData() instanceof RecordSetIFace)
            {
                printInvoice(null, cmdAction.getData());
                
            } if (cmdAction.getData() instanceof CommandAction)
            {
            	RecordSetIFace recordSet = RecordSetTask.askForRecordSet(this.printableInvoiceTblIds, null, true);
                if (recordSet != null)
                {
                	printInvoice(cmdAction.getPropertyAsString("file"), recordSet);
                }
            }
            
        } else if (isNewLoan || isNewGift || isNewLoanNoPreps)
        {
            if (cmdAction.getData() == cmdAction)
            {
                if (isOKToAdd)
                {
                    // We get here when a user clicks on a Loan NB action 
                    if (isNewLoan)
                    {
                        loanProcessor.createOrAdd();
                        
                    } else if (isNewLoanNoPreps)
                    {
                        createLoanNoPreps(null);
                        
                    } else
                    {
                        giftProcessor.createOrAdd();
                    }
                }
                
            } else if (cmdAction.getData() instanceof RecordSetIFace)
            {
                RecordSetIFace rs = (RecordSetIFace)cmdAction.getData();
                if (rs.getDbTableId() == colObjTableId || rs.getDbTableId() == infoRequestTableId)
                {
                    if (isOKToAdd)
                    {
                        if (isNewLoan)
                        {    
                            loanProcessor.createOrAdd(rs);
                        } else
                        {
                            giftProcessor.createOrAdd(rs);
                        }
                    }
                } else
                {
                    log.error("Dropped wrong table type."); // this shouldn't happen
                }
                
            } else if (cmdAction.getData() instanceof Viewable)
            {
                if (isNewLoan)
                {    
                    loanProcessor.createOrAdd((Viewable)cmdAction.getData());
                    
                } else if (isNewLoanNoPreps)
                {
                    createLoanNoPreps((Viewable)cmdAction.getData());
                    
                } else
                {
                    giftProcessor.createOrAdd((Viewable)cmdAction.getData());
                }
                
            } else if (cmdAction.getData() instanceof InfoRequest)
            {
                if (isOKToAdd)
                {
                    if (isNewLoan)
                    {
                        loanProcessor.createFromInfoRequest((InfoRequest)cmdAction.getData());
                    } else
                    {
                        giftProcessor.createFromInfoRequest((InfoRequest)cmdAction.getData());
                    }
                }
                
            }  else if (cmdAction.getData() instanceof CommandActionForDB)
            {
                if (isOKToAdd)
                {
                    CommandActionForDB cmdActionDB = (CommandActionForDB)cmdAction.getData();
                    RecordSetIFace     rs          = RecordSetFactory.getInstance().createRecordSet("", cmdActionDB.getTableId(), RecordSet.GLOBAL);
                    rs.addItem(cmdActionDB.getId());
                    
                    if (isNewLoan)
                    {
                        loanProcessor.createOrAdd(rs);
                    } else
                    {
                        giftProcessor.createOrAdd(rs);
                    }
                }
            }
            
        } else if (cmdAction.isAction(ADD_TO_LOAN))
        {
            loanProcessor.createOrAdd((Loan)cmdAction.getData());
            
        } else if (cmdAction.isAction(ADD_TO_GIFT))
        {
            giftProcessor.createOrAdd((Gift)cmdAction.getData());
            
        } else if (cmdAction.isAction(INFO_REQ_MESSAGE))
        {
            if (cmdAction.getData() == cmdAction)
            {
                // We get here when a user clicks on a InfoRequest NB action 
                createInfoRequest(null);
                
            } else if (cmdAction.getData() instanceof RecordSetIFace)
            {
                // We get here when a RecordSet is dropped on an InfoRequest
                Object data = cmdAction.getData();
                if (data instanceof RecordSetIFace)
                {
                    RecordSetIFace rs = (RecordSetIFace)data;
                    if (rs.getDbTableId() == infoRequestTableId)
                    {
                        showInfoReqForm(null, rs);
                        
                    } else if (rs.getDbTableId() == CollectionObject.getClassTableId())
                    {
                        createInfoRequest((RecordSetIFace)data);
                    }
                }
            } else if (cmdAction.getData() instanceof CommandActionForDB)
            {
                // We get here when a InfoRequest is dropped on an InfoRequest NB action
                showInfoReqForm((CommandActionForDB)cmdAction.getData(), null);
            }
            
        } else if (cmdAction.isAction("ReturnLoan"))
        {
            returnLoan();
            
        } else if (cmdAction.isAction(DELETE_CMD_ACT))
        {
            if (cmdAction instanceof CommandActionForDB)
            {
                deleteInfoRequest((CommandActionForDB)cmdAction);
            }
        } else if (cmdAction.isAction(OPEN_NEW_VIEW) || cmdAction.isAction(DataEntryTask.EDIT_DATA))
        {
            try
            {
                final CommandAction cachedCmdAction = (CommandAction)cmdAction.clone();
                
                cmdAction.setType("Data_Entry");
                cmdAction.setProperty(NavBoxAction.ORGINATING_TASK, this);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        CommandDispatcher.dispatch(cmdAction);
                        cmdAction.set(cachedCmdAction);
                    }
                });
            } catch (CloneNotSupportedException ex)
            {
                //ignore
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#doCommand(edu.ku.brc.ui.CommandAction)
     */
    @Override
    public void doCommand(final CommandAction cmdAction)
    {
        super.doCommand(cmdAction);
        
        if (cmdAction.isConsumed())
        {
            return;
        }
        
        if (cmdAction.isType(DB_CMD_TYPE))
        {
            processDatabaseCommands(cmdAction);

        } else if (cmdAction.isType(DataEntryTask.DATA_ENTRY))
        {
            if (cmdAction.isAction("SaveBeforeSetData"))
            {
                checkToPrintLoan(cmdAction);
            }
            
        } else if (cmdAction.isType(INTERACTIONS))
        {
            processInteractionsCommands(cmdAction);
            
        } else if (cmdAction.isType(PreferencesDlg.PREFERENCES))
        {
            prefsChanged(cmdAction);
            
        } else if (cmdAction.isType(RecordSetTask.RECORD_SET))
        {
            processRecordSetCommands(cmdAction);
        }
    }
    
    
    
    /**
     * @return the entries
     */
    public Vector<InteractionEntry> getEntries()
    {
        return entries;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#canRequestContext()
     */
    @Override
    protected boolean canRequestContext()
    {
        return Uploader.checkUploadLock(this) == Uploader.NO_LOCK;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#getPermEditorPanel()
     */
    @Override
    public PermissionEditorIFace getPermEditorPanel()
    {
        BasicPermisionPanel editor = new BasicPermisionPanel();
        editor.setAssociatedTableIds(new int[] {52});
        return editor;
    }

    /**
     * @return the permissions array
     */
    @Override
    protected boolean[][] getPermsArray()
    {
        return new boolean[][] {{true, true, true, true},
                                {true, true, true, true},
                                {true, true, false, false},
                                {true, false, false, false}};
    }
    
    /**
     * @author timbo
     *
     * @code_status Alpha
     *
     *Stores info about reports.
     */
    private class InvoiceInfo extends Pair<SpAppResource, SpReport> implements Comparable<InvoiceInfo>
    {
        /**
         * @param appResource
         * @param report
         */
        public InvoiceInfo(final SpAppResource spAppResource, final SpReport spReport)
        {
            super(spAppResource, spReport);
        }
        
        /**
         * @return the appResource
         */
        public SpAppResource getSpAppResource()
        {
            return getFirst();
        }
        
        /**
         * @return the spReport
         */
        public SpReport getSpReport()
        {
            return getSecond();
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.util.Pair#toString()
         */
        @Override
        public String toString()
        {
            return getSpAppResource().getName();
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(InvoiceInfo o)
        {
            // TODO Auto-generated method stub
            return getSpAppResource().getName().compareTo(o.getSpAppResource().getName());
        }
        
        
    }
    

}
