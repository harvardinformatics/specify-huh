package edu.harvard.huh.asa;

public class Botanist {

	private int id;
	
	private String name;
	private String startPrecision;
	private Integer startYear;
	private String endPrecision;
	private Integer endYear;
	private String datesType;
	private boolean activeFlag;
	private boolean teamFlag;
	private boolean corporateFlag;
	private String remarks;
	
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
	
	public boolean isTeam() { return teamFlag; }
	
	public boolean isCorporate() { return corporateFlag; }
	
	public String getName() { return name; }
	
	public String getRemarks() { return remarks; }
		
	public String getGuid() { return id + " " + "botanist"; }
	
	public boolean isPerson() {
		return !isCorporate() && !isTeam();
	}
	
	public void setId(int id) { this.id = id; }
	
	public void setName(String name) { this.name = name; }
	
	public void setStartPrecision(String startPrecision) { this.startPrecision = startPrecision; }
	
	public void setStartYear(Integer startYear) { this.startYear = startYear; }
	
	public void setEndPrecision(String endPrecision) { this.endPrecision = endPrecision; }
	
	public void setEndYear(Integer endYear) { this.endYear = endYear; }
	
	public void setDatesType(String datesType) { this.datesType = datesType; }
	
	public void setActive(boolean isActive) { this.activeFlag = isActive; }
	
	public void setTeam(boolean isTeam) { this.teamFlag = isTeam; }
	
	public void setCorporate(boolean isCorporate) { this.corporateFlag = isCorporate; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }

}
