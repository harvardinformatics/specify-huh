package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Hashtable;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.SpecimenItem;
import edu.harvard.huh.asa.SpecimenItem.CONTAINER_TYPE;
import edu.harvard.huh.asa.SpecimenItem.FORMAT;
import edu.harvard.huh.asa.SpecimenItem.PREP_METHOD;
import edu.harvard.huh.asa.SpecimenItem.REPRO_STATUS;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.CollectingTripLookup;
import edu.harvard.huh.asa2specify.lookup.ContainerLookup;
import edu.harvard.huh.asa2specify.lookup.SeriesLookup;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.SiteLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectingTrip;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.CollectionObjectCitation;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Container;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.OtherIdentifier;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Storage;

public class SpecimenItemLoader extends AuditedObjectLoader
{
    private Hashtable<String, PrepType> prepTypesByFormatAndColl;
	
	private SpecimenLookup collObjLookup;
	private PreparationLookup prepLookup;
	private CollectingTripLookup collTripLookup;
	
	private BotanistLookup botanistLookup;
	private ContainerLookup containerLookup;
	private SubcollectionLookup subcollLookup;
	private SiteLookup siteLookup;
	private SeriesLookup seriesLookup;
	
	// These objects are all related to the collection object
	// and need the collection object to be saved first
	
	private final Container      nullContainer      = new Container();
	private final CollectingTrip nullCollectingTrip = new CollectingTrip();
    private final Storage        nullStorage        = new Storage();
	
	public SpecimenItemLoader(File csvFile,
	                          Statement sqlStatement,
	                          File seriesBotanists,
	                          BotanistLookup botanistLookup,
	                          SubcollectionLookup subcollLookup,
	                          SeriesLookup seriesLookup,
	                          SiteLookup siteLookup) throws LocalException 
	{
		super(csvFile, sqlStatement);
		
		this.prepTypesByFormatAndColl = new Hashtable<String, PrepType>();
		
		this.botanistLookup  = botanistLookup;
		this.subcollLookup   = subcollLookup;
		this.siteLookup      = siteLookup;
        this.seriesLookup    = seriesLookup;
        this.containerLookup = getContainerLookup();
		this.prepLookup      = getPreparationLookup();
		this.collTripLookup  = getCollectingTripLookup();
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
	    SpecimenItem specimenItem = parse(columns);
		
		Integer specimenItemId = specimenItem.getId();
		
		Integer newSpecimenId = specimenItem.getSpecimenId();
		checkNull(newSpecimenId, "specimen id");
		
		setCurrentRecordId(specimenItemId == null ? newSpecimenId : specimenItemId);
        
        ////////////////////////////////////////////////
        // Begin construction of new CollectionObject //
        ////////////////////////////////////////////////

		try
		{
		    // Collection
		    Collection collection = getCollection(specimenItem);

		    Integer collectionId = collection.getId();

		    // CollectionObject
		    CollectionObject collectionObject = getCollectionObject(specimenItem, collection);
		    if (collectionObject.getId() == null)
		    {
		        String sql = getInsertSql(collectionObject);
		        Integer collectionObjectId = insert(sql);
		        collectionObject.setCollectionObjectId(collectionObjectId);
		    }

		    // Preparation
		    Preparation preparation = getPreparation(specimenItem, collectionId, collectionObject);
		    if (preparation != null)
		    {
		        if (preparation.getId() == null)
		        {
		            String sql = getInsertSql(preparation);
		            Integer preparationId = insert(sql);
		            preparation.setPreparationId(preparationId);
		        }
		    }
		    else
		    {
		        getLogger().warn(rec() + "Null specimen item");
		    }

		    // ExsiccataItem
		    CollectionObjectCitation exsiccataItem = getExsiccataItem(specimenItem, collectionObject, collectionId);
		    if (exsiccataItem != null)
		    {
		        if (exsiccataItem.getId() == null)
		        {
		            String sql = getInsertSql(exsiccataItem);
		            Integer exsiccataItemId = insert(sql);
		            exsiccataItem.setCollectionObjectCitationId(exsiccataItemId);
		        }
		    }

		    // OtherIdentifiers
		    OtherIdentifier seriesIdentifier = getSeriesIdentifier(specimenItem, collectionObject, collectionId);
		    if (seriesIdentifier != null)
		    {
		        if (seriesIdentifier.getId() == null)
		        {
		            String sql = getInsertSql(seriesIdentifier);
		            Integer otherIdentifierId = insert(sql);
		            seriesIdentifier.setOtherIdentifierId(otherIdentifierId);
		        }
		    }
		    OtherIdentifier accessionIdentifier = getAccessionIdentifier(specimenItem, collectionObject, collectionId);
		    if (accessionIdentifier != null)
		    {
		        if (accessionIdentifier.getId() == null)
		        {
		            String sql = getInsertSql(accessionIdentifier);
		            Integer otherIdentifierId = insert(sql);
		            accessionIdentifier.setOtherIdentifierId(otherIdentifierId);
		        }
		    }

		}
		catch (Exception e)
		{
		    e.printStackTrace();
		    throw new LocalException(e);
		}
	}
	
	public ContainerLookup getContainerLookup()
	{
		if (containerLookup == null)
		{
			containerLookup = new ContainerLookup() {
				public Container getByName(String name) throws LocalException
				{
					Container container = new Container();
					
					Integer logicalContainerId = getId("container", "ContainerID", "Name", name);
					
					container.setContainerId(logicalContainerId);
					
					return container;
				}
				
				public Container queryByName(String name) throws LocalException
				{
					Integer logicalContainerId = queryForInt("container", "ContainerID", "Name", name);
					
					if (logicalContainerId == null) return null;
					
					Container logicalContainer = new Container();
					
					logicalContainer.setContainerId(logicalContainerId);
					
					return logicalContainer;
				}
			};
		}
		return containerLookup;
	}

