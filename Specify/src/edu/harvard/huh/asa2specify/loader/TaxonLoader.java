package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaTaxon;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;

public class TaxonLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(TaxonLoader.class);

	private TaxonTreeDef treeDef;
	
	private HashMap <String, Integer> taxonRankIdsByType = new HashMap<String, Integer> ();

	public TaxonLoader(File csvFile, Statement sqlStatement, TaxonTreeDef treeDef) {
		super(csvFile, sqlStatement);

		this.treeDef = treeDef;
		
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

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		AsaTaxon asaTaxon = parseTaxonRecord(columns);
		
		Taxon taxon = convert(asaTaxon);
		
		String rank = asaTaxon.getRank();

		Integer rankId = taxonRankIdsByType.get(rank);
		if (rankId == null)
		{
			throw new LocalException("No rank id for " + rank);
		}
		taxon.setRankId(rankId);
		
		taxon.setDefinition(treeDef);
		
		TaxonTreeDefItem defItem = treeDef.getDefItemByRank(rankId);
		if (defItem == null)
		{
			throw new LocalException("No tree def item for rank " + rankId);
		}
		taxon.setDefinitionItem(defItem);
		
		// find matching parent
		Taxon parent = new Taxon();
		Integer parentId = asaTaxon.getParentId();
		String taxSerNum = SqlUtils.sqlString(String.valueOf(parentId));

        String sql = SqlUtils.getQueryIdByFieldSql("taxon", "TaxonID", "TaxonomicSerialNumber", taxSerNum);

        parentId = queryForId(sql);
        if (parentId == null)
        {
        	throw new LocalException("No parent found with taxonomic serial number " + taxSerNum);
        }
		parent.setTaxonId(parentId);
		
		sql = getInsertSql(taxon);
		insert(sql);
	}

	public void numberNodes() throws LocalException
	{
		numberNodes("taxon", "TaxonID");
	}
	
	private AsaTaxon parseTaxonRecord(String[] columns) throws LocalException
	{
		// from st_lookup category 130
		String NON_CITES_TYPE = "[none]";  // set to null 
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

		AsaTaxon taxon = new AsaTaxon();  // TODO: implement

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
		if (fullName.length() > 255)
		{
			log.warn("truncating full name");
			fullName = fullName.substring(0, 255);
		}
		specifyTaxon.setFullName(fullName);

		// Name
		String name = asaTaxon.getName();
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

		// return converted taxon record
		return specifyTaxon;
	}

	private String getInsertSql(Taxon taxon)
	{
		String fieldNames = "Name, FullName, Author, CitesStatus,  TaxonomicSerialNumber, " +
		"Remarks, TaxonTreeDefID, TaxonTreeDefItemID, RankID, ParentID, " +
		"TimestampCreated, Version";

		List<String> values = new ArrayList<String>(12);

		values.add(SqlUtils.sqlString(taxon.getName()                  ));
		values.add(SqlUtils.sqlString(taxon.getFullName()              ));
		values.add(SqlUtils.sqlString(taxon.getAuthor()                ));
		values.add(SqlUtils.sqlString(taxon.getCitesStatus()           ));
		values.add(SqlUtils.sqlString(taxon.getTaxonomicSerialNumber() ));
		values.add(SqlUtils.sqlString(taxon.getRemarks()               ));
		values.add(    String.valueOf(taxon.getDefinition().getId()    ));
		values.add(    String.valueOf(taxon.getDefinitionItem().getId()));
		values.add(    String.valueOf(taxon.getRankId()                ));
		values.add(    String.valueOf(taxon.getParent().getId()        ));
		values.add("now");
		values.add(    String.valueOf(1));

		return SqlUtils.getInsertSql("taxon", fieldNames, values);
	}
}
