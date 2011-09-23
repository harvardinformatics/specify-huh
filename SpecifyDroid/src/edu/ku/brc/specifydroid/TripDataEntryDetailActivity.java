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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import edu.ku.brc.specifydroid.datamodel.Trip;
import edu.ku.brc.specifydroid.datamodel.TripDataCell;
import edu.ku.brc.specifydroid.datamodel.TripDataDef;
import edu.ku.brc.utils.DialogHelper;
import edu.ku.brc.utils.SQLUtils;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 11, 2009
 *
 */
public class TripDataEntryDetailActivity extends SpBaseActivity
{
    private final static int CAL_DLG = 0;
    
    public final static String ID_EXTRA    = "edu.ku.brc.specifydroid._TripDataDefID";
    public final static String ID_ISCREATE = "edu.ku.brc.specifydroid._ISNEW";
    public final static String LAT_VAL     = "edu.ku.brc.specifydroid._LAT_VAL";
    public final static String LON_VAL     = "edu.ku.brc.specifydroid._LON_VAL";
    public final static String TITLE       = "edu.ku.brc.specifydroid.TITLE";

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    private AtomicBoolean              isActive = new AtomicBoolean(true);
    private String                     tripId      = null;
    private Integer                    tripType    = null;
    private String                     tripTitle   = null;
    private Integer                    ttdId       = null;
    private boolean                    isNewRec    = false;
    private boolean                    isCreateRec = false;
    private boolean                    isChanged   = false;
    private int                        numDataColumns = 0;
    
    private TripDataCell               tripDataCell = new TripDataCell();
    
    private ImageButton                posFirstBtn;
    private ImageButton                posLastBtn;
    private ImageButton                posPrevBtn;
    private ImageButton                posNextBtn;
    private Button                     saveBtn;
    private TextView                   recLabel;
    
    private Integer                    rowIndex    = null;
    private Integer                    numRows     = null;
    
    private HashMap<View, Integer>     recNumHash  = new HashMap<View, Integer>();
    private HashMap<Integer, String>   valueHash   = new HashMap<Integer, String>();
    private HashMap<View, Boolean>     changedHash = new HashMap<View, Boolean>();
    private HashMap<View, Integer>     viewToTTDId = new HashMap<View, Integer>();
    private HashMap<String, View>      compHash    = new HashMap<String, View>();
    private HashMap<String, Integer >  compTDD     = new HashMap<String, Integer>();
    private ArrayList<View>            comps       = new ArrayList<View>();
    
    private TextView                   calTextView = null;
    
