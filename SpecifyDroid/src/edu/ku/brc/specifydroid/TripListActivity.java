/*
 * Copyright (C) 2011, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute, 1345 Jayhawk Boulevard,
 * Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package edu.ku.brc.specifydroid;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import edu.ku.brc.specifydroid.datamodel.Trip;
import edu.ku.brc.utils.SQLUtils;
/**
 * @author rods
 * 
 * @code_status Beta
 * 
 * Oct 27, 2009
 * 
 */
public class TripListActivity extends SpBaseActivity
{
    //private static final String  TAG               = "TripListActivity";
    
    public final static int CONFIG_TRIP = 0; // Configure Trip
    public final static int COLL_TRIP   = 1; // Collection Trip
    public final static int OBS_TRIP    = 2; // Observation
    
    public final static String         ID_EXTRA     = "edu.ku.brc.specifydroid._ID";
    public final static String         TRIP_TITLE   = "edu.ku.brc.specifydroid.TRIP_TITLE";
    public final static String         TRIP_TYPE    = "edu.ku.brc.specifydroid.TRIP_TYPE";
    public final static String         DETAIL_CLASS = "edu.ku.brc.specifydroid.DETAIL_CLASS";
    
    private AtomicBoolean              isActive    = new AtomicBoolean(true);
    private SharedPreferences          prefs       = null;
    private ListView                   list        = null;
    private TextView                   emptyTV     = null;
    private LinearLayout               emptyLL     = null;
    
    private Integer                    tripType      = CONFIG_TRIP; // Default Trip type
    private AtomicBoolean              isLoading     = new AtomicBoolean(false);
    
    /**
     * 
     */
    public TripListActivity()
    {
        super();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null)
        {
            tripType = savedInstanceState.getInt(TRIP_TYPE, CONFIG_TRIP);
        } else
        {
            tripType = getIntent().getIntExtra(TRIP_TYPE, CONFIG_TRIP);
        }
        
        int titleId = 0;
        switch (tripType)
        {
            case COLL_TRIP : 
                titleId = R.string.collecting;
                break;
            case OBS_TRIP : 
                titleId = R.string.observations;
                break;
            default:
                titleId = R.string.trips;
                break;
        }
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.trip_list_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        ((TextView)findViewById(R.id.headertitle)).setText(titleId);
        

        emptyTV = (TextView) findViewById(R.id.listemptyid_tv);
        emptyLL = (LinearLayout) findViewById(R.id.listemptyid_ll);
        
        list  = (ListView) findViewById(R.id.trips);
        //list.getBackground().setDither(true);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        list.setOnItemClickListener(createItemClickListener());

