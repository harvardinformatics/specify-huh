package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.GeoUnit;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.GeographyLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.GeographyTreeDefItem;

// Run this class after OptrLoader.

public class GeoUnitLoader extends TreeLoader
{
    public GeographyLookup getGeographyLookup()
    {
        if (geoLookup == null)
        {
            geoLookup = new GeographyLookup() {

                public Geography getByGeoUnitId(Integer geoUnitId) throws LocalException
                {
                    Geography geography = new Geography();

                    String guid = getGuid(geoUnitId);

                    Integer geographyId = getIntByField("geography", "GeographyID", "GUID", guid);

                    geography.setGeographyId(geographyId);

                    return geography;
                }
            };
        }

        return geoLookup;
    }

	private String getGuid(Integer geoUnitId)
	{
	    return String.valueOf(geoUnitId);
	}
	
	private GeographyLookup geoLookup;
	   
	// root
	private final String ROOT_TYPE = "root";
	private final Integer ROOT_RANK = 0;  // Specify value

	// continent
	private final String CONTINENT_TYPE = "continent";
	private final Integer CONTINENT_RANK = 100;  // Specify value

	// region
	private final String REGION_TYPE = "region";
	private final Integer REGION_RANK = 150;

	// sub-region
	private final String ARCHIPELAGO_TYPE = "archipelago";
	private final Integer ARCHIPELAGO_RANK = 160;

	// country
	private final String COUNTRY_TYPE = "country";
	private final Integer COUNTRY_RANK = 200;  // Specify value

	// land
	private final String LAND_TYPE = "land";
	private final Integer LAND_RANK = 210;

	// territory
	private final String TERRITORY_TYPE = "territory";
	private final Integer TERRITORY_RANK = 220;

	// subcontinent island(s)
	private final String SUBCONTINENT_ISL_TYPE = "subcontinent island(s)";
	private final Integer SUBCONTINENT_ISL_RANK = 230;

	// continent subregion
	private final String CONTINTENT_SUBREGION_TYPE = "continent subregion";
	private final Integer CONTINENT_SUBREGION_RANK = 250;

	// country subregion
	private final String COUNTRY_SUBREGION_TYPE = "country subregion";
	private final Integer COUNTRY_SUBREGION_RANK = 260;

	// straights
	private final String STRAIGHTS_TYPE = "straights";
	private final Integer STRAIGHTS_RANK = 270;

	// subcountry island(s)
	private final String SUBCOUNTRY_ISL_TYPE = "subcountry island(s)";
	private final Integer SUBCOUNTRY_ISL_RANK = 280;

	// state
	private final String STATE_TYPE = "state / province";
	private final Integer STATE_RANK = 300;  // Specify value

	// peninsula
	private final String PENINSULA_TYPE = "peninsula";
	private final Integer PENINSULA_RANK = 310;

	// sub-state/province
	private final String SUBSTATE_ISL_TYPE = "substate island(s)";
	private final Integer SUBSTATE_ISL_RANK = 320;

	// coast
	private final String COAST_TYPE = "coast";
	private final Integer COAST_RANK = 330;

	// other
	private final String OTHER_TYPE = "other";
	private final Integer OTHER_RANK = 360;

	// state subregion
	private final String STATE_SUBREGION_TYPE = "state subregion";
	private final Integer STATE_SUBREGION_RANK = 380;

	// county
	private final String COUNTY_TYPE = "county";
	private final Integer COUNTY_RANK = 400;  // Specify value

	// mountain(s)
	private final String MOUNTAIN_TYPE = "mountain(s)";
	private final Integer MOUNTAIN_RANK = 410;

	// river
	private final String RIVER_TYPE = "river";
	private final Integer RIVER_RANK = 420;

	// forest
	private final String FOREST_TYPE = "forest";
	private final Integer FOREST_RANK = 430;

	// valley
	private final String VALLEY_TYPE = "valley";
	private final Integer VALLEY_RANK = 440;

	// island
	private final String ISLAND_TYPE = "island";
	private final Integer ISLAND_RANK = 450;

	// hills
	private final String HILL_TYPE = "hill(s)";
	private final Integer HILL_RANK = 460;

	// canyon
	private final String CANYON_TYPE = "canyon";
	private final Integer CANYON_RANK = 470;

	// lake
	private final String LAKE_TYPE = "lake";
	private Integer LAKE_RANK = 480;

