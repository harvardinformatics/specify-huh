/* Copyright (C) 2011, University of Kansas Center for Research
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
package edu.ku.brc.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import edu.ku.brc.specifydroid.R;
import static edu.ku.brc.utils.DialogHelper.*;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;

/**
 * Used to check a website for a newer version and then asks the user if they want to download it.
 * 
 * @author rods
 *
 * @code_status Beta
 *
 * Jan 22, 2011
 *
 */
public class VersionChecker
{
    protected Activity activity;
    protected String   baseURL;
    protected String   appPckName;
    
    /**
     * Constructor.
     * @param activity the current activity
     */
    public VersionChecker(final Activity activity)
    {
        super();
        this.activity = activity;
        baseURL       = "http://files.specifysoftware.org/specifydroid/";
        appPckName    = "SpecifyDroid.apk";
    }

    /**
     * Constructor.
     * @param activity the current activity
     * @param baseURL the base url of the site
     * @param appPckName the name of the 'apk' file.
     */
    public VersionChecker(final Activity activity, final String baseURL, final String appPckName)
    {
        super();
        this.activity   = activity;
        this.baseURL    = baseURL;
        this.appPckName = appPckName;
    }
    
    /**
     * 
     */
    public void checkForNewVersion()
    {
        Thread t = new Thread() {
            public void run()
            {
                checkVersion();
            }
        };
        t.start();
    }

    /**
     * Checks a web site for the current version and compares it to the app.
     */
    protected void checkVersion()
    {
        boolean isNewer = false;

        try
        {
            PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getApplicationInfo().packageName, 0);
            //int    versionCode = pi.versionCode;
            String appVersion  = pi.versionName;
            
            int     flag         = activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE;
            boolean isDebuggable = (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE; 

            Log.d("*******", flag+" "+isDebuggable+"  "+ ApplicationInfo.FLAG_DEBUGGABLE+"  "+activity.getApplicationInfo().flags);
            
            URL           url     = new URL(baseURL + "version.xml");
            URLConnection conn    = url.openConnection();
            
            BufferedReader bufRead = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String         line    = "";
            while ((line = bufRead.readLine()) != null)
            {
                if (line != null && line.length() > 0)
                {
                    isNewer = line.compareTo(appVersion) > 0;
                }
            }
            
        } catch (Exception e)
        {
            Log.e("checkVersion", "Error", e);
        }
        
        if (isNewer)
        {
            activity.runOnUiThread(new Runnable() {
                public void run() 
                {
                    DialogInterface.OnClickListener dwnLdAction = new DialogInterface.OnClickListener()
                    {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
                        {
                            downloadNewVersion();
                        }
                    };
                    
                    showDialog(activity, DlgType.yesno, R.string.askdownloadapk, dwnLdAction, null);
                }
            });
        }
    }

    /**
     * Asks the browser to download the new version.
     */
    public void downloadNewVersion()
    {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW,  Uri.parse(baseURL + appPckName));
        activity.startActivity(viewIntent); 
    }
    
    /**
     * Check the context's ApplicationInfo flag to see if it is in debug mode.
     * @param context the context
     * @return true if in debug
     */
    public static boolean isInDebugMode(final Context context)
    {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE; 
    }
}
