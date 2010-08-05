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
package edu.ku.brc.af.ui.db;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JScrollPane;

import edu.ku.brc.af.tasks.subpane.FormPane.FormPaneAdjusterIFace;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;

/**
 * Wrap ViewBasedDisplayPane in a JScrollPanel
 *
 * @code_status alpha
 *
 * @author mkelly
 *
 */
@SuppressWarnings("serial") //$NON-NLS-1$
public class ViewBasedDisplayScrollPane extends JScrollPane implements ActionListener
{
    private ViewBasedDisplayPanel jPanel;
    
    /**
     * Constructor.
     * @param className the name of the class to be created from the selected results
     * @param idFieldName the name of the field in the class that is the primary key which is filled in from the search table id
     */
    public ViewBasedDisplayScrollPane(final String className,
                                 final String idFieldName)
    {
        this.jPanel = new ViewBasedDisplayPanel(className, idFieldName);
        this.setViewportView(jPanel);
    }

    /**
     * Constructs a search dialog from form infor and from search info.
     * @param viewSetName the viewset name
     * @param viewName the form name from the viewset
     * @param displayName the search name, this is looked up by name in the "search_config.xml" file
     * @param closeBtnTitle the title of close btn
     * @param className the name of the class to be created from the selected results
     * @param idFieldName the name of the field in the class that is the primary key which is filled in from the search table id
     * @param isEdit whether it is in edit mode or not
     * @param options the options needed for creating the form
     */
    public ViewBasedDisplayScrollPane(final Window  parent,
                                 final String  viewSetName,
                                 final String  viewName,
                                 final String  displayName,
                                 final String  className,
                                 final String  idFieldName,
                                 final boolean isEdit,
                                 final int     options)
    {
        this.jPanel = new ViewBasedDisplayPanel(parent, viewSetName, viewName, displayName, className, idFieldName, isEdit, true, null, null, options);
        this.setViewportView(jPanel);
    }

    /**
     * @param viewSetName
     * @param viewName
     * @param className
     * @param isEdit
     * @param options
     */
    public ViewBasedDisplayScrollPane(final String  viewSetName,
                                 final String  viewName,
                                 final String  className,
                                 final boolean isEdit,
                                 final int     options)
    {
        this.jPanel = new ViewBasedDisplayPanel(null, viewSetName, viewName, null, className, null, isEdit, true, null, null, options);
        this.setViewportView(jPanel);
    }

    /**
     * Constructs a search dialog from form information and from search info.
     * @param viewSetName the viewset name
     * @param viewName the form name from the viewset
     * @param displayName the search name, this is looked up by name in the "search_config.xml" file
     * @param closeBtnTitle the title of close btn
     * @param className the name of the class to be created from the selected results
     * @param idFieldName the name of the field in the class that is the primary key which is filled in from the search table id
     * @param isEdit whether it is in edit mode or not
     * @param doRegOKBtn Indicates whether the OK btn should be registered so it calls save
     * @param cellName the cellName of the data
     * @param options the options needed for creating the form
     */
    public ViewBasedDisplayScrollPane(final Window    parent,
                                 final String    viewSetName,
                                 final String    viewName,
                                 final String    displayName,
                                 final String    className,
                                 final String    idFieldName,
                                 final boolean   isEdit,
                                 final boolean   doRegOKBtn,
                                 final String    cellName,
                                 final MultiView mvParent,
                                 final int       options)
    {
        this.jPanel = new ViewBasedDisplayPanel(parent, viewSetName, viewName, displayName, className, idFieldName, isEdit, doRegOKBtn, cellName, mvParent, options);
        this.setViewportView(jPanel);
    }

    /**
     * Creates the Default UI.
     * @param viewSetName the set to to create the form
     * @param viewName the view name to use
     * @param closeBtnTitle the title of close btn
     * @param isEdit true is in edit mode, false is in view mode
     * @param cellName the cellName of the data
     * @param options the options needed for creating the form
     */
    protected void createUI(final String  viewSetName,
                            final String  viewName,
                            final boolean isEdit,
                            final String  cellName,
                            final MultiView mvParent,
                            final int     options)
    {
        this.jPanel.createUI(viewSetName, viewName, isEdit, cellName, mvParent, options);
    }
    
    /**
     * @param adjuster
     */
    public void setFormAdjuster(final FormPaneAdjusterIFace adjuster)
    {
        this.jPanel.setFormAdjuster(adjuster);
    }
    
    /**
     * Sets the OK and Cancel; buttons into the panel
     * @param okBtn ok btn (cannot be null)
     * @param cancelBtn the cancel btn (can be null
     */
    public void setOkCancelBtns(final JButton okBtn, 
                                final JButton cancelBtn)
    {
        this.jPanel.setOkCancelBtns(okBtn, cancelBtn);
    }

    /**
     * Returns whether the form is in edit mode or not.
     * @return true in edit mode, false it is not
     */
    public boolean isEditMode()
    {
        return this.jPanel.isEditMode();
    }

    /**
     * Returns the OK button.
     * @return the OK button
     */
    public JButton getOkBtn()
    {
        return this.jPanel.getOkBtn();
    }

    /**
     * Returns the Cancel button or null if there isn't one.
     * @return the Canel button or null if there isn't one.
     */
    public JButton getCancelBtn()
    {
        return this.jPanel.getCancelBtn();
    }

    /**
     * Returns true if cancelled.
     * @return true if cancelled.
     */
    public boolean isCancelled()
    {
        return this.jPanel.isCancelled();
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        this.jPanel.actionPerformed(e);
    }

    /**
     * Returns the MultiView.
     * @return the multiview
     */
    public MultiView getMultiView()
    {
        return this.jPanel.getMultiView();
    }

    /**
     * Sets data into the dialog.
     * @param dataObj the data object
     */
    public void setData(final Object dataObj)
    {
        this.jPanel.setData(dataObj);
    }
    
    /**
     * @param session
     */
    public void setSession(DataProviderSessionIFace session)
    {
        this.jPanel.setSession(session);
    }
    
    /**
     * Tells the multiview that it is about to be shown.
     * We filter out true because it has already been called during the creation process.
     * @param show the is will be shown or hidden
     */
    protected void aboutToShow(final boolean show)
    {
        this.jPanel.aboutToShow(show); 
    }

    /**
     * Tells the panel that it is being shutdown and it should be cleaned up.
     */
    public void shutdown()
    {
        this.jPanel.shutdown();
    }

}
