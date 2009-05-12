package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.sql.Statement;

import edu.harvard.huh.asa.Site;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.util.LatLonConverter.FORMAT;

public class LocalityLoader extends CsvToSqlLoader
{
	private final Logger log  = Logger.getLogger(LocalityLoader.class);

	private Discipline discipline;

	// default locality name
	private static final String UNNAMED = "Unnamed locality";

	// set a default srcLatLongUnit
	private static final byte srcLatLongUnit = (byte) FORMAT.DDDDDD.ordinal();

	public LocalityLoader(File asaCsvFile, Statement specifySqlStatement) throws LocalException
	{
		super(asaCsvFile, specifySqlStatement);
		this.discipline = getBotanyDiscipline();
	}

	public void loadRecord(String[] columns) throws LocalException
	{
		Site site = parseSiteRecord(columns);

		// skip this record if it only has SRE initial values set
		if (! site.hasData()) return;

		// find matching geography
		Geography geography = new Geography();
		Integer geoUnitId = site.getGeoUnitId();
		if (geoUnitId != null)
		{
			String guid = String.valueOf(geoUnitId);
			String  sql = SqlUtils.getQueryIdByFieldSql("geography", "GeographyID", "GUID", guid);

			Integer geographyId = queryForId(sql);

			if (geographyId == null)
			{
				log.error("Couldn't find GeographyID with GUID " + guid);
			}
			else
			{
				geography.setGeographyId(geographyId);
			}
		}

		// convert site into locality
		Locality locality = convert(site);
		locality.setSrcLatLongUnit(srcLatLongUnit);
		locality.setDiscipline(discipline);
		locality.setGeography(geography);

		// convert locality to sql and insert
		String sql = getInsertSql(locality);
		insert(sql);
	}

