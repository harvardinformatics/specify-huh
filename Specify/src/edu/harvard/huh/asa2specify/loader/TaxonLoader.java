package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaTaxon;
import edu.harvard.huh.asa.AsaTaxon.ENDANGERMENT;
import edu.harvard.huh.asa.AsaTaxon.GROUP;
import edu.harvard.huh.asa.AsaTaxon.STATUS;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonCitation;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;

// Run this class after OptrLoader.

public class TaxonLoader extends TreeLoader
{
    private static final Logger log  = Logger.getLogger(TaxonLoader.class);
    
	private TaxonTreeDef treeDef;
	
	private HashMap <String, Integer> taxonRankIdsByType = new HashMap<String, Integer>();

	private HashMap <Integer, TaxonTreeDefItem> taxonDefItemsByRank = new HashMap<Integer, TaxonTreeDefItem>();
	
	private TaxonLookup taxonLookup;
	private PublicationLookup publicationLookup;
	
	public TaxonLoader(File csvFile, Statement sqlStatement, PublicationLookup publicationLookup) throws LocalException
	{
		super(csvFile, sqlStatement);

		this.publicationLookup = publicationLookup;
		
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

	    Integer taxonTreeDefId = getInt("taxontreedef", "TaxonTreeDefID", "Name", "Taxon");
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
		
		// TODO: create authors for taxon?
		
		// TaxonCitation
		TaxonCitation taxonCitation = getTaxonCitation(asaTaxon);
		if (taxonCitation != null)
		{
		    taxonCitation.setTaxon(taxon);
		    sql = getInsertSql(taxonCitation);
		    insert(sql);
		}
	}

