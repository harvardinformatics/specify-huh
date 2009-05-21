package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Series;
import edu.harvard.huh.asa.SpecimenItem;
import edu.harvard.huh.asa.Subcollection;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Container;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ExsiccataItem;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.OtherIdentifier;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;

public class SpecimenItemLoader extends CsvToSqlLoader
{
	private Discipline discipline;
	private Division   division;
	
	private Integer lastSpecimenId;
	private HashMap<String, CollectionObject> collObjByAccessionIdentifier;
	
	public SpecimenItemLoader(File csvFile, Statement sqlStatement) throws LocalException 
	{
		super(csvFile, sqlStatement);
		
		this.discipline = getBotanyDiscipline();
		this.division   = getBotanyDivision();
		
		this.collObjByAccessionIdentifier = new HashMap<String, CollectionObject>();
	}

	private boolean isEmpty(Accession accession)
	{
	    return accession.getAccessionNumber() == null;
	}

	// TODO: implement proper sharing of series/subcollection, container/exsiccata
	private void processSubcollection(Integer subcollectionId, CollectionObject collectionObject) throws LocalException
	{
	    if (subcollectionId != null)
        {            
            String sql = SqlUtils.getQueryIdByFieldSql("container", "ContainerID", "Number", String.valueOf(subcollectionId));
                        
            Integer exsiccataId = queryForId(sql);
            
            if (exsiccataId == null)
            {
                throw new LocalException("Couldn't find exsiccata id for ");
            }
            
            Exsiccata exsiccata = new Exsiccata();
            exsiccata.setExsiccataId(exsiccataId);
            
            ExsiccataItem exsiccataItem = new ExsiccataItem();
            
            exsiccataItem.setExsiccata(exsiccata);
            exsiccataItem.setCollectionObject(collectionObject);
            
            sql = getInsertSql(exsiccataItem);
            insert(sql);
        }
	}

	private void processAccession(Accession accession) throws LocalException
	{
        if (! isEmpty(accession))
        {
            // insert Accession
            accession.setDivision(division);
            String sql = getInsertSql(accession);
            
            // update accession with id
            Integer accessionId = insert(sql);
            accession.setAccessionId(accessionId);
        }
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		String sql;
		
		SpecimenItem specimenItem = parse(columns);
		
	    // find the matching prep type
        String format = specimenItem.getFormat();
        PrepType prepType = getPrepType(format);
        
	    // convert SpecimenItem into Preparation
        Preparation preparation = convert(specimenItem);

        // from which we get the CollectionObject
        CollectionObject collectionObject = preparation.getCollectionObject();
        
        // if this preparation shares the same collection object with the previously
        // inserted one, re-use it
        Integer specimenId = specimenItem.getSpecimenId();
        Accession accession = collectionObject.getAccession();
        String accessionIdentifier = accession.getAccessionNumber() + accession.getRemarks();

        if (specimenId.equals(lastSpecimenId))
        {
            // if the two specimen items don't have different accession numbers or provenance
            // then they share a collection object; just create a different preparation
            if (collObjByAccessionIdentifier.containsKey(accessionIdentifier))
            {
                preparation.setCollectionObject(collObjByAccessionIdentifier.get(accessionIdentifier));

                // insert Preparation
                preparation.setCollectionMemberId(preparation.getCollectionObject().getCollectionMemberId());
                preparation.setPrepType(prepType);

                sql = getInsertSql(preparation);
                insert(sql);

                return;
            }
            else
            {
                collObjByAccessionIdentifier.put(accessionIdentifier, collectionObject);
                
                // different collection object but same collecting event, collection, cataloger, creator, exsiccata, container
                CollectionObject previousCollectionObject =
                    collObjByAccessionIdentifier.get(collObjByAccessionIdentifier.values().iterator().next());

                collectionObject.setCollectingEvent(previousCollectionObject.getCollectingEvent());
                collectionObject.setCollection(previousCollectionObject.getCollection());
                collectionObject.setCollectionMemberId(previousCollectionObject.getCollectionMemberId());
                collectionObject.setCataloger(previousCollectionObject.getCataloger());
                collectionObject.setCreatedByAgent(previousCollectionObject.getCreatedByAgent());
                collectionObject.setContainer(previousCollectionObject.getContainer());

                // maybe insert a new accession
                processAccession(accession);

                // insert the new collectionobject
                sql = getInsertSql(collectionObject);
                Integer collectionObjectId = insert(sql);
                collectionObject.setCollectionObjectId(collectionObjectId);
                
                // insert the preparation
                preparation.setCollectionMemberId(previousCollectionObject.getCollectionMemberId());
                preparation.setPrepType(prepType);
                
                sql = getInsertSql(preparation);
                insert(sql);

                // maybe insert a new exsiccata item TODO: untangle series/subcollection
                processSubcollection(specimenItem.getSubcollectionId(), collectionObject);
                
                return;
            }
        }

        // if we made it here, the asa.specimen.ids are different, so we have a different collection object entirely

        lastSpecimenId = specimenId;
        collObjByAccessionIdentifier.clear();
        collObjByAccessionIdentifier.put(accessionIdentifier, collectionObject);
        
        // find the matching collection
		String herbariumCode = specimenItem.getHerbariumCode();
		Collection collection = this.getCollection(herbariumCode);
		
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
		Integer botanistId = specimenItem.getCollectorId();
		
		if (botanistId != null)
		{
			Botanist botanist = new Botanist();
			botanist.setId(botanistId);
			String guid = botanist.getGuid();
			
			sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);
			
			Integer agentId = queryForId(sql);
			agent.setAgentId(agentId);
		}
		
