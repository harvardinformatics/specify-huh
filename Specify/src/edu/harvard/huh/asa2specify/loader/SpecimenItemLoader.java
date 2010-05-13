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
import edu.harvard.huh.asa2specify.AsaIdMapper;
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
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Container;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.FragmentCitation;
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
	private AsaIdMapper specimenIdBarcodes;
	
	// These objects are all related to the collection object
	// and need the collection object to be saved first
	
	private final Container      nullContainer      = new Container();
	private final CollectingTrip nullCollectingTrip = new CollectingTrip();
	private final Preparation    nullPreparation    = new Preparation();
    private final Storage        nullStorage        = new Storage();
    
	private int nextCatalogNumber = 1;
	
	private CollectionObject collectionObject;

	public SpecimenItemLoader(File csvFile,
	                          Statement sqlStatement,
	                          File seriesBotanists,
	                          File specimenIdBarcodes,
	                          BotanistLookup botanistLookup,
	                          SubcollectionLookup subcollLookup,
	                          SeriesLookup seriesLookup,
	                          SiteLookup siteLookup) throws LocalException 
	{
		super(csvFile, sqlStatement);

		this.specimenIdBarcodes = new AsaIdMapper(specimenIdBarcodes);
		
		this.prepTypesByFormatAndColl = new Hashtable<String, PrepType>();
		
		this.botanistLookup  = botanistLookup;
		this.subcollLookup   = subcollLookup;
		this.siteLookup      = siteLookup;
        this.seriesLookup    = seriesLookup;
        this.containerLookup = getContainerLookup();
		this.prepLookup      = getPreparationLookup();
		this.collTripLookup  = getCollectingTripLookup();
		
		this.collectionObject = new CollectionObject();
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

		    Preparation preparation = nullPreparation;

		    // Preparation
		    preparation = getPreparation(specimenItem, collectionId);
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
		    
		    // Fragment
		    Fragment fragment = getFragment(specimenItem, collectionObject, preparation, collectionId);
		    if (fragment.getId() == null)
		    {
		        String sql = getInsertSql(fragment);
		        Integer fragmentId = insert(sql);
		        fragment.setFragmentId(fragmentId);
		    }

		    // ExsiccataItem
		    FragmentCitation exsiccataItem = getExsiccataItem(specimenItem, fragment, collectionId);
		    if (exsiccataItem != null)
		    {
		        if (exsiccataItem.getId() == null)
		        {
		            String sql = getInsertSql(exsiccataItem);
		            Integer exsiccataItemId = insert(sql);
		            exsiccataItem.setFragmentCitationId(exsiccataItemId);
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
                
                public Fragment getById(Integer specimenId) throws LocalException
                {
                    Fragment fragment = new Fragment();
                    
                    String altCatalogNumber = getAltCatalogNumber(specimenId);

                    String sql = "select f.FragmentID from fragment f left join collectionobject co " +
                    "on f.CollectionObjectID=co.CollectionObjectID left join preparation p " +
                    "on f.PreparationID=p.PreparationID where co.AltCatalogNumber=\"" + altCatalogNumber + "\" " +
                    "order by co.CollectionObjectID, p.SampleNumber limit 1";

                    Integer fragmentId = getInt(sql);
                    
                    fragment.setFragmentId(fragmentId);
                    
                    return fragment;
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
                    String sql = "select p.PreparationID from preparation p left join " +
                                 "fragment f on p.PreparationID=f.PreparationID " +
                                 "where f.Identifier=\"" + barcode + "\"";
                    
                    Integer preparationId = getInt(sql);
                    
                    Preparation preparation = new Preparation();
                    
                    preparation.setPreparationId(preparationId);
                    
                    return preparation;
                }

                @Override
                public Preparation getBySpecimenItemId(Integer specimenItemId) throws LocalException
                {
                    Preparation preparation = new Preparation();
                    
                    Integer preparationId = getInt("preparation", "PreparationID", "Text1", specimenItemId);
                    
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
    
    private Integer lookupBarcode(Integer specimenItemId)
    {
        return specimenIdBarcodes.map(specimenItemId);
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

	private String getAltCatalogNumber(Integer specimenId)
	{
		return String.valueOf(specimenId);
	}

	private Collector getCollector(SpecimenItem specimenItem, CollectingEvent collectingEvent, Integer collectionMemberId) throws LocalException
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
        collector.setCollectionMemberId(collectionMemberId);
        
        // IsPrimary
        collector.setIsPrimary(true);

        // OrderNumber
        collector.setOrderNumber(1);
        
        return collector;
	}
	
	private Collector getSeriesCollector(SpecimenItem specimenItem, CollectingEvent collectingEvent, Integer collectionMemberId) throws LocalException
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
        collector.setCollectionMemberId(collectionMemberId);
        
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
		// EndDateVerbatim
		else if ( DateUtils.isValidCollectionDate( endYear, endMonth, endDay ) )
		{
			String endDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
			if (endDateVerbatim != null && endDateVerbatim.length() > 50)
			{
				endDateVerbatim = truncate(endDateVerbatim, 50, "end date verbatim");
				collectingEvent.setEndDateVerbatim(endDateVerbatim);
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
	    
	    // VerbatimDate
	    String verbatimDate = bdate.getText();
	    if (verbatimDate!= null) verbatimDate = truncate(verbatimDate, 50, "date text");
	    collectingEvent.setVerbatimDate(verbatimDate);
	        
	    return collectingEvent;
	}
	
	private Preparation getPreparation(SpecimenItem specimenItem, Integer collectionMemberId)
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
        
	    // CollectionMemberId
	    preparation.setCollectionMemberId(collectionMemberId);
	    
	    // CountAmt: this needs to be set so that the create loan preparation logic works
	    preparation.setCountAmt(Integer.valueOf(1));

	    // PrepType
        PrepType prepType = getPrepType(specimenItem);
        preparation.setPrepType(prepType);
        
        // Remarks 
        
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
        
        if (locationStr.contains("burt slide collection") && subcollectionId != null)
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
        
        // Text1 (specimen_item id TODO: temporary, remove after load
        preparation.setText1(String.valueOf(specimenItemId));
        
	    // YesNo1 (isOversize)
        Boolean isOversize = specimenItem.isOversize();
        preparation.setYesNo1(isOversize);
        
        return preparation;
	}
	
	private Fragment getFragment(SpecimenItem specimenItem,
	                             CollectionObject collectionObject,
	                             Preparation preparation,
	                             Integer collectionMemberId) throws LocalException
	{
	    Fragment fragment = new Fragment();
	    
	    // AccessionNumber
	    String accessionNumber = specimenItem.getAccessionNo();
	    accessionNumber = truncate(accessionNumber, 32, "accession no");
	    if (accessionNumber != null && ! accessionNumber.toLowerCase().equals("na"))
	    {
	        fragment.setAccessionNumber(accessionNumber);
	    }
	    	    
	    // CollectionMemberID
	    fragment.setCollectionMemberId(collectionMemberId);

	    // CollectionObjectID
	    fragment.setCollectionObject(collectionObject);

	    // Description
	    
	    // Distribution
	    String distribution = specimenItem.getDistribution();
	    distribution = truncate(distribution, 100, "distribution");
	    fragment.setDistribution(distribution);
	    
	    // Identifier (barcode)
        Integer barcode = specimenItem.getBarcode();
        String identifier = getPreparationLookup().formatCollObjBarcode(barcode);
        
        if (identifier == null) getLogger().warn(rec() + "Null barcode");

        fragment.setIdentifier(identifier);
        
	    // Number1 (replicates)
	    Integer replicates = specimenItem.getReplicates();
	    fragment.setNumber1(replicates);
	    
	    // Phenology
	    REPRO_STATUS reproStatus = specimenItem.getReproStatus();
        if (reproStatus != null) fragment.setPhenology(SpecimenItem.toString(reproStatus));
        
        // Preparation
        fragment.setPreparation(preparation != null ? preparation : nullPreparation);

        // PrepMethod
        String prepMethod = getPrepMethod(specimenItem);
        fragment.setPrepMethod(prepMethod);

        // Provenance
        String provenance = specimenItem.getProvenance();
        provenance = truncate(provenance, 255, "provenance");
        fragment.setProvenance(provenance);
        
        // Remarks
        String note = specimenItem.getNote();
        fragment.setRemarks(note);
        
        // Sex
        String sex = specimenItem.getSex();
        fragment.setSex(sex);
        
        // Text1 (herbarium)
        String herbarium = specimenItem.getHerbariumCode();
        fragment.setText1(herbarium);
        
        // Text2 (reference)
        String reference = specimenItem.getReference();
        reference = truncate(reference, 300, "reference");
        fragment.setText2(reference);
        
        // Voucher
        String voucher = specimenItem.getVoucher();
        voucher = truncate(voucher, 255, "voucher");
        fragment.setVoucher(voucher);

        return fragment;
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
        
        Agent cataloger = collectionObject.getCreatedByAgent();
        collectionObject.setCataloger(cataloger);
        
        // CatalogedDate
        Date catalogedDate = collectionObject.getTimestampCreated();
        collectionObject.setCatalogedDate(DateUtils.toCalendar(catalogedDate));

        // CatalogedDatePrecision
        Byte catalogedDatePrecision = DateUtils.getFullDatePrecision();
        collectionObject.setCatalogedDatePrecision(catalogedDatePrecision);

        // CatalogNumber
        //String catalogNumber = getPreparationLookup().formatCollObjBarcode(nextCatalogNumber());
        //collectionObject.setCatalogNumber(catalogNumber);
        
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
        	description = truncate(description, 1024, "description");
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
        String remarks = specimenItem.getRemarks();
        collectionObject.setRemarks(remarks);
        
        // Text1 (host)
        String habitat = specimenItem.getHabitat();
        if (habitat != null)
        {
            int i =  habitat.toLowerCase().indexOf("host:");
            if (i >= 0 && i+5 < habitat.length())
            {
                String host = habitat.substring(i+5).trim();
                host = truncate(host, 300, "host");
                collectionObject.setText1(host);
            }
        }
        
        // Text2 (substrate)
        String substrate = specimenItem.getSubstrate();
        substrate = truncate(substrate, 300, "substrate");
        collectionObject.setText2(substrate);
        
        // Text3 (vernacular name)
        String vernacularName = specimenItem.getVernacularName();
        vernacularName = truncate(vernacularName, 300, "vernacular name");
        collectionObject.setText3(vernacularName);
        
        // YesNo1 (isCultivated)
        collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement cultivated specimens

        return collectionObject;
	}
    
	private CollectionObject getCollectionObject(SpecimenItem specimenItem, Collection collection) throws LocalException
	{
	    if (String.valueOf(specimenItem.getSpecimenId()).equals(this.collectionObject.getAltCatalogNumber()))
	    {
	        return this.collectionObject;
	    }
	    else
	    {
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
                this.collectionObject = collectionObject;
            }
            
            // Collector
            Collector collector = getCollector(specimenItem, collectingEvent, collectionId);
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
            Collector seriesCollector = getSeriesCollector(specimenItem, collectingEvent, collectionId);
            // it's here if we want to use it.
	    }
	    
	    return this.collectionObject;
	}
	
    private CollectingTrip getCollectingTrip(SpecimenItem specimenItem, Discipline discipline) throws LocalException
    {
        if (specimenItem.hasCollectingTrip())
        {
            String container = specimenItem.getContainer();
            checkNull(container, "container");
            
            String collectingTripName = truncate(container, 200, "collecting trip name");
            
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
    	
	private int nextCatalogNumber()
	{
	    return nextCatalogNumber++;
	}

	private Container getContainer(SpecimenItem specimenItem, Integer collectionMemberId, Integer type) throws LocalException
	{
	    String containerStr = specimenItem.getContainer();

        if (containerStr != null)
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
                containerStr = truncate(containerStr, 200, "container name");
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

	private FragmentCitation getExsiccataItem(SpecimenItem specimenItem, Fragment fragment, Integer collectionId) throws LocalException
	{
	    Integer subcollectionId = specimenItem.getSubcollectionId();

	    ReferenceWork exsiccata = lookupExsiccata(subcollectionId);
	    if (exsiccata == null) return null;

	    FragmentCitation exsiccataItem = new FragmentCitation();
        
	    // CollectionMemberID
	    exsiccataItem.setCollectionMemberId(collectionId);

	    // Fragment
        exsiccataItem.setFragment(fragment);
        
        // Exsiccata
        exsiccataItem.setReferenceWork(exsiccata);

	    return exsiccataItem;
	}
	
	private String getInsertSql(CollectingEvent collectingEvent) throws LocalException
    {
        String fieldNames = "CollectingTripID, DisciplineID, EndDate, EndDatePrecision, EndDateVerbatim, " +
        		            "LocalityID, Remarks, StartDate, StartDatePrecision, StartDateVerbatim, " +
                            "TimestampCreated, VerbatimDate, Version";

        String[] values = new String[13];
        
        values[0]  = SqlUtils.sqlString( collectingEvent.getCollectingTrip().getId());
        values[1]  = SqlUtils.sqlString( collectingEvent.getDiscipline().getDisciplineId());
        values[2]  = SqlUtils.sqlString( collectingEvent.getEndDate());
        values[3]  = SqlUtils.sqlString( collectingEvent.getEndDatePrecision());
        values[4]  = SqlUtils.sqlString( collectingEvent.getEndDateVerbatim());
        values[5]  = SqlUtils.sqlString( collectingEvent.getLocality().getLocalityId());
        values[6]  = SqlUtils.sqlString( collectingEvent.getRemarks());
        values[7]  = SqlUtils.sqlString( collectingEvent.getStartDate());
        values[8]  = SqlUtils.sqlString( collectingEvent.getStartDatePrecision());
        values[9]  = SqlUtils.sqlString( collectingEvent.getStartDateVerbatim());
        values[10] = SqlUtils.now();
        values[11] = SqlUtils.sqlString( collectingEvent.getVerbatimDate());
        values[12] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("collectingevent", fieldNames, values);
    }

    private String getInsertSql(Collector collector)
    {
        String fieldNames = "AgentID, CollectingEventID, CollectionMemberID, IsPrimary, OrderNumber, " +
        		            "TimestampCreated, Version";
        
        String[] values = new String[7];
        
        values[0] = SqlUtils.sqlString( collector.getAgent().getAgentId());
        values[1] = SqlUtils.sqlString( collector.getCollectingEvent().getId());
        values[2] = SqlUtils.sqlString( collector.getCollectionMemberId());
        values[3] = SqlUtils.sqlString( collector.getIsPrimary());
        values[4] = SqlUtils.sqlString( collector.getOrderNumber());
        values[5] = SqlUtils.now();
        values[6] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("collector", fieldNames, values);
    }
    
    private String getInsertSql(CollectionObject collectionObject) throws LocalException
	{
		String fieldNames = "AltCatalogNumber, CatalogerID, CatalogedDate, CatalogedDatePrecision, CatalogNumber, " +
							"CollectionID, CollectionMemberID, CollectingEventID, CreatedByAgentID, Description, " +
							"FieldNumber, ModifiedByAgentID, Remarks, Text1, Text2, Text3, TimestampCreated, " +
							"TimestampModified, Version, YesNo1";

		String[] values = new String[20];
		
		values[0]  = SqlUtils.sqlString( collectionObject.getAltCatalogNumber());
		values[1]  = SqlUtils.sqlString( collectionObject.getCataloger().getAgentId());
		values[2]  = SqlUtils.sqlString( collectionObject.getCatalogedDate());
		values[3]  = SqlUtils.sqlString( collectionObject.getCatalogedDatePrecision());
		values[4]  = SqlUtils.sqlString( collectionObject.getCatalogNumber());
		values[5]  = SqlUtils.sqlString( collectionObject.getCollection().getId());
		values[6]  = SqlUtils.sqlString( collectionObject.getCollectionMemberId());
		values[7]  = SqlUtils.sqlString( collectionObject.getCollectingEvent().getId());
		values[8]  = SqlUtils.sqlString( collectionObject.getCreatedByAgent().getId());
		values[9]  = SqlUtils.sqlString( collectionObject.getDescription());
		values[10] = SqlUtils.sqlString( collectionObject.getFieldNumber());
		values[11] = SqlUtils.sqlString( collectionObject.getModifiedByAgent().getId());
		values[12] = SqlUtils.sqlString( collectionObject.getRemarks());
		values[13] = SqlUtils.sqlString( collectionObject.getText1());
		values[14] = SqlUtils.sqlString( collectionObject.getText2());
		values[15] = SqlUtils.sqlString( collectionObject.getText3());
		values[16] = SqlUtils.sqlString( collectionObject.getTimestampCreated());
		values[17] = SqlUtils.sqlString( collectionObject.getTimestampModified());
        values[18] = SqlUtils.one();
		values[19] = SqlUtils.sqlString( collectionObject.getYesNo1());		

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}
    
    private String getInsertSql(Fragment fragment) throws LocalException
    {
        String fieldNames = "AccessionNumber, CollectionMemberID, CollectionObjectID, Distribution, " +
                            "Identifier, Number1, Phenology, PreparationID, PrepMethod, Provenance, " +
                            "Sex, Remarks, Text1, Text2, TimestampCreated, Version";
        
        String[] values = new String[16];
        
        values[0]  = SqlUtils.sqlString( fragment.getAccessionNumber());
        values[1]  = SqlUtils.sqlString( fragment.getCollectionMemberId());
        values[2]  = SqlUtils.sqlString( fragment.getCollectionObject().getId());
        values[3]  = SqlUtils.sqlString( fragment.getDistribution());
        values[4]  = SqlUtils.sqlString( fragment.getIdentifier());
        values[5]  = SqlUtils.sqlString( fragment.getNumber1());
        values[6]  = SqlUtils.sqlString( fragment.getPhenology());
        values[7]  = SqlUtils.sqlString( fragment.getPreparation().getId());
        values[8]  = SqlUtils.sqlString( fragment.getPrepMethod());
        values[9]  = SqlUtils.sqlString( fragment.getProvenance());
        values[10] = SqlUtils.sqlString( fragment.getSex());
        values[11] = SqlUtils.sqlString( fragment.getRemarks());
        values[12] = SqlUtils.sqlString( fragment.getText1());
        values[13] = SqlUtils.sqlString( fragment.getText2());
        values[14] = SqlUtils.now();
        values[15] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("fragment", fieldNames, values);
    }

    private String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CountAmt, PrepTypeID, Remarks, " +
				            "SampleNumber, StorageID, StorageLocation, Text1, " +
				            "TimestampCreated, Version, YesNo1";

		String[] values = new String[11];
		
		values[0]  = SqlUtils.sqlString( preparation.getCollectionMemberId());
		values[1]  = SqlUtils.sqlString( preparation.getCountAmt());
        values[2]  = SqlUtils.sqlString( preparation.getPrepType().getId());
		values[3]  = SqlUtils.sqlString( preparation.getRemarks());
		values[4]  = SqlUtils.sqlString( preparation.getSampleNumber());
		values[5]  = SqlUtils.sqlString( preparation.getStorage().getId());
		values[6]  = SqlUtils.sqlString( preparation.getStorageLocation());
		values[7]  = SqlUtils.sqlString( preparation.getText1());
        values[8]  = SqlUtils.now();
        values[9]  = SqlUtils.one();
		values[10] = SqlUtils.sqlString( preparation.getYesNo1());
        
		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}
    
    private String getInsertSql(FragmentCitation exsiccataItem) throws LocalException
	{
		String fieldNames = "CollectionMemberID, FragmentID, ReferenceWorkID, TimestampCreated, Version";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( exsiccataItem.getCollectionMemberId());
		values[1] = SqlUtils.sqlString( exsiccataItem.getFragment().getId());
		values[2] = SqlUtils.sqlString( exsiccataItem.getReferenceWork().getId());
		values[3] = SqlUtils.now();
		values[4] = SqlUtils.one();
		
		return SqlUtils.getInsertSql("fragmentcitation", fieldNames, values);
	}
    
    private String getInsertSql(OtherIdentifier otherIdentifier)
    {
        String fieldNames = "CollectionMemberID, CollectionObjectID, Identifier, Institution, " +
        		            "Remarks, TimestampCreated, Version";
        
        String[] values = new String[7];
        
        values[0] = SqlUtils.sqlString( otherIdentifier.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( otherIdentifier.getCollectionObject().getId());
        values[2] = SqlUtils.sqlString( otherIdentifier.getIdentifier());
        values[3] = SqlUtils.sqlString( otherIdentifier.getInstitution());
        values[4] = SqlUtils.sqlString( otherIdentifier.getRemarks());
        values[5] = SqlUtils.now();
        values[6] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("otheridentifier", fieldNames, values);
    }
    
    private String getInsertSql(Container container) throws LocalException
    {
        String fieldNames = "CollectionMemberID, Name, Number, TimestampCreated, Type, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( container.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( container.getName());
        values[2] = SqlUtils.sqlString( container.getNumber());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.sqlString( container.getType());
        values[5] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("container", fieldNames, values);
    }

    private String getInsertSql(CollectingTrip collectingTrip)
    {
        String fieldNames = "CollectingTripName, DisciplineID, Remarks, TimestampCreated, Version";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( collectingTrip.getCollectingTripName());
        values[1] = SqlUtils.sqlString( collectingTrip.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( collectingTrip.getRemarks());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("collectingtrip", fieldNames, values);
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