	public Logger getLogger()
    {
        return log;
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

					Integer taxonId = getInt("taxon", "TaxonID", "TaxonomicSerialNumber", taxonSerialNumber);

					taxon.setTaxonId(taxonId);

					return taxon;
				}
			};
		}

		return taxonLookup;
	}

	private AsaTaxon parse(String[] columns) throws LocalException
	{
	    if (columns.length < 22)
	    {
	        throw new LocalException("Not enough columns");
	    }

		AsaTaxon taxon = new AsaTaxon();

		try
		{
		    taxon.setParentId(              SqlUtils.parseInt( columns[0]  ));
		    taxon.setId(                    SqlUtils.parseInt( columns[1]  ));
		    taxon.setRank(                                     columns[2]  );
		    taxon.setGroup(               AsaTaxon.parseGroup( columns[3]  ));
		    taxon.setStatus(             AsaTaxon.parseStatus( columns[4]  ));
		    taxon.setEndangerment( AsaTaxon.parseEndangerment( columns[5]  ));
		    taxon.setIsHybrid(           Boolean.parseBoolean( columns[6]  ));
		    taxon.setFullName(                                 columns[7]  );
		    taxon.setName(                                     columns[8]  );
		    taxon.setAuthor(                                   columns[9]  );
		    taxon.setParAuthorId(           SqlUtils.parseInt( columns[10] ));
		    taxon.setParExAuthorId(         SqlUtils.parseInt( columns[11] ));
		    taxon.setStdAuthorId(           SqlUtils.parseInt( columns[12] ));
		    taxon.setStdExAuthorId(         SqlUtils.parseInt( columns[13] ));
		    taxon.setCitInAuthorId(         SqlUtils.parseInt( columns[14] ));
		    taxon.setCitPublId(             SqlUtils.parseInt( columns[15] ));
		    taxon.setCitCollation(                             columns[16] );
		    taxon.setCitDate(                                  columns[17] );
		    taxon.setRemarks(          SqlUtils.iso8859toUtf8( columns[18] ));
		    taxon.setCreatedById(           SqlUtils.parseInt( columns[19] ));
		    taxon.setDateCreated(          SqlUtils.parseDate( columns[20] ));
		    taxon.setBasionymId(            SqlUtils.parseInt( columns[21] ));
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
		String fullName = asaTaxon.getFullName();
		checkNull(fullName, "full name");
		fullName = truncate(fullName, 255, "full name");
		specifyTaxon.setFullName(fullName);

	    // isAccepted TODO: map taxon status to isAccepted
        STATUS status = asaTaxon.getStatus();
        specifyTaxon.setIsAccepted( status != STATUS.NomRej && status != STATUS.NomInvalid && status != STATUS.NomSuperfl);
        
        // isHybrid
        Boolean isHybrid = asaTaxon.isHybrid();
        specifyTaxon.setIsHybrid(isHybrid);

        // Name
		String name = asaTaxon.getName();
		checkNull(name, "name");
		name = truncate(name, 64, "name");
		specifyTaxon.setName(name);
		
		// Number1
		Integer basionymId = asaTaxon.getBasionymId();
		if (basionymId != null) specifyTaxon.setNumber1(basionymId);

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
		
		// Text1 (group)
		GROUP group = asaTaxon.getGroup();
		specifyTaxon.setText1(AsaTaxon.toString(group));
	        
		// Text2 (status)
		specifyTaxon.setText2(AsaTaxon.toString(status));

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

	private TaxonCitation getTaxonCitation(AsaTaxon asaTaxon) throws LocalException
	{
	    Integer citPublId = asaTaxon.getCitPublId();
	    
	    if (citPublId == null) return null;
	        
	    TaxonCitation taxonCitation = new TaxonCitation();
	    
	    // ReferenceWork
        ReferenceWork referenceWork = lookupPublication(citPublId);
        taxonCitation.setReferenceWork(referenceWork);
        
        // Text1 (collation)
        String citCollation = asaTaxon.getCitCollation();
        taxonCitation.setText1(citCollation);
        
        // Text2 (date)
        String citDate = asaTaxon.getCitDate();
        taxonCitation.setText2(citDate);

        return taxonCitation;
	}

	private ReferenceWork lookupPublication(Integer publicationId) throws LocalException
	{
		return publicationLookup.getById(publicationId);
	}
	
	private String getInsertSql(Taxon taxon)
	{
		String fieldNames = "Author, CitesStatus, CreatedByAgentID, FullName, IsAccepted, " +
				            "IsHybrid, Name, Number1, ParentID, RankID, Remarks, TaxonomicSerialNumber, " +
				            "TaxonTreeDefID, TaxonTreeDefItemID, Text1, Text2, TimestampCreated, Version";

		String[] values = new String[18];

		values[0]  = SqlUtils.sqlString( taxon.getAuthor());
		values[1]  = SqlUtils.sqlString( taxon.getCitesStatus());
		values[2]  = SqlUtils.sqlString( taxon.getCreatedByAgent().getId());
		values[3]  = SqlUtils.sqlString( taxon.getFullName());
		values[4]  = SqlUtils.sqlString( taxon.getIsAccepted());
		values[5]  = SqlUtils.sqlString( taxon.getIsHybrid());
		values[6]  = SqlUtils.sqlString( taxon.getName());
		values[7]  = SqlUtils.sqlString( taxon.getNumber1());
		values[8]  = SqlUtils.sqlString( taxon.getParent().getId());
		values[9] = SqlUtils.sqlString( taxon.getRankId());
		values[10] = SqlUtils.sqlString( taxon.getRemarks());
		values[11] = SqlUtils.sqlString( taxon.getTaxonomicSerialNumber());
		values[12] = SqlUtils.sqlString( taxon.getDefinition().getId());
		values[13] = SqlUtils.sqlString( taxon.getDefinitionItem().getId());
		values[14] = SqlUtils.sqlString( taxon.getText1());
		values[15] = SqlUtils.sqlString( taxon.getText2());
		values[16] = SqlUtils.sqlString( taxon.getTimestampCreated());
		values[17] = SqlUtils.sqlString( taxon.getVersion());

		return SqlUtils.getInsertSql("taxon", fieldNames, values);
	}
	
    private String getInsertSql(TaxonCitation taxonCitation)
    {
        String fieldNames = "TaxonID, ReferenceWorkID, Text1, Text2, TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( taxonCitation.getTaxon().getId());
        values[1] = SqlUtils.sqlString( taxonCitation.getReferenceWork().getId());
        values[2] = SqlUtils.sqlString( taxonCitation.getText1());
        values[3] = SqlUtils.sqlString( taxonCitation.getText2());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("taxoncitation", fieldNames, values);
    }
}
