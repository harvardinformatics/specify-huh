package edu.harvard.huh.asa;

import java.util.HashSet;
import java.util.Set;

import edu.ku.brc.specify.datamodel.Geography;

public class GeoUnit extends AuditedObject
{
    public static final Geography NullGeography = new Geography();
    
    public static final int CultRegionId = 10295;
    public static final int MiscRegionId = 10297;
    
    public static final String Cultivated = "Cultivated";
    public static final String Miscellaneous = "Miscellaneous";
    
    private     Integer parentId;
	private     Integer id;
    private      String rank;
    private      String isoCode;
    private      String displayQualifier;
    private      String name;
    private      String vernacularName;
	private      String remarks;

	private Set<String> variantNames = new HashSet<String>();
    
    public Integer getParentId() { return parentId; }
    
	public Integer getId() { return id; }
	
	public String getName() { return name; }
	
	public String getIsoCode() { return isoCode; }
	
	public String getVernacularName() { return vernacularName; }
	
	public String getDisplayQualifier() { return displayQualifier; }
	
	public String getRank() { return rank; }
	
	public String getRemarks() { return remarks; }
	
	public Set<String> getVariantNames() { return variantNames; }
	    
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    
	public void setId(Integer id) { this.id = id; }
	
	public void setName(String name) { this.name = name; }
	
	public void setVernacularName(String vernacularName) { this.vernacularName = vernacularName; }
	
	public void setDisplayQualifier(String displayQualifier) { this.displayQualifier = displayQualifier; }
	
	public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
	
	public void setRank(String rank) { this.rank = rank; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void addVariantName(String variantName)
	{
	    if (variantName != null) this.variantNames.add(variantName);
	}
}
