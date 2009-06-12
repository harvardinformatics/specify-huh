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

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.NavBox;
import edu.ku.brc.af.core.NavBoxButton;
import edu.ku.brc.af.core.NavBoxItemIFace;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.tasks.subpane.BaseSubPane;
import edu.ku.brc.af.tasks.subpane.FilteredPushPane;
import edu.ku.brc.af.tasks.subpane.SimpleDescPane;
import edu.ku.brc.services.filteredpush.FilteredPushMgr;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.ToolBarDropDownBtn;

public class FilteredPushTask extends BaseTask
{
    // Static Data Members
    private static final Logger log  = Logger.getLogger(FilteredPushTask.class);
    
    public static final String FILTEREDPUSH  = "FilteredPush";
    
    private Element panelDOM;
    private SubPaneIFace fpPane;
    private NavBoxButton fpConnectionBtn;
    
    // TODO: FP MMK this whole calss is an ugly mess
    private final String fpConnectLink = "fp_connect_link"; // see config/../fp_navpanel.xml
    private final String fpDisconnectLink = "fp_disconnect_link";
    private final String fpConnectedStatus = "fp_connected_status";
    private final String fpDisconnectedStatus = "fp_disconnected_status";
    private final String fpConnectErrorStatus = "fp_error_status";

    private final String fpConnectedIconName = "fp_on_mmk";
    private final String fpDisconnectedIconName = "fp_off_mmk";

    private final String fpConnectStr = getResourceString(fpConnectLink);
    private final String fpDisconnectStr = getResourceString(fpDisconnectLink);
    private final String fpIsOnStr = getResourceString(fpConnectedStatus);
    private final String fpIsOffStr = getResourceString(fpDisconnectedStatus);
    private final String fpConnectErrorStr = getResourceString(fpConnectErrorStatus);
    
    private NavBoxButton fpConnectBtn = (NavBoxButton) NavBox.createBtn(fpConnectStr, fpDisconnectedIconName, IconManager.STD_ICON_SIZE, new DisplayAction(fpConnectLink));
    private NavBoxButton fpDisconnectBtn = (NavBoxButton) NavBox.createBtn(fpDisconnectStr, fpConnectedIconName, IconManager.STD_ICON_SIZE, new DisplayAction(fpDisconnectLink));

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
            
            try
            {
                panelDOM = AppContextMgr.getInstance().getResourceAsDOM("FilteredPushNavPanel");   // contains a description of the NavBoxes //$NON-NLS-1$
                if (panelDOM == null)
                {
                    log.error("Couldn't load FilteredPushNavPanel"); //$NON-NLS-1$
                    return;
                }
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FilteredPushTask.class, ex);
                log.error("Couldn't load `FilteredPushNavPanel` " +ex); //$NON-NLS-1$
            }
    
            // Process the NavBox Panel and create all the commands
            // XXX This needs to be made generic so everyone can use it
            //
            List<?> boxes = panelDOM.selectNodes("/boxes/box"); //$NON-NLS-1$
            for ( Iterator<?> boxIter = boxes.iterator(); boxIter.hasNext(); )
            {
                Element box = (Element) boxIter.next();
                NavBox navBox = new NavBox(getResourceString(box.attributeValue("title"))); //$NON-NLS-1$

                List<?> items = box.selectNodes("item"); //$NON-NLS-1$
                for ( Iterator<?> itemIter = items.iterator(); itemIter.hasNext(); )
                {
                    Element item = (Element) itemIter.next();
                    String itemName  = item.attributeValue("name"); //$NON-NLS-1$
                    String itemTitle = item.attributeValue("title"); //$NON-NLS-1$
                    String itemIcon     = item.attributeValue("icon"); //$NON-NLS-1$
                    
                    ActionListener action = new DisplayAction(itemName);
                    NavBoxItemIFace btn = NavBox.createBtn(getResourceString(itemTitle), itemIcon, IconManager.STD_ICON_SIZE, action);

                    log.debug("FilteredPushTask.initialize() item title: " + itemTitle);
                    navBox.add(btn);

                    if (itemName.equals(fpConnectLink))
                    {
                        fpConnectionBtn = (NavBoxButton) btn;
                        updateConnectBtn();
                    }

                }
                navBoxes.add(navBox);
            }
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

        BaseSubPane newPane = null;
        if (fpName.equals(fpConnectLink))
        {
            boolean isFpOn = FilteredPushMgr.getInstance().isFpOn();
            String title = fpIsOnStr;
            if (!isFpOn)
            {
                boolean success = FilteredPushMgr.getInstance().connectToFilteredPush();
                if (!success)
                {
                    title = fpConnectErrorStr;
                }
                updateConnectBtn();
            }
            newPane = new SimpleDescPane(getResourceString(fpName), this, title);
        }
        else if (fpName.equals(fpDisconnectLink))
        {
            boolean isFpOn = FilteredPushMgr.getInstance().isFpOn();
            String title = fpIsOffStr;
            if (isFpOn)
            {
                FilteredPushMgr.getInstance().disconnectFromFilteredPush();
                updateConnectBtn();
            }
            newPane = new SimpleDescPane(getResourceString(fpName), this, title);
        }
        else
        {
            newPane = new SimpleDescPane(getResourceString(fpName), this, "Coming soon!");
        }

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
    
    private void updateConnectBtn()
    {
        boolean isFpOn = FilteredPushMgr.getInstance().isFpOn();
        log.debug("updateConnectBtn: " + isFpOn);

        Container jComponent = fpConnectionBtn.getUIComponent().getParent();
        
        jComponent.remove(fpConnectionBtn);
        if (isFpOn)
        {
            fpConnectionBtn = fpDisconnectBtn;
        }
        else
        {
            fpConnectionBtn = fpConnectBtn;
        }
        jComponent.add(fpConnectionBtn);
        jComponent.repaint();
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
