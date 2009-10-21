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
import edu.harvard.huh.asa2specify.lookup.CollectingTripLookup;
import edu.harvard.huh.asa2specify.lookup.SeriesLookup;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.ContainerLookup;
import edu.harvard.huh.asa2specify.lookup.SiteLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.ku.brc.dbsupport.AttributeIFace;
import edu.ku.brc.specify.conversion.GenericDBConversion;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AttributeDef;
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
import edu.ku.brc.specify.datamodel.PreparationAttr;

public class SpecimenItemLoader extends AuditedObjectLoader
{
    private static final Logger log  = Logger.getLogger(SpecimenItemLoader.class);
    
    private static final String ProvenanceFieldName   = "Provenance";
    private static final String VoucherFieldName      = "Voucher";
    private static final String ReferenceFieldName    = "Reference";
    private static final String ReproStatusFieldName  = "Repro. Status";
    private static final String CryptoStatusFieldName = "Crypto. Status";
    
    private AttributeDef provenanceAttrDef;
    private AttributeDef voucherAttrDef;
    private AttributeDef referenceAttrDef;
    private AttributeDef reproStatusAttrDef;
    private AttributeDef cryptoStatusAttrDef;
    
	private Hashtable<String, PrepType> prepTypesByNameAndColl;
	
	private SpecimenLookup collObjLookup;
	private PreparationLookup prepLookup;
	private ContainerLookup containerLookup;
	private CollectingTripLookup collTripLookup;
	
	private BotanistLookup botanistLookup;
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
	private Set<OtherIdentifier> otherIdentifiers = new HashSet<OtherIdentifier>();
	private Set<ExsiccataItem>   exsiccataItems   = new HashSet<ExsiccataItem>();

	// These items need to be remembered for comparison with
	// other specimen items' values
	private Integer      specimenId       = null;
	private Integer      subcollectionId  = null;
	private Integer      replicates       = null;
	private String       vernacularName   = null;
	private String       distribution     = null;
	private String       containerStr     = null;
	
	// This is the next available barcode for items without them.
	// Check with specimen_item_id_barcode.csv
	private int nextBarcode = 900000013;

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
		this.prepLookup      = getPreparationLookup();
		this.containerLookup = getContainerLookup();
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
            
    		// Preparation
            Preparation preparation = getPreparation(specimenItem, collectionObject, collectionId);
            addPreparation(preparation);

            // merge collectionobjectattr/collectingtrip; warn if changed
            updateContainerStr(specimenItem);

            // merge replicates; warn if changed
            updateReplicates(specimenItem);

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
        replicates      = null;
        vernacularName  = null;
        distribution    = null;
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
        
        // create attributedef for provenance preparationattr
        Short stringDataType = AttributeIFace.FieldType.StringType.getType();
        Short prepTableType = GenericDBConversion.TableType.Preparation.getType();

        provenanceAttrDef = getAttributeDef(stringDataType, prepTableType, ProvenanceFieldName);
        
        String sql = getInsertSql(provenanceAttrDef);
        Integer provAttrDefId = insert(sql);
        provenanceAttrDef.setAttributeDefId(provAttrDefId);
        
        voucherAttrDef = getAttributeDef(stringDataType, prepTableType, VoucherFieldName);
     
        sql = getInsertSql(voucherAttrDef);
        Integer vouchAttrDefId = insert(sql);
        voucherAttrDef.setAttributeDefId(vouchAttrDefId);
        
        referenceAttrDef = getAttributeDef(stringDataType, prepTableType, ReferenceFieldName);
        
        sql = getInsertSql(referenceAttrDef);
        Integer refAttrDefId = insert(sql);
        referenceAttrDef.setAttributeDefId(refAttrDefId);
        
        reproStatusAttrDef = getAttributeDef(stringDataType, prepTableType, ReproStatusFieldName);
        
        sql = getInsertSql(reproStatusAttrDef);
        Integer reproStatusAttrDefId = insert(sql);
        reproStatusAttrDef.setAttributeDefId(reproStatusAttrDefId);
        
        cryptoStatusAttrDef = getAttributeDef(stringDataType, prepTableType, CryptoStatusFieldName);
        
