package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.GeoUnit;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.GeographyTreeDefItem;

// Run this class after OptrLoader.

public class GeoUnitLoader extends TreeLoader
{
    private static final Logger log  = Logger.getLogger(GeoUnitLoader.class);
            
	private GeoUnitLookup geoLookup;
	   
	// root
	@SuppressWarnings("unused")
    private final String ROOT_TYPE = "root";
	private final Integer ROOT_RANK = 0;  // Specify value

	// continent
	private final String CONTINENT_TYPE = "continent";
	private final Integer CONTINENT_RANK = 100;  // Specify value

	// region
	private final String REGION_TYPE = "region";
	private final Integer REGION_RANK = 150;

	// sub-region
	@SuppressWarnings("unused")
	private final String ARCHIPELAGO_TYPE = "archipelago";
	private final Integer ARCHIPELAGO_RANK = 160;

	// country
	private final String COUNTRY_TYPE = "country";
	private final Integer COUNTRY_RANK = 200;  // Specify value

	// land
	@SuppressWarnings("unused")
	private final String LAND_TYPE = "land";
	private final Integer LAND_RANK = 210;

	// territory
	@SuppressWarnings("unused")
	private final String TERRITORY_TYPE = "territory";
	private final Integer TERRITORY_RANK = 220;

	// subcontinent island(s)
	@SuppressWarnings("unused")
	private final String SUBCONTINENT_ISL_TYPE = "subcontinent island(s)";
	private final Integer SUBCONTINENT_ISL_RANK = 230;

	// continent subregion
	@SuppressWarnings("unused")
	private final String CONTINTENT_SUBREGION_TYPE = "continent subregion";
	private final Integer CONTINENT_SUBREGION_RANK = 250;

	// country subregion
	@SuppressWarnings("unused")
	private final String COUNTRY_SUBREGION_TYPE = "country subregion";
	private final Integer COUNTRY_SUBREGION_RANK = 260;

	// straights
	@SuppressWarnings("unused")
	private final String STRAIGHTS_TYPE = "straights";
	private final Integer STRAIGHTS_RANK = 270;

	// subcountry island(s)
	@SuppressWarnings("unused")
	private final String SUBCOUNTRY_ISL_TYPE = "subcountry island(s)";
	private final Integer SUBCOUNTRY_ISL_RANK = 280;

	// state
	private final String STATE_TYPE = "state / province";
	private final Integer STATE_RANK = 300;  // Specify value

	// peninsula
	@SuppressWarnings("unused")
	private final String PENINSULA_TYPE = "peninsula";
	private final Integer PENINSULA_RANK = 310;

	// sub-state/province
	@SuppressWarnings("unused")
	private final String SUBSTATE_ISL_TYPE = "substate island(s)";
	private final Integer SUBSTATE_ISL_RANK = 320;

	// coast
	@SuppressWarnings("unused")
	private final String COAST_TYPE = "coast";
	private final Integer COAST_RANK = 330;

	// other
	private final String OTHER_TYPE = "other";
	private final Integer OTHER_RANK = 360;

	// state subregion
	@SuppressWarnings("unused")
	private final String STATE_SUBREGION_TYPE = "state subregion";
	private final Integer STATE_SUBREGION_RANK = 380;

	// county
	private final String COUNTY_TYPE = "county";
	private final Integer COUNTY_RANK = 400;  // Specify value

	// mountain(s)
	@SuppressWarnings("unused")
	private final String MOUNTAIN_TYPE = "mountain(s)";
	private final Integer MOUNTAIN_RANK = 410;

	// river
	@SuppressWarnings("unused")
	private final String RIVER_TYPE = "river";
	private final Integer RIVER_RANK = 420;

	// forest
	@SuppressWarnings("unused")
	private final String FOREST_TYPE = "forest";
	private final Integer FOREST_RANK = 430;

	// valley
	@SuppressWarnings("unused")
	private final String VALLEY_TYPE = "valley";
	private final Integer VALLEY_RANK = 440;

