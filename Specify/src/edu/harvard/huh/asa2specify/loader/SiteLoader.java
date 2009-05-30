package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Statement;

import edu.harvard.huh.asa.Site;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.harvard.huh.asa2specify.lookup.SiteLookup;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.util.LatLonConverter.FORMAT;

// Run this class after GeoUnitLoader.

public class SiteLoader extends CsvToSqlLoader
{
 
	// default locality name
	private static final String UNNAMED = "Unnamed locality";

	// set a default srcLatLongUnit
	private static final byte srcLatLongUnit = (byte) FORMAT.DDDDDD.ordinal();
	
	// lookup for geography
	private GeoUnitLookup geoLookup;

	private SiteLookup siteLookup;
	
	public SiteLoader(File asaCsvFile,
	                      Statement specifySqlStatement,
	                      GeoUnitLookup geoLookup) throws LocalException
	{
		super(asaCsvFile, specifySqlStatement);
		
		this.geoLookup = geoLookup;
	}

	public void loadRecord(String[] columns) throws LocalException
	{
		Site site = parse(columns);

		Integer siteId = site.getId();
		setCurrentRecordId(siteId);
		
		// skip this record if it only has SRE initial values set
		if (! site.hasData()) return;

		Locality locality = getLocality(site);

		// convert locality to sql and insert
		String sql = getInsertSql(locality);
		insert(sql);
	}
	
	public SiteLookup getSiteLookup()
	{
		if (siteLookup == null)
		{
			siteLookup = new SiteLookup() {

				public Locality queryById(Integer siteId) throws LocalException
				{
					String guid = getGuid(siteId);  // TODO: put a getLocalityById method in superclass

					Integer localityId = queryForInt("locality", "LocalityID", "GUID", guid);

					if (localityId == null) return null;

					Locality locality = new Locality();
					locality.setLocalityId(localityId);

					return locality;
				}
			};
		}
		return siteLookup;
	}

