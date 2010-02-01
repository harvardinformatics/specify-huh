package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.SpecimenItem;
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
import edu.ku.brc.specify.datamodel.CollectionObjectAttribute;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Container;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ExsiccataItem;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.OtherIdentifier;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.PreparationAttribute;
import edu.ku.brc.specify.datamodel.Storage;

public class SpecimenItemLoader extends AuditedObjectLoader
{
    // See edu.ku.brc.specify.conversion.GenericDBConversion for AttributeDef
    
	private Hashtable<String, PrepType> prepTypesByNameAndColl;
	
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
	private Container            container        = null;
	private CollectionObject     collectionObject = null;
	private CollectionObjectAttribute collObjAttr = null;
	private Collector            collector        = null;
	private Collector            seriesCollector  = null;
	private CollectingEvent      collectingEvent  = null;
	private CollectingTrip       collectingTrip   = null;
	private Set<Preparation>     preparations     = new HashSet<Preparation>();
	private Set<PreparationAttribute> prepAttrs   = new HashSet<PreparationAttribute>();
	private Set<OtherIdentifier> otherIdentifiers = new HashSet<OtherIdentifier>();
	private Set<ExsiccataItem>   exsiccataItems   = new HashSet<ExsiccataItem>();
	
	private final Storage nullStorage = new Storage();
	
	// These items need to be remembered for comparison with
	// other specimen items' values
	private Integer      specimenId       = null;
	private Integer      subcollectionId  = null;
	private String       containerStr     = null;
	
	private int nextCatalogNumber = 1;

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
		
		this.prepTypesByNameAndColl = new Hashtable<String, PrepType>();
		
		this.botanistLookup  = botanistLookup;
		this.subcollLookup   = subcollLookup;
		this.siteLookup      = siteLookup;
        this.seriesLookup    = seriesLookup;
        this.containerLookup = getContainerLookup();
		this.prepLookup      = getPreparationLookup();
		this.collTripLookup  = getCollectingTripLookup();
		
		init();
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
	    SpecimenItem specimenItem = parse(columns);
		
		Integer specimenItemId = specimenItem.getId();
		
		Integer newSpecimenId = specimenItem.getSpecimenId();
		checkNull(newSpecimenId, "specimen id");
		
		setCurrentRecordId(specimenItemId != null ? specimenItemId : newSpecimenId );
	      
        // if this specimen item shares a specimen with the previous one,
        // then its preparation shares a collection object (and maybe a container)
        if (newSpecimenId.equals(specimenId))
        {
            Integer collectionId = collectionObject.getCollectionMemberId();
            
            // PreparationAttribute
            PreparationAttribute prepAttr = getPrepAttr(specimenItem, collectionId);
            addPrepAttr(prepAttr);
            
    		// Preparation
            Preparation preparation = getPreparation(specimenItem, collectionObject, collectionId, prepAttr);
            addPreparation(preparation);

            // merge collectionobjectattr/collectingtrip; warn if changed
            updateContainerStr(specimenItem);

            // merge subcollection; warn if changed
            updateSubcollection(specimenItem, collectionId);

            // maybe add an OtherIdentifier for accession
            OtherIdentifier accession = getAccessionIdentifier(specimenItem, collectionObject, collectionId);
            addOtherIdentifier(accession);

            // ExsiccataItem
            ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem, collectionObject);
            addExsiccataItem(exsiccataItem);
            
