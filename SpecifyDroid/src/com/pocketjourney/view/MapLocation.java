package com.pocketjourney.view;

import com.google.android.maps.GeoPoint;

/** Class to hold our location information */
/**
 * @author Anthony (Acopernicus)
 *
 * @code_status Alpha
 *
 * Nov 20, 2009
 *
 */
public class MapLocation {

	private GeoPoint	point;
    private String      name;
    private String      desc;

	public MapLocation(String name, String desc, double latitude, double longitude) {
        this.name = name;
        this.desc = desc;
		point = new GeoPoint((int)(latitude*1e6),(int)(longitude*1e6));
	}

	public GeoPoint getPoint() {
		return point;
	}

	public String getName() {
		return name;
	}

    /**
     * @return the desc
     */
    public String getDesc()
    {
        return desc;
    }
	
	
}
