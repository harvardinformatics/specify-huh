package edu.harvard.huh.asa;

public class Organization extends AuditedObject
{
    // These are the ids of Asa organization records that represent private individuals; they each have redundant Asa agent records
    // whose parent organization represents them
    public final static int[] SelfOrganizations = { 1047,1055,1057,1072,1081,1083,1084,1125,1158,1162,1178,
        1191,1196,1214,1245,1260,1287,1302,1312,1334,1354,1358,1359,1383,1388,1396,1398,1420,1425,1460,1465,
        1481,1490,1500,1505,1511,1513,1556,1573,1607,1611,1620,1654,1658,1665,1674,1678,1689,1701,1703,1755,
        1773,1774,1804,1811,1819,1865,1878,1918,1977,2041,2046,2077,2097,2112,2130,2134,2138,2142 };
        
    public static boolean IsSelfOrganizing(int organizationId)
    {
        for (int selfOrganizationId : SelfOrganizations)
        {
            if (selfOrganizationId == organizationId) return true;
        }
        return false;
    }
    
	private  String name;
	private  String acronym;
	private  String city;
	private  String state;
	private  String country;
	private  String uri;
	private  String remarks;
	
	public String getName() { return name; }
	
	public String getAcronym() { return acronym; }
	
	public String getCity() { return city; }
	
	public String getState() { return state; }
	
	public String getCountry() { return country; }
	
	public String getUri() { return uri; }

	public String getRemarks() { return remarks; }
	
	public void setName(String name) { this.name = name; }
	
	public void setAcronym(String acronym) { this.acronym = acronym; }
	
	public void setCity(String city) { this.city = city; }
	
	public void setState(String state) { this.state = state; }
	
	public void setCountry(String country) { this.country = country; }
	
	public void setUri(String uri) { this.uri = uri; }
    
	public void setRemarks(String remarks) { this.remarks = remarks; }
}
