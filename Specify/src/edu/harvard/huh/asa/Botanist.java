package edu.harvard.huh.asa;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Botanist extends AuditedObject
{    
    // TODO: dates type: no place to put this at the moment: birth/death, flourished, collected, received specimens
    // TODO: date precision: doing nothing with this at the moment: ?, circa; null means default, exact

	private Integer id;	
	private Boolean isTeam;
	private Boolean isCorporate;
	private  String name;
	private  String datesType;
	private Integer startYear;
	private  String startPrecision;
    private Integer endYear;
	private  String endPrecision;
	private  String remarks;
	private  String uri;
	private  String authorNote;
	private  String collectorNote;
	
    private final static Pattern groupPattern = Pattern.compile("&|et al| and ");
    private final static Pattern orgPattern   = Pattern.compile("Bureau of|Commission|Committee|Consortium|Department|Expedition|Group|Herbarium|Missionaries|Museum|National|Nurseries|Nursery|Program|Research|School|Scientific|Service|Society|Survey|University");
	
	public Integer getId() { return id; }

	public Boolean isTeam() { return isTeam; }

	public Boolean isCorporate() { return isCorporate; }

	public String getName() { return name; }

	public String getDatesType() { return datesType; }

	public Integer getStartYear() { return startYear; }
	
	public String getStartPrecision() { return startPrecision; }

	public Integer getEndYear() { return endYear; }
	
	public String getEndPrecision() { return endPrecision; }
	
	public String getRemarks() { return remarks; }
	
	public String getUri() { return uri; }
	
	public String getAuthorNote() { return authorNote; }
	
	public String getCollectorNote() { return collectorNote; }
		
	public void setId(Integer id) { this.id = id; }
	
	public void setIsTeam(Boolean isTeam) { this.isTeam = isTeam; }
	    
	public void setIsCorporate(Boolean isCorporate) { this.isCorporate = isCorporate; }
	
	public void setName(String name) { this.name = name; }
	
	public void setDatesType(String datesType) { this.datesType = datesType; }
	
    public void setStartPrecision(String startPrecision) { this.startPrecision = startPrecision; }
    
    public void setStartYear(Integer startYear) { this.startYear = startYear; }
    
	public void setEndYear(Integer endYear) { this.endYear = endYear; }

	public void setEndPrecision(String endPrecision) { this.endPrecision = endPrecision; }
    
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void setUri(String uri) { this.uri = uri; }
	
	public void setAuthorNote(String authorNote) { this.authorNote = authorNote; }
	
	public void setCollectorNote(String collectorNote) { this.collectorNote = collectorNote; }
	
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
	    if (name == null) return isCorporate;
	    
	    else return isCorporate || isOrg(name);
	}
	
	public boolean isGroup()
	{
	    if (name == null) return isCorporate;
	    
	    return isTeam || isGroup(name);
	}
	
	public boolean isPerson()
	{
	    return !isOrganization() && !isGroup();
	}
	
    private boolean isGroup( String name )
    { 
        Matcher m = groupPattern.matcher( name );
        return m.matches();
    }
    
    private boolean isOrg( String name )
    {   
        Matcher m = orgPattern.matcher( name );
        return m.matches();
    }
}
