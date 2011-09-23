package com.pocketjourney.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomButtonsController;

import com.google.android.maps.MapView;

import edu.ku.brc.specifydroid.R;
import edu.ku.brc.specifydroid.TripMapLocationActivity;

/**
 * @author Anthony (Acopernicus)
 *
 * @code_status Alpha
 *
 * Nov 20, 2009
 *
 */
public class MapLocationViewer extends LinearLayout {

	protected MapLocationOverlay overlay;
	
    //  Known latitude/longitude coordinates that we'll be using.
	protected List<MapLocation> mapLocations;
    
	protected MapView  mapView;
	protected TextView satBtn;
	protected boolean  isSatellite = true;
	
    
	/**
	 * @param context
	 * @param attrs
	 */
	public MapLocationViewer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MapLocationViewer(Context context) {
		super(context);
		init();
	}

	/**
	 * 
	 */
	public void init() {		

		setOrientation(VERTICAL);
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));

        mapView = new MapView(getContext(),"0JCMEdmz3F_ggW_XsZILSwgwJm4CyKrEwksYBGg"); // debug
        //mapView = new MapView(getContext(),"0JCMEdmz3F_jjwD-dDDwr9rb_fzQN-kn9Aj3bkQ");
        
        //String mapKey = VersionChecker.isInDebugMode(getContext()) ? "0JCMEdmz3F_ggW_XsZILSwgwJm4CyKrEwksYBGg" :
        //    "0JCMEdmz3F_jjwD-dDDwr9rb_fzQN-kn9Aj3bkQ";

		mapView.setEnabled(true);
		mapView.setClickable(true);
		addView(mapView);
		
        mapView.setBuiltInZoomControls(true); 
        mapView.displayZoomControls(true);
        ZoomButtonsController zbc = mapView.getZoomButtonsController();
        zbc.setAutoDismissed(false);

		overlay = new MapLocationOverlay(this);
		mapView.getOverlays().add(overlay);

    	mapView.getController().setZoom(14);
    	
    	if (getMapLocations().size() > 0)
    	{
    	    mapView.getController().setCenter(getMapLocations().get(0).getPoint());
    	}
	}
	
	/**
	 * @param isSat
	 */
	protected void setSatellite(final boolean isSat)
	{
	    isSatellite = isSat;
        satBtn.setText(isSatellite ? String.format("   %s   ", getContext().getString(R.string.map)) :
                                     String.format("%s", getContext().getString(R.string.satellite)));
        mapView.setSatellite(isSatellite);
	}
	
	/**
	 * 
	 */
	protected void initUI()
	{
        if (satBtn == null)
        {
            TripMapLocationActivity tmlAct = ((TripMapLocationActivity)getContext());
            satBtn = (TextView)tmlAct.findViewById(R.id.sat_terrain_btn);
            if (satBtn != null)
            {
                setSatellite(mapView.isSatellite());
                
                satBtn.setTextColor(Color.DKGRAY);
                satBtn.setBackgroundColor(Color.argb(128, 200, 200, 200));
                
                satBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        setSatellite(!mapView.isSatellite());
                    }
                });
            }
        }
	}
	
	/* (non-Javadoc)
     * @see android.view.View#onFinishInflate()
     */
    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        
        initUI();
    }

    /* (non-Javadoc)
     * @see android.widget.LinearLayout#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        initUI();
        super.onLayout(changed, l, t, r, b);
    }

    /* (non-Javadoc)
     * @see android.view.View#onRestoreInstanceState(android.os.Parcelable)
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        super.onRestoreInstanceState(state);
    }

    public List<MapLocation> getMapLocations() {
		if (mapLocations == null) {
			mapLocations = new ArrayList<MapLocation>();
		}
		return mapLocations;
	}

	public MapView getMapView() {
		return mapView;
	}
}
