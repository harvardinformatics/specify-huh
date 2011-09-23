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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.ku.brc.specifydroid.datamodel.Trip;
import edu.ku.brc.utils.DialogHelper;
import edu.ku.brc.utils.SQLUtils;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 27, 2009
 *
 */
public class TripMainPanelAdapter extends BaseAdapter
{
    private static final int BROWSE_INX = 1;
    private static final int EXPORT_INX = 3;
    private static final int MAP_INX    = 4;
    
    private static final int[] DISABLE_ICONS_INX = {BROWSE_INX, EXPORT_INX, MAP_INX};
    
    private static final Integer[] iconIds = 
    { 
        R.drawable.mylocation, R.drawable.browsedb,
        R.drawable.camera,     R.drawable.exportdataset,
        R.drawable.email,      R.drawable.googlemaps,
        R.drawable.config,     R.drawable.delete,     
    };
    
    private static final Integer[] iconFadedIds = 
    { 
        R.drawable.mylocation,  R.drawable.browsedb_faded,
        R.drawable.camera,      R.drawable.exportdataset_faded,
        R.drawable.email_faded, R.drawable.googlemaps_faded,
        R.drawable.config,      R.drawable.delete,     
    };
    
    private static final Integer[] titleIds = 
    {
        R.string.tmgcollect, R.string.tmgbrowsedb,
        R.string.tmgcamera, R.string.tmgexportdataset,
        R.string.tmgemail,  R.string.tmggooglemaps,
        R.string.tmgconfig, R.string.tmgdeltrip,    
    };
    
    // Data Members
    private TripMainActivity     tripMainActivity;
    private String               tripId;
    private String               tripTitle;
    private int                  tripType;
    
    private ArrayList<ImageView> imgViews = new ArrayList<ImageView>();
    private ArrayList<TextView>  txtViews = new ArrayList<TextView>();

    /**
     * @param activity the TripMainActivity
     * @param tripId the id of the current trip
     */
    public TripMainPanelAdapter(final TripMainActivity activity,
                                final String tripId,
                                final int    tripType,
                                final String tripTitle)
    {
        this.tripMainActivity = activity;
        this.tripId           = tripId;
        this.tripTitle        = tripTitle;
        this.tripType         = tripType;
        
        this.titleIds[0]      = tripType == TripListActivity.OBS_TRIP ? R.string.tmgobserve : R.string.tmgcollect;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent)
    {
        // NOTE: Tried ImageButton but disabled state doesn't make image look disabled.
        LinearLayout llCell    = new LinearLayout(tripMainActivity);
        ImageView    imageView = null; 
        TextView     titleView = null;
        if (convertView == null)
        { 
            llCell.setOrientation(LinearLayout.VERTICAL);
            
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(tripMainActivity);
            imageView.setImageResource(iconIds[position]);
            imageView.setFocusable(true);
            imageView.setPadding(8, 8, 8, 8);
            
            //imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //imageView.setLayoutParams(new GridView.LayoutParams(48, 48));

            titleView = new TextView(tripMainActivity);
            titleView.setText(titleIds[position]);
            titleView.setGravity(Gravity.CENTER_HORIZONTAL);
            
            llCell.addView(imageView);
            llCell.addView(titleView);
            
            imgViews.add(imageView);
            txtViews.add(titleView);
            
        } else
        {
            llCell = (LinearLayout)convertView;
            imageView = (ImageView)llCell.getChildAt(0);
        }
        
        imageView.setOnClickListener(new ClickedViewListener(position));
        setEnabled(tripMainActivity.getItemCount() > 0);

        return llCell;
    }
    
    /**
     * @param tripTitle the tripTitle to set
     */
    public void setTripTitle(String tripTitle)
    {
        this.tripTitle = tripTitle;
    }

    /**
     * @param enabled
     */
    public void setEnabled(final boolean enabled)
    {
        Integer[] ids = enabled ? iconIds : iconFadedIds;
        for (int inx : DISABLE_ICONS_INX)
        {
            if (inx < imgViews.size())
            {
                imgViews.get(inx).setImageResource(ids[inx]);
                txtViews.get(inx).setEnabled(enabled);
            }
        }
    }
    
