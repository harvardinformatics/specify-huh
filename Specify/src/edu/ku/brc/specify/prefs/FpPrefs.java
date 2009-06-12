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

import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.prefs.GenericPrefsPanel;
import edu.ku.brc.af.ui.forms.validation.ValCheckBox;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Created Date: Sep 24, 2008
 *
 */
public class FpPrefs extends GenericPrefsPanel
{
    protected static final String FP_AUTOLOGIN = "fp.autologin";
    
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
            localPrefs.putBoolean(FP_AUTOLOGIN, (Boolean)chk.getValue());
            
            try
            {
                localPrefs.flush();
                
            } catch (BackingStoreException ex) {}
        }
    }
}
