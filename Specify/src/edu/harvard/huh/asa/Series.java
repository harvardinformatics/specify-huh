package edu.harvard.huh.asa;

public class Series {

	private Integer id;
	private  String name;
	private  String abbreviation;
	private Integer institutionId;
	private  String note;
	
	public Series() { ; }
	
	public Integer getId() { return id; }
	
	public String getGuid() { return id + " series"; }
	
	public String getName() { return name; }
	
	public String getAbbreviation() { return abbreviation; }
	
	public Integer getInstitutionId() { return institutionId; }
	
	public String getNote() { return note; }
	
	public void setId(int id) { this.id = id; }
	
	public void setName(String name) { this.name = name; }
	
	public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
	
	public void setInstitutionId(Integer institutionId) { this.institutionId = institutionId; }
	
	public void setNote(String note) { this.note = note; }

}