        sql = getInsertSql(cryptoStatusAttrDef);
        Integer cryptoStatusAttrDefId = insert(sql);
        cryptoStatusAttrDef.setAttributeDefId(cryptoStatusAttrDefId);
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
        getLogger().info("Creating sample number index");
        String sql =  "create index samplenum on preparation(SampleNumber)";
        execute(sql);
        
        getLogger().info("Creating alt catalog number index");
        sql =  "create index altcatnum on collectionobject(AltCatalogNumber)";
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
		if (columns.length < 45)
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
            specimenItem.setSeriesId(        SqlUtils.parseInt( columns[30] ));
            specimenItem.setSeriesName(                         columns[31] );
            specimenItem.setSiteId(          SqlUtils.parseInt( columns[32] ));
            specimenItem.setCollectorId(     SqlUtils.parseInt( columns[33] ));
            specimenItem.setCatalogedById(   SqlUtils.parseInt( columns[34] ));
            specimenItem.setFormat(                             columns[35] );
            specimenItem.setSeriesAbbrev(                       columns[36] );
            specimenItem.setSeriesNo(                           columns[37] );
            specimenItem.setContainer(                          columns[38] );
            specimenItem.setSubcollectionId( SqlUtils.parseInt( columns[39] ));
            specimenItem.setHasExsiccata( Boolean.parseBoolean( columns[40] ));
            specimenItem.setReplicates(      SqlUtils.parseInt( columns[41] ));
            specimenItem.setLocation(                           columns[42] );
            specimenItem.setVernacularName(                     columns[43] );
            specimenItem.setDistribution(                       columns[44] );
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
                collObjAttr.setText3(containerStr);
            }
            else
            {
                collectingTrip = getCollectingTrip(specimenItem);
                collectingEvent.setCollectingTrip(collectingTrip);
            }
        } 
	}

	private void updateReplicates(SpecimenItem specimenItem) throws LocalException
	{
	    Integer newReplicates = specimenItem.getReplicates();
	    if (newReplicates != null && !newReplicates.equals(replicates))
	    {
	        if (replicates != null) getLogger().warn(rec() + "Changing replicates from " + replicates + " to " + newReplicates);
	        replicates = newReplicates;
	        collectionObject.setNumber1((float) replicates);
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

	private Container lookupContainer(Integer subcollectionId) throws LocalException
	{
	    return subcollLookup.getContainerById(subcollectionId);
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
	    if (preparation != null) preparations.add(preparation);
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
	
	private Preparation getPreparation(SpecimenItem specimenItem, CollectionObject collectionObject, Integer collectionMemberId)
		throws LocalException
	{
        if (specimenItem.getId() == null) return null;
        
        Preparation preparation = new Preparation();
        preparation.setPreparationAttrs(new HashSet<PreparationAttr>());
        
	    // CollectionMemberId
	    preparation.setCollectionMemberId(collectionMemberId);
	    
	    // CollectionObject
	    preparation.setCollectionObject(collectionObject);
	    
	    // CountAmt: this needs to be set so that the create loan preparation logic works
	    preparation.setCountAmt(Integer.valueOf(1));

	    // Number1 (itemNo)
        Integer itemNo = specimenItem.getItemNo();
        preparation.setNumber1((float) itemNo);
        
        // Number2 (specimenItemId) TODO: temporary
        Integer specimenItemId = specimenItem.getId();
        preparation.setNumber2((float) specimenItemId);
        
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
	    }
	    else
	    {
	        preparation.setSampleNumber(getPreparationLookup().formatPrepBarcode(barcode));
	    }
        
	    // StorageLocation (location/temp location)
	    String location = specimenItem.getLocation();
	    if (location != null) location = truncate(location, 50, "location");
	    preparation.setStorageLocation(location);

	    // Text1 (herbariumCode)
	    String herbarium = specimenItem.getHerbariumCode();
	    preparation.setText1(herbarium);

	    // Text2 (sex)
	    String sex = specimenItem.getSex();
	    preparation.setText2(sex);

	    // YesNo1 (isOversize)
        Boolean isOversize = specimenItem.isOversize();
        preparation.setYesNo1(isOversize);
        
        // add PreparationAttr for provenances
        String provenanceStr = specimenItem.getProvenance();
        if (provenanceStr != null)
        {
            String[] provenances = provenanceStr.split(";");

            for (String provenance : provenances)
            {
                PreparationAttr provAttr = getPreparationAttr(getProvenanceAttrDef(), collectionMemberId, preparation, provenance.trim());
                
                preparation.getPreparationAttrs().add(provAttr);
            }
        }
        
        // add PreparationAttr for voucher
        String voucher = specimenItem.getVoucher(); // TODO: investigate specimen.voucher
        if (voucher != null)
        {
            PreparationAttr vouchAttr = getPreparationAttr(getVoucherAttrDef(), collectionMemberId, preparation, voucher.trim());
            preparation.getPreparationAttrs().add(vouchAttr);
        }
        
        // add PreparationAttr for reference
        String reference = specimenItem.getReference(); // TODO: investigate specimen.reference
        if (reference != null)
        {
            PreparationAttr refAttr = getPreparationAttr(getReferenceAttrDef(), collectionMemberId, preparation, reference.trim());
            preparation.getPreparationAttrs().add(refAttr);
        }

        // add PreparationAttr for repro Status
        REPRO_STATUS reproStatus = specimenItem.getReproStatus();
        if (reproStatus != null)
        {
            PreparationAttr reproStatusAttr = getPreparationAttr(getReproStatusAttrDef(), collectionMemberId, preparation, SpecimenItem.toString(reproStatus));
            preparation.getPreparationAttrs().add(reproStatusAttr);
        }
        
        return preparation;
	}

	private AttributeDef getProvenanceAttrDef()
	{
	    return provenanceAttrDef;
	}

	private AttributeDef getVoucherAttrDef()
    {
        return voucherAttrDef;
    }
	
	private AttributeDef getReferenceAttrDef()
    {
        return referenceAttrDef;
    }
	
	private AttributeDef getReproStatusAttrDef()
	{
	    return reproStatusAttrDef;
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

        // AltCatalogNumber
        Integer specimenId = specimenItem.getSpecimenId();
        checkNull(specimenId, "specimen id");
        
        String altCatalogNumber = getAltCatalogNumber(specimenId);
        collectionObject.setAltCatalogNumber(altCatalogNumber);
        
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
        Integer barcode = null;

        Integer specimenItemId = specimenItem.getId();
        if (specimenItemId != null)
        {
            barcode = lookupBarcode(specimenItemId);
            if (barcode == null) barcode = specimenItem.getBarcode();
        }
        else
        {
            getLogger().warn(rec() + "Null specimen item");
            barcode = nextBarcode();
        }

        checkNull(barcode, "barcode");
        
        String catalogNumber = getPreparationLookup().formatCollObjBarcode(barcode);
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
        
        // Number1
        Integer replicates = specimenItem.getReplicates();
        if (replicates != null) collectionObject.setNumber1((float) replicates);
        
         // Remarks
        String remarks = specimenItem.getRemarks();
        collectionObject.setRemarks(remarks);
        
        // Text1 (host)
          
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

	private CollectionObjectAttribute getCollObjAttr(SpecimenItem specimenItem, Integer collectionMemberId) throws LocalException
	{
	    CollectionObjectAttribute collObjAttr = new CollectionObjectAttribute();
	    
	    // CollectionMemberID
	    collObjAttr.setCollectionMemberId(collectionMemberId);

	    // Text1 (vernacular name)
	    vernacularName = specimenItem.getVernacularName();
	    collObjAttr.setText1(vernacularName);
	    
	    // Text2 (distribution)
	    distribution = specimenItem.getDistribution();
	    collObjAttr.setText2(distribution);
	    
	    // Text3 (container)
	    containerStr = specimenItem.getContainer();
	    if (!specimenItem.hasCollectingTrip()) collObjAttr.setText3(containerStr);
	    
	    return collObjAttr;
	}
   
    private AttributeDef getAttributeDef(Short dataType, Short tableType, String fieldName) throws LocalException
    {
        AttributeDef attributeDef = new AttributeDef();
        
        attributeDef.setDataType(dataType);
        attributeDef.setDiscipline(getBotanyDiscipline());
        attributeDef.setFieldName(fieldName);
        attributeDef.setTableType(tableType);
        
        return attributeDef;
    }
    
    private PreparationAttr getPreparationAttr(AttributeDef attrdef, Integer collectionMemberId, Preparation preparation, String strValue)
    {
        PreparationAttr prepAttr = new PreparationAttr();

        prepAttr.setDefinition(attrdef);
        prepAttr.setCollectionMemberId(collectionMemberId);
        prepAttr.setPreparation(preparation);
        prepAttr.setStrValue(strValue);
        
        return prepAttr;
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
            
            String collectingTripName = truncate(container, 64, "collecting trip name");
            
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
	
	private int nextBarcode()
	{
	    return nextBarcode++;
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
                container = lookupContainer(subcollectionId);

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

        // Remarks
        series.setRemarks("[series]");

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
	    otherIdentifier.setRemarks("[accession]");
	    
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
							"ContainerID, CreatedByAgentID, Description, FieldNumber, Number1, Remarks, Text1, " +
							"Text2, TimestampCreated, Version, YesNo1";

		String[] values = new String[20];
		
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
		values[13] = SqlUtils.sqlString( collectionObject.getNumber1());
		values[14] = SqlUtils.sqlString( collectionObject.getRemarks());
		values[15] = SqlUtils.sqlString( collectionObject.getText1());
		values[16] = SqlUtils.sqlString( collectionObject.getText2());
		values[17] = SqlUtils.sqlString( collectionObject.getTimestampCreated());
        values[18] = SqlUtils.zero();
		values[19] = SqlUtils.sqlString( collectionObject.getYesNo1());		

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

    private String getInsertSql(Preparation preparation) throws LocalException
	{
		String fieldNames = "CollectionMemberID, CollectionObjectID, CountAmt, Number1, Number2, " +
				            "PrepTypeID, Remarks, SampleNumber, StorageLocation, Text1, Text2, " +
				            "TimestampCreated, Version, YesNo1";

		String[] values = new String[14];
		
		values[0]  = SqlUtils.sqlString( preparation.getCollectionMemberId());
		values[1]  = SqlUtils.sqlString( preparation.getCollectionObject().getId());
		values[2]  = SqlUtils.sqlString( preparation.getCountAmt());
		values[3]  = SqlUtils.sqlString( preparation.getNumber1());
		values[4]  = SqlUtils.sqlString( preparation.getNumber2());
		values[5]  = SqlUtils.sqlString( preparation.getPrepType().getId());
		values[6]  = SqlUtils.sqlString( preparation.getRemarks());
		values[7]  = SqlUtils.sqlString( preparation.getSampleNumber());
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

    // See edu.ku.brc.specify.conversion.GenericDBConversion
    private String getInsertSql(AttributeDef attributeDef)
    {
        String fieldNames = "DataType, DisciplineID, FieldName, TableType, TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( attributeDef.getDataType());
        values[1] = SqlUtils.sqlString( attributeDef.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( attributeDef.getFieldName());
        values[3] = SqlUtils.sqlString( attributeDef.getTableType());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("attributedef", fieldNames, values);
    }
	
    private String getInsertSql(PreparationAttr preparationAttr)
    {
        String fieldNames = "AttributeDefID, CollectionMemberID, PreparationId, " +
        		            "StrValue, TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( preparationAttr.getDefinition().getId());
        values[1] = SqlUtils.sqlString( preparationAttr.getCollectionMemberId());
        values[2] = SqlUtils.sqlString( preparationAttr.getPreparation().getId());
        values[3] = SqlUtils.sqlString( preparationAttr.getStrValue());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("preparationattr", fieldNames, values);
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
        
        // save the current Preparations
        for (Preparation preparation : preparations)
        {
            sql = getInsertSql(preparation);
            Integer preparationId = insert(sql);
            preparation.setPreparationId(preparationId);
            
            for (PreparationAttr prepAttr : preparation.getPreparationAttrs())
            {
                sql = getInsertSql(prepAttr);
                insert(sql);
            }
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
        preparations.clear();
        otherIdentifiers.clear();
        exsiccataItems.clear();
    }
}
