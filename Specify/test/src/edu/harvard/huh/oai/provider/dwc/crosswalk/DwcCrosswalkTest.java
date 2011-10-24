package edu.harvard.huh.oai.provider.dwc.crosswalk;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import dwc.huh_harvard_edu.tdwg_dwc_simple.SimpleDarwinRecord;

import edu.harvard.huh.specify.mapper.SpecifyMapper;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class DwcCrosswalkTest {

	// see au.org.tern.ecoinformatics.oai.provider.util.DateUtils
	private String OAI_DATE_FORMAT = "yyyy-MM-dd";
	private String OAI_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	private DatatypeFactory datatypeFactory = null;
	private DwcCrosswalk crosswalk = null;
	
	HashMap<String, String>  map = null;
	
	HashMap<String, Integer> intMap = null;
	HashMap<String, Double> doubleMap = null;
	HashMap<String, XMLGregorianCalendar> dateMap = null;
	HashMap<String, BigInteger> bigIntMap = null;
	
	@Before
	public void setUp() {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		}
		catch (DatatypeConfigurationException e) {
			fail(e.getMessage());
		}
		
		crosswalk = new DwcCrosswalk();
		
		// this map will represent the results returned from calling SpecifyMapper on a CollectionObject;
		// it has lower-cased dwc term keys mapped to test values
		map = new HashMap<String, String>();

		// these are the dwc terms that have String values
		String[] stringFields = { "acceptedNameUsage", "acceptedNameUsageID", "accessRights", "associatedMedia", "associatedOccurrences", "associatedReferences", "associatedSequences",
				"associatedTaxa", "basisOfRecord", "bed", "behavior", "bibliographicCitation", "catalogNumber", "collectionCode", "collectionID", "continent", "coordinatePrecision", 
				"country", "countryCode", "county", "dataGeneralizations", "datasetID", "datasetName", "disposition", "dynamicProperties", "earliestEonOrLowestEonothem",
				"earliestEpochOrLowestSeries", "earliestEraOrLowestErathem", "earliestPeriodOrLowestSystem", "establishmentMeans", "eventID", "eventRemarks", "family", "fieldNotes",
				"fieldNumber", "footprintSRS", "footprintSpatialFit", "footprintWKT", "genus", "geodeticDatum", "geologicalContextID", "georeferenceProtocol", "georeferenceRemarks",
				"georeferenceSources", "georeferenceVerificationStatus", "georeferencedBy", "group", "habitat", "formation", "higherClassification", "higherGeography",
				"higherGeographyID", "highestBiostratigraphicZone", "identificationID", "identificationQualifier", "identificationReferences", "identificationRemarks", "kingdom", 
				"identifiedBy", "individualID", "informationWithheld", "infraspecificEpithet", "institutionCode", "institutionID", "island", "islandGroup", "language",
				"latestAgeOrHighestStage", "latestEonOrHighestEonothem", "latestEpochOrHighestSeries", "latestEraOrHighestErathem", "latestPeriodOrHighestSystem",
				"lithostratigraphicTerms", "lifeStage", "locality", "locationID", "locationAccordingTo", "locationRemarks", "lowestBiostratigraphicZone", "member", "municipality",
				"nameAccordingTo", "nameAccordingToID", "namePublishedIn", "namePublishedInID", "nomenclaturalCode", "nomenclaturalStatus", "occurrenceDetails", "occurrenceID",
				"occurrenceRemarks", "occurrenceStatus", "order", "originalNameUsage", "originalNameUsageID", "otherCatalogNumbers", "parentNameUsage", "parentNameUsageID",
				"phylum", "pointRadiusSpatialFit", "preparations", "previousIdentifications", "recordNumber", "recordedBy", "reproductiveCondition", "rights", "specificEpithet",
				"rightsHolder", "samplingEffort", "samplingProtocol", "scientificName", "scientificNameAuthorship", "scientificNameID", "sex", "stateProvince", "subgenus",
				"taxonConceptID", "taxonID", "taxonomicStatus", "taxonRank", "taxonRemarks", "type", "typeStatus", "verbatimCoordinateSystem", "verbatimCoordinates", "verbatimDepth",
				"verbatimCoordinateSystem", "verbatimElevation", "verbatimEventDate", "verbatimLatitude", "verbatimLocality", "verbatimLongitude", "verbatimSRS", "verbatimTaxonRank",
				"vernacularName", "waterbody" };

		String[] stringTestValues = stringFields;
		
		// just use the term name as the test value
		for (int i = 0; i < stringFields.length; i++) {
			map.put(stringFields[i].toLowerCase(), stringTestValues[i]);
		}

		// these are the dwc terms that have Integer values, some test values for them, and a map
		// to look up the test value by term (using the Specify lower-case key convention)
		String[] intFields = { "endDayOfYear", "startDayOfYear" };
		int[] intValues = { 1, 2 };
		intMap = new HashMap<String, Integer>();
		
		for (int j = 0; j < intFields.length; j++ ) {
			map.put(intFields[j].toLowerCase(), String.valueOf(intValues[j]));
			intMap.put(intFields[j].toLowerCase(), intValues[j]);
		}

		// these are the dwc terms that have Double values, some test values for them, and a map
		// to look up the test value by term (using the Specify lower-case key convention)
		String[] doubleFields = { "coordinateUncertaintyInMeters", "decimalLatitude", "decimalLongitude", "maximumDepthInMeters", "maximumDistanceAboveSurfaceInMeters",
				"maximumElevationInMeters",  "minimumDepthInMeters", "minimumDistanceAboveSurfaceInMeters", "minimumElevationInMeters" };
		double[] doubleValues = { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9 };
		doubleMap = new HashMap<String, Double>();
		
		for (int k = 0; k < doubleFields.length; k++ ) {
			map.put(doubleFields[k].toLowerCase(), String.valueOf(doubleValues[k]));
			doubleMap.put(doubleFields[k].toLowerCase(), doubleValues[k]);
		}

		// these are the dwc terms that have XMLGregorianCalendar values, some test values for them, and a map
		// to look up the test value by term (using the Specify lower-case key convention)
		String[] dateFields = { "dateIdentified", "day", "eventDate", "eventTime", "modified", "month", "year" };
		String[] dateStringValues = { "2000-01-02", "2001-02-03", "2002-03-04", "2003-04-05 06:07:08", "2004-05-06 07:08:09", "2005-06", "2006" };

		XMLGregorianCalendar[] dateValues = new XMLGregorianCalendar[dateFields.length];
		
		try {
			dateValues[0] = getXmlGregorianCalendar(crosswalk.getMapper().parseDate(dateStringValues[0]));
			dateValues[1] = getXmlGregorianCalendar(crosswalk.getMapper().parseDay(dateStringValues[1]));
			dateValues[2] = getXmlGregorianCalendar(crosswalk.getMapper().parseDate(dateStringValues[2]));
			dateValues[3] = getXmlGregorianCalendar(crosswalk.getMapper().parseTime(dateStringValues[3]));
			dateValues[4] = getXmlGregorianCalendar(crosswalk.getMapper().parseTime(dateStringValues[4]));
			dateValues[5] = getXmlGregorianCalendar(crosswalk.getMapper().parseMonth(dateStringValues[5]));
			dateValues[6] = getXmlGregorianCalendar(crosswalk.getMapper().parseYear(dateStringValues[6]));
		}
		catch (ParseException e) {
			fail(e.getMessage());
		}

		dateMap = new HashMap<String, XMLGregorianCalendar>();

		for (int n = 0; n < dateFields.length; n++) {
			map.put(dateFields[n].toLowerCase(), dateStringValues[n]);
			dateMap.put(dateFields[n].toLowerCase(), dateValues[n]);
		}

		// these are the dwc terms that have BigInteger values, some test values for them, and a map
		// to look up the test value by term (using the Specify lower-case key convention)
		String[] bigIntFields = { "individualCount" };
		BigInteger[] bigIntValues = { BigInteger.valueOf(1) };
		bigIntMap = new HashMap<String, BigInteger>();

		for (int q = 0; q < bigIntFields.length; q++) {
			map.put(bigIntFields[q].toLowerCase(), String.valueOf(bigIntValues[q]));
			bigIntMap.put(bigIntFields[q].toLowerCase(), bigIntValues[q]);
		}
		
		crosswalk.setMapper(new SpecifyMapper() {
			@Override
			public HashMap<String, String> map(CollectionObject collObj) {
				return map;
			}
		});
	}

	@Test
	public void testCrosswalk() {
		
		// test that invoking crosswalk on non-CollectionObject throws an exception
		SimpleDarwinRecord dwcRecord = null;
		try {
			Object nativeObject = new Integer(0);
			crosswalk.crosswalk(nativeObject);
			fail("Should not be able to crosswalk non-CollectionObject");
		}
		catch (IllegalArgumentException e) {
			;
		}

		dwcRecord = crosswalk.crosswalk(new CollectionObject());
		assertNotNull(dwcRecord);

		assertEquals(map.get("acceptedNameUsage".toLowerCase()), dwcRecord.getAcceptedNameUsage());
		assertEquals(map.get("acceptednameUsageID".toLowerCase()), dwcRecord.getAcceptedNameUsageID());
		assertEquals(map.get("accessRights".toLowerCase()), dwcRecord.getAccessRights());
		assertEquals(map.get("associatedMedia".toLowerCase()), dwcRecord.getAssociatedMedia());
		assertEquals(map.get("associatedOccurrences".toLowerCase()), dwcRecord.getAssociatedOccurrences());
		assertEquals(map.get("associatedReferences".toLowerCase()), dwcRecord.getAssociatedReferences());
		assertEquals(map.get("associatedSequences".toLowerCase()), dwcRecord.getAssociatedSequences());
		assertEquals(map.get("associatedTaxa".toLowerCase()), dwcRecord.getAssociatedTaxa());
		assertEquals("PreservedSpecimen", dwcRecord.getBasisOfRecord());
		assertEquals(map.get("bed".toLowerCase()), dwcRecord.getBed());
		assertEquals(map.get("behavior".toLowerCase()), dwcRecord.getBehavior());
		assertEquals(map.get("bibliographicCitation".toLowerCase()), dwcRecord.getBibliographicCitation());
		assertEquals(map.get("catalogNumber".toLowerCase()), dwcRecord.getCatalogNumber());
		assertEquals(map.get("collectionCode".toLowerCase()), dwcRecord.getCollectionCode());
		assertEquals(map.get("collectionID".toLowerCase()), dwcRecord.getCollectionID());
		assertEquals(map.get("continent"), dwcRecord.getContinent());
		assertEquals(map.get("coordinatePrecision".toLowerCase()), dwcRecord.getCoordinatePrecision());
		assertEquals(map.get("country"), dwcRecord.getCountry());
		assertEquals(map.get("countryCode".toLowerCase()), dwcRecord.getCountryCode());
		assertEquals(map.get("county"), dwcRecord.getCounty());
		assertEquals(map.get("dataGeneralizations".toLowerCase()), dwcRecord.getDataGeneralizations());
		assertEquals(map.get("datasetID".toLowerCase()), dwcRecord.getDatasetID());
		assertEquals(map.get("datasetName".toLowerCase()), dwcRecord.getDatasetName());
		assertEquals(map.get("disposition"), dwcRecord.getDisposition());
		assertEquals(map.get("dynamicProperties".toLowerCase()), dwcRecord.getDynamicProperties());
		assertEquals(map.get("earliestEonOrLowestEonothem".toLowerCase()), dwcRecord.getEarliestEonOrLowestEonothem());
		assertEquals(map.get("earliestEpochOrLowestSeries".toLowerCase()), dwcRecord.getEarliestEpochOrLowestSeries());
		assertEquals(map.get("earliestEraOrLowestErathem".toLowerCase()), dwcRecord.getEarliestEraOrLowestErathem());
		assertEquals(map.get("earliestPeriodOrLowestSystem".toLowerCase()), dwcRecord.getEarliestPeriodOrLowestSystem());
		assertEquals(map.get("establishmentMeans".toLowerCase()), dwcRecord.getEstablishmentMeans());
		assertEquals(map.get("eventID".toLowerCase()), dwcRecord.getEventID());
		assertEquals(map.get("eventRemarks".toLowerCase()), dwcRecord.getEventRemarks());
		assertEquals(map.get("family"), dwcRecord.getFamily());
		assertEquals(map.get("fieldNotes".toLowerCase()), dwcRecord.getFieldNotes());
		assertEquals(map.get("fieldNumber".toLowerCase()), dwcRecord.getFieldNumber());
		assertEquals(map.get("footprintSRS".toLowerCase()), dwcRecord.getFootprintSRS());
		assertEquals(map.get("footprintSpatialFit".toLowerCase()), dwcRecord.getFootprintSpatialFit());
		assertEquals(map.get("footprintWKT".toLowerCase()), dwcRecord.getFootprintWKT());
		assertEquals(map.get("genus"), dwcRecord.getGenus());
		assertEquals(map.get("geodeticDatum".toLowerCase()), dwcRecord.getGeodeticDatum());
		assertEquals(map.get("geologicalContextID".toLowerCase()), dwcRecord.getGeologicalContextID());
		assertEquals(map.get("georeferenceProtocol".toLowerCase()), dwcRecord.getGeoreferenceProtocol());
		assertEquals(map.get("georeferenceRemarks".toLowerCase()), dwcRecord.getGeoreferenceRemarks());
		assertEquals(map.get("georeferenceSources".toLowerCase()), dwcRecord.getGeoreferenceSources());
		assertEquals(map.get("georeferenceVerificationStatus".toLowerCase()), dwcRecord.getGeoreferenceVerificationStatus());
		assertEquals(map.get("georeferencedBy".toLowerCase()), dwcRecord.getGeoreferencedBy());
		assertEquals(map.get("group"), dwcRecord.getGroup());
		assertEquals(map.get("habitat"), dwcRecord.getHabitat());
		assertEquals(map.get("formation"), dwcRecord.getFormation());
		assertEquals(map.get("higherClassification".toLowerCase()), dwcRecord.getHigherClassification());
		assertEquals(map.get("higherGeography".toLowerCase()), dwcRecord.getHigherGeography());
		assertEquals(map.get("higherGeographyID".toLowerCase()), dwcRecord.getHigherGeographyID());
		assertEquals(map.get("highestBiostratigraphicZone".toLowerCase()), dwcRecord.getHighestBiostratigraphicZone());
		assertEquals(map.get("identificationID".toLowerCase()), dwcRecord.getIdentificationID());
		assertEquals(map.get("identificationQualifier".toLowerCase()), dwcRecord.getIdentificationQualifier());
		assertEquals(map.get("identificationReferences".toLowerCase()), dwcRecord.getIdentificationReferences());
		assertEquals(map.get("identificationRemarks".toLowerCase()), dwcRecord.getIdentificationRemarks());
		assertEquals(map.get("kingdom"), dwcRecord.getKingdom());
		assertEquals(map.get("identifiedBy".toLowerCase()), dwcRecord.getIdentifiedBy());
		assertEquals(map.get("individualID".toLowerCase()), dwcRecord.getIndividualID());
		assertEquals(map.get("informationWithheld".toLowerCase()), dwcRecord.getInformationWithheld());
		assertEquals(map.get("infraspecificEpithet".toLowerCase()), dwcRecord.getInfraspecificEpithet());
		assertEquals(map.get("institutionCode".toLowerCase()), dwcRecord.getInstitutionCode());
		assertEquals(map.get("institutionID".toLowerCase()), dwcRecord.getInstitutionID());
		assertEquals(map.get("island"), dwcRecord.getIsland());
		assertEquals(map.get("islandGroup".toLowerCase()), dwcRecord.getIslandGroup());
		assertEquals(map.get("language"), dwcRecord.getLanguage());
		assertEquals(map.get("latestAgeOrHighestStage".toLowerCase()), dwcRecord.getLatestAgeOrHighestStage());
		assertEquals(map.get("latestEonOrHighestEonothem".toLowerCase()), dwcRecord.getLatestEonOrHighestEonothem());
		assertEquals(map.get("latestEpochOrHighestSeries".toLowerCase()), dwcRecord.getLatestEpochOrHighestSeries());
		assertEquals(map.get("latestEraOrHighestErathem".toLowerCase()), dwcRecord.getLatestEraOrHighestErathem());
		assertEquals(map.get("latestPeriodOrHighestSystem".toLowerCase()), dwcRecord.getLatestPeriodOrHighestSystem());
		assertEquals(map.get("lithostratigraphicTerms".toLowerCase()), dwcRecord.getLithostratigraphicTerms());
		assertEquals(map.get("lifeStage".toLowerCase()), dwcRecord.getLifeStage());
		assertEquals(map.get("locality"), dwcRecord.getLocality());
		assertEquals(map.get("locationID".toLowerCase()), dwcRecord.getLocationID());
		assertEquals(map.get("locationAccordingTo".toLowerCase()), dwcRecord.getLocationAccordingTo());
		assertEquals(map.get("locationRemarks".toLowerCase()), dwcRecord.getLocationRemarks());
		assertEquals(map.get("lowestBiostratigraphicZone".toLowerCase()), dwcRecord.getLowestBiostratigraphicZone());
		assertEquals(map.get("member"), dwcRecord.getMember());
		assertEquals(map.get("municipality"), dwcRecord.getMunicipality());
		assertEquals(map.get("nameAccordingTo".toLowerCase()), dwcRecord.getNameAccordingTo());
		assertEquals(map.get("nameAccordingToID".toLowerCase()), dwcRecord.getNameAccordingToID());
		assertEquals(map.get("namePublishedIn".toLowerCase()), dwcRecord.getNamePublishedIn());
		assertEquals(map.get("namePublishedInID".toLowerCase()), dwcRecord.getNamePublishedInID());
		assertEquals(map.get("nomenclaturalCode".toLowerCase()), dwcRecord.getNomenclaturalCode());
		assertEquals(map.get("nomenclaturalStatus".toLowerCase()), dwcRecord.getNomenclaturalStatus());
		assertEquals(map.get("occurrenceDetails".toLowerCase()), dwcRecord.getOccurrenceDetails());
		assertEquals(map.get("occurrenceID".toLowerCase()), dwcRecord.getOccurrenceID());
		assertEquals(map.get("occurrenceRemarks".toLowerCase()), dwcRecord.getOccurrenceRemarks());
		assertEquals(map.get("occurrenceStatus".toLowerCase()), dwcRecord.getOccurrenceStatus());
		assertEquals(map.get("order"), dwcRecord.getOrder());
		assertEquals(map.get("originalNameUsage".toLowerCase()), dwcRecord.getOriginalNameUsage());
		assertEquals(map.get("originalNameUsageID".toLowerCase()), dwcRecord.getOriginalNameUsageID());
		assertEquals(map.get("otherCatalogNumbers".toLowerCase()), dwcRecord.getOtherCatalogNumbers());
		assertEquals(map.get("parentNameUsage".toLowerCase()), dwcRecord.getParentNameUsage());
		assertEquals(map.get("parentNameUsageID".toLowerCase()), dwcRecord.getParentNameUsageID());
		assertEquals(map.get("phylum"), dwcRecord.getPhylum());
		assertEquals(map.get("pointRadiusSpatialFit".toLowerCase()), dwcRecord.getPointRadiusSpatialFit());
		assertEquals(map.get("preparations"), dwcRecord.getPreparations());
		assertEquals(map.get("previousIdentifications".toLowerCase()), dwcRecord.getPreviousIdentifications());
		assertEquals(map.get("recordNumber".toLowerCase()), dwcRecord.getRecordNumber());
		assertEquals(map.get("recordedBy".toLowerCase()), dwcRecord.getRecordedBy());
		assertEquals(map.get("reproductiveCondition".toLowerCase()), dwcRecord.getReproductiveCondition());
		assertEquals(map.get("rights"), dwcRecord.getRights());
		assertEquals(map.get("specificEpithet".toLowerCase()), dwcRecord.getSpecificEpithet());
		assertEquals(map.get("rightsHolder".toLowerCase()), dwcRecord.getRightsHolder());
		assertEquals(map.get("samplingEffort".toLowerCase()), dwcRecord.getSamplingEffort());
		assertEquals(map.get("samplingProtocol".toLowerCase()), dwcRecord.getSamplingProtocol());
		assertEquals(map.get("scientificName".toLowerCase()), dwcRecord.getScientificName());
		assertEquals(map.get("scientificNameAuthorship".toLowerCase()), dwcRecord.getScientificNameAuthorship());
		assertEquals(map.get("scientificNameID".toLowerCase()), dwcRecord.getScientificNameID());
		assertEquals(map.get("sex"), dwcRecord.getSex());
		assertEquals(map.get("stateProvince".toLowerCase()), dwcRecord.getStateProvince());
		assertEquals(map.get("subgenus"), dwcRecord.getSubgenus());
		assertEquals(map.get("taxonConceptID".toLowerCase()), dwcRecord.getTaxonConceptID());
		assertEquals(map.get("taxonID".toLowerCase()), dwcRecord.getTaxonID());
		assertEquals(map.get("taxonomicStatus".toLowerCase()), dwcRecord.getTaxonomicStatus());
		assertEquals(map.get("taxonRank".toLowerCase()), dwcRecord.getTaxonRank());
		assertEquals(map.get("taxonRemarks".toLowerCase()), dwcRecord.getTaxonRemarks());
		assertEquals(map.get("type"), dwcRecord.getType());		
		assertEquals(map.get("typeStatus".toLowerCase()), dwcRecord.getTypeStatus());
		assertEquals(map.get("verbatimCoordinateSystem".toLowerCase()), dwcRecord.getVerbatimCoordinateSystem());
		assertEquals(map.get("verbatimCoordinates".toLowerCase()), dwcRecord.getVerbatimCoordinates());
		assertEquals(map.get("verbatimDepth".toLowerCase()), dwcRecord.getVerbatimDepth());
		assertEquals(map.get("verbatimCoordinateSystem".toLowerCase()), dwcRecord.getVerbatimCoordinateSystem());
		assertEquals(map.get("verbatimElevation".toLowerCase()), dwcRecord.getVerbatimElevation());
		assertEquals(map.get("verbatimEventDate".toLowerCase()), dwcRecord.getVerbatimEventDate());
		assertEquals(map.get("verbatimLatitude".toLowerCase()), dwcRecord.getVerbatimLatitude());
		assertEquals(map.get("verbatimLocality".toLowerCase()), dwcRecord.getVerbatimLocality());
		assertEquals(map.get("verbatimLongitude".toLowerCase()), dwcRecord.getVerbatimLongitude());
		assertEquals(map.get("verbatimSRS".toLowerCase()), dwcRecord.getVerbatimSRS());
		assertEquals(map.get("verbatimTaxonRank".toLowerCase()), dwcRecord.getVerbatimTaxonRank());
		assertEquals(map.get("vernacularName".toLowerCase()), dwcRecord.getVernacularName());
		assertEquals(map.get("waterbody"), dwcRecord.getWaterbody());
		
		assertEquals(intMap.get("endDayOfYear".toLowerCase()), dwcRecord.getEndDayOfYear());
		assertEquals(intMap.get("startDayOfYear".toLowerCase()), dwcRecord.getStartDayOfYear());
		
		assertEquals(doubleMap.get("coordinateUncertaintyInMeters".toLowerCase()), dwcRecord.getCoordinateUncertaintyInMeters());
		assertEquals(doubleMap.get("decimalLatitude".toLowerCase()), dwcRecord.getDecimalLatitude());
		assertEquals(doubleMap.get("decimalLongitude".toLowerCase()), dwcRecord.getDecimalLongitude());
		assertEquals(doubleMap.get("maximumDepthInMeters".toLowerCase()), dwcRecord.getMaximumDepthInMeters());
		assertEquals(doubleMap.get("maximumDistanceAboveSurfaceInMeters".toLowerCase()), dwcRecord.getMaximumDistanceAboveSurfaceInMeters());
		assertEquals(doubleMap.get("maximumElevationInMeters".toLowerCase()), dwcRecord.getMaximumElevationInMeters());
		assertEquals(doubleMap.get("minimumDepthInMeters".toLowerCase()), dwcRecord.getMinimumDepthInMeters());
		assertEquals(doubleMap.get("minimumDistanceAboveSurfaceInMeters".toLowerCase()), dwcRecord.getMinimumDistanceAboveSurfaceInMeters());
		assertEquals(doubleMap.get("minimumElevationInMeters".toLowerCase()), dwcRecord.getMinimumElevationInMeters());
		
		assertEquals(dateMap.get("dateIdentified".toLowerCase()), dwcRecord.getDateIdentified());
		assertEquals(dateMap.get("day"), dwcRecord.getDay());
		assertEquals(dateMap.get("eventDate".toLowerCase()), dwcRecord.getEventDate());
		assertEquals(dateMap.get("eventTime".toLowerCase()), dwcRecord.getEventTime());
		assertEquals(dateMap.get("modified"), dwcRecord.getModified());
		assertEquals(dateMap.get("month"), dwcRecord.getMonth());
		assertEquals(dateMap.get("year"), dwcRecord.getYear());
		
		assertEquals(bigIntMap.get("individualCount".toLowerCase()), dwcRecord.getIndividualCount());
	}

	@Test
	public void testCrosswalkToString() {
		// test that invoking crosswalkToString on non-CollectionObject throws an exception
		try {
			Object nativeObject = new Integer(0);
			crosswalk.crosswalk(nativeObject);
			fail("Should not be able to crosswalk non-CollectionObject");
		}
		catch (IllegalArgumentException e) {
			;
		}

		String dwcRecord = crosswalk.crosswalkToString(new CollectionObject());
		assertNotNull(dwcRecord);
	}

	private XMLGregorianCalendar getXmlGregorianCalendar(Date date) {

		GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		return datatypeFactory.newXMLGregorianCalendar(c);
	}

	@Test
	public void testGetDatestamp() {

		String timestampString1 = "1992-06-23 15:37:04";
		Timestamp timestamp1 = Timestamp.valueOf(timestampString1);

		CollectionObject collectionObject = new CollectionObject();
		collectionObject.setTimestampCreated(timestamp1);
		
		DwcCrosswalk crosswalk = new DwcCrosswalk();

		// test that without a modification timestamp, the creation timestamp is returned.
		assertEquals(crosswalk.getDatestamp(collectionObject), (new SimpleDateFormat(OAI_DATE_FORMAT)).format(timestamp1));
		
		String timestampString2 = "1998-12-10 08:00:04";
		Timestamp timestamp2 = Timestamp.valueOf(timestampString2);
		collectionObject.setTimestampModified(timestamp2);
		
		// test that with a modification timestamp, the modification timestamp is returned.
		assertEquals(crosswalk.getDatestamp(collectionObject), (new SimpleDateFormat(OAI_DATE_FORMAT)).format(timestamp2));
	}


}
