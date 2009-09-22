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
package edu.ku.brc.specify.prefs;

import java.util.prefs.BackingStoreException;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.prefs.GenericPrefsPanel;
import edu.ku.brc.af.ui.forms.validation.ValCheckBox;

/**
 * @author mkelly
 *
 * @code_status Alpha
 *
 * Created Date: June 11, 2009
 *
 */
public class FpPrefs extends GenericPrefsPanel
{
    protected static final String FP_AUTOLOGIN = "fp.autologin";
    protected static final String FP_MSG_CHK_INTERVAL="fp.msgchkinterval";
    
    /**
     * 
     */
    public FpPrefs()
    {
        super();
        
        createUI();
        
        this.hContext = "PrefsFp";
    }

    /**
     * Create the UI for the panel.
     */
    protected void createUI()
    {
        createForm("Preferences", "FpPrefs");

        if (formView != null & form != null && form.getUIComponent() != null)
        {
            JCheckBox chkbx = form.getCompById("fpautologin");
            if (chkbx != null)
            {
                chkbx.setSelected(AppPreferences.getLocalPrefs().getBoolean(FP_AUTOLOGIN, false));
            }
            
            JTextField textfld = form.getCompById("fpmsgchkinterval");
            if (textfld != null)
            {
                // TODO: FP MMK where do you state the default?  Surely not here.
                textfld.setText(String.valueOf(AppPreferences.getLocalPrefs().getInt(FP_MSG_CHK_INTERVAL, 5)));
            }
            
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.prefs.GenericPrefsPanel#savePrefs()
     */
    @Override
    public void savePrefs()
    {
        if (form.getValidator() == null || form.getValidator().hasChanged())
        {
            super.savePrefs();
            
            AppPreferences localPrefs  = AppPreferences.getLocalPrefs();
            
            ValCheckBox chk = form.getCompById("fpautologin");
            localPrefs.putBoolean(FP_AUTOLOGIN, chk.isSelected());
            
            JTextField txt = form.getCompById("fpmsginterval");
            
            try
            {
                localPrefs.putInt(FP_MSG_CHK_INTERVAL, Integer.parseInt(txt.getText()));
            }
            catch (NumberFormatException e)
            {
                ;
            }

            try
            {
                localPrefs.flush();
                
            } catch (BackingStoreException ex) {}
        }
    }
}