	public CollectingTripLookup getCollectingTripLookup()
	{
	    if (collTripLookup == null)
	    {
	        collTripLookup = new CollectingTripLookup() {
	            public CollectingTrip queryByName(String name) throws LocalException
	            {	                
	                Integer collectingTripId = queryForInt("collectingtrip", "CollectingTripID", "CollectingTripName", name);

	                if (collectingTripId == null) return null;
	                
	                CollectingTrip collectingTrip = new CollectingTrip();
	                
	                collectingTrip.setCollectingTripId(collectingTripId);
	                
	                return collectingTrip;
	            }
	        };
	    }
	    return collTripLookup;
	}
	
	public SpecimenLookup getSpecimenLookup()
    {
        if (collObjLookup == null)
        {
            collObjLookup = new SpecimenLookup() {
                
                public CollectionObject getById(Integer specimenId) throws LocalException
                {
                    CollectionObject collObj = new CollectionObject();
                    
                    Integer collObjectId = null;
                    
                    String altCatalogNumber = getAltCatalogNumber(specimenId);

                    String sql = "select co.CollectionObjectID from collectionobject co left join preparation p " +
                    		"on co.CollectionObjectID=p.CollectionObjectID where co.AltCatalogNumber=\""
                    		+ altCatalogNumber + "\"";

                    // first, assume there is a prep, possibly more, and take the one with SampleNumber=1
                    collObjectId = queryForInt(sql + " and p.SampleNumber=1");

                    // as a backup, try the plain collobj
                    if (collObjectId == null) collObjectId = getInt(sql);

                    collObj.setCollectionObjectId(collObjectId);
                    
                    return collObj;
                }

            };
        }
        
        return collObjLookup;
    }

    public PreparationLookup getPreparationLookup()
    {
        if (prepLookup == null)
        {
            prepLookup = new PreparationLookup() {
                public String formatCollObjBarcode(Integer barcode) throws LocalException
                {
                    if (barcode == null)
                    {
                        return null;
                    }
                    
                    try
                    {
                        return (new DecimalFormat( "00000000" ) ).format( barcode );
                    }
                    catch (IllegalArgumentException e)
                    {
                        throw new LocalException("Couldn't parse barcode");
                    }
                }
                
                public String formatPrepBarcode(Integer barcode) throws LocalException
                {
                    if (barcode == null)
                    {
                        return null;
                    }
                    
                    try
                    {
                        String prepBarcode = formatCollObjBarcode(barcode);
                        return prepBarcode.replaceFirst("^0*", "");

                    }
                    catch (IllegalArgumentException e)
                    {
                        throw new LocalException("Couldn't parse barcode");
                    }
                }
                
                public Preparation getByBarcode(String barcode) throws LocalException
                {                    
                    String sql = "select p.PreparationID from preparation p join collectionobject co on p.CollectionObjectID=co.CollectionObjectID " +
                    		"where co.CatalogNumber=\"" + barcode + "\"";
                    
                    Integer preparationId = getInt(sql);
                    
                    Preparation preparation = new Preparation();
                    
                    preparation.setPreparationId(preparationId);
                    
                    return preparation;
                }

                @Override
                public Preparation getBySpecimenItemId(Integer specimenItemId) throws LocalException
                {
                    Preparation preparation = new Preparation();
                    
                    Integer preparationId = getInt("preparation", "PreparationID", "Number1", specimenItemId);
                    
                    preparation.setPreparationId(preparationId);
                    
                    return preparation;
                }
            };
        }
        return prepLookup;
    }
	
    protected void preLoad() throws LocalException
    {
        // disable keys
        String[] tables = { "collectionobject", "collectingevent", "collector", "preparation" };
        
        for (String table : tables)
        {
            String sql = "alter table " + table + " disable keys";
            execute(sql);
        }
       
    }

    @Override
    protected void postLoad() throws LocalException
    {        
        // enable keys
        getLogger().info("Enabling keys");
        
        String[] tables = { "collectionobject", "collectingevent", "collector", "preparation" };
        
        for (String table : tables)
        {
            String sql = "alter table " + table + " enable keys";
            execute(sql);
        }
        
        // TODO: probably drop these indexes after import        
        getLogger().info("Creating alt catalog number index");
        String sql =  "create index altcatnum on collectionobject(AltCatalogNumber)";
        execute(sql);
        
        // create two fragment relationships
        String hostRel = "insert into collectionreltype set TimestampCreated=now(), Version=0, Name='Host of'";
        execute(hostRel);  
        
        String parasiteRel = "insert into collectionreltype set TimestampCreated=now(), Version=0, Name='Parasite of'";
        execute(parasiteRel);
    }
    
    private Agent lookupSeries(Integer seriesId) throws LocalException
    {
        return seriesLookup.getById(seriesId);
    }
    
