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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 14, 2009
 *
 */
public class SQLUtils
{
    /**
     * @param db
     * @param sql
     * @return
     */
    public static int getCount(final SQLiteDatabase db, final String sql)
    {
        int    cnt = -1;
        Cursor c   = null;
        try
        {
            c = db.rawQuery(sql, null);
            if (c.moveToFirst())
            {
                int countColumn = c.getColumnIndex("count");
                if (countColumn < 0)
                {
                    countColumn = 0;
                }
                cnt =  c.getInt(countColumn);
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            if (c != null)
            {
                c.close();
            }
        }
        return cnt;
    }

    /**
     * Gets a single String value from the first row, first column
     * @param db the database
     * @param sql the SQL that should return just one row and one column
     * @return a string or null
     */
    public static String getStringObj(final SQLiteDatabase db, final String sql)
    {
        String strVal = null;
        Cursor c      = null;
        try
        {
            c = db.rawQuery(sql, null);
            if (c.moveToFirst())
            {
                strVal = c.getString(0);
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            if (c != null)
            {
                c.close();
            }
        }
        return strVal;
    }


}