	// id, geo_unit_id, locality, latlong_method, latitude_a, longitude_a, latitude_b, longitude_b, elev_from, elev_to, elev_method
	private Site parseSiteRecord(String[] columns) throws LocalException
	{
		if (columns.length < 11)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Site object
		Site site = new Site();

		try {
			site.setId(       Integer.parseInt(StringUtils.trimToNull(columns[0])));
			site.setGeoUnitId(Integer.parseInt(StringUtils.trimToNull(columns[1])));
			site.setLocality(                  StringUtils.trimToNull(columns[2]));
			site.setMethod(                    StringUtils.trimToNull(columns[3]));

			String lat1Str = StringUtils.trimToNull(columns[4]);
			if (lat1Str != null)
			{
				site.setLatitudeA(BigDecimal.valueOf(Double.parseDouble(lat1Str)));
			}

			String long1Str = StringUtils.trimToNull(columns[5]);
			if (long1Str != null)
			{
				site.setLongitudeA(BigDecimal.valueOf(Double.parseDouble(long1Str)));
			}

			String lat2Str = StringUtils.trimToNull(columns[6]);
			if (lat2Str != null)
			{
				site.setLatitudeB(BigDecimal.valueOf(Double.parseDouble(lat2Str)));
			}

			String long2Str = StringUtils.trimToNull(columns[7]);
			if (long2Str != null)
			{
				site.setLongitudeB(BigDecimal.valueOf(Double.parseDouble(long2Str)));
			}

			String elevFromStr = StringUtils.trimToNull(columns[8]);
			if (elevFromStr != null)
			{
				site.setElevFrom(Integer.parseInt(elevFromStr));
			}

			String elevToStr = StringUtils.trimToNull(columns[9]);
			if (elevToStr != null)
			{
				site.setElevTo(Integer.parseInt(elevToStr));
			}

			site.setElevMethod(StringUtils.trimToNull(columns[10]));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return site;
	}

	private Locality convert(Site site)
	{
		Locality locality = new Locality();

		String elevMethod = site.getElevMethod();
		if (elevMethod != null)
		{
			if (elevMethod.length() > 50)
			{
				log.warn("truncating elev method");
				elevMethod = elevMethod.substring(0, 50);
			}
			locality.setElevationMethod( elevMethod );
		}

		// GUID TODO: temporary, remove after import
		locality.setGuid(String.valueOf(site.getId()));

		// Lat1Text, Latitude1 TODO: any validity checks?
		BigDecimal latitudeA = site.getLatitudeA();
		if (latitudeA != null)
		{
			locality.setLatitude1( latitudeA );
			locality.setLat1text( String.valueOf( latitudeA ) );
		}

		// Lat2Text, Latitude2 TODO: any validity checks?
		BigDecimal latitudeB = site.getLatitudeB();
		if (latitudeB != null)
		{
			locality.setLatitude2(latitudeB);
			locality.setLat2text(String.valueOf(latitudeB));
		}

		// LatLongMethod TODO: do we want this?
		String method = site.getLatLongMethod();
		if (method != null)
		{
			if (method.length() > 50)
			{
				log.warn("truncating lat/long method");
				method = method.substring(0, 50);
			}
			locality.setLatLongMethod(method);
		}

		// LocalityName is a required field, but we don't use it
		locality.setLocalityName(UNNAMED);

		// Long1Text, Longitude1 TODO: any validty checks?
		BigDecimal longitudeA = site.getLongitudeA();
		if (longitudeA != null)
		{
			locality.setLongitude1(longitudeA);
			locality.setLong1text(String.valueOf(longitudeA));
		}

		// Long2Text, Longitude2 TODO: any validty checks?
		BigDecimal longitudeB = site.getLongitudeB();
		if (longitudeB != null)
		{
			locality.setLongitude2(longitudeB);
			locality.setLong2text(String.valueOf(longitudeB));
		}

		// MaxElevation TODO: validity checks?
		Integer elevTo = site.getElevTo();
		if (elevTo != null)
		{
			locality.setMaxElevation(elevTo.doubleValue());
		}

		// MinElevation
		Integer elevFrom = site.getElevFrom();
		if (elevFrom != null)
		{
			locality.setMinElevation(elevFrom.doubleValue());
		} 

		// Remarks seems to be the equivalent of the asa site.locality
		String remarks = site.getLocality();
		if (remarks != null)
		{
			locality.setRemarks( remarks );
		}

		return locality;
	}

	private String getInsertSql(Locality locality)
	{
		String fieldNames =
			"ElevationMethod, GUID, LatLongMethod, Lat1Text, Lat2Text, Latitude1, Latitude2, " +
			"Long1Text, Long2Text, Longitude1, Longitude2, LocalityName, MaxElevation, " +
			"MinElevation, SrcLatLongUnit, DisciplineID, TimestampCreated, Remarks";

		List<String> values = new ArrayList<String>(18);

		values.add(SqlUtils.sqlString(locality.getElevationMethod()    ));
		values.add(SqlUtils.sqlString(locality.getGuid()               ));
		values.add(SqlUtils.sqlString(locality.getLatLongMethod()      ));
		values.add(SqlUtils.sqlString(locality.getLat1text()           ));
		values.add(SqlUtils.sqlString(locality.getLat2text()           ));
		values.add(    String.valueOf(locality.getLat1()               ));
		values.add(    String.valueOf(locality.getLat2()               ));
		values.add(SqlUtils.sqlString(locality.getLong1text()          ));
		values.add(SqlUtils.sqlString(locality.getLong2text()          ));
		values.add(    String.valueOf(locality.getLong1()              ));
		values.add(    String.valueOf(locality.getLong2()              ));
		values.add(SqlUtils.sqlString(locality.getLocalityName()       ));
		values.add(    String.valueOf(locality.getMaxElevation()       ));
		values.add(    String.valueOf(locality.getMinElevation()       ));
		values.add(    String.valueOf(locality.getSrcLatLongUnit()     ));
		values.add(    String.valueOf(locality.getDiscipline().getId() ));
		values.add("now()");
		values.add(SqlUtils.sqlString(locality.getRemarks()            ));

		return SqlUtils.getInsertSql("locality", fieldNames, values);
	}
}
