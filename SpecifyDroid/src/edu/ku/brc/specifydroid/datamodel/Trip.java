package edu.ku.brc.specifydroid.datamodel;

import static edu.ku.brc.utils.XMLHelper.addAttr;
import static edu.ku.brc.utils.XMLHelper.addNode;
import static edu.ku.brc.utils.XMLHelper.indent;
import static edu.ku.brc.utils.XMLHelper.xmlNode;

import java.io.PrintWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Vector;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import edu.ku.brc.specifydroid.BaseDataObj;
import edu.ku.brc.utils.SQLUtils;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Oct 28, 2009
 *
 */
public class Trip extends BaseDataObj<Trip>
{
    private static final String TAG = "Trip";
    
    protected Integer   id   = null;
    protected String    name = "";
    protected int       type = 0;
    protected int       discipline = 0;
    protected String    notes = "";
    protected Date      tripDate = null;
    protected String    firstName1 = "";
    protected String    lastName1 = "";
    protected String    firstName2 = "";
    protected String    lastName2 = "";
    protected String    firstName3 = "";
    protected String    lastName3 = "";
    protected Timestamp timestampCreated = null;
    protected Timestamp timestampModified = null;

    // transient
    protected Vector<TripDataDef> defs    = new Vector<TripDataDef>();
    protected Vector<TripDataDef> newDefs = new Vector<TripDataDef>();
    protected Vector<TripDataDef> updDefs = new Vector<TripDataDef>();
    protected Vector<TripDataDef> delDefs = new Vector<TripDataDef>();
    
