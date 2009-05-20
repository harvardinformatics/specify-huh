package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaTaxon;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;

public class TaxonLoader extends CsvToSqlLoader
{
	private final Logger log = Logger.getLogger(TaxonLoader.class);

	private TaxonTreeDef treeDef;
	
	private HashMap <String, Integer> taxonRankIdsByType = new HashMap<String, Integer>();

	private HashMap <Integer, TaxonTreeDefItem> taxonDefItemsByRank = new HashMap<Integer, TaxonTreeDefItem>();
	
	public TaxonLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
		super(csvFile, sqlStatement);

		this.treeDef = getTaxonTreeDef();
		
		taxonRankIdsByType.put("life",             0);
		taxonRankIdsByType.put("kingdom",         10);
		taxonRankIdsByType.put("\"Major Group\"", 20);
		taxonRankIdsByType.put("division",        30);
		taxonRankIdsByType.put("class",           60);
		taxonRankIdsByType.put("order",          100);
		taxonRankIdsByType.put("family",         140);
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

	    String sql = SqlUtils.getQueryIdByFieldSql("taxontreedef", "TaxonTreeDefID", "Name", "Taxon");

	    Integer taxonTreeDefId = queryForId(sql);
	    t.setTaxonTreeDefId(taxonTreeDefId);