	private SpecimenItem parse(String[] columns) throws LocalException
	{
	    SpecimenItem specimenItem = new SpecimenItem();
	    
	    int i = super.parse(columns, specimenItem);
	    
		if (columns.length < i + 41)
		{
			throw new LocalException("Not enough columns");
		}

		
		try {
		    specimenItem.setSpecimenId(              SqlUtils.parseInt( columns[i + 0]  ));
		    specimenItem.setBarcode(                 SqlUtils.parseInt( columns[i + 1]  ));
		    specimenItem.setCollectorNo(                                columns[i + 2]  );
            specimenItem.setCultivated(           Boolean.parseBoolean( columns[i + 3]  ));
            specimenItem.setDescription(                                columns[i + 4]  );
            specimenItem.setHabitat(                                    columns[i + 5]  );
            specimenItem.setSubstrate(                                  columns[i + 6]  );
            specimenItem.setReproStatus( SpecimenItem.parseReproStatus( columns[i + 7]  ));
            specimenItem.setSex(                                        columns[i + 8]  );
            specimenItem.setRemarks(                                    columns[i + 9]  );
            specimenItem.setAccessionNo(                                columns[i + 10] );
            specimenItem.setProvenance(                                 columns[i + 11] );
            specimenItem.setAccessionStatus(                            columns[i + 12] );
            
            BDate bdate = new BDate();
            specimenItem.setCollDate( bdate );

            bdate.setStartYear(  SqlUtils.parseInt( columns[i + 13] ));
            bdate.setStartMonth( SqlUtils.parseInt( columns[i + 14] ));
            bdate.setStartDay(   SqlUtils.parseInt( columns[i + 15] ));
            bdate.setStartPrecision(                columns[i + 16] );
            bdate.setEndYear(    SqlUtils.parseInt( columns[i + 17] ));
            bdate.setEndMonth(   SqlUtils.parseInt( columns[i + 18] ));
            bdate.setEndDay(     SqlUtils.parseInt( columns[i + 19] ));
            bdate.setEndPrecision(                  columns[i + 20] );
            bdate.setText(                          columns[i + 21] );  
            
            specimenItem.setItemNo(          SqlUtils.parseInt( columns[i + 22] ));
            specimenItem.setOversize(     Boolean.parseBoolean( columns[i + 23] ));
            specimenItem.setVoucher(                            columns[i + 24] );
            specimenItem.setReference(                          columns[i + 25] );
            specimenItem.setNote(                               columns[i + 26] );
            specimenItem.setHerbariumCode(                      columns[i + 27] );
            specimenItem.setSeriesId(        SqlUtils.parseInt( columns[i + 28] ));
            specimenItem.setSeriesName(                         columns[i + 29] );
            specimenItem.setSiteId(          SqlUtils.parseInt( columns[i + 30] ));
            specimenItem.setCollectorId(     SqlUtils.parseInt( columns[i + 31] ));
            specimenItem.setFormat(   SpecimenItem.parseFormat( columns[i + 32] ));
            specimenItem.setSeriesAbbrev(                       columns[i + 33] );
            specimenItem.setSeriesNo(                           columns[i + 34] );
            specimenItem.setContainer(                          columns[i + 35] );
            specimenItem.setSubcollectionId( SqlUtils.parseInt( columns[i + 36] ));
            specimenItem.setReplicates(      SqlUtils.parseInt( columns[i + 37] ));
            specimenItem.setLocation(                           columns[i + 38] );
            specimenItem.setVernacularName(                     columns[i + 39] );
            specimenItem.setDistribution(                       columns[i + 40] );
		}
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
        	throw new LocalException("Couldn't parse repro status", e);
        }

