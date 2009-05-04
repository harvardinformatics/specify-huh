package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Series;
import edu.harvard.huh.asa.SpecimenItem;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ExsiccataItem;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;

public class SpecimenItemLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(SpecimenItemLoader.class);
	private Discipline discipline;

	// initialize collection code to id hashtable
	private Hashtable<String, Collection> collectionsByCode = new Hashtable<String, Collection>();

	// initialize prep type hashtable
	private Hashtable<String, PrepType> prepTypesByName = new Hashtable<String, PrepType>();
	
	// initialize cataloger hashtable
	private Hashtable<String, Agent> catalogersByName = new Hashtable<String, Agent>();

	public SpecimenItemLoader(File csvFile, Statement sqlStatement, Discipline discipline) {
		super(csvFile, sqlStatement);
		
		this.discipline = discipline;
		
		// TODO: initialize catalogersByName with botanist-optrs; write OptrLoader
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		String sql;
		
		SpecimenItem specimenItem = parseSpecimenItemRecord(columns);
		
		// find the matching collection
		String herbariumCode = specimenItem.getHerbariumCode();
		Collection collection = collectionsByCode.get(herbariumCode);
		if (collection == null) {
			sql = SqlUtils.getQueryIdByFieldSql("collection", "CollectionID", "Code", herbariumCode);
			Integer collectionId = queryForId(sql);

			if (collectionId == null) {
				throw new LocalException("Didn't find collection for code " + herbariumCode);
			}

			collection = new Collection();
			collection.setCollectionId(collectionId);
			collectionsByCode.put(herbariumCode, collection);
		}

		// find the matching accession record
		Accession accession = new Accession();
		String accessionNumber = specimenItem.getAccessionNo();

		if (accessionNumber != null) {
			sql = SqlUtils.getQueryIdByFieldSql("accession", "AccessionID", "AccessionNumber", accessionNumber);

			Integer accessionId = queryForId(sql);

			if (accessionId != null)
			{
				accession.setAccessionId(accessionId);
			}
		}
		
		// find the matching series record
		Exsiccata exsiccata = new Exsiccata();
		Integer seriesId = specimenItem.getSeriesId();
		
		if (seriesId != null) {
			Series series =  new Series();
			
			String guid = SqlUtils.sqlString(series.getGuid());
			
			String subselect =  "(" + SqlUtils.getQueryIdByFieldSql("referencework", "ReferenceWorkID", "GUID", guid) + ")";
			
			sql = SqlUtils.getQueryIdByFieldSql("exsiccata", "ExsiccataId", "ReferenceWorkID", subselect);
			
			Integer exsiccataId = queryForId(sql);
			
			if (exsiccataId == null)
			{
				throw new LocalException("Couldn't find exsiccata id for " + guid);
			}
			
			exsiccata.setExsiccataId(exsiccataId);
		}
		
		// find the matching locality record
		Locality locality = new Locality();
		Integer siteId = specimenItem.getSiteId();

		if (siteId != null)
		{
			String guid = SqlUtils.sqlString(String.valueOf(siteId));

			sql = SqlUtils.getQueryIdByFieldSql("locality", "LocalityID", "GUID", guid);

			Integer localityId = queryForId(sql);
			locality.setLocalityId(localityId);
		}
		
		// find the matching collector
		Agent agent = new Agent();
		Integer botanistId = specimenItem.getBotanistId();
		
		if (botanistId != null)
		{
			Botanist botanist = new Botanist();
			String guid = SqlUtils.sqlString(botanist.getGuid());
			
			sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);
			
			Integer agentId = queryForId(sql);
			
			if (agentId == null)
			{
				throw new LocalException("Couldn't find agent with guid " + guid);
			}
			agent.setAgentId(agentId);
		}
		
		// find the matching cataloger
		String createdBy = specimenItem.getCreatedBy();
		Agent cataloger = catalogersByName.get(createdBy);
		if (cataloger == null)
		{
			sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", createdBy);
			Integer catalogerId = queryForId(sql);

			if (catalogerId == null) {
				throw new LocalException("Didn't find cataloger for name " + createdBy);
			}

			cataloger = new Agent();
			cataloger.setAgentId(catalogerId);
			catalogersByName.put(createdBy, cataloger);
		}

		
		// find the matching prep type
		String format = specimenItem.getFormat();
		PrepType prepType = prepTypesByName.get(format);
		if (prepType == null) {
			sql = SqlUtils.getQueryIdByFieldSql("preptype", "PrepTypeID", "Name", format);
			Integer prepTypeId = queryForId(sql);

			if (prepTypeId == null)
			{
				throw new LocalException("Didn't find prep type for name " + format);
			}

			prepType = new PrepType();
			prepType.setPrepTypeId(prepTypeId);
			prepTypesByName.put(format, prepType);
		}

		// convert SpecimenItem into Preparation
		Preparation preparation = convert(specimenItem);

		// from which we get the CollectionObject
		CollectionObject collectionObject = preparation.getCollectionObject();
		
		// from which we get the CollectingEvent
		CollectingEvent collectingEvent = collectionObject.getCollectingEvent();
		collectingEvent.setDiscipline(discipline);
		collectingEvent.setLocality(locality);

		// insert CollectingEvent
		sql = getInsertSql(collectingEvent);
		Integer collectingEventId = insert(sql);
		collectingEvent.setCollectingEventId(collectingEventId);
		
		// insert Collector
		Collector collector = new Collector();
		collector.setAgent(agent);
		collector.setCollectingEvent(collectingEvent);
		collector.setCollectionMemberId(collection.getCollectionId());
		collector.setIsPrimary(true);
		collector.setOrderNumber(1);
		
		sql = getInsertSql(collector);
		
		// insert CollectionObject
		collectionObject.setCollectingEvent(collectingEvent);
		collectionObject.setCollection(collection);
		collectionObject.setCollectionMemberId(collection.getCollectionId());
		collectionObject.setAccession(accession);
		collectionObject.setCataloger(cataloger);
		
		sql = getInsertSql(collectionObject);
		Integer collectionObjectId = insert(sql);
		collectionObject.setCollectionObjectId(collectionObjectId);
		
		// insert Preparation
		preparation.setCollectionMemberId(collection.getCollectionId());
		preparation.setPrepType(prepType);
		
		sql = getInsertSql(preparation);
		insert(sql);
		
		// and possibly into an ExsiccataItem
		if (exsiccata.getExsiccataId() != null)
		{
			ExsiccataItem exsiccataItem = new ExsiccataItem();
			exsiccataItem.setCollectionObject(collectionObject);
			exsiccataItem.setExsiccata(exsiccata);
			
			sql = getInsertSql(exsiccataItem);
			insert(sql);
		}
	}

	public SpecimenItem parseSpecimenItemRecord(String[] columns) throws LocalException
	{
		if (columns.length < 35) //TODO: implement
		{
			throw new LocalException("Wrong number of columns");
		}

		SpecimenItem specimenItem = new SpecimenItem();

		return specimenItem;
	}

	public Preparation convert(SpecimenItem specimenItem) {

		CollectionObject collectionObject = new CollectionObject();

		collectionObject.setGuid(String.valueOf(specimenItem.getId()));

		Integer barcode = specimenItem.getBarcode();
		String catalogNumber = (new DecimalFormat( "000000000" ) ).format( String.valueOf(barcode) );
		collectionObject.setCatalogNumber(catalogNumber);

		String collectorNo = specimenItem.getCollectorNo();
		if (collectorNo != null && collectorNo.length() > 50) {
			log.warn("truncating collector number");
			collectorNo = collectorNo.substring(0, 50);
		}
		collectionObject.setFieldNumber(collectorNo);

		// TODO: countAmt: do we need to preserve this?  I'm not importing it at the moment.

		collectionObject.setCatalogedDate( specimenItem.getCatalogedDate() );
		collectionObject.setCatalogedDatePrecision( (byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal() );

		collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement

		StringBuilder sb = new StringBuilder();

		String description = specimenItem.getDescription();
		if (description != null && description.length() > 255)
		{
			log.warn("Truncating description");
			description = description.substring(0, 255);
		}
		collectionObject.setDescription(description);

		String habitat = specimenItem.getHabitat();
		collectionObject.setText1(habitat);

		String substrate = specimenItem.getSubstrate();
		if (substrate != null) {
			sb.append("SUBSTRATE: ");
			sb.append(substrate);
		}

		String reproStatus = specimenItem.getReproStatus();
		if (substrate != null) {
			if (sb.length() > 0) sb.append("; ");
			sb.append("REPRO. STATUS: ");
			sb.append(reproStatus);
		}

		String sex = specimenItem.getSex();
		if (sex != null) {
			if (sb.length() > 0) sb.append("; ");
			sb.append("SEX: ");
			sb.append(sex);
		}

		if (sb.length() > 0) {
			collectionObject.setText2(sb.toString());
		}

		String remarks = specimenItem.getRemarks();
		collectionObject.setRemarks(remarks);
		
		CollectingEvent collectingEvent = new CollectingEvent();
		BDate bdate = specimenItem.getCollDate();

		Integer startYear = bdate.getStartYear();
		Integer startMonth = bdate.getStartMonth();
		Integer startDay = bdate.getStartDay();

		if ( DateUtils.isValidSpecifyDate( startYear, startMonth, startDay ) ) {
			collectingEvent.setStartDate( DateUtils.getSpecifyStartDate( bdate ) );
			collectingEvent.setStartDatePrecision( DateUtils.getDatePrecision( startYear, startMonth, startDay ) );
		}
		else if ( DateUtils.isValidCollectionDate( startYear, startMonth, startDay ) ) {
			String startDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
			if (startDateVerbatim != null && startDateVerbatim.length() > 50) {
				log.warn("truncating start date verbatim");
				startDateVerbatim = startDateVerbatim.substring(0, 50);
			}
			collectingEvent.setStartDateVerbatim(startDateVerbatim);
		}
		else {
			log.warn( "Invalid start date" );
		}

		// StartDatePrecision
		// TODO: find out what the options are

		Integer endYear = bdate.getEndYear();
		Integer endMonth = bdate.getEndMonth();
		Integer endDay = bdate.getEndDay();

		if ( DateUtils.isValidSpecifyDate( endYear, endMonth, endDay ) ) {
			collectingEvent.setEndDate( DateUtils.getSpecifyEndDate( bdate ) );
			collectingEvent.setEndDatePrecision( DateUtils.getDatePrecision( endYear, endMonth, endDay ) );
		}
		else if ( DateUtils.isValidCollectionDate( endYear, endMonth, endDay ) ) {
			String endDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
			if (endDateVerbatim != null && endDateVerbatim.length() > 50) {
				log.warn("truncating end date verbatim");
				endDateVerbatim = endDateVerbatim.substring(0, 50);
			}
			collectingEvent.setStartDateVerbatim(endDateVerbatim);
		}

		collectionObject.setCollectingEvent(collectingEvent);

		Preparation preparation = new Preparation();

		Integer itemNo = specimenItem.getItemNo();
		preparation.setNumber1((float) itemNo);

		Boolean isOversize = specimenItem.isOversize();
		preparation.setYesNo1(isOversize);

		String voucher = specimenItem.getVoucher(); // TODO: investigate
		preparation.setText1(voucher);

		String reference = specimenItem.getReference(); // TODO: investigate
		preparation.setText2(reference);

		String note = specimenItem.getNote();
		preparation.setRemarks(note);

		preparation.setCollectionObject(collectionObject);

		return preparation;
	}

	public String getInsertSql(CollectionObject collectionObject) throws LocalException
	{
		String fieldNames = "AccessionID, CollectionID, CollectionMemberID, CatalogerID, CatalogNumber, " +
							"CatalogedDate, CatalogedDatePrecision, CollectingEventID, Description, " +
							"FieldNumber, GUID, Text1, Text2, YesNo1, Remarks, TimestampCreated";

		List<String> values = new ArrayList<String>(16);
		
		values.add(    String.valueOf(collectionObject.getAccession().getAccessionId()            ));
		values.add(    String.valueOf(collectionObject.getCollection().getCollectionId()          ));
		values.add(    String.valueOf(collectionObject.getCollectionMemberId()                    ));
		values.add(    String.valueOf(collectionObject.getCataloger().getAgentId()                ));
		values.add(SqlUtils.sqlString(collectionObject.getCatalogNumber()                         ));
		values.add(SqlUtils.sqlString(collectionObject.getCatalogedDate()                         ));
		values.add(    String.valueOf(collectionObject.getCatalogedDatePrecision()                ));
		values.add(    String.valueOf(collectionObject.getCollectingEvent().getCollectingEventId()));
		values.add(SqlUtils.sqlString(collectionObject.getDescription()                           ));
		values.add(SqlUtils.sqlString(collectionObject.getFieldNumber()                           ));
		values.add(SqlUtils.sqlString(collectionObject.getGuid()                                  ));
		values.add(SqlUtils.sqlString(collectionObject.getText1()                                 ));
		values.add(SqlUtils.sqlString(collectionObject.getText2()                                 ));
		values.add(    String.valueOf(collectionObject.getYesNo1()                                ));
		values.add(SqlUtils.sqlString(collectionObject.getRemarks()                               ));
		values.add("now()");

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}

	public String getInsertSql(CollectingEvent collectingEvent) throws LocalException
	{
		String fieldNames = "EndDate, EndDatePrecision, EndDateVerbatim, StartDate, StartDatePrecision, " +
				            "StartDateVerbatim, VerbatimDate, DisciplineID, LocalityID, TimestampCreated";

		List<String> values = new ArrayList<String>(10);
		
		values.add(SqlUtils.sqlString(collectingEvent.getEndDate()                     ));
		values.add(    String.valueOf(collectingEvent.getEndDatePrecision()            ));
		values.add(SqlUtils.sqlString(collectingEvent.getEndDateVerbatim()             ));
		values.add(SqlUtils.sqlString(collectingEvent.getStartDate()                   ));
		values.add(    String.valueOf(collectingEvent.getStartDatePrecision()          ));
		values.add(SqlUtils.sqlString(collectingEvent.getStartDateVerbatim()           ));
		values.add(SqlUtils.sqlString(collectingEvent.getVerbatimDate()                ));
		values.add(    String.valueOf(collectingEvent.getDiscipline().getDisciplineId()));
		values.add(    String.valueOf(collectingEvent.getLocality().getLocalityId()    ));
		values.add("now()");

		return SqlUtils.getInsertSql("collectingevent", fieldNames, values);
	}

	public String getInsertSql(Collector collector)
	{
		String fieldNames = "CollectionMemberID, IsPrimary, OrderNumber, AgentID, CollectingEventID, TimestampCreated";
		
		List<String> values = new ArrayList<String>(6);
		
		values.add(String.valueOf(collector.getCollectionMemberId()                    ));
		values.add(String.valueOf(collector.getIsPrimary()                             ));
		values.add(String.valueOf(collector.getOrderNumber()                           ));
		values.add(String.valueOf(collector.getAgent().getAgentId()                    ));
		values.add(String.valueOf(collector.getCollectingEvent().getCollectingEventId()));
		values.add("now()");
		
		return SqlUtils.getInsertSql("collector", fieldNames, values);
	}
	
	public String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, PrepTypeID, " +
				            "Number1, YesNo1, Text1, Text2, TimestampCreated, Remarks";

		List<String> values = new ArrayList<String>(9);
		
		values.add(    String.valueOf(preparation.getCollectionMemberId()                      ));
		values.add(    String.valueOf(preparation.getCollectionObject().getCollectionObjectId()));
		values.add(    String.valueOf(preparation.getPrepType().getPrepTypeId()                ));
		values.add(    String.valueOf(preparation.getNumber1()                                 ));
		values.add(    String.valueOf(preparation.getYesNo1()                                  ));
		values.add(SqlUtils.sqlString(preparation.getText1()                                   ));
		values.add(SqlUtils.sqlString(preparation.getText2()                                   ));
		values.add("now()");
		values.add(SqlUtils.sqlString(preparation.getRemarks()                                 ));

		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}

	public String getInsertSql(ExsiccataItem exsiccataItem) throws LocalException
	{
		String fieldNames = "Fascicle, Number, ExsiccataID, CollectionObjectID, TimestampCreated";

		List<String> values = new ArrayList<String>(5);

		values.add(SqlUtils.sqlString(exsiccataItem.getFascicle()                ));
		values.add(SqlUtils.sqlString(exsiccataItem.getNumber()                  ));
		values.add(    String.valueOf(exsiccataItem.getExsiccata().getId()       ));
		values.add(    String.valueOf(exsiccataItem.getCollectionObject().getId()));
		values.add("now");

		return SqlUtils.getInsertSql("exsiccataitem", fieldNames, values);
	}

}
