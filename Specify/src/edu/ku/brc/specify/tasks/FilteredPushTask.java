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
package edu.ku.brc.specify.tasks;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.ContextMgr;
import edu.ku.brc.af.core.DroppableNavBox;
import edu.ku.brc.af.core.NavBox;
import edu.ku.brc.af.core.NavBoxButton;
import edu.ku.brc.af.core.NavBoxIFace;
import edu.ku.brc.af.core.NavBoxItemIFace;
import edu.ku.brc.af.core.NavBoxMgr;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.tasks.subpane.BaseSubPane;
import edu.ku.brc.af.tasks.subpane.FilteredPushPane;
import edu.ku.brc.af.tasks.subpane.SimpleDescPane;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.services.filteredpush.FilteredPushMgr;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.specify.datamodel.SpQuery;
import edu.ku.brc.specify.tasks.QueryTask.EditQueryWorker;
import edu.ku.brc.specify.tools.schemalocale.SchemaLocalizerDlg;
import edu.ku.brc.specify.ui.treetables.TreeDefinitionEditor;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.DataFlavorTableExt;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.RolloverCommand;
import edu.ku.brc.ui.ToolBarDropDownBtn;
import edu.ku.brc.ui.dnd.FpPublish;
import edu.ku.brc.ui.dnd.Trash;

public class FilteredPushTask extends BaseTask
{
    // Static Data Members
    private static final Logger log  = Logger.getLogger(FilteredPushTask.class);
    
    public static final String FILTEREDPUSH  = "FilteredPush";
    public static final String NEW_SUBSCRIP  = "NewSubscription";
    public static final String DEL_SUBSCRIP  = "DeleteSubscription";
    private SubPaneIFace fpPane;
    
    private String subscripsBoxTitle = getResourceString("fp_subscrips_title");
    protected NavBox subscripsNavBox;

    // When requestedContext(Taskable) is called on ContextMgr, it calls NavBoxMgr.register(Taskable).
    // In that method, this class's getNavBoxes() method is called, which is where this item gets
    // populated.
    protected Vector<NavBoxIFace>  extendedNavBoxes = new Vector<NavBoxIFace>();
    
    public FilteredPushTask()
    {
        super(FILTEREDPUSH, getResourceString(FILTEREDPUSH));
        CommandDispatcher.register(FILTEREDPUSH, this);
    }

    // Initialize the left nav panel
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.Taskable#initialize()
     */
    public void initialize() // see config/backsotp/fp_navpanel.xml, src/resources_en.properties, config/icons.xml
    {
        if (!isInitialized)
        {
            super.initialize(); // sets isInitialized to false
            
            extendedNavBoxes.clear();
            
            subscripsNavBox = new DroppableNavBox(subscripsBoxTitle, QueryTask.QUERY_FLAVOR, FILTEREDPUSH, NEW_SUBSCRIP);

            String newSubscripTitle = getResourceString("fp_new_subscrip_title");
            String newSubscripIcon = "EMail";
            ActionListener defaultAction = new DisplayAction(subscripsBoxTitle);

            NavBoxItemIFace btn = NavBox.createBtn(newSubscripTitle, newSubscripIcon, IconManager.STD_ICON_SIZE, defaultAction);

            subscripsNavBox.add(btn);

            // TODO: FP MMK implement.  these should be draggable to trash

            NavBoxItemIFace btn1 = NavBox.createBtn("Rubus", "Taxon", IconManager.STD_ICON_SIZE, defaultAction);
            subscripsNavBox.add(btn1);

            NavBoxItemIFace btn2 = NavBox.createBtn("Macklin", "Collector", IconManager.STD_ICON_SIZE, defaultAction);
            subscripsNavBox.add(btn2);

            NavBoxItemIFace btn3 = NavBox.createBtn("HUH", "Institution", IconManager.STD_ICON_SIZE, defaultAction);
            subscripsNavBox.add(btn3);


            navBoxes.add(subscripsNavBox);

        }
        
        // TODO: FP MMK just tucking this away for safekeeping
        RecordSetTask rsTask = (RecordSetTask)ContextMgr.getTaskByClass(RecordSetTask.class);
        List<NavBoxIFace> nbs = rsTask.getNavBoxes();
        if (nbs != null)
        {
            extendedNavBoxes.addAll(nbs);
        }
        
        isShowDefault = true;
    }
    