        return specimenItem;
	}

	private Agent lookupBotanist(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}
	   
	private ReferenceWork lookupExsiccata(Integer subCollectionId) throws LocalException
	{
	    return subcollLookup.queryExsiccataById(subCollectionId);
	}

	private CollectingTrip lookupCollectingTrip(String name) throws LocalException
	{
	    return collTripLookup.queryByName(name);
	}
	
	private Container lookupContainer(String name) throws LocalException
	{
	    return containerLookup.queryByName(name);
	}

	private Storage lookupStorage(Integer subcollectionId) throws LocalException
	{
	    return subcollLookup.getStorageById(subcollectionId);
	}

	private Locality lookupSite(Integer siteId) throws LocalException
	{
	    return siteLookup.queryById(siteId);
	}

	private Collector getCollector(SpecimenItem specimenItem, CollectingEvent collectingEvent, Division division) throws LocalException
	{
	    Collector collector = new Collector();

        // Agent
        Integer botanistId = specimenItem.getCollectorId();
        if (botanistId == null) return null;
        
        Agent agent = lookupBotanist(botanistId);
        collector.setAgent(agent);

        // CollectingEvent
        collector.setCollectingEvent(collectingEvent);
        
        // CollectionMemberId
        collector.setDivision(division);
        
        // IsPrimary
        collector.setIsPrimary(true);

        // OrderNumber
        collector.setOrderNumber(1);
        
        return collector;
	}
	
	private Collector getSeriesCollector(SpecimenItem specimenItem, CollectingEvent collectingEvent, Division division) throws LocalException
	{        
        Collector collector = new Collector();

        // Agent
        Integer seriesId = specimenItem.getSeriesId();
        if (seriesId == null) return null;
        
        Agent agent = lookupSeries(seriesId);
        collector.setAgent(agent);

        // CollectingEvent
        collector.setCollectingEvent(collectingEvent);
        
        // CollectionMemberId
        collector.setDivision(division);
        
        // IsPrimary
        collector.setIsPrimary(collector == null);

        // OrderNumber
        collector.setOrderNumber(collector == null ? 1 : 2);
        
        return collector;
	}

	private CollectingEvent getCollectingEvent(SpecimenItem specimenItem, CollectingTrip collectingTrip, Discipline discipline) throws LocalException
	{
		CollectingEvent collectingEvent = new CollectingEvent();

		// CollectingTrip
		collectingEvent.setCollectingTrip(collectingTrip);

		// DisciplineID
        collectingEvent.setDiscipline(discipline);
        
		BDate bdate = specimenItem.getCollDate();

		Integer startYear  = bdate.getStartYear();
		Integer startMonth = bdate.getStartMonth();
		Integer startDay   = bdate.getStartDay();

        // StartDate and StartDatePrecision
		if ( DateUtils.isValidSpecifyDate( startYear, startMonth, startDay ) )
		{
			collectingEvent.setStartDate( DateUtils.getSpecifyStartDate( bdate ) );
			collectingEvent.setStartDatePrecision( DateUtils.getDatePrecision( startYear, startMonth, startDay ) );
		}
		// StartDateVerbatim
		else if ( DateUtils.isValidCollectionDate( startYear, startMonth, startDay ) )
		{
			String startDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
			if (startDateVerbatim != null)
			{
				startDateVerbatim = truncate(startDateVerbatim, 50, "start date verbatim");
				collectingEvent.setStartDateVerbatim(startDateVerbatim);
			}
		}
		else
		{
			getLogger().warn(rec() + "Invalid start date: " +
			        String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay));
		}

		// TODO: asa start/end date precision: find out what the options are

		Integer endYear  = bdate.getEndYear();
		Integer endMonth = bdate.getEndMonth();
		Integer endDay   = bdate.getEndDay();

		// EndDate and EndDatePrecision
		if ( DateUtils.isValidSpecifyDate( endYear, endMonth, endDay ) )
		{
			collectingEvent.setEndDate( DateUtils.getSpecifyEndDate( bdate ) );
			collectingEvent.setEndDatePrecision( DateUtils.getDatePrecision( endYear, endMonth, endDay ) );
		}
        else if (endYear != null || endMonth != null || endDay != null)
        {            
            BDate idate = DateUtils.getInterpolatedEndDate(bdate);

            if (idate != null)
            {
                // try again with interpolated values
                endYear  = idate.getEndYear();
                endMonth = idate.getEndMonth();
                endDay   = idate.getEndDay();

                // EndDate and EndDatePrecision
                if ( DateUtils.isValidSpecifyDate( endYear, endMonth, endDay ) )
                {
                    collectingEvent.setEndDate( DateUtils.getSpecifyEndDate( idate ) );
                    collectingEvent.setEndDatePrecision( DateUtils.getDatePrecision( endYear, endMonth, endDay ) );
                }
                // EndDateVerbatim
                else if ( DateUtils.isValidCollectionDate( endYear, endMonth, endDay ) )
                {
                    String endDateVerbatim = DateUtils.getSpecifyEndDateVerbatim( idate );
                    if (endDateVerbatim != null)
                    {
                        endDateVerbatim = truncate(endDateVerbatim, 50, "end date verbatim");
                        collectingEvent.setEndDateVerbatim(endDateVerbatim);
                    }
                }
                else
                {
                    getLogger().warn(rec() + "Invalid end date: " +
                            String.valueOf(endYear) + " " + String.valueOf(endMonth) + " " +String.valueOf(endDay));
                }
            }
            else
            {
                getLogger().warn(rec() + "Invalid end date: " +
                        String.valueOf(endYear) + " " + String.valueOf(endMonth) + " " +String.valueOf(endDay));
            }
        }
		
		if (collectingEvent.getStartDate() != null && collectingEvent.getEndDate() != null)
		{
		    if (collectingEvent.getStartDate().after(collectingEvent.getEndDate()))
		    {
		        getLogger().warn(rec() + "Start date after end date: " +
		                String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay) + ", " +
		                String.valueOf(endYear) + " " + String.valueOf(endMonth) + " " +String.valueOf(endDay));
		    }
		}
		
		String habitat = specimenItem.getHabitat();

        // Locality
	    Locality locality = null;
		
		Integer siteId = specimenItem.getSiteId();

		if (siteId != null)
		{
			// not checking for null; many localities were empty and not loaded
			locality = lookupSite(siteId);
			
			if (locality == null)
			{
				locality = new Locality();
			}
		}
		collectingEvent.setLocality(locality);
		
	    // Remarks (habitat)
	    collectingEvent.setRemarks(habitat);
	    
	    // StationFieldNumber (collector number)
	    String collectorNo = specimenItem.getCollectorNo();
        if (collectorNo != null)
        {
            collectorNo = truncate(collectorNo, 50, "collector number");
            collectingEvent.setStationFieldNumber(collectorNo);
        }
        
	    // VerbatimDate
	    String verbatimDate = bdate.getText();
	    if (verbatimDate!= null) verbatimDate = truncate(verbatimDate, 50, "date text");
	    collectingEvent.setVerbatimDate(verbatimDate);
	        
	    return collectingEvent;
	}
	
	private Preparation getPreparation(SpecimenItem specimenItem, Integer collectionMemberId, CollectionObject collObj)
		throws LocalException
	{
        if (specimenItem.getId() == null) return null;
        
        Integer specimenItemId = specimenItem.getId();
        Integer barcode = specimenItem.getBarcode();
        if (barcode == null)
        {
            getLogger().warn(rec() + "Null barcode");
        }

        Preparation preparation = new Preparation();
        
        // CollectionObject
        preparation.setCollectionObject(collObj);
        
	    // CollectionMemberId
	    preparation.setCollectionMemberId(collectionMemberId);
	    
	    // CountAmt: this needs to be set so that the create loan preparation logic works
	    preparation.setCountAmt(Integer.valueOf(1));

	    // Description
	    String prepMethod = getPrepMethod(specimenItem);
        preparation.setDescription(prepMethod);
        
	    // Number1 (specimen_item id TODO: temporary, remove after load
        preparation.setNumber1(specimenItemId + (float) 0.0);
        
	    // Number2 (replicates)
	    Integer replicates = specimenItem.getReplicates();
	    preparation.setNumber2((replicates == null ? 0 : replicates) + (float) 0.0);
	    
	    // PrepType
        PrepType prepType = getPrepType(specimenItem);
        preparation.setPrepType(prepType);
        
        // Remarks
        // ... note
        String note = specimenItem.getNote();
        
	    // ... distribution
	    String distribution = denormalize("distribution", specimenItem.getDistribution());
	            	    
        // ... provenance
        String provenance = denormalize("provenance", specimenItem.getProvenance());
        
        // ... voucher
        String voucher = denormalize("voucher", specimenItem.getVoucher());
        
        preparation.setRemarks(concatenate(note, distribution, provenance, voucher));
        
        // SampleNumber
        Integer itemNo = specimenItem.getItemNo();
        checkNull(itemNo, "item no");
        preparation.setSampleNumber(String.valueOf(itemNo));
                
        // Storage
        String location = specimenItem.getLocation();
        String locationStr = location == null ? "" : location.toLowerCase();
        
        Integer subcollectionId = specimenItem.getSubcollectionId();
        Storage storage = nullStorage;
        if (subcollectionId != null)
        {
            storage = lookupStorage(subcollectionId);
        }
        preparation.setStorage(storage);
        
        if (locationStr.contains("burt slide collection") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.BURT_SLIDE_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to burt slide collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("farlow") && locationStr.contains("slide collection") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.FARLOW_SLIDE_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to farlow slide collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("general fungus herbarium type collection") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.GEN_FUN_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to general fungus herbarium type collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("fruit collection") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.FRUIT_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to fruit collection based on location field: (" + location + ")");
        }
        else if ((locationStr.contains("glycerine") || locationStr.contains("in slide collection tray")) && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.GLYCERINE_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to glycerine collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("hymenomycete") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.HYMENO_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to hymenomycete collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("theissen") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.THEISSEN_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to theissen collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("trichomycete") && locationStr.contains("type") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.TRICHO_TYPE_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to trichomycete type collection based on location field: (" + location + ")");
        }
        else if (locationStr.contains("trichomycete slide collection") && subcollectionId == null)
        {
            storage = lookupStorage(SubcollectionLoader.TRICHO_SUBCOLL);
            preparation.setStorage(storage);
            getLogger().warn(rec() + "Moving to trichomycete collection based on location field: (" + location + ")");
        }
        
	    // StorageLocation (location/temp location)
        location = truncate(location, 50, "location");
        preparation.setStorageLocation(location);
        
        // Text1 (herbarium)
        String herbarium = specimenItem.getHerbariumCode();
        preparation.setText1(herbarium);
        
        // Text2 (reference)
        String reference = specimenItem.getReference();
        reference = truncate(reference, 300, "reference");
        preparation.setText2(reference);
        
	    // YesNo1 (isOversize)
        Boolean isOversize = specimenItem.isOversize();
        preparation.setYesNo1(isOversize);
        
        return preparation;
	}

	private Collection getCollection(SpecimenItem specimenItem) throws LocalException
	{
        String herbariumCode = specimenItem.getHerbariumCode();
        checkNull(herbariumCode, "herbrium code");
        
        Collection collection = new Collection();
        
        Integer collectionId = getCollectionId(herbariumCode);
        collection.setCollectionId(collectionId);
        
        return collection;
	}


	private CollectionObject getCollectionObject(SpecimenItem specimenItem,
	                                             Collection collection,
	                                             Container container,
	                                             CollectingEvent collectingEvent) throws LocalException
	{
		CollectionObject collectionObject = new CollectionObject();

		setAuditFields(specimenItem, collectionObject);

	    
        // AltCatalogNumber
        Integer specimenId = specimenItem.getSpecimenId();
        checkNull(specimenId, "specimen id");
        
        String altCatalogNumber = getAltCatalogNumber(specimenId);
        collectionObject.setAltCatalogNumber(altCatalogNumber);
        
        // Cataloger
        Agent cataloger = collectionObject.getCreatedByAgent();
        collectionObject.setCataloger(cataloger);
        
        // CatalogedDate
        Date catalogedDate = collectionObject.getTimestampCreated();
        collectionObject.setCatalogedDate(DateUtils.toCalendar(catalogedDate));

        // CatalogedDatePrecision
        Byte catalogedDatePrecision = DateUtils.getFullDatePrecision();
        collectionObject.setCatalogedDatePrecision(catalogedDatePrecision);

        // CatalogNumber
        Integer barcode = specimenItem.getBarcode();
        String catalogNumber = getPreparationLookup().formatCollObjBarcode(barcode);
        collectionObject.setCatalogNumber(catalogNumber);
        
        // CollectionMemberID
        Integer collectionId = collection.getId();
        collectionObject.setCollectionMemberId(collectionId);
        
        // Collection
        collectionObject.setCollection(collection);
        
        // CollectingEvent
        collectionObject.setCollectingEvent(collectingEvent);
		
        // Container
        collectionObject.setContainer(container);

        // CreatedByAgent
        collectionObject.setCreatedByAgent(cataloger); 
        
        // Description
        String description = specimenItem.getDescription();
        if (description != null)
        {
        	description = truncate(description, 255, "description");
            collectionObject.setDescription(description);
        }

        // FieldNumber
        String collectorNo = specimenItem.getCollectorNo();
        if (collectorNo != null)
        {
        	collectorNo = truncate(collectorNo, 50, "collector number");
            collectionObject.setFieldNumber(collectorNo);
        }
        
        // Remarks
        // ... remarks
        String remarks = specimenItem.getRemarks();
        
        // ... vernacular name
        String vernacularName = denormalize("vernacular name", specimenItem.getVernacularName());
        
        // ... host
        String host = null;
        String habitat = specimenItem.getHabitat();
        if (habitat != null)
        {
            int i =  habitat.toLowerCase().indexOf("host:");
            if (i >= 0 && i+5 < habitat.length())
            {
                host = denormalize("host", habitat.substring(i+5).trim());
            }
        }
        
        // ... substrate
        String substrate = denormalize("substrate", specimenItem.getSubstrate());
        
        collectionObject.setRemarks(concatenate(remarks, vernacularName, host, substrate));
        
        // Text1 (repro status)
	    REPRO_STATUS reproStatus = specimenItem.getReproStatus();
        if (reproStatus != null) collectionObject.setText1(SpecimenItem.toString(reproStatus));
        
        // Text2 (sex)
        String sex = specimenItem.getSex();
        collectionObject.setText2(sex);
        
        // YesNo1 (isCultivated)
        collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement cultivated specimens

        return collectionObject;
	}
    
	private CollectionObject getCollectionObject(SpecimenItem specimenItem, Collection collection) throws LocalException
	{
		Division division = this.getBotanyDivision();
		Integer collectionId = collection.getCollectionId();

		// Container
		Container container = getContainer(specimenItem, collectionId, CONTAINER_TYPE.Logical.ordinal());
		if (container != null)
		{
			if (container.getId() == null)
			{
				String sql = getInsertSql(container);
				Integer containerId = insert(sql);
				container.setContainerId(containerId);
			}
		}
		else
		{
			container = this.nullContainer;
		}

		// CollectingTrip
		CollectingTrip collectingTrip = getCollectingTrip(specimenItem, getBotanyDiscipline());
		if (collectingTrip != null)
		{
			if (collectingTrip.getId() == null)
			{
				String sql = getInsertSql(collectingTrip);
				Integer collectingTripId = insert(sql);
				collectingTrip.setCollectingTripId(collectingTripId);
			}
		}
		else
		{
			collectingTrip = this.nullCollectingTrip;
		}

		// CollectingEvent
		CollectingEvent collectingEvent = getCollectingEvent(specimenItem, collectingTrip, getBotanyDiscipline());
		if (collectingEvent.getId() == null)
		{
			String sql = getInsertSql(collectingEvent);
			Integer collectingEventId = insert(sql);
			collectingEvent.setCollectingEventId(collectingEventId);
		}

		// CollectionObject
		CollectionObject collectionObject = getCollectionObject(specimenItem, collection, container, collectingEvent);
		if (collectionObject.getId() == null)
		{
			String sql = getInsertSql(collectionObject);
			Integer collectionObjectId = insert(sql);
			collectionObject.setCollectionObjectId(collectionObjectId);
		}

		// Collector
		Collector collector = getCollector(specimenItem, collectingEvent, division);
		if (collector != null)
		{
			if (collector.getId() == null)
			{
				String sql = getInsertSql(collector);
				Integer collectorId = insert(sql);
				collector.setCollectorId(collectorId);
			}
		}

		// Series collector
		// Collector seriesCollector = getSeriesCollector(specimenItem, collectingEvent, division);
		// it's here if we want to use it.
	    
	    return collectionObject;
	}
	
    private CollectingTrip getCollectingTrip(SpecimenItem specimenItem, Discipline discipline) throws LocalException
    {
        if (specimenItem.hasCollectingTrip())
        {
            String container = specimenItem.getContainer();
            checkNull(container, "container");
            
            String collectingTripName = truncate(container, 64, "collecting trip name");
            
            CollectingTrip collectingTrip = lookupCollectingTrip(collectingTripName);

            if (collectingTrip == null)
            {          
                collectingTrip = new CollectingTrip();

                // CollectingTripName
                collectingTrip.setCollectingTripName(collectingTripName);

                // Discipline
                collectingTrip.setDiscipline(discipline);

                // Remarks
                collectingTrip.setRemarks(container);
            }
            return collectingTrip;
        }
        
        return null;
    }
    

	private Container getContainer(SpecimenItem specimenItem, Integer collectionMemberId, Integer type) throws LocalException
	{
	    String containerStr = specimenItem.getContainer();

        if (containerStr != null && !specimenItem.hasCollectingTrip()) // if it has a collecting trip, that will be saved elsewhere
        {
            // TODO: normalizing of container and subcollection name strings.
            // Note that if the container string = subcollection.name for some subcollection (it does happen),
            // we are in effect adding that subcollection_id to the specimen_item record
            
            Container container = lookupContainer(containerStr);
            
            // new Container
            if (container == null)
            {
                container = new Container();
                
                // CollectionMemberId
                container.setCollectionMemberId(collectionMemberId);

                // Name
                containerStr = truncate(containerStr, 64, "container name");
                container.setName(containerStr);
                
                // Type
                container.setType(type.shortValue());
            }
            
            return container;
        }

        return null;
	}
	
	
	private OtherIdentifier getSeriesIdentifier(SpecimenItem specimenItem, CollectionObject collectionObject, Integer collectionMemberId)
		throws LocalException
	{
        String seriesName = specimenItem.getSeriesName();
        String seriesNo = specimenItem.getSeriesNo();
        String seriesAbbrev = specimenItem.getSeriesAbbrev();

        if (seriesName == null || seriesNo == null)
        {
            if (seriesNo != null)
            {
                getLogger().warn(rec() + "Series number and no series id: " + seriesNo);
            }
            else if (seriesName != null)
            {
                getLogger().warn(rec() + "Series id and no series number: "+ seriesName);
            }
            
            return null;
        }

        OtherIdentifier series = new OtherIdentifier();
        
        // CollectionMemberID
        series.setCollectionMemberId(collectionMemberId);
        
        // CollectionObject
        series.setCollectionObject(collectionObject);
        
        // Institution (series-organization)
        series.setInstitution(seriesName);

        // Identifier (seriesNo)
        series.setIdentifier( (seriesAbbrev == null ? "" : seriesAbbrev + " ") + seriesNo );

        // Remarks
        series.setRemarks("series");

        return series;
	}
	
	
	private OtherIdentifier getAccessionIdentifier(SpecimenItem specimenItem, CollectionObject collectionObject, Integer collectionMemberId)
	{
	    // AccessionNumber
	    String accessionNumber = specimenItem.getAccessionNo();
	    if (accessionNumber == null || accessionNumber.toLowerCase().equals("na")) return null;
		
	    accessionNumber = truncate(accessionNumber, 64, "accession no");
	    	    
	    OtherIdentifier accession = new OtherIdentifier();
        
        // CollectionMemberID
	    accession.setCollectionMemberId(collectionMemberId);
        
        // CollectionObject
	    accession.setCollectionObject(collectionObject);
        
        // Institution

        // Identifier
	    accession.setIdentifier(accessionNumber);

        // Remarks
	    accession.setRemarks("accession");

        return accession;
	}
	

	private CollectionObjectCitation getExsiccataItem(SpecimenItem specimenItem, CollectionObject collObj, Integer collectionId) throws LocalException
	{
	    Integer subcollectionId = specimenItem.getSubcollectionId();

	    ReferenceWork exsiccata = lookupExsiccata(subcollectionId);
	    if (exsiccata == null) return null;

	    CollectionObjectCitation exsiccataItem = new CollectionObjectCitation();
        
	    // CollectionMemberID
	    exsiccataItem.setCollectionMemberId(collectionId);

	    // Fragment
        exsiccataItem.setCollectionObject(collObj);
        
        // Exsiccata
        exsiccataItem.setReferenceWork(exsiccata);

	    return exsiccataItem;
	}
	
	
	private String getInsertSql(CollectingEvent collectingEvent) throws LocalException
    {
        String fieldNames = "CollectingTripID, DisciplineID, EndDate, EndDatePrecision, EndDateVerbatim, " +
        		            "LocalityID, Remarks, StartDate, StartDatePrecision, StartDateVerbatim, " +
                            "StationFieldNumber, TimestampCreated, VerbatimDate, Version";

        String[] values = {        
        		SqlUtils.sqlString( collectingEvent.getCollectingTrip().getId()),
        		SqlUtils.sqlString( collectingEvent.getDiscipline().getDisciplineId()),
        		SqlUtils.sqlString( collectingEvent.getEndDate()),
        		SqlUtils.sqlString( collectingEvent.getEndDatePrecision()),
        		SqlUtils.sqlString( collectingEvent.getEndDateVerbatim()),
        		SqlUtils.sqlString( collectingEvent.getLocality().getLocalityId()),
        		SqlUtils.sqlString( collectingEvent.getRemarks()),
        		SqlUtils.sqlString( collectingEvent.getStartDate()),
        		SqlUtils.sqlString( collectingEvent.getStartDatePrecision()),
        		SqlUtils.sqlString( collectingEvent.getStartDateVerbatim()),
        		SqlUtils.sqlString( collectingEvent.getStationFieldNumber()),
        		SqlUtils.now(),
        		SqlUtils.sqlString( collectingEvent.getVerbatimDate()),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("collectingevent", fieldNames, values);
    }
	

    private String getInsertSql(Collector collector)
    {
        String fieldNames = "AgentID, CollectingEventID, DivisionID, IsPrimary, OrderNumber, " +
        		            "TimestampCreated, Version";
        
        String[] values = {
        		SqlUtils.sqlString( collector.getAgent().getAgentId()),
        		SqlUtils.sqlString( collector.getCollectingEvent().getId()),
        		SqlUtils.sqlString( collector.getDivision().getId()),
        		SqlUtils.sqlString( collector.getIsPrimary()),
        		SqlUtils.sqlString( collector.getOrderNumber()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("collector", fieldNames, values);
    }
    
    
    private String getInsertSql(CollectionObject collectionObject) throws LocalException
	{
		String fieldNames = "AltCatalogNumber, CatalogerID, CatalogedDate, CatalogedDatePrecision, CatalogNumber, " +
							"CollectionID, CollectionMemberID, CollectingEventID, ContainerID, CreatedByAgentID, " +
							"Description, FieldNumber, ModifiedByAgentID, Remarks, Text1, Text2, " +
							"TimestampCreated, TimestampModified, Version, YesNo1";

		String[] values = {		
				SqlUtils.sqlString( collectionObject.getAltCatalogNumber()),
				SqlUtils.sqlString( collectionObject.getCataloger().getAgentId()),
				SqlUtils.sqlString( collectionObject.getCatalogedDate()),
				SqlUtils.sqlString( collectionObject.getCatalogedDatePrecision()),
				SqlUtils.sqlString( collectionObject.getCatalogNumber()),
				SqlUtils.sqlString( collectionObject.getCollection().getId()),
				SqlUtils.sqlString( collectionObject.getCollectionMemberId()),
				SqlUtils.sqlString( collectionObject.getCollectingEvent().getId()),
				SqlUtils.sqlString( collectionObject.getContainer().getId()),
				SqlUtils.sqlString( collectionObject.getCreatedByAgent().getId()),
				SqlUtils.sqlString( collectionObject.getDescription()),
				SqlUtils.sqlString( collectionObject.getFieldNumber()),
				SqlUtils.sqlString( collectionObject.getModifiedByAgent().getId()),
				SqlUtils.sqlString( collectionObject.getRemarks()),
				SqlUtils.sqlString( collectionObject.getText1()),
				SqlUtils.sqlString( collectionObject.getText2()),
				SqlUtils.sqlString( collectionObject.getTimestampCreated()),
				SqlUtils.sqlString( collectionObject.getTimestampModified()),
				SqlUtils.one(),
				SqlUtils.sqlString( collectionObject.getYesNo1())
		};		

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}

    protected String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, CountAmt, Description, Number1, Number2, " +
							"PrepTypeID, Remarks, SampleNumber, StorageID, StorageLocation, Text1, " +
				            "Text2, TimestampCreated, Version, YesNo1";

		String[] values = {
				SqlUtils.sqlString( preparation.getCollectionMemberId()),
				SqlUtils.sqlString( preparation.getCollectionObject().getId()),
				SqlUtils.sqlString( preparation.getCountAmt()),
				SqlUtils.sqlString( preparation.getDescription()),
				SqlUtils.sqlString( preparation.getNumber1()),
				SqlUtils.sqlString( preparation.getNumber2()),
				SqlUtils.sqlString( preparation.getPrepType().getId()),
				SqlUtils.sqlString( preparation.getRemarks()),
				SqlUtils.sqlString( preparation.getSampleNumber()),
				SqlUtils.sqlString( preparation.getStorage().getId()),
				SqlUtils.sqlString( preparation.getStorageLocation()),
				SqlUtils.sqlString( preparation.getText1()),
				SqlUtils.sqlString( preparation.getText2()),
				SqlUtils.now(),
				SqlUtils.one(),
				SqlUtils.sqlString( preparation.getYesNo1())
		};
        
		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}
    
    private String getInsertSql(CollectionObjectCitation exsiccataItem) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, ReferenceWorkID, TimestampCreated, Version";

		String[] values = {
				SqlUtils.sqlString( exsiccataItem.getCollectionMemberId()),
				SqlUtils.sqlString( exsiccataItem.getCollectionObject().getId()),
				SqlUtils.sqlString( exsiccataItem.getReferenceWork().getId()),
				SqlUtils.now(),
				SqlUtils.one()
		};
		
		return SqlUtils.getInsertSql("collectionobjectcitation", fieldNames, values);
	}
    
    private String getInsertSql(OtherIdentifier otherIdentifier)
    {
        String fieldNames = "CollectionMemberID, CollectionObjectID, Identifier, Institution, " +
        		            "Remarks, TimestampCreated, Version";
        
        String[] values = {        
        		SqlUtils.sqlString( otherIdentifier.getCollectionMemberId()),
        		SqlUtils.sqlString( otherIdentifier.getCollectionObject().getId()),
        		SqlUtils.sqlString( otherIdentifier.getIdentifier()),
        		SqlUtils.sqlString( otherIdentifier.getInstitution()),
        		SqlUtils.sqlString( otherIdentifier.getRemarks()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("otheridentifier", fieldNames, values);
    }
    
    private String getInsertSql(Container container) throws LocalException
    {
        String fieldNames = "CollectionMemberID, Name, Number, TimestampCreated, Type, Version";
        
        String[] values = {
        		SqlUtils.sqlString( container.getCollectionMemberId()),
        		SqlUtils.sqlString( container.getName()),
        		SqlUtils.sqlString( container.getNumber()),
        		SqlUtils.now(),
        		SqlUtils.sqlString( container.getType()),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("container", fieldNames, values);
    }

    private String getInsertSql(CollectingTrip collectingTrip)
    {
        String fieldNames = "CollectingTripName, DisciplineID, Remarks, TimestampCreated, Version";
        
        String[] values = {        
        		SqlUtils.sqlString( collectingTrip.getCollectingTripName()),
        		SqlUtils.sqlString( collectingTrip.getDiscipline().getId()),
        		SqlUtils.sqlString( collectingTrip.getRemarks()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("collectingtrip", fieldNames, values);
    }
    
    private String getAltCatalogNumber(Integer specimenId)
    {
    	return String.valueOf(specimenId);
    }
    
    private String getPrepMethod(SpecimenItem specimenItem) throws LocalException
	{
        FORMAT format = specimenItem.getFormat();
        if (format == null) return null;
        
        String location = specimenItem.getLocation() != null ? specimenItem.getLocation().toLowerCase() : null;
        
        PREP_METHOD prepMethod = null;
        if      (format.equals(FORMAT.OnSheet))           prepMethod = PREP_METHOD.Pressed;
        else if (format.equals(FORMAT.InPacket))          prepMethod = PREP_METHOD.Dried;
        else if (format.equals(FORMAT.InBox))             prepMethod = PREP_METHOD.Other;
        else if (format.equals(FORMAT.InBag))             prepMethod = PREP_METHOD.Dried;
        else if (format.equals(FORMAT.InJar))             prepMethod = PREP_METHOD.SpiritMedium;
        else if (format.equals(FORMAT.InSpiritMedium))    prepMethod = PREP_METHOD.SpiritMedium;
        else if (format.equals(FORMAT.Wood))              prepMethod = PREP_METHOD.Wood;
        else if (format.equals(FORMAT.Fossil))            prepMethod = PREP_METHOD.Fossil;
        else if (format.equals(FORMAT.OnMicroscopeSlide))
        {
            if (location != null && location.contains("glycerine")) prepMethod = PREP_METHOD.Glycerine;
            else prepMethod = PREP_METHOD.Other;
        }
        else if (format.equals(FORMAT.DNAsample))         prepMethod = PREP_METHOD.Other;
        else if (format.equals(FORMAT.Photograph))        prepMethod = PREP_METHOD.Photograph;
        else if (format.equals(FORMAT.Drawing))           prepMethod = PREP_METHOD.Drawing;
        else if (format.equals(FORMAT.ProtologOnSheet))   prepMethod = PREP_METHOD.Protolog;
        else if (location != null && location.contains("35mm kodachrome slide"))
        {
            prepMethod = PREP_METHOD.Photograph;
        }
        else                                              prepMethod = PREP_METHOD.Other;

        return prepMethod.name();
	}
    
    private PrepType getPrepType(SpecimenItem specimenItem) throws LocalException
    {
        FORMAT format = specimenItem.getFormat();
        Integer collectionId = getCollection(specimenItem).getCollectionId();
        String location = specimenItem.getLocation() != null ? specimenItem.getLocation().toLowerCase() : null;
        
        CONTAINER_TYPE container = null;
        
        if      (format.equals(FORMAT.OnSheet))           container = CONTAINER_TYPE.Sheet;
        else if (format.equals(FORMAT.InPacket))          container = CONTAINER_TYPE.Packet;
        else if (format.equals(FORMAT.InBox))             container = CONTAINER_TYPE.Box;
        else if (format.equals(FORMAT.InBag))             container = CONTAINER_TYPE.Bag;
        else if (format.equals(FORMAT.InJar))             container = CONTAINER_TYPE.Jar;
        else if (format.equals(FORMAT.InSpiritMedium))    container = CONTAINER_TYPE.Jar;
        else if (format.equals(FORMAT.Wood))              container = CONTAINER_TYPE.Self;
        else if (format.equals(FORMAT.Fossil))            container = CONTAINER_TYPE.Self;
        else if (format.equals(FORMAT.OnMicroscopeSlide)) container = CONTAINER_TYPE.MicroscopeSlide;
        else                                              container = CONTAINER_TYPE.Other;

        if (location != null && location.contains("35mm kodachrome slide")) container = CONTAINER_TYPE.Slide35mm;

        String key = SpecimenItem.toString(container) + " " + String.valueOf(collectionId);
        
        PrepType prepType = prepTypesByFormatAndColl.get(key);
        
        if (prepType == null)
        {
            String sql = "select PrepTypeID from preptype where CollectionID=" + collectionId + " and Name=" + SqlUtils.sqlString(SpecimenItem.toString(container));
            Integer prepTypeId = queryForInt(sql);
            if (prepTypeId == null) throw new LocalException("Couldn't find prep type for " + key);
            
            prepType = new PrepType();
            prepType.setPrepTypeId(prepTypeId);
            prepTypesByFormatAndColl.put(key, prepType);
        }

        return prepType;
    }
}
