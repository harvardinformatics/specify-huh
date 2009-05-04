package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.GeoUnit;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.GeographyTreeDefItem;

public class GeoUnitLoader extends CsvToSqlLoader {

	// root
	private String ROOT_TYPE = "root";
	private Integer ROOT_RANK = 0;  // Specify value

	// continent
	private String CONTINENT_TYPE = "continent";
	private Integer CONTINENT_RANK = 100;  // Specify value

	// region
	private String REGION_TYPE = "region";
	private Integer REGION_RANK = 150;

	// sub-region
	private String ARCHIPELAGO_TYPE = "archipelago";
	private Integer ARCHIPELAGO_RANK = 160;

	// country
	private String COUNTRY_TYPE = "country";
	private Integer COUNTRY_RANK = 200;  // Specify value

	// land
	private String LAND_TYPE = "land";
	private Integer LAND_RANK = 210;

	// territory
	private String TERRITORY_TYPE = "territory";
	private Integer TERRITORY_RANK = 220;

	// subcontinent island(s)
	private String SUBCONTINENT_ISL_TYPE = "subcontinent island(s)";
	private Integer SUBCONTINENT_ISL_RANK = 230;

	// continent subregion
	private String CONTINTENT_SUBREGION_TYPE = "continent subregion";
	private Integer CONTINENT_SUBREGION_RANK = 250;

	// country subregion
	private String COUNTRY_SUBREGION_TYPE = "country subregion";
	private Integer COUNTRY_SUBREGION_RANK = 260;

	// straights
	private String STRAIGHTS_TYPE = "straights";
	private Integer STRAIGHTS_RANK = 270;

	// subcountry island(s)
	private String SUBCOUNTRY_ISL_TYPE = "subcountry island(s)";
	private Integer SUBCOUNTRY_ISL_RANK = 280;

	// state
	private String STATE_TYPE = "state / province";
	private Integer STATE_RANK = 300;  // Specify value

	// peninsula
	private String PENINSULA_TYPE = "peninsula";
	private Integer PENINSULA_RANK = 310;

	// sub-state/province
	private String SUBSTATE_ISL_TYPE = "substate island(s)";
	private Integer SUBSTATE_ISL_RANK = 320;

	// coast
	private String COAST_TYPE = "coast";
	private Integer COAST_RANK = 330;

	// other
	private String OTHER_TYPE = "other";
	private Integer OTHER_RANK = 360;

	// state subregion
	private String STATE_SUBREGION_TYPE = "state subregion";
	private Integer STATE_SUBREGION_RANK = 380;

	// county
	private String COUNTY_TYPE = "county";
	private Integer COUNTY_RANK = 400;  // Specify value

	// mountain(s)
	private String MOUNTAIN_TYPE = "mountain(s)";
	private Integer MOUNTAIN_RANK = 410;

	// river
	private String RIVER_TYPE = "river";
	private Integer RIVER_RANK = 420;

	// forest
	private String FOREST_TYPE = "forest";
	private Integer FOREST_RANK = 430;

	// valley
	private String VALLEY_TYPE = "valley";
	private Integer VALLEY_RANK = 440;

	// island
	private String ISLAND_TYPE = "island";
	private Integer ISLAND_RANK = 450;

	// hills
	private String HILL_TYPE = "hill(s)";
	private Integer HILL_RANK = 460;

	// canyon
	private String CANYON_TYPE = "canyon";
	private Integer CANYON_RANK = 470;

	// lake
	private String LAKE_TYPE = "lake";
	private Integer LAKE_RANK = 480;

	// county subregion
	private String COUNTY_SUBREGION_TYPE = "county subregion";
	private Integer COUNTY_SUBREGION_RANK = 490;

	// city / town
	private String CITY_TYPE = "city / town";
	private Integer CITY_RANK = 500;  // Specify value

	// sub-city
	private String CITY_SUBREGION_TYPE = "city subregion";
	private Integer CITY_SUBREGION_RANK = 510;

	private final Logger log = Logger.getLogger(GeoUnitLoader.class);

	private GeographyTreeDef treeDef;