	// island
	@SuppressWarnings("unused")
	private final String ISLAND_TYPE = "island";
	private final Integer ISLAND_RANK = 450;

	// hills
	@SuppressWarnings("unused")
	private final String HILL_TYPE = "hill(s)";
	private final Integer HILL_RANK = 460;

	// canyon
	@SuppressWarnings("unused")
	private final String CANYON_TYPE = "canyon";
	private final Integer CANYON_RANK = 470;

	// lake
	@SuppressWarnings("unused")
	private final String LAKE_TYPE = "lake";
	private Integer LAKE_RANK = 480;

	// county subregion
	@SuppressWarnings("unused")
	private final String COUNTY_SUBREGION_TYPE = "county subregion";
	private final Integer COUNTY_SUBREGION_RANK = 490;

	// city / town
	private final String CITY_TYPE = "city / town";
	private final Integer CITY_RANK = 500;  // Specify value

	// sub-city
	@SuppressWarnings("unused")
	private final String CITY_SUBREGION_TYPE = "city subregion";
	private final Integer CITY_SUBREGION_RANK = 510;

	private GeographyTreeDef treeDef;

	private final Geography nullIdGeography;
	
	private HashMap <Integer, GeographyTreeDefItem> geoDefItemsByRank = new HashMap<Integer, GeographyTreeDefItem>();
	
	public GeoUnitLoader(File csvFile, Statement sqlStatement, OptrLookup optrLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		setOptrLookup(optrLookup);

		this.treeDef = getGeoTreeDef();
		this.nullIdGeography = new Geography();
		
		nullIdGeography.setGeographyId(null);
	}
	   
    private GeographyTreeDef getGeoTreeDef() throws LocalException
    {
        GeographyTreeDef g = new GeographyTreeDef();

        Integer geoTreeDefId = getId("geographytreedef", "GeographyTreeDefID", "Name", "Geography");

        g.setGeographyTreeDefId(geoTreeDefId);

        return g;
    }
    
