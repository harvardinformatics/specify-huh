package edu.ku.brc.specifydroid;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import edu.ku.brc.specifydroid.datamodel.Trip;
import edu.ku.brc.specifydroid.datamodel.TripDataDef;
import edu.ku.brc.specifydroid.datamodel.TripDataDef.TripDataDefType;
import edu.ku.brc.utils.DialogHelper;
import edu.ku.brc.utils.SQLUtils;
import edu.ku.brc.utils.ZipFileHelper;

public class TripSQLiteHelper extends SQLiteOpenHelper
{
    private static final String  TAG            = "TRP_HELPER";
    private static final String  DATABASE_NAME  = "trip.db";
    private static final int     SCHEMA_VERSION = 1;
    
    private static boolean firstTime      = false;
    
    private Context appContext;

    /**
     * @param context
     */
    public TripSQLiteHelper(final Context appContext)
    {
        super(appContext, DATABASE_NAME, null, SCHEMA_VERSION);
        this.appContext = appContext;
    }
    
    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onOpen(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onOpen(final SQLiteDatabase db)
    {
        super.onOpen(db);
        
        dropAndBuild(db, firstTime);
        
        firstTime = false;
    }
    
    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(final SQLiteDatabase db)
    {
        dropAndBuild(db, true);
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion)
    {
        android.util.Log.w("Trip", "Upgrading database, which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS trip");
        //onCreate(db);
    }
    /**
     * @param text
     * @param repl
     * @param with
     * @param max
     * @return
     */
    public static String replace(final String text, final String repl, final String with, int max)
    {
        if (text == null || (repl != null && repl.length() == 0) || with == null || max == 0) { return text; }

        StringBuffer buf = new StringBuffer(text.length());
        int start = 0, end = 0;
        while ((end = text.indexOf(repl, start)) != -1)
        {
            buf.append(text.substring(start, end)).append(with);
            start = end + repl.length();

            if (--max == 0)
            {
                break;
            }
        }
        buf.append(text.substring(start));
        return buf.toString();
    }
    
    /**
     * @param db
     * @param doDrop
     */
    private void dropAndBuild(final SQLiteDatabase db, 
                              final boolean doDrop)
    {
        if (doDrop)
        {
            db.execSQL("DROP TABLE IF EXISTS trip");
            db.execSQL("DROP TABLE IF EXISTS tripdatadef");
            db.execSQL("DROP TABLE IF EXISTS tripdatacell");
        }
        
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='trip'", null);
        try
        {
            //if (true)
            if (c.getCount() == 0)
            {
                Resources    res      = appContext.getResources();
                AssetManager assetMgr = res.getAssets();
                for (String assetName : assetMgr.list(""))
                {
                    android.util.Log.w("Trip", assetName);
                    if (assetName.equals("build.sql"))
                    {
                        byte[]        bytes  = new byte[2048];
                        StringBuilder sb     = new StringBuilder();
                        InputStream   inStrm = assetMgr.open(assetName);
                        while (inStrm.available() > 0)
                        {
                            int numRead = inStrm.read(bytes);
                            if (numRead > 0)
                            {
                                String line = (new String(bytes, 0, numRead)).trim();
                                line = replace(line, "\n", " ", -1);
                                sb.append(line);
                            } else
                            {
                                break;
                            }
                        }
                        
                        String[] lines = sb.toString().split(";");
                        for (String sql : lines)
                        {
                            android.util.Log.i("Trip", "["+sql+"]");
                            db.execSQL(sql);
                        }
                        inStrm.close();
                    }
                }
            } 
           
        } catch (IOException ex)
        {
            ex.printStackTrace();
            android.util.Log.e(TAG, "Error building DBs", ex);
            
        } finally
        {
            if (c != null)
            {
                c.close();
            }
        }
    }
    
    /**
     * @param activity
     * @param db
     * @param tripId
     */
    public void exportToCSV(final Activity activity, 
                            final SQLiteDatabase db, 
                            final String tripId,
                            final CSVExportIFace exportIFace)
    {
        boolean isExternalStorageAvailable = false;
        boolean isExternalStorageWriteable = false;
        String  state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            isExternalStorageAvailable = isExternalStorageWriteable = true;
            
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            isExternalStorageAvailable = true;
            isExternalStorageWriteable = false;
            
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            isExternalStorageAvailable = isExternalStorageWriteable = false;
        }
        
        if (!isExternalStorageAvailable)
        {
            DialogHelper.showDialog(activity, R.string.extstrgnotavail);
            return;
        }
        
        if (!isExternalStorageWriteable)
        {
            DialogHelper.showDialog(activity, R.string.extstrgnotwrt);
            return;
        }
        
        final File root = Environment.getExternalStorageDirectory();
        if (root.canWrite())
        {
            final Trip trip = Trip.getById(db, tripId);
            if (trip != null)
            {
                String fileName = trip.getName();
                if (fileName != null)
                {
                    fileName = fileName.replace(' ', '_').replace('/', '_');
                } else
                {
                    fileName = "trip";
                }
                
                fileName += ".csv";
                final File outFile = new File(root, fileName);
                final String fName = fileName;
                if (true)//outFile.canWrite())
                {
                    String title = activity.getString(R.string.exporting);
                    String msg   = activity.getString(R.string.file_exp, fileName);
                    final ProgressDialog prgDlg = ProgressDialog.show(activity, title, msg, true);
                    prgDlg.show();
                    
                    new Thread() 
                    {
                        public void run() 
                        {
                            PrintWriter pw = null;
                             try
                             {
                                 pw = new PrintWriter(outFile);
                                 trip.writeCVSValues(db, pw);
    
                                 //Thread.sleep(2000);
                                 
                                 prgDlg.dismiss();
                                 
                                 if (exportIFace != null)
                                 {
                                     activity.runOnUiThread(new Runnable() {
                                         public void run() {
                                             exportIFace.done(outFile);
                                         }
                                     });
                                 } else
                                 {
                                     DialogHelper.showDialog(activity, R.string.file_wrt, fName);
                                 }
                                 
                             } catch (Exception e) 
                             {  
                                 prgDlg.dismiss();
                                 Log.e(TAG, "Error", e); 
                                 e.printStackTrace(); 
                                 DialogHelper.showDialog(activity, e.getMessage());
                                 
                             } finally 
                             {
                                 if (pw != null)
                                 {
                                     pw.flush();
                                     pw.close();
                                 }
                             }
                             // Dismiss the Dialog
                             //prgDlg.dismiss();
                        }
                   }.start();
                    
                }/* else
                {
                    ErrorDlgHelper.showErrorDlg(activity, R.string.no_wrt_file, fileName);
                }*/
            } else
            {
                
                DialogHelper.showDialog(activity, R.string.err_load_trp, tripId);
            }
        } else
        {
            DialogHelper.showDialog(activity, R.string.extstrgnotwrt);
        }
    }
    