	public GeoUnitLoader(File csvFile, Statement sqlStatement, GeographyTreeDef geoTreeDef)
	{
		super(csvFile, sqlStatement);
		this.treeDef = geoTreeDef;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		// TODO Auto-generated method stub

		GeoUnit geoUnit = parseGeoUnitRecord(columns);

		Geography geography = convert(geoUnit);

		geography.setDefinition(treeDef);

		// find matching parent
		Geography parent = new Geography();
		Integer parentId = geoUnit.getParentId();
		String guid = SqlUtils.sqlString(String.valueOf(parentId));

		String sql = SqlUtils.getQueryIdByFieldSql("geography", "GeographyID", "GUID", guid);

		parentId = queryForId(sql);
		if (parentId == null)
		{
			throw new LocalException("No parent found with guid " + guid);
		}
		parent.setGeographyId(parentId);

		// find parent's rank id
		sql = SqlUtils.getQueryRankByIdSql("geography", "GeographyID", parentId);
		Integer parentRankId = queryForId(sql);

		// RankId
		String rank = geoUnit.getRank();
		String name = geoUnit.getName();
		Integer rankId = getGeoTreeDefItem(name, rank, parentRankId);
		if (rankId == null)
		{
			throw new LocalException("No rank id for " + rank);
		}

		geography.setRankId( rankId );
		if ( parent != null && rankId <= parent.getRankId() ) {
			log.warn("Parent rank is greater or equal");
		}

		GeographyTreeDefItem defItem = treeDef.getDefItemByRank(rankId);

		if (defItem == null)
		{
			throw new LocalException("No tree def item for rank " + rankId);
		}
		geography.setDefinitionItem(defItem);
		sql = getInsertSql(geography);
		insert(sql);
	}

	public void numberNodes() throws LocalException
	{
		numberNodes("geography", "GeographyID");
	}

	private GeoUnit parseGeoUnitRecord(String[] columns) throws LocalException
	{
		GeoUnit geoUnit = new GeoUnit();  // TODO: implement

		return geoUnit;
	}

	private Geography convert(GeoUnit geoUnit) throws LocalException
	{
		Geography geography = new Geography(); // TODO: implement

		// GUID TODO: temporary, remove after import
		geography.setGuid( String.valueOf( geoUnit.getId() ) );

		// Abbreviation
		String abbrev = geoUnit.getAbbreviation();
		if (abbrev != null && abbrev.length() > 16)
		{
			log.warn("truncating abbreviation");
			abbrev = abbrev.substring(0, 16);
		}
		geography.setAbbrev(abbrev);

		// CommonName
		String vernacularName = geoUnit.getVernacularName();
		if (vernacularName != null && vernacularName.length() > 128)
		{
			log.warn("truncating vernacular name");
			vernacularName = vernacularName.substring(0, 128);
		}
		geography.setCommonName(vernacularName);

		// Name
		String name = geoUnit.getName();
		if ( name == null ) {
			throw new LocalException("No name");
		}
		if (name.length() > 64) {
			log.warn("Truncating name");
			name = name.substring(0, 64);
		}
		geography.setName(name);

		// FullName
		String qualifiedName = geoUnit.getQualifiedName();
		if (qualifiedName == null)
		{
			throw new LocalException("No qualified name");
		}
		if (qualifiedName.length() > 255)
		{
			log.warn("Truncating qualified name");
			qualifiedName = qualifiedName.substring(0, 255);
		}
		geography.setFullName(qualifiedName);

		// GeographyCode
		String isoCode = geoUnit.getIsoCode();
		if (isoCode != null && isoCode.length() > 8) {
			log.warn("Truncating iso code");
			isoCode = isoCode.substring(0, 8);
		}
		geography.setGeographyCode(isoCode);

		// Remarks
		String remarks = geoUnit.getRemarks();
		geography.setRemarks(remarks);

		// Text1 = variant names TODO: synonymize?
		//List <String> variantNames = geoUnit.getVariantNames();
		//String variantNamesList = join( "; ", variantNames );
		//if ( variantNamesList != null && variantNamesList.trim().length() > 0 ) {
		//    geography.setText1( substring( variantNamesList.trim(), 32, "variant name" ) );
		//}

		return geography;
	}

	private String getInsertSql(Geography geography)
	{
		String fieldNames = ""; // TODO: implement

		List<String> values = new ArrayList<String>();

		return SqlUtils.getInsertSql("geography", fieldNames, values);
	}

