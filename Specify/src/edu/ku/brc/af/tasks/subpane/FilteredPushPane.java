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
package edu.ku.brc.af.tasks.subpane;

import static edu.ku.brc.helpers.XMLHelper.getAttr;
import static edu.ku.brc.ui.UIHelper.createDuplicateJGoodiesDef;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.Taskable;
import edu.ku.brc.af.tasks.subpane.StatsPane.FadeBtn;
import edu.ku.brc.stats.BarChartPanel;
import edu.ku.brc.stats.StatsMgr;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

// took this from StatsPane
public class FilteredPushPane extends BaseSubPane
{
    private static final Logger log = Logger.getLogger(FilteredPushPane.class);
            
    protected String  resourceName       = null; // "FilteredPushPanel": see FilteredPushTask.getStarterPane
    protected Color   bgColor            = Color.WHITE;
    protected boolean useSeparatorTitles = false;
    protected FadeBtn updateBtn         = null;

    protected int     PREFERREDWIDTH     = 300;
    protected int     SPACING            = 35;
    
    protected JComponent upperDisplayComp = null;
    protected JPanel     centerPanel      = null;
    protected Vector<Component> comps     = new Vector<Component>();
    
    public FilteredPushPane(String name, Taskable task, String resourceName)
    {
        super(name, task);

        this.resourceName   = resourceName;
        
        init();
    }

    /**
     * Loads all the panels.
     */
    protected void init()
    {
        JComponent parentComp = this;
        for (Component c : comps)
        {
            parentComp.remove(c);
        }
        comps.clear();
        
        if (centerPanel != null)
        {
            remove(centerPanel);
        }
        
        Element rootElement = null;
        try
        {
            rootElement = AppContextMgr.getInstance().getResourceAsDOM(resourceName);
            if (rootElement == null)
            {
                throw new RuntimeException("Couldn't find resource ["+resourceName+"]"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            // count up rows and column
            String rowsDef = "c:p:g";
            String colsDef = "p";
            List<?> rows = rootElement.selectNodes("/panel/row"); //$NON-NLS-1$

            FormLayout      formLayout = new FormLayout(colsDef, rowsDef);
            PanelBuilder    builder    = new PanelBuilder(formLayout);
            CellConstraints cc         = new CellConstraints();

            if (rows.size() > 0)
            {
                Element rowElement = (Element) rows.get(0);

                int x = 1;
                List<?> boxes = rowElement.selectNodes("box"); //$NON-NLS-1$
                if (boxes.size() > 0)
                {
                    Element box = (Element) boxes.get(0);
                    String title = UIRegistry.getResourceString(box.attributeValue("title"));

                    if (isNotEmpty(title))
                    {
                        JLabel jLabel = new JLabel(title);

                        validate();
                        doLayout();
                        repaint();

                        if (jLabel != null)
                        {
                            comps.add(jLabel);

                            builder.add(jLabel, cc.xy(1, 1));
                        }
                    }
                }
            }
            setBackground(bgColor);

            JPanel fpPanel = builder.getPanel();
            fpPanel.setBackground(Color.WHITE);

            builder = new PanelBuilder(new FormLayout("C:P:G", "p"));
                        
            builder.add(fpPanel, cc.xy(1, 1));
            centerPanel = builder.getPanel();

            centerPanel.setBackground(Color.WHITE);
            
            //For Tiling
            if (isTiled())
            {
                centerPanel.setOpaque(false);
                setOpaque(false);
                fpPanel.setOpaque(false);
            }
            
            centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            add(centerPanel, BorderLayout.CENTER);
                       
            centerPanel.validate();
            validate();
            doLayout();

        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(StatsPane.class, ex);
            log.error(ex);
            ex.printStackTrace();
        }
    }
}
