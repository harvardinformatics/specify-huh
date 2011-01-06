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

@SuppressWarnings("serial")
public class BatchReturnPreps extends JButton implements UIPluginable, GetSetValueIFace
{
    private String title = null;

    @SuppressWarnings("unused")
	private FormViewObj parent = null;
    private Loan loan;
    
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
     * 
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