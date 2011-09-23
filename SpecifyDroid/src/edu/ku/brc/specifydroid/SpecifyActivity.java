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
package edu.ku.brc.specifydroid;

import java.io.File;
import java.util.HashMap;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import edu.ku.brc.utils.VersionChecker;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 27, 2009
 *
 */
public class SpecifyActivity extends SpBaseActivity
{
    private static final int SOUND_EXPLOSION = 1;
    private SoundPool                 soundPool;
    private HashMap<Integer, Integer> soundPoolMap;

    public  static final String   TAXA_FILE_PREF    = "TAXA_FILE_PREF";
    private static final String   HAS_NEW_VER       = "HAS_NEW_VER";
    
    private boolean hasAskedForNewVersion = false;
    
    /**
     * 
     */
    public SpecifyActivity()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null)
        {
            hasAskedForNewVersion = savedInstanceState.getBoolean(HAS_NEW_VER, false);
        }
        
        /*
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        String str = metrics.heightPixels+", "+metrics.widthPixels+"  d:"+metrics.density+
        "  Dpi:"+metrics.densityDpi+"  sc:"+metrics.scaledDensity+
        "  xdpi:"+metrics.xdpi+"  ydpi:"+metrics.ydpi;
        Log.i("XXX", str);
        */
        
        SQLiteDatabase database = getDB();
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.specify_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        ((TextView)findViewById(R.id.headersep)).setText("");

        //this.setTitleColor(Color.parseColor("#53a1e5"))); // Sets the Text Color of the title
        
        ImageView btn = (ImageView)findViewById(R.id.tripsbtn);
        btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(SpecifyActivity.this, TripListActivity.class);
                intent.putExtra(TripListActivity.TRIP_TYPE, TripListActivity.CONFIG_TRIP);
                startActivity(intent);
            }
        });
        
        btn = (ImageView)findViewById(R.id.collectingbtn);
        btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(SpecifyActivity.this, TripListActivity.class);
                intent.putExtra(TripListActivity.TRIP_TYPE, TripListActivity.COLL_TRIP);
                intent.putExtra(TripListActivity.DETAIL_CLASS, TripDataEntryDetailActivity.class.getName());
                //intent.putExtra(TripListActivity.TRIP_TITLE, null); // do not send title causing it to be null
                startActivity(intent);
            }
        });
        
        btn = (ImageView)findViewById(R.id.observationsbtn);
        btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(SpecifyActivity.this, TripListActivity.class);
                intent.putExtra(TripListActivity.TRIP_TYPE, TripListActivity.OBS_TRIP);
                intent.putExtra(TripListActivity.DETAIL_CLASS, TripDataEntryDetailActivity.class.getName());
                //intent.putExtra(TripListActivity.TRIP_TITLE, null); // do not send title causing it to be null
                startActivity(intent);
            }
        });
        
        btn = (ImageView)findViewById(R.id.satellitebtn);
        btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                startActivity(new Intent(SpecifyActivity.this, SatelliteActivity.class));
            }
        });
        
        File  root          = Environment.getExternalStorageDirectory();
        final File taxaFile = new File(root, "fish_taxa.csv");
        long  fileTime      = taxaFile.lastModified();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        long prefFileTime = prefs.getLong(TAXA_FILE_PREF, -1);
        
        if (prefFileTime == fileTime)
        {
            return;
        }

        if (taxaFile.exists())
        {
            final ProgressDialog prgDlg = new ProgressDialog( this );
            prgDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            prgDlg.setMessage(getString(R.string.loading));
            prgDlg.setCancelable(false);

            prgDlg.show();
            
            prgDlg.setMax((int)taxaFile.length());
            prgDlg.setProgress(0); 
            
            if (TaxonLoadThread.getInstance() == null)
            {
                TaxonLoadThread tlt = new TaxonLoadThread(database, taxaFile);
                tlt.set(this, database, prgDlg);
                tlt.start();
                TaxonLoadThread.setInstance(tlt);
                
            } else
            {
                TaxonLoadThread.getInstance().set(this, database, prgDlg);
            }
        }
        
        if (!hasAskedForNewVersion)
        {
            hasAskedForNewVersion = true;
            VersionChecker versionChecker = new VersionChecker(this);
            versionChecker.checkForNewVersion();
        }
        
        initSounds();
        
        if (prefs.getBoolean("useStartupSound", true))
        {
            Thread t = new Thread() {
                public void run() {
                    try
                    {
                        Thread.sleep(2000);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    //playSound(SOUND_EXPLOSION);
                }
            };
            t.start();
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        
        if (TaxonLoadThread.getInstance() != null && 
            TaxonLoadThread.getInstance().getPrgDlg() != null)
        {
            TaxonLoadThread.getInstance().getPrgDlg().dismiss();
            TaxonLoadThread.getInstance().set(null, null, null);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(HAS_NEW_VER, hasAskedForNewVersion);
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        new MenuInflater(getApplication()).inflate(R.menu.main_menu, menu);

        return (super.onCreateOptionsMenu(menu));
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.about_mi)
        {
            showDialog(0);
            return true;
            
        }  else if (item.getItemId() == R.id.addtestdata_mi)
        {
            TripSQLiteHelper.loadTestData(getDB());
            return true;
            
        } else if (item.getItemId() == R.id.prefs)
        {
            startActivity(new Intent(this, EditPreferences.class));
            return true;
            
        } 
        return super.onOptionsItemSelected(item);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.aboutdlg_layout);
        dialog.setTitle(R.string.about_title);

        TextView text = (TextView) dialog.findViewById(R.id.about_text);
        String aboutText = "";
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0);
            aboutText = getAboutText("SpecifyDroid", pi.versionName);
            
        } catch (NameNotFoundException e)
        {
            e.printStackTrace();
        }
        text.setText(Html.fromHtml(aboutText));
        text.setMovementMethod(LinkMovementMethod.getInstance());
        
        Button closeBtn = (Button)dialog.findViewById(R.id.aboutclose_btn);
        closeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });
        
        return dialog;
    }
    
    private static String getAboutText(final String appNameArg, 
                                      final String appVersionArg)
    {
        return "<html><font face=\"sans-serif\" size=\"11pt\">"+appNameArg+" " + appVersionArg +  //$NON-NLS-1$ //$NON-NLS-2$
        "<br><br>Specify Software Project<br>" +//$NON-NLS-1$
        "Biodiversity Institute<br>University of Kansas<br>1345 Jayhawk Blvd.<br>Lawrence, KS  USA 66045<br><br>" +  //$NON-NLS-1$
        "<a href=\"http://www.specifysoftware .org\">www.specifysoftware.org</a>"+ //$NON-NLS-1$
        "<br><a href=\"mailto:specify@ku.edu\">specify@ku.edu</a><br>" +  //$NON-NLS-1$
        "<p>The Specify Software Project is "+ //$NON-NLS-1$
        "funded by the Advances in Biological Informatics Program, " + //$NON-NLS-1$
        "U.S. National Science Foundation  (Award DBI-0960913 and earlier awards).<br><br>" + //$NON-NLS-1$
        appNameArg + " Copyright \u00A9 2011 University of Kansas Center for Research. " + 
        "Specify comes with ABSOLUTELY NO WARRANTY.<br><br>" + //$NON-NLS-1$
        "This is free software licensed under GNU General Public License 2 (GPL2).</P></font></html>"; //$NON-NLS-1$
    }
    

    /**
     * 
     */
    private void initSounds()
    {
        try
        {
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
            soundPoolMap = new HashMap<Integer, Integer>();
            soundPoolMap.put(SOUND_EXPLOSION, soundPool.load(this, R.raw.specifydroid, 1));
            
        } catch (Exception ex)
        {
            //Log.e("XXX", "No Sound", ex);
        }
    }
         
              
    /**
     * @param sound
     */
    public void playSound(int sound)
    {
        /* Updated: The next 4 lines calculate the current volume in a scale of 0.0 to 1.0 */
        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        //float streamVolumeMax     = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //float volume = 15.0F;// streamVolumeCurrent / streamVolumeMax;

        /* Play the sound with the correct volume */
        soundPool.play(soundPoolMap.get(sound), streamVolumeCurrent, streamVolumeCurrent, 1, 0, 1f);
    }
}
