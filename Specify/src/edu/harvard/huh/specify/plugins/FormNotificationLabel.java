/*
 * Created on 2012 January 17
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

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeListener;

import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.ui.GetSetValueIFace;

/** Very simple plugin that extends a JLabel and can be used on forms to display
 * error and message text.
 * 
 * @author lowery
 *
 */
@SuppressWarnings("serial")
public class FormNotificationLabel extends JLabel implements UIPluginable, GetSetValueIFace
{
    private FormViewObj parent = null;
    public static final int ERROR = 1;
    public static final int MESSAGE = 0;
    
    /** 
     * Default constructor will set visible to false as a default
     * 
     */
    public FormNotificationLabel() {
    	setVisible(false);
    }

    /** Displays the text supplied as an argument. The type of the
     * notification determines the visual display of the string
     * (such as color).
     * 
     * @param text
     * @param type
     */
    public void display(String text, int type) {
    	setText(text);

    	switch (type) {
    	case ERROR: 
    		setForeground(Color.RED);
    	case MESSAGE:
        	setForeground(Color.BLACK);
    	}
    	
    	setVisible(true);
    }
    
    /**
     * Clears the component and sets visible to false
     */
    public void clear() {
    	// defaults
    	setText("");
    	setForeground(Color.BLACK);
    	setVisible(false);
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
        return null;
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
    }

    @Override
    public Object getValue()
    {
        return null;
    }

    @Override
    public void setValue(Object value, String defaultValue)
    {   
    	
    }

}