            return;
        }

        // If we made it here, the asa.specimen.ids are different, so we have a different
        // collection object entirely.  First, save the objects associated with the old asa.specimen.id
        specimenId      = newSpecimenId;
        subcollectionId = null;
        containerStr    = null;
        
        if (collectionObject != null)
        {
            try
            {
                saveObjects();
            }
            catch (Exception e)
            {
                String guid = collectionObject.getGuid();
                init();
                throw new LocalException("Problem saving records for " + guid, e);
            }
        }
        
        ////////////////////////////////////////////////
        // Begin construction of new CollectionObject //
        ////////////////////////////////////////////////

        try
        {
            // Collection
            Collection collection = getCollection(specimenItem);

            Integer collectionId = collection.getId();

            // Container
            container = getContainer(specimenItem, collectionId);

            // CollectingTrip
            collectingTrip = getCollectingTrip(specimenItem, getBotanyDiscipline());

            // CollectingEvent
            collectingEvent = getCollectingEvent(specimenItem, collectingTrip, getBotanyDiscipline());

            // CollectionObjectAttribute
            collObjAttr = getCollObjAttr(specimenItem, collectionId);
            
            // CollectionObject
            collectionObject = getCollectionObject(specimenItem, collection, container, collectingEvent, collObjAttr);

            // Collector
            collector = getCollector(specimenItem, collectingEvent, collectionId);

            // Series collector
            seriesCollector = getSeriesCollector(specimenItem, collectingEvent, collectionId);
            
            // ExsiccataItem
            ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem, collectionObject);
            addExsiccataItem(exsiccataItem);

            // OtherIdentifiers
            OtherIdentifier series = getSeriesIdentifier(specimenItem, collectionObject, collectionId);
            addOtherIdentifier(series);

            OtherIdentifier accession = getAccessionIdentifier(specimenItem, collectionObject, collectionId);
            addOtherIdentifier(accession);

            // PreparationAttribute
            PreparationAttribute prepAttr = getPrepAttr(specimenItem, collectionId);
            addPrepAttr(prepAttr);
            
            // Preparation
            Preparation preparation = getPreparation(specimenItem, collectionObject, collectionId, prepAttr);
            addPreparation(preparation);
        }
        catch (Exception e)
        {
            init();
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
					
					Integer containerId = getId("container", "ContainerID", "Name", name);
					
					container.setContainerId(containerId);
					
					return container;
				}
				
				public Container queryByName(String name) throws LocalException
				{
					Integer containerId = queryForInt("container", "ContainerID", "Name", name);
					
					if (containerId == null) return null;
					
					Container container = new Container();
					
					container.setContainerId(containerId);
					
					return container;
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
                    CollectionObject collectionObject = new CollectionObject();
                    
                    String altCatalogNumber = getAltCatalogNumber(specimenId);
                    
                    Integer collectionObjectId = getId("collectionobject", "CollectionObjectID", "AltCatalogNumber", altCatalogNumber);
                    
                    collectionObject.setCollectionObjectId(collectionObjectId);
                    
                    return collectionObject;
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
                        throw new LocalException("Null barcode");
                    }
                    
                    try
                    {
                        return (new DecimalFormat( "000000000" ) ).format( barcode );
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
                        throw new LocalException("Null barcode");
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

                public Preparation getBySpecimenItemId(Integer specimenItemId) throws LocalException
                {
                    Preparation preparation = new Preparation();
                    
                    Integer preparationId = getInt("preparation", "PreparationID","Number1", specimenItemId);
                    
                    preparation.setPreparationId(preparationId);
                    
                    return preparation;
                }
                
                public Preparation getByBarcode(String barcode) throws LocalException
                {
                    Preparation preparation = new Preparation();
                    
                    Integer preparationId = getId("preparation", "PreparationID","SampleNumber", barcode);
                    
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
        // save the last specimen record
        try
        {
            saveObjects();
        }
        catch (Exception e)
        {
            String guid = collectionObject == null ? "null" : collectionObject.getGuid();
            getLogger().error("Problem saving records for " + guid, e);
        }
        
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
            specimenItem.setFormat(                             columns[i + 32] );
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

	private void updateContainerStr(SpecimenItem specimenItem) throws LocalException
	{	    
        String newContainerStr = specimenItem.getContainer();
        if (newContainerStr != null && !newContainerStr.equals(containerStr))
        {
            getLogger().warn(rec() + "Changing containerStr from " + containerStr + " to " + newContainerStr);
            containerStr = newContainerStr;
            
            if (! specimenItem.hasCollectingTrip())
            {
                //collObjAttr.setText3(containerStr);
                collectionObject.setText1(containerStr);
            }
            else
            {
                collectingTrip = getCollectingTrip(specimenItem);
                collectingEvent.setCollectingTrip(collectingTrip);
            }
        } 
	}

	private Agent lookupBotanist(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}
	   
	private Exsiccata lookupExsiccata(Integer subCollectionId) throws LocalException
	{
	    return subcollLookup.getExsiccataById(subcollectionId);
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
    
	private void updateSubcollection(SpecimenItem specimenItem, Integer collectionMemberId) throws LocalException
	{
        Integer newSubcollectionId = specimenItem.getSubcollectionId();
        
        if (newSubcollectionId == null) return;
        
        if (!newSubcollectionId.equals(subcollectionId))
        {
            if (subcollectionId == null)
            {
                subcollectionId = newSubcollectionId;
                
                if (specimenItem.hasExsiccata())
                {
                    ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem, collectionObject);
                    exsiccataItems.add(exsiccataItem);
                }

            }
            else if (!subcollectionId.equals(newSubcollectionId))
            {
                getLogger().warn(rec() + "Multiple subcollections, ignoring this one: " + String.valueOf(newSubcollectionId));
            }
        }
	}

	private void addPreparation(Preparation preparation)
	{
	    if (preparation != null) preparations.add(preparation);
	}

	private void addPrepAttr(PreparationAttribute prepAttr)
	{
	    if (prepAttr != null) prepAttrs.add(prepAttr);
	}
	
	private void addExsiccataItem(ExsiccataItem exsiccataItem)
	{
	    if (exsiccataItem == null) return;
	    
        for (ExsiccataItem e : exsiccataItems)
        {
            if (e.getExsiccata().getId().equals(exsiccataItem.getExsiccata().getId())) return;
        }
	    exsiccataItems.add(exsiccataItem);
	}
	
	private void addOtherIdentifier(OtherIdentifier otherIdentifier)
	{
	    if (otherIdentifier == null) return;
	    
        for (OtherIdentifier o : otherIdentifiers)
        {
            if (o.getInstitution().equals(otherIdentifier.getInstitution()) &&
                    o.getIdentifier().equals(otherIdentifier.getIdentifier()))
            {
                String oRemarks = o.getRemarks();
                
                if (oRemarks == null && otherIdentifier.getRemarks() == null) return;
                if (oRemarks.equals(otherIdentifier.getRemarks())) return;
            }
        }

	    otherIdentifiers.add(otherIdentifier);
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
	
	private Preparation getPreparation(SpecimenItem specimenItem, CollectionObject collectionObject, Integer collectionMemberId, PreparationAttribute prepAttr)
		throws LocalException
	{
        if (specimenItem.getId() == null) return null;
        
        Integer specimenItemId = specimenItem.getId();
        Integer barcode = lookupBarcode(specimenItemId);
        if (barcode == null) barcode = specimenItem.getBarcode();
        if (barcode == null)
        {
            getLogger().warn(rec() + "Null barcode");
        }

        Preparation preparation = new Preparation();
        
	    // CollectionMemberId
	    preparation.setCollectionMemberId(collectionMemberId);
	    
	    // CollectionObject
	    preparation.setCollectionObject(collectionObject);
	    
	    // CountAmt: this needs to be set so that the create loan preparation logic works
	    preparation.setCountAmt(Integer.valueOf(1));
                
	    // PreparationAttribute
	    preparation.setPreparationAttribute(prepAttr);
	    
        // PrepType
        String format = specimenItem.getFormat();
        PrepType prepType = getPrepType(format, collectionMemberId);
        preparation.setPrepType(prepType);
        
        // Remarks (note)
        String note = specimenItem.getNote();
        preparation.setRemarks(note);
        
        // SampleNumber (barcode)
        String sampleNumber = null;
        if (barcode != null) sampleNumber = getPreparationLookup().formatPrepBarcode(barcode);
        preparation.setSampleNumber(sampleNumber);

	    // Storage
	    Integer subcollectionId = specimenItem.getSubcollectionId();
	    Storage storage = nullStorage;
	    if (subcollectionId != null)
	    {
	        storage = lookupStorage(subcollectionId);
	    }

        preparation.setStorage(storage);
        
	    // StorageLocation (location/temp location)
	    String location = specimenItem.getLocation();
	    if (location != null) location = truncate(location, 50, "location");
	    preparation.setStorageLocation(location);

	    // Text1 (provenance)
        String provenance = specimenItem.getProvenance();
        preparation.setText1(provenance);

        // Text2 (voucher)
        String voucher = specimenItem.getVoucher();
        preparation.setText2(voucher);
        
	    // YesNo1 (isOversize)
        Boolean isOversize = specimenItem.isOversize();
        preparation.setYesNo1(isOversize);
        
        return preparation;
	}

	private PreparationAttribute getPrepAttr(SpecimenItem specimenItem, Integer collectionMemberId)
	{
	    PreparationAttribute prepAttr = new PreparationAttribute();
	    
	    // CollectionMemberID
	    prepAttr.setCollectionMemberId(collectionMemberId);
	    
	    // Number4 (item_no)
	    Integer itemNo = specimenItem.getItemNo();
	    prepAttr.setNumber4(itemNo);
	    
	    // Number5 (replicates)
	    Integer replicates = specimenItem.getReplicates();
	    prepAttr.setNumber5(replicates);
	    
	    // Number6 (specimenItemId) TODO: temporary
        Integer specimenItemId = specimenItem.getId();
        prepAttr.setNumber6(specimenItemId);
        
	    // Text1 (reference)
	    String reference = specimenItem.getReference();
	    prepAttr.setText1(reference);
	    
	    // Text3 (herbarium)
	    String herbarium = specimenItem.getHerbariumCode();
	    prepAttr.setText3(herbarium);

	    // Text4 (sex)
        String sex = specimenItem.getSex();
        prepAttr.setText4(sex);
        
	    // Text5 (repro)
        REPRO_STATUS reproStatus = specimenItem.getReproStatus();
        if (reproStatus != null) prepAttr.setText5(SpecimenItem.toString(reproStatus));
        
	    return prepAttr;
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
	                                             CollectingEvent collectingEvent,
	                                             CollectionObjectAttribute collObjAttr) throws LocalException
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
        Integer specimenItemId = specimenItem.getId();
        if (specimenItemId == null)
        {
            getLogger().warn(rec() + "Null specimen item");
        }
        
        String catalogNumber = getPreparationLookup().formatCollObjBarcode(nextCatalogNumber());
        collectionObject.setCatalogNumber(catalogNumber);
        
        // CollectionMemberID
        Integer collectionId = collection.getId();
        collectionObject.setCollectionMemberId(collectionId);
        
        // Collection
        collectionObject.setCollection(collection);
        
        // CollectingEvent
        collectionObject.setCollectingEvent(collectingEvent);

        // CollectionObjectAttribute TODO: change to CollectionObjectAttr?
        collectionObject.setCollectionObjectAttribute(collObjAttr);

        // Container (subcollection)
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
        String remarks = specimenItem.getRemarks();
        collectionObject.setRemarks(remarks);
        
        // Text1 (container)
        containerStr = specimenItem.getContainer();
        if (!specimenItem.hasCollectingTrip()) collectionObject.setText1(containerStr);
        
        // Text2 (substrate)
        String substrate = specimenItem.getSubstrate();
        collectionObject.setText2(substrate);
        
        // YesNo1 (isCultivated)
        collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement cultivated specimens

        return collectionObject;
	}

	private CollectionObjectAttribute getCollObjAttr(SpecimenItem specimenItem, Integer collectionMemberId) throws LocalException
	{
	    CollectionObjectAttribute collObjAttr = new CollectionObjectAttribute();
	    
	    // CollectionMemberID
	    collObjAttr.setCollectionMemberId(collectionMemberId);

	    // Text1 (vernacular name)
	    String vernacularName = specimenItem.getVernacularName();
	    collObjAttr.setText1(vernacularName);
	    
	    // Text2 (host)
        String habitat = specimenItem.getHabitat();
        if (habitat != null)
        {
            int i =  habitat.toLowerCase().indexOf("host:");
            if (i >= 0 && i+5 < habitat.length())
            {
                String host = habitat.substring(i+5).trim();
                collObjAttr.setText2(host);
            }
        }
        
        // Text3 (distribution)
	    String distribution = specimenItem.getDistribution();
	    if (distribution != null) distribution = truncate(distribution, 50, "distribution");
	    collObjAttr.setText3(distribution);
	    
	    return collObjAttr;
	}

    private CollectingTrip getCollectingTrip(SpecimenItem specimenItem) throws LocalException
    {
        return getCollectingTrip(specimenItem, getBotanyDiscipline());
    }
    
    private CollectingTrip getCollectingTrip(SpecimenItem specimenItem, Discipline discipline) throws LocalException
    {
        if (specimenItem.hasCollectingTrip())
        {
            String container = specimenItem.getContainer();
            checkNull(container, "container");
            
            String collectingTripName = truncate(container, 200, "collecting trip name");
            
            CollectingTrip existingTrip = lookupCollectingTrip(collectingTripName);

            if (existingTrip == null)
            {          
                collectingTrip = new CollectingTrip();

                // CollectingTripName
                collectingTrip.setCollectingTripName(collectingTripName);

                // Discipline
                collectingTrip.setDiscipline(discipline);

                // Remarks
                collectingTrip.setRemarks(container);
            }
            else
            {
                Integer collectingTripId = existingTrip.getId();
                collectingTrip.setCollectingTripId(collectingTripId);
            }
        }
        
        return collectingTrip;
    }
    
	private boolean containsData(CollectionObjectAttribute collObjAttr)
	{
	    if (collObjAttr.getText1()  != null) return true;
	    if (collObjAttr.getText2()  != null) return true;
	    if (collObjAttr.getText3()  != null) return true;
        if (collObjAttr.getYesNo1() != null) return true;
	    return false;
	}
	
	private int nextCatalogNumber()
	{
	    return nextCatalogNumber++;
	}

	private Container getContainer(SpecimenItem specimenItem, Integer collectionMemberId) throws LocalException
	{
	    subcollectionId = specimenItem.getSubcollectionId();
        String containerStr = specimenItem.getContainer();

        if (containerStr != null)
        {
            // TODO: normalizing of container and subcollection name strings.
            // Note that if the container string = subcollection.name for some subcollection (it does happen),
            // we are in effect adding that subcollection_id to the specimen_item record
            
            Container existingContainer = lookupContainer(containerStr);
            
            // new Container
            if (existingContainer == null)
            {
                // CollectionMemberId
                container.setCollectionMemberId(collectionMemberId);

                // Name
                containerStr = truncate(containerStr, 200, "container name");
                container.setName(containerStr);
            }
            else
            {
                Integer containerId = existingContainer.getId();
                container.setContainerId(containerId);
            }
        }

        return container;
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
	    String accessionNo = specimenItem.getAccessionNo();
	    
	    if (accessionNo == null)
	    {
	        return null;
	    }

	    OtherIdentifier otherIdentifier = new OtherIdentifier();
	    
	    // CollectionMemberId
	    otherIdentifier.setCollectionMemberId(collectionMemberId);

	    // CollectionObject
	    otherIdentifier.setCollectionObject(collectionObject);
	    
	    // Identifier
	    otherIdentifier.setIdentifier(accessionNo);

	    // Institution
	    otherIdentifier.setInstitution(specimenItem.getHerbariumCode());

	    // Remarks
	    otherIdentifier.setRemarks("accession");
	    
	    return otherIdentifier;
	}

	private ExsiccataItem getExsiccataItem(SpecimenItem specimenItem, CollectionObject collectionObject) throws LocalException
	{
	    Integer subcollectionId = specimenItem.getSubcollectionId();

	    if (subcollectionId == null || !specimenItem.hasExsiccata()) return null;

	    ExsiccataItem exsiccataItem = new ExsiccataItem();
        
        // CollectionObject
        exsiccataItem.setCollectionObject(collectionObject);
        
        // Exsiccata
        Exsiccata exsiccata = lookupExsiccata(subcollectionId);
        exsiccataItem.setExsiccata(exsiccata);

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
        values[12] = SqlUtils.zero();
        
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
        values[6] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("collector", fieldNames, values);
    }
    
    private String getInsertSql(CollectionObject collectionObject) throws LocalException
	{
		String fieldNames = "AltCatalogNumber, CatalogerID, CatalogedDate, CatalogedDatePrecision, CatalogNumber, " +
							"CollectionID, CollectionMemberID, CollectingEventID, CollectionObjectAttributeID, " +
							"ContainerID, CreatedByAgentID, Description, FieldNumber, ModifiedByAgentID, Remarks, " +
							"Text1, Text2, TimestampCreated, TimestampModified, Version, YesNo1";

		String[] values = new String[21];
		
		values[0]  = SqlUtils.sqlString( collectionObject.getAltCatalogNumber());
		values[1]  = SqlUtils.sqlString( collectionObject.getCataloger().getAgentId());
		values[2]  = SqlUtils.sqlString( collectionObject.getCatalogedDate());
		values[3]  = SqlUtils.sqlString( collectionObject.getCatalogedDatePrecision());
		values[4]  = SqlUtils.sqlString( collectionObject.getCatalogNumber());
		values[5]  = SqlUtils.sqlString( collectionObject.getCollection().getId());
		values[6]  = SqlUtils.sqlString( collectionObject.getCollectionMemberId());
		values[7]  = SqlUtils.sqlString( collectionObject.getCollectingEvent().getId());
		values[8]  = SqlUtils.sqlString( collectionObject.getCollectionObjectAttribute().getId());
		values[9]  = SqlUtils.sqlString( collectionObject.getContainer().getId());
		values[10] = SqlUtils.sqlString( collectionObject.getCreatedByAgent().getId());
		values[11] = SqlUtils.sqlString( collectionObject.getDescription());
		values[12] = SqlUtils.sqlString( collectionObject.getFieldNumber());
		values[13] = SqlUtils.sqlString( collectionObject.getModifiedByAgent().getId());
		values[14] = SqlUtils.sqlString( collectionObject.getRemarks());
		values[15] = SqlUtils.sqlString( collectionObject.getText1());
		values[16] = SqlUtils.sqlString( collectionObject.getText2());
		values[17] = SqlUtils.sqlString( collectionObject.getTimestampCreated());
		values[18] = SqlUtils.sqlString( collectionObject.getTimestampModified());
        values[19] = SqlUtils.zero();
		values[20] = SqlUtils.sqlString( collectionObject.getYesNo1());		

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}
		
    private String getInsertSql(CollectionObjectAttribute collObjAttr) throws LocalException
    {
        String fieldNames = "CollectionMemberID, Text1, Text2, Text3, TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( collObjAttr.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( collObjAttr.getText1());
        values[2] = SqlUtils.sqlString( collObjAttr.getText2());
        values[3] = SqlUtils.sqlString( collObjAttr.getText3());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("collectionobjectattribute", fieldNames, values);
    }

    private String getInsertSql(PreparationAttribute prepAttr) throws LocalException
    {
        String fieldNames = "CollectionMemberID, Number4, Number5, Number6, Text1, Text3, " +
        		            "Text4, Text5, TimestampCreated, Version";
        
        String[] values = new String[10];
        
        values[0] = SqlUtils.sqlString( prepAttr.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( prepAttr.getNumber4());
        values[2] = SqlUtils.sqlString( prepAttr.getNumber5());
        values[3] = SqlUtils.sqlString( prepAttr.getNumber6());
        values[4] = SqlUtils.sqlString( prepAttr.getText1());
        values[5] = SqlUtils.sqlString( prepAttr.getText3());
        values[6] = SqlUtils.sqlString( prepAttr.getText4());
        values[7] = SqlUtils.sqlString( prepAttr.getText5());
        values[8] = SqlUtils.now();
        values[9] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("preparationattribute", fieldNames, values);
    }

    private String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, CountAmt, PreparationAttributeID, " +
				            "PrepTypeID, Remarks, SampleNumber, StorageID, StorageLocation, Text1, " +
				            "Text2, TimestampCreated, Version, YesNo1";

		String[] values = new String[14];
		
		values[0]  = SqlUtils.sqlString( preparation.getCollectionMemberId());
		values[1]  = SqlUtils.sqlString( preparation.getCollectionObject().getId());
		values[2]  = SqlUtils.sqlString( preparation.getCountAmt());
		values[3]  = SqlUtils.sqlString( preparation.getPreparationAttribute().getId());
		values[4]  = SqlUtils.sqlString( preparation.getPrepType().getId());
		values[5]  = SqlUtils.sqlString( preparation.getRemarks());
		values[6]  = SqlUtils.sqlString( preparation.getSampleNumber());
		values[7]  = SqlUtils.sqlString( preparation.getStorage().getId());
		values[8]  = SqlUtils.sqlString( preparation.getStorageLocation());
		values[9]  = SqlUtils.sqlString( preparation.getText1());
		values[10] = SqlUtils.sqlString( preparation.getText2());
        values[11] = SqlUtils.now();
        values[12] = SqlUtils.zero();
		values[13] = SqlUtils.sqlString( preparation.getYesNo1());
        
		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}

    private String getInsertSql(ExsiccataItem exsiccataItem) throws LocalException
	{
		String fieldNames = "CollectionObjectID, ExsiccataID, TimestampCreated, Version";

		String[] values = new String[4];

		values[0] = SqlUtils.sqlString( exsiccataItem.getCollectionObject().getId());
		values[1] = SqlUtils.sqlString( exsiccataItem.getExsiccata().getId());
		values[2] = SqlUtils.now();
		values[3] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("exsiccataitem", fieldNames, values);
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
        values[6] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("otheridentifier", fieldNames, values);
    }
       
    private String getInsertSql(Container container) throws LocalException
    {
        String fieldNames = "Name, CollectionMemberID, TimestampCreated, Version";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( container.getName());
        values[1] = SqlUtils.sqlString( container.getCollectionMemberId());
        values[2] = SqlUtils.now();
        values[3] = SqlUtils.zero();
        
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
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("collectingtrip", fieldNames, values);
    }

    private PrepType getPrepType(String format, Integer collectionId) throws LocalException
	{
	    String key = format + " " + collectionId;
	    
        PrepType prepType = prepTypesByNameAndColl.get(key);
        if (prepType == null)
        {
            String sql = "select PrepTypeID from preptype where CollectionID=" + collectionId + " and Name=" + SqlUtils.sqlString(format);
            Integer prepTypeId = queryForInt(sql);
            if (prepTypeId == null) throw new LocalException("Couldn't find prep type for " + key);
            
            prepType = new PrepType();
            prepType.setPrepTypeId(prepTypeId);
            prepTypesByNameAndColl.put(key, prepType);
        }
        return prepType;
	}
	
    private void saveObjects() throws LocalException
    {
        String sql;  // this variable is going to be re-used.

        // save the current container if necessary
        if (container.getName() != null && container.getId() == null)
        {
        	sql = getInsertSql(container);
        	Integer containerId = insert(sql);
        	container.setContainerId(containerId);
        }

        // save the current collection object attributes object if necessary
        if (collObjAttr != null && collObjAttr.getId() == null && containsData(collObjAttr))
        {
            sql = getInsertSql(collObjAttr);
            Integer collObjAttrId = insert(sql);
            collObjAttr.setCollectionObjectAttributeId(collObjAttrId);
        }

        // save the current collecting trip if necessary
        if (collectingTrip != null && collectingTrip.getId() == null && collectingTrip.getCollectingTripName() != null)
        {
            sql = getInsertSql(collectingTrip);
            Integer collectingTripId = insert(sql);
            collectingTrip.setCollectingTripId(collectingTripId);
        }

        // save the current collecting event
        if (collectingEvent != null && collectingEvent.getId() == null)
        {
            sql = getInsertSql(collectingEvent);
            Integer collectingEventId = insert(sql);
            collectingEvent.setCollectingEventId(collectingEventId);
        }
        
        // save the current Collector
        if (collector != null && collector.getId() == null)
        {
            sql = getInsertSql(collector);
            Integer collectorId = insert(sql);
            collector.setCollectorId(collectorId);
        }
        
        // save the series Collector
        if (seriesCollector != null && seriesCollector.getId() == null)
        {
            sql = getInsertSql(seriesCollector);
            Integer collectorId = insert(sql);
            seriesCollector.setCollectorId(collectorId);
        }

        // save the current CollectionObject
        sql = getInsertSql(collectionObject);
        Integer collectionObjectId = insert(sql);
        collectionObject.setCollectionObjectId(collectionObjectId);
        
        // save the PreparationAttributes
        for (PreparationAttribute prepAttr : prepAttrs)
        {
            sql = getInsertSql(prepAttr);
            Integer preparationAttributeId = insert(sql);
            prepAttr.setPreparationAttributeId(preparationAttributeId);
        }

        // save the current Preparations
        for (Preparation preparation : preparations)
        {
            sql = getInsertSql(preparation);
            Integer preparationId = insert(sql);
            preparation.setPreparationId(preparationId);
        }

        // save the current OtherIdentifiers
        for (OtherIdentifier otherId : otherIdentifiers)
        {
            sql = getInsertSql(otherId);
            insert(sql);
        }

        // save the current ExsiccataItem
        for (ExsiccataItem exsiccataItem : exsiccataItems)
        {            
            sql = getInsertSql(exsiccataItem);
            insert(sql);
        }
        
        init();
    }
    
    private void init()
    {
        collectingEvent  = null;
        collector        = null;
        collectionObject = null;
        collObjAttr      = new CollectionObjectAttribute();
        container        = new Container();
        collectingTrip   = new CollectingTrip();
        prepAttrs.clear();
        preparations.clear();
        otherIdentifiers.clear();
        exsiccataItems.clear();
    }
}
