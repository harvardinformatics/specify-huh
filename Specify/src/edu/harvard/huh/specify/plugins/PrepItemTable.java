/*
 * Created on 2011 January 6
 *
 * Copyright © 2011 President and Fellows of Harvard College
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */

package edu.harvard.huh.specify.plugins;

import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static edu.ku.brc.ui.UIRegistry.loadAndPushResourceBundle;
import static edu.ku.brc.ui.UIRegistry.popResourceBundle;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeListener;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.UIRegistry;

/**
 * This class is a plugin implemented as a JPanel component for listing multiple 
 * barcoded items per preparation on the loanpreparation form.
 */
@SuppressWarnings("serial")
public class PrepItemTable extends JPanel implements UIPluginable, GetSetValueIFace
{
    private String title = null;
    private JTable table;

    private FormViewObj parent = null;
    private Preparation preparation;
    private LoanPreparation loanPreparation;
    
    private static final Vector<String> columnNames = new Vector<String>();
    private Vector<Vector<String>> data;

    /**
     * The constructor initializes a Vector of column names as strings and
     * sets up the plugin.
     */
	public PrepItemTable() {
        super(new GridLayout(1,0));
        
        if (columnNames.isEmpty()) {
			columnNames.add("Barcode");
			columnNames.add("Taxon");
			columnNames.add("Collector");
			columnNames.add("Type Status");
        }
		
        loadAndPushResourceBundle("specify_plugins");
        
        title = UIRegistry.getResourceString("ItemCountsLabelPlugin");
        String tooltip = getResourceString("ItemCountsLabelTooltip");
        
        popResourceBundle();
        this.setToolTipText(tooltip);
	}
	
	public void clearItems() {
		this.removeAll();
	}
	
	/**
	 * Updates the UI component with data (JTable in a JScrollPane). Looks at
	 * each of the fragments associated with a preparation and adds the prep identifier
	 * taxon name, type status, and collector. After creating the list, adds the
	 * component to the JPanel for display.
	 *  
	 * @param preparation
	 */
	public void updateItems(Preparation preparation) {
		clearItems();	// Clear the existing list before creating and displaying a new one
		data = new Vector<Vector<String>>();
		
		for ( Fragment f : preparation.getFragments()) {
			Vector<String> v = new Vector<String>();
			v.add(f.getIdentifier());
			String taxonName = "";
			String status = "";
			String collector = "";
			for ( Determination d : f.getDeterminations()) {
				if (d.getTypeStatusName() != null && !d.getTypeStatusName().equals(""))
					status = d.getTypeStatusName();
				
				if (d.isCurrentDet()) {
				    if (d.getTaxon() != null) {
    					taxonName = d.getTaxon().getFullName();
				    }
				}
			}
			
			CollectionObject colObj = f.getCollectionObject();
            // If check added by lchan for bug 494. One of the fragments does
            // not have collectionobject reference in the database. The worst
            // thing that will happen is that the "Collector" name will be ""
            // in the loan return preparations display table.
			if (colObj != null) {
			    for ( Collector c : colObj.getCollectingEvent().getCollectors()) {
			        collector = c.getAgent().getCollectorName();
			    }
			}
			v.add(taxonName);
			v.add(collector);
			v.add(status);
			data.add(v);
		}
        table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        
        if (data.size() > 0)
        	add(scrollPane);
	}
	@Override
    public void addChangeListener(ChangeListener listener)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canCarryForward()
    {
        return false;
    }

    @Override
    public String[] getCarryForwardFields()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public JComponent getUIComponent()
    {
        return this;
    }

    @Override
    public void initialize(Properties properties, boolean isViewMode)
    {
    	
    }

    @Override
    public boolean isNotEmpty()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCellName(String cellName)
    {
        ;
    }

    @Override
    public void setParent(FormViewObj parent)
    {
        this.parent = parent;
    }

    @Override
    public void shutdown()
    {
        parent = null;
        loanPreparation = null;
    }

    @Override
    public Object getValue()
    {
        return loanPreparation;
    }

    @Override
    public void setValue(Object value, String defaultValue)
    {   
    	 boolean enable = false;
         if (value != null && value instanceof LoanPreparation)
         {
             loanPreparation = (LoanPreparation)value;
             if (loanPreparation.getPreparation() != null) {
            	 preparation = loanPreparation.getPreparation();
            	 updateItems(preparation);
             }
             
         }
         
         if (loanPreparation != null)
         {
             enable = true;
         }
         setEnabled(enable);     
    }

}