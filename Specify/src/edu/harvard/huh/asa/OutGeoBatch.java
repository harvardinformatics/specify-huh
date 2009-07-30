package edu.harvard.huh.asa;

import edu.harvard.huh.asa.Transaction.TYPE;

public class OutGeoBatch extends CountableBatch
{	
    private TYPE   type;
	private String geoUnit;
	
	public TYPE getType() { return type; }
	
	public String getGeoUnit() { return geoUnit; }
		
	public void setType(TYPE type) { this.type = type; }
	
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
        
        if (typeCount == null) typeCount = 0;
        if (nonSpecimenCount == null) nonSpecimenCount = 0;
        
        String itemCountNote = itemCount + " general collection" + (itemCount == 1 ? "" : "s");
        String nonSpecimenNote = nonSpecimenCount + " non-specimen" + (nonSpecimenCount == 1 ? "" : "s");
        String typeNote = typeCount + " type" + (typeCount == 1 ? "" : "s");
        
        return "Quantity includes " +  itemCountNote + ", " + 
               nonSpecimenNote + ", and " + typeNote + ".";
    }
}
