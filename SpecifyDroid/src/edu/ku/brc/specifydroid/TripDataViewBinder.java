/* Copyright (C) 2011, Sandbox Software, Inc
 * 
 * Sandbox Software Inc,
 * 1601 Inverness Drive, Lawrence, Kansas, 66047, USA
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

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 10, 2009
 *
 */
public class TripDataViewBinder implements SimpleCursorAdapter.ViewBinder
{
    /**
     * @param iconId
     */
    public TripDataViewBinder()
    {
        super();
    }

    /* (non-Javadoc)
     * @see android.widget.SimpleCursorAdapter.ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)
     */
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex)
    {
        int nImageIndex = cursor.getColumnIndex("Type");
        if (nImageIndex == columnIndex)
        {
            ImageView typeControl = (ImageView)view;
            //int       type        = cursor.getInt(nImageIndex);

            Integer[] dispIcons = TripDetailActivity.getDiscpIcons();
            int dispInx = cursor.getInt(cursor.getColumnIndex("Discipline"));
            int id      = dispIcons[dispInx > dispIcons.length-1 ? 0 : dispInx];
            typeControl.setImageResource(id);
            return true;
        }

        return false; 
    }

}