	private GeographyTreeDefItem getTreeDefItemByRankId(Integer rankId) throws LocalException
	{
	    GeographyTreeDefItem treeDefItem = geoDefItemsByRank.get(rankId);

	    if (treeDefItem == null)
	    {
	        Integer geoTreeDefItemId = getInt("geographytreedefitem", "GeographyTreeDefItemID", "RankID", rankId);

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
		    getLogger().info("Processing " + name);
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

	public Logger getLogger()
	{
	    return log;
	}
	
	public void numberNodes() throws LocalException
	{
		numberNodes("geography", "GeographyID");
	}

    public GeoUnitLookup getGeographyLookup()
    {
        if (geoLookup == null)
        {
            geoLookup = new GeoUnitLookup() {

                public Geography getById(Integer geoUnitId) throws LocalException
                {
                    Geography geography = new Geography();

                    String guid = getGuid(geoUnitId);

                    Integer geographyId = getId("geography", "GeographyID", "GUID", guid);

                    geography.setGeographyId(geographyId);

                    return geography;
                }
            };
        }

        return geoLookup;
    }

    @Override
    protected void postLoad() throws LocalException
    {
        // TODO: probably drop this index after import
        getLogger().info("Creating guid index");
        String sql =  "create index guid on geography(GUID)";
        execute(sql);
    }
    
	private GeoUnit parse(String[] columns) throws LocalException
	{
	    if (columns.length < 15)
	    {
	        throw new LocalException("Not enough columns");
	    }

	    GeoUnit geoUnit = new GeoUnit();
	    try
	    {
	        geoUnit.setParentId(     SqlUtils.parseInt( columns[0]  ));
	        geoUnit.setId(           SqlUtils.parseInt( columns[1]  ));
	        geoUnit.setRank(                            columns[2]  );
	        geoUnit.setIsoCode(                         columns[3]  );
	        geoUnit.setDisplayQualifier(                columns[4]  );
	        geoUnit.setName(                            columns[5]  );
	        geoUnit.setVernacularName(                  columns[6]  );
	        geoUnit.setRemarks( SqlUtils.iso8859toUtf8( columns[7]  ));
	        geoUnit.setCreatedById(  SqlUtils.parseInt( columns[8]  ));
	        geoUnit.setDateCreated( SqlUtils.parseDate( columns[9]  ));
            geoUnit.addVariantName(                     columns[10] );
            geoUnit.addVariantName(                     columns[11] );
            geoUnit.addVariantName(                     columns[12] );
            geoUnit.addVariantName(                     columns[13] );
            geoUnit.addVariantName(                     columns[14] );
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
		
	    // AcceptedGeography TODO: is this correct?
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
			parent = getGeographyLookup().getById(parentId);
		}
		else
		{
		    parent = new Geography();
		    parent.setGeographyId(1); // TODO: this only happens to work
		}
		
		geography.setParent(parent);

		// RankId
		String rank = geoUnit.getRank();
		Integer parentRankId = getInt("geography", "RankID", "GeographyID", parent.getId());
		
		Integer rankId = getGeoTreeDefItem(name, rank, parentRankId);
		checkNull(rankId, "rank id for " + name);
		geography.setRankId( rankId );
		
		if (parent != null && rankId <= parentRankId)
		{
			getLogger().warn(rec() + "Parent rank is greater or equal: " + geoUnit.getName());
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

	private String getGuid(Integer geoUnitId)
	{
	    return String.valueOf(geoUnitId);
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
	    String fieldNames = "AcceptedID, CommonName, CreatedByAgentID, FullName, GeographyCode, " +
	                        "GeographyTreeDefID, GeographyTreeDefItemID, GUID, IsAccepted, Name, " +
	                        "ParentID, RankID, Remarks, TimestampCreated, Version";

	    String[] values = new String[15];

	    values[0]  = SqlUtils.sqlString( geography.getAcceptedGeography().getId());
	    values[1]  = SqlUtils.sqlString( geography.getCommonName());
	    values[2]  = SqlUtils.sqlString( geography.getCreatedByAgent().getId());
	    values[3]  = SqlUtils.sqlString( geography.getFullName());
	    values[4]  = SqlUtils.sqlString( geography.getGeographyCode());
	    values[5]  = SqlUtils.sqlString( geography.getDefinition().getId());
	    values[6]  = SqlUtils.sqlString( geography.getDefinitionItem().getId());
	    values[7]  = SqlUtils.sqlString( geography.getGuid());
	    values[8]  = SqlUtils.sqlString( geography.getIsAccepted());
	    values[9]  = SqlUtils.sqlString( geography.getName());
	    values[10] = SqlUtils.sqlString( geography.getParent().getId());
	    values[11] = SqlUtils.sqlString( geography.getRankId());
	    values[12] = SqlUtils.sqlString( geography.getRemarks());
	    values[13] = SqlUtils.sqlString( geography.getTimestampCreated());
	    values[14] = SqlUtils.sqlString( geography.getVersion());

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
			if ( type.equals( COUNTY_TYPE ) )     return COUNTY_RANK;
			else if ( type.equals( STATE_TYPE ) ) return STATE_RANK;
			else if ( type.equals( OTHER_TYPE ) ) return OTHER_RANK;
		}
		else if ( parentRank.equals( TERRITORY_RANK ) ) {
			if ( name.indexOf( "Island" ) >= 0 ) return SUBCOUNTRY_ISL_RANK;
		}
		else if ( parentRank.equals( COUNTRY_SUBREGION_RANK ) ) {
			if ( type.equals( STATE_TYPE ) )       return STATE_RANK;
			else if ( type.equals( COUNTY_TYPE ) ) return COUNTY_RANK;
			else if ( type.equals( CITY_TYPE ) )   return CITY_RANK;
			else if ( type.equals( OTHER_TYPE ) )  return OTHER_RANK;
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