	// county subregion
	private final String COUNTY_SUBREGION_TYPE = "county subregion";
	private final Integer COUNTY_SUBREGION_RANK = 490;

	// city / town
	private final String CITY_TYPE = "city / town";
	private final Integer CITY_RANK = 500;  // Specify value

	// sub-city
	private final String CITY_SUBREGION_TYPE = "city subregion";
	private final Integer CITY_SUBREGION_RANK = 510;

	private final Logger log = Logger.getLogger(GeoUnitLoader.class);

	private GeographyTreeDef treeDef;

	private final Geography nullIdGeography;
	
	private HashMap <Integer, GeographyTreeDefItem> geoDefItemsByRank = new HashMap<Integer, GeographyTreeDefItem>();
	
	public GeoUnitLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.treeDef = getGeoTreeDef();
		this.nullIdGeography = new Geography();
		
		nullIdGeography.setGeographyId(null);
	}
	   
    private GeographyTreeDef getGeoTreeDef() throws LocalException
    {
        GeographyTreeDef g = new GeographyTreeDef();

        Integer geoTreeDefId = getIntByField("geographytreedef", "GeographyTreeDefID", "Name", "Geography");

        g.setGeographyTreeDefId(geoTreeDefId);

        return g;
    }
    
	private GeographyTreeDefItem getTreeDefItemByRankId(Integer rankId) throws LocalException
	{
	    GeographyTreeDefItem treeDefItem = geoDefItemsByRank.get(rankId);

	    if (treeDefItem == null)
	    {
	        Integer geoTreeDefItemId = getIntByField("geographytreedefitem", "GeographyTreeDefID", "RankID", rankId);

	        treeDefItem = new GeographyTreeDefItem();
	        treeDefItem.setGeographyTreeDefItemId(geoTreeDefItemId);
	        geoDefItemsByRank.put(rankId, treeDefItem);
	    } 

	    return treeDefItem;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		GeoUnit geoUnit = parse(columns);

		Integer geoUnitId = geoUnit.getId();
		setCurrentRecordId(geoUnitId);
		
		String rank = geoUnit.getRank();
		checkNull(rank, "rank");
		
		String name = geoUnit.getName();
		checkNull(name, "name");
		
		if (rank.equals(REGION_TYPE))
		{
		    log.info("Processing " + name);
		    if (frame != null)
		    {
		        frame.setDesc("Loading " + name + "...");
		    }
		}
		
		Geography geography = getGeography(geoUnit);

		String sql = getInsertSql(geography);
		Integer geographyId = insert(sql);
		geography.setGeographyId(geographyId);
		
		// variant names
		for (String variantName : geoUnit.getVariantNames())
		{
			String fullName = getQualifiedName(name, geoUnit.getDisplayQualifier());
		    Geography synonym = getSynonym(geography, variantName, fullName);

		    sql = getInsertSql(synonym);
		    insert(sql);
		}
	}

	public void numberNodes() throws LocalException
	{
		numberNodes("geography", "GeographyID");
	}

	private GeoUnit parse(String[] columns) throws LocalException
	{
	    if (columns.length < 15)
	    {
	        throw new LocalException("Wrong number of columns");
	    }

	    GeoUnit geoUnit = new GeoUnit();
	    try
	    {
	        geoUnit.setParentId(     SqlUtils.parseInt( StringUtils.trimToNull( columns[0]  )));
	        geoUnit.setId(           SqlUtils.parseInt( StringUtils.trimToNull( columns[1]  )));
	        geoUnit.setRank(                            StringUtils.trimToNull( columns[2]  ));
	        geoUnit.setIsoCode(                         StringUtils.trimToNull( columns[3]  ));
	        geoUnit.setDisplayQualifier(                StringUtils.trimToNull( columns[4]  ));
	        geoUnit.setName(                            StringUtils.trimToNull( columns[5]  ));
	        geoUnit.setVernacularName(                  StringUtils.trimToNull( columns[6]  ));
	        geoUnit.setRemarks( SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[7]  )));
	        geoUnit.setCreatedById(  SqlUtils.parseInt( StringUtils.trimToNull( columns[8]  )));
	        geoUnit.setDateCreated( SqlUtils.parseDate( StringUtils.trimToNull( columns[9]  )));
            geoUnit.addVariantName(                     StringUtils.trimToNull( columns[10] ));
            geoUnit.addVariantName(                     StringUtils.trimToNull( columns[11] ));
            geoUnit.addVariantName(                     StringUtils.trimToNull( columns[12] ));
            geoUnit.addVariantName(                     StringUtils.trimToNull( columns[13] ));
            geoUnit.addVariantName(                     StringUtils.trimToNull( columns[14] ));
	    }
	    catch (NumberFormatException e)
	    {
	        throw new LocalException("Couldn't parse numeric field", e);
	    }

	    return geoUnit;
	}

	private Geography getGeography(GeoUnit geoUnit) throws LocalException
	{
		Geography geography = new Geography();
		
		// Abbreviation
		String abbrev = geoUnit.getAbbreviation();
		if (abbrev != null)
		{
			abbrev = truncate(abbrev, 16, "abbreviation");
			geography.setAbbrev(abbrev);
		}
		
	    // AcceptedGeography
        geography.setAcceptedGeography(nullIdGeography);

		// CommonName
		String vernacularName = geoUnit.getVernacularName();
		if (vernacularName != null)
		{
			vernacularName = truncate(vernacularName, 128, "vernacular name");
			geography.setCommonName(vernacularName);
		}
        
        // CreatedByAgent
        Integer creatorOptrId = geoUnit.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        geography.setCreatedByAgent(createdByAgent);
		
		// GeographyTreeDef
		geography.setDefinition(treeDef);
		
		// GUID TODO: temporary, remove after import
		Integer geoUnitId = geoUnit.getId();
		checkNull(geoUnitId, "id");
		
		String guid = getGuid(geoUnitId);
		geography.setGuid(guid);

		// FullName
		String name = geoUnit.getName();
		String displayQualifier = geoUnit.getDisplayQualifier();
		String qualifiedName = getQualifiedName(name, displayQualifier);
        qualifiedName = truncate(qualifiedName, 255, "qualified name");
		geography.setFullName(qualifiedName);

		// GeographyCode
		String isoCode = geoUnit.getIsoCode();
		if (isoCode != null)
		{
		    isoCode = truncate(isoCode, 8, "iso code");
		    geography.setGeographyCode(isoCode);
		}

		// IsAccepted
		geography.setIsAccepted(true);
		
		// Name
		checkNull(name, "name");
		name = truncate(name, 64, "name");
		geography.setName(name);
		
		// Parent
		Geography parent = null;
		Integer parentId = geoUnit.getParentId();
		
		if (parentId != 10100)
		{
			parent = getGeographyLookup().getByGeoUnitId(parentId);
		}
		else
		{
		    parent = new Geography();
		    parent.setGeographyId(1); // TODO: this only happens to work
		}
		
		geography.setParent(parent);

		// RankId
		String rank = geoUnit.getRank();
		Integer parentRankId = getIntByField("geography", "RankID", "GeographyID", parentId);
		
		Integer rankId = getGeoTreeDefItem(name, rank, parentRankId);
		checkNull(rankId, "rank id" + name);
		geography.setRankId( rankId );
		
		if (parent != null && rankId <= parentRankId)
		{
			warn("Parent rank is greater or equal", geoUnit.getName());
		}

		// GeographyTreeDefItem
		GeographyTreeDefItem defItem = getTreeDefItemByRankId(rankId);
		checkNull(defItem, "tree def item");
		geography.setDefinitionItem(defItem);
        
		// Remarks
		String remarks = geoUnit.getRemarks();
		geography.setRemarks(remarks);

		// TimestampCreated
        Date dateCreated = geoUnit.getDateCreated();
        geography.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // Version
        geography.setVersion(1);

        return geography;
	}

	private Geography getSynonym(Geography geography, String name, String fullName)
	{
		Geography synonym = new Geography();
		
		// Abbreviation
		
		// AcceptedGeography
		synonym.setAcceptedGeography(geography);
		
		// CommonName
		
		// CreatedBy
		synonym.setCreatedByAgent(geography.getCreatedByAgent());
	    	
		// FullName
		fullName = truncate(name, 255, "variant full name");
		synonym.setFullName(fullName);
		
		// GeographyCode
		
		// GeographyTreeDef
	    synonym.setDefinition(treeDef);
		
		// GeographyTreeDefItem
	    synonym.setDefinitionItem(geography.getDefinitionItem());
		
		// GUID
		
		// IsAccepted
	    synonym.setIsAccepted(false);
	    
		// Name
		name = truncate(name, 64, "variant name");
	    synonym.setName(name);
	    
		// Parent
	    synonym.setParent(geography.getParent());

	    // RankID
	    synonym.setRankId(geography.getRankId());
		
		// Remarks
		
		// TimestampCreated
	    synonym.setTimestampCreated(geography.getTimestampCreated());
		
		// Version
	    synonym.setVersion(1);
	    
	    return synonym;
	}

	private String getQualifiedName(String name, String displayQualifier)
	{
	    if (displayQualifier != null)
        {
            return name + " (" + displayQualifier + ")";
        }
	    else
	    {
	        return name;
	    }
	}

	private String getInsertSql(Geography geography)
	{
	    String fieldNames = "Abbrev, AcceptedID, CommonName, CreatedByAgentID, FullName, GeographyCode, " +
	                        "GeographyTreeDefID, GeographyTreeDefItemID, GUID, IsAccepted, Name, " +
	                        "ParentID, RankID, Remarks, TimestampCreated, Version";

	    String[] values = new String[16];

	    values[0]  = SqlUtils.sqlString( geography.getAbbrev());
	    values[1]  = SqlUtils.sqlString( geography.getAcceptedGeography().getId());
	    values[2]  = SqlUtils.sqlString( geography.getCommonName());
	    values[3]  = SqlUtils.sqlString( geography.getCreatedByAgent().getId());
	    values[4]  = SqlUtils.sqlString( geography.getFullName());
	    values[5]  = SqlUtils.sqlString( geography.getGeographyCode());
	    values[6]  = SqlUtils.sqlString( geography.getDefinition().getId());
	    values[7]  = SqlUtils.sqlString( geography.getDefinitionItem().getId());
	    values[8]  = SqlUtils.sqlString( geography.getGuid());
	    values[9]  = SqlUtils.sqlString( geography.getIsAccepted());
	    values[10] = SqlUtils.sqlString( geography.getName());
	    values[11] = SqlUtils.sqlString( geography.getParent().getId());
	    values[12] = SqlUtils.sqlString( geography.getRankId());
	    values[13] = SqlUtils.sqlString( geography.getRemarks());
	    values[14] = SqlUtils.sqlString( geography.getTimestampCreated());
	    values[15] = SqlUtils.sqlString( geography.getVersion());

	    return SqlUtils.getInsertSql("geography", fieldNames, values);
	}

	private Integer getGeoTreeDefItem( String name, String type, Integer parentRank) {
	    
	    if (parentRank == null) return ROOT_RANK;

		if ( parentRank.equals( REGION_RANK ) && type.equals( OTHER_TYPE ) ) {
			if ( name.indexOf( "archipelago" ) >= 0 ) return ARCHIPELAGO_RANK;
			else if ( name.indexOf( "Island" ) >= 0 ) return SUBCONTINENT_ISL_RANK;
			else if ( name.indexOf( "Lake" ) >= 0 )   return LAKE_RANK;
			else if ( name.indexOf( "Land" ) >= 0 )   return LAND_RANK;
			else return CONTINENT_SUBREGION_RANK;
		}
		else if ( parentRank.equals( ARCHIPELAGO_RANK ) ) {
			if ( name.indexOf( "Straight" ) >= 0 )      return STRAIGHTS_RANK;
			else if ( name.indexOf( "Estrecho" ) >= 0 ) return STRAIGHTS_RANK;
		}
		else if ( parentRank.equals( LAND_RANK ) ) {
			if ( name.indexOf( "Island" ) >= 0 ) return SUBCONTINENT_ISL_RANK;
			else if ( name.indexOf( "Mount" ) >= 0 || name.indexOf( "Range" ) >= 0 ) return MOUNTAIN_RANK;
			else if ( name.indexOf( "Coast" ) >= 0 ) return COAST_RANK;
		}
		else if ( parentRank.equals( SUBCONTINENT_ISL_RANK ) && type.equals( OTHER_TYPE ) ) {
			return CONTINENT_SUBREGION_RANK;
		}
		else if ( parentRank.equals( COUNTRY_RANK )  && 
				( type.equals( OTHER_TYPE ) || type.equals( REGION_TYPE ) ) ) {
			if ( name.indexOf( "Territory" ) >= 0 ) return TERRITORY_RANK;
			else if ( name.indexOf( "Island" ) >= 0 || name.indexOf ( "Iles") >= 0 ) return SUBCOUNTRY_ISL_RANK;
			else if ( name.indexOf( "Mountain" ) >= 0 ) return MOUNTAIN_RANK;
			else if ( name.indexOf( "River" ) >= 0 ) return RIVER_RANK;
			else return COUNTRY_SUBREGION_RANK;
		}
		else if ( parentRank.equals( SUBCOUNTRY_ISL_RANK ) ) {
			if ( type.equals( COUNTY_TYPE ) )     return COUNTRY_RANK;
			else if ( type.equals( STATE_TYPE ) ) return STATE_RANK;
			else if ( type.equals( OTHER_TYPE ) ) return COUNTRY_SUBREGION_RANK;
		}
		else if ( parentRank.equals( TERRITORY_RANK ) ) {
			if ( name.indexOf( "Island" ) >= 0 ) return SUBCOUNTRY_ISL_RANK;
		}
		else if ( parentRank.equals( COUNTRY_SUBREGION_RANK ) ) {
			if ( type.equals( STATE_TYPE ) )      return STATE_RANK;
			else if ( type.equals( CITY_TYPE ) )  return CITY_RANK;
			else if ( type.equals( OTHER_TYPE ) ) return OTHER_RANK;
		}
		else if ( parentRank.equals( STATE_RANK ) && 
				( type.equals( OTHER_TYPE ) || type.equals( STATE_TYPE ) ) ) {
			if ( name.indexOf( "Coast" ) >= 0 )          return COAST_RANK;
			else if ( name.indexOf( "Hills" ) >= 0 )     return HILL_RANK;
			else if ( name.indexOf( "Island" ) >= 0 )    return SUBSTATE_ISL_RANK;
			else if ( name.indexOf( "Mountain" ) >= 0 )  return MOUNTAIN_RANK;
			else if ( name.indexOf( "Peninsula" ) >= 0 ) return PENINSULA_RANK;
			else if ( name.indexOf( "River" ) >= 0 )     return RIVER_RANK;
			else if ( name.indexOf( "Valley" ) >= 0 )    return VALLEY_RANK;
			else return STATE_SUBREGION_RANK;
		}
		else if ( parentRank.equals( SUBSTATE_ISL_RANK ) ) {
			if ( type.equals( COUNTY_TYPE ) )         return COUNTY_RANK;
			else if ( name.indexOf( "Island" ) >= 0 ) return ISLAND_RANK;
		}
		else if ( parentRank.equals( STATE_SUBREGION_RANK ) ) {
			if ( name.indexOf( "Island" ) >= 0 )   return ISLAND_RANK;
			else if ( type.equals( COUNTY_TYPE ) ) return COUNTY_RANK;
		}
		else if ( parentRank.equals( COUNTY_RANK ) && 
				( type.equals( OTHER_TYPE ) || type.equals( COUNTY_TYPE ) ) ) {
			if ( name.indexOf( "Canyon" ) >= 0 )        return CANYON_RANK;
			else if ( name.indexOf( "Forest" ) >= 0 )   return FOREST_RANK;
			else if ( name.indexOf( "Island" ) >= 0 )   return ISLAND_RANK;
			else if ( name.indexOf( "Lake" ) >= 0 )     return LAKE_RANK;
			else if ( name.indexOf( "Mountain" ) >= 0 ) return MOUNTAIN_RANK;
			else return COUNTY_SUBREGION_RANK;
		}
		else if ( parentRank.equals( CITY_RANK ) && ( type.equals( CITY_TYPE ) || type.equals( OTHER_TYPE ) ) ) {
			return CITY_SUBREGION_RANK;
		}
		else if ( type.equals( CITY_TYPE ) ) {
			return CITY_RANK;
		}
		else if ( type.equals( COUNTY_TYPE ) ) {
			return COUNTY_RANK;
		}
		else if ( type.equals( STATE_TYPE ) ) {
			return STATE_RANK;
		}
		else if ( type.equals( COUNTRY_TYPE ) ) {
			return COUNTRY_RANK;
		}
		else if ( type.equals( REGION_TYPE ) ) {
			return REGION_RANK;
		}
		else if ( type.equals( CONTINENT_TYPE ) ) {
			return CONTINENT_RANK;
		}

		return null;
	}
}
