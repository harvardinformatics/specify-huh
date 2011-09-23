package edu.ku.brc.specifydroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 27, 2009
 *
 */
public class EditPreferences extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
