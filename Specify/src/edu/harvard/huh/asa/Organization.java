package edu.harvard.huh.asa;

public class Organization {

	private Integer id;
	private  String name;
	private  String acronym;
	private  String city;
	private  String state;
	private  String country;
	private  String uri;
	private  String remarks;
	
	public Organization() {
		;
	}
	
	public Integer getId() { return id; }
	
	public String getGuid() { return id + " " + "org"; }
	
	public String getName() { return name; }
	
	public String getAcronym() { return acronym; }
	
	public String getCity() { return city; }
	
	public String getState() { return state; }
	
	public String getCountry() { return country; }
	
	public String getUri() { return uri; }
	
	public String getRemarks() { return remarks; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setName(String name) { this.name = name; }
	
	public void setAcronym(String acronym) { this.acronym = acronym; }
	
	public void setCity(String city) { this.city = city; }
	
	public void setState(String state) { this.state = state; }
	
	public void setCountry(String country) { this.country = country; }
	
	public void setUri(String uri) { this.uri = uri; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }

}
