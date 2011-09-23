package edu.ku.brc.specifydroid;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import edu.ku.brc.specifydroid.datamodel.Trip;
import edu.ku.brc.specifydroid.datamodel.TripDataDef;
import edu.ku.brc.specifydroid.datamodel.TripDataDef.TripDataDefType;
import edu.ku.brc.utils.DialogHelper;
import edu.ku.brc.utils.SQLUtils;

public class TripDetailActivity extends SpBaseActivity implements DatePickerDialog.OnDateSetListener
{
    //private static final String ERR_SAV = "Error saving a new trip";
    private static final String ERR_INS = "Error inserting a new trip";
    private static final String ERR_UPD = "Error updating a new trip";
    
    public static final String   SPECIFYDROID_PREF = "edu.ku.brc.specifydroid.SPECIFYDROID";
    public static final String   DISP_KEY_PREF     = "edu.ku.brc.specifydroid.DISP_TYPE_ID";
    public final static String   ISNEW_EXTRA       = "edu.ku.brc.specifydroid.ISNEW_TRIP";
    
    private final static String  HAS_STARTED_NEW   = "edu.ku.brc.specifydroid.HASSTARTEDNEW";

    public final static String ID_EXTRA = "edu.ku.brc.specifydroid._ID";
    
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    private int[]          txtEdtIds   = {R.id.name,    R.id.notes};
    private String[]       txtEdtNames = {"name",       "notes"};


    private TextView       dateLbl  = null;
    private RadioGroup     types    = null;
    private Trip           current  = null;
    private String         tripId   = null;
    private Calendar       tripDate = Calendar.getInstance();
    
    private boolean        hasChanged    = false;
    private boolean        hasStartedNew;
    private boolean        isNew;
    private Integer        tripType      = null;
    private String         tripTitle     = null;
    
    private HashMap<Integer, EditText> editTexts = new HashMap<Integer, EditText>();

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public TripDetailActivity()
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
        
        if (savedInstanceState != null)
        {
            tripId        = savedInstanceState.getString(TripListActivity.ID_EXTRA);
            isNew         = savedInstanceState.getBoolean(TripDetailActivity.ISNEW_EXTRA, false);
            hasStartedNew = savedInstanceState.getBoolean(TripDetailActivity.HAS_STARTED_NEW, false);
            tripType      = savedInstanceState.getInt(TripListActivity.TRIP_TYPE, -1);
            tripTitle     = savedInstanceState.getString(TripListActivity.TRIP_TITLE);
            discpInx      = savedInstanceState.getInt("discpInx", 0);
            
        } else
        {
            tripId        = getIntent().getStringExtra(TripListActivity.ID_EXTRA);
            isNew         = getIntent().getBooleanExtra(TripDetailActivity.ISNEW_EXTRA, false);
            hasStartedNew = getIntent().getBooleanExtra(TripDetailActivity.HAS_STARTED_NEW, false);
            tripType      = getIntent().getIntExtra(TripListActivity.TRIP_TYPE, -1);
            tripTitle     = getIntent().getStringExtra(TripListActivity.TRIP_TITLE);
            discpInx      = 0;
        }
        
        if (tripTitle == null)
        {
            if (tripType == -1)
            {
                tripTitle = TripMainActivity.createTitle(this, TripListActivity.CONFIG_TRIP); // Shouldn't happen 
            } else
            {
                tripTitle = TripMainActivity.createTitle(this, tripType);
            }
        }
        
