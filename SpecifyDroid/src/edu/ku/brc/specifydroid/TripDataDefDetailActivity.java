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

import java.util.HashMap;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import edu.ku.brc.specifydroid.datamodel.TripDataDef;
import edu.ku.brc.utils.DialogHelper;
import edu.ku.brc.utils.SQLUtils;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Nov 20, 2009
 *
 */
public class TripDataDefDetailActivity extends SpBaseActivity implements AdapterView.OnItemSelectedListener
{
    public final static String ID_EXTRA     = "edu.ku.brc.specifydroid.TripDataDefDetailActivity._ID";
    
    public static final String[] dataTypeItems = new String[] {"Integer", "Double", "Float", "String", "Date"};
    
    private int[]          txtEdtIds   = {R.id.tddname, R.id.tddtitle,};
    private String[]       txtEdtNames = {"name",       "title"};

    private Spinner        dataTypeSP    = null;
    private TripDataDef    current       = null;
    private String         tripId        = null;
    private String         tripDataDefId = null;
    
    private HashMap<Integer, EditText> editTexts = new HashMap<Integer, EditText>();

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public TripDataDefDetailActivity()
    {
        super();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        String tripTitle;
        if (savedInstanceState != null)
        {
            tripId        = savedInstanceState.getString(TripListActivity.ID_EXTRA);
            tripDataDefId = savedInstanceState.getString(TripDataDefDetailActivity.ID_EXTRA);
            tripTitle = savedInstanceState.getString(TripListActivity.TRIP_TITLE);

        } else
        {
            tripId        = getIntent().getStringExtra(TripListActivity.ID_EXTRA);
            tripDataDefId = getIntent().getStringExtra(TripDataDefDetailActivity.ID_EXTRA);
            tripTitle = getIntent().getStringExtra(TripListActivity.TRIP_TITLE);
        }
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.tdd_detail_form);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        ((TextView)findViewById(R.id.headertitle)).setText(tripTitle);
        
        int i = 0;
        for (Integer id : txtEdtIds)
        {
            EditText edtTxt = (EditText) findViewById(id);
            editTexts.put(id, edtTxt);
            if (i < txtEdtIds.length-2)
            {
                edtTxt.setNextFocusDownId(txtEdtIds[i+1]);
            }
            i++;
        }

        ArrayAdapter<String> dataTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dataTypeItems);
        
        dataTypeSP = (Spinner)findViewById(R.id.tdddatatype);
        dataTypeSP.setOnItemSelectedListener(this);
        dataTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        dataTypeSP.setAdapter(dataTypeAdapter);
        
        Button save = (Button) findViewById(R.id.ttdsave);
        save.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doSave();
            }
        });

        if (tripDataDefId == null)
        {
            current = new TripDataDef();
        } else
        {
            load();
        }

        if (savedInstanceState != null)
        {
            i = 0;
            for (Integer id : txtEdtIds)
            {
                editTexts.get(id).setText(savedInstanceState.getString(txtEdtNames[i]));
                i++;
            }
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putShort("datatype", ((Integer)dataTypeSP.getSelectedItemPosition()).shortValue());
        
        savedInstanceState.putString(TripListActivity.ID_EXTRA, tripId);
        savedInstanceState.putString(TripDataDefDetailActivity.ID_EXTRA, tripDataDefId);
        savedInstanceState.putString(TripListActivity.TRIP_TITLE, ((TextView)findViewById(R.id.headertitle)).getText().toString());
        
        int i = 0;
        for (Integer id : txtEdtIds)
        {
            savedInstanceState.putString(txtEdtNames[i], editTexts.get(id).getText().toString());
            i++;
        }
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemSelected(AdapterView<?> adptView, View view, int position, long id)
    {
        
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView)
     */
    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
    }

    /**
     * 
     */
    private void load()
    {
        current = TripDataDef.getById(tripDataDefId, tripId, getDB());
        editTexts.get(R.id.tddname).setText(current.getName());
        editTexts.get(R.id.tddtitle).setText(current.getTitle());
        
        short inx = current.getDataType();
        dataTypeSP.setSelection(inx);
    }
    
    private void doSave()
    {
        int count = SQLUtils.getCount(getDB(), "SELECT COUNT(*) AS count FROM tripdatadef WHERE TripID = " + tripId);
        if (count > -1)
        {
            current.setName(editTexts.get(R.id.tddname).getText().toString());
            current.setTitle(editTexts.get(R.id.tddtitle).getText().toString());
            current.setDataType(((Integer)dataTypeSP.getSelectedItemPosition()).shortValue());
            
            if (tripDataDefId == null)
            {
                current.setTripID(Integer.parseInt(tripId));
                current.setColumnIndex(count);
                long rv = current.insert(getDB());
                if (rv == -1)
                {
                    DialogHelper.showDialog(TripDataDefDetailActivity.this, "Error inserting "+TripDataDefDetailActivity.this.getClass().getSimpleName());
                }
            } else
            {
                if (current.update(tripDataDefId, getDB()) == 0)
                {
                    DialogHelper.showDialog(TripDataDefDetailActivity.this, "Error updating "+TripDataDefDetailActivity.this.getClass().getSimpleName());
                }
            }
        }
        closeDB();
        finish();
    }
}