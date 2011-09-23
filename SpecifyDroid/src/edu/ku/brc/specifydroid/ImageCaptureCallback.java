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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;



/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 23, 2009
 *
 */
public class ImageCaptureCallback implements PictureCallback
{
    private Activity     activity;
    private OutputStream fileOutputStream;
    private String       fileName;
    
    private File         cameraDir = new File("/sdcard/dcim/Camera/");

    public ImageCaptureCallback(final OutputStream fileOutputStream)
    {
        this.fileOutputStream = fileOutputStream;
    }

    public ImageCaptureCallback(final Activity activity, final String fileName)
    {
        this.fileName = fileName;
        this.activity = activity;
    }

    @Override
    public void onPictureTaken(final byte[] data, final Camera camera)
    {
        try
        {
            boolean dirOK = true;
            if (!cameraDir.exists())
            {
                dirOK = cameraDir.mkdirs();
            }
            Log.v(getClass().getSimpleName(), "onPictureTaken=" + data + " length = " + data.length);

            if (dirOK)
            {
                FileOutputStream buf = new FileOutputStream("/sdcard/dcim/Camera/" + fileName + ".jpg");
                buf.write(data);
                buf.flush();
                buf.close();
                
                // filoutputStream.write(data);
                fileOutputStream.flush();
                fileOutputStream.close();
                
            } else
            {
                // error dialog
            }
            
            Uri u = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri u2 = Uri.withAppendedPath(u, "1");

            // this is what you have
            // u2 == content://media/external/images/media/1

            String[] projection = { MediaStore.Images.ImageColumns.DATA, /*col1*/
                            MediaStore.Images.ImageColumns.DISPLAY_NAME /*col2*/};

            Cursor c = activity.managedQuery(u2, projection, null, null, null);
            if (c != null && c.moveToFirst())
            {
                String column0Value = c.getString(0);
                String column1Value = c.getString(1);
                Log.d("Data", column0Value);
                Log.d("Display name", column1Value);
            } 
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
