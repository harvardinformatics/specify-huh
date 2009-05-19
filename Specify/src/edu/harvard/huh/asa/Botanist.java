package edu.harvard.huh.asa;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Botanist {

    public final static int BFRANZONE  = 190672;
    public final static int BRACH      = 127603;
    public final static int BTAN       = 114612;
    public final static int CBEANS     = 189463;
    public final static int DBOUFFORD  = 101575;
    public final static int DPFISTER   = 111411;
    public final static int EPFISTER   = 129141;
    public final static int ESHAW1     = 101588;
    public final static int ESHAW2     = 150860;
    public final static int EWOOD      = 125914;
    public final static int EZACHARIAS = 189934;
    public final static int GLEWISG    = 187933;
    public final static int HALLING    = 138332;
    public final static int HKESNER    = 187731;
    public final static int IHAY       = 109002;
    public final static int JCACAVIO   = 126030;
    public final static int JDOLAN     = 124284;
    public final static int JMACKLIN   = 163557;
    public final static int KGANDHI    = 124885;
    public final static int KITTREDGE  = 144144;
    public final static int LLUKAS     = 121560;
    public final static int MACKLILN   = 129028;
    public final static int MPETERS    = 110401;
    public final static int MSCHMULL   = 187205;
    public final static int PWHITE     = 105902;
    public final static int ROMERO     = 100809;
    public final static int SDAVIES    = 145254;
    public final static int SHULTZ1    = 111761;
    public final static int SHULTZ2    = 172018;
    public final static int SKELLEY    = 108165;
    public final static int SLAGRECA   = 133139;    
    public final static int SLINDSAY   = 104616;
    public final static int SZABEL     = 187625;
    public final static int THIERS     = 121627;
    public final static int ZANONI     = 150824;

    
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
	private Integer createdById;
	private Date dateCreated;
	
    private final static Pattern groupPattern = Pattern.compile("&|et al| and ");
    private final static Pattern orgPattern   = Pattern.compile("Bureau of|Commission|Committee|Consortium|Department|Expedition|Group|Herbarium|Missionaries|Museum|National|Nurseries|Nursery|Program|Research|School|Scientific|Service|Society|Survey|University");
    
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
	
	public Integer getCreatedById() { return createdById; }
	
	public Date getDateCreated() { return dateCreated; }
		
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
	
	public void setCreatedById(Integer createdById) { this.createdById = createdById; }
	
	public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
	
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
	    return !isOrganization() && !isGroup();
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
