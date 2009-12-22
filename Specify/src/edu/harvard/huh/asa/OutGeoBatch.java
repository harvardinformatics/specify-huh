package edu.harvard.huh.asa;

import edu.harvard.huh.asa.Transaction.REQUEST_TYPE;
import edu.harvard.huh.asa.Transaction.TYPE;

public class OutGeoBatch extends CountableBatch
{	
    private TYPE   type;
    private REQUEST_TYPE requestType;
	private String geoUnit;
	
	public TYPE getType() { return type; }
	
	public REQUEST_TYPE getRequestType() { return requestType; }
	
	public String getGeoUnit() { return geoUnit; }
		
	public void setType(TYPE type) { this.type = type; }
	
	public void setRequestType(REQUEST_TYPE requestType) { this.requestType = requestType; }
	
	public void setGeoUnit(String geoUnit) { this.geoUnit = geoUnit; }
	
    /**
     * "Quantity includes [itemCount] general collections, [nonSpecimenCount] non-specimens,
     *  and [typeCount] types."
     */
    @Override
    public String getItemCountNote()
    {        
        Integer itemCount = getItemCount();
        Integer typeCount = getTypeCount();
        Integer nonSpecimenCount = getNonSpecimenCount();
        
        if (itemCount == null) itemCount = 0;
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        String itemCountNote = itemCount + " general collection" + (itemCount == 1 ? "" : "s");
        String nonSpecimenNote = nonSpecimenCount + " non-specimen" + (nonSpecimenCount == 1 ? "" : "s");
        String typeNote = typeCount + " type" + (typeCount == 1 ? "" : "s");
        
        return "Quantity includes " +  itemCountNote + ", " + 
               nonSpecimenNote + ", and " + typeNote + ".";
    }
}
