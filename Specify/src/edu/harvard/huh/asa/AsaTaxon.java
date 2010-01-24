package edu.harvard.huh.asa;

public class AsaTaxon extends AuditedObject
{
    // from st_lookup category 129
    public static enum STATUS              {  NomLeg,      NomNov,      NomCons,      OrthCons,      NomRej,      NomSuperfl,      Synonym,    LaterHomonym,   OrthVar,      NomInvalid,      Unknown  };
    private static String[] StatusNames  = { "nom. leg.", "nom. nov.", "nom. cons.", "orth. cons.", "nom. rej.", "nom. superfl.", "synonym", "later homonym", "orth. var.", "nom. invalid.", "unknown" };

    // from st_lookup category 130
    public static enum ENDANGERMENT             { CitesI,    CitesII,    CitesIII,    None    };
    private static String[] EndangermentNames = {"CITES I", "CITES II", "CITES III", "[none]" };
    
    // from st_lookup category 140
    public static enum GROUP             {  Algae,   Diatoms,   FungiLichens,      Hepatics,   Monera,   Mosses,   Vascular         };
    private static String[] GroupNames = { "Algae", "Diatoms", "Fungi & Lichens", "Hepatics", "Monera", "Mosses", "Vascular plants" };

	public static STATUS parseStatus(String string) throws AsaException
	{
	    for (STATUS status : STATUS.values())
	    {
	        if (StatusNames[status.ordinal()].equals(string)) return status;
	    }
	    throw new AsaException("Invalid taxon status: " + string);
	}

    public static ENDANGERMENT parseEndangerment(String string) throws AsaException
    {
        if (string == null) return ENDANGERMENT.None;
        for (ENDANGERMENT endangerment : ENDANGERMENT.values())
        {
            if (EndangermentNames[endangerment.ordinal()].equals(string)) return endangerment;
        }
        throw new AsaException("Invalid endangerment status: " + string);
    }

    public static GROUP parseGroup(String string) throws AsaException
    {
        for (GROUP group : GROUP.values())
        {
            if (GroupNames[group.ordinal()].equals(string)) return group;
        }
        throw new AsaException("Invalid taxon group: " + string);
    }

    public static String toString(STATUS status)
    {
        return status.name();
    }
    
    public static String toString(ENDANGERMENT endangerment)
    {
        return endangerment.name();
    }

    public static String toString(GROUP group)
    {
        return group.name();
    }

    private      Integer parentId;
	private       String rank;
	private       String rankType;
	private       String rankAbbrev;
	private        GROUP group;
	private       STATUS status;
	private ENDANGERMENT endangerment;
	private      Boolean isHybrid;
	private       String fullName;
	private       String name;
	private       String author;
	private      Integer parAuthorId;
	private      Integer parExAuthorId;
	private      Integer stdAuthorId;
	private      Integer stdExAuthorId;
	private      Integer citInAuthorId;
	private      Integer citPublId;
	private       String citCollation;
	private       String citDate;
	private       String remarks;
	private       String basionym;
	private       String parAuthor;
	private       String parExAuthor;
	private       String stdAuthor;
	private       String stdExAuthor;
	private       String citInAuthor;
	private       String dataSource;
	
    public Integer getParentId() { return parentId; }
    
	public String getRank() { return rank; }
	
	public String getRankType() { return rankType; }
	
	public String getRankAbbrev() { return rankAbbrev; }
	
	public GROUP getGroup() { return group; }
	
	public STATUS getStatus() { return status; }
	
	public ENDANGERMENT getEndangerment() { return endangerment; }
	
	public Boolean isHybrid() { return isHybrid; }

	public String getFullName() { return fullName; }

	public String getName() { return name; }

	public String getAuthor() { return author; }
	
	public Integer getParAuthorId() { return parAuthorId; }
	
	public Integer getParExAuthorId() { return parExAuthorId; }
	
	public Integer getStdAuthorId() { return stdAuthorId; }
	
	public Integer getStdExAuthorId() { return stdExAuthorId; }
	
	public Integer getCitInAuthorId() { return citInAuthorId; }
	
	public Integer getCitPublId() { return citPublId; }
	
	public String getCitCollation() { return citCollation; }
	
	public String getCitDate() { return citDate; }

	public String getRemarks() { return remarks; }
	
	public String getBasionym() { return basionym; }
	
	public String getParAuthor() { return parAuthor; }
	
	public String getParExAuthor() { return parExAuthor; }
	
	public String getStdAuthor() { return stdAuthor; }
	
	public String getStdExAuthor() { return stdExAuthor; }
	
	public String getCitInAuthor() { return citInAuthor; }
	
	public String getDataSource() { return dataSource; }
    
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    
    public void setRank(String rank) { this.rank = rank; }
    
    public void setRankType(String rankType) { this.rankType = rankType; }
    
    public void setRankAbbrev(String rankAbbrev) { this.rankAbbrev = rankAbbrev; }
    
    public void setGroup(GROUP group) { this.group = group; }
    
    public void setStatus(STATUS status) { this.status = status; }
    
    public void setEndangerment(ENDANGERMENT endangerment) { this.endangerment = endangerment; }
    
    public void setIsHybrid(Boolean isHybrid) { this.isHybrid = isHybrid; }
	   
    public void setFullName(String fullName) { this.fullName = fullName; }
    
	public void setName(String name) { this.name = name; }

    public void setAuthor(String author) { this.author = author; }
    
    public void setParAuthorId(Integer parAuthorId) { this.parAuthorId = parAuthorId; }
    
    public void setParExAuthorId(Integer parExAuthorId) { this.parExAuthorId = parExAuthorId; }
    
    public void setStdAuthorId(Integer stdAuthorId) { this.stdAuthorId = stdAuthorId; }
    
    public void setStdExAuthorId(Integer stdExAuthorId) { this.stdExAuthorId = stdExAuthorId; }
    
    public void setCitInAuthorId(Integer citInAuthorId) { this.citInAuthorId = citInAuthorId; }
    
    public void setCitPublId(Integer citPublId) { this.citPublId = citPublId; }
    
    public void setCitCollation(String citCollation) { this.citCollation = citCollation; }
    
    public void setCitDate(String citDate) { this.citDate = citDate; }
	
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public void setBasionym(String basionym) { this.basionym = basionym; }
	
	public void setParAuthor(String parAuthor) { this.parAuthor = parAuthor; }
	
	public void setParExAuthor(String parExAuthor) { this.parExAuthor = parExAuthor; }
	
	public void setStdAuthor(String stdAuthor) { this.stdAuthor = stdAuthor; }
	
	public void setStdExAuthor(String stdExAuthor) { this.stdExAuthor = stdExAuthor; }
	
	public void setCitInAuthor(String citInAuthor) { this.citInAuthor = citInAuthor; }
	
	public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
