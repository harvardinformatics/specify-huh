package edu.harvard.huh.asa;

import java.util.Date;

public class AsaTaxon {
	public static enum TYPE { Algae, Diatoms, FungiLichens, Hepatics, Monera, Mosses, Vascular };

    // from st_lookup category 130
    String NON_CITES_TYPE = "[none]";
    String CITES_I_TYPE = "CITES I";
    String CITES_II_TYPE = "CITES II";
    String CITES_III_TYPE = "CITES III";

    // from st_lookup category 140
    String ALGAE_TYPE = "Algae";
    String DIATOMS_TYPE = "Diatoms";
    String FUNGI_TYPE = "Fungi & Lichens";
    String HEPATICS_TYPE = "Hepatics";
    String MONERA_TYPE = "Monera";
    String MOSSES_TYPE = "Mosses";
    String VASCULAR_TYPE = "Vascular plants";
    
	private Integer id;
	private  String author;
	private  String name;
	private  String fullName;
	private  String citesStatus;
	private Integer parentId;
	private  String rank;
	private  String remarks;
	private Integer createdById;
	private    Date dateCreated;
	
	public Integer getId() { return id; }
	
	public String getAuthor() { return author; }
	
	public String getName() { return name; }
	
	public String getFullName() { return fullName; }
	
	public String getCitesStatus() { return citesStatus; }
	
	public Integer getParentId() { return parentId; }
	
	public String getRank() { return rank; }
	
	public String getRemarks() { return remarks; }
	
	public Integer getCreatedById() { return createdById; }
	
	public Date getDateCreated() { return dateCreated; }
	
	public void setId(Integer id) { this.id = id; }
	
	public void setAuthor(String author) { this.author = author; }
	
	public void setName(String name) { this.name = name; }
	
	public void setFullName(String fullName) { this.fullName = fullName; }
	
	public void setCitesStatus(String citesStatus) { this.citesStatus = citesStatus; }
	
	public void setParentId(Integer parentId) { this.parentId = parentId; }
	
	public void setRank(String rank) { this.rank = rank; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void setCreatedById(Integer createdById) { this.createdById = createdById; }
	
	public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