        disciplineId = discpIcons[discpInx];
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.detail_form);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        ((TextView)findViewById(R.id.headertitle)).setText(tripTitle);

        for (Integer id : txtEdtIds)
        {
            editTexts.put(id, (EditText)findViewById(id));
        }

        types   = (RadioGroup)findViewById(R.id.types);
        dateLbl = (TextView)findViewById(R.id.date);

        if (isNew && tripId == null)
        {
            current = new Trip();
            current.setTripDate(new java.sql.Date(Calendar.getInstance().getTime().getTime()));
            
            getDisciplineId(); // retrieves the both the index and the sets the discipline id
            current.setDiscipline(discpInx);
        }
        
        setDataIntoUI();
        
        if (savedInstanceState != null)
        {
            int i = 0;
            for (Integer id : txtEdtIds)
            {
                editTexts.get(id).setText(savedInstanceState.getString(txtEdtNames[i]));
                i++;
            }
            
            char[] str = savedInstanceState.getCharArray("date");
            if (str != null && str.length > 0)
            {
                dateLbl.setText(str, 0, str.length);
            }
        }
        
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                hasChanged = true;
            }
        };
        
        editTexts.get(R.id.name).addTextChangedListener(textWatcher);
        editTexts.get(R.id.name).setSingleLine();
        editTexts.get(R.id.notes).addTextChangedListener(textWatcher);
        
        View.OnClickListener rbListener = new View.OnClickListener() {
            public void onClick(View v) {
                hasChanged = true;
            }
        };
        
        RadioButton rb = (RadioButton)findViewById(R.id.collecting);
        rb.setOnClickListener(rbListener);
        
        rb = (RadioButton)findViewById(R.id.observing);
        rb.setOnClickListener(rbListener);
        
        ImageView calBtn = (ImageView) findViewById(R.id.calendarbtn);
        calBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                showDialog(0);
            }
        });
        
        ImageView dispImgView = (ImageView)findViewById(R.id.trpdispicon);
        
        dispImgView.setImageResource(discpIcons[discpInx]);
        
        Button changeDispBtn = (Button)findViewById(R.id.changedisp);
        if (changeDispBtn != null)
        {
            changeDispBtn.setVisibility(View.GONE);
        }
        
        Button edtFieldsBtn = (Button)findViewById(R.id.editfieldsbtn);
        edtFieldsBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent i = new Intent(TripDetailActivity.this, TripFieldsActivity.class);
                i.putExtra(TripDetailActivity.ID_EXTRA, String.valueOf(tripId));
                i.putExtra(TripListActivity.TRIP_TITLE, tripTitle);
                startActivity(i);
            }
        });
        
        Button doneBtn = (Button)findViewById(R.id.donebtn);
        doneBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                checkAndSave();
                TripDetailActivity.this.finish();
            }
        });
        
        if (isNew && !hasStartedNew)
        {
            checkAndSave();
            doChooseDiscipline();
            hasStartedNew = true;
            
            if (tripType > -1)
            {
                if (tripType == TripListActivity.OBS_TRIP)
                {
                    types.check(R.id.observing);
                } else
                {
                    types.check(R.id.collecting);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) 
    {
        return new DatePickerDialog(this,
                                    this,
                                    tripDate.get(Calendar.YEAR), 
                                    tripDate.get(Calendar.MONTH), 
                                    tripDate.get(Calendar.DAY_OF_MONTH));
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        checkAndSave();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        
        if (tripId != null)
        {
            savedInstanceState.putString(TripListActivity.ID_EXTRA, tripId);
        }

        savedInstanceState.putInt("type", types.getCheckedRadioButtonId() == R.id.observing ? TripListActivity.OBS_TRIP : TripListActivity.COLL_TRIP);
        savedInstanceState.putInt("discipline", disciplineId);
        savedInstanceState.putBoolean(TripDetailActivity.ISNEW_EXTRA, isNew);
        savedInstanceState.putBoolean(TripDetailActivity.HAS_STARTED_NEW, hasStartedNew);
        
        int i = 0;
        for (Integer id : txtEdtIds)
        {
            savedInstanceState.putString(txtEdtNames[i], editTexts.get(id).getText().toString());
            i++;
        }
    }
    
    /**
     * 
     */
    private void setDataIntoUI()
    {
        if (tripId != null)
        {   
            current      = Trip.getById(getDB(), tripId);
            discpInx     = current.getDiscipline();
            disciplineId = discpIcons[discpInx];
        }

        editTexts.get(R.id.name).setText(current.getName());
        editTexts.get(R.id.notes).setText(current.getNotes());
        
        /*editTexts.get(R.id.firstName1).setText(current.getFirstName1());
        editTexts.get(R.id.lastName1).setText(current.getLastName1());
        editTexts.get(R.id.firstName2).setText(current.getFirstName2());
        editTexts.get(R.id.lastName2).setText(current.getLastName2());
        editTexts.get(R.id.firstName3).setText(current.getFirstName3());
        editTexts.get(R.id.lastName3).setText(current.getLastName3());*/
        
        if (current.getType() == TripListActivity.OBS_TRIP)
        {
            types.check(R.id.observing);
        } else
        {
            types.check(R.id.collecting);
        }
        
        if (tripDate == null) tripDate = Calendar.getInstance();
        
        if (current.getTripDate() == null)
        {
            current.setTripDate(new java.sql.Date(tripDate.getTime().getTime()));
        }
        
        tripDate.setTime(current.getTripDate());
        
        char[] str = (new String(sdf.format(tripDate.getTime()))).toCharArray();
        dateLbl.setText(str, 0, str.length);

    }

    /* (non-Javadoc)
     * @see android.app.DatePickerDialog.OnDateSetListener#onDateSet(android.widget.DatePicker, int, int, int)
     */
    @Override
    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth)
    {
        tripDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        tripDate.set(Calendar.MONTH, monthOfYear);
        tripDate.set(Calendar.YEAR, year);
        
        char[] str = (new String(sdf.format(tripDate.getTime()))).toCharArray();
        dateLbl.setText(str, 0, str.length);
        
        hasChanged = true;
    }
    
    /**
     * 
     */
    private void checkAndSave()
    {
        if (tripId == null || hasChanged)
        {
            doSave();
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        checkAndSave();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        checkAndSave();
    }
    
    /**
     * 
     */
    private void doSave()
    {
        current.setName(editTexts.get(R.id.name).getText().toString());
        current.setNotes(editTexts.get(R.id.notes).getText().toString());
        
        editTexts.get(R.id.name).setNextFocusDownId(R.id.notes);
        
        /*current.setFirstName1(editTexts.get(R.id.firstName1).getText().toString());
        current.setLastName1(editTexts.get(R.id.lastName1).getText().toString());
        current.setFirstName2(editTexts.get(R.id.firstName2).getText().toString());
        current.setLastName2(editTexts.get(R.id.lastName2).getText().toString());
        current.setFirstName3(editTexts.get(R.id.firstName3).getText().toString());
        current.setLastName3(editTexts.get(R.id.lastName3).getText().toString());
        */
        
        current.setTripDate(new java.sql.Date(tripDate.getTime().getTime()));
        
        if (current.getTimestampCreated() == null)
        {
            current.setTimestampCreated(new Timestamp(Calendar.getInstance().getTime().getTime()));
        }
        current.setTimestampModified(new Timestamp(Calendar.getInstance().getTime().getTime()));
        
        switch (types.getCheckedRadioButtonId())
        {
            case R.id.collecting:
                current.setType(TripListActivity.COLL_TRIP);
                break;

            case R.id.observing:
                current.setType(TripListActivity.OBS_TRIP);
                break;
        }
        
        current.setDiscipline(discpInx);
        
        if (tripId == null)
        {
            current.setName(getString(R.string.new_trip));
            Long id = current.insert(getDB());
            if (id == -1)
            {
                DialogHelper.showDialog(TripDetailActivity.this, ERR_INS);
            }

            tripId = id.toString();
            doStdConfig(true, current.getType()); // true means do silently
            
        } else
        {
            long rv = current.update(tripId, getDB());
            if (rv != 1)
            {
                DialogHelper.showDialog(TripDetailActivity.this, ERR_UPD);
            }
        }
        
        hasChanged = false;
    }
    
    /**
     * @param doSilently
     */
    private void doStdConfig(final boolean doSilently, final int tripType)
    {
        String fileName = (tripType == TripListActivity.COLL_TRIP ? "trip" : "obs") + "_stdfields.xml";
        final Vector<TripDataDef> tripDataDefs = new Vector<TripDataDef>();
        if (!readStdFieldsXML(fileName, tripDataDefs))
        {
            if (!doSilently)
            {
                finish();
            }
            return;
        }
        
        final CharSequence[] items  = new CharSequence[tripDataDefs.size()];
        final boolean[]      values = new boolean[tripDataDefs.size()];
        
        int i = 0;
        for (TripDataDef tdd : tripDataDefs)
        {
            items[i]  = tdd.getTitle();
            values[i] = true;
            i++;
        }

        if (!doSilently)
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                        doStdPopulate(tripDataDefs, values);
                        TripDetailActivity.this.finish();
                        closeDB();
                    }
                    })
                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        TripDetailActivity.this.finish();
                    }
                    });
            
            builder.setTitle(R.string.chooseflds);
            builder.setMultiChoiceItems(items, values, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dlg, int which, boolean isChecked)
                {
                    values[which] = isChecked;
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            
        } else
        {
            doStdPopulate(tripDataDefs, values);
        }
    }

    /**
     * @param tripDataDefs
     * @param selected
     */
    private void doStdPopulate(final Vector<TripDataDef> tripDataDefs, final boolean[] selected)
    {
        String sql      = String.format("SELECT ColumnIndex FROM tripdatadef WHERE TripID = %s ORDER BY ColumnIndex DESC LIMIT 1", tripId);
        int    colIndex = SQLUtils.getCount(getDB(), sql);
        int    trpId    = Integer.parseInt(tripId);
        
        int i = 0;
        for (TripDataDef tdd : tripDataDefs)
        {
            if (selected[i])
            {
                sql = String.format("SELECT COUNT(*) AS count FROM tripdatadef WHERE TripID = %s AND Name = '%s'", tripId, tdd.getName());
                if (SQLUtils.getCount(getDB(), sql) < 1)
                {
                    colIndex++;
                    
                    tdd.setTripID(trpId);
                    tdd.setColumnIndex(colIndex);
                    
                    long _id = tdd.insert(getDB());
                    if (_id == -1)
                    {
                        DialogHelper.showDialog(TripDetailActivity.this, ERR_INS);
                        break;
                    }
                    
                    //Log.d("POPULATE", "_id: ["+_id+"] trpId: " + tdd.getTripID() +"  colIndex["+tdd.getColumnIndex()+"] Name: "+ tdd.getName() + "  Type: "+tdd.getDataType());
                }
            }
            i++;
        }
    }
    
    /**
     * @param tripDataDefs
     * @return
     */
    private boolean readStdFieldsXML(final String fileName, final Vector<TripDataDef> tripDataDefs)
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder docBldr = dbf.newDocumentBuilder();
            InputStream     fis     = getAssets().open(fileName);
            Document        doc     = docBldr.parse(fis);

            Element topElement = doc.getDocumentElement();

            NodeList tddElements = topElement.getElementsByTagName("field");

            for (int elementIndex = 0; elementIndex < tddElements.getLength(); elementIndex++)
            {
                Element tddEle = (Element) tddElements.item(elementIndex);
                TripDataDef tdd = new TripDataDef();

                if (tddEle.hasAttributes())
                {
                    NamedNodeMap attributes = tddEle.getAttributes();
                    String title = attributes.getNamedItem("title").getNodeValue();
                    String name  = attributes.getNamedItem("name").getNodeValue();
                    String type  = attributes.getNamedItem("type").getNodeValue();
                    tdd.setTitle(title);
                    tdd.setName(name);
                    tdd.setDataType((short)TripDataDefType.valueOf(type).ordinal());
                }
                tripDataDefs.add(tdd);
            }
            
            return true;
            
        } catch (Exception e)
        {
            Log.e("xml_perf", "DOM parser failed", e);
        }
        
        return false;
    }

    //------------------------------------------------------------------------------
    //-- Choosing Discipline Code
    //------------------------------------------------------------------------------
    private Integer disciplineId = null;
    private Integer discpInx     = null;
    
    /**
     * 
     */
    private void doChooseDiscipline()
    {
        // Need handler for callbacks to the UI thread
        final Handler mHandler = new Handler();

        // Create runnable for posting
        final Runnable mUpdateResults = new Runnable() {
            public void run() 
            {
                chooseDiscipline();
            }
        };
        
        Thread t = new Thread() {
            public void run() {
                mHandler.post(mUpdateResults);
            }
        };
        t.start();
    }
   
    /**
     * 
     */
    protected void chooseDiscipline()
    {
        ListAdapter adapter = new ArrayAdapter<Integer>(getApplicationContext(), R.layout.icon_title_row, discpIcons) 
        {
            ViewHolder holder;
            
            class ViewHolder 
            {
                ImageView icon;
                TextView title;
            }

            /* (non-Javadoc)
             * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
             */
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) 
                {
                    convertView = inflater.inflate(R.layout.icon_title_row, null);

                    holder       = new ViewHolder();
                    holder.icon  = (ImageView) convertView.findViewById(R.id.icon);
                    holder.title = (TextView) convertView.findViewById(R.id.title);
                    convertView.setTag(holder);
                    
                } else 
                {
                    holder = (ViewHolder) convertView.getTag();
                }       

                Drawable tile = getResources().getDrawable(discpIcons[position]); //this is an image from the drawables discipline
                
                holder.title.setText(dispNames[position]);
                holder.icon.setImageDrawable(tile);

                return convertView;
            }
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choosedisp);
        builder.setAdapter(adapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index)
                    {
                        SharedPreferences settings = getSharedPreferences(SPECIFYDROID_PREF, MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(DISP_KEY_PREF, index);
                        editor.commit();
                        dialog.dismiss();
                        
                        discpInx     = index;
                        disciplineId = discpIcons[index];
                        ImageView dispImgView = (ImageView)TripDetailActivity.this.findViewById(R.id.trpdispicon);
                        TripDetailActivity.this.current.setDiscipline(index);
                        dispImgView.setImageResource(disciplineId);
                        
                        //Log.i(SpecifyActivity.class.getSimpleName(), "Disp:"+index);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * @return the disciplineId
     */
    public int getDisciplineId()
    {
        if (disciplineId == null)
        {
            SharedPreferences settings = getSharedPreferences(SPECIFYDROID_PREF, MODE_PRIVATE);
            discpInx     = settings.getInt(DISP_KEY_PREF, 0);
            disciplineId = discpIcons[discpInx];
            
            //Log.i(SpecifyActivity.class.getSimpleName(), "*Disp:"+discpInx+"  "+disciplineId);
        }
        return disciplineId;
    }

    /**
     * @return the discpicons
     */
    public static Integer[] getDiscpIcons()
    {
        return discpIcons;
    }

    static final Integer[] discpIcons = {
        R.drawable.flower, // B
        R.drawable.lower_plant, // B
        R.drawable.bug,   // E
        R.drawable.spider, // E
        R.drawable.frog, // H
        R.drawable.lizard, // H
        R.drawable.snake, // H
        R.drawable.fish,  // I
        R.drawable.seahorse, // I
        R.drawable.starfish, // Invert
        R.drawable.mammal, // M
        R.drawable.bird,  // O
        R.drawable.paleo_bot, // P
        R.drawable.paleo_invert, // P
        R.drawable.paleo_vert, // P
    };

    static final int[] dispNames = {
        R.string.botany,
        R.string.botany,
        R.string.insect, 
        R.string.insect,
        R.string.herpetology, 
        R.string.herpetology,
        R.string.herpetology,
        R.string.fish, 
        R.string.fish,
        R.string.invertebrate,
        R.string.mammal,
        R.string.bird, 
        R.string.paleobotany,
        R.string.invertpaleo,
        R.string.vertpaleo,
        };

}