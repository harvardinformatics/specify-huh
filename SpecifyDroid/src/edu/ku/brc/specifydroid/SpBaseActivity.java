package edu.ku.brc.specifydroid;
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import edu.ku.brc.utils.DialogHelper;

/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/**
 * 
 */

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Created Date: Jan 12, 2011
 *
 */
public class SpBaseActivity extends Activity
{
    protected TripSQLiteHelper  tripDBHelper = null;
    protected Cursor            cursorModel  = null;

    /**
     * 
     */
    public SpBaseActivity()
    {
        super();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        
        closeCursor();
        closeDB();
    }
    
    
    /**
     * Gets a resource string by name.
     * @param strResName the name of the string
     * @return the localized string
     */
    protected String getStringResourceByName(String strResName)
    {
        return DialogHelper.getStringResourceByName(this, strResName);
    }
    
    //------------------------------------------------------------------------
    //-- Database Access
    //------------------------------------------------------------------------

    /**
     * Closes the cursor if it is open.
     */
    protected void closeCursor()
    {
        if (cursorModel != null)
        {
            stopManagingCursor(cursorModel);
            cursorModel.close();
            cursorModel = null;
        }
    }
    
    /**
     * @return opens the database and returns the database object.
     */
    protected SQLiteDatabase getDB()
    {
        if (tripDBHelper == null)
        {
            tripDBHelper = new TripSQLiteHelper(this.getApplicationContext());
        }
        return tripDBHelper.getWritableDatabase();
    }
    
    /**
     * Closes the database.
     */
    protected void closeDB()
    {
        if (tripDBHelper != null)
        {
            tripDBHelper.close();
            tripDBHelper = null;
        }
    }
}
