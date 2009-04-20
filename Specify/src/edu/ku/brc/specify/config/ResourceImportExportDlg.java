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
package edu.ku.brc.specify.config;

import static edu.ku.brc.ui.UIHelper.createButton;
import static edu.ku.brc.ui.UIHelper.createComboBox;
import static edu.ku.brc.ui.UIHelper.createLabel;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Node;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.AppResourceIFace;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.persist.ViewIFace;
import edu.ku.brc.af.ui.forms.persist.ViewSetIFace;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.SpAppResource;
import edu.ku.brc.specify.datamodel.SpAppResourceDir;
import edu.ku.brc.specify.datamodel.SpReport;
import edu.ku.brc.specify.datamodel.SpViewSetObj;
import edu.ku.brc.specify.datamodel.SpecifyUser;
import edu.ku.brc.specify.tasks.QueryTask;
import edu.ku.brc.specify.tasks.ReportsBaseTask;
import edu.ku.brc.specify.tools.ireportspecify.MainFrameSpecify;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Beta
 *
 * Dec 1, 2007
 *
 */
@SuppressWarnings("serial")
public class ResourceImportExportDlg extends CustomDialog
{
    protected static final Logger  log = Logger.getLogger(ResourceImportExportDlg.class);
    
    protected JComboBox              levelCBX;
    protected JList                  viewSetsList;
    protected DefaultListModel       viewSetsModel = new DefaultListModel();
    
    protected JList                  viewsList;
    protected DefaultListModel       viewsModel = new DefaultListModel();
    protected JPanel				 viewsPanel = null;
    
    protected JList                  resList;
    protected DefaultListModel       resModel = new DefaultListModel();
    protected JPanel				 resPanel = null;

    protected JList					 repList;
    protected DefaultListModel       repModel = new DefaultListModel();
    protected JPanel				 repPanel = null;

    protected JTabbedPane            tabbedPane;
    
    
    protected JButton                exportBtn;
    protected JButton                importBtn;
    protected JButton                revertBtn;

    protected List<SpAppResource>    resources = new Vector<SpAppResource>();
    protected List<SpAppResourceDir> dirs;
    
    protected boolean                hasChanged = false;

    /**
     * @throws HeadlessException
     */
    public ResourceImportExportDlg() throws HeadlessException
    {
        super((Frame)UIRegistry.getTopWindow(), 
                getResourceString("RIE_TITLE"), 
                true, 
                OKHELP,
                null);
        okLabel = getResourceString("CLOSE");
    }
    
