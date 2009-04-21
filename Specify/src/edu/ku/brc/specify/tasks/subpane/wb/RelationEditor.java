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
package edu.ku.brc.specify.tasks.subpane.wb;

import static edu.ku.brc.ui.UIHelper.createIconBtn;
import static edu.ku.brc.ui.UIHelper.createLabel;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.specify.datamodel.Workbench;
import edu.ku.brc.specify.datamodel.WorkbenchTemplateMappingItem;
import edu.ku.brc.specify.rstools.WorkbenchRelation;
import edu.ku.brc.specify.tasks.WorkbenchTask;
import edu.ku.brc.specify.ui.HelpMgr;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.DefaultModifiableListModel;
import edu.ku.brc.ui.IconManager;

/**
 * This panel is enables a user to associate one workbench with another based on matching values
 * one column of each. (This might look like TemplateEditor, that's what I started with.)
 * 
 * @author maureen
 *
 * @code_status Alpha
 *
 * Created Date: Nov 19, 2008
 *
 */
public class RelationEditor extends CustomDialog
{
    private static final Logger log = Logger.getLogger(RelationEditor.class);

    protected static ImageIcon     blankIcon = IconManager.getIcon("BlankIcon",  IconManager.STD_ICON_SIZE);
    protected static ImageIcon     checkMark = IconManager.getIcon("Checkmark",  IconManager.IconSize.Std16);
    protected static ImageIcon workbenchIcon = IconManager.getImage("DataSet16", IconManager.STD_ICON_SIZE);
    
    protected WorkbenchInfo                                       workbenchInfo = null;
    protected List<WorkbenchInfo>                        otherWorkbenchInfoList = null;
    protected HashMap<WorkbenchInfo, WorkbenchInfoRelation>  workbenchRelations = null;
    
    protected DefaultModifiableListModel<WorkbenchFieldInfo>        keyFieldListModel;
    protected DefaultModifiableListModel<WorkbenchFieldInfo> foreignKeyFieldListModel;
    protected DefaultModifiableListModel<WorkbenchInfo>        otherDatasetsListModel; 

    protected JButton           mapToBtn;
    protected JButton           unmapBtn;
    protected JList        keyFieldJList;
    protected JList foreignKeyFieldJList;
    protected JList   otherDatasetsJList;

    protected boolean doingFill  = false;
    protected boolean isEditMode;
    protected boolean isReadOnly = false;
        
    public RelationEditor(final Frame frame, final String title, final Workbench workbench, final List<Workbench> list)
    {
        super(frame, title, true, OKCANCELAPPLYHELP, null);
        
        // henceforth the Apply button is the Skip button. TODO: localization
        setApplyLabel("Skip");
        
        this.isEditMode = workbench != null;
        
        this.helpContext = "WorkbenchEditMapping"; // TODO: change this, add help
                
        this.otherWorkbenchInfoList = new ArrayList<WorkbenchInfo>();
        for (Workbench otherWorkbench : list)
        {
            WorkbenchInfo otherWorkbenchInfo = getWorkbenchInfo(otherWorkbench);
            if (otherWorkbench != workbench)
            {
                this.otherWorkbenchInfoList.add(otherWorkbenchInfo);
            }
            else {
                this.workbenchInfo = otherWorkbenchInfo;
            }
        }

        this.workbenchRelations = new HashMap<WorkbenchInfo, WorkbenchInfoRelation>();
        
        createUI();
    }
        
    private WorkbenchInfo getWorkbenchInfo(Workbench workbench) {

        Vector<WorkbenchTemplateMappingItem> wbtmis = new Vector<WorkbenchTemplateMappingItem>();           
        for (WorkbenchTemplateMappingItem wbtmi : workbench.getTemplateMappings().values())
        {
            wbtmis.add(wbtmi);
        }
        Collections.sort(wbtmis);
        
        Vector<TableListItemIFace> workbenchFieldInfoList = new Vector<TableListItemIFace>();
        for (WorkbenchTemplateMappingItem wbtmi : wbtmis)
        {            
            workbenchFieldInfoList.add(new WorkbenchFieldInfo(wbtmi, getIcon(wbtmi)));
        }
        
        WorkbenchInfo workbenchInfo = new WorkbenchInfo(workbench, RelationEditor.workbenchIcon);
        workbenchInfo.setFieldItems(workbenchFieldInfoList);
        
        return workbenchInfo;
    }
    
