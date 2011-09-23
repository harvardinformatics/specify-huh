package edu.ku.brc.specifydroid;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 28, 2009
 *
 */
public abstract class BaseDataObj<T>
{
    protected static SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    protected static SimpleDateFormat dateFormatter      = new SimpleDateFormat("yyyy-MM-dd");

    protected String tableName;
    
    /**
     * @param tableName
     */
    public BaseDataObj(final String tableName)
    {
        this.tableName = tableName;
    }
    
    /**
     * @param db
     * @param tableName
     * @param tripType
     * @param orderBy
     * @return
     */
    public static Cursor getAll(final SQLiteDatabase db, 
                                final String tableName, 
                                final String orderBy)
    {
        return getAll(db, tableName, null, orderBy);
    }

    /**
     * @param db
     * @param tableName
     * @param tripType
     * @param orderBy
     * @return
     */
    public static Cursor getAll(final SQLiteDatabase db, 
                                final String  tableName, 
                                final String  where,
                                final String  orderBy)
    {
        String sql = String.format("SELECT * FROM %s %s %s", tableName, where != null ? where : "", orderBy != null && orderBy.length() > 0 ? " ORDER BY " + orderBy : "");
        //Log.d("Trip", sql);
        return db.rawQuery(sql, null);
    }
    
    /**
     * @param db
     * @param tableName
     * @param tripType
     * @param orderBy
     * @return
     */
    public static Cursor getAll(final SQLiteDatabase db, 
                                final String tableName)
    {
        return getAll(db, tableName, null, null);
    }
    
    /**
     * @param cursor
     * @param colName
     * @return
     */
    protected Timestamp getTimestamp(final Cursor cursor, final String colName)
    {
        String tmStr = cursor.getString(cursor.getColumnIndex(colName));
        if (tmStr != null)
        {
            try
            {
                return new Timestamp(timestampFormatter.parse(tmStr).getTime());
            } catch (Exception ex)
            {
                
            }
        }
        return null;
    }
    
    /**
     * @param cv
     * @param ts
     * @param colName
     */
    protected void setTimestamp(final ContentValues cv, final Timestamp ts, final String colName)
    {
        if (ts != null)
        {
            String tsStr = timestampFormatter.format(ts);
            cv.put(colName, tsStr);
        }
    }
    
    /**
     * @param cursor
     * @param colName
     * @return
     */
    protected Date getDate(final Cursor cursor, final String colName)
    {
        String dtStr = cursor.getString(cursor.getColumnIndex(colName));
        if (dtStr != null)
        {
            try
            {
                return new Date(dateFormatter.parse(dtStr).getTime());
                
            } catch (Exception ex)
            {
                
            }
        }
        return null;
    }
    
    /**
     * @param cv
     * @param ts
     * @param colName
     */
    protected void setDate(final ContentValues cv, final Date date, final String colName)
    {
        if (date != null)
        {
            String dtStr = dateFormatter.format(date);
            cv.put(colName, dtStr);
        }
    }
    
    /**
     * @param cursor
     * @throws Exception
     */
    public abstract void getDataFromCursor(Cursor cursor) throws Exception;
    
    /**
     * @param cursor
     * @return
     */
    @SuppressWarnings("unchecked")
    public T loadFrom(final Cursor cursor)
    {
        if (cursor != null)
        {
            try
            {
                getDataFromCursor(cursor);
                
            } catch (Exception ex)
            {
                android.util.Log.e(tableName, "loadFrom", ex);
            }
        }

        return (T)this;
    }
    
    protected abstract void putContentValues(final ContentValues cv);

    /**
     * @param db
     */
    public long insert(final SQLiteDatabase db)
    {
        long rv = -1;
        ContentValues cv = new ContentValues();
        putContentValues(cv);
        try
        {
            db.beginTransaction();
            rv = db.insertOrThrow(tableName, "", cv);
            if (rv != -1)
            {
                db.setTransactionSuccessful();
            }
            
        } catch  (Exception ex)
        {
            Log.e(getClass().getSimpleName(), ex.getMessage(), ex);
            
        } finally
        {
            db.endTransaction();
        }
        
        return rv;
    }
    
    /**
     * @param db
     */
    public long update(final SQLiteDatabase db, 
                       final String whereClause, 
                       final String[] whereArgs)
    {
        ContentValues cv = new ContentValues();
        putContentValues(cv);
        
        long rv = -1;
        try
        {
            db.beginTransaction();
            rv = db.update(tableName, cv, whereClause, whereArgs);
            if (rv > 0)
            {
                db.setTransactionSuccessful();
            }
            
        } catch  (Exception ex)
        {
            Log.e(getClass().getSimpleName(), ex.getMessage(), ex);
            
        } finally
        {
            db.endTransaction();
        }
        
        return rv;
    }
    
    /**
     * @param id
     * @param db
     */
    public long update(final String id, final SQLiteDatabase db)
    {
        ContentValues cv = new ContentValues();
        String[] args = { id };

        putContentValues(cv);
        
        long rv = -1;
        try
        {
            db.beginTransaction();
            rv = db.update(tableName, cv, "_id=?", args);
            if (rv > 0)
            {
                db.setTransactionSuccessful();
            }
            
        } catch  (Exception ex)
        {
            Log.e(getClass().getSimpleName(), ex.getMessage(), ex);
            
        } finally
        {
            db.endTransaction();
        }
        
        return rv;
    }
    
}