    /**
     * @param dir the directory
     * @return a title that describes the hierarchy.
     */
    protected String getHierarchicalTitle(final SpAppResourceDir dir)
    {
        DBTableInfo collectionTI = DBTableIdMgr.getInstance().getByClassName(Collection.class.getName());
        DBTableInfo disciplineTI = DBTableIdMgr.getInstance().getByClassName(Discipline.class.getName());
        
        String hierTitle = "XXX";
        if (dir.getIsPersonal())
        {
            hierTitle = dir.getTitle();
            
        } else if (dir.getUserType() != null)
        {
            if (dir.getUserType().equals("Common") || dir.getUserType().equals("BackStop"))
            {
                hierTitle = dir.getTitle();
            } else
            {
                hierTitle = getResourceString("RIE_GROUP");
            }
            
        } else if (dir.getCollection() != null)
        {
            hierTitle = dir.getCollection().getCollectionName() + " ("+collectionTI.getTitle()+")";
            
        } else if (dir.getDiscipline() != null)
        {
            hierTitle = dir.getDiscipline().getType() + " ("+disciplineTI.getTitle()+")";
        } else
        {
            hierTitle = dir.getIdentityTitle();
        }
        return hierTitle;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CustomDialog#createUI()
     */
    @Override
    public void createUI()
    {
        super.createUI();

        this.setHelpContext("Import");

        CellConstraints cc = new CellConstraints();
        
        levelCBX = createComboBox();
        
        SpecifyAppContextMgr context = (SpecifyAppContextMgr)AppContextMgr.getInstance();
        dirs = context.getSpAppResourceList();
        for (SpAppResourceDir dir : dirs)
        {
            levelCBX.addItem(getHierarchicalTitle(dir));
        }
        
        PanelBuilder  centerPB = new PanelBuilder(new FormLayout("f:p:g,p,f:p:g", "p"));
        centerPB.add(levelCBX, cc.xy(2,1));

        tabbedPane = new JTabbedPane();
        
        PanelBuilder viewPanel = new PanelBuilder(new FormLayout("f:p:g,10px,f:p:g", "p,2px,f:p:g"));
        viewPanel.add(createLabel(getResourceString("RIE_VIEWSETS"), SwingConstants.CENTER),   cc.xy(1,1));
        viewSetsList = new JList(viewSetsModel);
        viewSetsList.setCellRenderer(new ARListRenderer());
        JScrollPane sp = new JScrollPane(viewSetsList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        viewPanel.add(sp, cc.xy(1,3));
        
        viewPanel.add(createLabel(getResourceString("RIE_VIEWS"), SwingConstants.CENTER),   cc.xy(3,1));
        viewsList = new JList(viewsModel);
        viewsList.setCellRenderer(new ViewRenderer());
        sp = new JScrollPane(viewsList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        viewPanel.add(sp, cc.xy(3,3));
        viewsList.setEnabled(false);
        
        PanelBuilder resPane = new PanelBuilder(new FormLayout("f:p:g", "p,2px,p"));
        resPane.add(createLabel(getResourceString("RIE_OTHER_RES"), SwingConstants.CENTER), cc.xy(1,1));
        resList   = new JList(resModel);
        resList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resList.setCellRenderer(new ARListRenderer());
        sp = new JScrollPane(resList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        resPane.add(sp, cc.xy(1,3));

        PanelBuilder repPane = new PanelBuilder(new FormLayout("f:p:g", "p,2px,f:p:g"));
        resPane.add(createLabel(getResourceString("RIE_REPORT_RES"), SwingConstants.CENTER), cc.xy(1,1));
        repList   = new JList(repModel);
        repList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repList.setCellRenderer(new ARListRenderer());
        sp = new JScrollPane(repList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        repPane.add(sp, cc.xy(1,3));
        
        boolean addResourcesPanel = AppPreferences.getLocalPrefs().getBoolean("ADD_IMP_RES", false);

        
        viewsPanel = viewPanel.getPanel();
        tabbedPane.addTab(getResourceString("RIE_VIEWSETS"), viewsPanel);
        if (addResourcesPanel)
        {
            resPanel = resPane.getPanel();
            tabbedPane.addTab(getResourceString("RIE_OTHER_RES"), resPanel);
        }
        repPanel = repPane.getPanel();
        tabbedPane.addTab(getResourceString("RIE_REPORT_RES"), repPanel);
        
        PanelBuilder    pb = new PanelBuilder(new FormLayout("f:p:g", "p,4px,f:p:g,2px,p"));
        pb.add(centerPB.getPanel(), cc.xy(1,1));
        pb.add(tabbedPane,          cc.xy(1,3));
        
        exportBtn = createButton(getResourceString("RIE_EXPORT"));
        importBtn = createButton(getResourceString("RIE_IMPORT"));
        revertBtn  = createButton(getResourceString("RIE_REVERT"));
        PanelBuilder btnPB = new PanelBuilder(new FormLayout("f:p:g,p,f:p:g,p,f:p:g,p,f:p:g", "p,10px"));
        btnPB.add(exportBtn, cc.xy(2,1));
        btnPB.add(importBtn, cc.xy(4,1));
        btnPB.add(revertBtn,  cc.xy(6,1));
                
        pb.add(btnPB.getPanel(), cc.xy(1,5));
        
        
        pb.getPanel().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel = pb.getPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                Component selectedComp = tabbedPane.getSelectedComponent();
                if (selectedComp != null)
                {
                	if (selectedComp == viewsPanel)
                	{
                		viewSetsList.setSelectedIndex(-1);
                	} else if (selectedComp == resPanel)
                	{
                		resList.setSelectedIndex(-1);
                	}
                	else
                	{
                		repList.setSelectedIndex(-1);
                	}
                	revertBtn.setVisible(selectedComp != repPanel);
                }
                enableUI();
            }
        });
        
        levelCBX.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        levelSelected();
                    }
                });
            }
        });
        
        levelCBX.setSelectedIndex(0);
        
        pack();
        
        exportBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                exportResource();
            }
        });
        
        importBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                importResource();
            }
        });
        
        revertBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                revertResource();
            }
        });
        
        
        viewSetsList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    if (viewSetsList.getSelectedIndex() > -1)
                    {
                        resList.clearSelection(); 
                        repList.clearSelection();
                    }
                    fillViewsList();
                    enableUI();
                }
            }
        });
        
        resList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    if (resList.getSelectedIndex() > -1)
                    {
                        viewSetsList.clearSelection(); 
                        repList.clearSelection();
                    }
                    enableUI();
                }
            }
        });

        repList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    if (resList.getSelectedIndex() > -1)
                    {
                        viewSetsList.clearSelection(); 
                        resList.clearSelection();
                    }
                    enableUI();
                }
            }
        });
        
        pack();
}
    
    /**
     * Fill the list with the view names.
     */
    protected void fillViewsList()
    {
        int levelIndex = levelCBX.getSelectedIndex();
        if (levelIndex > -1)
        {
            SpAppResourceDir dir = dirs.get(levelIndex);
            viewsModel.clear();
            levelIndex = viewSetsList.getSelectedIndex();
            if (levelIndex > -1)
            {
                ViewSetIFace vs = ((SpecifyAppContextMgr)AppContextMgr.getInstance()).getViewSetList(dir).get(levelIndex);
                Vector<ViewIFace> views = new Vector<ViewIFace>(vs.getViews().values());
                Collections.sort(views);
                for (ViewIFace view : views)
                {
                    viewsModel.addElement(view);
                }
            }
        }
    }
    
    /**
     * Enables the Import / Export and Revert Buttons
     */
    protected void enableUI()
    {
        int currentTabIndex = tabbedPane.getModel().getSelectedIndex();
        
        if (currentTabIndex == 0) // Views
        {
            boolean enable = !viewSetsList.isSelectionEmpty();
            
            importBtn.setEnabled(enable && levelCBX.getSelectedIndex() < 5);
            exportBtn.setEnabled(enable && viewSetsModel.size() > 0);
            
            SpViewSetObj vso = (SpViewSetObj)viewSetsList.getSelectedValue();
            
            revertBtn.setEnabled(vso != null && vso.getId() != null);
            
        } else if (currentTabIndex != -1)
        {
            
        	JList activeList = tabbedPane.getSelectedComponent() == resPanel ? resList : repList;
        	if (activeList.getSelectedValue() instanceof String)
            {
                importBtn.setEnabled(true);
                exportBtn.setEnabled(false);
                revertBtn.setEnabled(false);
                
            } else
            {
                boolean enable = !activeList.isSelectionEmpty();
                
                importBtn.setEnabled(enable && levelCBX.getSelectedIndex() < 2);
                exportBtn.setEnabled(enable && ((DefaultListModel )activeList.getModel()).size() > 1);
                
                SpAppResource appRes = (SpAppResource)activeList.getSelectedValue();
                enable = false;
                if (appRes != null && appRes.getId() != null)
                {
                	if (appRes.getMimeType() != null && 
                			(appRes.getMimeType().equals(ReportsBaseTask.REPORTS_MIME) 
                					|| appRes.getMimeType().equals(ReportsBaseTask.LABELS_MIME)))
                	{
                		if (!isSpReportResource((SpAppResource )appRes))
                		{
                			//XXX what if appres is imported report with no config file???
                			//enable = true;
                			
                			//revert not currently working
                		}
                		
                	} else
                	{
                	    enable = true;
                	}
                }
                revertBtn.setEnabled(enable);
            }
        }
        
    }
    
    /**
     * Revert a resource. Re-read it from the disk.
     */
    protected void revertResource()
    {
        int index = levelCBX.getSelectedIndex();
        if (index > -1)
        {
            String exportedName   = null;
            String virtualDirName = SpecifyAppContextMgr.getVirtualDirName(index);
            
            if (tabbedPane.getSelectedComponent() == viewsPanel)
            {
                SpViewSetObj vso = (SpViewSetObj)viewSetsList.getSelectedValue();
                if (vso != null)
                {
                    exportedName = vso.getName();
                    
                    index = viewSetsList.getSelectedIndex();
                    if (index > -1)
                    {
                        viewSetsModel.remove(index);
                        SpViewSetObj revertedNewVSO = ((SpecifyAppContextMgr)AppContextMgr.getInstance()).revertViewSet(virtualDirName, vso.getName());
                        if (revertedNewVSO != null)
                        {
                            viewSetsModel.insertElementAt(revertedNewVSO, index);
                            viewSetsList.setSelectedIndex(index);
                            setHasChanged(true);
                        }
                    }
                }
            }
            else
            {
            	JList theList = tabbedPane.getSelectedComponent() == repPanel ? repList : resList;
            	DefaultListModel theModel = theList == repList ? repModel : resModel;
            	index = theList.getSelectedIndex(); 
            	if (index > 0)
            	{
            		AppResourceIFace appRes = resources.get(index - 1); 
                
            		AppResourceIFace revertedNewAR = ((SpecifyAppContextMgr)AppContextMgr.getInstance()).revertResource(virtualDirName, appRes);
                
            		setHasChanged(true);
                
            		if (revertedNewAR != null)
            		{
            			theModel.insertElementAt(revertedNewAR, index);
            			theList.setSelectedIndex(index);
            		} else
            		{
            			theModel.removeElementAt(index);
            			theList.clearSelection();
            		}
                
            		levelSelected();
            	}
            }
            
            if (exportedName != null)
            {
                UIRegistry.getStatusBar().setText(UIRegistry.getLocalizedMessage("RIE_RES_REVERTED", exportedName));
            }
            
            enableUI();
        }
    }
    
    private void setHasChanged(final boolean changed)
    {
        hasChanged = changed;
        okBtn.setText(getResourceString(changed ? "EXIT" : "CLOSE"));
    }
    
    /**
     * @return the hasChanged
     */
    public boolean hasChanged()
    {
        return hasChanged;
    }

    /**
     * 
     */
    protected void exportResource()
    {
        
        int index = levelCBX.getSelectedIndex();
        if (index > -1)
        {
        	String exportedName = null;
            
            String data     = null;
            String fileName = null;
            AppResourceIFace appRes = null;
            if (tabbedPane.getSelectedComponent() == viewsPanel)
            {
                if (viewSetsList.getSelectedIndex() > -1)
                {
                    SpViewSetObj vso = (SpViewSetObj)viewSetsList.getSelectedValue();
                    exportedName = vso.getName();
                    fileName     = FilenameUtils.getName(vso.getFileName());
                    data         = vso.getDataAsString();
                }
            }
            else 
            {
            	JList theList = tabbedPane.getSelectedComponent() == repPanel ? repList : resList;
                if (theList.getSelectedIndex() >  0)
                {
                    appRes = (AppResourceIFace )theList.getSelectedValue();
                    exportedName = appRes.getName();
                    fileName     = FilenameUtils.getName(exportedName);
                    data         = appRes.getDataAsString();
                    
                }   
            }
            
            if (StringUtils.isNotEmpty(data))
            {
                FileDialog fileDlg = new FileDialog(this, "RIE_ExportResource", FileDialog.SAVE); 
                fileDlg.setFile(fileName);
                fileDlg.setVisible(true);
                
                String dirStr = fileDlg.getDirectory();
                fileName      = fileDlg.getFile();
                
                if (StringUtils.isNotEmpty(dirStr) && StringUtils.isNotEmpty(fileName))
                {
                    File expFile  = new File(dirStr + File.separator + fileName);
                    try
                    {
                        if (isReportResource((SpAppResource )appRes) && isSpReportResource((SpAppResource )appRes))
                        {
                        	writeSpReportResToZipFile(expFile, data, appRes);
                        }
                        else
                        {
                        	FileUtils.writeStringToFile(expFile, data);
                        }
                        
                    } catch (Exception ex)
                    {
                        edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(ResourceImportExportDlg.class, ex);
                        ex.printStackTrace();
                    }
                }
            }
            
            if (exportedName != null)
            {
                UIRegistry.getStatusBar().setText(UIRegistry.getLocalizedMessage("RIE_RES_EXPORTED", exportedName));
            }
        }
    }
    
    /**
     * @param data
     * @return true if data represents a report resource.
     */
    protected boolean isSpReportResource(final String data)
    {
    	return data.indexOf("<reportresource name=") == 0; 
    }
    
    protected boolean isSpReportResource(final SpAppResource res)
    {
        if (res.getId() == null)
        {
        	return false;
        }
        
    	DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        try
        {
            return session.getData("from SpReport where appResourceId = " + res.getId()) != null;
        }
        finally
        {
        	session.close();
        }

    }
    /**
     * @param data
     * @return true if data is a Jasper report definition (a .jrxml file).
     * 
     */
    protected boolean isJasperReport(final String data)
    {
    	try
    	{
    		Element element = XMLHelper.readStrToDOM4J(data);
    		return element.getDocument().getDocType().getName().equals("jasperReport");
    	}
    	catch (Exception ex)
    	{
    		return false;
    	}
    }
    
    /**
     * @param file
     * @return name of report resource contained in file, or null if file does not
     * contain a report resource.
     */
    protected String getSpReportResourceName(final File file)
    {
    	try
    	{
    		ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
    		ZipEntry app = zin.getNextEntry();
    		if (app == null)
    		{
    			return null;
    		}
    		if (zin.available() == 0)
    		{
    			return null;
    		}
    		String appStr = readZipEntryToString(zin, app);
    		if (isSpReportResource(appStr))
    		{
    			Element appElement = XMLHelper.readStrToDOM4J(appStr);
    			return XMLHelper.getAttr(appElement, "name", null);
    		}
    		return null;
    	}
    	catch (ZipException ex)
    	{
    		//I think this means it is not a zip file.
    		return null;
    	}
    	catch (Exception ex)
    	{
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyAppContextMgr.class, ex);
    		return null;
    	}
    }
    
    /**
     * @param zin
     * @param entry
     * @return the contents of entry.
     * @throws IOException
     */
    protected String readZipEntryToString(final ZipInputStream zin,
			final ZipEntry entry) throws IOException
	{
		StringBuilder result = new StringBuilder();
		byte[] bytes = new byte[100];
		int bytesRead = zin.read(bytes, 0, 100);
		while (bytesRead > 0)
		{
			result.append(new String(bytes, 0, bytesRead));
			bytesRead = zin.read(bytes, 0, 100);
		}
		return result.toString();
	}
    
    /**
     * @param expFile
     * @param data
     * @param appRes
     * @throws IOException
     * 
     * writes the contents of a report resource - the AppResource data, plus
     * SpReport, SpQuery, SpQueryField if necessary to a single text file.
     * 
     */
    //XXX obsolete.
    protected void writeReportResToFile(final File expFile, final String data, final AppResourceIFace appRes) throws IOException
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<reportresource name=\"" + appRes.getName() + "\">\r\n");
    	sb.append("<resourcedata>\r\n");
    	sb.append("<header>\r\n");
    	int reportTag = data.indexOf("<jasperReport");
    	String header = data.substring(0, reportTag);
    	sb.append("<![CDATA[\r\n");
    	sb.append(header);
    	sb.append("]]>\r\n");
    	sb.append("</header>\r\n");
    	sb.append(data.substring(reportTag));
    	sb.append("\r\n</resourcedata>\r\n");
    	sb.append("<metadata > <![CDATA[");
    	sb.append(appRes.getMetaData());
    	sb.append("]]>");
    	sb.append("</metadata>\r\n");
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        try
        {
            SpReport spRep = (SpReport )session.getData("from SpReport where appResourceId = " +
            		((SpAppResource )appRes).getId());
            if (spRep != null)
            {
            	spRep.forceLoad();
            	spRep.toXML(sb);
            }
        }
        finally
        {
        	session.close();
        }
    	sb.append("\r\n</reportresource>\r\n");
    	FileUtils.writeStringToFile(expFile, sb.toString());    
    }
 
    /**
     * @param expFile
     * @param data
     * @param appRes
     * @throws IOException
     * 
     * writes the contents of a report resource to a zip file. 
     * Currently creates 3 entries: 1) AppResource, 2) AppResource data,
     * and if present, 3)SpReport, SpQuery, SpQueryFields.
     * 
     */
    //XXX implement support for subreports
    protected void writeSpReportResToZipFile(final File expFile, final String data, final AppResourceIFace appRes) throws IOException
    {
    	StringBuilder sb = new StringBuilder();
    	
    	ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(expFile));
    	
    	//the appResource name and metadata
    	sb.append("<reportresource name=\"" + appRes.getName() + "\">\r\n");
    	sb.append("<metadata > <![CDATA[");
    	sb.append(appRes.getMetaData());
    	sb.append("]]>");
    	sb.append("</metadata>\r\n");
    	sb.append("\r\n</reportresource>\r\n");
    	
    	zout.putNextEntry(new ZipEntry("app.xml"));
    	byte[] bytes = sb.toString().getBytes();
    	zout.write(bytes, 0, bytes.length);
    	zout.closeEntry();
    	    	
    	//the data
    	zout.putNextEntry(new ZipEntry("data.xml"));
    	bytes = data.getBytes();
    	zout.write(bytes, 0, bytes.length);
    	zout.closeEntry();

    	
    	//the spReport
    	sb.setLength(0);
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        try
        {
            SpReport spRep = (SpReport )session.getData("from SpReport where appResourceId = " +
            		((SpAppResource )appRes).getId());
            if (spRep != null)
            {
            	spRep.forceLoad();
            	spRep.toXML(sb);
            	bytes = sb.toString().getBytes();
            	zout.putNextEntry(new ZipEntry("SpReport.xml"));
            	zout.write(bytes, 0, bytes.length);
            	zout.closeEntry();
            }
        }
        finally
        {
        	session.close();
        }        
        zout.close();
    }

    /**
     * @param root
     * @param data
     * 
     * imports a report resource from a text file.
     */
    //XXX obsolete.
    protected void importReportResource(final Element root, final String data)
    {
    	boolean resourceSaved = false;
        int index = levelCBX.getSelectedIndex();
        AppResourceIFace appRes = null;
        SpAppResourceDir dir = dirs.get(index);
    	try
    	{
    		Node header = root.selectSingleNode("resourcedata/header");
    		String headerStr = header.getStringValue().trim();
    		int jrStart = data.indexOf("<jasperReport");
    		int jrEnd = data.indexOf("</jasperReport>") + "</jasperReport>".length();
    		String jrStr = data.substring(jrStart, jrEnd);
    		String appResData = headerStr + " " + jrStr.trim();
            appRes = ((SpecifyAppContextMgr )AppContextMgr.getInstance()).createAppResourceForDir(dir);
            appRes.setDataAsString(appResData);
            Node metadata = root.selectSingleNode("metadata");
            String metadataStr = metadata.getStringValue() + ";";
            appRes.setMimeType("jrxml/report"); 
            appRes.setMetaData(metadataStr.trim());
            appRes.setLevel((short )3);
            Element repElement = root.element("report");
            if (repElement != null)
            {
            	SpReport rep = new SpReport();
            	rep.initialize();
            	rep.setSpecifyUser(AppContextMgr.getInstance().getClassObject(SpecifyUser.class));
            	rep.fromXML(repElement);
            	appRes.setName(rep.getName());
                appRes.setDescription(appRes.getName());
                AppContextMgr.getInstance().saveResource(appRes); 
                resourceSaved = true;
            	rep.setAppResource((SpAppResource)appRes);
            	DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
                boolean transOpen = false;
            	try
                {
                	session.beginTransaction();
                	transOpen = true;
                	if (rep.getQuery().getId() == null)
                	{
                		session.saveOrUpdate(rep.getQuery());
                	}
                	session.saveOrUpdate(rep);
                	session.commit();
                	transOpen = false;
                }
                finally
                {
                	if (transOpen)
                	{
                		session.rollback();
                	}
                	session.close();
                }
            }
        }
    	catch (Exception e)
    	{
    		if (resourceSaved)
    		{
    			((SpecifyAppContextMgr )AppContextMgr.getInstance()).removeAppResourceSp(dir, appRes);
    		}
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyAppContextMgr.class, e);
            log.error(e);
    	}
    	
    }

    /**
     * @param file
     * 
     * Imports a report resource from a zip file (see writeReportResToZipFile())
     * 
     * If resource contains an SpReport, the SpReport's data source query will be imported
     * as well if an equivalent query does not exist in the database.
     *  
     */
    protected String importSpReportZipResource(final File file)
    {
    	boolean resourceSaved = false;
        int index = levelCBX.getSelectedIndex();
        AppResourceIFace appRes = null;
        SpAppResourceDir dir = dirs.get(index);

        try
    	{
    		
        	ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
        	
        	//Assuming they come out in the order they were put in.
    		ZipEntry entry = zin.getNextEntry();
    		if (entry == null)
    		{
    			throw new Exception(getResourceString("RIE_ReportImportFileError"));
    		}
    		String app = readZipEntryToString(zin, entry);
    		zin.closeEntry();
    		
    		entry = zin.getNextEntry();
    		if (entry == null)
    		{
    			throw new Exception(getResourceString("RIE_ReportImportFileError"));
    		}
    		String data = readZipEntryToString(zin, entry);
    		zin.closeEntry();
    		
            appRes = ((SpecifyAppContextMgr )AppContextMgr.getInstance()).createAppResourceForDir(dir);
            appRes.setDataAsString(data);
    		Element appRoot = XMLHelper.readStrToDOM4J(app);
            Node metadata = appRoot.selectSingleNode("metadata");
            String metadataStr = metadata.getStringValue() + ";";
            appRes.setMetaData(metadataStr.trim());
            String repType = appRes.getMetaDataMap().getProperty("reporttype");
            if (repType != null && repType.equalsIgnoreCase("label"))
            {
                appRes.setMimeType("jrxml/label"); 
            }
            else
            {
            	appRes.setMimeType("jrxml/report"); 
            }
            //XXX level?????????????????
            appRes.setLevel((short )3);

            entry = zin.getNextEntry();
            if (entry != null)
            {
        		String spReport = readZipEntryToString(zin, entry);
        		zin.closeEntry();
        		Element repElement = XMLHelper.readStrToDOM4J(spReport);
        		SpReport rep = new SpReport();
				rep.initialize();
				rep.setSpecifyUser(AppContextMgr.getInstance().getClassObject(SpecifyUser.class));
				rep.fromXML(repElement);
				appRes.setName(rep.getName());
				appRes.setDescription(appRes.getName());
				AppContextMgr.getInstance().saveResource(appRes);
				resourceSaved = true;
				rep.setAppResource((SpAppResource) appRes);
				DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        		boolean transOpen = false;
        		try
        		{
                	session.beginTransaction();
                	transOpen = true;
                	if (rep.getQuery().getId() == null)
                	{
                		session.saveOrUpdate(rep.getQuery());
                	}
                	session.saveOrUpdate(rep);
                	session.commit();
                	transOpen = false;
                }
                finally
                {
                	if (transOpen)
                	{
                		session.rollback();
                	}
                	session.close();
                }
            }
    		zin.close();
        }
    	catch (Exception e)
    	{
    		if (resourceSaved)
    		{
    			((SpecifyAppContextMgr )AppContextMgr.getInstance()).removeAppResourceSp(dir, appRes);
    		}
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpecifyAppContextMgr.class, e);
            log.error(e);
            return null;
    	}
    	if (appRes != null)
    	{
    		return appRes.getName();
    	}
    	else
    	{
    		return null;
    	}
    }

    /**
     * @param appRes
     * 
     * removes appRes and its associated SpReport
     */
    protected void removeSpResource(SpAppResource appRes)
    {
    	ReportsBaseTask.deleteReportAndResource(null, appRes);
    }
    
    /**
     * 
     */
    protected void importResource()
{
	int levelIndex = levelCBX.getSelectedIndex();
	if (levelIndex > -1)
	{
		String importedName = null;

		FileDialog fileDlg = new FileDialog(this,
				getResourceString("RIE_IMPORT_RES"), FileDialog.LOAD);
		fileDlg.setVisible(true);

		String dirStr = fileDlg.getDirectory();
		String fileName = fileDlg.getFile();

		if (StringUtils.isNotEmpty(dirStr) && StringUtils.isNotEmpty(fileName))
		{
			String data = null;
			File importFile = new File(dirStr + File.separator + fileName);
			String repResourceName = getSpReportResourceName(importFile); 
			boolean isSpRepRes = repResourceName != null;
			boolean isJRRepRes = false;
			try
			{
				data = FileUtils.readFileToString(importFile);
				isJRRepRes = isJasperReport(data);

			} catch (Exception ex)
			{
				edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
				edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(ResourceImportExportDlg.class, ex);
				ex.printStackTrace();
				return;
			}
			
			boolean isRepRes = isJRRepRes || isSpRepRes;
			
			if (tabbedPane.getSelectedComponent() == viewsPanel)
			{
				int viewIndex = viewSetsList.getSelectedIndex();
				if (viewIndex > -1)
				{
					boolean isOK = false;
					SpViewSetObj vso = null;

					DataProviderSessionIFace session = null;
					try
					{
						session = DataProviderFactory.getInstance().createSession();
						session.beginTransaction();

						vso = (SpViewSetObj) viewSetsList.getSelectedValue();
						if (vso.getId() == null)
						{
							// vso = (SpViewSetObj)vso.clone();
						}
						SpAppResourceDir appResDir = vso.getSpAppResourceDir();
						importedName = vso.getName();

						if (vso.getSpViewSetObjId() == null)
						{
							appResDir.getSpPersistedViewSets().add(vso);
							vso.setSpAppResourceDir(appResDir);
						}
						vso.setDataAsString(data);
						session.saveOrUpdate(appResDir);
						session.saveOrUpdate(vso);
						session.commit();
						session.flush();

						setHasChanged(true);
						isOK = true;

					} catch (Exception ex)
					{
						edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
						edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(ResourceImportExportDlg.class, ex);
						session.rollback();

						ex.printStackTrace();

					} finally
					{
						try
						{
							session.close();

						} catch (Exception ex)
						{
	                        edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
	                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(ResourceImportExportDlg.class, ex);
							ex.printStackTrace();
						}
					}

					if (isOK)
					{
						viewSetsModel.remove(viewIndex);
						viewSetsModel.insertElementAt(vso, viewIndex);
						viewSetsList.repaint();
					}
				}

			} else
			{
				JList theList = tabbedPane.getSelectedComponent() == repPanel ? repList : resList;
				int resIndex = theList.getSelectedIndex();
				if (resIndex > -1)
				{
					if (resIndex == 0)
					{
						try
						{
							SpAppResourceDir dir = dirs.get(levelIndex);
							SpAppResource appRes = new SpAppResource();
							appRes.initialize();

							SpAppResource fndAppRes = checkForOverrideAppRes(isSpRepRes ? repResourceName : fileName);
							if (fndAppRes != null)
							{
								// Show Dialog here and tell them it found a
								// resource to override by the same name
								// and ask whether they want to override it
								// or not
								String title = getResourceString("RIE_ConfirmResourceOverwriteTitle");
								String msg   = String.format(getResourceString("RIE_ConfirmResourceOverwriteMsg"), fndAppRes.getName(), getHierarchicalTitle(fndAppRes.getSpAppResourceDir()));
								if (!UIRegistry.displayConfirm(title, msg, getResourceString("Yes"), getResourceString("Cancel"), JOptionPane.WARNING_MESSAGE))
								{
									return;
								}
							}

							if (isRepRes)
							{
								// importReportResource(reportDom, data);
								if (fndAppRes != null)
								{
									if (isSpRepRes)
									{
										this.removeSpResource(fndAppRes);
									}
									else
									{
										//XXX ???????????
										if (fndAppRes.getSpAppResourceId() != null)
										{
											((SpecifyAppContextMgr) AppContextMgr.getInstance()).removeAppResourceSp(fndAppRes.getSpAppResourceDir(), fndAppRes);
										}
									}
								}
								if (isSpRepRes)
								{
									importedName = importSpReportZipResource(importFile);
								}
								else
								{
						            if (MainFrameSpecify.importJasperReport(importFile, false))
						            {
						            	importedName = importFile.getName();
						            }
								}
								if (importedName != null)
								{
						            CommandDispatcher.dispatch(new CommandAction(ReportsBaseTask.REPORTS, ReportsBaseTask.REFRESH, null));
						            CommandDispatcher.dispatch(new CommandAction(QueryTask.QUERY, QueryTask.REFRESH_QUERIES, null));
						            levelSelected();
								}
							}
							
							else
							{
								if (fndAppRes != null) // overriding
								{
									appRes.setMetaData(fndAppRes.getMetaData());
									appRes.setDescription(fndAppRes.getDescription());
									appRes.setFileName(fileName);
									appRes.setMimeType(appRes.getMimeType());
									appRes.setName(fileName);

									SpecifyUser user = AppContextMgr.getInstance().getClassObject(SpecifyUser.class);
									Agent agent = AppContextMgr.getInstance().getClassObject(Agent.class);
									appRes.setCreatedByAgent(agent);
									appRes.setSpecifyUser(user);

									appRes.setLevel(fndAppRes.getLevel());

								} else if (!getMetaInformation(appRes))
								{
									return;
								}

								appRes.setSpAppResourceDir(dir);
								dir.getSpAppResources().add(appRes);

								appRes.setDataAsString(data);
								SpecifyAppContextMgr mgr = (SpecifyAppContextMgr) AppContextMgr.getInstance();
								mgr.saveResource(appRes);
							}
						} catch (Exception e)
						{
							edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
							edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(ResourceImportExportDlg.class, e);
							e.printStackTrace();
						}
					} else
					{
						resIndex++;
						AppResourceIFace appRes = resources.get(resIndex);
						importedName = appRes.getName();
						String fName = FilenameUtils.getName(importedName);
						String dbBaseName = FilenameUtils.getBaseName(fileName);
						if (dbBaseName.equals(fName))
						{
							appRes.setDataAsString(data);
							((SpecifyAppContextMgr) AppContextMgr.getInstance()).saveResource(appRes);
						}

					}
				}
			}
		}

		if (importedName != null)
		{
			UIRegistry.getStatusBar().setText(
					UIRegistry.getLocalizedMessage("RIE_RES_IMPORTED",
							importedName));
		}

		enableUI();
	}
}
    
    /**
	 * @return
	 */
    protected boolean getMetaInformation(final SpAppResource appRes)
    {
        ResImpExpMetaInfoDlg dlg = new ResImpExpMetaInfoDlg(appRes);
        dlg.setVisible(true);
        return !dlg.isCancelled();
    }
    
    /**
	 * @param title
	 * @return
	 */
    protected SpAppResource checkForOverrideAppRes(final String filename/*
																		 * ,
																		 * final
																		 * Element
																		 * dom
																		 */)
    {
    	String name = filename;
// if (dom != null)
// {
// name = XMLHelper.getAttr(dom, "name", filename);
// }
    	for (SpAppResourceDir dir : dirs)
        {
            for (SpAppResource ar : dir.getSpAppResources())
            {
                if (ar.getName() != null && ar.getName().equals(name))
                {
                    return ar;
                }
            }
        }
        return null;
    }
    
    /**
     * @param appRes
     * @return true if appRes is a report resource
     */
    protected boolean isReportResource(final SpAppResource appRes)
    {
    	return appRes != null && appRes.getMimeType() != null && 
			(appRes.getMimeType().equals(ReportsBaseTask.REPORTS_MIME) 
					|| appRes.getMimeType().equals(ReportsBaseTask.LABELS_MIME)
					|| appRes.getMimeType().equals(ReportsBaseTask.SUBREPORTS_MIME));    
	}
    
    /**
	 * A Virtual Directory Level has been choosen.
	 */
    protected void levelSelected()
    {
        int index = levelCBX.getSelectedIndex();
        if (index > -1)
        {
            SpAppResourceDir dir = dirs.get(index);
            viewSetsModel.clear();
            viewsModel.clear();
            resModel.clear();
            repModel.clear();
            
            resources.clear();
            resources.addAll(dir.getSpAppResources());
            resModel.addElement(UIRegistry.getResourceString("RIE_ADD_NEW_RESOURCE"));
            repModel.addElement(UIRegistry.getResourceString("RIE_ADD_NEW_RESOURCE"));
            for (SpAppResource appRes : resources)
            {
                if (isReportResource(appRes))
                {
                	repModel.addElement(appRes);
                }
                else
                {
                	resModel.addElement(appRes);
                }
            }
             
            for (SpViewSetObj vso : dir.getSpViewSets())
            {
                viewSetsModel.addElement(vso);
            }
            
            viewSetsList.setSelectedIndex(viewSetsModel.size() > 0 ? 0 : -1);
            
            enableUI();
            
        }
    }
    
    
    // ------------------------------------------------------------------------------------------------------
    // --
    // ------------------------------------------------------------------------------------------------------
    class ARListRenderer extends DefaultListCellRenderer
    {
        private String databaseStr = getResourceString("RIE_FROM_DATABASE");
        
        /*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
		 *      java.lang.Object, int, boolean, boolean)
		 */
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            String title;
            Integer id;
            if (value instanceof SpViewSetObj)
            {
                title = ((SpViewSetObj)value).getName();
                id    = ((SpViewSetObj)value).getId();
                
            } else if (value instanceof SpAppResource)
            {
                title = ((SpAppResource)value).getName();
                id    = ((SpAppResource)value).getId();
            } else
            {
                title = value.toString();
                id    = null;
            }
            if (id != null)
            {
                title += " " + databaseStr;
            }
            
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            label.setText(title);
            
            return label;
        }
    }
    
    class ViewRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            return super.getListCellRendererComponent(list, value, index, false, false);
        }
    }
}
