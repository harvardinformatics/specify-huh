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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.tasks.InteractionsTask;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.UIRegistry;

/**
 * This class is a plugin implemented as a JButton component for the Batch return preparations
 * functionality on the loan form for performing multiple returns at once.
 */
@SuppressWarnings("serial")
public class BatchReturnPreps extends JButton implements UIPluginable, GetSetValueIFace
{
    private String title = null;

    @SuppressWarnings("unused")
	private FormViewObj parent = null;
    private Loan loan;
    
    /** Constructor method for plugin sets the title and tooltip text and adds an 
     * action listener that will call doButtonAction() when the button is clicked.
     */
    public BatchReturnPreps()
    {
    	
        loadAndPushResourceBundle("specify_plugins");
        
        title = UIRegistry.getResourceString("BatchReturnPrepsPlugin");
        String tooltip = getResourceString("BatchReturnPrepsTooltip");
        
        popResourceBundle();
        
        //setIcon(IconManager.getIcon("BatchReturnPreps", IconManager.IconSize.Std16));
        setText(title);
        this.setToolTipText(tooltip);
        
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0)
            {
                doButtonAction();
                
            }
        });
    }
    /**
     * Creates a new InteractionsTask object and passes a commandAction for ReturnLoan to the task's
     * doCommand() method.
     */
    protected void doButtonAction()
    {
    	InteractionsTask task = new InteractionsTask();
    	CommandAction cmdAction = new CommandAction(InteractionsTask.INTERACTIONS, "ReturnLoan");
    	task.doCommand(cmdAction);
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
        setEnabled(true);
    }
}