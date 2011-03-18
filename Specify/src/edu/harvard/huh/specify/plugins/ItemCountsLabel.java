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

import static edu.ku.brc.ui.UIHelper.createLabel;
import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static edu.ku.brc.ui.UIRegistry.loadAndPushResourceBundle;
import static edu.ku.brc.ui.UIRegistry.popResourceBundle;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.ResultSetController;
import edu.ku.brc.af.ui.forms.ResultSetControllerListener;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.af.ui.forms.validation.ValFormattedTextFieldSingle;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.InfoRequest;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.tasks.InteractionsProcessor;
import edu.ku.brc.specify.tasks.InteractionsTask;
import edu.ku.brc.specify.ui.ColObjInfo;
import edu.ku.brc.specify.ui.PrepInfo;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.VerticalSeparator;

/**
 * plugin class for the Item counts labels displayed on the loan form. Extends a JPanel object
 * which contains multiple JLabels that display counts.
 * @author lowery
 *
 */
@SuppressWarnings("serial")
public class ItemCountsLabel extends JPanel implements UIPluginable, GetSetValueIFace
{
    private String title = null;

    private FormViewObj parent = null;
    
    private Loan loan;
    
    private final int TOTAL = 0;
    private final int RETURNED = 1;
    private final int OUTSTANDING = 2;
    
    private JLabel[] itemCountLabels = {new JLabel(), new JLabel(), new JLabel()};
    private JLabel[] typeCountLabels = {new JLabel(), new JLabel(), new JLabel()};
    private JLabel[] nonSpecimenCountLabels = {new JLabel(), new JLabel(), new JLabel()};
    
    private JLabel totalTotalLabel = new JLabel();
    private JLabel returnedTotalLabel = new JLabel();
    private JLabel outstandingTotalLabel = new JLabel();
    
    /**
     * Constructor method for plugin sets title and tooltip text and initializes the
     * form layout and all the labels that will be used to display counts.
     */
    public ItemCountsLabel()
    {	
    	
        loadAndPushResourceBundle("specify_plugins");
        
        title = UIRegistry.getResourceString("ItemCountsLabelPlugin");
        String tooltip = getResourceString("ItemCountsLabelTooltip");
        
        popResourceBundle();
        
        Font f = this.getFont();
        this.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        
        //setText("4 Items, 2 Returned, 2 Outstanding");
        this.setToolTipText(tooltip);
        
		FormLayout fl = new FormLayout("p,20px,p,10px,p,10px,p",
				                       "p,p,p,p,p,5px,p,p");
				                       
        PanelBuilder    pbuilder = new PanelBuilder(fl, this);
        CellConstraints cc       = new CellConstraints();
        
        pbuilder.add(createLabel("Total"), cc.xy(3,1));
        pbuilder.add(createLabel("Returned"), cc.xy(5,1));
        pbuilder.add(createLabel("Outstanding"), cc.xy(7,1));
        
        pbuilder.add(createLabel("Items:"), cc.xy(1,2));
        pbuilder.add(itemCountLabels[TOTAL], cc.xy(3,2, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(itemCountLabels[RETURNED], cc.xy(5,2, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(itemCountLabels[OUTSTANDING], cc.xy(7,2, CellConstraints.CENTER, CellConstraints.BOTTOM));
        
        pbuilder.add(createLabel("Type:"), cc.xy(1,3));
        pbuilder.add(typeCountLabels[TOTAL], cc.xy(3,3, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(typeCountLabels[RETURNED], cc.xy(5,3, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(typeCountLabels[OUTSTANDING], cc.xy(7,3, CellConstraints.CENTER, CellConstraints.BOTTOM));
        
        pbuilder.add(createLabel("Non Specimen:"), cc.xy(1,4));
        pbuilder.add(nonSpecimenCountLabels[TOTAL], cc.xy(3,4, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(nonSpecimenCountLabels[RETURNED], cc.xy(5,4, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(nonSpecimenCountLabels[OUTSTANDING], cc.xy(7,4, CellConstraints.CENTER, CellConstraints.BOTTOM));
        
        pbuilder.add(new JSeparator(), cc.xyw(1,5,7));
        
        pbuilder.add(createLabel("Grand Total:"), cc.xy(1,7));
        pbuilder.add(totalTotalLabel, cc.xy(3,7, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(returnedTotalLabel, cc.xy(5,7, CellConstraints.CENTER, CellConstraints.BOTTOM));
        pbuilder.add(outstandingTotalLabel, cc.xy(7,7, CellConstraints.CENTER, CellConstraints.BOTTOM));
        
        
       // pbuilder.add(accountingBtn);
       
     
    }
    
    /**
     * This method takes a FormViewObj as a parameter and pulls the loan from it. From the loan
     * the method will get the counts needed for tallying and updating all the labels
     * @param formViewObj
     */
    public void updateLabels(int itemTotal, int typeTotal, int nonSpecimenTotal,
    		                 int itemReturned, int typeReturned, int nonSpecimenReturned) {
    	
    	int itemOutstanding = itemTotal - itemReturned;
    	int typeOutstanding = typeTotal - typeReturned;
    	int nonSpecimenOutstanding = nonSpecimenTotal - nonSpecimenReturned;
    	
        	itemCountLabels[TOTAL].setText(Integer.toString(itemTotal));
        	itemCountLabels[RETURNED].setText(Integer.toString(itemReturned));
        	itemCountLabels[OUTSTANDING].setText(Integer.toString(itemOutstanding));
        	
        	typeCountLabels[TOTAL].setText(Integer.toString(typeTotal));
        	typeCountLabels[RETURNED].setText(Integer.toString(typeReturned));
        	typeCountLabels[OUTSTANDING].setText(Integer.toString(typeOutstanding));
        	
        	nonSpecimenCountLabels[TOTAL].setText(Integer.toString(nonSpecimenTotal));
        	nonSpecimenCountLabels[RETURNED].setText(Integer.toString(nonSpecimenReturned));
        	nonSpecimenCountLabels[OUTSTANDING].setText(Integer.toString(nonSpecimenOutstanding));
        	
        	totalTotalLabel.setText(Integer.toString(itemTotal + typeTotal + nonSpecimenTotal));
        	returnedTotalLabel.setText(Integer.toString(itemReturned + typeReturned + nonSpecimenReturned));
        	outstandingTotalLabel.setText(Integer.toString(itemOutstanding + typeOutstanding + nonSpecimenOutstanding));
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
        loan = null;
    }

    @Override
    public Object getValue()
    {
        return loan;
    }

    @Override
    public void setValue(Object value, String defaultValue)
    {   
        boolean enable = true;
        setEnabled(enable);
    }

}