	    return t;
	}

	protected TaxonTreeDefItem getTreeDefItemByRankId(Integer rankId) throws LocalException
	{
	    TaxonTreeDefItem treeDefItem = taxonDefItemsByRank.get(rankId);

	    if (treeDefItem == null)
	    {

	        String sql = SqlUtils.getQueryIdByFieldSql("taxontreedefitem", "TaxonTreeDefID", "RankID", String.valueOf(rankId));

	        Integer taxonTreeDefItemId = queryForId(sql);

	        treeDefItem = new TaxonTreeDefItem();
	        treeDefItem.setTaxonTreeDefItemId(taxonTreeDefItemId);
	        taxonDefItemsByRank.put(rankId, treeDefItem);
	    } 

	    return treeDefItem;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		AsaTaxon asaTaxon = parse(columns);
		
		Taxon taxon = convert(asaTaxon);
		
		String rank = asaTaxon.getRank();

		Integer rankId = taxonRankIdsByType.get(rank);
		if (rankId == null)
		{
			throw new LocalException("No rank id for " + asaTaxon.getId() + " " + rank);
		}
		taxon.setRankId(rankId);
		
		if (rankId <= taxonRankIdsByType.get("family"))
		{
		    log.info("Processing " + taxon.getName());
		    if (frame != null)
		    {
		        frame.setDesc("Loading " + taxon.getName() + "...");
		    }
		}

		taxon.setDefinition(treeDef);
		
		TaxonTreeDefItem defItem = getTreeDefItemByRankId(rankId);
		if (defItem == null)
		{
			throw new LocalException("No tree def item for rank " + asaTaxon.getId() + " " + rankId);
		}
		taxon.setDefinitionItem(defItem);
		
		// find matching parent
		Taxon parent = new Taxon();
		Integer parentId = asaTaxon.getParentId();
		if (parentId != null)
		{
		    String taxSerNum = String.valueOf(parentId);

		    String sql = SqlUtils.getQueryIdByFieldSql("taxon", "TaxonID", "TaxonomicSerialNumber", taxSerNum);

		    parentId = queryForId(sql);
		    if (parentId == null)
		    {
		        throw new LocalException("No parent found for " + asaTaxon.getId() + " with taxonomic serial number " + taxSerNum);
		    }
		}
		else
		{
		    parentId = Integer.valueOf(1); // TODO: this only just happens to work.
		}
		parent.setTaxonId(parentId);
		taxon.setParent(parent);
		
        // find matching record creator
        Integer creatorOptrId = asaTaxon.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        taxon.setCreatedByAgent(createdByAgent);
        
		String sql = getInsertSql(taxon);
		insert(sql);
	}

	public void numberNodes() throws LocalException
	{
		numberNodes("taxon", "TaxonID");
	}
	
	private AsaTaxon parse(String[] columns) throws LocalException
	{
	    if (columns.length < 10)
	    {
	        throw new LocalException("Wrong number of columns");
	    }

		AsaTaxon taxon = new AsaTaxon();

		try {
		    String parentId = StringUtils.trimToNull(columns[0]);
		    if (parentId != null)
		    {
		        taxon.setParentId(Integer.parseInt(parentId));
		    }

		    taxon.setId(            Integer.parseInt(StringUtils.trimToNull(columns[1])));
		    taxon.setAuthor(                         StringUtils.trimToNull(columns[2]));
		    taxon.setCitesStatus(                    StringUtils.trimToNull(columns[3]));
		    taxon.setFullName(                       StringUtils.trimToNull(columns[4]));
		    taxon.setName(                           StringUtils.trimToNull(columns[5]));
		    taxon.setRank(                           StringUtils.trimToNull(columns[6]));
		    taxon.setRemarks( SqlUtils.iso8859toUtf8(StringUtils.trimToNull(columns[7])));
		    taxon.setCreatedById(   Integer.parseInt(StringUtils.trimToNull(columns[8])));
		    taxon.setDateCreated( SqlUtils.parseDate(StringUtils.trimToNull(columns[9])));
		}
		catch (NumberFormatException e) {
		    throw new LocalException("Couldn't parse numeric field", e);
		}

		return taxon;
	}

	private Taxon convert(AsaTaxon asaTaxon) throws LocalException
	{
		Taxon specifyTaxon = new Taxon();
		// isAccepted
		specifyTaxon.setIsAccepted( Boolean.TRUE );

		// Author TODO: see BugID:13
		String author = asaTaxon.getAuthor();
		specifyTaxon.setAuthor(author);

		// CITES Status
		String endangerment = asaTaxon.getCitesStatus();
		specifyTaxon.setCitesStatus(endangerment);

		// FullName
		String fullName = asaTaxon.getFullName();
		if (fullName != null && fullName.length() > 255)
		{
			log.warn("truncating full name " + asaTaxon.getId() + " " + fullName);
			fullName = fullName.substring(0, 255);
		}
		specifyTaxon.setFullName(fullName);

		// Name
		String name = asaTaxon.getName();
		if (name == null)
		{
		    throw new LocalException("No name in taxon record " + asaTaxon.getId() + " " + name);
		}
		if (name.length() > 64)
		{
			log.warn("truncating name");
			name = name.substring(0, 64);
		}
		specifyTaxon.setName(name);

		// TaxonomicSerialNumber
		specifyTaxon.setTaxonomicSerialNumber( Integer.toString( asaTaxon.getId() ) );

		// Remarks
		String remarks = asaTaxon.getRemarks();
		specifyTaxon.setRemarks(remarks);

		// TimestampCreated
        Date dateCreated = asaTaxon.getDateCreated();
        specifyTaxon.setTimestampCreated(DateUtils.toTimestamp(dateCreated));

        // Version
        specifyTaxon.setVersion(1);

        // return converted taxon record
		return specifyTaxon;
	}

	private String getInsertSql(Taxon taxon)
	{
		String fieldNames = "Name, FullName, Author, CitesStatus,  TaxonomicSerialNumber, " +
		                    "TaxonTreeDefID, TaxonTreeDefItemID, RankID, ParentID, Remarks, " +
		                    "CreatedByAgentID, TimestampCreated, Version";

		String[] values = new String[13];

		values[0]  = SqlUtils.sqlString( taxon.getName());
		values[1]  = SqlUtils.sqlString( taxon.getFullName());
		values[2]  = SqlUtils.sqlString( taxon.getAuthor());
		values[3]  = SqlUtils.sqlString( taxon.getCitesStatus());
		values[4]  = SqlUtils.sqlString( taxon.getTaxonomicSerialNumber());
		values[5]  = SqlUtils.sqlString( taxon.getDefinition().getId());
		values[6]  = SqlUtils.sqlString( taxon.getDefinitionItem().getId());
		values[7]  = SqlUtils.sqlString( taxon.getRankId());
		values[8]  = SqlUtils.sqlString( taxon.getParent().getId());
	    values[9]  = SqlUtils.sqlString( taxon.getRemarks());
		values[10] = SqlUtils.sqlString( taxon.getCreatedByAgent().getId());
		values[11] = SqlUtils.sqlString( taxon.getTimestampCreated());
		values[12] = SqlUtils.sqlString( taxon.getVersion());

		return SqlUtils.getInsertSql("taxon", fieldNames, values);
	}
}