    /**
     * 
     */
    public TripDataEntryDetailActivity()
    {
        super();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.tdc_detail_form);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);

        if (savedInstanceState != null)
        {
            tripId      = savedInstanceState.getString(TripListActivity.ID_EXTRA);
            isCreateRec = savedInstanceState.getBoolean(ID_ISCREATE, false);
            tripType    = savedInstanceState.getInt(TripListActivity.TRIP_TYPE);
            tripTitle   = savedInstanceState.getString(TripListActivity.TRIP_TITLE);

        } else
        {
            tripId      = getIntent().getStringExtra(TripListActivity.ID_EXTRA);
            isCreateRec = getIntent().getBooleanExtra(ID_ISCREATE, false);
            tripType    = getIntent().getIntExtra(TripListActivity.TRIP_TYPE, TripListActivity.CONFIG_TRIP);
            tripTitle   = getIntent().getStringExtra(TripListActivity.TRIP_TITLE);
        }
        
        ((TextView)findViewById(R.id.headertitle)).setText(tripTitle == null ? TripMainActivity.createTitle(this, tripType) : tripTitle);
        
        isNewRec = isCreateRec;
        
        saveBtn = (Button) findViewById(R.id.tdcsave);
        saveBtn.setEnabled(false);

        posFirstBtn = (ImageButton)findViewById(R.id.posfirst);
        posLastBtn  = (ImageButton)findViewById(R.id.poslast);
        posNextBtn  = (ImageButton)findViewById(R.id.posnext);
        posPrevBtn  = (ImageButton)findViewById(R.id.posprev);
        recLabel    = (TextView)findViewById(R.id.reclabel);

        if (!isCreateRec)
        {
            posFirstBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    adjustIndex(Integer.MIN_VALUE);
                }
            });
    
            posPrevBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    adjustIndex(-1);
                }
            });
    
            posNextBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    adjustIndex(1);
                }
            });
    
            posLastBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    adjustIndex(Integer.MAX_VALUE);
                }
            });
            
        } else
        {
            posFirstBtn.setVisibility(View.INVISIBLE);
            posLastBtn.setVisibility(View.INVISIBLE);
            posNextBtn.setVisibility(View.INVISIBLE);
            posPrevBtn.setVisibility(View.INVISIBLE);
            recLabel.setVisibility(View.INVISIBLE);
            recLabel.setText(" ");
        }
        
        saveBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (isChanged)
                {
                    doSave();
                }
            }
        });

        buildUI();
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putString(TripListActivity.ID_EXTRA, tripId);
        outState.putString(TripListActivity.TRIP_TITLE, tripTitle);
        outState.putBoolean(ID_ISCREATE, isCreateRec);
        outState.putInt(TripListActivity.ID_EXTRA, tripType);
    }
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        new MenuInflater(getApplication()).inflate(R.menu.tdc_menu, menu);
        return (super.onCreateOptionsMenu(menu));
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.delitem)
        {
            clearForm();
            doDeleteTripRow();
            return true;
        } 
        return super.onOptionsItemSelected(item);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) 
    {
        if (id == CAL_DLG)
        {
            DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener()
            {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth)
                {
                    Calendar tripDate = Calendar.getInstance();
                    tripDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    tripDate.set(Calendar.MONTH, monthOfYear);
                    tripDate.set(Calendar.YEAR, year);
                    
                    char[] str = (new String(sdf.format(tripDate.getTime()))).toCharArray();
                    calTextView.setText(str, 0, str.length);
                    isChanged = true;
                    updateUIState();
                }
            };
            Calendar tripDate = Calendar.getInstance();
            return new DatePickerDialog(this,
                                        listener,
                                        tripDate.get(Calendar.YEAR), 
                                        tripDate.get(Calendar.MONTH), 
                                        tripDate.get(Calendar.DAY_OF_MONTH));
        }
        return null;
    }
    
    /**
     * @param delta
     */
    private void adjustIndex(final int delta)
    {
        if (rowIndex == null)
        {
            clearForm();
            
        } else if (delta == 1)
        {
            rowIndex++;
            if (rowIndex == numRows)
            {
                adjustIndex(-1);
            }
            
        } else if (delta == -1)
        {
            rowIndex--;
            if (rowIndex == 0)
            {
                cursorModel.moveToFirst();
                
            } else
            {
                moveToPrevious();
            }
        } else if (delta == Integer.MIN_VALUE)
        {
            rowIndex = 0;
            cursorModel.moveToFirst();
            
        } else if (delta == Integer.MAX_VALUE)
        {
            rowIndex = numRows-1;
            cursorModel.moveToLast();
            
            cursorModel.move(-numDataColumns + 1);
        }
        
        fillForm();
        
        updateUIState();
               
        updateRecDisplay();
        
        isChanged = false;
        changedHash.clear();
    }
    
    /**
     * 
     */
    private void moveToPrevious()
    {
        int rowInx = cursorModel.getColumnIndex("TripRowIndex");
        int rowNum = cursorModel.getInt(rowInx);
        
        //Log.d("moveToPrevious", "Pos: "+cursorModel.getPosition()+", rowNum: "+rowNum+", rowIndex: "+rowIndex);
        
        while (rowNum >= rowIndex && !cursorModel.isBeforeFirst())
        {
            if (!cursorModel.moveToPrevious())
            {
                break;
            }
            rowNum = cursorModel.getInt(rowInx);
            //Log.d("moveToPrevious", "Pos: "+cursorModel.getPosition()+", rowNum: "+rowNum+", rowIndex: "+rowIndex);
        }
        cursorModel.moveToNext();
    }
    
    /**
     * 
     */
    private void updateRecDisplay()
    {
        if (!isCreateRec)
        {
            String lblStr = String.format("%3d of %3d", (rowIndex != null ? rowIndex : 0)+1, numRows);
            recLabel.setText(lblStr);
        }
    }
    
    /**
     * 
     */
    private void doSave()
    {
        if (!doSaveToDB())
        {
            DialogHelper.showTimedDialog(this, 1.5, R.string.saved, new DialogHelper.TimedDialogListener() {
                @Override
                public void dialogClosed()
                {
                    TripDataEntryDetailActivity.this.finish();
                }
            });
        }
    }
    
    /**
     * 
     */
    private boolean doSaveToDB()
    {
        boolean isError = false;
        Integer trpId   = Integer.parseInt(tripId);
        
        if (isNewRec)
        {
            if (rowIndex == null)
            {
                int count = SQLUtils.getCount(getDB(), "SELECT COUNT(*) FROM tripdatacell WHERE TripID = " + tripId);
                if (count == 0)
                {
                    rowIndex = 0;
                } else
                {
                    rowIndex = SQLUtils.getCount(getDB(), "SELECT TripRowIndex AS count FROM tripdatacell WHERE TripID = " +tripId+" ORDER BY TripRowIndex DESC LIMIT 1");
                    rowIndex++;
                }
            } else
            {
                rowIndex++;
            }
            
            numRows++;
            
            long rv = -1;
            for (View view : viewToTTDId.keySet())
            {
                String value = getValue(view);
                
                tripDataCell.setTripDataDefID(viewToTTDId.get(view));
                tripDataCell.setTripID(trpId);
                tripDataCell.setTripRowIndex(rowIndex);
                tripDataCell.setData(value);
                rv = tripDataCell.insert(getDB());
                if (rv == -1)
                {
                    isError = true;
                    DialogHelper.showDialog(TripDataEntryDetailActivity.this, "Error inserting "+TripDataEntryDetailActivity.this.getClass().getSimpleName());
                    break;
                }
                //Log.d("DBG", "New Row: V["+value+"] tripDataDefID["+tripDataCell.getTripDataDefID()+"] rowIndex["+tripDataCell.getTripRowIndex()+"] data["+tripDataCell.getData()+"] TripID["+tripDataCell.getTripID()+"]");
            }
            isNewRec = false;
            if (cursorModel != null)
            {
                cursorModel.requery();
                rowIndex = null;
            }
            
        } else
        {
            long rv = -1;
            for (View view : changedHash.keySet())
            {
                Boolean changed = changedHash.get(view);
                if (changed != null && changed)
                {
                    String value = getValue(view);
                    tripDataCell.setData(value);
                    tripDataCell.setTripDataDefID(viewToTTDId.get(view));
                    tripDataCell.setTripID(trpId);
                    tripDataCell.setTripRowIndex(rowIndex);
                    
                    //Log.d("DBG", "Update Row: ID["+cursorModel.getString(idInx)+"] tripDataDefID["+tripDataCell.getTripDataDefID()+"] rowIndex["+tripDataCell.getTripRowIndex()+"] data["+tripDataCell.getData()+"] TripID["+tripDataCell.getTripID()+"]");
                    
                    Integer recNo = recNumHash.get(view);
                    rv = tripDataCell.update(recNo.toString(), getDB());
                    if (rv != 1)
                    {
                        isError = true;
                        DialogHelper.showDialog(TripDataEntryDetailActivity.this, "Error updating "+TripDataEntryDetailActivity.this.getClass().getSimpleName());
                        break;
                    }
                    cursorModel.requery();
                }
            }
        }
        isChanged = false;
        changedHash.clear();
        updateUIState();
        
        Log.d("SAVE", "isError "+isError);
        return isError;
    }
    
    /**
     * @param fieldView
     * @return
     */
    private String getValue(final View fieldView)
    {
        if (fieldView instanceof EditText)
        {
            EditText edtTxt = (EditText)fieldView;
            return edtTxt.getText().toString();
        }
        
        TextView txtView = (TextView)fieldView;
        return txtView.getText().toString();   
    }
    
    /**
     * 
     */
    private void updateUIState()
    {
        if (!isCreateRec)
        {
            boolean isLastOrPast    = rowIndex == null ? true : rowIndex == numRows-1;
            boolean isFirstOrBefore = rowIndex == null ? true : rowIndex == 0;
            
            posFirstBtn.setEnabled(!isFirstOrBefore);
            posPrevBtn.setEnabled(!isFirstOrBefore);
            
            posNextBtn.setEnabled(!isLastOrPast);
            posLastBtn.setEnabled(!isLastOrPast);
            
            saveBtn.setEnabled(isChanged);
        }
    }
    
    /**
     * 
     */
    protected void buildUI()
    {
        ProgressDialog prgDlg = ProgressDialog.show(this, null, getString(R.string.loading), true);
        
        TableLayout tableLayout = (TableLayout)findViewById(R.id.uicontainer);
        Cursor      cursor      = TripDataDef.getAll(getDB(), "tripdatadef", "WHERE TripId = " + tripId, " ColumnIndex ASC");
        if (cursor.moveToFirst())
        {
            int nmInx = cursor.getColumnIndex("Name");
            int ttInx = cursor.getColumnIndex("Title");
            int dtInx = cursor.getColumnIndex("DataType");
            TripDataDef.TripDataDefType[] types = TripDataDef.TripDataDefType.values();
            int id = 0;
            do
            {
                String   cellName  = cursor.getString(nmInx);
                String   cellTitle = cursor.getString(ttInx);
                short    type      = cursor.getShort(dtInx);
                
                TableRow tblRow    = new TableRow(this);
                tblRow.setOrientation(TableRow.HORIZONTAL); 
                
                TextView label  = new TextView(this);
                label.setText(cellTitle + ":");
                label.setGravity(Gravity.RIGHT);
                
                EditText edtTxt     = null;
                View     view       = null;
                View     layoutView = null;  // The Layout View
                switch (types[type])
                {
                    case intType:
                    case floatType:
                    case doubleType:
                    {
                        edtTxt = new EditText(this);
                        view = layoutView = edtTxt;
                    } break;
                    
                    case dateType:
                    {
                        boolean doDate = true;
                        if (doDate)
                        {
                            TextView  txtView = new TextView(this);
                            ImageView calBtn  = new ImageView(this);
                            calBtn.setImageResource(R.drawable.calendar);
                            calBtn.setOnClickListener(new CalClickListener(txtView));
                            
                            char[] str = (new String(sdf.format(Calendar.getInstance().getTime()))).toCharArray();
                            txtView.setText(str, 0, str.length);
                            
                            LinearLayout container = new LinearLayout(this);
                            container.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(txtView);
                            container.addView(calBtn);
                            
                            view       = txtView;
                            layoutView = container;
                            txtView.setPadding(4, 6, 0, 6);
                        } else
                        {
                            TextView txtView = new TextView(this);
                            view       = txtView;
                            layoutView = txtView;
                            txtView.setPadding(4, 6, 0, 6);
                        }
                    } break;
                    
                    case strType:
                    {
                        /*if (cellName.equals("Genus1") || cellName.equals("Species1"))
                        {
                            AutoCompleteTextView actv = new AutoCompleteTextView(this);
                            hookupAutoCompTextField(this, actv);
                            edtTxt = actv;
                            view   = edtTxt;
                        } else
                        {*/
                            edtTxt = new EditText(this);
                            view   = edtTxt;
                        //}
                        layoutView = view;
                    } break;
                }
                
                if (edtTxt != null)
                {
                    edtTxt.addTextChangedListener(createTextWatcher(edtTxt));
                    edtTxt.setId(id);
                    edtTxt.setSingleLine();
                }
               
                tblRow.addView(label, 0);
                tblRow.addView(layoutView, 1);
                
                int rows = tableLayout.getChildCount();
                tableLayout.addView(tblRow, rows);
                
                ttdId = cursor.getInt(cursor.getColumnIndex("_id"));
                
                viewToTTDId.put(view, ttdId);
                compHash.put(cellName, view);
                compTDD.put(cellName, ttdId);
                comps.add(view);
                id++;
                
            } while (cursor.moveToNext());
            
            numDataColumns = id;
            
            for (int i=0;i<comps.size()-1;i++)
            {
                comps.get(i).setNextFocusDownId(comps.get(i+1).getId());
            }
            //comps.get(comps.size()-1).setNextFocusDownId(R.id.ttdsave);

        }
        cursor.close();
        
        closeCursor();
        
        startBrowse(prgDlg);
    }
    
    /**
     * 
     */
    private void startBrowse(final ProgressDialog prgDlg)
    {
        if (!isCreateRec)
        {
            String sql = String.format("SELECT COUNT(*) FROM tripdatacell WHERE TripID = %s", tripId);
            numRows = SQLUtils.getCount(getDB(), sql);
            if (numRows == 0)
            {
                if (prgDlg != null)
                {
                    prgDlg.dismiss();
                }
                
                finish();
                return;
            }
        }
        
        String sql = String.format("SELECT TripRowIndex AS count FROM tripdatacell WHERE TripID = %s ORDER BY TripRowIndex DESC LIMIT 1", tripId);
        numRows = SQLUtils.getCount(getDB(), sql);
        numRows++;
        
        rowIndex = null;
        
        boolean doFill = true;
        if (!isCreateRec)
        {
            int count = SQLUtils.getCount(getDB(), "SELECT COUNT(*) FROM tripdatacell WHERE TripID = " + tripId);
            if (count == 0)
            {
                isNewRec = true;
                clearForm();
                
            } else
            {
                sql = String.format("SELECT tc._id, tc.Data, tc.TripRowIndex, td.ColumnIndex FROM tripdatacell tc INNER JOIN tripdatadef td ON tc.TripDataDefID = td._id WHERE tc.TripID = %s ORDER BY tc.TripRowIndex, td.ColumnIndex", tripId);
                		
                cursorModel = getDB().rawQuery(sql, null);
                
                if (cursorModel.moveToFirst())
                {
                    rowIndex = 0;
                    fillForm();
                }
                
                doFill = false;
            }
        }
            
        updateUIState();
        updateRecDisplay();

        isChanged = false;
        changedHash.clear();
        
        if (doFill)  
        {
            fillInGPSValues();
        }
        
        if (prgDlg != null)
        {
            prgDlg.dismiss();
        }
        
        if (cursorModel != null)
        {
            startManagingCursor(cursorModel);
        }
    }
    
    /**
     * 
     */
    private void fillInGPSValues()
    {
        String latStr = "";
        String lonStr = "";
        
        Double lat = getIntent().getDoubleExtra(LAT_VAL, -1000.0);
        Double lon = getIntent().getDoubleExtra(LON_VAL, -1000.0);
        
        if (lat != null && lat != -1000.0 && lon != null && lon != 1000.0)
        {
            latStr = Double.toString(lat);
            lonStr = Double.toString(lon);
        }
        
        EditText latEdtTxt = (EditText)compHash.get("Latitude1");
        if (latEdtTxt != null)
        {
            latEdtTxt.setText(latStr);
        }
        
        EditText lonEdtTxt = (EditText)compHash.get("Longitude1");
        if (latEdtTxt != null)
        {
            lonEdtTxt.setText(lonStr);
        }
        
        TextView dateTF = (TextView)compHash.get("CollectedDate");
        if (dateTF != null)
        {
            dateTF.setText(sdf.format(Calendar.getInstance().getTime()));
        } else
        {
            Log.e("DatEntry", "Couldn't find Date Component.");
        }
    }
    
    /**
     * @param editText
     * @return
     */
    private TextWatcher createTextWatcher(final EditText editText)
    {
        return new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                changedHash.put(editText, Boolean.TRUE);
                isChanged = true;
                saveBtn.setEnabled(true);
            }
        };
    }
    
    /**
     * Assume it is already positioned at the beginning of a row
     */
    private void fillForm()
    {
        //Log.d("fillForm", "isBefore: "+cursorModel.isBeforeFirst());
        //Log.d("fillForm", "isFirst:  "+cursorModel.isFirst());
        //Log.d("fillForm", "Position: "+cursorModel.getPosition());
        
        if (cursorModel.isBeforeFirst())
        {
            cursorModel.moveToFirst();
        }
        
        valueHash.clear();
        
        int recNumInx  = cursorModel.getColumnIndex("_id");
        int dataInx    = cursorModel.getColumnIndex("Data");
        int dataColInx = cursorModel.getColumnIndex("ColumnIndex");
        int dataRowInx = cursorModel.getColumnIndex("TripRowIndex");
        
        for (int i=0;i<numDataColumns;i++)
        {
            int rowInx = cursorModel.getInt(dataRowInx);
            //Log.d("DEBUG", "id: " + cursorModel.getInt(cursorModel.getColumnIndex("_id")) +"  rowInx: "+rowInx +"  rowIndex: "+rowIndex);

            if (rowInx != rowIndex)
            {
                //cursorModel.moveToPrevious();
                //Log.d("DEBUG", "id: " + cursorModel.getInt(cursorModel.getColumnIndex("_id")) +"  rowInx: "+rowInx +"  rowIndex: "+rowIndex);
                break;
            }
            
            int    recNum = cursorModel.getInt(recNumInx);
            int    column = cursorModel.getInt(dataColInx);
            String data   = cursorModel.getString(dataInx);
            
            recNumHash.put(comps.get(i), recNum);
            
            //Log.d("DEBUG", "id: " + cursorModel.getInt(cursorModel.getColumnIndex("_id")) +"  data["+data+"] Col: "+ column + "  Row: "+rowInx);
            
            valueHash.put(column, data);
            
            if (!cursorModel.isLast())
            {
                if (!cursorModel.moveToNext())
                {
                    break;
                }
            }
        }
        
        for (int i=0;i<numDataColumns;i++)
        {
            String data = valueHash.get(i);
            setValue(comps.get(i), data != null ? data : "");
        }
        
        saveBtn.setEnabled(false);
        isChanged = false;
    }
    
    /**
     * @param view
     * @param data
     */
    private void setValue(final View view, final String data)
    {
        if (view instanceof EditText)
        {
            ((EditText)view).setText(data);
        } else if (view instanceof AutoCompleteTextView)
        {
            ((AutoCompleteTextView)view).setText(data);
        } else
        {
            ((TextView)view).setText(data);
        }
    }
    
    /**
     * 
     */
    private void doDeleteTripRow()
    {
        closeCursor();
        
        if (rowIndex != null)
        {
            ProgressDialog prgDlg = ProgressDialog.show(this, null, getString(R.string.loading), true);
            
            Trip.doDeleteTripRow(getDB(), tripId, rowIndex.toString());
            TripDataCell.renumberRowIndexes(getDB(), rowIndex, tripId);
            startBrowse(prgDlg);
        }
    }   

    /**
     * 
     */
    private void clearForm()
    {
        String data = "";
        for (int i=0;i<numDataColumns;i++)
        {
            setValue(comps.get(i), data);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause()
    {
        super.onPause();
        isActive.set(false);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();

        isActive.set(true);
    }
    
    /**
     * @param activity
     * @param autoCompTextField
     */
    public void hookupAutoCompTextField(final Activity activity, 
                                        final AutoCompleteTextView autoCompTextField)
    {
        SimpleCursorAdapter adapter2 = new SimpleCursorAdapter(activity,
                android.R.layout.simple_dropdown_item_1line, null, new String[] { "Name" },
                new int[] { android.R.id.text1 });
        
        adapter2.setCursorToStringConverter(new HistoryCursorConverter());
        
        if (SQLUtils.getCount(getDB(), "SELECT COUNT(*) FROM taxon") > 0)
        {
            adapter2.setFilterQueryProvider(new FilterQueryProvider()
            {
                @Override
                public Cursor runQuery(final CharSequence constraint)
                {
                    if (constraint != null && constraint.length() > 0)
                    {
                        String sql = "SELECT _id, Name FROM taxon WHERE Name LIKE '"+constraint+"%'";
                        //Log.d("T", sql);
                        return getDB().rawQuery(sql, null);
                    }
                    return null;
                }
            });
            autoCompTextField.setAdapter(adapter2);
        }
    }
    
    //-----------------------------------------------------------
    class CalClickListener implements View.OnClickListener
    {
        private TextView txtView;
        
        /**
         * @param txtView
         */
        public CalClickListener(final TextView txtView)
        {
            super();
            this.txtView = txtView;
        }

        /* (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v)
        {
            calTextView = txtView;
            showDialog(0);
        }
    }

    //--------------------------------------------------------------------
    public class HistoryCursorConverter implements CursorToStringConverter
    {
        public CharSequence convertToString(final Cursor theCursor)
        {
            // Return the first column of the database cursor
            String aColumnString = theCursor.getString(1);
            return aColumnString;
        }
    } 
}
