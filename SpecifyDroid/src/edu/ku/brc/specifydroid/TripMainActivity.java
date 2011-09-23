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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.GridView;
import android.widget.TextView;
import edu.ku.brc.utils.DialogHelper;
import edu.ku.brc.utils.SQLUtils;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 15, 2009
 *
 */
public class TripMainActivity extends SpBaseActivity implements TripSQLiteHelper.CSVExportIFace
{
    private static final String ID_PNTWASCPT = "ID_PNTWASCPT";
    
    private String    tripId;
    private String    tripTitle;
    private TextView  titleView;
    private int       itemCount = 0;
    
    //private GpsStatus            gpsStatus = null;
    private LocationManager      locMgr           = null;
    private Location             loc              = null;
    private AtomicBoolean        pointWasCaptured = new AtomicBoolean(false);
    private AtomicLong           milliseconds     = new AtomicLong(0); 
    private TripMainPanelAdapter adapter;
    private ProgressDialog       prgDlg           = null;
    private Listener             onGpsStatusChange = null;
    private int                  tripType;

    /**
     * 
     */
    public TripMainActivity()
    {
        super();
    }

    /* (non-Javadoc)
     * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null)
        {
            tripId           = savedInstanceState.getString(TripListActivity.ID_EXTRA);
            pointWasCaptured = new AtomicBoolean(savedInstanceState.getBoolean(ID_PNTWASCPT));
            tripType         = savedInstanceState.getInt(TripListActivity.TRIP_TYPE, TripListActivity.CONFIG_TRIP);
            
        } else
        {
            tripId           = getIntent().getStringExtra(TripListActivity.ID_EXTRA);
            pointWasCaptured = new AtomicBoolean(getIntent().getBooleanExtra(ID_PNTWASCPT, false));
            tripType         = getIntent().getIntExtra(TripListActivity.TRIP_TYPE, TripListActivity.CONFIG_TRIP);
        }

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.tripmain);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);

        titleView = (TextView)findViewById(R.id.headertitle);

        adapter = new TripMainPanelAdapter(this, tripId, tripType, tripTitle);
        GridView gridview = (GridView)findViewById(R.id.tripgridview);
        gridview.setAdapter(adapter);

        updateTitle(); // sets the title with all the data
        
     }
    
    /**
     * @param context
     * @param tripType
     * @return
     */
    public static String createTitle(final Context context, final int tripType)
    {
        int resId;
        switch (tripType)
        {
            case TripListActivity.COLL_TRIP:
                resId = R.string.newcollsortie;
                break;
                
            case TripListActivity.OBS_TRIP:
                resId = R.string.newobssortie;
                break;
                
             default:
                 resId = R.string.newsortie;
                 break;
        }
        return context.getString(resId);
    }
    
    /**
     * @return the itemCount
     */
    public int getItemCount()
    {
        return itemCount;
    }

    /**
     * 
     */
    public void doEmailExport(final File file)
    {
        try
        {
            String path = "file://"+Environment.getExternalStorageDirectory()+"/" + file.getName();
            
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("text/csv");
            //emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ "rods@ku.edu"});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, file.getName());
            //emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Your Export");
            //emailIntent.putExtra(Intent.EXTRA_STREAM, file.toURI());// Uri.parse("file://sdcard/dcim/Camera/filename.jpg"));
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path)); 