	// id, geo_unit_id, locality, latlong_method, latitude_a, longitude_a, latitude_b, longitude_b, elev_from, elev_to, elev_method
	private Site parse(String[] columns) throws LocalException
	{
		if (columns.length < 11)
		{
			throw new LocalException("Wrong number of columns");
		}

		Site site = new Site();
		try
		{
			site.setId(                SqlUtils.parseInt( columns[0]  ));
			site.setGeoUnitId(         SqlUtils.parseInt( columns[1]  ));
			site.setLocality(                             columns[2]  );
			site.setMethod(                               columns[3]  );
			site.setLatitudeA(  SqlUtils.parseBigDecimal( columns[4]  ));
			site.setLongitudeA( SqlUtils.parseBigDecimal( columns[5]  ));
			site.setLatitudeB(  SqlUtils.parseBigDecimal( columns[6]  ));
			site.setLongitudeB( SqlUtils.parseBigDecimal( columns[7]  ));
			site.setElevFrom(   SqlUtils.parseBigDecimal( columns[8]  ));
			site.setElevTo(     SqlUtils.parseBigDecimal( columns[9]  ));
			site.setElevMethod(                           columns[10] );
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return site;
	}

	private Locality getLocality(Site site) throws LocalException
	{		
		Locality locality = new Locality();
		
		// Disicpline
		locality.setDiscipline(getBotanyDiscipline());

		// ElevationMethod		
		String elevMethod = site.getElevMethod();
		if (elevMethod != null)
		{
			elevMethod = truncate(elevMethod, 50, "elevation method");
			locality.setElevationMethod( elevMethod );
		}

		// Geography
		Geography geography = null;
		Integer geoUnitId = site.getGeoUnitId();
		if (geoUnitId != null)
		{
			geography = lookup(geoUnitId);
			locality.setGeography(geography);
		}
		else
		{
			geography = new Geography();
		}
		locality.setGeography(geography);
		
		// GUID TODO: temporary, remove after import
		Integer siteId = site.getId();
		checkNull(siteId, "id");
		
		String guid = getGuid(siteId);
		locality.setGuid(guid);

		// Lat1Text, Latitude1
		BigDecimal latitudeA = site.getLatitudeA();
		if (latitudeA != null)
		{
			checkLatitude(latitudeA);
			locality.setLatitude1(latitudeA);
			locality.setLat1text(String.valueOf(latitudeA));
		}

		// Lat2Text, Latitude2
		BigDecimal latitudeB = site.getLatitudeB();
		if (latitudeB != null)
		{
			checkLatitude(latitudeB);
			locality.setLatitude2(latitudeB);
			locality.setLat2text(String.valueOf(latitudeB));
		}

		// LatLongMethod TODO: keep lat-long method?
		String method = site.getLatLongMethod();
		if (method != null)
		{
			if (method.length() > 50)
			{
				warn("Truncating lat/long method", method);
				method = method.substring(0, 50);
			}
			locality.setLatLongMethod(method);
		}

		// LocalityName is a required field, but we don't use it
		locality.setLocalityName(UNNAMED);

		// Long1Text, Longitude1
		BigDecimal longitudeA = site.getLongitudeA();
		if (longitudeA != null)
		{
			checkLongitude(longitudeA);
			locality.setLongitude1(longitudeA);
			locality.setLong1text(String.valueOf(longitudeA));
		}

		// Long2Text, Longitude2
		BigDecimal longitudeB = site.getLongitudeB();
		if (longitudeB != null)
		{
			checkLongitude(longitudeB);
			locality.setLongitude2(longitudeB);
			locality.setLong2text(String.valueOf(longitudeB));
		}

		// MaxElevation
		BigDecimal elevTo = site.getElevTo();
		if (elevTo != null)
		{
			locality.setMaxElevation(elevTo.doubleValue());
		}

		// MinElevation
		BigDecimal elevFrom = site.getElevFrom();
		if (elevFrom != null)
		{
			locality.setMinElevation(elevFrom.doubleValue());
		} 

		// Remarks seems to be the equivalent of the asa site.locality
		String remarks = site.getLocality();
		locality.setRemarks(remarks);

		// SrcLatLongUnit
		locality.setSrcLatLongUnit(srcLatLongUnit);
		
		return locality;
	}
    
	private String getGuid(Integer siteId)
	{
		return String.valueOf(siteId);
	}
    
	private Geography lookup(Integer geoUnitId) throws LocalException
	{
	    return geoLookup.getById(geoUnitId);
	}

	private void checkLatitude(BigDecimal latitude) throws LocalException
	{
		if (latitude.intValue() < 0 || latitude.intValue() > 90)
		{
			throw new LocalException("Invalid latitude: " + latitude);
		}
	}
	
	private void checkLongitude(BigDecimal longitude) throws LocalException
	{
		if (longitude.abs().intValue() > 180)
		{
			throw new LocalException("Invalid longitude: " + longitude);
		}
	}
	
	private String getInsertSql(Locality locality)
	{
		String fieldNames =
			"DisciplineID, ElevationMethod, GeographyID, GUID, LatLongMethod, Lat1Text, Lat2Text, " +
			"Latitude1, Latitude2, Long1Text, Long2Text, Longitude1, Longitude2, LocalityName, " +
			"MaxElevation, MinElevation, Remarks, SrcLatLongUnit, TimestampCreated";

		String[] values = new String[19];

		values[0]  = SqlUtils.sqlString( locality.getDiscipline().getId());
		values[1]  = SqlUtils.sqlString( locality.getElevationMethod());
		values[2]  = SqlUtils.sqlString( locality.getGeography().getId());
		values[3]  = SqlUtils.sqlString( locality.getGuid());
		values[4]  = SqlUtils.sqlString( locality.getLatLongMethod());
		values[5]  = SqlUtils.sqlString( locality.getLat1text());
		values[6]  = SqlUtils.sqlString( locality.getLat2text());
		values[7]  = SqlUtils.sqlString( locality.getLat1());
		values[8]  = SqlUtils.sqlString( locality.getLat2());
		values[9]  = SqlUtils.sqlString( locality.getLong1text());
		values[10] = SqlUtils.sqlString( locality.getLong2text());
		values[11] = SqlUtils.sqlString( locality.getLong1());
		values[12] = SqlUtils.sqlString( locality.getLong2());
		values[13] = SqlUtils.sqlString( locality.getLocalityName());
		values[14] = SqlUtils.sqlString( locality.getMaxElevation());
		values[15] = SqlUtils.sqlString( locality.getMinElevation());
		values[16] = SqlUtils.sqlString( locality.getRemarks());
		values[17] = SqlUtils.sqlString( locality.getSrcLatLongUnit());
		values[18] = SqlUtils.now();

		return SqlUtils.getInsertSql("locality", fieldNames, values);
	}
}