    /**
     * @param tableName
     */
    public Trip()
    {
        super("trip");
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specifydroid.BaseDataObj#getDataFromCursor(android.database.Cursor)
     */
    @Override
    public void getDataFromCursor(Cursor cursor) throws Exception
    {
        id         = cursor.getInt(cursor.getColumnIndex("_id"));
        name       = cursor.getString(cursor.getColumnIndex("Name"));
        type       = cursor.getInt(cursor.getColumnIndex("Type"));
        discipline = cursor.getInt(cursor.getColumnIndex("Discipline"));
        notes      = cursor.getString(cursor.getColumnIndex("Notes"));
        tripDate   = getDate(cursor, "TripDate");
        firstName1 = cursor.getString(cursor.getColumnIndex("FirstName1"));
        lastName1  = cursor.getString(cursor.getColumnIndex("LastName1"));
        firstName2 = cursor.getString(cursor.getColumnIndex("FirstName2"));
        lastName2  = cursor.getString(cursor.getColumnIndex("LastName2"));
        firstName3 = cursor.getString(cursor.getColumnIndex("FirstName3"));
        lastName3  = cursor.getString(cursor.getColumnIndex("LastName3"));
        timestampCreated = getTimestamp(cursor, "TimestampCreated");
        timestampModified = getTimestamp(cursor, "TimestampModified");
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specifydroid.BaseDataObj#putContentValues(android.content.ContentValues)
     */
    @Override
    protected void putContentValues(ContentValues cv)
    {
        cv.put("Name", name);
        cv.put("Type", type);
        cv.put("Discipline", discipline);
        cv.put("Notes", notes);
        setDate(cv, tripDate, "TripDate");
        cv.put("FirstName1", firstName1);
        cv.put("LastName1", lastName1);
        cv.put("FirstName2", firstName2);
        cv.put("LastName2", lastName2);
        cv.put("FirstName3", firstName3);
        cv.put("LastName3", lastName3);
        setTimestamp(cv, timestampCreated, "TimestampCreated");
        setTimestamp(cv, timestampModified, "TimestampModified");
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

    /**
     * @return the newDefs
     */
    public Vector<TripDataDef> getNewDefs()
    {
        return newDefs;
    }

    /**
     * @return the delDefs
     */
    public Vector<TripDataDef> getDelDefs()
    {
        return delDefs;
    }

    /**
     * @return the defs
     */
    public Vector<TripDataDef> getDefs()
    {
        return defs;
    }

    /**
     * @return the updDefs
     */
    public Vector<TripDataDef> getUpdDefs()
    {
        return updDefs;
    }

    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public int getType()
    {
        return type;
    }
    public void setType(int type)
    {
        this.type = type;
    }
    
    /**
     * @return the discipline
     */
    public int getDiscipline()
    {
        return discipline;
    }

    /**
     * @param discipline the discipline to set
     */
    public void setDiscipline(int discipline)
    {
        this.discipline = discipline;
    }

    public String getNotes()
    {
        return notes;
    }
    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public Date getTripDate()
    {
        return tripDate;
    }
    public void setTripDate(Date tripDate)
    {
        this.tripDate = tripDate;
    }

    public String getFirstName1()
    {
        return firstName1;
    }
    public void setFirstName1(String firstName1)
    {
        this.firstName1 = firstName1;
    }

    public String getLastName1()
    {
        return lastName1;
    }
    public void setLastName1(String lastName1)
    {
        this.lastName1 = lastName1;
    }

    public String getFirstName2()
    {
        return firstName2;
    }
    public void setFirstName2(String firstName2)
    {
        this.firstName2 = firstName2;
    }

    public String getLastName2()
    {
        return lastName2;
    }
    public void setLastName2(String lastName2)
    {
        this.lastName2 = lastName2;
    }

    public String getFirstName3()
    {
        return firstName3;
    }
    public void setFirstName3(String firstName3)
    {
        this.firstName3 = firstName3;
    }

    public String getLastName3()
    {
        return lastName3;
    }
    public void setLastName3(String lastName3)
    {
        this.lastName3 = lastName3;
    }

    public Timestamp getTimestampCreated()
    {
        return timestampCreated;
    }
    public void setTimestampCreated(Timestamp timestampCreated)
    {
        this.timestampCreated = timestampCreated;
    }

    public Timestamp getTimestampModified()
    {
        return timestampModified;
    }
    public void setTimestampModified(Timestamp timestampModified)
    {
        this.timestampModified = timestampModified;
    }
    
    /**
     * @param db
     */
    public void loadTripDataDefs(final SQLiteDatabase db)
    {
        defs.clear();
        
        if (id != null)
        {
            String where = "WHERE TripId = " + id;
            Cursor cursor = TripDataDef.getAll(db, "tripdatadef", where, null);
            if (cursor.moveToFirst())
            {
                do
                {
                    TripDataDef tdd = new TripDataDef();
                    tdd.loadFrom(cursor);
                    defs.add(tdd);
                    
                } while (cursor.moveToNext());
            }
        }
    }


    /**
     * @param id
     * @param db
     * @return
     */
    public static Trip getById(final SQLiteDatabase db, final String id)
    {
        String[] args = { id };
        
        Cursor c = null;
        try
        {
            c = db.rawQuery("SELECT * FROM trip WHERE _id=?", args);
            c.moveToFirst();
    
            return new Trip().loadFrom(c);
            
        } catch (Exception ex)
        {
            Log.e(TAG, "Error getting id: "+id, ex);
        } finally
        {
            if (c != null) c.close();
        }

        return null;
    }
    
    /**
     * @param db
     * @return
     */
    public static int getCount(final SQLiteDatabase db)
    {
        return SQLUtils.getCount(db, "SELECT COUNT(*) FROM trip");
    }
    
    /**
     * @param pw
     */
    public void writeCVSHeader(final PrintWriter pw)
    {
        pw.println("Id,Name,Type,Discipline,Notes,TripDate,TimestampCreated");
    }
    
    /**
     * @param pw
     */
    public void writeCVSValues(final SQLiteDatabase db, final PrintWriter pw)
    {
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //SimpleDateFormat stsf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        //pw.println(String.format("%d,%s,%d,\"%s\",\"%s\",\"%d\"", id,name,type,notes,sdf.format(tripDate),stsf.format(timestampCreated)));
        
        Cursor c = null;
        try
        {
            int cnt = 0;
            
            String[] args = { id.toString() };
            c = db.rawQuery("SELECT Name FROM tripdatadef WHERE TripID=? ORDER BY ColumnIndex", args);
            if (c != null && c.moveToFirst())
            {
                do 
                {
                    if (cnt > 0) pw.print(',');
                    pw.print(c.getString(0));
                    cnt++;
                } while (c.moveToNext());
                pw.println();
                c.close();
            }
            
            String[] dataArray = new String[cnt];
            clear(dataArray);
            
            String sql = "SELECT c.Data, c.TripRowIndex, ColumnIndex FROM tripdatacell AS c INNER JOIN tripdatadef AS d ON c.TripDataDefID = d.\"_id\" WHERE c.TripID = ? ORDER BY TripRowIndex, ColumnIndex";
            c = db.rawQuery(sql, args);
            if (c != null && c.moveToFirst())
            {
                int row     = -1;
                Integer prevRow = null;
                do 
                {
                    int col = c.getInt(2);
                    row = c.getInt(1);
                    if (prevRow == null) prevRow = row;
                    if (row != prevRow)
                    {
                        write(pw, dataArray);
                        prevRow = row;
                    }
                    dataArray[col] = c.getString(0);
                    
                } while (c.moveToNext());
                write(pw, dataArray);
                c.close();
            }
        } catch (Exception ex)
        {
            Log.e(TAG, "Error writing CSV", ex);
        }
    }
    
    /**
     * Nulls out the array.
     * @param array the array
     */
    private void clear(final String[] array)
    {
        for (int i=0;i<array.length;i++)
        {
            array[i] = null;
        }
    }
    
    /**
     * Write and array of comma separated strings.
     * @param pw the PrintWriter
     * @param array the array of strings
     */
    private void write(final PrintWriter pw, final String[] array)
    {
        for (int i=0;i<array.length;i++)
        {
            if (i > 0) pw.print(',');
            pw.print(array[i] != null ? array[i] : "");
        }
        pw.println();
    }
    
    /**
     * @param db
     * @param pw
     */
    public void toXML(final SQLiteDatabase db, final PrintWriter pw)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat stsf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        
        StringBuilder sb = new StringBuilder(); 
        sb.append("<trip");
        addAttr(sb, "name", name);
        addAttr(sb, "type", type);
        addAttr(sb, "discipline", discipline);
        addAttr(sb, "tripdate", sdf.format(tripDate));
        addAttr(sb, "timestampcreated", stsf.format(timestampCreated));
        sb.append(">\n");
        xmlNode(sb, 4, "notes", notes, true);
        
        Cursor c = null;
        try
        {
            addNode(sb, 4, "celldefs", false);
            sb.append("\n");
            String[] args = { id.toString() };
            c = db.rawQuery("SELECT * FROM tripdatadef WHERE TripID=?", args);
            if (c != null && c.moveToFirst())
            {
                do 
                {
                    indent(sb, 8);
                    sb.append("<def");
                    addAttr(sb, "name", c.getString(c.getColumnIndex("Name")));
                    addAttr(sb, "title", c.getString(c.getColumnIndex("Title")));
                    addAttr(sb, "type", c.getInt(c.getColumnIndex("DataType")));
                    addAttr(sb, "columnindex", c.getString(c.getColumnIndex("ColumnIndex")));
                    sb.append("/>\n");
                   
                } while (c.moveToNext());
            }
        } catch (Exception ex)
        {
            Log.e(TAG, "Error writing xml", ex);
        } finally
        {
            if (c != null)
            {
                c.close();
            }
        }
        addNode(sb, 4, "celldefs", true);
        sb.append("</trip>\n");
               
        pw.println(sb.toString());
    }
    
    /**
     * @param tridId
     * @return
     */
    public static boolean doDeleteTrip(final SQLiteDatabase db, final String tripId)
    {
        try
        {
            db.beginTransaction();
            String[] args = new String[] {tripId};
            db.delete("tripdatacell", "TripID = ?", args);
            db.delete("tripdatadef",  "TripID = ?", args);
            db.delete("trip",         "_id = ?",    args);
            db.setTransactionSuccessful();
            return true;
            
        } catch  (Exception ex)
        {
            Log.e(TAG, ex.getMessage(), ex);
            
        } finally
        {
            db.endTransaction();
        }
        return false;
    }
    
    /**
     * Delete a row (a marked point) from a trip.
     * @param db
     * @param tripId
     * @param rowIndex
     * @return true on success
     */
    public static boolean doDeleteTripRow(final SQLiteDatabase db, 
                                          final String tripId, 
                                          final String rowIndex)
    {
        long rv = -1;
        try
        {
            db.beginTransaction();
            String[] args = new String[] {tripId, rowIndex};
            rv = db.delete("tripdatacell", "TripID = ? AND TripRowIndex = ?", args);
            if (rv > 0)
            {
                db.setTransactionSuccessful();
            }
            
        } catch  (Exception ex)
        {
            Log.e(TAG, ex.getMessage(), ex);
            
        } finally
        {
            db.endTransaction();
        }
        return rv > 0;
    }
    
}