		// find a matching container
		Container container = new Container();
		Integer subcollectionId = specimenItem.getSubcollectionId();
		if (subcollectionId != null)
		{            
		    sql = SqlUtils.getQueryIdByFieldSql("container", "ContainerID", "Number", String.valueOf(subcollectionId));

		    Integer containerId = queryForId(sql);
	        container.setContainerId(containerId);
		}
		        
		// find the matching cataloger
		Integer createdById = specimenItem.getCatalogedById();
		Agent cataloger = getAgentByOptrId(createdById);
		
	    // maybe insert an accession
		processAccession(accession);
            
		// from CollectionObject we get the CollectingEvent
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
		insert(sql);

		// insert CollectionObject
		collectionObject.setCollectingEvent(collectingEvent);
		collectionObject.setCollection(collection);
		collectionObject.setCollectionMemberId(collection.getCollectionId());
		collectionObject.setAccession(accession);
		collectionObject.setCataloger(cataloger);
		collectionObject.setCreatedByAgent(cataloger);
		collectionObject.setContainer(container);

		sql = getInsertSql(collectionObject);
		Integer collectionObjectId = insert(sql);
		collectionObject.setCollectionObjectId(collectionObjectId);
		
		// insert Preparation
		preparation.setCollectionMemberId(collection.getCollectionId());
		preparation.setPrepType(prepType);
		
		sql = getInsertSql(preparation);
		insert(sql);
		
		// maybe insert a new exsiccata item TODO: untangle series/subcollection
		//processSeries(specimenItem.getSeriesId(), specimenItem.getSeriesNo(), collectionObject);
		
	    // TODO: connect series (organization) agent as collector?
        // find a matching series (series was converted to agent w/ type org & guid "id organization"
        OtherIdentifier otherId = new OtherIdentifier();

        Integer seriesId = specimenItem.getSeriesId();
        String seriesNo = specimenItem.getSeriesNo();
        String seriesAbbrev = specimenItem.getSeriesAbbrev();