        OnCreateContextMenuListener listener = new OnCreateContextMenuListener()
        {
            @Override
            public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo)
            {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
                menu.setHeaderTitle(getNameFromCursor(list.getItemAtPosition(info.position)));

                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.triplistcontextmenu, menu);
            }
        };
        list.setOnCreateContextMenuListener(listener);

        initList();

        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(final MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int index = info.position;
        
        if (item.getItemId() == R.id.dellistitem)
        {
            askForDeleteTrip(index);
            
        } else if (item.getItemId() == R.id.cfgtripitem || 
                   item.getItemId() == R.id.edttripitem)
        {
            Cursor cursor   = (Cursor)list.getItemAtPosition(index);
            String trpId    = cursor.getString(0);
            int    trpType  = cursor.getInt(cursor.getColumnIndex("Type"));
            
            String trpTitle = cursor.getString(cursor.getColumnIndex("Name")) + " " + getString(R.string.trip);
            Intent intent = new Intent(this, item.getItemId() == R.id.cfgtripitem ? TripMainActivity.class : TripDetailActivity.class);
            intent.putExtra(ID_EXTRA, trpId);
            intent.putExtra(TRIP_TYPE, trpType);
            intent.putExtra(TRIP_TITLE, trpTitle);
            startActivity(intent);
        }
        return true;
    }
    
    /**
     * @param cursor
     * @return
     */
    private String getNameFromCursor(final Object obj)
    {
        String tripName = "Unknown";
        if (obj instanceof Cursor)
        {
            tripName = ((Cursor)obj).getString(1);
        }
        return tripName;
    }
    
    /**
     * @param index
     */
    protected void askForDeleteTrip(final int index)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this).setPositiveButton(
                R.string.alert_dialog_ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        doDeleteItem(index);
                    }
                }).setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        
                    }
                });

        builder.setTitle(String.format(getString(R.string.deletetrip), getNameFromCursor(list.getItemAtPosition(index))));
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    /**
     * @param index
     */
    private void doDeleteItem(final int index)
    {
        String trpId = ((Cursor)list.getItemAtPosition(index)).getString(0); 
        closeCursor();
        
        Trip.doDeleteTrip(getDB(), trpId);
        
        initList();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause()
    {
        super.onPause();

        isActive.set(false);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        
        if (cursorModel == null)
        {
            initList();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        
        if (cursorModel == null)
        {
            initList();
        }

        isActive.set(true);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(TRIP_TYPE, tripType);
        savedInstanceState.putString(DETAIL_CLASS, TripMainActivity.class.getName());
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        new MenuInflater(getApplication()).inflate(R.menu.tripmenus, menu);

        return (super.onCreateOptionsMenu(menu));
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.add_trip_mi)
        {
            Intent intent = new Intent(this, TripDetailActivity.class);
            intent.putExtra(TripDetailActivity.ISNEW_EXTRA, true);
            intent.putExtra(TRIP_TYPE, tripType);
            // TRIP_TITLE will be null
            startActivity(intent);
            return true;
            
        } else if (item.getItemId() == R.id.prefs)
        {
            startActivity(new Intent(this, EditPreferences.class));
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * 
     */
    private void initList()
    {
        if (isLoading.get())
        {
            return;
        }
        
        isLoading.set(true);
        
        closeCursor();

        final ProgressDialog prgDlg = ProgressDialog.show(this, null, getString(R.string.loading), true);
        prgDlg.show();
        
        new Thread() 
        {
            public void run() 
            {
                 boolean isEmpty = false;
                 try
                 {
                     String where = null;
                     if (tripType > 0)
                     {
                         where = String.format("WHERE Type = %d", tripType);
                     }
                     
                     int cnt = SQLUtils.getCount(getDB(), "SELECT COUNT(*) as count FROM trip " + (where != null ? where : ""));
                     if (cnt < 1)
                     {
                         isEmpty = true;
                         isLoading.set(false);
                         
                     } else
                     {
                         
                         cursorModel = Trip.getAll(getDB(), "trip", where, prefs.getString("sort_order", ""));
                         
                         runOnUiThread(new Runnable()
                         {
                             public void run()
                             {
                                 if (cursorModel != null)
                                 {
                                     startManagingCursor(cursorModel);
        
                                     list.setAdapter(new DataAdapterWithBinder(new TripDataViewBinder(),
                                                                               TripListActivity.this, 
                                                                               R.layout.row, 
                                                                               cursorModel, 
                                                                               new String[] {"Name", "TripDate", "Type"}, 
                                                                               new int[] {R.id.name, R.id.date, R.id.icon}));
                                     isLoading.set(false);
                                 }
                             }
                         });
                     }
                     
                 } catch (Exception e) 
                 { 
                     e.printStackTrace();  
                 }
                 // Dismiss the Dialog
                 prgDlg.dismiss();
                 
                 final boolean isEmty = isEmpty;
                 runOnUiThread(new Runnable()
                 {
                     public void run()
                     {
                         if (isEmty)
                         {
                             emptyTV.setText(R.string.trpclcknew);
                             emptyLL.setVisibility(View.VISIBLE);
                             list.setVisibility(View.GONE);
                         } else
                         {
                             emptyLL.setVisibility(View.GONE);
                             list.setVisibility(View.VISIBLE);
                         }
                         LinearLayout mainLL  = (LinearLayout)TripListActivity.this.findViewById(R.id.mainlist_ll);
                         mainLL.requestLayout();
                         mainLL.invalidate();
                     }
                 });

                 
            }
       }.start(); 
    }
    
    /**
     * @return
     */
    protected AdapterView.OnItemClickListener createItemClickListener()
    {
        return new AdapterView.OnItemClickListener()
        {
            public void onItemClick(final AdapterView<?> parent,
                                    final View view,
                                    final int position,
                                    final long id)
            {
            	String tripId = String.valueOf(id);
                Intent intent = new Intent(TripListActivity.this, TripMainActivity.class);
                intent.putExtra(ID_EXTRA, tripId);
                int trpType = tripType == CONFIG_TRIP ? getTripType(tripId) : tripType;
                intent.putExtra(TripListActivity.TRIP_TYPE, trpType);
                // Not sending title for new object (TRIP_TITLE will be null)
                startActivity(intent);
            }
        };
    }
    
    /**
     * @param tripId
     * @return
     */
    private int getTripType(final String tripId)
    {
    	int trpTyp = SQLUtils.getCount(getDB(), "SELECT Type FROM trip WHERE _id = "+tripId);
    	return trpTyp == -1 ? tripType : trpTyp;
    }

    //------------------------------------------------------------------------------
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener()
    {
        public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
                                              String key)
        {
            if (key.equals("sort_order"))
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        initList();
                    }
                });
            }
        }
    };
}
