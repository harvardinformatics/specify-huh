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
package edu.ku.brc.specify.tools.schemalocale;

import static edu.ku.brc.ui.UIHelper.createButton;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.betwixt.XMLIntrospector;
import org.apache.commons.betwixt.io.BeanWriter;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.ContextMgr;
import edu.ku.brc.af.core.SchemaI18NService;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.SpLocaleContainer;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.dnd.SimpleGlassPane;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Oct 3, 2007
 *
 */
public class SchemaToolsDlg extends CustomDialog
{
    protected JButton      editSchemaBtn      = createButton(getResourceString("SL_EDIT_SCHEMA"));
    protected JButton      removeLocaleBtn    = createButton(getResourceString("SL_REMOVE_SCHEMA_LOC"));
    protected JButton      exportSchemaLocBtn = createButton(getResourceString("SL_EXPORT_SCHEMA_LOC"));
    protected JButton      importSchemaLocBtn = createButton(getResourceString("SL_IMPORT_SCHEMA_LOC"));
    protected JList        localeList;
    protected Byte         schemaType;
    protected DBTableIdMgr tableMgr;

    /**
     * @param frame
     * @param schemaType
     * @param tableMgr
     * @throws HeadlessException
     */
    public SchemaToolsDlg(final Frame        frame, 
                          final Byte         schemaType,
                          final DBTableIdMgr tableMgr) throws HeadlessException
    {
        super(frame, getResourceString("SL_TOOLS_TITLE"), true, OKHELP, null);
        this.schemaType = schemaType;
        this.tableMgr   = tableMgr;
        
        helpContext = "SL_TOOLS_HELP_CONTEXT";
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CustomDialog#createUI()
     */
    @Override
    public void createUI()
    {
        setOkLabel(getResourceString("CLOSE"));
        
        super.createUI();
        

        Vector<DisplayLocale> localeDisplays = new Vector<DisplayLocale>();
        for (Locale locale : SchemaLocalizerDlg.getLocalesInUseInDB(schemaType))
        {
            localeDisplays.add(new DisplayLocale(locale));
        }
        
        localeList = new JList(localeDisplays);
        JScrollPane sp   = UIHelper.createScrollPane(localeList, true);

        CellConstraints cc = new CellConstraints();
        
        PanelBuilder builder   = new PanelBuilder(new FormLayout("p,2px,f:p:g", "p,2px,p,16px,p,4px,p,8px,p,10px"));
        builder.addSeparator(getResourceString("SL_LOCALES_IN_USE"), cc.xywh(1, 1, 3, 1));
        builder.add(sp, cc.xywh(1,3,3,1));
        
        builder.addSeparator(getResourceString("SL_TASKS"), cc.xywh(1, 5, 3, 1));
        builder.add(editSchemaBtn,      cc.xy(1,7));
        builder.add(removeLocaleBtn,    cc.xy(3,7));
        builder.add(exportSchemaLocBtn, cc.xy(1,9));
        builder.add(importSchemaLocBtn, cc.xy(3,9));
        
        builder.setDefaultDialogBorder();
        
        contentPanel = builder.getPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        enableBtns(false);
        
        localeList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                localeSelected();
            }
        });
        localeList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me)
            {
                if (me.getClickCount() == 2)
                {
                    editSchema();
                }
            }
        });
        
        editSchemaBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                editSchema();
            }
        });
        
        removeLocaleBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                removeSchemaLocale();
            }
        });
        
        exportSchemaLocBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                exportSchemaLocales();
            }
        });
        
        importSchemaLocBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                JOptionPane.showMessageDialog(UIRegistry.getTopWindow(), getResourceString("SL_NOT_IMPLEMENTED"));
            }
        });
        
        pack();
    }
    
    /**
     * @param enable
     */
    protected void enableBtns(final boolean enable)
    {
        
        editSchemaBtn.setEnabled(enable);
        removeLocaleBtn.setEnabled(localeList.getModel().getSize() > 1);
        exportSchemaLocBtn.setEnabled(enable);
    }

    /**
     * 
     */
    protected void localeSelected()
    {
        DisplayLocale dispLocale = (DisplayLocale)localeList.getSelectedValue();
        if (dispLocale != null)
        {
            enableBtns(true);
            
        } else
        {
            enableBtns(false);
        }
    }
    
    
    /**
     * 
     */
    protected void editSchema()
    {
        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
                okButtonPressed();
                
                DisplayLocale dispLocale = (DisplayLocale)localeList.getSelectedValue();
                if (dispLocale != null)
                {
                    Locale currLocale = SchemaI18NService.getCurrentLocale();
                    
                    SchemaI18NService.setCurrentLocale(dispLocale.getLocale());

                    SchemaLocalizerDlg dlg = new SchemaLocalizerDlg((Frame)UIRegistry.getTopWindow(), schemaType, tableMgr); // MUST BE MODAL!
                    dlg.setVisible(true);
                    SchemaI18NService.setCurrentLocale(currLocale);
                    
                    isCancelled = true; // We need to do this here so we don't get a StatsPane we don't want

                    //if (dlg.wasSaved())
                    //{
                        //UIRegistry.showLocalizedMsg("Specify.ABT_EXIT");
                        //CommandDispatcher.dispatch(new CommandAction(BaseTask.APP_CMD_TYPE, BaseTask.APP_REQ_EXIT));
                        
                    //} else
                    //{
                        ContextMgr.getTaskByName("Startup").requestContext();
                    //}
                }
            }
        });
    }
    
    /**
     * 
     */
    protected void removeSchemaLocale()
    {
        
    }
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    protected void exportSchemaLocales()
    {
        FileDialog dlg = new FileDialog(((Frame)UIRegistry.getTopWindow()), getResourceString("Save"), FileDialog.SAVE);
        dlg.setVisible(true);
        
        String fileName = dlg.getFile();
        if (fileName != null)
        {
            final File    outFile = new File(dlg.getDirectory() + File.separator + fileName);
            //final File    outFile = new File("xxx.xml");
        
            final SimpleGlassPane glassPane = new SimpleGlassPane("Exporting Schema...", 18);
            glassPane.setBarHeight(12);
            glassPane.setFillColor(new Color(0, 0, 0, 85));
            
            setGlassPane(glassPane);
            glassPane.setVisible(true);
            
            SwingWorker<Integer, Integer> backupWorker = new SwingWorker<Integer, Integer>()
            {
                @Override
                protected Integer doInBackground() throws Exception
                {
                    
                    DataProviderSessionIFace  session    = null;
                    try
                    {
                        session = DataProviderFactory.getInstance().createSession();
                        
                        String sql = "FROM SpLocaleContainer WHERE disciplineId = "+ AppContextMgr.getInstance().getClassObject(Discipline.class).getDisciplineId();
                        List<SpLocaleContainer> spContainers = (List<SpLocaleContainer>)session.getDataList(sql);

                        try
                        {
                            FileWriter fw   = new FileWriter(outFile);

                            //fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<vector>\n");
                            fw.write("<vector>\n");

                            BeanWriter      beanWriter = new BeanWriter(fw);
                            XMLIntrospector introspector = beanWriter.getXMLIntrospector();
                            introspector.getConfiguration().setWrapCollectionsInElement(true);
                            beanWriter.getBindingConfiguration().setMapIDs(false);
                            beanWriter.setWriteEmptyElements(false);

                            beanWriter.enablePrettyPrint();
                            
                            double step  = 100.0 / (double)spContainers.size();
                            double total = 0.0;
                            for (SpLocaleContainer container : spContainers)
                            {
                                // force Load of lazy collections
                                container.getDescs().size();
                                container.getNames().size();
                                beanWriter.write(container);
                                
                                total += step;
                                firePropertyChange("progress", 0, (int)total);
                            }
                            
                            fw.write("</vector>\n");
                            fw.close();

                        } catch(Exception ex)
                        {
                            ex.printStackTrace();
                        }
                        
                    } catch (Exception e)
                    {
                        edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SchemaLocalizerDlg.class, e);
                        e.printStackTrace();
                        
                    } finally
                    {
                        if (session != null)
                        {
                            session.close();
                        }
                    }

                    return null;
                }

                @Override
                protected void done()
                {
                    super.done();
                    
                    glassPane.setVisible(false);
                }
            };
            
            backupWorker.addPropertyChangeListener(
                    new PropertyChangeListener() {
                        public  void propertyChange(final PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals("progress")) 
                            {
                                glassPane.setProgress((Integer)evt.getNewValue());
                            }
                        }
                    });
            backupWorker.execute();
        }
    }
}