    /**
     * @param db
     * @param tableName
     * @return
     */
    public static int getHighestID(final SQLiteDatabase db, final String tableName)
    {
        int cnt = SQLUtils.getCount(db, String.format("SELECT COUNT(*) as count FROM %s", tableName));
        if (cnt > 0)
        {
            cnt = SQLUtils.getCount(db, String.format("SELECT _id as count FROM %s ORDER BY _id DESC LIMIT 0,1", tableName));
        }
        return cnt;
    }
    
    /**
     * REmoves all current records and loads data to WHERE TripId = 1
     * @param db the database
     */
    public static void loadTestData(final SQLiteDatabase db)
    {
        try
        {
            String tripName = "Great Smoky Mountains";
            db.beginTransaction();
            
            Integer tripIdInt = SQLUtils.getCount(db, String.format("SELECT _id as count FROM trip WHERE Name = '%s'", tripName));
            if (tripIdInt != -1)
            {
                Trip.doDeleteTrip(db, tripIdInt.toString());
            }
            
            String[]          titleStrs  = {"Locality Name", "Latitude1", "Longitude1", "Genus", "Species",};// "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8", };
            String[]          colDefStrs = {"LocalityName", "Latitude1", "Longitude1",  "Genus1", "Species1", };//"X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8", };
            TripDataDefType[] defTypes   = {TripDataDefType.strType, TripDataDefType.doubleType, TripDataDefType.doubleType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, TripDataDefType.strType, };
            String[] values= {
                    "Little Pigeon River", "35.69161799381586", "-83.53372824519164", "Catostomus", "commersoni",// "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8",
                    "Little Pigeon River", "35.69182845399097", "-83.5334314327779",  "Hypentelium", "nigricans",// "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8",
                    "Little Pigeon River", "35.69043458326836", "-83.53709797155",    "Moxostoma", "carinatum",// "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8",
                    "Small Falls",         "35.68309906312557", "-83.49230859919675", "Moxostoma", "duquesnei",// "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8",
            };
            
            Timestamp tsCreated = new Timestamp(Calendar.getInstance().getTime().getTime());
            Trip trip = new Trip();
            trip.setType(1); // Collecting Trip
            trip.setTripDate(new Date(111,0,1));
            trip.setName(tripName);
            trip.setDiscipline(1);
            trip.setTimestampCreated(tsCreated);
            trip.setTimestampModified(tsCreated);
            
            tripIdInt = (int)trip.insert(db);
            if (tripIdInt == -1)
            {
                Log.e(TAG, "Error writing trip");
                return;
            }
            String   tripId = tripIdInt.toString();
            String[] args   = new String[] {tripId};
            
            int         numColumns = colDefStrs.length;
            int[]       tddRecIds = new int[numColumns];
            TripDataDef tdd = new TripDataDef();
            for (int i=0;i<numColumns;i++)
            {
                tdd.setColumnIndex(i);
                tdd.setDataType((short)defTypes[i].ordinal());
                tdd.setName(colDefStrs[i]);
                tdd.setTitle(titleStrs[i]);
                tdd.setTripID(tripIdInt);

                int id = (int)tdd.insert(db);
                if (id != -1)
                {
                    tddRecIds[i] = id;
                } else
                {
                    Log.e(TAG, "Error writing tripdatadef " +i);
                    return;
                }
            }
            
            int numNewRows = values.length / numColumns; 
            
            // Fill Taxon Table with Names from Records
            for (int i=(numColumns-1);i<values.length;i+=numColumns)
            {
                args[0] = values[i];
                Cursor c = db.rawQuery("SELECT Name FROM taxon WHERE Name=?", args);
                if (!c.moveToFirst())
                {
                    args[0] = values[i];
                    String sql = "INSERT INTO taxon (ParentID, Name, RankID) VALUES(0, ?, 220)";
                    db.execSQL(sql, args);
                }
                c.close();
            }
            
            String insertStr = "INSERT INTO tripdatacell (TripDataDefID, TripID, TripRowIndex, Data) VALUES(%d, %d, %d, '%s')";
            
            int valInx = 0;
            for (int rowIndex=0;rowIndex<numNewRows;rowIndex++)
            {
                for (int col=0;col<numColumns;col++)
                {
                    String sql = String.format(insertStr, tddRecIds[col], tripIdInt, rowIndex, values[valInx++]);
                    db.execSQL(sql);
                }
            }
            
            db.setTransactionSuccessful();
            
        } catch  (Exception ex)
        {
            Log.e(TAG, ex.getMessage(), ex);
            
        } finally
        {
            db.endTransaction();
        }
    }
    
