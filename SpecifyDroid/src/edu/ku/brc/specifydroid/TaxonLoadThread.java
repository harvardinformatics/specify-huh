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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.ku.brc.utils.SQLUtils;
import edu.ku.brc.utils.ZipFileHelper;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 25, 2009
 *
 */
public class TaxonLoadThread extends Thread
{
    protected SQLiteDatabase database;
    protected File           taxonFile;
    protected ProgressDialog prgDlg;
    protected Activity       activity;
    
    protected boolean        isOK = false;
    
    private static TaxonLoadThread  instance = null;
    
    /**
     * @param runnable
     */
    public TaxonLoadThread(final SQLiteDatabase database, 
                           final File           taxonFile)
    {
        super();
        
        this.database  = database;
        this.taxonFile = taxonFile;
    }

    /**
     * @return the prgDlg
     */
    public ProgressDialog getPrgDlg()
    {
        return prgDlg;
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run()
    {
        try
        {
            boolean doCreateTable = false;
            boolean doAddRecs     = false;
            Cursor c = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='taxon'", null);
            if (c.getCount() == 0)
            {
                doCreateTable = true;
            }
            c.close();
            
            boolean doDrop = false;
            
            if (!doCreateTable)
            {
                int cnt = SQLUtils.getCount(database, "SELECT COUNT(*) AS count FROM taxon");
                if (cnt < 1)
                {
                    doAddRecs = true;
                } else
                {
                    doDrop = true;
                }
            } 

            if (doDrop)
            {
                database.execSQL("DROP TABLE taxon");
                doCreateTable = true;
                doAddRecs     = true;
            }
            
            if (doCreateTable || doAddRecs)
            {
                String taxaFileName = taxonFile.getName();
                File   inFile       = null;
                if (taxaFileName.endsWith(".zip"))
                {
                    String outName = taxaFileName.substring(0, taxaFileName.length()-4);
                    inFile = ZipFileHelper.getInstance().unzipToSingleFile(taxonFile, new File(outName));
                    if (inFile == null)
                    {
                        return;
                    }
                } else
                {
                    inFile = taxonFile;
                }
                
                if (doCreateTable)
                {
                    String tblStr = "CREATE TABLE `taxon` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `ParentID` INTEGER, `Name` VAR CHAR(128),  `RankID` SHORT)";
                    database.execSQL(tblStr);
                }
                
                //final double maxSize = inFile.length();
                //Log.d("LOAD", "Size: "+maxSize);
                
                BufferedInputStream bis    = null;
                DataInputStream     dis    = null;

                try
                {
                    // Count lines in taxon File
                    InputStream inStrm = new FileInputStream(inFile);
                    bis = new BufferedInputStream(inStrm);
                    dis = new DataInputStream(bis);
                    
                    int    cnt   = 1;
                    double total = 0.0;
                    while (dis.available() != 0)
                    {
                        String   line     = dis.readLine();
                        String[] dataCols = line.split(",");
                        total += line.length() + 1;
                        
                        final String taxonName = dataCols[1]+' '+dataCols[2];
                        String sql = "INSERT INTO taxon (ParentID, Name, RankID) VALUES(0,'"+taxonName+"', 220)";
                        database.execSQL(sql);
                        
                        if (cnt % 100 == 0)
                        {
                            //Log.d("load", "Cnt: "+cnt);
                            
                            final double tot   = total;
                            if (activity != null && prgDlg != null)
                            {
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        prgDlg.setMessage("Loaded: \n"+taxonName);
                                        prgDlg.setProgress((int)tot);
                                        }
                                    });
                            }
                        }
                        cnt++;
                        
                        // Temp stop after a 1,000
                        if (cnt > 300)
                        {
                            break;
                        }
                    }

                    bis.close();
                    dis.close();
                    inStrm.close();
                    
                    inFile.delete();
                    
                    isOK = true;
                    
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                    Editor editor = prefs.edit();
                    editor.putLong(SpecifyActivity.TAXA_FILE_PREF, taxonFile.lastModified());
                    editor.commit();
                    
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            
        } catch (Exception ex)
        {
            Log.e("SpecifyActivity", "Error", ex);
        } finally
        {
            if (prgDlg != null)
            {
                prgDlg.dismiss();
            }
        }
    }

    /**
     * @param prgDlg the prgDlg to set
     */
    public synchronized void set(final Activity       activity, 
                                 final SQLiteDatabase database,
                                 final ProgressDialog prgDlg)
    {
        this.prgDlg   = prgDlg;
        this.database = database;
        this.activity = activity;
    }

    /**
     * @return the instance
     */
    public static TaxonLoadThread getInstance()
    {
        return TaxonLoadThread.instance;
    }

    /**
     * @param instance the instance to set
     */
    public static void setInstance(final TaxonLoadThread instance)
    {
        TaxonLoadThread.instance = instance;
    }
    
    
}
