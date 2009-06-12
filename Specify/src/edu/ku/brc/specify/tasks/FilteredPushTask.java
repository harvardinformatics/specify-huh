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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.NavBox;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.core.UsageTracker;
import edu.ku.brc.af.tasks.subpane.FilteredPushPane;
import edu.ku.brc.af.tasks.subpane.SimpleDescPane;
import edu.ku.brc.specify.tasks.DataEntryTask.DroppableFormRecordSetAccepter;
import edu.ku.brc.stats.StatsMgr;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.JTiledPanel;
import edu.ku.brc.ui.ToolBarDropDownBtn;
import edu.ku.brc.ui.UIRegistry;

public class FilteredPushTask extends BaseTask
{
    // Static Data Members
    private static final Logger log  = Logger.getLogger(FilteredPushTask.class);
    
    public static final String FILTEREDPUSH  = "FilteredPush";
    
    protected Element panelDOM;
    
    public FilteredPushTask()
    {
        super(FILTEREDPUSH, getResourceString(FILTEREDPUSH));
        CommandDispatcher.register(FILTEREDPUSH, this);
    }

    // from StatsTask
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
            for ( Iterator<?> iter = boxes.iterator(); iter.hasNext(); )
            {
                Element box = (Element) iter.next();
                NavBox navBox = new NavBox(UIRegistry.getResourceString(box.attributeValue("title"))); //$NON-NLS-1$

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
    
    /**
     * Looks up statName and creates the appropriate SubPane
     * @param statName the name of the stat to be displayed
     */
    private void createFpPane(final String fpName)
    {
        // Create stat pane return a non-null panel for charts and null for non-charts
        // Of coarse, it could pass back nul if a chart was missed named
        // but error would be shown inside the StatsMgr for that case
        JPanel panel = new JTiledPanel();
        if (panel != null)
        {
            SimpleDescPane pane = new SimpleDescPane(fpName, this, panel);
            addSubPaneToMgr(pane);
        }
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
