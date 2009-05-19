package edu.harvard.huh.asa;

import java.math.BigDecimal;

public class Site {
	
	private int id;
	
	private Integer     geoUnitId;
	private String      locality;
	private String      latLongMethod;
	private BigDecimal  latitudeA;
	private BigDecimal  longitudeA;
	private BigDecimal  latitudeB;
	private BigDecimal  longitudeB;
	private BigDecimal  elevFrom;
	private BigDecimal  elevTo;
	private String      elevMethod; // not in test asa
	private Integer     disciplineId;
	
	public Site() { ; }
		 
	public int getId() { return id; }
	
	public Integer getGeoUnitId() { return geoUnitId; }
	
	public String getLocality() { return locality; }
	
	public String getLatLongMethod() {
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
	
	public String getElevMethod() {
	    if (elevTo == null && elevFrom == null) {
	        return null;
	    }
	    else {
	        return elevMethod;
	    }
	}
	
	public Integer getDisciplineId() { return disciplineId; }
	
	public boolean hasData() {
	    return locality   != null ||
	           latitudeA  != null ||
	           longitudeB != null ||
	           latitudeB  != null ||
	           longitudeB != null ||
	           elevTo     != null ||
	           elevFrom   != null;
	}
	
	public void setId(int id) { this.id = id; }
	
	public void setGeoUnitId(Integer geoUnitId) { this.geoUnitId = geoUnitId; }
	
	public void setLocality(String locality) { this.locality = locality; }
	
	public void setMethod(String latLongMethod) { this.latLongMethod = latLongMethod; }
	
	public void setLatitudeA(BigDecimal latitudeA) { this.latitudeA = latitudeA; }
	
	public void setLongitudeA(BigDecimal longitudeA) { this.longitudeA = longitudeA; }
	
	public void setLatitudeB(BigDecimal latitudeB) { this.latitudeB = latitudeB; }
	
	public void setLongitudeB(BigDecimal longitudeB) { this.longitudeB = longitudeB; }
	
	public void setElevFrom(BigDecimal elevFrom) { this.elevFrom = elevFrom; }
	
	public void setElevTo(BigDecimal elevTo) { this.elevTo = elevTo; }
	
	public void setElevMethod(String elevMethod) { this.elevMethod = elevMethod; }
	
	public void setDisciplineId(Integer disciplineId) { this.disciplineId = disciplineId; }
}
