package edu.harvard.huh.asa;

import java.math.BigDecimal;

public class Site
{	
    public static enum ELEV_METHOD            {  GPS,   Altimeter,   Other,   Unknown  };
    private static String[] ElevMethodNames = { "GPS", "altimeter", "other", "unknown" };
    
    public static enum LATLON_METHOD            {  GPS,   Gazetteer,   Other,   Unknown  };
    private static String[] LatLonMethodNames = { "GPS", "gazetteer", "other", "unknown" };
    
    public static enum MAP_DATUM            {  WGS84,    Other,   Unknown  };
    private static String[] MapDatumNames = { "WGS 84", "other", "unknown" };
    
	private Integer       id;
	private Integer       geoUnitId;
	private String        locality;
	private LATLON_METHOD latLongMethod;
	private BigDecimal    latitudeA;
	private BigDecimal    longitudeA;
	private BigDecimal    latitudeB;
	private BigDecimal    longitudeB;
	private BigDecimal    elevFrom;
	private BigDecimal    elevTo;
	private ELEV_METHOD   elevMethod;
	private Integer       disciplineId;
		 
	public static ELEV_METHOD parseElevMethod(String string) throws AsaException
	{
	    if (string == null) return null;

	    for (ELEV_METHOD elevMethod : ELEV_METHOD.values())
	    {
	        if (ElevMethodNames[elevMethod.ordinal()].equals(string)) return elevMethod;
	    }
	    throw new AsaException("Invalid elevation method: " + string);
	}

	public static LATLON_METHOD parseLatLongMethod(String string) throws AsaException
	{
	    if (string == null) return null;

	    for (LATLON_METHOD latLonMethod : LATLON_METHOD.values())
	    {
	        if (LatLonMethodNames[latLonMethod.ordinal()].equals(string)) return latLonMethod;
	    }
	    throw new AsaException("Invalid lat/long method: " + string);
	}
	   
	public static MAP_DATUM parseMapDatum(String string) throws AsaException
    {
        if (string == null) return null;

        for (MAP_DATUM mapDatum : MAP_DATUM.values())
        {
            if (MapDatumNames[mapDatum.ordinal()].equals(string)) return mapDatum;
        }
        throw new AsaException("Invalid map datum method: " + string);
    }
	
	public static String toString(ELEV_METHOD elevMethod)
	{
	    if (elevMethod == null) return null;
	    return elevMethod.name();
	}
	
	public static String toString(LATLON_METHOD latLonMethod)
    {
	    if (latLonMethod == null) return null;
        return latLonMethod.name();
    }
	
	public static String toString(MAP_DATUM mapDatum)
    {
	    if (mapDatum == null) return null;
        return mapDatum.name();
    }
	
	public int getId() { return id; }
	
	public Integer getGeoUnitId() { return geoUnitId; }
	
	public String getLocality() { return locality; }
	
	public LATLON_METHOD getLatLongMethod() {
        if (latitudeA == null && latitudeB == null && longitudeA == null && longitudeB == null) {
            return null;
        }
        else {
            return latLongMethod;
        }
    }
	
	public BigDecimal getLatitudeA() { return latitudeA; }
	
	public BigDecimal getLongitudeA() { return longitudeA; }
	
	public BigDecimal getLatitudeB() { return latitudeB; }
	
	public BigDecimal getLongitudeB() { return longitudeB; }
	
	public BigDecimal getElevFrom() { return elevFrom; }
	
	public BigDecimal getElevTo() { return elevTo; }
	
	public ELEV_METHOD getElevMethod() {
	    if (elevTo == null && elevFrom == null) {
	        return null;
	    }
	    else {
	        return elevMethod;
	    }
	}
	
	public Integer getDisciplineId() { return disciplineId; }
	
	public boolean hasData() {
	    return geoUnitId  != null ||
	           locality   != null ||
	           latitudeA  != null ||
	           longitudeA != null ||
	           latitudeB  != null ||
	           longitudeB != null ||
	           elevTo     != null ||
	           elevFrom   != null;
	}
	
	public void setId(int id) { this.id = id; }
	
	public void setGeoUnitId(Integer geoUnitId) { this.geoUnitId = geoUnitId; }
	
	public void setLocality(String locality) { this.locality = locality; }
	
	public void setMethod(LATLON_METHOD latLongMethod) { this.latLongMethod = latLongMethod; }
	
	public void setLatitudeA(BigDecimal latitudeA) { this.latitudeA = latitudeA; }
	
	public void setLongitudeA(BigDecimal longitudeA) { this.longitudeA = longitudeA; }
	
	public void setLatitudeB(BigDecimal latitudeB) { this.latitudeB = latitudeB; }
	
	public void setLongitudeB(BigDecimal longitudeB) { this.longitudeB = longitudeB; }
	
	public void setElevFrom(BigDecimal elevFrom) { this.elevFrom = elevFrom; }
	
	public void setElevTo(BigDecimal elevTo) { this.elevTo = elevTo; }
	
	public void setElevMethod(ELEV_METHOD elevMethod) { this.elevMethod = elevMethod; }
	
	public void setDisciplineId(Integer disciplineId) { this.disciplineId = disciplineId; }
}
