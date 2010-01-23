package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaTaxon;
import edu.harvard.huh.asa.AsaTaxon.ENDANGERMENT;
import edu.harvard.huh.asa.AsaTaxon.GROUP;
import edu.harvard.huh.asa.AsaTaxon.STATUS;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonCitation;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;

// Run this class after OptrLoader.

public class TaxonLoader extends TreeLoader
{
    private TaxonTreeDef treeDef;
	
	private HashMap <String, Integer> taxonRankIdsByType = new HashMap<String, Integer>();

	private HashMap <Integer, TaxonTreeDefItem> taxonDefItemsByRank = new HashMap<Integer, TaxonTreeDefItem>();
	
	private TaxonLookup       taxonLookup;
	private PublicationLookup publicationLookup;
	private BotanistLookup    botanistLookup;
	
	public TaxonLoader(File csvFile, Statement sqlStatement,
	                   PublicationLookup publicationLookup,
	                   BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);

		this.publicationLookup = publicationLookup;
		this.botanistLookup    = botanistLookup;
		
		this.treeDef = getTaxonTreeDef();
		
		// this must match config/botany/taxon_init.xml
		
		taxonRankIdsByType.put("life",             0);
		taxonRankIdsByType.put("kingdom",         10);
		taxonRankIdsByType.put("\"Major Group\"", 20);
		taxonRankIdsByType.put("division",        30);
		taxonRankIdsByType.put("class",           60);
		taxonRankIdsByType.put("order",          100);
		taxonRankIdsByType.put("family",         140);
		taxonRankIdsByType.put("tribe",          160);
		taxonRankIdsByType.put("genus",          180);
		taxonRankIdsByType.put("subgenus",       190);
		taxonRankIdsByType.put("species",        220);
		taxonRankIdsByType.put("subspecies",     230);
		taxonRankIdsByType.put("variety",        240);
		taxonRankIdsByType.put("nothovariety",   242);
		taxonRankIdsByType.put("nothomorph",     244);
		taxonRankIdsByType.put("subvariety",     250);
		taxonRankIdsByType.put("forma",          260);
		taxonRankIdsByType.put("subforma",       270);
		taxonRankIdsByType.put("lusus",          280);
		taxonRankIdsByType.put("modification",   290);
		taxonRankIdsByType.put("prolus",         300);
	}

	protected TaxonTreeDef getTaxonTreeDef() throws LocalException
	{
	    TaxonTreeDef t = new TaxonTreeDef();

	    Integer taxonTreeDefId = getId("taxontreedef", "TaxonTreeDefID", "Name", "Taxon");
	    t.setTaxonTreeDefId(taxonTreeDefId);

	    return t;
	}

	protected TaxonTreeDefItem getTreeDefItemByRankId(Integer rankId) throws LocalException
	{
	    TaxonTreeDefItem treeDefItem = taxonDefItemsByRank.get(rankId);

	    if (treeDefItem == null)
	    {
	        Integer taxonTreeDefItemId = getInt("taxontreedefitem", "TaxonTreeDefItemID", "RankID", rankId);

	        treeDefItem = new TaxonTreeDefItem();
	        treeDefItem.setTaxonTreeDefItemId(taxonTreeDefItemId);
	        taxonDefItemsByRank.put(rankId, treeDefItem);
	    } 

	    return treeDefItem;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		AsaTaxon asaTaxon = parse(columns);
		
		Integer asaTaxonId = asaTaxon.getId();
		setCurrentRecordId(asaTaxonId);
		
		Taxon taxon = getTaxon(asaTaxon);
		
		String rank = asaTaxon.getRank();
		checkNull(rank, "rank");
		
		Integer rankId = taxonRankIdsByType.get(rank);
		checkNull(rankId, "rank id");
		
		if (rankId <= taxonRankIdsByType.get("family"))
		{
		    getLogger().info("Processing " + taxon.getName());
		    if (frame != null)
		    {
		        frame.setDesc("Loading " + taxon.getName() + "...");
		    }
		}
		
		String sql = getInsertSql(taxon);
		Integer taxonId = insert(sql);
		taxon.setTaxonId(taxonId);
		
		// TaxonCitation
		Integer citPublId = asaTaxon.getCitPublId();
		if (citPublId != null)
		{
		    ReferenceWork parent = lookupPublication(citPublId);
		    ReferenceWork referenceWork = getReferenceWork(asaTaxon, parent);

		    sql = getInsertSql(referenceWork);
            Integer referenceWorkId = insert(sql);
            referenceWork.setReferenceWorkId(referenceWorkId);
		    
            // CitInAuthor
            Agent authorAgent = null;
            Integer citInAuthorId = asaTaxon.getCitInAuthorId();
            Integer stdAuthorId = asaTaxon.getStdAuthorId();
            
            if (citInAuthorId != null)
            {
                authorAgent = lookupBotanist(citInAuthorId);
            }
            else if (stdAuthorId != null)
            {
                authorAgent = lookupBotanist(stdAuthorId);
            }

            if (authorAgent != null)
            {
                Author author = getAuthor(authorAgent, referenceWork, 1);
                sql = getInsertSql(author);
                insert(sql);
            }

            TaxonCitation taxonCitation = getTaxonCitation(asaTaxon, taxon, referenceWork);

		    taxonCitation.setTaxon(taxon);
		    sql = getInsertSql(taxonCitation);
		    insert(sql);
		}
	}
	
	public void numberNodes() throws LocalException
	{
		numberNodes("taxon", "TaxonID");
	}
	
	public TaxonLookup getTaxonLookup()
	{
		if (taxonLookup == null)
		{
			taxonLookup = new TaxonLookup() {

				public Taxon getById(Integer asaTaxonId) throws LocalException
				{
					Taxon taxon = new Taxon();

					String taxonSerialNumber = getTaxonSerialNumber(asaTaxonId);

					Integer taxonId = getId("taxon", "TaxonID", "TaxonomicSerialNumber", taxonSerialNumber);

					taxon.setTaxonId(taxonId);

					return taxon;
				}
			};
		}

		return taxonLookup;
	}

	private AsaTaxon parse(String[] columns) throws LocalException
	{
	    if (columns.length < 25)
	    {
	        throw new LocalException("Not enough columns");
	    }

		AsaTaxon taxon = new AsaTaxon();

		try
		{
		    taxon.setFullName(                                 columns[0]  );
		    taxon.setParentId(              SqlUtils.parseInt( columns[1]  ));
		    taxon.setId(                    SqlUtils.parseInt( columns[2]  ));
		    taxon.setRank(                                     columns[3]  );
		    taxon.setRankType(                                 columns[4]  );
		    taxon.setRankAbbrev(                               columns[5]  );
		    taxon.setGroup(               AsaTaxon.parseGroup( columns[6]  ));
		    taxon.setStatus(             AsaTaxon.parseStatus( columns[7]  ));
		    taxon.setEndangerment( AsaTaxon.parseEndangerment( columns[8]  ));
		    taxon.setIsHybrid(           Boolean.parseBoolean( columns[9]  ));
		    //taxon.setFullName(                               columns[10] );
		    taxon.setName(                                     columns[11] );
		    taxon.setAuthor(                                   columns[12] );
		    taxon.setParAuthorId(           SqlUtils.parseInt( columns[13] ));
		    taxon.setParExAuthorId(         SqlUtils.parseInt( columns[14] ));
		    taxon.setStdAuthorId(           SqlUtils.parseInt( columns[15] ));
		    taxon.setStdExAuthorId(         SqlUtils.parseInt( columns[16] ));
		    taxon.setCitInAuthorId(         SqlUtils.parseInt( columns[17] ));
		    taxon.setCitPublId(             SqlUtils.parseInt( columns[18] ));
		    taxon.setCitCollation(                             columns[19] );
		    taxon.setCitDate(                                  columns[20] );
		    taxon.setRemarks(          SqlUtils.iso8859toUtf8( columns[21] ));
		    taxon.setCreatedById(           SqlUtils.parseInt( columns[22] ));
		    taxon.setDateCreated(          SqlUtils.parseDate( columns[23] ));
		    taxon.setBasionym(                                 columns[24] );
		}
		catch (NumberFormatException e)
		{
		    throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
		    throw new LocalException("Couldn't parse taxon field", e);
		}

		return taxon;
	}

	private Taxon getTaxon(AsaTaxon asaTaxon) throws LocalException
	{
		Taxon specifyTaxon = new Taxon();

		// Author
		String author = asaTaxon.getAuthor();
		specifyTaxon.setAuthor(author);
		
		// CITES Status TODO: cites status = none: insert none or leave null?
		ENDANGERMENT endangerment = asaTaxon.getEndangerment();
		specifyTaxon.setCitesStatus(AsaTaxon.toString(endangerment));

        // CreatedByAgent
        Integer creatorOptrId = asaTaxon.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        specifyTaxon.setCreatedByAgent(createdByAgent);
        
		// FullName
        String name = asaTaxon.getName();
        checkNull(name, "name");

        String fullName = asaTaxon.getFullName();
		checkNull(fullName, "full name");
		
		fullName = truncate(fullName, 255, "full name");
		specifyTaxon.setFullName(fullName);

		// GroupNumber
		GROUP group = asaTaxon.getGroup();
		specifyTaxon.setGroupNumber(AsaTaxon.toString(group));
	        
	    // isAccepted (this is an internal field for distinguishing an accepted taxon among a set of synonyms)
        specifyTaxon.setIsAccepted(true);
        
        // isHybrid
        Boolean isHybrid = asaTaxon.isHybrid();
        specifyTaxon.setIsHybrid(isHybrid);

        // Name
		name = truncate(name, 64, "name");
		specifyTaxon.setName(name);

		// ParAuthor
		Agent parAuthor = NullAgent();
		Integer parAuthorId = asaTaxon.getParAuthorId();
		if (parAuthorId != null) parAuthor = lookupBotanist(parAuthorId);
		specifyTaxon.setParAuthor(parAuthor);
		    
		// ParExAuthor
        Agent parExAuthor = NullAgent();
        Integer parExAuthorId = asaTaxon.getParExAuthorId();
        if (parExAuthorId != null) parExAuthor = lookupBotanist(parExAuthorId);
        specifyTaxon.setParExAuthor(parExAuthor);
        
		// Parent
		Taxon parent = null;
		Integer parentId = asaTaxon.getParentId();
		if (parentId != null)
		{
		    parent = getTaxonLookup().getById(parentId);
		}
		else
		{
			parent = new Taxon();
			parent.setTaxonId(1); // TODO: this only just happens to work.
		}
		specifyTaxon.setParent(parent);
		        
		// RankID
		String rank = asaTaxon.getRank();
		checkNull(rank, "rank");
		
		Integer rankId = taxonRankIdsByType.get(rank);
		checkNull(rankId, "rank id");
		
		specifyTaxon.setRankId(rankId);
		
		// Remarks
        String remarks = asaTaxon.getRemarks();
        specifyTaxon.setRemarks(remarks);
        
        // StdAuthor
        Agent stdAuthor = NullAgent();
        Integer stdAuthorId = asaTaxon.getStdAuthorId();
        if (stdAuthorId != null) stdAuthor = lookupBotanist(stdAuthorId);
        specifyTaxon.setStdAuthor(stdAuthor);
        
        // StdExAuthor
        Agent stdExAuthor = NullAgent();
        Integer stdExAuthorId = asaTaxon.getStdExAuthorId(); 
        if (stdExAuthorId != null) stdExAuthor = lookupBotanist(stdExAuthorId);
        specifyTaxon.setStdExAuthor(stdExAuthor);
        
		// TaxonomicSerialNumber
		Integer asaTaxonId = asaTaxon.getId();
		checkNull(asaTaxonId, "id");
		
        String taxonSerialNumber = getTaxonSerialNumber(asaTaxonId);
		specifyTaxon.setTaxonomicSerialNumber(taxonSerialNumber);

		// TaxonTreeDef
		specifyTaxon.setDefinition(treeDef);
		
		// TaxonTreeDefItem
		TaxonTreeDefItem defItem = getTreeDefItemByRankId(rankId);
		checkNull(defItem, "tree def item");
		
		specifyTaxon.setDefinitionItem(defItem);
		
		// Text1 (status)
		STATUS status = asaTaxon.getStatus();
        specifyTaxon.setText1(AsaTaxon.toString(status));
	        
		// Text2 (basionym)
		String basionym = asaTaxon.getBasionym();
		specifyTaxon.setText2(basionym);

		// TimestampCreated
        Date dateCreated = asaTaxon.getDateCreated();
        specifyTaxon.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // Version
        specifyTaxon.setVersion(1);

        // return converted taxon record
		return specifyTaxon;
	}

    private String getTaxonSerialNumber(Integer asaTaxonId)
	{
		return String.valueOf(asaTaxonId);
	}

    private ReferenceWork getReferenceWork(AsaTaxon asaTaxon, ReferenceWork parent)
    {
        ReferenceWork referenceWork = new ReferenceWork();

        // ContainedRFParent
        referenceWork.setContainedRFParent(parent);

        // ReferenceWorkType
        referenceWork.setReferenceWorkType(ReferenceWork.PROTOLOGUE);

        return referenceWork;
    }
    
    private Author getAuthor(Agent agent, ReferenceWork referenceWork, int orderNumber)
    {
        Author author = new Author();

        // Agent
        author.setAgent(agent);
        
        // OrderNumber
        author.setOrderIndex(orderNumber);
        
        // ReferenceWork
        author.setReferenceWork(referenceWork);
        
        return author;
    }
    
	private TaxonCitation getTaxonCitation(AsaTaxon asaTaxon, Taxon taxon, ReferenceWork referenceWork) throws LocalException
	{	        
	    TaxonCitation taxonCitation = new TaxonCitation();
	    
	    // ReferenceWork
        taxonCitation.setReferenceWork(referenceWork);

        // Taxon
        taxonCitation.setTaxon(taxon);
        
        // Text1 (collation)
        String collation = asaTaxon.getCitCollation();
        taxonCitation.setText1(collation);
        
        // Text2 (date)
        String date = asaTaxon.getCitDate();
        taxonCitation.setText2(date);
        
        return taxonCitation;
	}

	private ReferenceWork lookupPublication(Integer publicationId) throws LocalException
	{
		return publicationLookup.getById(publicationId);
	}
	
	private Agent lookupBotanist(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}
	
	private String getInsertSql(Taxon taxon)
	{
		String fieldNames = "Author, CitesStatus, CreatedByAgentID, FullName, GroupNumber, " +
				            "IsAccepted, IsHybrid, Name, ParAuthorID, ParExAuthorID, ParentID, " +
				            "RankID, Remarks, StdAuthorID, StdExAuthorID, TaxonomicSerialNumber, " +
				            "TaxonTreeDefID, TaxonTreeDefItemID, Text1, Text2, TimestampCreated, " +
				            "Version";

		String[] values = new String[22];

		values[0]  = SqlUtils.sqlString( taxon.getAuthor());
		values[1]  = SqlUtils.sqlString( taxon.getCitesStatus());
		values[2]  = SqlUtils.sqlString( taxon.getCreatedByAgent().getId());
		values[3]  = SqlUtils.sqlString( taxon.getFullName());
		values[4]  = SqlUtils.sqlString( taxon.getGroupNumber());
		values[5]  = SqlUtils.sqlString( taxon.getIsAccepted());
		values[6]  = SqlUtils.sqlString( taxon.getIsHybrid());
		values[7]  = SqlUtils.sqlString( taxon.getName());
		values[8]  = SqlUtils.sqlString( taxon.getParAuthor().getId());
		values[9]  = SqlUtils.sqlString( taxon.getParExAuthor().getId());
		values[10] = SqlUtils.sqlString( taxon.getParent().getId());
		values[11] = SqlUtils.sqlString( taxon.getRankId());
		values[12] = SqlUtils.sqlString( taxon.getRemarks());
		values[13] = SqlUtils.sqlString( taxon.getStdAuthor().getId());
		values[14] = SqlUtils.sqlString( taxon.getStdExAuthor().getId());
		values[15] = SqlUtils.sqlString( taxon.getTaxonomicSerialNumber());
		values[16] = SqlUtils.sqlString( taxon.getDefinition().getId());
		values[17] = SqlUtils.sqlString( taxon.getDefinitionItem().getId());
		values[18] = SqlUtils.sqlString( taxon.getText1());
		values[19] = SqlUtils.sqlString( taxon.getText2());
		values[20] = SqlUtils.sqlString( taxon.getTimestampCreated());
		values[21] = SqlUtils.sqlString( taxon.getVersion());

		return SqlUtils.getInsertSql("taxon", fieldNames, values);
	}
	
    private String getInsertSql(TaxonCitation taxonCitation)
    {
        String fieldNames = "ReferenceWorkID, TaxonID, Text1, Text2, TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( taxonCitation.getReferenceWork().getId());
        values[1] = SqlUtils.sqlString( taxonCitation.getTaxon().getId());
        values[2] = SqlUtils.sqlString( taxonCitation.getText1());
        values[3] = SqlUtils.sqlString( taxonCitation.getText2());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("taxoncitation", fieldNames, values);
    }
    
    private String getInsertSql(ReferenceWork referenceWork)
    {
        String fieldNames = "ContainedRFParentID, ReferenceWorkType, " +
        		            "TimestampCreated, Version";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( referenceWork.getContainedRFParent().getId());
        values[1] = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
        values[2] = SqlUtils.now();
        values[3] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("referencework", fieldNames, values);
    }
    
    private String getInsertSql(Author author)
    {
        String fieldNames = "AgentID, OrderNumber, ReferenceWorkID, TimestampCreated, Version";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( author.getAgent().getId());
        values[1] = SqlUtils.sqlString( author.getOrderNumber());
        values[2] = SqlUtils.sqlString( author.getReferenceWork().getId());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("author", fieldNames, values);
    }
}