    /**
     * 
     */
    private void doDeleteTrip()
    {
        String sql      = String.format("SELECT Name FROM trip WHERE _id = %s", tripId);
        String tripName = SQLUtils.getStringObj(getDB(), sql);
        
        final AlertDialog.Builder builder = new AlertDialog.Builder(tripMainActivity)
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (Trip.doDeleteTrip(getDB(), tripId))
                {
                    tripMainActivity.finish();   
                }
            }
            })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
            });
        builder.setTitle(String.format(tripMainActivity.getString(R.string.deletetrip), tripName));
        AlertDialog alert = builder.create();
        alert.show();
        
        closeDB();
    }
    
    /* (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount()
    {
        return iconIds.length;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(final int position)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position)
    {
        return 0;
    }
    
    //------------------------------------------------------------
    class ClickedViewListener implements View.OnClickListener
    {
        private int inx;

        /**
         * @param inx
         */
        public ClickedViewListener(final int inx)
        {
            this.inx = inx;
        }

        @Override
        public void onClick(View view)
        {
            switch (inx)
            {
                case 0: // My Location Lat/Lon
                {
                    tripMainActivity.addLatLon();
                    break;
                } 
                
                case 1: // Browse
                {
                    Intent intent = new Intent(tripMainActivity, TripDataEntryDetailActivity.class);
                    intent.putExtra(TripListActivity.ID_EXTRA, tripId);
                    intent.putExtra(TripListActivity.TRIP_TITLE, tripTitle);
                    intent.putExtra(TripListActivity.TRIP_TYPE, tripType);
                    intent.putExtra(TripListActivity.DETAIL_CLASS, TripDataEntryDetailActivity.class.getName());
                    tripMainActivity.startActivity(intent);
                    break;
                } 

                case 2: // Camera
                {
                    //Intent intent = new Intent(tripMainActivity, TripListActivity.class);
                    //intent.putExtra(TripListActivity.TRIP_TYPE, TripListActivity.CONFIG_TRIP);
                    //tripMainActivity.startActivity(intent);
                    
                    DialogHelper.showDialog(tripMainActivity, R.string.notimpl);
                    break;
                }
                
                case 3: // Export as CSV
                {
                    TripSQLiteHelper dbHelper = new TripSQLiteHelper(tripMainActivity);
                    dbHelper.exportToCSV(tripMainActivity, getDB(), tripId, null);
                    break;
                }
                
                case 4: // Email exported file.
                {
                    TripSQLiteHelper dbHelper = new TripSQLiteHelper(tripMainActivity);
                    dbHelper.exportToCSV(tripMainActivity, getDB(), tripId, tripMainActivity);
                    break;
                }
                
                case 5: // Maps
                {
                    Intent intent = new Intent(tripMainActivity, TripMapLocationActivity.class);
                    intent.putExtra(TripListActivity.ID_EXTRA, tripId);
                    intent.putExtra(TripListActivity.TRIP_TYPE, tripType);
                    intent.putExtra(TripListActivity.TRIP_TITLE, tripTitle);
                    tripMainActivity.startActivity(intent);
                    break;
                }
                
                case 6: // Config
                {
                    Intent intent = new Intent(tripMainActivity, TripDetailActivity.class);
                    intent.putExtra(TripListActivity.ID_EXTRA, tripId);
                    intent.putExtra(TripListActivity.TRIP_TYPE, tripType);
                    intent.putExtra(TripDetailActivity.ISNEW_EXTRA, false);
                    intent.putExtra(TripListActivity.TRIP_TITLE, tripTitle);
                    tripMainActivity.startActivity(intent);
                    break;
                }
                    
                case 7: // Delete Trip
                    doDeleteTrip();
                    break;
                    
            } 
        }
    }
    
    //------------------------------------------------------------------------
    //-- Database Access
    //------------------------------------------------------------------------
    private TripSQLiteHelper  tripDBHelper = null;
    
    protected void closeDB()
    {
        if (tripDBHelper != null)
        {
            //Log.d(getClass().getName(), "close()");
            tripDBHelper.close();
            tripDBHelper = null;
        }
    }
    
    private SQLiteDatabase getDB()
    {
        //Log.d(getClass().getName(), "getDB");
        if (tripDBHelper == null)
        {
            tripDBHelper = new TripSQLiteHelper(tripMainActivity.getApplicationContext());
        }
        return tripDBHelper.getWritableDatabase();
    }
}

