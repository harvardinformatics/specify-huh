package edu.harvard.huh.asa;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Botanist {

	private int id;
	
	private String name;
	private String startPrecision;
	private Integer startYear;
	private String endPrecision;
	private Integer endYear;
	private String datesType;
	private boolean activeFlag;
	private boolean isTeam;
	private boolean isCorporate;
	private String remarks;
	
    private final Pattern groupPattern = Pattern.compile("&|et al| and ");
    private final Pattern orgPattern   = Pattern.compile("Bureau of|Commission|Committee|Consortium|Department|Expedition|Group|Herbarium|Missionaries|Museum|National|Nurseries|Nursery|Program|Research|School|Scientific|Service|Society|Survey|University");
    
	public Botanist() { 
	    ;
	}
	
	public int getId() { return id; }
	
	public Integer getStartYear() { return startYear; }
	
	public String getStartPrecision() { return startPrecision; }

	public Integer getEndYear() { return endYear; }
	
	public String getEndPrecision() { return endPrecision; }
	
	public String getDatesType() { return datesType; }
	
	public boolean isActive() { return activeFlag; }
	
	public boolean isTeam() { return isTeam; }
	
	public boolean isCorporate() { return isCorporate; }
	
	public String getName() { return name; }
	
	public String getRemarks() { return remarks; }
		
	public String getGuid() { return id + " " + "botanist"; }
		
	public void setId(int id) { this.id = id; }
	
	public void setName(String name) { this.name = name; }
	
	public void setStartPrecision(String startPrecision) { this.startPrecision = startPrecision; }
	
	public void setStartYear(Integer startYear) { this.startYear = startYear; }
	
	public void setEndPrecision(String endPrecision) { this.endPrecision = endPrecision; }
	
	public void setEndYear(Integer endYear) { this.endYear = endYear; }
	
	public void setDatesType(String datesType) { this.datesType = datesType; }
	
	public void setActive(boolean isActive) { this.activeFlag = isActive; }
	
	public void setTeam(boolean isTeam) { this.isTeam = isTeam; }
	
	public void setCorporate(boolean isCorporate) { this.isCorporate = isCorporate; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public String getFirstName()
	{
        if (isPerson() && name.contains( "," ) )
        {
            // If there's at least one comma, the first name is everything after the comma
            int commaIndex = name.indexOf( ',' );

            return name.substring( commaIndex + 1 ).trim();  
        }
        else
        {
            return null;
        }
	}
	
	public String getLastName()
	{
        if (isPerson() && name.contains( "," ) )
        {
            int commaIndex = name.indexOf( ',' );

            // If there's at least one comma, the last name is everything before the comma
            return name.substring( 0, commaIndex ).trim();
        }
        else
        {
            return name;
        }
	}
    
	public boolean isOrganization()
	{
	    return isCorporate || isOrg(name);
	}
	
	public boolean isGroup()
	{
	    return isTeam || isGroup(name);
	}
	
	public boolean isPerson()
	{
	    return !isCorporate && !isTeam;
	}
	
    private boolean isGroup( String name ) {
        
        Matcher m = groupPattern.matcher( name );
        return m.matches();
    }
    
    private boolean isOrg( String name ) {
        Matcher m = orgPattern.matcher( name );
        return m.matches();
    }
}
