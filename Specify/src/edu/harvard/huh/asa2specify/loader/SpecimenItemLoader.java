package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ExsiccataItem;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.util.Pair;

public class SpecimenItemLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(SpecimenItemLoader.class);

	private Discipline discipline;
	private Division   division;
	
	private Integer lastSpecimenId;
	private HashMap<String, CollectionObject> collObjByAccessionIdentifier;
	
	// initialize collection code to id hashtable
	private Hashtable<String, Collection> collectionsByCode = new Hashtable<String, Collection>();

	// initialize prep type hashtable
	private Hashtable<String, PrepType> prepTypesByName = new Hashtable<String, PrepType>();
	
	public SpecimenItemLoader(File csvFile, Statement sqlStatement, Discipline discipline, Division division) {
		super(csvFile, sqlStatement);
		
		this.discipline = discipline;
		this.division   = division;
		
		this.collObjByAccessionIdentifier = new HashMap<String, CollectionObject>();
	}

	private boolean isEmpty(Accession accession)
	{
	    return accession.getAccessionNumber() != null;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		String sql;
		
		SpecimenItem specimenItem = parseSpecimenItemRecord(columns);
		
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
        
        // if this preparation shares the same collection object with the previously
        // inserted one, re-use it TODO: this is probably mostly correct, but should analyze.
        Integer specimenId = specimenItem.getSpecimenId();
        Accession accession = collectionObject.getAccession();
        String accessionIdentifier = accession.getAccessionNumber() + accession.getRemarks();

        if (specimenId.equals(lastSpecimenId))
        {
            // if the two specimen items don't have different accession numbers they 
            // share a collection object; just create a different preparation
            if (collObjByAccessionIdentifier.containsKey(accessionIdentifier))
            {
                preparation.setCollectionObject(collObjByAccessionIdentifier.get(accessionIdentifier));

                // insert Preparation
                preparation.setCollectionMemberId(collectionObject.getCollectionMemberId());
                preparation.setPrepType(prepType);

                sql = getInsertSql(preparation);
                insert(sql);

                return;
            }
            else
            {
                collObjByAccessionIdentifier.put(accessionIdentifier, collectionObject);
                
                // different collection object but same collecting event, collection, cataloger, creator, exsiccata
                CollectionObject previousCollectionObject =
                    collObjByAccessionIdentifier.get(collObjByAccessionIdentifier.values().iterator().next());

                collectionObject.setCollectingEvent(previousCollectionObject.getCollectingEvent());
                collectionObject.setCollection(previousCollectionObject.getCollection());
                collectionObject.setCollectionMemberId(previousCollectionObject.getCollectionMemberId());
                collectionObject.setCataloger(previousCollectionObject.getCataloger());
                collectionObject.setCreatedByAgent(previousCollectionObject.getCreatedByAgent());
                
                // insert an accession if necessary
                
                if (! isEmpty(accession))
                {
                    sql = getInsertSql(accession);
                    Integer accessionId = insert(sql);
                    accession.setAccessionId(accessionId);
                    collectionObject.setAccession(accession);
                }

                // insert the collectionobject
                sql = getInsertSql(collectionObject);
                Integer collectionObjectId = insert(sql);
                collectionObject.setCollectionObjectId(collectionObjectId);
                
                // insert the preparation
                preparation.setCollectionMemberId(previousCollectionObject.getCollectionMemberId());
                preparation.setPrepType(prepType);
                
                sql = getInsertSql(preparation);
                insert(sql);

                // insert new exsiccata item if necessary
                if (specimenItem.getSeriesId() != null)
                {
                    Series series =  new Series();
                    
                    String guid = SqlUtils.sqlString(series.getGuid());
                    
                    String subselect =  "(" + SqlUtils.getQueryIdByFieldSql("referencework", "ReferenceWorkID", "GUID", guid) + ")";
                    
                    sql = SqlUtils.getQueryIdByFieldSql("exsiccata", "ExsiccataId", "ReferenceWorkID", subselect);
                    
                    Integer exsiccataId = queryForId(sql);
                    
                    if (exsiccataId == null)
                    {
                        throw new LocalException("Couldn't find exsiccata id for " + guid);
                    }
                    
                    Exsiccata exsiccata = new Exsiccata();
                    exsiccata.setExsiccataId(exsiccataId);
                    
                    ExsiccataItem exsiccataItem = new ExsiccataItem();
                    
                    exsiccataItem.setExsiccata(exsiccata);
                    exsiccataItem.setNumber(specimenItem.getSeriesNo());
                    exsiccataItem.setCollectionObject(collectionObject);
                    
                    sql = getInsertSql(exsiccataItem);
                    insert(sql);
                }
                else if (specimenItem.getSeriesNo() != null)
                {
                    log.warn("Null series and non-null series_no");
                }
                
                return;
            }
        }

        // if we made it here, the asa.specimen.ids are different, so we have a different collection object entirely

        lastSpecimenId = specimenId;
        collObjByAccessionIdentifier.clear();
        collObjByAccessionIdentifier.put(accessionIdentifier, collectionObject);
        
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
		
		// find the matching series record
		Exsiccata exsiccata = new Exsiccata();
		Integer seriesId = specimenItem.getSeriesId();
		
		if (seriesId != null)
		{
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
        else if (specimenItem.getSeriesNo() != null)
        {
            log.warn("Null series and non-null series_no");
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
		Integer botanistId = specimenItem.getCollectorId();
		
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
		Integer createdById = specimenItem.getCatalogedById();
		Agent cataloger = this.getAgentByOptrId(createdById);
		
	    // and from which we might get an Accession
        if (! isEmpty(accession))
        {
            // insert Accession
            sql = getInsertSql(accession);
            Integer accessionId = insert(sql);
            accession.setAccessionId(accessionId);
            accession.setDivision(division);
        }
            
		// also from CollectionObject we get the CollectingEvent
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
		collectionObject.setCreatedByAgent(cataloger);
		
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
			exsiccataItem.setNumber(specimenItem.getSeriesNo());
			exsiccataItem.setCollectionObject(collectionObject);
			exsiccataItem.setExsiccata(exsiccata);
			
			sql = getInsertSql(exsiccataItem);
			insert(sql);
		}
	}

	private SpecimenItem parseSpecimenItemRecord(String[] columns) throws LocalException
	{
		if (columns.length < 37)
		{
			throw new LocalException("Wrong number of columns");
		}

		SpecimenItem specimenItem = new SpecimenItem();

		try {
		    specimenItem.setId(         Integer.parseInt( StringUtils.trimToNull( columns[0] )));
		    specimenItem.setSpecimenId( Integer.parseInt( StringUtils.trimToNull( columns[1] )));
		    specimenItem.setBarcode(    Integer.parseInt( StringUtils.trimToNull( columns[2] )));
		    specimenItem.setCollectorNo(                  StringUtils.trimToNull( columns[3] ));
		    
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
            
            bdate.setStartYear(           Integer.parseInt(  StringUtils.trimToNull( columns[15] )));
            bdate.setStartMonth(          Integer.parseInt(  StringUtils.trimToNull( columns[16] )));
            bdate.setStartDay(            Integer.parseInt(  StringUtils.trimToNull( columns[17] )));
            bdate.setStartPrecision(                         StringUtils.trimToNull( columns[18] ));
            bdate.setEndYear(              Integer.parseInt( StringUtils.trimToNull( columns[19] )));
            bdate.setEndMonth(             Integer.parseInt( StringUtils.trimToNull( columns[20] )));
            bdate.setEndDay(               Integer.parseInt( StringUtils.trimToNull( columns[21] )));
            bdate.setEndPrecision(                           StringUtils.trimToNull( columns[22] ));
            bdate.setText(                                   StringUtils.trimToNull( columns[23] ));            
            specimenItem.setItemNo(        Integer.parseInt( StringUtils.trimToNull( columns[24] )));
            
            String isOversize = StringUtils.trimToNull( columns[25] );
            specimenItem.setOversize( isOversize != null && isOversize.equals("true") ? true : false);
            
            specimenItem.setVoucher(                         StringUtils.trimToNull( columns[26] ));
            specimenItem.setReference(                       StringUtils.trimToNull( columns[27] ));
            specimenItem.setNote(                            StringUtils.trimToNull( columns[28] ));
            specimenItem.setHerbariumCode(                   StringUtils.trimToNull( columns[29] ));
            specimenItem.setSeriesId(      Integer.parseInt( StringUtils.trimToNull( columns[30] )));
            specimenItem.setSiteId(        Integer.parseInt( StringUtils.trimToNull( columns[31] )));
            specimenItem.setCollectorId(   Integer.parseInt( StringUtils.trimToNull( columns[32] )));
            specimenItem.setCatalogedById( Integer.parseInt( StringUtils.trimToNull( columns[33] )));
            specimenItem.setFormat(                          StringUtils.trimToNull( columns[34] ));
            specimenItem.setSeriesNo(                        StringUtils.trimToNull( columns[35] ));
            specimenItem.setContainer(                       StringUtils.trimToNull( columns[36] ));
		}
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return specimenItem;
	}

	// TODO: oops, I'm creating a new collectionobject for each preparation.
	private Preparation convert(SpecimenItem specimenItem) {

		CollectionObject collectionObject = new CollectionObject();

		collectionObject.setGuid(String.valueOf(specimenItem.getSpecimenId()));

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
		// TODO: container: this should go somewhere, but I don't know where.
		
		// TimestampCreated
        Date dateCreated = specimenItem.getCatalogedDate();
        collectionObject.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
		collectionObject.setCatalogedDatePrecision( (byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal() );

		collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement

		String description = specimenItem.getDescription();
		if (description != null && description.length() > 255)
		{
			log.warn("Truncating description");
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
		// TODO: start/end date precision: find out what the options are

		Integer endYear  = bdate.getEndYear();
		Integer endMonth = bdate.getEndMonth();
		Integer endDay   = bdate.getEndDay();

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
        String fieldNames = "AccessionNumber, AccessionStatus, Remarks, DivisionID, TimestampCreated";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( accession.getAccessionNumber()  );
        values[1] = SqlUtils.sqlString( accession.getStatus()           );
        values[2] = SqlUtils.sqlString( accession.getRemarks()          );
        values[3] =     String.valueOf( accession.getDivision().getId() );
        values[4] = "now()";
        
        return SqlUtils.getInsertSql("accession", fieldNames, values);
    }

    private String getInsertSql(CollectingEvent collectingEvent) throws LocalException
    {
        String fieldNames = "EndDate, EndDatePrecision, EndDateVerbatim, StartDate, StartDatePrecision, " +
                            "StartDateVerbatim, VerbatimDate, DisciplineID, LocalityID, TimestampCreated";

        String[] values = new String[10];
        
        values[0] = SqlUtils.sqlString( collectingEvent.getEndDate()                      );
        values[1] =     String.valueOf( collectingEvent.getEndDatePrecision()             );
        values[2] = SqlUtils.sqlString( collectingEvent.getEndDateVerbatim()              );
        values[3] = SqlUtils.sqlString( collectingEvent.getStartDate()                    );
        values[4] =     String.valueOf( collectingEvent.getStartDatePrecision()           );
        values[5] = SqlUtils.sqlString( collectingEvent.getStartDateVerbatim()            );
        values[6] = SqlUtils.sqlString( collectingEvent.getVerbatimDate()                 );
        values[7] =     String.valueOf( collectingEvent.getDiscipline().getDisciplineId() );
        values[8] =     String.valueOf( collectingEvent.getLocality().getLocalityId()     );
        values[9] = "now()";

        return SqlUtils.getInsertSql("collectingevent", fieldNames, values);
    }

    private String getInsertSql(Collector collector)
    {
        String fieldNames = "CollectionMemberID, IsPrimary, OrderNumber, AgentID, CollectingEventID, TimestampCreated";
        
        String[] values = new String[6];
        
        values[0] = String.valueOf( collector.getCollectionMemberId()                     );
        values[1] = String.valueOf( collector.getIsPrimary()                              );
        values[2] = String.valueOf( collector.getOrderNumber()                            );
        values[3] = String.valueOf( collector.getAgent().getAgentId()                     );
        values[4] = String.valueOf( collector.getCollectingEvent().getCollectingEventId() );
        values[5] = "now()";
        
        return SqlUtils.getInsertSql("collector", fieldNames, values);
    }
    
	private String getInsertSql(CollectionObject collectionObject) throws LocalException
	{
		String fieldNames = "AccessionID, CollectionID, CollectionMemberID, CatalogerID, CatalogNumber, " +
							"CatalogedDate, CatalogedDatePrecision, CollectingEventID, Description, " +
							"FieldNumber, GUID, Text1, Text2, YesNo1, Remarks, CreatedByAgentID, TimestampCreated";

		String[] values = new String[17];
		
		values[0]  =     String.valueOf( collectionObject.getAccession().getAccessionId()             );
		values[1]  =     String.valueOf( collectionObject.getCollection().getCollectionId()           );
		values[2]  =     String.valueOf( collectionObject.getCollectionMemberId()                     );
		values[3]  =     String.valueOf( collectionObject.getCataloger().getAgentId()                 );
		values[4]  = SqlUtils.sqlString( collectionObject.getCatalogNumber()                          );
		values[5]  = SqlUtils.sqlString( collectionObject.getCatalogedDate()                          );
		values[6]  =     String.valueOf( collectionObject.getCatalogedDatePrecision()                 );
		values[7]  =     String.valueOf( collectionObject.getCollectingEvent().getCollectingEventId() );
		values[8]  = SqlUtils.sqlString( collectionObject.getDescription()                            );
		values[9]  = SqlUtils.sqlString( collectionObject.getFieldNumber()                            );
		values[10] = SqlUtils.sqlString( collectionObject.getGuid()                                   );
		values[11] = SqlUtils.sqlString( collectionObject.getText1()                                  );
		values[12] = SqlUtils.sqlString( collectionObject.getText2()                                  );
		values[13] =     String.valueOf( collectionObject.getYesNo1()                                 );
		values[14] = SqlUtils.sqlString( collectionObject.getRemarks()                                );
		values[15] =     String.valueOf( collectionObject.getCreatedByAgent().getId()                 );
		values[16] = SqlUtils.sqlString( collectionObject.getTimestampCreated()                       );

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}
	
    private String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, PrepTypeID, " +
				            "Number1, YesNo1, Text1, Text2, Remarks, TimestampCreated";

		String[] values = new String[9];
		
		values[0] =     String.valueOf( preparation.getCollectionMemberId()                       );
		values[1] =     String.valueOf( preparation.getCollectionObject().getCollectionObjectId() );
		values[2] =     String.valueOf( preparation.getPrepType().getPrepTypeId()                 );
		values[3] =     String.valueOf( preparation.getNumber1()                                  );
		values[4] =     String.valueOf( preparation.getYesNo1()                                   );
		values[5] = SqlUtils.sqlString( preparation.getText1()                                    );
		values[6] = SqlUtils.sqlString( preparation.getText2()                                    );
		values[7] = SqlUtils.sqlString( preparation.getRemarks()                                  );
        values[8] = "now()";
        
		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}

    private String getInsertSql(ExsiccataItem exsiccataItem) throws LocalException
	{
		String fieldNames = "Fascicle, Number, ExsiccataID, CollectionObjectID, TimestampCreated";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( exsiccataItem.getFascicle()                 );
		values[1] = SqlUtils.sqlString( exsiccataItem.getNumber()                   );
		values[2] =     String.valueOf( exsiccataItem.getExsiccata().getId()        );
		values[3] =     String.valueOf( exsiccataItem.getCollectionObject().getId() );
		values[4] ="now";

		return SqlUtils.getInsertSql("exsiccataitem", fieldNames, values);
	}

}
