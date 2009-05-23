package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.sql.Statement;

import edu.harvard.huh.asa.Site;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
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
		Site site = parse(columns);

		// skip this record if it only has SRE initial values set
		if (! site.hasData()) return;

		// find matching geography
		Geography geography = new Geography();
		Integer geoUnitId = site.getGeoUnitId();
		if (geoUnitId != null)
		{
			String guid = String.valueOf(geoUnitId);

			Integer geographyId = getIntByField("geography", "GeographyID", "GUID", guid);

			geography.setGeographyId(geographyId);
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
	private Site parse(String[] columns) throws LocalException
	{
		if (columns.length < 11)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Site object
		Site site = new Site();

		try
		{
			site.setId(                 Integer.parseInt( StringUtils.trimToNull( columns[0]  )));
			site.setGeoUnitId(         SqlUtils.parseInt( StringUtils.trimToNull( columns[1]  )));
			site.setLocality(                             StringUtils.trimToNull( columns[2]  ));
			site.setMethod(                               StringUtils.trimToNull( columns[3]  ));
			site.setLatitudeA(  SqlUtils.parseBigDecimal( StringUtils.trimToNull( columns[4]  )));
			site.setLongitudeA( SqlUtils.parseBigDecimal( StringUtils.trimToNull( columns[5]  )));
			site.setLatitudeB(  SqlUtils.parseBigDecimal( StringUtils.trimToNull( columns[6]  )));
			site.setLongitudeB( SqlUtils.parseBigDecimal( StringUtils.trimToNull( columns[7]  )));
			site.setElevFrom(   SqlUtils.parseBigDecimal( StringUtils.trimToNull( columns[8]  )));
			site.setElevTo(     SqlUtils.parseBigDecimal( StringUtils.trimToNull( columns[9]  )));
			site.setElevMethod(                           StringUtils.trimToNull( columns[10] ));
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
				warn("Truncating elev method", site.getId(), elevMethod);
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
				warn("Truncating lat/long method", site.getId(), method);
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

		String[] values = new String[18];

		values[0]  = SqlUtils.sqlString( locality.getElevationMethod());
		values[1]  = SqlUtils.sqlString( locality.getGuid());
		values[2]  = SqlUtils.sqlString( locality.getLatLongMethod());
		values[3]  = SqlUtils.sqlString( locality.getLat1text());
		values[4]  = SqlUtils.sqlString( locality.getLat2text());
		values[5]  = SqlUtils.sqlString( locality.getLat1());
		values[6]  = SqlUtils.sqlString( locality.getLat2());
		values[7]  = SqlUtils.sqlString( locality.getLong1text());
		values[8]  = SqlUtils.sqlString( locality.getLong2text());
		values[9]  = SqlUtils.sqlString( locality.getLong1());
		values[10] = SqlUtils.sqlString( locality.getLong2());
		values[11] = SqlUtils.sqlString( locality.getLocalityName());
		values[12] = SqlUtils.sqlString( locality.getMaxElevation());
		values[13] = SqlUtils.sqlString( locality.getMinElevation());
		values[14] = SqlUtils.sqlString( locality.getSrcLatLongUnit());
		values[15] = SqlUtils.sqlString( locality.getDiscipline().getId());
		values[16] = SqlUtils.now();
		values[17] = SqlUtils.sqlString( locality.getRemarks());

		return SqlUtils.getInsertSql("locality", fieldNames, values);
	}
}
