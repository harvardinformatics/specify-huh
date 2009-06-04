package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.SpecimenItem;
import edu.harvard.huh.asa.SpecimenItem.REPRO_STATUS;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.ContainerLookup;
import edu.harvard.huh.asa2specify.lookup.SiteLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Container;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ExsiccataItem;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.OtherIdentifier;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;

public class SpecimenItemLoader extends AuditedObjectLoader
{
    private static final Logger log  = Logger.getLogger(SpecimenItemLoader.class);
    
	private Hashtable<String, PrepType> prepTypesByNameAndColl;
	
	private SpecimenLookup collObjLookup;
	private PreparationLookup prepLookup;
	private ContainerLookup containerLookup;
	
	private BotanistLookup botanistLookup;
	private SubcollectionLookup subcollLookup;
	private SiteLookup siteLookup;
	
	private AsaIdMapper seriesBotanists;
	private AsaIdMapper specimenIdBarcodes;
	
	// These objects are all related to the collection object
	// and need the collection object to be saved first
	private Container            container        = null;
	private CollectionObject     collectionObject = null;
	private Collector            collector        = null;
	private CollectingEvent      collectingEvent  = null;
	private Set<Preparation>     preparations     = new HashSet<Preparation>();
	private Set<OtherIdentifier> otherIdentifiers = new HashSet<OtherIdentifier>();
	private Set<ExsiccataItem>   exsiccataItems   = new HashSet<ExsiccataItem>();

	// These items need to be remembered for comparison with
	// other specimen items' values
	private Integer              specimenId       = null;
	private REPRO_STATUS         reproStatus      = null;
	private Integer              subcollectionId  = null;
	
