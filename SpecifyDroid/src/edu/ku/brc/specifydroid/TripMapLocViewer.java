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
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;

import com.pocketjourney.view.MapLocation;
import com.pocketjourney.view.MapLocationViewer;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 20, 2009
 *
 */
public class TripMapLocViewer extends MapLocationViewer
{
    private static final String LONGITUDE     = "Longitude1";
    private static final String LATITUDE      = "Latitude1";
    private static final String GENUS         = "Genus1";
    private static final String SPECIES       = "Species1";
    private static final String VARIETY       = "Variety1";
    private static final String LOCALITYNAME  = "LocalityName";
    
    private String tripId = null;
    
    /**
     * @param context
     * @param attrs
     */
    public TripMapLocViewer(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * @param context
     */
    public TripMapLocViewer(Context context)
    {
        super(context);
    }

    /* (non-Javadoc)
     * @see com.pocketjourney.view.MapLocationViewer#getMapLocations()
     */
    @Override
    public List<MapLocation> getMapLocations()
    {
        if (mapLocations == null) {
            mapLocations = new ArrayList<MapLocation>();
            
            tripId = ((Activity)getContext()).getIntent().getStringExtra(TripListActivity.ID_EXTRA);
            
            String sql = String.format("SELECT tc._id, tc.Data, tc.TripRowIndex, td.Name FROM tripdatacell tc INNER JOIN tripdatadef td ON tc.TripDataDefID = td._id WHERE tc.TripID = %s ORDER BY tc.TripRowIndex, td.ColumnIndex", tripId);
            Cursor  cursor = getDB().rawQuery(sql, null);
            if (cursor.moveToFirst())
            {
                HashMap<String, String> dataHash = new HashMap<String, String>();
                
                Integer rowIndex = null;
                //int idInx = cursor.getColumnIndex("_id");
                int dInx  = cursor.getColumnIndex("Data");
                int rInx  = cursor.getColumnIndex("TripRowIndex");
                int nInx  = cursor.getColumnIndex("Name");
                do
                {
                    int rowNum = cursor.getInt(rInx);
                    if (rowIndex == null)
                    {
                        rowIndex = rowNum;
                        
                    } 
                        
                    //Log.d("debug", "rowIndex: "+rowIndex+ "   rowNum: "+rowNum);
                    if (rowIndex != rowNum)
                    {
                        String name    = dataHash.get(LOCALITYNAME);
                        String genus   = dataHash.get(GENUS);
                        String species = dataHash.get(SPECIES);
                        String variety = dataHash.get(VARIETY);
                        
                        String latStr = dataHash.get(LATITUDE);
                        String lonStr = dataHash.get(LONGITUDE);
                        if (latStr == null || latStr.length() == 0 ||
                            lonStr == null || lonStr.length() == 0 )
                        {
                            continue;
                        }
                        
                        double lat;
                        double lon;
                        try
                        {
                            lat  = Double.parseDouble(latStr);
                            lon  = Double.parseDouble(lonStr);
                            
                        } catch (NumberFormatException ex)
                        {
                            continue;
                        }
                        
                        //Log.d("debug", lat+ " = "+lon);
                        String desc = getDesc(genus, species, variety);
                        MapLocation ml = new MapLocation(name, desc, lat, lon);
                        mapLocations.add(ml);
                        
                        dataHash.clear();
                        rowIndex = rowNum;
                    } 
                    
                    //Log.d("debug", cursor.getString(nInx)+ " = "+cursor.getString(dInx));
                    
                    dataHash.put(cursor.getString(nInx), cursor.getString(dInx));
                    
                } while (cursor.moveToNext());
                
                cursor.close();
                tripDBHelper.close();
                
                String name    = dataHash.get(LOCALITYNAME);
                String genus   = dataHash.get(GENUS);
                String species = dataHash.get(SPECIES);
                String variety = dataHash.get(VARIETY);
                String latStr  = dataHash.get(LATITUDE);
                String lonStr  = dataHash.get(LONGITUDE);
                
                String desc = getDesc(genus, species, variety);
                
                if (latStr != null && latStr.length() > 0 &&
                    lonStr != null && lonStr.length() > 0)
                {
                    double lat  = Double.parseDouble(latStr);
                    double lon  = Double.parseDouble(lonStr);
                    
                    MapLocation ml = new MapLocation(name, desc, lat, lon);
                    mapLocations.add(ml);
                }
            }
            
        }
        return mapLocations;
    }
    
    /**
     * @param genus
     * @param species
     * @param variety
     * @return
     */
    private String getDesc(final String genus, 
                           final String species,
                           final String variety)
    {
        boolean addSp = false;
        StringBuilder sb = new StringBuilder();
        if (genus != null && genus.length() > 0)
        {
            sb.append(genus);
            if (species != null && species.length() > 0)
            {
                sb.append(' ');
                addSp = true;
            }
        }
        
        boolean addVar = false;
        if (addSp)
        {
            sb.append(species);
            if (variety != null && variety.length() > 0)
            {
                sb.append(' ');
                addVar = true;
            }
        }
        
        if (addVar)
        {
            sb.append(variety);
        }
        
        return sb.toString();
    }
    
    //------------------------------------------------------------------------
    //-- Database Access
    //------------------------------------------------------------------------
    private TripSQLiteHelper  tripDBHelper = null;
    private SQLiteDatabase getDB()
    {
        if (tripDBHelper == null)
        {
            tripDBHelper = new TripSQLiteHelper(this.getContext().getApplicationContext());
        }
        return tripDBHelper.getWritableDatabase();
    }
}