    /**
     * @param db
     * @param taxaFile
     * @return
     */
    public static boolean loadTaxa(final Activity       activity,
                                   final SQLiteDatabase db, 
                                   final File           taxaFile,
                                   final ProgressDialog prgDlg)
    {
        boolean doCreateTable = false;
        boolean doAddRecs     = false;
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='taxon'", null);
        if (c.getCount() == 0)
        {
            doCreateTable = true;
        }
        c.close();
        
        boolean doDrop = false;
        
        if (!doCreateTable)
        {
            int cnt = SQLUtils.getCount(db, "SELECT COUNT(*) AS count FROM taxon");
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
            db.execSQL("DROP TABLE taxon");
            doCreateTable = true;
            doAddRecs     = true;
        }
        
        if (doCreateTable || doAddRecs)
        {
            String taxaFileName = taxaFile.getName();
            File   inFile       = null;
            if (taxaFileName.endsWith(".zip"))
            {
                String outName = taxaFileName.substring(0, taxaFileName.length()-4);
                inFile = ZipFileHelper.getInstance().unzipToSingleFile(taxaFile, new File(outName));
                if (inFile == null)
                {
                    return false;
                }
            } else
            {
                inFile = taxaFile;
            }
            
            if (doCreateTable)
            {
                String tblStr = "CREATE TABLE `taxon` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `ParentID` INTEGER, `Name` VAR CHAR(128),  `RankID` SHORT)";
                db.execSQL(tblStr);
            }
            
            final double maxSize = inFile.length();
            
            Log.d("LOAD", "Size: "+maxSize);
            
            //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            
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
                    db.execSQL(sql);
                    
                    if (cnt % 100 == 0)
                    {
                        Log.d("load", "Cnt: "+cnt);
                        
                        final double tot   = total;
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                prgDlg.setMessage("Loaded: \n"+taxonName);
                                prgDlg.setProgress((int)tot);
                                //prgDlg.incrementProgressBy(1);
                            }
                        });
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
                
                return true;
                
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }

    //--------------------------------------------------------------    
    public interface CSVExportIFace
    {
        public abstract void done(File file);
    }
}