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

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 10, 2009
 *
 */
public class DataAdapterWithBinder extends SimpleCursorAdapter
{
    /**
     * @param context
     * @param layout
     * @param c
     * @param from
     * @param to
     */
    public DataAdapterWithBinder(SimpleCursorAdapter.ViewBinder viewBinder,
                                 Context context, 
                                 int layout, 
                                 Cursor c, 
                                 String[] from, 
                                 int[] to)
    {
        super(context, layout, c, from, to);
        
        setViewBinder(viewBinder); 
    }

}
