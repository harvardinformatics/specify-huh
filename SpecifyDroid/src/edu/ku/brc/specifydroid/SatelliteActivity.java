package edu.ku.brc.specifydroid;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 28, 2009
 *
 */
public class SatelliteActivity extends Activity
{
    private EditText          status = null;
    private SharedPreferences prefs  = null;
    private LocationManager   locMgr = null;
    
    private HashMap<Integer, GpsSatellite> satHash = new HashMap<Integer, GpsSatellite>();

    private SatellitesView satView = null;
    
    /* (non-Javadoc)
     * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.sat_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        ((TextView)findViewById(R.id.headertitle)).setText(R.string.satellites);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        satView = (SatellitesView)findViewById(R.id.satellites);
        satView.init(new int[] {R.drawable.satellite48});
        satView.setBackgroundColor(Color.BLACK);

        locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10.0f, onLocationChange);

        // Listener for GPS Status...

        final Listener onGpsStatusChange = new GpsStatus.Listener()
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
                        doStatelliteStatus();
                        break;
                }
            }
        };
        locMgr.addGpsStatusListener(onGpsStatusChange);
        
        checkForGPS(this);
    }
    
    /**
     * @param activity
     */
    public static boolean checkForGPS(final Activity activity)
    {
        final LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            buildAlertMessageNoGps(activity);
            return false;
        }
        return true;
    }
    
    /**
     * @param activity
     */
    private static void buildAlertMessageNoGps(final Activity activity)
    {
        String yes = activity.getString(R.string.yes);
        String no  = activity.getString(R.string.no);
        
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.no_gps)
                .setCancelable(false).setPositiveButton(yes,
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
                                                @SuppressWarnings("unused") final int id)
                            {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                activity.startActivityForResult(intent, 101);
                            }
                        }).setNegativeButton(no, new DialogInterface.OnClickListener()
                {
                    public void onClick(final DialogInterface dialog,
                                        @SuppressWarnings("unused") final int id)
                    {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 
     */
    private void doStatelliteStatus()
    {
        satView.startSatUpdate();
        
        GpsStatus xGpsStatus = locMgr.getGpsStatus(null);
        Iterable<GpsSatellite> iSatellites = xGpsStatus.getSatellites();
        Iterator<GpsSatellite> it = iSatellites.iterator();
        while (it.hasNext())
        {
            GpsSatellite sat = (GpsSatellite) it.next();
            if (satHash.get(sat.getPrn()) == null)
            {
                satView.updateSatellite(sat);
            }
            //Log.v("TEST", "LocationActivity - onGpsStatusChange: Satellites: " + sat.getPrn() + ", " + sat.getSnr()+", "+sat.getAzimuth());
            //satView.updateView();
        }
        satView.endSatUpdate();
    }

    /* (non-Javadoc)
     * @see android.app.ActivityGroup#onDestroy()
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();

        locMgr.removeUpdates(onLocationChange);

    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        new MenuInflater(getApplication()).inflate(R.menu.option, menu);

        return (super.onCreateOptionsMenu(menu));
    }*/

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.prefs)
        {
            startActivity(new Intent(this, EditPreferences.class));
            return (true);
            
        } else if (item.getItemId() == R.id.bff)
        {
            return (true);
            
        } else if (item.getItemId() == R.id.location)
        {
            insertLocation();
            return (true);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 
     */
    private void insertLocation()
    {
        Location loc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (loc == null)
        {
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
        } else
        {
            StringBuffer buf = new StringBuffer(status.getText().toString());

            buf.append(" L:");
            buf.append(String.valueOf(loc.getLatitude()));
            buf.append(",");
            buf.append(String.valueOf(loc.getLongitude()));

            status.setText(buf.toString());
        }
    }
	
	private SharedPreferences.OnSharedPreferenceChangeListener prefListener=
		new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
			if (key.equals("user") || key.equals("password")) {
			}
		}
	};
	
	LocationListener onLocationChange=new LocationListener() {
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