        if (seriesId != null && seriesNo != null)
        {
            Organization organization = new Organization();
            organization.setId(seriesId);
            String guid = organization.getGuid();

            sql = SqlUtils.getQueryIdByFieldSql("agent", "LastName", "GUID", guid);

            String institution = queryForString(sql);
            
            otherId.setCollectionMemberId(collection.getId());
            otherId.setInstitution(institution);
            otherId.setIdentifier( (seriesAbbrev == null ? "" : seriesAbbrev + " ") + seriesNo );
            otherId.setRemarks("Created by SpecimenItemLoader from series id " + seriesId);
            
            sql = getInsertSql(otherId);
            insert(sql);
        }
        else if (seriesNo != null)
        {
            warn("Series number and no series id", specimenItem.getId(), seriesNo);
        }
        else if (seriesId != null)
        {
            warn("Series id and no series number", specimenItem.getId(), String.valueOf(seriesId));
        }
	}

	private SpecimenItem parse(String[] columns) throws LocalException
	{
		if (columns.length < 39)
		{
			throw new LocalException("Wrong number of columns");
		}

		SpecimenItem specimenItem = new SpecimenItem();

		try {
		    specimenItem.setId(         Integer.parseInt( StringUtils.trimToNull( columns[0] )));
		    specimenItem.setSpecimenId( Integer.parseInt( StringUtils.trimToNull( columns[1] )));
		    
		    String barcodeStr = StringUtils.trimToNull( columns[2] );
		    if (barcodeStr != null)
		    {
		        specimenItem.setBarcode( Integer.parseInt( barcodeStr ));
		    }
		    
		    specimenItem.setCollectorNo( StringUtils.trimToNull( columns[3] ));
		    
            String createDateString = StringUtils.trimToNull( columns[4] );
            Date createDate = SqlUtils.parseDate(createDateString);
            specimenItem.setCatalogedDate(createDate);
            
            String isCultivated = StringUtils.trimToNull( columns[5] );
            specimenItem.setCultivated( isCultivated != null && isCultivated.equals("true") ? true : false);
            
            specimenItem.setDescription(     StringUtils.trimToNull( columns[6]  ));
            specimenItem.setHabitat(         StringUtils.trimToNull( columns[7]  ));
            specimenItem.setSubstrate(       StringUtils.trimToNull( columns[8]  ));
            specimenItem.setReproStatus(     StringUtils.trimToNull( columns[9]  ));
            specimenItem.setSex(             StringUtils.trimToNull( columns[10] ));
            specimenItem.setRemarks(         StringUtils.trimToNull( columns[11] ));
            specimenItem.setAccessionNo(     StringUtils.trimToNull( columns[12] ));
            specimenItem.setProvenance(      StringUtils.trimToNull( columns[13] ));
            specimenItem.setAccessionStatus( StringUtils.trimToNull( columns[14] ));
            
            BDate bdate = new BDate();
            specimenItem.setCollDate( bdate );

            String startYearStr = StringUtils.trimToNull( columns[15] );
            if (startYearStr != null)
            {
                bdate.setStartYear( Integer.parseInt( startYearStr ));
            }

            String startMonthStr = StringUtils.trimToNull( columns[16] );
            if (startMonthStr != null)
            {
                bdate.setStartMonth( Integer.parseInt( startMonthStr ));
            }
            
            String startDayStr = StringUtils.trimToNull( columns[17] );
            if (startDayStr != null)
            {
                bdate.setStartDay( Integer.parseInt( startDayStr ));
            }
            
            bdate.setStartPrecision( StringUtils.trimToNull( columns[18] ));
            
            String endYearStr = StringUtils.trimToNull( columns[19] );
            if (endYearStr != null)
            {
                bdate.setEndYear( Integer.parseInt( endYearStr ));
            }

            String endMonthStr = StringUtils.trimToNull( columns[20] );
            if (endMonthStr != null)
            {
                bdate.setEndMonth( Integer.parseInt( endMonthStr ));
            }
            
            String endDayStr =            StringUtils.trimToNull( columns[21] );
            if (endDayStr != null)
            {
                bdate.setEndDay( Integer.parseInt( endDayStr ));
            }

            bdate.setEndPrecision( StringUtils.trimToNull( columns[22] ));

            bdate.setText( StringUtils.trimToNull( columns[23] ));  
            
            String itemNoStr = StringUtils.trimToNull( columns[24] );
            if (itemNoStr != null)
            {
                specimenItem.setItemNo( Integer.parseInt( itemNoStr ));
            }
            
            String isOversize = StringUtils.trimToNull( columns[25] );
            specimenItem.setOversize( isOversize != null && isOversize.equals("true") ? true : false);
            
            specimenItem.setVoucher(                         StringUtils.trimToNull( columns[26] ));
            specimenItem.setReference(                       StringUtils.trimToNull( columns[27] ));
            specimenItem.setNote(                            StringUtils.trimToNull( columns[28] ));
            specimenItem.setHerbariumCode(                   StringUtils.trimToNull( columns[29] ));
            
            String seriesIdStr = StringUtils.trimToNull( columns[30] );
            if (seriesIdStr != null)
            {
                specimenItem.setSeriesId( Integer.parseInt( seriesIdStr ));
            }
            
            String siteIdStr = StringUtils.trimToNull( columns[31] );
            if (siteIdStr != null)
            {
                specimenItem.setSiteId( Integer.parseInt( siteIdStr ));
            }

            String collectorIdStr = StringUtils.trimToNull( columns[32] );
            if (collectorIdStr != null)
            {
                specimenItem.setCollectorId( Integer.parseInt( collectorIdStr ));
            }

            specimenItem.setCatalogedById( Integer.parseInt(   StringUtils.trimToNull( columns[33] )));
            specimenItem.setFormat(                            StringUtils.trimToNull( columns[34] ));
            specimenItem.setSeriesAbbrev(                      StringUtils.trimToNull( columns[35] ));
            specimenItem.setSeriesNo(                          StringUtils.trimToNull( columns[36] ));
            specimenItem.setContainer(                         StringUtils.trimToNull( columns[37] ));
            specimenItem.setSubcollectionId( Integer.parseInt( StringUtils.trimToNull( columns[38] )));
		}
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return specimenItem;
	}

	private Preparation convert(SpecimenItem specimenItem) throws LocalException
	{
		CollectionObject collectionObject = new CollectionObject();

		collectionObject.setGuid(String.valueOf(specimenItem.getSpecimenId()));

		Integer barcode = specimenItem.getBarcode();
		if (barcode == null)
		{
		    throw new LocalException("Null barcode");
		}

		try
		{
		    String catalogNumber = (new DecimalFormat( "000000000" ) ).format( barcode );
		    collectionObject.setCatalogNumber(catalogNumber);
		}
		catch (IllegalArgumentException e)
		{
		    throw new LocalException("Couldn't parse barcode");
		}

		String collectorNo = specimenItem.getCollectorNo();
		if (collectorNo != null && collectorNo.length() > 50)
		{
			warn("Truncating collector number", specimenItem.getSpecimenId(), collectorNo);
			collectorNo = collectorNo.substring(0, 50);
		}
		collectionObject.setFieldNumber(collectorNo);

		// TODO: countAmt: do we need to preserve this?  I'm not importing it at the moment.
		// TODO: container: this should go somewhere, but I don't know where.
		
		// TimestampCreated
        Date dateCreated = specimenItem.getCatalogedDate();
        collectionObject.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
		collectionObject.setCatalogedDatePrecision( (byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal() );

		collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement

		String description = specimenItem.getDescription();
		if (description != null && description.length() > 255)
		{
			warn("Truncating description", specimenItem.getSpecimenId(), description);
			description = description.substring(0, 255);
		}
		collectionObject.setDescription(description);

		String reproStatus = specimenItem.getReproStatus();
		collectionObject.setText1(reproStatus);
	      
		String substrate = specimenItem.getSubstrate();
		collectionObject.setText2(substrate);

		String remarks = specimenItem.getRemarks();
		collectionObject.setRemarks(remarks);
		
	    // Accession
        Accession accession = new Accession();
        String accessionNumber = specimenItem.getAccessionNo();
        String provenance = specimenItem.getProvenance();
        String status = specimenItem.getAccessionStatus();
        
        if (provenance != null && accessionNumber == null)
        {
            accessionNumber = "N/A";
        }
        if (accessionNumber != null)
        {
            accession.setAccessionNumber(accessionNumber);
            accession.setRemarks(provenance);
            accession.setStatus(status);
        }
		collectionObject.setAccession(accession);
        
		CollectingEvent collectingEvent = new CollectingEvent();
		BDate bdate = specimenItem.getCollDate();

		Integer startYear  = bdate.getStartYear();
		Integer startMonth = bdate.getStartMonth();
		Integer startDay   = bdate.getStartDay();

		if ( DateUtils.isValidSpecifyDate( startYear, startMonth, startDay ) )
		{
			collectingEvent.setStartDate( DateUtils.getSpecifyStartDate( bdate ) );
			collectingEvent.setStartDatePrecision( DateUtils.getDatePrecision( startYear, startMonth, startDay ) );
		}
		else if ( DateUtils.isValidCollectionDate( startYear, startMonth, startDay ) )
		{
			String startDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
			if (startDateVerbatim != null && startDateVerbatim.length() > 50)
			{
				warn("Truncating start date verbatim", specimenItem.getSpecimenId(), startDateVerbatim);
				startDateVerbatim = startDateVerbatim.substring(0, 50);
			}
			collectingEvent.setStartDateVerbatim(startDateVerbatim);
		}
		else {
			warn("Invalid start date", specimenItem.getSpecimenId(),
			        String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay));
		}

		// StartDatePrecision
		// TODO: start/end date precision: find out what the options are

		Integer endYear  = bdate.getEndYear();
		Integer endMonth = bdate.getEndMonth();
		Integer endDay   = bdate.getEndDay();

		if ( DateUtils.isValidSpecifyDate( endYear, endMonth, endDay ) )
		{
			collectingEvent.setEndDate( DateUtils.getSpecifyEndDate( bdate ) );
			collectingEvent.setEndDatePrecision( DateUtils.getDatePrecision( endYear, endMonth, endDay ) );
		}
		else if ( DateUtils.isValidCollectionDate( endYear, endMonth, endDay ) )
		{
			String endDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
			if (endDateVerbatim != null && endDateVerbatim.length() > 50)
			{
				warn("Truncating end date verbatim", specimenItem.getSpecimenId(), endDateVerbatim);
				endDateVerbatim = endDateVerbatim.substring(0, 50);
			}
			collectingEvent.setEndDateVerbatim(endDateVerbatim);
		}

	    String habitat = specimenItem.getHabitat();
	    collectingEvent.setRemarks(habitat);
	    
		collectionObject.setCollectingEvent(collectingEvent);

		Preparation preparation = new Preparation();
		preparation.setSampleNumber(String.valueOf(barcode));
		
		Integer itemNo = specimenItem.getItemNo();
		preparation.setNumber1((float) itemNo);

		Boolean isOversize = specimenItem.isOversize();
		preparation.setYesNo1(isOversize);
		
		String sex = specimenItem.getSex();
		if (sex != null)
		{
		    if (sex.equals("male"))
		    {
		        preparation.setYesNo2(true);
		    }
		    else if (sex.equals("female"))
		    {
		        preparation.setYesNo3(true);
		    }
		}

		String voucher = specimenItem.getVoucher(); // TODO: investigate
		preparation.setText1(voucher);

		String reference = specimenItem.getReference(); // TODO: investigate
		preparation.setText2(reference);

		String note = specimenItem.getNote();
		preparation.setRemarks(note);

		preparation.setCollectionObject(collectionObject);

		return preparation;
	}
	   
    private String getInsertSql(Accession accession)
    {
        String fieldNames = "AccessionNumber, Status, Remarks, DivisionID, TimestampCreated";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( accession.getAccessionNumber()  );
        values[1] = SqlUtils.sqlString( accession.getStatus()           );
        values[2] = SqlUtils.sqlString( accession.getRemarks()          );
        values[3] = SqlUtils.sqlString( accession.getDivision().getId() );
        values[4] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    private String getInsertSql(CollectingEvent collectingEvent) throws LocalException
    {
        String fieldNames = "EndDate, EndDatePrecision, EndDateVerbatim, StartDate, StartDatePrecision, " +
                            "StartDateVerbatim, VerbatimDate, DisciplineID, LocalityID, TimestampCreated";

        String[] values = new String[10];
        
        values[0] = SqlUtils.sqlString( collectingEvent.getEndDate()                      );
        values[1] = SqlUtils.sqlString( collectingEvent.getEndDatePrecision()             );
        values[2] = SqlUtils.sqlString( collectingEvent.getEndDateVerbatim()              );
        values[3] = SqlUtils.sqlString( collectingEvent.getStartDate()                    );
        values[4] = SqlUtils.sqlString( collectingEvent.getStartDatePrecision()           );
        values[5] = SqlUtils.sqlString( collectingEvent.getStartDateVerbatim()            );
        values[6] = SqlUtils.sqlString( collectingEvent.getVerbatimDate()                 );
        values[7] = SqlUtils.sqlString( collectingEvent.getDiscipline().getDisciplineId() );
        values[8] = SqlUtils.sqlString( collectingEvent.getLocality().getLocalityId()     );
        values[9] = SqlUtils.now();

        return SqlUtils.getInsertSql("collectingevent", fieldNames, values);
    }

    private String getInsertSql(Collector collector)
    {
        String fieldNames = "CollectionMemberID, IsPrimary, OrderNumber, AgentID, CollectingEventID, TimestampCreated";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( collector.getCollectionMemberId()                     );
        values[1] = SqlUtils.sqlString( collector.getIsPrimary()                              );
        values[2] = SqlUtils.sqlString( collector.getOrderNumber()                            );
        values[3] = SqlUtils.sqlString( collector.getAgent().getAgentId()                     );
        values[4] = SqlUtils.sqlString( collector.getCollectingEvent().getCollectingEventId() );
        values[5] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("collector", fieldNames, values);
    }
    
	private String getInsertSql(CollectionObject collectionObject) throws LocalException
	{
		String fieldNames = "AccessionID, CollectionID, CollectionMemberID, CatalogerID, CatalogNumber, " +
							"CatalogedDate, CatalogedDatePrecision, CollectingEventID, Description, " +
							"FieldNumber, GUID, Text1, Text2, YesNo1, Remarks, CreatedByAgentID, ContainerID," +
							"TimestampCreated";

		String[] values = new String[18];
		
		values[0]  = SqlUtils.sqlString( collectionObject.getAccession().getId()       );
		values[1]  = SqlUtils.sqlString( collectionObject.getCollection().getId()      );
		values[2]  = SqlUtils.sqlString( collectionObject.getCollectionMemberId()      );
		values[3]  = SqlUtils.sqlString( collectionObject.getCataloger().getAgentId()  );
		values[4]  = SqlUtils.sqlString( collectionObject.getCatalogNumber()           );
		values[5]  = SqlUtils.sqlString( collectionObject.getCatalogedDate()           );
		values[6]  = SqlUtils.sqlString( collectionObject.getCatalogedDatePrecision()  );
		values[7]  = SqlUtils.sqlString( collectionObject.getCollectingEvent().getId() );
		values[8]  = SqlUtils.sqlString( collectionObject.getDescription()             );
		values[9]  = SqlUtils.sqlString( collectionObject.getFieldNumber()             );
		values[10] = SqlUtils.sqlString( collectionObject.getGuid()                    );
		values[11] = SqlUtils.sqlString( collectionObject.getText1()                   );
		values[12] = SqlUtils.sqlString( collectionObject.getText2()                   );
		values[13] = SqlUtils.sqlString( collectionObject.getYesNo1()                  );
		values[14] = SqlUtils.sqlString( collectionObject.getRemarks()                 );
		values[15] = SqlUtils.sqlString( collectionObject.getCreatedByAgent().getId()  );
		values[16] = SqlUtils.sqlString( collectionObject.getContainer().getId()       );
		values[17] = SqlUtils.sqlString( collectionObject.getTimestampCreated()        );

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}
	
    private String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, PrepTypeID, " +
				            "Number1, YesNo1, Text1, Text2, Remarks, TimestampCreated";

		String[] values = new String[9];
		
		values[0] = SqlUtils.sqlString( preparation.getCollectionMemberId()                       );
		values[1] = SqlUtils.sqlString( preparation.getCollectionObject().getCollectionObjectId() );
		values[2] = SqlUtils.sqlString( preparation.getPrepType().getPrepTypeId()                 );
		values[3] = SqlUtils.sqlString( preparation.getNumber1()                                  );
		values[4] = SqlUtils.sqlString( preparation.getYesNo1()                                   );
		values[5] = SqlUtils.sqlString( preparation.getText1()                                    );
		values[6] = SqlUtils.sqlString( preparation.getText2()                                    );
		values[7] = SqlUtils.sqlString( preparation.getRemarks()                                  );
        values[8] = SqlUtils.now();
        
		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}

    private String getInsertSql(ExsiccataItem exsiccataItem) throws LocalException
	{
		String fieldNames = "Fascicle, Number, ExsiccataID, CollectionObjectID, TimestampCreated";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( exsiccataItem.getFascicle()                 );
		values[1] = SqlUtils.sqlString( exsiccataItem.getNumber()                   );
		values[2] = SqlUtils.sqlString( exsiccataItem.getExsiccata().getId()        );
		values[3] = SqlUtils.sqlString( exsiccataItem.getCollectionObject().getId() );
		values[4] = SqlUtils.now();

		return SqlUtils.getInsertSql("exsiccataitem", fieldNames, values);
	}
    
    // TODO: implement OtherIdentifier
    private String getInsertSql(OtherIdentifier otherIdentifer)
    {
        String fieldNames = "";
        
        String[] values = new String[0];
        
        return SqlUtils.getInsertSql("otheridentifier", fieldNames, values);
    }
}
