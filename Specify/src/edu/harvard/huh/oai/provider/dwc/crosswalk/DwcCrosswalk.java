package edu.harvard.huh.oai.provider.dwc.crosswalk;

import java.io.StringWriter;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import au.org.tern.ecoinformatics.oai.provider.util.DateUtils;
import dwc.huh_harvard_edu.tdwg_dwc_simple.SimpleDarwinRecord;
import dwc.huh_harvard_edu.tdwg_dwc_simple.TypeEnum;
import edu.harvard.huh.oai.provider.crosswalk.Crosswalk;
import edu.harvard.huh.specify.mapper.SpecifyMapper;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class DwcCrosswalk implements Crosswalk {

	private static Logger logger = Logger.getLogger(DwcCrosswalk.class);

	private static String BASIS_OF_RECORD = "PreservedSpecimen";
	
	private SpecifyMapper mapper;
	private DatatypeFactory datatypeFactory;
	
	public DwcCrosswalk() {
		;
	}
		
	@Override
	public SimpleDarwinRecord crosswalk(Object nativeObject) {

		if (! (nativeObject instanceof CollectionObject)) {
			throw new IllegalArgumentException("Can't apply crosswalk to anything but CollectionObject");
		}

		HashMap<String, String> map = getMapper().map((CollectionObject) nativeObject);

		return getSimpleDarwinRecord(map);
	}

	// TODO: Specify lowercases darwin core terms; the options are to match the Specify version to the real
	// version in SpecifyMapper, or deal with it here. 
	protected SimpleDarwinRecord getSimpleDarwinRecord(HashMap<String, String> map) {
		SimpleDarwinRecord dwcRecord = new SimpleDarwinRecord();

		dwcRecord.setType(TypeEnum.OCCURRENCE.toString());

		//String abstrct = map.get("abstract");
		//if (abstrct != null) dwcRecord.setAbstract(abstrct);

		String acceptedNameUsage = map.get("acceptedNameUsage".toLowerCase());
		if (acceptedNameUsage != null) dwcRecord.setAcceptedNameUsage(acceptedNameUsage);

		String acceptedNameUsageID = map.get("acceptedNameUsageID".toLowerCase());
		if (acceptedNameUsageID != null) dwcRecord.setAcceptedNameUsageID(acceptedNameUsageID);

		String accessRights = map.get("accessRights".toLowerCase());
		if (accessRights != null) dwcRecord.setAccessRights(accessRights);

		//String accrualMethod = map.get("accrualMethod".toLowerCase());
		//if (accrualMethod != null) dwcRecord.setAccrualMethod(accrualMethod);

		//String accrualPeriodicity = map.get("accrualPeriodicity".toLowerCase());
		//if (accrualPeriodicity != null) dwcRecord.setAccrualPeriodicity(accrualPeriodicity);

		//String accrualPolicy = map.get("accrualPolicy".toLowerCase());
		//if (accrualPolicy != null) dwcRecord.setAccrualPolicy(accrualPolicy);

		//String alternative = map.get("alternative");
		//if (alternative != null) dwcRecord.setAlternative(alternative);
		
		String associatedMedia = map.get("associatedMedia".toLowerCase());
		if (associatedMedia != null) dwcRecord.setAssociatedMedia(associatedMedia);

		String associatedOccurrences = map.get("associatedOccurrences".toLowerCase());
		if (associatedOccurrences != null) dwcRecord.setAssociatedOccurrences(associatedOccurrences);

		String associatedReferences = map.get("associatedReferences".toLowerCase());
		if (associatedReferences != null) dwcRecord.setAssociatedReferences(associatedReferences);

		String associatedSequences = map.get("associatedSequences".toLowerCase());
		if (associatedSequences != null) dwcRecord.setAssociatedSequences(associatedSequences);

		String associatedTaxa = map.get("associatedTaxa".toLowerCase());
		if (associatedMedia != null) dwcRecord.setAssociatedTaxa(associatedTaxa);

		//String audience = map.get("audience");
		//if (audience != null) dwcRecord.setAudience(audience);

		//String available = map.get("available");
		//if (available != null) dwcRecord.setAvailable(available);

		String basisOfRecord = map.get("basisOfRecord".toLowerCase());
		if (basisOfRecord != null) dwcRecord.setBasisOfRecord(BASIS_OF_RECORD);

		String bed = map.get("bed");
		if (bed != null) dwcRecord.setBed(bed);

		String behavior = map.get("behavior");
		if (behavior != null) dwcRecord.setBehavior(behavior);

		String bibliographicCitation = map.get("bibliographicCitation".toLowerCase());
		if (bibliographicCitation != null) dwcRecord.setBibliographicCitation(bibliographicCitation);

		String catalogNumber = map.get("catalogNumber".toLowerCase());
		if (catalogNumber != null) dwcRecord.setCatalogNumber(catalogNumber);
		
		String collectionCode = map.get("collectionCode".toLowerCase());
		if (collectionCode != null) dwcRecord.setCollectionCode(collectionCode);

		String collectionID = map.get("collectionID".toLowerCase());
		if (collectionID != null) dwcRecord.setCollectionID(collectionID);

		//String conformsTo = map.get("conformsTo".toLowerCase());
		//if (conformsTo != null) dwcRecord.setConformsTo(conformsTo);

		String continent = map.get("continent");
		if (continent != null) dwcRecord.setContinent(continent);

		//String contributor = map.get("contributor");
		//if (contributor != null) dwcRecord.setContributor(contributor);

		String coordinatePrecision = map.get("coordinatePrecision".toLowerCase());
		if (coordinatePrecision != null) dwcRecord.setCoordinatePrecision(coordinatePrecision);

		String coordinateUncertaintyInMeters = map.get("coordinateUncertaintyInMeters".toLowerCase());
		if (coordinateUncertaintyInMeters != null) {
			try {
				dwcRecord.setCoordinateUncertaintyInMeters(Double.parseDouble(coordinateUncertaintyInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("DecimalLatitude did not map to an integer: " + coordinateUncertaintyInMeters); // TODO: handle parse exception otherwise?
			}
		}
		
		String country = map.get("country");
		if (country != null) dwcRecord.setCountry(country);

		String countryCode = map.get("countryCode".toLowerCase());
		if (countryCode != null) dwcRecord.setCountryCode(countryCode);

		String county = map.get("county");
		if (county != null) dwcRecord.setCounty(county);

		//String coverage = map.get("coverage");
		//if (coverage != null) dwcRecord.setCoverage(coverage);

		//String created = map.get("created");
		//if (created != null) dwcRecord.setCreated(created);

		//String creator = map.get("creator");
		//if (creator != null) dwcRecord.setCreator(creator);

		String dataGeneralizations = map.get("dataGeneralizations".toLowerCase());
		if (dataGeneralizations != null) dwcRecord.setDataGeneralizations(dataGeneralizations);

		String datasetID = map.get("datasetID".toLowerCase());
		if (datasetID != null) dwcRecord.setDatasetID(datasetID);

		String datasetName = map.get("datasetName".toLowerCase());
		if (datasetName != null) dwcRecord.setDatasetName(datasetName);

		//String date = map.get("date");
		//if (date != null) dwcRecord.setDate(date);

		//String dateAccepted = map.get("dateAccepted".toLowerCase());
		//if (dateAccepted != null) dwcRecord.setDateAccepted(dateAccepted);

		String dateIdentified = map.get("dateIdentified".toLowerCase());
		if (dateIdentified != null) {
			try {
				Date date = getMapper().parseDate(dateIdentified);
				dwcRecord.setDateIdentified(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("DateIdentified did not map to a date: " + dateIdentified, pe); // TODO: handle parse exception otherwise?
			}
		}
		
		//String dateSubmitted = map.get("dateSubmitted".toLowerCase());
		//if (dateSubmitted != null) dwcRecord.setDateSubmitted(dateSubmitted);

		String day = map.get("day");
		if (day != null) {
			try {
				Date date = getMapper().parseDay(day);
				dwcRecord.setDay(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("Day did not map to an integer: " + day, pe); // TODO: handle parse exception otherwise?
			}
		}

		String decimalLatitude = map.get("decimalLatitude".toLowerCase());
		if (decimalLatitude != null) {
			try {
				dwcRecord.setDecimalLatitude(Double.parseDouble(decimalLatitude));
			}
			catch (NumberFormatException nfe) {
				logger.warn("DecimalLatitude did not map to an integer: " + decimalLatitude); // TODO: handle parse exception otherwise?
			}
		}
		
		String decimalLongitude = map.get("decimalLongitude".toLowerCase());
		if (decimalLongitude != null) {
			try {
				dwcRecord.setDecimalLongitude(Double.parseDouble(decimalLongitude));
			}
			catch (NumberFormatException nfe) {
				logger.warn("DecimalLongitude did not map to an integer: " + decimalLongitude); // TODO: handle parse exception otherwise?
			}
		}
		
		//String description = map.get("description");
		//if (description != null) dwcRecord.setDescription(description);

		String disposition = map.get("disposition");
		if (disposition != null) dwcRecord.setDisposition(disposition);
		
		String dynamicProperties = map.get("dynamicProperties".toLowerCase());
		if (dynamicProperties != null) dwcRecord.setDynamicProperties(dynamicProperties);

		String earliestEonOrLowestEonothem = map.get("earliestEonOrLowestEonothem".toLowerCase());
		if (earliestEonOrLowestEonothem != null) dwcRecord.setEarliestEonOrLowestEonothem(earliestEonOrLowestEonothem);

		String earliestEpochOrLowestSeries = map.get("earliestEpochOrLowestSeries".toLowerCase());
		if (earliestEpochOrLowestSeries != null) dwcRecord.setEarliestEpochOrLowestSeries(earliestEpochOrLowestSeries);

		String earliestEraOrLowestErathem = map.get("earliestEraOrLowestErathem".toLowerCase());
		if (earliestEraOrLowestErathem != null) dwcRecord.setEarliestEraOrLowestErathem(earliestEraOrLowestErathem);

		String earliestPeriodOrLowestSystem = map.get("earliestPeriodOrLowestSystem".toLowerCase());
		if (earliestPeriodOrLowestSystem != null) dwcRecord.setEarliestPeriodOrLowestSystem(earliestPeriodOrLowestSystem);

		//String educationLevel = map.get("educationLevel".toLowerCase());
		//if (educationLevel != null) dwcRecord.setEducationLevel(educationLevel);

		String endDayOfYear = map.get("endDayOfYear".toLowerCase());
		if (endDayOfYear != null) {
			try {
				dwcRecord.setEndDayOfYear(Integer.parseInt(endDayOfYear));
			}
			catch (NumberFormatException nfe) {
				logger.warn("EndDayOfYear did not map to an integer: " + endDayOfYear); // TODO: handle parse exception otherwise?
			}
		}

		String establishmentMeans = map.get("establishmentMeans".toLowerCase());
		if (establishmentMeans != null) dwcRecord.setEstablishmentMeans(establishmentMeans);

		String eventDate = map.get("eventDate".toLowerCase());
		if (eventDate != null) {
			try {
				Date date = getMapper().parseDate(eventDate);
				dwcRecord.setEventDate(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("EventDate did not map to a date: " + eventDate, pe); // TODO: handle parse exception otherwise?
			}
		}
		
		String eventID = map.get("eventID".toLowerCase());
		if (eventID != null) dwcRecord.setEventID(eventID);

		String eventRemarks = map.get("eventRemarks".toLowerCase());
		if (eventRemarks != null) dwcRecord.setEventRemarks(eventRemarks);

		String eventTime = map.get("eventTime".toLowerCase());
		if (eventTime != null) {
			try {
				Date date = getMapper().parseTime(eventTime);
				dwcRecord.setEventTime(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("EventTime did not map to a time: " + eventTime, pe); // TODO: handle parse exception otherwise?
			}
		}

		String family = map.get("family");
		if (family != null) dwcRecord.setFamily(family);

		String fieldNotes = map.get("fieldNotes".toLowerCase());
		if (fieldNotes != null) dwcRecord.setFieldNotes(fieldNotes);

		String fieldNumber = map.get("fieldNumber".toLowerCase());
		if (fieldNumber != null) dwcRecord.setFieldNumber(fieldNumber);

		String footprintSRS = map.get("footprintSRS".toLowerCase());
		if (footprintSRS != null) dwcRecord.setFootprintSRS(footprintSRS);

		String footprintSpatialFit = map.get("footprintSpatialFit".toLowerCase());
		if (footprintSpatialFit != null) dwcRecord.setFootprintSpatialFit(footprintSpatialFit);

		String footprintWKT = map.get("footprintWKT".toLowerCase());
		if (footprintWKT != null) dwcRecord.setFootprintWKT(footprintWKT);

		//String format = map.get("format");
		//if (format != null) dwcRecord.setFormat(format);

		String genus = map.get("genus");
		if (genus != null) dwcRecord.setGenus(genus);

		String geodeticDatum = map.get("geodeticDatum".toLowerCase());
		if (geodeticDatum != null) dwcRecord.setGeodeticDatum(geodeticDatum);

		String geologicalContextID = map.get("geologicalContextID".toLowerCase());
		if (geologicalContextID != null) dwcRecord.setGeologicalContextID(geologicalContextID);

		String georeferenceProtocol = map.get("georeferenceProtocol".toLowerCase());
		if (georeferenceProtocol != null) dwcRecord.setGeoreferenceProtocol(georeferenceProtocol);

		String georeferenceRemarks = map.get("georeferenceRemarks".toLowerCase());
		if (georeferenceRemarks != null) dwcRecord.setGeoreferenceRemarks(georeferenceRemarks);

		String georeferenceSources = map.get("georeferenceSources".toLowerCase());
		if (georeferenceSources != null) dwcRecord.setGeoreferenceSources(georeferenceSources);

		String georeferenceVerificationStatus = map.get("georeferenceVerificationStatus".toLowerCase());
		if (georeferenceVerificationStatus != null) dwcRecord.setGeoreferenceVerificationStatus(georeferenceVerificationStatus);

		String georeferencedBy = map.get("georeferencedBy".toLowerCase());
		if (georeferencedBy != null) dwcRecord.setGeoreferencedBy(georeferencedBy);

		String group = map.get("group");
		if (group != null) dwcRecord.setGroup(group);

		String habitat = map.get("habitat");
		if (habitat != null) dwcRecord.setHabitat(habitat);

		//String hasFormat = map.get("hasFormat".toLowerCase());
		//if (hasFormat != null) dwcRecord.setHasFormat(hasFormat);

		String formation = map.get("formation");
		if (formation != null) dwcRecord.setFormation(formation);
		
		//String hasPart = map.get("hasPart".toLowerCase());
		//if (hasPart != null) dwcRecord.setHasPart(hasPart);

		//String hasVersion = map.get("hasVersion".toLowerCase());
		//if (hasVersion != null) dwcRecord.setHasVersion(hasVersion);

		String higherClassification = map.get("higherClassification".toLowerCase());
		if (higherClassification != null) dwcRecord.setHigherClassification(higherClassification);

		String higherGeography = map.get("higherGeography".toLowerCase());
		if (higherGeography != null) dwcRecord.setHigherGeography(higherGeography);

		String higherGeographyID = map.get("higherGeographyID".toLowerCase());
		if (higherGeographyID != null) dwcRecord.setHigherGeographyID(higherGeographyID);

		String highestBiostratigraphicZone = map.get("highestBiostratigraphicZone".toLowerCase());
		if (highestBiostratigraphicZone != null) dwcRecord.setHighestBiostratigraphicZone(highestBiostratigraphicZone);
		
		String identificationID = map.get("identificationID".toLowerCase());
		if (identificationID != null) dwcRecord.setIdentificationID(identificationID);
		
		String identificationQualifier = map.get("identificationQualifier".toLowerCase());
		if (identificationQualifier != null) dwcRecord.setIdentificationQualifier(identificationQualifier);
		
		String identificationReferences = map.get("identificationReferences".toLowerCase());
		if (identificationReferences != null) dwcRecord.setIdentificationReferences(identificationReferences);
		
		String identificationRemarks = map.get("identificationRemarks".toLowerCase());
		if (identificationRemarks != null) dwcRecord.setIdentificationRemarks(identificationRemarks);

		String identifiedBy = map.get("identifiedBy".toLowerCase());
		if (identifiedBy != null) dwcRecord.setIdentifiedBy(identifiedBy);

		//String identifier = map.get("identifier");
		//if (identifier != null) dwcRecord.setIdentifier(identifier);

		String individualCount = map.get("individualCount".toLowerCase());
		if (individualCount != null) {
			try {
				dwcRecord.setIndividualCount(BigInteger.valueOf(Integer.parseInt(individualCount)));
			}
			catch (NumberFormatException nfe) {
				logger.warn("IndividualCount did not map to an integer: " + individualCount); // TODO: handle parse exception otherwise?
			}
		}

		String individualID = map.get("individualID".toLowerCase());
		if (individualID != null) dwcRecord.setIndividualID(individualID);
		
		String informationWithheld = map.get("informationWithheld".toLowerCase());
		if (informationWithheld != null) dwcRecord.setInformationWithheld(informationWithheld);

		String infraspecificEpithet = map.get("infraspecificEpithet".toLowerCase());
		if (infraspecificEpithet != null) dwcRecord.setInfraspecificEpithet(infraspecificEpithet);

		String institutionCode = map.get("institutionCode".toLowerCase());
		if (institutionCode != null) dwcRecord.setInstitutionCode(institutionCode);

		String institutionID = map.get("institutionID".toLowerCase());
		if (institutionID != null) dwcRecord.setInstitutionID(institutionID);

		//String instructionalMethod = map.get("instructionalMethod".toLowerCase());
		//if (instructionalMethod != null) dwcRecord.setInstructionalMethod(instructionalMethod);

		//String isFormatOf = map.get("isFormatOf".toLowerCase());
		//if (isFormatOf != null) dwcRecord.setIsFormatOf(isFormatOf);

		//String isPartOf = map.get("isPartOf".toLowerCase());
		//if (isPartOf != null) dwcRecord.setIsPartOf(isPartOf);

		//String isReferencedBy = map.get("isReferencedBy".toLowerCase());
		//if (isReferencedBy != null) dwcRecord.setIsReferencedBy(isReferencedBy);

		//String isReplacedBy = map.get("isReplacedBy".toLowerCase());
		//if (isReplacedBy != null) dwcRecord.setIsReplacedBy(isReplacedBy);

		//String isRequiredBy = map.get("isRequiredBy".toLowerCase());
		//if (isRequiredBy != null) dwcRecord.setIsRequiredBy(isRequiredBy);

		//String isVersionOf = map.get("isVersionOf".toLowerCase());
		//if (isVersionOf != null) dwcRecord.setIsVersionOf(isVersionOf);

		String island = map.get("island");
		if (island != null) dwcRecord.setIsland(island);
		
		String islandGroup = map.get("islandGroup".toLowerCase());
		if (islandGroup != null) dwcRecord.setIslandGroup(islandGroup);

		//String issued = map.get("issued");
		//if (issued != null) dwcRecord.setIssued(issued);

		String kingdom = map.get("kingdom");
		if (kingdom != null) dwcRecord.setKingdom(kingdom);

		String language = map.get("language");
		if (language != null) dwcRecord.setLanguage(language);

		String latestAgeOrHighestStage = map.get("latestAgeOrHighestStage".toLowerCase());
		if (latestAgeOrHighestStage != null) dwcRecord.setLatestAgeOrHighestStage(latestAgeOrHighestStage);

		String latestEonOrHighestEonothem = map.get("latestEonOrHighestEonothem".toLowerCase());
		if (latestEonOrHighestEonothem != null) dwcRecord.setLatestEonOrHighestEonothem(latestEonOrHighestEonothem);

		String latestEpochOrHighestSeries = map.get("latestEpochOrHighestSeries".toLowerCase());
		if (latestEpochOrHighestSeries != null) dwcRecord.setLatestEpochOrHighestSeries(latestEpochOrHighestSeries);

		String latestEraOrHighestErathem = map.get("latestEraOrHighestErathem".toLowerCase());
		if (latestEraOrHighestErathem != null) dwcRecord.setLatestEraOrHighestErathem(latestEraOrHighestErathem);

		String latestPeriodOrHighestSystem = map.get("latestPeriodOrHighestSystem".toLowerCase());
		if (latestPeriodOrHighestSystem != null) dwcRecord.setLatestPeriodOrHighestSystem(latestPeriodOrHighestSystem);

		//String license = map.get("license");
		//if (license != null) dwcRecord.setLicense(license);

		String lifeStage = map.get("lifeStage".toLowerCase());
		if (lifeStage != null) dwcRecord.setLifeStage(lifeStage);

		String lithostratigraphicTerms = map.get("lithostratigraphicTerms".toLowerCase());
		if (lithostratigraphicTerms != null) dwcRecord.setLithostratigraphicTerms(lithostratigraphicTerms);

		String locality = map.get("locality");
		if (locality != null) dwcRecord.setLocality(locality);

		String locationID = map.get("locationID".toLowerCase());
		if (locationID != null) dwcRecord.setLocationID(locationID);
		
		String locationAccordingTo = map.get("locationAccordingTo".toLowerCase());
		if (locationAccordingTo != null) dwcRecord.setLocationAccordingTo(locationAccordingTo);
		
		String locationRemarks = map.get("locationRemarks".toLowerCase());
		if (locationRemarks != null) dwcRecord.setLocationRemarks(locationRemarks);

		String lowestBiostratigraphicZone = map.get("lowestBiostratigraphicZone".toLowerCase());
		if (lowestBiostratigraphicZone != null) dwcRecord.setLowestBiostratigraphicZone(lowestBiostratigraphicZone);

		String maximumDepthInMeters = map.get("maximumDepthInMeters".toLowerCase());
		if (maximumDepthInMeters != null) {
			try {
				dwcRecord.setMaximumDepthInMeters(Double.parseDouble(maximumDepthInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("MaximumDepthInMeters did not map to a double: " + maximumDepthInMeters); // TODO: handle parse exception otherwise?
			}
		}

		String maximumDistanceAboveSurfaceInMeters = map.get("maximumDistanceAboveSurfaceInMeters".toLowerCase());
		if (maximumDistanceAboveSurfaceInMeters != null) {
			try {
				dwcRecord.setMaximumDistanceAboveSurfaceInMeters(Double.parseDouble(maximumDistanceAboveSurfaceInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("MaximumDistanceAboveSurfaceInMeters did not map to a double: " + maximumDistanceAboveSurfaceInMeters); // TODO: handle parse exception otherwise?
			}
		}
		
		String maximumElevationInMeters = map.get("maximumElevationInMeters".toLowerCase());
		if (maximumElevationInMeters != null) {
			try {
				dwcRecord.setMaximumElevationInMeters(Double.parseDouble(maximumElevationInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("MaximumElevationInMeters did not map to a double: " + maximumElevationInMeters); // TODO: handle parse exception otherwise?
			}
		}
		
		//String mediator = map.get("mediator");
		//if (mediator != null) dwcRecord.setMediator(mediator);

		//String medium = map.get("medium");
		//if (medium != null) dwcRecord.setMedium(medium);

		String member = map.get("member");
		if (member != null) dwcRecord.setMember(member);

		String minimumDepthInMeters = map.get("minimumDepthInMeters".toLowerCase());
		if (minimumDepthInMeters != null) {
			try {
				dwcRecord.setMinimumDepthInMeters(Double.parseDouble(minimumDepthInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("MinimumDepthInMeters did not map to a double: " + minimumDepthInMeters); // TODO: handle parse exception otherwise?
			}
		}

		String minimumDistanceAboveSurfaceInMeters = map.get("minimumDistanceAboveSurfaceInMeters".toLowerCase());
		if (minimumDistanceAboveSurfaceInMeters != null) {
			try {
				dwcRecord.setMinimumDistanceAboveSurfaceInMeters(Double.parseDouble(minimumDistanceAboveSurfaceInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("MinimumDistanceAboveSurfaceInMeters did not map to a double: " + minimumDistanceAboveSurfaceInMeters); // TODO: handle parse exception otherwise?
			}
		}

		String minimumElevationInMeters = map.get("minimumElevationInMeters".toLowerCase());
		if (minimumElevationInMeters != null) {
			try {
				dwcRecord.setMinimumElevationInMeters(Double.parseDouble(minimumElevationInMeters));
			}
			catch (NumberFormatException nfe) {
				logger.warn("MinimumElevationInMeters did not map to a double: " + minimumElevationInMeters); // TODO: handle parse exception otherwise?
			}
		}

		String modified = map.get("modified");
		if (modified != null) {
			try {
				Date date = getMapper().parseTime(modified);
				dwcRecord.setModified(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("Modified did not map to a date: " + modified, pe); // TODO: handle parse exception otherwise?
			}
		}
		
		String month = map.get("month");
		if (month != null) {
			try {
				Date date = getMapper().parseMonth(month);
				dwcRecord.setMonth(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("Month did not map to a date: " + month, pe); // TODO: handle parse exception otherwise?
			}
		}

		String municipality = map.get("municipality");
		if (municipality != null) dwcRecord.setMunicipality(municipality);
		
		String nameAccordingTo = map.get("nameAccordingTo".toLowerCase());
		if (nameAccordingTo != null) dwcRecord.setNameAccordingTo(nameAccordingTo);

		String nameAccordingToID = map.get("nameAccordingToID".toLowerCase());
		if (nameAccordingToID != null) dwcRecord.setNameAccordingToID(nameAccordingToID);

		String namePublishedIn = map.get("namePublishedIn".toLowerCase());
		if (namePublishedIn != null) dwcRecord.setNamePublishedIn(namePublishedIn);

		String namePublishedInID = map.get("namePublishedInID".toLowerCase());
		if (namePublishedInID != null) dwcRecord.setNamePublishedInID(namePublishedInID);

		String nomenclaturalCode = map.get("nomenclaturalCode".toLowerCase());
		if (nomenclaturalCode != null) dwcRecord.setNomenclaturalCode(nomenclaturalCode);

		String nomenclaturalStatus = map.get("nomenclaturalStatus".toLowerCase());
		if (nomenclaturalStatus != null) dwcRecord.setNomenclaturalStatus(nomenclaturalStatus);
		
		String occurrenceDetails = map.get("occurrenceDetails".toLowerCase());
		if (occurrenceDetails != null) dwcRecord.setOccurrenceDetails(occurrenceDetails);

		String occurrenceID = map.get("occurrenceID".toLowerCase());
		if (occurrenceID != null) dwcRecord.setOccurrenceID(occurrenceID);

		String occurrenceRemarks = map.get("occurrenceRemarks".toLowerCase());
		if (occurrenceRemarks != null) dwcRecord.setOccurrenceRemarks(occurrenceRemarks);

		String occurrenceStatus = map.get("occurrenceStatus".toLowerCase());
		if (occurrenceStatus != null) dwcRecord.setOccurrenceStatus(occurrenceStatus);

		String order = map.get("order");
		if (order != null) dwcRecord.setOrder(order);

		String originalNameUsage = map.get("originalNameUsage".toLowerCase());
		if (originalNameUsage != null) dwcRecord.setOriginalNameUsage(originalNameUsage);

		String originalNameUsageID = map.get("originalNameUsageID".toLowerCase());
		if (originalNameUsageID != null) dwcRecord.setOriginalNameUsageID(originalNameUsageID);
		
		String otherCatalogNumbers = map.get("otherCatalogNumbers".toLowerCase());
		if (otherCatalogNumbers != null) dwcRecord.setOtherCatalogNumbers(otherCatalogNumbers);

		String ownerInstitutionCode = map.get("ownerInstitutionCode".toLowerCase());
		if (ownerInstitutionCode != null) dwcRecord.setOwnerInstitutionCode(ownerInstitutionCode);

		String parentNameUsage = map.get("parentNameUsage".toLowerCase());
		if (parentNameUsage != null) dwcRecord.setParentNameUsage(parentNameUsage);

		String parentNameUsageID = map.get("parentNameUsageID".toLowerCase());
		if (parentNameUsageID != null) dwcRecord.setParentNameUsageID(parentNameUsageID);

		String phylum = map.get("phylum");
		if (phylum != null) dwcRecord.setPhylum(phylum);

		String pointRadiusSpatialFit = map.get("pointRadiusSpatialFit".toLowerCase());
		if (pointRadiusSpatialFit != null) dwcRecord.setPointRadiusSpatialFit(pointRadiusSpatialFit);

		String preparations = map.get("preparations");
		if (preparations != null) dwcRecord.setPreparations(preparations);

		String previousIdentifications = map.get("previousIdentifications".toLowerCase());
		if (previousIdentifications != null) dwcRecord.setPreviousIdentifications(previousIdentifications);

		//String provenance = map.get("provenance");
		//if (provenance != null) dwcRecord.setProvenance(provenance);

		//String publisher = map.get("publisher");
		//if (publisher != null) dwcRecord.setPublisher(publisher);
		
		String recordNumber = map.get("recordNumber".toLowerCase());
		if (recordNumber != null) dwcRecord.setRecordNumber(recordNumber);

		String recordedBy = map.get("recordedBy".toLowerCase());
		if (recordedBy != null) dwcRecord.setRecordedBy(recordedBy);

		//String references = map.get("references");
		//if (references != null) dwcRecord.setReferences(references);

		//String relatedResourceID = map.get("relatedResourceID".toLowerCase());
		//if (relatedResourceID != null) dwcRecord.setRelatedResourceID(relatedResourceID);

		//String relation = map.get("relation");
		//if (relation != null) dwcRecord.setRelation(relation);

		//String relationshipAccordingTo = map.get("relationshipAccordingTo".toLowerCase());
		//if (relationshipAccordingTo != null) dwcRecord.setRelationshipAccordingTo(relationshipAccordingTo);

		//String relationshipEstablishedDate = map.get("relationshipEstablishedDate".toLowerCase());
		//if (relationshipEstablishedDate != null) dwcRecord.setRelationshipEstablishedDate(relationshipEstablishedDate);

		//String relationshipOfResource = map.get("relationshipOfResource".toLowerCase());
		//if (relationshipOfResource != null) dwcRecord.setRelationshipOfResource(relationshipOfResource);

		//String relationshipRemarks = map.get("relationshipRemarks".toLowerCase());
		//if (relationshipRemarks != null) dwcRecord.setRelationshipRemarks(relationshipRemarks);

		//String replaces = map.get("replaces");
		//if (replaces != null) dwcRecord.setReplaces(replaces);

		String reproductiveCondition = map.get("reproductiveCondition".toLowerCase());
		if (reproductiveCondition != null) dwcRecord.setReproductiveCondition(reproductiveCondition);

		//String requires = map.get("requires");
		//if (requires != null) dwcRecord.setRequires(requires);

		//String resourceID = map.get("resourceID".toLowerCase());
		//if (resourceID != null) dwcRecord.setResourceID(resourceID);

		//String resourceRelationshipID = map.get("resourceRelationshipID".toLowerCase());
		//if (resourceRelationshipID != null) dwcRecord.setResourceRelationshipID(resourceRelationshipID);

		String rights = map.get("rights");
		if (rights != null) dwcRecord.setRights(rights);

		String rightsHolder = map.get("rightsHolder".toLowerCase());
		if (rightsHolder != null) dwcRecord.setRightsHolder(rightsHolder);

		String samplingEffort = map.get("samplingEffort".toLowerCase());
		if (samplingEffort != null) dwcRecord.setSamplingEffort(samplingEffort);

		String samplingProtocol = map.get("samplingProtocol".toLowerCase());
		if (samplingProtocol != null) dwcRecord.setSamplingProtocol(samplingProtocol);

		String scientificName = map.get("scientificName".toLowerCase());
		if (scientificName != null) dwcRecord.setScientificName(scientificName);

		String scientificNameAuthorship = map.get("scientificNameAuthorship".toLowerCase());
		if (scientificNameAuthorship != null) dwcRecord.setScientificNameAuthorship(scientificNameAuthorship);

		String scientificNameID = map.get("scientificNameID".toLowerCase());
		if (scientificNameID != null) dwcRecord.setScientificNameID(scientificNameID);

		String sex = map.get("sex");
		if (sex != null) dwcRecord.setSex(sex);			

		//String source = map.get("source");
		//if (source != null) dwcRecord.setSource(source);

		//String spatial = map.get("spatial");
		//if (spatial != null) dwcRecord.setSpatial(spatial);
		
		String specificEpithet = map.get("specificEpithet".toLowerCase());
		if (specificEpithet != null) dwcRecord.setSpecificEpithet(specificEpithet);

		String startDayOfYear = map.get("startDayOfYear".toLowerCase());
		if (startDayOfYear != null) {
			try {
				dwcRecord.setStartDayOfYear(Integer.parseInt(startDayOfYear));
			}
			catch (NumberFormatException nfe) {
				logger.warn("StartDayOfYear did not map to an integer: " + startDayOfYear); // TODO: handle parse exception otherwise?
			}
		}

		String stateProvince = map.get("stateProvince".toLowerCase());
		if (stateProvince != null) dwcRecord.setStateProvince(stateProvince);

		String subgenus = map.get("subgenus");
		if (subgenus != null) dwcRecord.setSubgenus(subgenus);

		//String subject = map.get("subject");
		//if (subject != null) dwcRecord.setSubject("subject");
		
		//String tableOfContents = map.get("tableOfContents".toLowerCase());
		//if (tableOfContents != null) dwcRecord.setTableOfContents(tableOfContents);	
		
		String taxonConceptID = map.get("taxonConceptID".toLowerCase());
		if (taxonConceptID != null) dwcRecord.setTaxonConceptID(taxonConceptID);	
		
		String taxonID = map.get("taxonID".toLowerCase());
		if (taxonID != null) dwcRecord.setTaxonID(taxonID);

		String taxonomicstatus = map.get("taxonomicStatus".toLowerCase());
		if (taxonomicstatus != null) dwcRecord.setTaxonomicStatus(taxonomicstatus);	
		
		String taxonRank = map.get("taxonRank".toLowerCase());
		if (taxonRank != null) dwcRecord.setTaxonRank(taxonRank);

		String taxonRemarks = map.get("taxonRemarks".toLowerCase());
		if (taxonRemarks != null) dwcRecord.setTaxonRemarks(taxonRemarks);

		//String temporal = map.get("temporal");
		//if (temporal != null) dwcRecord.setTemporal(temporal);

		//String title = map.get("title");
		//if (title != null) dwcRecord.setTitle.setTitle(title);	
		
		String type = map.get("type");
		if (type != null) dwcRecord.setType(type);

		String typeStatus = map.get("typeStatus".toLowerCase());
		if (typeStatus != null) dwcRecord.setTypeStatus(typeStatus);

		//String valid = map.get("valid");
		//if (valid != null) dwcRecord.setValid(valid);

		String verbatimCoordinateSystem = map.get("verbatimCoordinateSystem".toLowerCase());
		if (verbatimCoordinateSystem != null) dwcRecord.setVerbatimCoordinateSystem(verbatimCoordinateSystem);

		String verbatimCoordinates = map.get("verbatimCoordinates".toLowerCase());
		if (verbatimCoordinates != null) dwcRecord.setVerbatimCoordinates(verbatimCoordinates);

		String verbatimDepth = map.get("verbatimDepth".toLowerCase());
		if (verbatimDepth != null) dwcRecord.setVerbatimDepth(verbatimDepth);

		String verbatimElevation = map.get("verbatimElevation".toLowerCase());
		if (verbatimElevation != null) dwcRecord.setVerbatimElevation(verbatimElevation);

		String verbatimEventDate = map.get("verbatimEventDate".toLowerCase());
		if (verbatimEventDate != null) dwcRecord.setVerbatimEventDate(verbatimEventDate);

		String verbatimLatitude = map.get("verbatimLatitude".toLowerCase());
		if (verbatimLatitude != null) dwcRecord.setVerbatimLatitude(verbatimLatitude);

		String verbatimLocality = map.get("verbatimLocality".toLowerCase());
		if (verbatimLocality != null) dwcRecord.setVerbatimLocality(verbatimLocality);	
		
		String verbatimLongitude = map.get("verbatimLongitude".toLowerCase());
		if (verbatimLongitude != null) dwcRecord.setVerbatimLongitude(verbatimLongitude);

		String verbatimSRS = map.get("verbatimSRS".toLowerCase());
		if (verbatimSRS != null) dwcRecord.setVerbatimSRS(verbatimSRS);

		String verbatimTaxonRank = map.get("verbatimTaxonRank".toLowerCase());
		if (verbatimTaxonRank != null) dwcRecord.setVerbatimTaxonRank(verbatimTaxonRank);

		String vernacularName = map.get("vernacularName".toLowerCase());
		if (vernacularName != null) dwcRecord.setVernacularName(vernacularName);

		String waterbody = map.get("waterbody");
		if (waterbody != null) dwcRecord.setWaterbody(waterbody);

		String year = map.get("year");
		if (year != null) {
			try {
				Date date = getMapper().parseYear(year);
				dwcRecord.setYear(getXmlGregorianCalendar(date));
			}
			catch (ParseException pe) {
				logger.warn("Month did not map to a date: " + month, pe); // TODO: handle parse exception otherwise?
			}
		}

		return dwcRecord;
	}

	// TODO: this should go in DateUtils
	private XMLGregorianCalendar getXmlGregorianCalendar(Date date) {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		return getDatatypeFactory().newXMLGregorianCalendar(c);
	}

	// this method is based entirely on the superclass's version.  --mmk
	@Override
	public String crosswalkToString(Object nativeObject) {
		SimpleDarwinRecord occurrence = crosswalk(nativeObject);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance("dwc.huh_harvard_edu.tdwg_dwc_simple");
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter writer = new StringWriter();
			marshaller.marshal(occurrence, writer);
			
			return "<metadata>" + writer.toString() + "</metadata>";
		} 
		catch (JAXBException je) {
			
			return null;			
		}
	}

	// this method is based entirely on the superclass's version.  --mmk
	@Override
	public String getDatestamp(Object nativeItem) {

		return DateUtils.formatDate(((CollectionObject) nativeItem).getTimestampModified(), false);
	}

	protected SpecifyMapper getMapper() {
		if (mapper == null) {
			mapper = new SpecifyMapper();			
		}
		return mapper;
	}
	
	public void setMapper(SpecifyMapper mapper) {
		this.mapper = mapper;
	}

	protected DatatypeFactory getDatatypeFactory() {
		if (datatypeFactory == null) {
			try {
				datatypeFactory = DatatypeFactory.newInstance();
			}
			catch (DatatypeConfigurationException e) {
				logger.error("Could not instantiate DatatypeFactory", e);
			}
		}
		return datatypeFactory;
	}
}
