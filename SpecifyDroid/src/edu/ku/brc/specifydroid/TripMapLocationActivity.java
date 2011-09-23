package edu.ku.brc.specifydroid;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.pocketjourney.view.TransparentPanel;

public class TripMapLocationActivity extends MapActivity 
{
    /* (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.maplocviewer);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        
        String tripTitle;
        if (savedInstanceState != null)
        {
            tripTitle = savedInstanceState.getString(TripListActivity.TRIP_TITLE);
        } else
        {
            tripTitle = getIntent().getStringExtra(TripListActivity.TRIP_TITLE);
        }
        ((TextView)findViewById(R.id.headertitle)).setText(tripTitle);
        
        Paint innerPaint = new Paint();
        innerPaint.setARGB(0, 255, 255, 255); //gray
        innerPaint.setAntiAlias(true);

        TransparentPanel transPanel = (TransparentPanel)findViewById(R.id.transparent_panel);
        transPanel.setInnerPaint(innerPaint);
        transPanel.setBorderPaint(innerPaint);
    }

    /* (non-Javadoc)
     * @see com.google.android.maps.MapActivity#isRouteDisplayed()
     */
    @Override
    protected boolean isRouteDisplayed()
    {
        return false;
    }

}