    private ImageIcon getIcon(WorkbenchTemplateMappingItem wbtmi) {
        Integer srcTableId = wbtmi.getSrcTableId();
        String srcTableShortClassName = WorkbenchTask.getDatabaseSchema().getInfoById(srcTableId).getShortClassName();
        return IconManager.getIcon(srcTableShortClassName.toLowerCase(), IconManager.STD_ICON_SIZE);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CustomDialog#createUI()
     */
    @Override
    public void createUI()
    {
        super.createUI();
        
        // Main Pane Layout
        String columnFormat = "f:max(200px;p):g, 2px, f:max(200px;p):g, 5px, p:g, 5px, f:max(200px;p):g";
        String    rowFormat = "p, 2px, f:max(350px;p):g";

        JPanel mainLayoutPanel = new JPanel();
        mainLayoutPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        this.contentPanel = mainLayoutPanel;
        this.mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        PanelBuilder   builder = new PanelBuilder(new FormLayout(columnFormat, rowFormat), mainLayoutPanel);
        CellConstraints     cc = new CellConstraints();
        
        // Labels for the list panels
        String    otherDatasetsLabel = "Other Data Sets";   // TODO: un-hardcode this: getResourceString("WB_DATASETS")?
        String foreignKeyFieldsLabel = "Related Data Set";  // TODO: un-hardcode this
        String        keyFieldsLabel = "Exported Data Set"; // TODO: un-hardcode this
        
        builder.add(createLabel(otherDatasetsLabel,    SwingConstants.CENTER), cc.xy(1, 1));
        builder.add(createLabel(foreignKeyFieldsLabel, SwingConstants.CENTER), cc.xy(3, 1));
        builder.add(createLabel(keyFieldsLabel,        SwingConstants.CENTER), cc.xy(7, 1));
        
        // List of fields of the workbench to which additional workbenches may be related
        JScrollPane datasetScrollPane = null;

        this.keyFieldListModel = new DefaultModifiableListModel<WorkbenchFieldInfo>();
        
        for (TableListItemIFace fi : this.workbenchInfo.getFieldItems())
        {
            keyFieldListModel.add((WorkbenchFieldInfo) fi);
        }

        this.keyFieldJList = new JList(keyFieldListModel);
        
        keyFieldJList.setCellRenderer(new TableInfoListRenderer(IconManager.STD_ICON_SIZE));
        keyFieldJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyFieldJList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    updateMapButtonsState();
                }
            }
        });
        
        datasetScrollPane = new JScrollPane(keyFieldJList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // List of fields of a (potentially) related workbench
        JScrollPane relatedDatasetScrollPane = null;

        this.foreignKeyFieldListModel = new DefaultModifiableListModel<WorkbenchFieldInfo>();

        this.foreignKeyFieldJList = new JList(foreignKeyFieldListModel);
        
        foreignKeyFieldJList.setCellRenderer(new TableInfoListRenderer(IconManager.STD_ICON_SIZE));
        foreignKeyFieldJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        foreignKeyFieldJList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    updateMapButtonsState();
                }
            }
        });
        
        relatedDatasetScrollPane = new JScrollPane(foreignKeyFieldJList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Panel containing buttons for adding or removing a relation
        this.mapToBtn = createIconBtn("Map", "WB_ADD_MAPPING_ITEM", new ActionListener()  // TODO: "Link" instead of "Map"?
        {
            public void actionPerformed(ActionEvent ae)
            {
                WorkbenchInfo relatedWorkbenchInfo =
                    (WorkbenchInfo) otherDatasetsJList.getSelectedValue();

                WorkbenchFieldInfo keyFieldInfo =
                    (WorkbenchFieldInfo) keyFieldJList.getSelectedValue();

                WorkbenchFieldInfo foreignKeyFieldInfo =
                    (WorkbenchFieldInfo) foreignKeyFieldJList.getSelectedValue();
                     
                relate(relatedWorkbenchInfo, keyFieldInfo, foreignKeyFieldInfo);
                updateMapButtonsState();
     
                okBtn.setEnabled(true);
                
                repaint();
            }
        });
        
        this.unmapBtn = createIconBtn("Unmap", "WB_REMOVE_MAPPING_ITEM", new ActionListener() // TODO: "Unlink" instead of "Unmap"?
        {
            public void actionPerformed(ActionEvent ae)
            {
                unrelate((WorkbenchInfo) otherDatasetsJList.getSelectedValue());
                updateMapButtonsState();

                okBtn.setEnabled(true);
                
                repaint();
            }
        });
        
        JPanel mapUnmapPane = getMapUnmapPane(mapToBtn, unmapBtn);
        
        // List of other workbenches available for relating
        this.otherDatasetsListModel = new DefaultModifiableListModel<WorkbenchInfo>();

        for (WorkbenchInfo wi : this.otherWorkbenchInfoList)
        {
            otherDatasetsListModel.add(wi);
        }

        this.otherDatasetsJList = new JList(otherDatasetsListModel);

        otherDatasetsJList.setCellRenderer(new TableInfoListRenderer(IconManager.STD_ICON_SIZE));
        otherDatasetsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        otherDatasetsJList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    WorkbenchInfo otherDatasetInfo =
                        (WorkbenchInfo) otherDatasetsJList.getSelectedValue();

                    selectDataset(otherDatasetInfo);
                    updateMapButtonsState();
                    repaint();
                }
            }
        });
        
        JScrollPane otherDatasetsScrollPane = new JScrollPane(otherDatasetsJList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        builder.add(otherDatasetsScrollPane,  cc.xy(1, 3));
        builder.add(relatedDatasetScrollPane, cc.xy(3, 3));
        builder.add(mapUnmapPane,             cc.xy(5, 3));
        builder.add(datasetScrollPane,        cc.xy(7, 3));
        
        // OK, Help buttons additional configuration
        okBtn.setEnabled(false);
        HelpMgr.registerComponent(this.helpBtn, this.helpContext);
     
        // Prepare for display
        pack();
        
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run()
            {
                cancelBtn.requestFocus();
                foreignKeyFieldListModel.clear();
                updateMapButtonsState();                
            }
        });
    }

    /**
     * overriding this; the Apply button is now the Skip button
     */
    protected void applyButtonPressed()
    {
        // remove all mappings
        this.workbenchRelations.clear();

        isCancelled = false;
        btnPressed  = APPLY_BTN;
        setVisible(false);
    }
    
    private JPanel getMapUnmapPane(JButton mapTo, JButton unMap) {
        CellConstraints cc = new CellConstraints();
        
        PanelBuilder middlePanel = new PanelBuilder(new FormLayout("c:p:g", "p, 2px, p"));
        middlePanel.add(mapTo, cc.xy(1, 1));
        middlePanel.add(unMap, cc.xy(1, 3));
        
        PanelBuilder outerMiddlePanel = new PanelBuilder(new FormLayout("c:p:g", "f:p:g, p, f:p:g"));
        outerMiddlePanel.add(middlePanel.getPanel(), cc.xy(1, 2));
        
        return outerMiddlePanel.getPanel();
    }
 
    /**
     * Update the MappingTo and unmapping buttons.
     */
    protected void updateMapButtonsState()
    {
        WorkbenchFieldInfo keyField =
            (WorkbenchFieldInfo) this.keyFieldJList.getSelectedValue();

        WorkbenchFieldInfo foreignKeyField =
            (WorkbenchFieldInfo) this.foreignKeyFieldJList.getSelectedValue();

        if (keyField != null && foreignKeyField != null)
        {
            if (keyField.isChecked() && foreignKeyField.isChecked())
            {
                mapToBtn.setEnabled(false);
                unmapBtn.setEnabled(true);
            }
            else
            {
                mapToBtn.setEnabled(true);
                unmapBtn.setEnabled(false);
            }
        }
        else {
            mapToBtn.setEnabled(false);
            unmapBtn.setEnabled(false);
        }
    }
    
    protected void selectDataset(WorkbenchInfo selectedDatasetInfo)
    {
        // uncheck any checked buttons in the key field list
        for (TableListItemIFace fieldInfo : workbenchInfo.getFieldItems())
        {
            fieldInfo.setChecked(false);
        }
        
        // if there's a mapping for the newly selected workbench, check the key field
        WorkbenchInfoRelation currentRelation = workbenchRelations.get(selectedDatasetInfo);
        if (currentRelation != null)
        {
            currentRelation.getKeyFieldInfo().setChecked(true);
        }
        
        // remove any selections in the key field list
        keyFieldJList.clearSelection();
        
        // re-fill the foreign key field list
        foreignKeyFieldListModel.clear();
        for (TableListItemIFace fieldInfo : selectedDatasetInfo.getFieldItems())
        {
            foreignKeyFieldListModel.add((WorkbenchFieldInfo) fieldInfo);
        }

        // remove any selections in the foreign key field list
        foreignKeyFieldJList.clearSelection();
    }
    
    protected void relate(WorkbenchInfo relatedWorkbenchInfo, WorkbenchFieldInfo keyFieldInfo, WorkbenchFieldInfo foreignKeyFieldInfo)
    {
        unrelate(relatedWorkbenchInfo);
        
        this.workbenchRelations.put(relatedWorkbenchInfo,
                new WorkbenchInfoRelation(relatedWorkbenchInfo, keyFieldInfo, foreignKeyFieldInfo));

        keyFieldInfo.setChecked(true);
        foreignKeyFieldInfo.setChecked(true);
        relatedWorkbenchInfo.setChecked(true);
    }

    private void unrelate(WorkbenchInfo relatedWorkbenchInfo) {
        if (this.workbenchRelations.containsKey(relatedWorkbenchInfo))
        {
            WorkbenchInfoRelation relation = this.workbenchRelations.get(relatedWorkbenchInfo);

            this.workbenchRelations.remove(relatedWorkbenchInfo);

            relation.getRelatedWorkbenchInfo().setChecked(false);
            relation.getKeyFieldInfo().setChecked(false);
            relation.getForeignKeyFieldInfo().setChecked(false);
        }
    }

    public Set<WorkbenchRelation> getRelations() {
        Set<WorkbenchRelation> result = new HashSet<WorkbenchRelation>();

        for (WorkbenchInfoRelation infoRelation : this.workbenchRelations.values())
        {
            Workbench        workbench = this.workbenchInfo.getWorkbench();
            Workbench relatedWorkbench = infoRelation.getRelatedWorkbenchInfo().getWorkbench();
            
            WorkbenchTemplateMappingItem        keyColumn = infoRelation.getKeyFieldInfo().getMappingItem();
            WorkbenchTemplateMappingItem foreignKeyColumn = infoRelation.getForeignKeyFieldInfo().getMappingItem();

            WorkbenchRelation relation =
                new WorkbenchRelation(workbench, keyColumn, relatedWorkbench, foreignKeyColumn);

            result.add(relation);
        }
        return result;
    }
 
    class WorkbenchFieldInfo implements TableListItemIFace, Comparable<WorkbenchFieldInfo> {
        
        WorkbenchTemplateMappingItem wbtmi;
        ImageIcon                     icon;
        boolean                    isInUse;

        public WorkbenchFieldInfo(WorkbenchTemplateMappingItem wbtmi, ImageIcon icon)
        {
            this.wbtmi = wbtmi;
            this.icon  = icon;
        }
        
        public WorkbenchTemplateMappingItem getMappingItem() {
            return this.wbtmi;
        }
 
        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#getIcon()
         */
        public ImageIcon getIcon()
        {
            return this.icon;
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#getText()
         */
        public String getText()
        {
            return getMappingItem().getCaption();
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#isChecked()
         */
        public boolean isChecked()
        {
            return this.isInUse;
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#isExpandable()
         */
        public boolean isExpandable()
        {
            return false;
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#isExpanded()
         */
        public boolean isExpanded()
        {
            return false;
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#setChecked(boolean)
         */
        public void setChecked(boolean checked)
        {
           this.isInUse = checked;
        }

        /* (non-Javadoc)
         * @see edu.ku.brc.specify.tasks.subpane.wb.TableListItemIFace#setExpanded(boolean)
         */
        public void setExpanded(boolean expand)
        {
            // no-op
        }

        /**
         * @param tableInfo the tableInfo to set.
         */
        public void setTableInfo(final DBTableInfo tableInfo)
        {
            // no-op
        }
        
        public int compareTo(WorkbenchFieldInfo obj)
        {
            return getMappingItem().getViewOrder().compareTo(obj.getMappingItem().getViewOrder());
        }
    }

    class WorkbenchInfo implements TableListItemIFace, Comparable<WorkbenchInfo>
    {
        Workbench                   workbench;
        Vector<TableListItemIFace> fieldItems;
        boolean                     isChecked;
        ImageIcon                        icon;

        WorkbenchInfo(Workbench workbench, ImageIcon icon)
        {
            this.workbench = workbench;
            this.icon      = icon;
        }

        public int compareTo(WorkbenchInfo obj)    { return getText().compareTo(obj.getText()); }

        public String getText()                    { return getWorkbench().getName(); }

        public boolean isChecked()                 { return isChecked; }

        public boolean isExpandable()              { return false; }

        public boolean isExpanded()                { return false; }

        public void setChecked(boolean checked)    { this.isChecked = checked; }

        public void setExpanded(boolean expand)    { ; } // no-op

        public ImageIcon getIcon()                 { return icon; }
        
        void setIcon(ImageIcon icon)               { this.icon = icon; }

        Workbench getWorkbench()                   { return workbench; }
        
        Vector<TableListItemIFace> getFieldItems() { return fieldItems; }

        void setFieldItems(Vector<TableListItemIFace> fieldItems) { this.fieldItems = fieldItems; }
    }
    
    class WorkbenchInfoRelation {
        WorkbenchInfo     relatedWorkbenchInfo;
        WorkbenchFieldInfo        keyFieldInfo;
        WorkbenchFieldInfo foreignKeyFieldInfo;
        
        WorkbenchInfoRelation(WorkbenchInfo workbenchInfo, WorkbenchFieldInfo keyFieldInfo, WorkbenchFieldInfo foreignKeyFieldInfo)
        {
            this.relatedWorkbenchInfo = workbenchInfo;
            this.keyFieldInfo         = keyFieldInfo;
            this.foreignKeyFieldInfo  = foreignKeyFieldInfo;
        }

        WorkbenchInfo getRelatedWorkbenchInfo()     { return relatedWorkbenchInfo; }
        WorkbenchFieldInfo getKeyFieldInfo()        { return keyFieldInfo;         }
        WorkbenchFieldInfo getForeignKeyFieldInfo() { return foreignKeyFieldInfo;  }
    }
}
