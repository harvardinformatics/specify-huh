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
import edu.ku.brc.af.ui.forms.validation.ValFormattedTextFieldSingle;
import edu.ku.brc.af.ui.forms.validation.ValTextField;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.UIRegistry;

public class FeetToMeters extends JButton implements UIPluginable, GetSetValueIFace
{
    private static final double C = 0.3048;

    private String title = null;
    private String minElev = "minElevation";
    private String maxElev = "maxElevation";

    private FormViewObj parent = null;
    private Locality locality = null;

    public FeetToMeters()
    {
        loadAndPushResourceBundle("specify_plugins");
        
        title = UIRegistry.getResourceString("FeetToMetersPlugin");
        String tooltip = String.format(getResourceString("FeetToMetersTooltip"), new Object[] { C } );
        
        popResourceBundle();
        
        setIcon(IconManager.getIcon("FeetToMeters", IconManager.IconSize.Std16));
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
        if (minElev != null || maxElev != null)
        {
            if (minElev != null)
            {
                ValFormattedTextFieldSingle txt = parent.getCompById(minElev);
                if (txt != null)
                {
                    String feet = (String)txt.getValue();
                    if (feet != null)
                    {
                        String meters = feetToMeters(feet);
                        txt.setValue(meters, null);
                    }
                }
            }
            if (maxElev != null)
            {
                ValFormattedTextFieldSingle txt = parent.getCompById(maxElev);
                if (txt != null)
                {
                    String feet = (String)txt.getValue();
                    if (feet != null)
                    {
                        String meters = feetToMeters(feet);
                        txt.setValue(meters, null);
                    }
                }
            }
        }
    }

    private String feetToMeters(String feet)
    {
        try
        {
            Float f = Float.parseFloat(feet);
            
            return String.valueOf(f * C);
        }
        catch (NumberFormatException e)
        {
            return "";
        }
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
        minElev = properties.getProperty("minElev");
        maxElev  = properties.getProperty("maxElev");
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
        locality = null;
    }

    @Override
    public Object getValue()
    {
        return locality;
    }

    @Override
    public void setValue(Object value, String defaultValue)
    {   
        boolean enable = false;
        if (value != null && value instanceof Locality)
        {
            locality = (Locality)value;
            
        }
        
        if (locality != null)
        {
            enable = true;
        }
        setEnabled(enable);
    }

}