    /*
     *  (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getToolBarItems()
     */
    @Override
    public List<ToolBarItemDesc> getToolBarItems()
    {
        toolbarItems = new Vector<ToolBarItemDesc>();

        String label     = getResourceString(FILTEREDPUSH);
        String hint     = getResourceString("filteredpush_hint");
        ToolBarDropDownBtn btn = createToolbarButton(label, iconName, hint);

        toolbarItems.add(new ToolBarItemDesc(btn));

        return toolbarItems;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.BaseTask#getStarterPane()
     */
    @Override
    public SubPaneIFace getStarterPane()
    {
                
        if (starterPane == null) // TODO: FP MMK do the data-set drop thing
        {
            starterPane = new FilteredPushPane(title, this, "FilteredPushPanel");
        }
        
        return starterPane;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#isSingletonPane()
     */
    @Override
    public boolean isSingletonPane()  // TODO: FP MMK clicking the task button always creates a new pane
    {
        return true;
    }
    
    /**
     * Looks up statName and creates the appropriate SubPane
     * @param statName the name of the stat to be displayed
     */
    private void createFpPane(final String fpName)
    {

        BaseSubPane newPane = new SimpleDescPane(getResourceString(fpName), this, "Coming soon!");

        if (starterPane != null)
        {
            SubPaneMgr.getInstance().replacePane(starterPane, newPane);
            starterPane = null;
        }
        else if (fpPane != null)
        {
            SubPaneMgr.getInstance().replacePane(fpPane, newPane);
        }
        fpPane = newPane;
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

        // TODO: FP MMK if the query pane has not already been opened, clicking on one of these opens it
        // in a non-accessible pane.  maybe recordset nav box items know how to fix that.
        QueryTask qTask = (QueryTask)ContextMgr.getTaskByClass(QueryTask.class);
        List<NavBoxIFace> qnbs = qTask.getNavBoxes();
        if (qnbs != null)
        {
            String title = getResourceString("QUERIES"); // see QueryTask initialize()
            for (NavBoxIFace qnb : qnbs)
            {
                if (title.equals(qnb.getName())) extendedNavBoxes.add(qnb);
            }
        }
        
        RecordSetTask rsTask = (RecordSetTask)ContextMgr.getTaskByClass(RecordSetTask.class);
        List<NavBoxIFace> rsnbs = rsTask.getNavBoxes();
        if (rsnbs != null)
        {
            extendedNavBoxes.addAll(rsnbs);
        }
        return extendedNavBoxes;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.ui.CommandListener#doCommand(edu.ku.brc.specify.ui.CommandAction)
     */
    @Override
    public void doCommand(CommandAction cmdAction)
    {
        super.doCommand(cmdAction);
        
        if (cmdAction.isType(FILTEREDPUSH))
        {
            processFpCommands(cmdAction);
            
        }
    }
    
    private void processFpCommands(CommandAction cmdAction)
    {
        log.debug("processFpCommands");

        if (cmdAction.isAction(NEW_SUBSCRIP) && cmdAction.getData() instanceof RecordSetIFace)
        {
            log.debug("adding to nav box");
            addToNavBox((RecordSet) cmdAction.getData()); // TODO: FP MMK we should be adding a subscription, not a recordset.
        }
        else if (cmdAction.isAction(DEL_SUBSCRIP) & cmdAction.getData() instanceof RecordSetIFace)
        {
            log.debug("deleting from nav box");
            deleteQueryFromUI(null, (RecordSet)cmdAction.getData());
        }
    }

    /** FP MMK: took this from QueryTask
     * 
     * Adds a Query to the Left Pane NavBox (Refactor this with Workbench)
     * @param query the Query to be added
     * @return the nav box
     */
    protected NavBoxItemIFace addToNavBox(final RecordSet recordSet)
    {
        //boolean canDelete = AppContextMgr.isSecurityOn() ? getPermissions().canDelete() : true;
        boolean canDelete = true;
        final RolloverCommand roc = (RolloverCommand) makeDnDNavBtn(subscripsNavBox, recordSet.getName(),
                "Query", null,
                canDelete ? new CommandAction(FILTEREDPUSH, DEL_SUBSCRIP, recordSet) : null, 
                true, false); // TODO: sort
        //roc.setToolTip(getResourceString("QY_CLICK2EDIT"));
        roc.setData(recordSet);
        roc.addActionListener(new DisplayAction(subscripsBoxTitle));

        NavBoxItemIFace nbi = (NavBoxItemIFace)roc;
        
        DBTableInfo tblInfo = DBTableIdMgr.getInstance().getInfoById(recordSet.getDbTableId()); // was getTableId; didn't work.
        if (tblInfo != null)
        {
            ImageIcon rsIcon = tblInfo.getIcon(IconManager.STD_ICON_SIZE);
            if (rsIcon != null)
            {
                nbi.setIcon(rsIcon);
            }
        }
        
        //roc.addDragDataFlavor(new DataFlavorTableExt(QueryTask.class, QUERY, recordSet.getTableId()));
        if (canDelete)
        {
            roc.addDragDataFlavor(Trash.TRASH_FLAVOR);
        }
        
        // FP MMK
        //roc.addDragDataFlavor(FpPublish.FP_PUB_FLAVOR);

        //boolean fpPubOK = true;
        //CommandAction fpPubCmdAction = fpPubOK ? new CommandAction(QUERY, FP_PUB_RECORDSET, recordSet) : null;
        //roc.setFpPublishCommandAction(fpPubCmdAction);
        
        return nbi;
    }
    
    /** FP MMK: took from QueryTask
     * 
     * Delete the RecordSet from the UI, which really means remove the NavBoxItemIFace.
     * This method first checks to see if the boxItem is not null and uses that, if
     * it is null then it looks the box up by name and used that
     * @param boxItem the box item to be deleted
     * @param recordSets the record set that is "owned" by some UI object that needs to be deleted (used for secondary lookup
     */
    protected void deleteQueryFromUI(final NavBoxItemIFace boxItem, final RecordSet rs)
    {
        deleteDnDBtn(subscripsNavBox, boxItem != null ? boxItem : getBoxByTitle(subscripsNavBox, rs.getName()));
    }
    
    /**
    *
    * @author rods
    *
    */
   class DisplayAction implements ActionListener
   {
       private String   fpActionName;

       public DisplayAction(final String statName)
       {
           this.fpActionName = statName;
       }

       public void actionPerformed(ActionEvent e)
       {
           //UsageTracker.incrUsageCount("FP."+statName);
           createFpPane(fpActionName);
       }
   }
   
}