            String msg = getString(R.string.sendingemail);
            startActivityForResult(Intent.createChooser(emailIntent, msg), 0);
            
        } catch (Exception ex)
        {
            DialogHelper.showDialog(this, "Mail could not be started.");
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putString(TripListActivity.ID_EXTRA, tripId);
        outState.putInt(TripListActivity.TRIP_TYPE, tripType);
        outState.putBoolean(ID_PNTWASCPT, pointWasCaptured.get());
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        
        resetLocationManager();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        updateTitle();
        
        resetLocationManager();
    }

    /**
     * 
     */
    private void resetLocationManager()
    {
        if (prgDlg != null)
        {
            prgDlg.dismiss();
            prgDlg = null;
        }
        
        //if (pointWasCaptured.get())
        //{
            if (locMgr != null)
            {
                if (onLocationChange != null)
                {
                    locMgr.removeUpdates(onLocationChange);
                }
                
                if (onGpsStatusChange != null)
                {
                    locMgr.removeGpsStatusListener(onGpsStatusChange);
                }
            }
            loc = null;
            locMgr = null;
        //}
    }

    /**
     * 
     */
    public void updateTitle()
    {
        if (tripId != null)
        {
            String sql = String.format("select COUNT(*) AS count FROM (select TripRowIndex from tripdatacell where TripID = %s GROUP BY TripRowIndex)", tripId);
            itemCount = SQLUtils.getCount(getDB(), sql);
            
            sql = String.format("SELECT Name FROM trip WHERE _id = %s", tripId);
            String titleStr = SQLUtils.getStringObj(getDB(), sql);
            if (titleView != null)
            {
                tripTitle = String.format("%s: %s %s", getString(R.string.trip), titleStr, getString(R.string.tmgtitleitems, itemCount));
                titleView.setText(tripTitle);
            }
            
            if (adapter != null)
            {
                adapter.setEnabled(itemCount != 0);
                adapter.setTripTitle(tripTitle);
            }
        }
    }
    
    /**
     * 
     */
    protected void addLatLon()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getBoolean("NotUseGPS", false))
        {
            showMarkLocActivity(null, null);
            return;
        }
        
        if (SatelliteActivity.checkForGPS(this))
        {
            if (locMgr == null)
            {
                prgDlg = ProgressDialog.show(this, null, getString(R.string.srch_gps), true);
                prgDlg.show();
                
                milliseconds.set(System.currentTimeMillis());
                
                locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                
                // Listener for GPS Status...
    
                if (onGpsStatusChange == null)
                {
                    onGpsStatusChange = new GpsStatus.Listener()
                    {
                        public void onGpsStatusChanged(int event)
                        {
                            switch (event)
                            {
                                case GpsStatus.GPS_EVENT_STARTED:
                                    //Log.v("TEST", "GPS_EVENT_STARTED");
                                    // Started...
                                    break;
        
                                case GpsStatus.GPS_EVENT_FIRST_FIX:
                                    //Log.v("TEST", "GPS_EVENT_FIRST_FIX");
                                    // First Fix...
                                    break;
        
                                case GpsStatus.GPS_EVENT_STOPPED:
                                    //Log.v("TEST", "GPS_EVENT_STOPPED");
                                    // Stopped...
                                    break;
        
                                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                                    //Log.v("TEST", "GPS_EVENT_SATELLITE_STATUS");
                                    doCapturePoint();
                                    break;
                            }
                        }
                    };
                }
                locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10.0f, onLocationChange);
                locMgr.addGpsStatusListener(onGpsStatusChange);
            }
        }
    }
    
    /**
     * Start the Activity for entering a record with lat, lon.
     * @param lat latitude (can be null)
     * @param lon longitude (can be null)
     */
    private void showMarkLocActivity(final Double lat, final Double lon)
    {
        Intent intent = new Intent(this, TripDataEntryDetailActivity.class);
        intent.putExtra(TripListActivity.ID_EXTRA, tripId);
        intent.putExtra(TripListActivity.TRIP_TYPE, tripType);
        
        //Log.i("DBG", String.format("%8.5f, %8.5f", loc != null ? loc.getLatitude() : 0.0f, loc != null ? loc.getLongitude() : 0.0f));
        
        intent.putExtra(TripDataEntryDetailActivity.ID_ISCREATE, true);
        if (lat != null && lon != null)
        {
            intent.putExtra(TripDataEntryDetailActivity.LAT_VAL, lat);
            intent.putExtra(TripDataEntryDetailActivity.LON_VAL, lon);
        }
        
        startActivity(intent);
    }
    
    /**
     * 
     */
    private void doCapturePoint()
    {
        if (loc == null)
        {
            if (locMgr != null && locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                loc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            
            if (loc != null)
            {
                pointWasCaptured.set(true);

                showMarkLocActivity(loc != null ? loc.getLatitude()  :  38.958654,
                                    loc != null ? loc.getLongitude() : -95.243829);
    
                resetLocationManager();
                
            } else if (!pointWasCaptured.get())
            {
                long delta = (System.currentTimeMillis() - milliseconds.get()) / 1000;
                if (delta > 15)
                {
                    resetLocationManager();
                    DialogHelper.showDialog(this, "Timed out.");
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specifydroid.TripSQLiteHelper.CSVExportIFace#done(java.io.File)
     */
    @Override
    public void done(final File file)
    {
        doEmailExport(file);
    }

    LocationListener onLocationChange = new LocationListener() {
        public void onLocationChanged(Location location) 
        {
            // required for interface, not used
            //Log.v("TEST", "onLocationChanged");
        }
        
        public void onProviderDisabled(String provider) 
        {
            // required for interface, not used
            //Log.v("TEST", "onProviderDisabled");
        }
        
        public void onProviderEnabled(String provider) 
        {
            // required for interface, not used
            //Log.v("TEST", "onProviderEnabled");
        }
        
        public void onStatusChanged(String provider, int status, Bundle extras) 
        {
            // required for interface, not used
            //Log.v("TEST", "onStatusChanged");
        }
    };

}