	public Integer getGeoTreeDefItem( String name, String type, Integer parentRank) {

		if (parentRank == null) return ROOT_RANK;

		if ( parentRank == REGION_RANK && type.equals( OTHER_TYPE ) ) {
			if ( name.indexOf( "archipelago") >= 0 ) return ARCHIPELAGO_RANK;
			else if ( name.indexOf( "Island" ) >= 0 ) return SUBCONTINENT_ISL_RANK;
			else if ( name.indexOf( "Lake" ) >= 0 ) return LAKE_RANK;
			else if ( name.indexOf( "Land" ) >= 0 ) return LAND_RANK;
			else return CONTINENT_SUBREGION_RANK;
		}
		else if ( parentRank == ARCHIPELAGO_RANK  ) {
			if ( name.indexOf( "Straight" ) >= 0 ) return STRAIGHTS_RANK;
			else if ( name.indexOf( "Estrecho" ) >= 0 ) return STRAIGHTS_RANK;
		}
		else if ( parentRank == LAND_RANK  ) {
			if ( name.indexOf( "Island" ) >= 0 ) return SUBCONTINENT_ISL_RANK;
			else if ( name.indexOf( "Mount" ) >= 0 || name.indexOf( "Range" ) >= 0 ) return MOUNTAIN_RANK;
			else if ( name.indexOf( "Coast" ) >= 0 ) return COAST_RANK;
		}
		else if ( parentRank == SUBCONTINENT_ISL_RANK   && type.equals( OTHER_TYPE ) ) {
			return CONTINENT_SUBREGION_RANK;
		}
		else if ( parentRank == COUNTRY_RANK  && 
				(type.equals( OTHER_TYPE ) || type.equals( REGION_TYPE ) ) ) {
			if ( name.indexOf( "Territory" ) >= 0 ) return TERRITORY_RANK;
			else if ( name.indexOf( "Island" ) >= 0 || name.indexOf ( "Iles") >= 0 ) return SUBCOUNTRY_ISL_RANK;
			else if ( name.indexOf( "Mountain" ) >= 0 ) return MOUNTAIN_RANK;
			else if ( name.indexOf( "River" ) >= 0 ) return RIVER_RANK;
			else return COUNTRY_SUBREGION_RANK;
		}
		else if ( parentRank == SUBCOUNTRY_ISL_RANK ) {
			if ( type.equals( COUNTY_TYPE ) ) return COUNTRY_RANK;
			else if ( type.equals( STATE_TYPE ) ) return STATE_RANK;
			else if ( type.equals( OTHER_TYPE ) ) return COUNTRY_SUBREGION_RANK;
		}
		else if ( parentRank == TERRITORY_RANK ) {
			if ( name.indexOf( "Island" ) >= 0 ) return SUBCOUNTRY_ISL_RANK;
		}
		else if ( parentRank == COUNTRY_SUBREGION_RANK ) {
			if ( type.equals( STATE_TYPE ) ) return STATE_RANK;
			else if ( type.equals( CITY_TYPE ) ) return CITY_RANK;
			else if ( type.equals( OTHER_TYPE ) ) return OTHER_RANK;
		}
		else if ( parentRank == STATE_RANK && 
				( type.equals( OTHER_TYPE ) || type.equals( STATE_TYPE ) ) ) {
			if ( name.indexOf( "Coast" ) >= 0 ) return COAST_RANK;
			else if ( name.indexOf( "Hills" ) >= 0 ) return HILL_RANK;
			else if ( name.indexOf( "Island" ) >= 0 ) return SUBSTATE_ISL_RANK;
			else if ( name.indexOf( "Mountain" ) >= 0 ) return MOUNTAIN_RANK;
			else if ( name.indexOf( "Peninsula" ) >= 0 ) return PENINSULA_RANK;
			else if ( name.indexOf( "River" ) >= 0 ) return RIVER_RANK;
			else if ( name.indexOf( "Valley" ) >= 0 ) return VALLEY_RANK;
			else return STATE_SUBREGION_RANK;
		}
		else if ( parentRank == SUBSTATE_ISL_RANK  ) {
			if ( type.equals( COUNTY_TYPE ) ) return COUNTY_RANK;
			else if ( name.indexOf( "Island" ) >= 0 ) return ISLAND_RANK;
		}
		else if ( parentRank == STATE_SUBREGION_RANK ) {
			if ( name.indexOf( "Island" ) >= 0 ) return ISLAND_RANK;
			else if ( type.equals( COUNTY_TYPE ) ) return COUNTY_RANK;
		}
		else if ( parentRank == COUNTY_RANK && 
				( type.equals( OTHER_TYPE ) || type.equals( COUNTY_TYPE ) ) ) {
			if ( name.indexOf( "Canyon" ) >= 0 ) return CANYON_RANK;
			else if ( name.indexOf( "Forest" ) >= 0 ) return FOREST_RANK;
			else if ( name.indexOf( "Island" ) >= 0 ) return ISLAND_RANK;
			else if ( name.indexOf( "Lake" ) >= 0 ) return LAKE_RANK;
			else if ( name.indexOf( "Mountain" ) >= 0 ) return MOUNTAIN_RANK;
			else return COUNTY_SUBREGION_RANK;
		}
		else if ( parentRank == CITY_RANK && ( type.equals( CITY_TYPE ) || type.equals( OTHER_TYPE ) ) ) {
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