	public SpecimenItemLoader(File csvFile,
	                          Statement sqlStatement,
	                          File seriesBotanists,
	                          File specimenIdBarcodes,
	                          BotanistLookup botanistLookup,
	                          SubcollectionLookup subcollLookup,
	                          SiteLookup siteLookup) throws LocalException 
	{
		super(csvFile, sqlStatement);
		
		this.seriesBotanists = new AsaIdMapper(seriesBotanists);
		this.specimenIdBarcodes = new AsaIdMapper(specimenIdBarcodes);
		
		this.prepTypesByNameAndColl = new Hashtable<String, PrepType>();
		
		this.botanistLookup  = botanistLookup;
		this.subcollLookup   = subcollLookup;
		this.siteLookup      = siteLookup;
		this.prepLookup      = getPreparationLookup();
		this.containerLookup = getContainerLookup();
		
		init();
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
	    SpecimenItem specimenItem = parse(columns);
		
		Integer specimenItemId = specimenItem.getId();
		setCurrentRecordId(specimenItemId);
		
		Integer newSpecimenId = specimenItem.getSpecimenId();
		checkNull(newSpecimenId, "specimen id");
		
        // if this specimen item shares a specimen with the previous one,
        // then its preparation shares a collection object (and maybe a container)
        if (newSpecimenId.equals(specimenId))
        {
            Integer collectionId = collectionObject.getCollectionMemberId();
            
    		// Preparation
            Preparation preparation = getPreparation(specimenItem, collectionObject, collectionId);
            addPreparation(preparation);

            // merge repro status; warn if changed
            updateReproStatus(specimenItem);

            // merge subcollection; warn if 
            updateSubcollection(specimenItem, collectionId);
            
            // OtherIdentifier TODO: connect series (organization) agent as collector?
            OtherIdentifier series = getSeriesIdentifier(specimenItem, collectionObject, collectionId);
            addOtherIdentifier(series);

            // TODO: create accessions from provenance?
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
        reproStatus     = null;
        subcollectionId = null;
        
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

            // CollectingEvent
            collectingEvent = getCollectingEvent(specimenItem, getBotanyDiscipline());

            // CollectionObject
            collectionObject = getCollectionObject(specimenItem, collection, container, collectingEvent);

            // Collector
            collector = getCollector(specimenItem, collectingEvent, collectionId);

            // ExsiccataItem
            ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem, collectionObject);
            addExsiccataItem(exsiccataItem);

            // OtherIdentifiers TODO: connect series (organization) agent as collector?
            OtherIdentifier series = getSeriesIdentifier(specimenItem, collectionObject, collectionId);
            addOtherIdentifier(series);

            OtherIdentifier accession = getAccessionIdentifier(specimenItem, collectionObject, collectionId);
            addOtherIdentifier(accession);

            // Preparation
            Preparation preparation = getPreparation(specimenItem, collectionObject, collectionId);
            addPreparation(preparation);
        }
        catch (Exception e)
        {
            init();
            throw new LocalException(e);
        }
	}

	public Logger getLogger()
    {
        return log;
    }
	
	public ContainerLookup getContainerLookup()
	{
		if (containerLookup == null)
		{
			containerLookup = new ContainerLookup() {
				public Container getByName(String name) throws LocalException
				{
					Container container = new Container();
					
					Integer containerId = getInt("container", "ContainerID", "Name", name);
					
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

	public SpecimenLookup getSpecimenLookup()
    {
        if (collObjLookup == null)
        {
            collObjLookup = new SpecimenLookup() {
                
                public CollectionObject getById(Integer specimenId) throws LocalException
                {
                    CollectionObject collectionObject = new CollectionObject();
                    
                    String guid = getGuid(specimenId);
                    
                    Integer collectionObjectId = getInt("collectionobject", "CollectionObjectID", "GUID", guid);
                    
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
                public String formatBarcode(Integer barcode) throws LocalException
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
                    
                    Integer preparationId = getInt("preparation", "PreparationID","SampleNumber", barcode);
                    
                    preparation.setPreparationId(preparationId);
                    
                    return preparation;
                }
            };
        }
        return prepLookup;
    }
	
    protected void preLoad() throws LocalException
    {
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
        getLogger().info("Enabling keys");
        
        String[] tables = { "collectionobject", "collectingevent", "collector", "preparation" };
        
        for (String table : tables)
        {
            String sql = "alter table " + table + " enable keys";
            execute(sql);
        }
        
        // TODO: probably drop this index after import
        getLogger().info("Creating sample number index");
        String sql =  "create index samplenum on preparation(SampleNumber)";
        execute(sql);
    }
    
    private Integer lookupSeries(Integer seriesId)
    {
        return seriesBotanists.map(seriesId);
    }
    
    private Integer lookupBarcode(Integer specimenItemId)
    {
        return specimenIdBarcodes.map(specimenItemId);
    }
    
	private SpecimenItem parse(String[] columns) throws LocalException
	{
		if (columns.length < 40)
		{
			throw new LocalException("Not enough columns");
		}

		SpecimenItem specimenItem = new SpecimenItem();
		try {
		    specimenItem.setId(                      SqlUtils.parseInt( columns[0]  ));
		    specimenItem.setSpecimenId(              SqlUtils.parseInt( columns[1]  ));
		    specimenItem.setBarcode(                 SqlUtils.parseInt( columns[2]  ));
		    specimenItem.setCollectorNo(                                columns[3]  );
            specimenItem.setCatalogedDate(          SqlUtils.parseDate( columns[4]  ));
            specimenItem.setCultivated(           Boolean.parseBoolean( columns[5]  ));
            specimenItem.setDescription(                                columns[6]  );
            specimenItem.setHabitat(                                    columns[7]  );
            specimenItem.setSubstrate(                                  columns[8]  );
            specimenItem.setReproStatus( SpecimenItem.parseReproStatus( columns[9]  ));
            specimenItem.setSex(                                        columns[10] );
            specimenItem.setRemarks(                                    columns[11] );
            specimenItem.setAccessionNo(                                columns[12] );
            specimenItem.setProvenance(                                 columns[13] );
            specimenItem.setAccessionStatus(                            columns[14] );
            
            BDate bdate = new BDate();
            specimenItem.setCollDate( bdate );

            bdate.setStartYear(  SqlUtils.parseInt( columns[15] ));
            bdate.setStartMonth( SqlUtils.parseInt( columns[16] ));
            bdate.setStartDay(   SqlUtils.parseInt( columns[17] ));
            bdate.setStartPrecision(                columns[18] );
            bdate.setEndYear(    SqlUtils.parseInt( columns[19] ));
            bdate.setEndMonth(   SqlUtils.parseInt( columns[20] ));
            bdate.setEndDay(     SqlUtils.parseInt( columns[21] ));
            bdate.setEndPrecision(                  columns[22] );
            bdate.setText(                          columns[23] );  
            
            specimenItem.setItemNo(          SqlUtils.parseInt( columns[24] ));
            specimenItem.setOversize(     Boolean.parseBoolean( columns[25] ));
            specimenItem.setVoucher(                            columns[26] );
            specimenItem.setReference(                          columns[27] );
            specimenItem.setNote(                               columns[28] );
            specimenItem.setHerbariumCode(                      columns[29] );
            specimenItem.setSeriesName(                         columns[30] );
            specimenItem.setSiteId(          SqlUtils.parseInt( columns[31] ));
            specimenItem.setCollectorId(     SqlUtils.parseInt( columns[32] ));
            specimenItem.setCatalogedById(   SqlUtils.parseInt( columns[33] ));
            specimenItem.setFormat(                             columns[34] );
            specimenItem.setSeriesAbbrev(                       columns[35] );
            specimenItem.setSeriesNo(                           columns[36] );
            specimenItem.setContainer(                          columns[37] );
            specimenItem.setSubcollectionId( SqlUtils.parseInt( columns[38] ));
            specimenItem.setHasExsiccata( Boolean.parseBoolean( columns[39] ));
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


	private void updateReproStatus(SpecimenItem specimenItem) throws LocalException
	{
		REPRO_STATUS newReproStatus = specimenItem.getReproStatus();
        checkNull(newReproStatus, "repro status");
        
        if (!newReproStatus.equals(reproStatus))
        {
        	REPRO_STATUS status;
            if (reproStatus.equals(REPRO_STATUS.NotDetermined) ||
                    newReproStatus.equals(REPRO_STATUS.FlowerAndFruit) && 
                        (reproStatus.equals(REPRO_STATUS.Flower) || reproStatus.equals(REPRO_STATUS.Fruit)))
            {
                status = newReproStatus;
            }
            else if (reproStatus.equals(REPRO_STATUS.Flower) && newReproStatus.equals(REPRO_STATUS.Fruit) ||
                        reproStatus.equals(REPRO_STATUS.Fruit) && newReproStatus.equals(REPRO_STATUS.Flower))
            {
                status = REPRO_STATUS.FlowerAndFruit;
            }
            else
            {
                status = reproStatus;
            }
            
            if (!reproStatus.equals(status))
            {
                getLogger().warn(rec() + "Changing repro status from " + reproStatus + " to " + status);
                collectionObject.setText1(SpecimenItem.toString(status));
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
	
	private Container lookupContainer(String name) throws LocalException
	{
	    return containerLookup.queryByName(name);
	}

	private Container lookupContainer(Integer subcollectionId) throws LocalException
	{
	    return subcollLookup.getContainerById(subcollectionId);
	}

	private Locality lookupSite(Integer siteId) throws LocalException
	{
	    return siteLookup.queryById(siteId);
	}
	
	private String formatBarcode(Integer barcode) throws LocalException
	{
	    return prepLookup.formatBarcode(barcode);
	}

    private String getGuid(Integer specimenId)
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
                else
                {
                    container = getContainer(specimenItem, collectionMemberId);
                    collectionObject.setContainer(container);
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
	    preparations.add(preparation);
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
	    // TODO: if there's a series, should it be a collector?
	    
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

	private CollectingEvent getCollectingEvent(SpecimenItem specimenItem, Discipline discipline) throws LocalException
	{
		CollectingEvent collectingEvent = new CollectingEvent();

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
	    
	    return collectingEvent;
	}
	
	
	private Preparation getPreparation(SpecimenItem specimenItem, CollectionObject collectionObject, Integer collectionMemberId)
		throws LocalException
	{
	    Preparation preparation = new Preparation();

	    // CollectionMemberId
	    preparation.setCollectionMemberId(collectionMemberId);
	    
	    // CollectionObject
	    preparation.setCollectionObject(collectionObject);
	    
        // Number1 (itemNo)
        Integer itemNo = specimenItem.getItemNo();
        preparation.setNumber1((float) itemNo);
        
        // Number2 (specimenItemId) TODO: temporary
        Integer specimenItemId = specimenItem.getId();
        preparation.setNumber1((float) specimenItemId);
        
        // PrepType
        String format = specimenItem.getFormat();
        PrepType prepType = getPrepType(format, collectionMemberId);
        preparation.setPrepType(prepType);
        
        // Remarks(note)
        String note = specimenItem.getNote();
        preparation.setRemarks(note);
        
        // SampleNumber (barcode)
	    Integer barcode = lookupBarcode(specimenItemId);
	    if (barcode == null) barcode = specimenItem.getBarcode();
	    if (barcode == null)
	    {
	        getLogger().warn(rec() + "Null barcode");
	        barcode = specimenItemId;
	    }
        preparation.setSampleNumber(formatBarcode(barcode));
        
        // Text1 (voucher)
        String voucher = specimenItem.getVoucher(); // TODO: investigate specimen.voucher
        preparation.setText1(voucher);

        // Text2 (reference)
        String reference = specimenItem.getReference(); // TODO: investigate specimen.reference
        preparation.setText2(reference);

        // YesNo1 (isOversize)
        Boolean isOversize = specimenItem.isOversize();
        preparation.setYesNo1(isOversize);
        
        // YesNo2 (sex, isMale), YesNo3 (sex, is Female)
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

	private CollectionObject getCollectionObject(SpecimenItem specimenItem, Collection collection, Container container, CollectingEvent collectingEvent) throws LocalException
	{
		CollectionObject collectionObject = new CollectionObject();

        // Cataloger
        Integer createdById = specimenItem.getCatalogedById();
        checkNull(createdById, "created by id");
        
        Agent cataloger = getAgentByOptrId(createdById);
        collectionObject.setCataloger(cataloger);
        
        // CatalogedDate
        Date catalogedDate = specimenItem.getCatalogedDate();
        collectionObject.setCatalogedDate(DateUtils.toCalendar(catalogedDate));

        // CatalogedDatePrecision
        Byte catalogedDatePrecision = DateUtils.getFullDatePrecision();
        collectionObject.setCatalogedDatePrecision(catalogedDatePrecision);

        // CatalogNumber
        Integer specimenItemId = specimenItem.getId();
        checkNull(specimenItemId, "specimen item id");
        
        Integer barcode = lookupBarcode(specimenItemId);
        if (barcode == null) barcode = specimenItem.getBarcode();

        checkNull(barcode, "barcode");
        
        String catalogNumber = formatBarcode(barcode);
        collectionObject.setCatalogNumber(catalogNumber);

        // CollectionMemberID
        Integer collectionId = collection.getId();
        collectionObject.setCollectionMemberId(collectionId);
        
        // Collection
        collectionObject.setCollection(collection);
        
        // CollectingEvent
        collectionObject.setCollectingEvent(collectingEvent);

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
        
        // GUID
	    Integer specimenId = specimenItem.getSpecimenId();
	    checkNull(specimenId, "specimen id");
	    
	    String guid = getGuid(specimenId);
        collectionObject.setGuid(guid);
        
         // Remarks
        String remarks = specimenItem.getRemarks();
        collectionObject.setRemarks(remarks);
        
        // Text1 (reproStatus/phenology)
        reproStatus = specimenItem.getReproStatus();
        collectionObject.setText1(SpecimenItem.toString(reproStatus));
          
        // Text2 (substrate)
        String substrate = specimenItem.getSubstrate();
        collectionObject.setText2(substrate);
        
        // TimestampCreated
        Date dateCreated = specimenItem.getCatalogedDate();
        collectionObject.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isCultivated)
        collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement cultivated specimens

        return collectionObject;
	}

	private Container getContainer(SpecimenItem specimenItem, Integer collectionMemberId) throws LocalException
	{
	    subcollectionId = specimenItem.getSubcollectionId();
        String containerStr = specimenItem.getContainer();

        // SubcollectionLoader created Container objects for subcollections without authors and
        // Exsiccata objects for subcollections with authors (exsiccatae are handled by getExsiccataItem)
        if (subcollectionId != null)
        {            
            if (!specimenItem.hasExsiccata())
            {
                Container container = lookupContainer(subcollectionId);

                Integer containerId = container.getId();
                container.setContainerId(containerId);    

                if (containerStr != null)
                {
                    getLogger().warn("Subcollection and container present, dropping container: " + containerStr);
                }
            }
        }

        else if (containerStr != null)
        {
            // TODO: normalizing of container and subcollection name strings.
            // Note that if the container string = subcollection.name for some subcollection (it does happen),
            // we are in effect adding that subcollection_id to the specimen_item_record
            
            Container existingContainer = lookupContainer(containerStr);
            
            // new Container
            if (existingContainer == null)
            {
                // CollectionMemberId
                container.setCollectionMemberId(collectionMemberId);

                // Name
                containerStr = truncate(containerStr, 64, "container name");
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

        return series;
	}

	private OtherIdentifier getAccessionIdentifier(SpecimenItem specimenItem, CollectionObject collectionObject, Integer collectionMemberId)
	{
	    String accessionNo = specimenItem.getAccessionNo();
	    if (accessionNo == null) accessionNo = "NA";
	    
	    String provenance = specimenItem.getProvenance();
	    
	    if (accessionNo.equals("NA") && provenance == null)
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
	    otherIdentifier.setRemarks(provenance);
	    
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
        String fieldNames = "DisciplineID, EndDate, EndDatePrecision, EndDateVerbatim, " +
        		            "LocalityID, Remarks, StartDate, StartDatePrecision, StartDateVerbatim, " +
                            "TimestampCreated, Version";

        String[] values = new String[11];
        
        values[0]  = SqlUtils.sqlString( collectingEvent.getDiscipline().getDisciplineId());
        values[1]  = SqlUtils.sqlString( collectingEvent.getEndDate());
        values[2]  = SqlUtils.sqlString( collectingEvent.getEndDatePrecision());
        values[3]  = SqlUtils.sqlString( collectingEvent.getEndDateVerbatim());
        values[4]  = SqlUtils.sqlString( collectingEvent.getLocality().getLocalityId());
        values[5]  = SqlUtils.sqlString( collectingEvent.getRemarks());
        values[6]  = SqlUtils.sqlString( collectingEvent.getStartDate());
        values[7]  = SqlUtils.sqlString( collectingEvent.getStartDatePrecision());
        values[8]  = SqlUtils.sqlString( collectingEvent.getStartDateVerbatim());
        values[9]  = SqlUtils.now();
        values[10] = SqlUtils.one();
        
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
		String fieldNames = "CatalogerID, CatalogedDate, CatalogedDatePrecision, CatalogNumber, " +
							"CollectionID, CollectionMemberID, CollectingEventID, ContainerID, " +
							"CreatedByAgentID, Description, FieldNumber, GUID, Remarks, Text1, " +
							"Text2, TimestampCreated, Version, YesNo1";

		String[] values = new String[18];
		
		values[0]  = SqlUtils.sqlString( collectionObject.getCataloger().getAgentId());
		values[1]  = SqlUtils.sqlString( collectionObject.getCatalogedDate());
		values[2]  = SqlUtils.sqlString( collectionObject.getCatalogedDatePrecision());
		values[3]  = SqlUtils.sqlString( collectionObject.getCatalogNumber());
		values[4]  = SqlUtils.sqlString( collectionObject.getCollection().getId());
		values[5]  = SqlUtils.sqlString( collectionObject.getCollectionMemberId());
		values[6]  = SqlUtils.sqlString( collectionObject.getCollectingEvent().getId());
		values[7]  = SqlUtils.sqlString( collectionObject.getContainer().getId());
		values[8]  = SqlUtils.sqlString( collectionObject.getCreatedByAgent().getId());
		values[9]  = SqlUtils.sqlString( collectionObject.getDescription());
		values[10] = SqlUtils.sqlString( collectionObject.getFieldNumber());
		values[11] = SqlUtils.sqlString( collectionObject.getGuid());
		values[12] = SqlUtils.sqlString( collectionObject.getRemarks());
		values[13] = SqlUtils.sqlString( collectionObject.getText1());
		values[14] = SqlUtils.sqlString( collectionObject.getText2());
		values[15] = SqlUtils.sqlString( collectionObject.getTimestampCreated());
        values[16] = SqlUtils.one();
		values[17] = SqlUtils.sqlString( collectionObject.getYesNo1());		

		return SqlUtils.getInsertSql("collectionobject", fieldNames, values);
	}
		

    private String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, Number1, Number2, PrepTypeID, " +
				            "Remarks, SampleNumber, Text1, Text2, TimestampCreated, Version, YesNo1, YesNo2";

		String[] values = new String[13];
		
		values[0]  = SqlUtils.sqlString( preparation.getCollectionMemberId());
		values[1]  = SqlUtils.sqlString( preparation.getCollectionObject().getId());
		values[2]  = SqlUtils.sqlString( preparation.getNumber1());
		values[3]  = SqlUtils.sqlString( preparation.getNumber2());
		values[4]  = SqlUtils.sqlString( preparation.getPrepType().getId());
		values[5]  = SqlUtils.sqlString( preparation.getRemarks());
		values[6]  = SqlUtils.sqlString( preparation.getSampleNumber());
		values[7]  = SqlUtils.sqlString( preparation.getText1());
		values[8]  = SqlUtils.sqlString( preparation.getText2());
        values[9]  = SqlUtils.now();
        values[10] = SqlUtils.one();
		values[11] = SqlUtils.sqlString( preparation.getYesNo1());
		values[12] = SqlUtils.sqlString( preparation.getYesNo2());
        
		return SqlUtils.getInsertSql("preparation", fieldNames, values);
	}

    private String getInsertSql(ExsiccataItem exsiccataItem) throws LocalException
	{
		String fieldNames = "CollectionObjectID, ExsiccataID, TimestampCreated, Version";

		String[] values = new String[4];

		values[0] = SqlUtils.sqlString( exsiccataItem.getCollectionObject().getId());
		values[1] = SqlUtils.sqlString( exsiccataItem.getExsiccata().getId());
		values[2] = SqlUtils.now();
		values[3] = SqlUtils.one();
		
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
        values[6] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("otheridentifier", fieldNames, values);
    }
       
    private String getInsertSql(Container container) throws LocalException
    {
        String fieldNames = "Name, CollectionMemberID, TimestampCreated, Version";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( container.getName());
        values[1] = SqlUtils.sqlString( container.getCollectionMemberId());
        values[2] = SqlUtils.now();
        values[3] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("container", fieldNames, values);
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
        if (container.getName() != null && container.getContainerId() == null)
        {
        	sql = getInsertSql(container);
        	Integer containerId = insert(sql);
        	container.setContainerId(containerId);
        }

        // save the current CollectingEvent
        if (collectingEvent != null)
        {
            sql = getInsertSql(collectingEvent);
            Integer collectingEventId = insert(sql);
            collectingEvent.setCollectingEventId(collectingEventId);
        }
        
        // save the current Collector
        if (collector != null)
        {
            sql = getInsertSql(collector);
            Integer collectorId = insert(sql);
            collector.setCollectorId(collectorId);
        }
        
        // save the current CollectionObject
        sql = getInsertSql(collectionObject);
        Integer collectionObjectId = insert(sql);
        collectionObject.setCollectionObjectId(collectionObjectId);
        
        // save the current Preparations
        for (Preparation preparation : preparations)
        {
            sql = getInsertSql(preparation);
            insert(sql);
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
        container        = new Container();
        preparations.clear();
        otherIdentifiers.clear();
        exsiccataItems.clear();
    }
}
