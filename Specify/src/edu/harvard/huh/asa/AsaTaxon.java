package edu.harvard.huh.asa;

public class AsaTaxon {
	public static enum TYPE { Algae, Diatoms, FungiLichens, Hepatics, Monera, Mosses, Vascular };

	private Integer id;
	private  String author;
	private  String name;
	private  String fullName;
	private  String citesStatus;
	private Integer parentId;
	private  String rank;
	private  String remarks;
	
	public Integer getId() { return id; }

	public String getAuthor() { return author; }
	
	public String getName() { return name; }
	
	public String getFullName() { return fullName; }
	
	public String getCitesStatus() { return citesStatus; }
	
	public Integer getParentId() { return parentId; }
	
	public String getRank() { return rank; }
	
	public String getRemarks() { return remarks; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setAuthor(String author) { this.author = author; }
	
	public void setName(String name) { this.name = name; }
	
	public void setFullName(String fullName) { this.fullName = fullName; }
	
	public void setCitesStatus(String citesStatus) { this.citesStatus = citesStatus; }
	
	public void setParentId(Integer parentId) { this.parentId = parentId; }
	
	public void setRank(String rank) { this.rank = rank; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
}
