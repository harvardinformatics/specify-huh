package edu.ku.brc.specifydroid.datamodel;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import edu.ku.brc.specifydroid.BaseDataObj;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 28, 2009
 *
 */
public class TripDataCell extends BaseDataObj<TripDataCell>
{
    protected Integer id            = null;
    protected Integer tripDataDefID = null;
    protected Integer tripID        = null;
    protected Integer tripRowIndex  = null;
    protected String  data = "";


    /**
     * @param tableName
     */
    public TripDataCell()
    {
        super("tripdatacell");
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specifydroid.BaseDataObj#getDataFromCursor(android.database.Cursor)
     */
    @Override
    public void getDataFromCursor(Cursor cursor) throws Exception
    {
        id            = cursor.getInt(cursor.getColumnIndex("_id"));
        tripDataDefID = cursor.getInt(cursor.getColumnIndex("TripDataDefID"));
        tripID        = cursor.getInt(cursor.getColumnIndex("TripID"));
        tripRowIndex  = cursor.getInt(cursor.getColumnIndex("TripRowIndex"));
        data          = cursor.getString(cursor.getColumnIndex("Data"));

    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specifydroid.BaseDataObj#putContentValues(android.content.ContentValues)
     */
    @Override
    protected void putContentValues(ContentValues cv)
    {
        cv.put("TripDataDefID", tripDataDefID);
        cv.put("TripID",        tripID);
        cv.put("TripRowIndex",  tripRowIndex);
        cv.put("Data",          data);
    }

    /**
     * @return the id
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getTripDataDefID()
    {
        return tripDataDefID;
    }
    public void setTripDataDefID(Integer tripDataDefID)
    {
        this.tripDataDefID = tripDataDefID;
    }

    public Integer getTripID()
    {
        return tripID;
    }
    public void setTripID(Integer tripID)
    {
        this.tripID = tripID;
    }

    public Integer getTripRowIndex()
    {
        return tripRowIndex;
    }
    public void setTripRowIndex(Integer tripRowIndex)
    {
        this.tripRowIndex = tripRowIndex;
    }

    public String getData()
    {
        return data;
    }
    public void setData(String data)
    {
        this.data = data;
    }


    /**
     * @param id
     * @param db
     * @return
     */
    public static TripDataCell getById(final String id, final SQLiteDatabase db)
    {
        String[] args = { id };
        
        Cursor c = null;
        try
        {
            c = db.rawQuery("SELECT * FROM tripdatacell WHERE _id=?", args);
            c.moveToFirst();
    
            return new TripDataCell().loadFrom(c);
            
        } catch (Exception ex)
        {
            Log.e("TripDataCell", "Error getting id: "+id, ex);
        } finally
        {
            if (c != null) c.close();
        }

        return null;
    }
    

    /**
     * Renumbers the row indexes from startingRow+1 to the highest row number. 'startingRow' should be
     * the index of the row that was removed.
     * @param db the database connection
     * @param startingRow The index of the row that was removed.
     * @param trpId the trip id
     */
    public static void renumberRowIndexes(final SQLiteDatabase db, 
                                          final int    startingRow, 
                                          final String trpId)
    {
        try
        {
            String sql = String.format("SELECT _id, TripRowIndex FROM tripdatacell WHERE TripID=%s AND TripRowIndex > %d ORDER BY TripRowIndex", trpId, startingRow);
            ArrayList<Integer> ids     = new ArrayList<Integer>();
            ArrayList<Integer> rowInxs = new ArrayList<Integer>();
            Cursor          cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst())
            {
                do
                {
                    ids.add(cursor.getInt(0));
                    rowInxs.add(cursor.getInt(1));
                    
                } while (cursor.moveToNext());
                cursor.close();
            }
            
            db.beginTransaction();
            
            String[] args = {" "};
            
            boolean isOK = true;
            ContentValues cv = new ContentValues();
            for (int i=0;i<ids.size();i++)
            {
                int id     = ids.get(i);
                int rowInx = rowInxs.get(i);
                
                args[0] = Integer.toString(id);
                cv.put("TripRowIndex", rowInx-1);
                int rv = db.update("tripdatacell", cv, "_id=?", args);
                if (rv != 1)
                {
                    isOK = false;
                    break;
                }
            }
        
            if (isOK)
            {
                db.setTransactionSuccessful();
            }
        } catch (SQLException ex)
        {
        }
        db.endTransaction();
    }
}