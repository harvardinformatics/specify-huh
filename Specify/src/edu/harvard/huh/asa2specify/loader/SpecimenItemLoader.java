package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.SpecimenItem;
import edu.harvard.huh.asa.Subcollection;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
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

public class SpecimenItemLoader extends CsvToSqlLoader
{
	private Discipline discipline;
	
	private CollectionObject     collectionObject = null;
	private String               reproStatus      = null;
	private Integer              subcollectionId  = null;
	private Collector            collector        = null;
	private CollectingEvent      collectingEvent  = null;
	private Set<Preparation>     preparations     = new HashSet<Preparation>();
	private Set<OtherIdentifier> otherIdentifiers = new HashSet<OtherIdentifier>();
	private Set<ExsiccataItem>     exsiccataItems   = null;
	
	public SpecimenItemLoader(File csvFile, Statement sqlStatement) throws LocalException 
	{
		super(csvFile, sqlStatement);
		
		this.discipline = getBotanyDiscipline();
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		
		SpecimenItem specimenItem = parse(columns);
		String specimenId = String.valueOf(specimenItem.getSpecimenId());

		// Preparation
        Preparation preparation = getPreparation(specimenItem);

        // if this preparation shares the same collection object
        // with the previously inserted one, re-use it
        if (collectionObject != null && specimenId.equals(collectionObject.getGuid()))
        {
            Integer collectionMemberId = collectionObject.getCollectionMemberId();
            
            preparation.setCollectionObject(collectionObject);
            preparation.setCollectionMemberId(collectionMemberId);
            preparations.add(preparation);

            // possibly update repro status; warn if so
            updateReproStatus(specimenItem);

            // possibly update subcollection; warn if so
            updateSubcollection(specimenItem);
            
            // TODO: connect series (organization) agent as collector?
            OtherIdentifier series = getSeriesIdentifier(specimenItem);
            if (series != null)
            {
                series.setCollectionObject(collectionObject);
                series.setCollectionMemberId(collectionMemberId);
                addOtherIdentifier(series);
            }

            // TODO: create accessions from provenance?
            OtherIdentifier accession = getAccessionIdentifier(specimenItem);
            if (accession != null)
            {
                accession.setCollectionObject(collectionObject);
                accession.setCollectionMemberId(collectionMemberId);
                addOtherIdentifier(accession);
            }
            
            // ExsiccataItem
            ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem);
            if (exsiccataItem != null)
            {
                exsiccataItem.setCollectionObject(collectionObject);
                addExsiccataItem(exsiccataItem);
            }
        }

        // If we made it here, the asa.specimen.ids are different, so we have a different
        // collection object entirely.  First, save the objects associated with the old asa.specimen.id
        if (collectionObject != null)
        {
            saveObjects();
        }

        ////////////////////////////////////////////////
        // Begin construction of new CollectionObject //
        ////////////////////////////////////////////////

        // Collection
        Collection collection = getCollection(specimenItem);
        Integer collectionMemberId = collection.getId();
        
        // CollectionObject
        collectionObject = getCollectionObject(specimenItem);
        collectionObject.setCollection(collection);
        collectionObject.setCollectionMemberId(collectionMemberId);
        
        // Preparation
        preparation.setCollectionMemberId(collectionMemberId);
        preparation.setCollectionObject(collectionObject);
        preparations.add(preparation);

        // Cataloger
        Integer createdById = specimenItem.getCatalogedById();
        Agent cataloger = getAgentByOptrId(createdById);
        
        collectionObject.setCataloger(cataloger);
        collectionObject.setCreatedByAgent(cataloger);
        
        // Collector
        collector = getCollector(specimenItem);
        collector.setCollectionMemberId(collectionMemberId);

        // CollectingEvent
        collectingEvent = getCollectingEvent(specimenItem);
        collector.setCollectingEvent(collectingEvent);
        collectionObject.setCollectingEvent(collectingEvent);

        // Locality
		Locality locality = new Locality();
		Integer siteId = specimenItem.getSiteId();

		if (siteId != null)
		{
			String guid = SqlUtils.sqlString(String.valueOf(siteId));

			// not checking for null; many localities were empty and not loaded
			Integer localityId = queryForInt("locality", "LocalityID", "GUID", guid);			 
			locality.setLocalityId(localityId);
		}
		collectingEvent.setLocality(locality);

        // Container (subcollection)
        Container container = getContainer(specimenItem);
        container.setCollectionMemberId(collectionMemberId);
        collectionObject.setContainer(container);
		
	    // TODO: connect series (organization) agent as collector?
        OtherIdentifier series = getSeriesIdentifier(specimenItem);
        if (series != null)
        {
            series.setCollectionObject(collectionObject);
            series.setCollectionMemberId(collectionMemberId);
            addOtherIdentifier(series);
        }

        // TODO: create accessions from provenance?
        OtherIdentifier accession = getAccessionIdentifier(specimenItem);
        if (accession != null)
        {
            accession.setCollectionObject(collectionObject);
            accession.setCollectionMemberId(collectionMemberId);
            addOtherIdentifier(accession);
        }
        
        // ExsiccataItem
        ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem);
        if (exsiccataItem != null)
        {
            exsiccataItem.setCollectionObject(collectionObject);
            addExsiccataItem(exsiccataItem);
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

	private void updateReproStatus(SpecimenItem specimenItem)
	{
        String newReproStatus = specimenItem.getReproStatus();
        if (!newReproStatus.equals(reproStatus))
        {
            String status;
            if (reproStatus.equals(SpecimenItem.NotDetermined) ||
                    newReproStatus.equals(SpecimenItem.FlowerAndFruit) && 
                        (reproStatus.equals(SpecimenItem.Flower) || reproStatus.equals(SpecimenItem.Fruit)))
            {
                status = newReproStatus;
            }
            else if (reproStatus.equals(SpecimenItem.Flower) && newReproStatus.equals(SpecimenItem.Fruit) ||
                        reproStatus.equals(SpecimenItem.Fruit) && newReproStatus.equals(SpecimenItem.Flower))
            {
                status = SpecimenItem.FlowerAndFruit;
            }
            else
            {
                status = reproStatus;
            }
            
            if (!reproStatus.equals(status))
            {
                warn("Changing repro status from " + reproStatus, specimenItem.getId(), status);
                collectionObject.setText1(status);
            }
        }
	}

	private void updateSubcollection(SpecimenItem specimenItem) throws LocalException
	{
        Integer newSubcollectionId = specimenItem.getSubcollectionId();
        if (!newSubcollectionId.equals(subcollectionId))
        {
            if (subcollectionId == null)
            {
                subcollectionId = newSubcollectionId;
                
                if (specimenItem.hasExsiccata())
                {
                    ExsiccataItem exsiccataItem = getExsiccataItem(specimenItem);
                    exsiccataItem.setCollectionObject(collectionObject);
                    exsiccataItems.add(exsiccataItem);
                }
                else
                {
                    Container container = getContainer(specimenItem);
                    container.setCollectionMemberId(collectionObject.getCollectionMemberId());
                    collectionObject.setContainer(container);
                }
            }
            else if (!subcollectionId.equals(newSubcollectionId))
            {
                warn("Multiple subcollections, ignoring this one", specimenItem.getSpecimenId(), String.valueOf(newSubcollectionId));
            }
        }
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

	private Collection getCollection(SpecimenItem specimenItem) throws LocalException
	{
        String herbariumCode = specimenItem.getHerbariumCode();
        
        return getCollection(herbariumCode);
	}

	private Collector getCollector(SpecimenItem specimenItem) throws LocalException
	{
	    Agent agent = new Agent();
        Integer botanistId = specimenItem.getCollectorId();
        
        if (botanistId != null)
        {
            Botanist botanist = new Botanist();
            botanist.setId(botanistId);
            String guid = botanist.getGuid();
            
            Integer agentId = getIdByField("agent", "AgentID", "GUID", guid);

            agent.setAgentId(agentId);
        }

        Collector collector = new Collector();

        collector.setAgent(agent);
        collector.setIsPrimary(true);
        collector.setOrderNumber(1);
        
        return collector;
	}
	
	private CollectingEvent getCollectingEvent(SpecimenItem specimenItem) throws LocalException
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
		// StartDateVerbatime
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
				warn("Truncating end date verbatim", specimenItem.getSpecimenId(), endDateVerbatim);
				endDateVerbatim = endDateVerbatim.substring(0, 50);
			}
			collectingEvent.setEndDateVerbatim(endDateVerbatim);
		}

	    String habitat = specimenItem.getHabitat();

	    // Remarks (habitat)
	    collectingEvent.setRemarks(habitat);
	    
	    return null;
	}
	
	private String formatBarcode(Integer barcode) throws LocalException
	{
        try
        {
            return (new DecimalFormat( "000000000" ) ).format( barcode );
        }
        catch (IllegalArgumentException e)
        {
            throw new LocalException("Couldn't parse barcode");
        }
	}
	
	private Preparation getPreparation(SpecimenItem specimenItem) throws LocalException
	{
	    Preparation preparation = new Preparation();

	    // PrepType
        String format = specimenItem.getFormat();
        PrepType prepType = getPrepType(format);
        preparation.setPrepType(prepType);
        
        // SampleNumber (barcode)
	    Integer barcode = specimenItem.getBarcode();
	    if (barcode == null)
	    {
	        throw new LocalException("Null barcode");
	    }
        preparation.setSampleNumber(formatBarcode(barcode));
        
        // Number1 (itemNo)
        Integer itemNo = specimenItem.getItemNo();
        preparation.setNumber1((float) itemNo);

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

        // Text1 (voucher)
        String voucher = specimenItem.getVoucher(); // TODO: investigate
        preparation.setText1(voucher);

        // Text2 (reference)
        String reference = specimenItem.getReference(); // TODO: investigate
        preparation.setText2(reference);

        // Text3 (note)
        String note = specimenItem.getNote();
        preparation.setRemarks(note);
        
        return preparation;
	}

	private CollectionObject getCollectionObject(SpecimenItem specimenItem) throws LocalException
	{
	    CollectionObject collectionObject = new CollectionObject();

	    // GUID
        collectionObject.setGuid(String.valueOf(specimenItem.getSpecimenId()));

        // CatalogNumber
        Integer barcode = specimenItem.getBarcode();
        if (barcode == null)
        {
            throw new LocalException("Null barcode");
        }
        String catalogNumber = formatBarcode(barcode);
        collectionObject.setCatalogNumber(catalogNumber);

        // FieldNumber
        String collectorNo = specimenItem.getCollectorNo();
        if (collectorNo != null && collectorNo.length() > 50)
        {
            warn("Truncating collector number", specimenItem.getSpecimenId(), collectorNo);
            collectorNo = collectorNo.substring(0, 50);
        }
        collectionObject.setFieldNumber(collectorNo);
        
        // TimestampCreated
        Date dateCreated = specimenItem.getCatalogedDate();
        collectionObject.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        collectionObject.setCatalogedDatePrecision( (byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal() );

        // YesNo1 (isCultivated)
        collectionObject.setYesNo1(specimenItem.isCultivated()); // TODO: implement

        // Description
        String description = specimenItem.getDescription();
        if (description != null && description.length() > 255)
        {
            warn("Truncating description", specimenItem.getSpecimenId(), description);
            description = description.substring(0, 255);
        }
        collectionObject.setDescription(description);

        // Text1 (reproStatus/phenology)
        reproStatus = specimenItem.getReproStatus();
        collectionObject.setText1(reproStatus);
          
        // Text2 (substrate)
        String substrate = specimenItem.getSubstrate();
        collectionObject.setText2(substrate);

        // Remarks
        String remarks = specimenItem.getRemarks();
        collectionObject.setRemarks(remarks);
        
        return collectionObject;
	}

	private Container getContainer(SpecimenItem specimenItem) throws LocalException
	{
	    Container container = new Container();

	    subcollectionId = specimenItem.getSubcollectionId();
        String containerStr = specimenItem.getContainer();

        if (subcollectionId != null)
        {            
            Integer containerId = getIdByField("container", "ContainerID", "Number", subcollectionId);

            container.setContainerId(containerId);    
            
            if (specimenItem.getContainer() != null)
            {
                warn("Subcollection and container present, dropping container", specimenItem.getId(), containerStr);
            }
        }
        else if (containerStr != null)
        {
            // TODO: normalizing of container and subcollection name strings.
            // Note that if the container string = subcollection.name for some subcollection (it does happen),
            // we are in effect adding that subcollection_id to the specimen_item_record
            
            Integer containerId = queryForInt("container", "ContainerID", "Name", containerStr);
            
            if (containerId == null)
            {
                // insert new Container
                container.setCollectionMemberId(collectionObject.getCollection().getId());
                container.setName(containerStr);
                
                String sql = getInsertSql(container);
                containerId = insert(sql);
            }
            container.setContainerId(containerId);
        }

        return container;
	}
	
	private OtherIdentifier getSeriesIdentifier(SpecimenItem specimenItem) throws LocalException
	{
        Integer seriesId = specimenItem.getSeriesId();
        String seriesNo = specimenItem.getSeriesNo();
        String seriesAbbrev = specimenItem.getSeriesAbbrev();

        if (seriesId == null || seriesNo == null)
        {
            if (seriesNo != null)
            {
                warn("Series number and no series id", specimenItem.getId(), seriesNo);
            }
            else if (seriesId != null)
            {
                warn("Series id and no series number", specimenItem.getId(), String.valueOf(seriesId));
            }
            
            return null;
        }

        OtherIdentifier series = new OtherIdentifier();
        
        if (seriesId != null && seriesNo != null)
        {
            Organization organization = new Organization();
            organization.setId(seriesId);
            String guid = organization.getGuid();

            String sql = SqlUtils.getQueryIdByFieldSql("agent", "LastName", "GUID", guid);
            String institution = queryForString(sql);
            
            // Institution (series-organization)
            series.setInstitution(institution);
            
            // Identifier (seriesNo)
            series.setIdentifier( (seriesAbbrev == null ? "" : seriesAbbrev + " ") + seriesNo );
        }
        
        return series;
	}

	private OtherIdentifier getAccessionIdentifier(SpecimenItem specimenItem)
	{
	    String accessionNo = specimenItem.getAccessionNo();
	    if (accessionNo == null) accessionNo = "NA";
	    
	    String provenance = specimenItem.getProvenance();
	    
	    if (accessionNo.equals("NA") && provenance == null)
	    {
	        return null;
	    }

	    OtherIdentifier otherIdentifier = new OtherIdentifier();
	    
	    otherIdentifier.setInstitution(specimenItem.getHerbariumCode());
	    otherIdentifier.setIdentifier(accessionNo);
	    otherIdentifier.setRemarks(provenance);
	    
	    return otherIdentifier;
	}

	private ExsiccataItem getExsiccataItem(SpecimenItem specimenItem) throws LocalException
	{
	    Integer subcollectionId = specimenItem.getSubcollectionId();

	    if (subcollectionId == null || !specimenItem.hasExsiccata()) return null;

	    ExsiccataItem exsiccataItem = new ExsiccataItem();
        
        Exsiccata exsiccata = new Exsiccata();

        Subcollection subcollection =  new Subcollection();
        subcollection.setId(subcollectionId);
        String guid = subcollection.getGuid();
        
        String subselect =  "(" + SqlUtils.getQueryIdByFieldSql("referencework", "ReferenceWorkID", "GUID", guid) + ")";
                
        Integer exsiccataId = getIdByField("exsiccata", "ExsiccataId", "ReferenceWorkID", subselect);

	    exsiccata.setExsiccataId(exsiccataId);
	    exsiccataItem.setExsiccata(exsiccata);

	    return exsiccataItem;
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
    
    private String getInsertSql(OtherIdentifier otherIdentifier)
    {
        String fieldNames = "CollectionMemberID, Identifier, Institution, Remarks, TimestampCreated";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( otherIdentifier.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( otherIdentifier.getIdentifier());
        values[2] = SqlUtils.sqlString( otherIdentifier.getInstitution());
        values[3] = SqlUtils.sqlString( otherIdentifier.getRemarks());
        values[4] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("otheridentifier", fieldNames, values);
    }
       
    private String getInsertSql(Container container) throws LocalException
    {
        String fieldNames = "Name, CollectionMemberID";
        
        String[] values = new String[2];
        
        values[0] = SqlUtils.sqlString( container.getName());
        values[1] = SqlUtils.sqlString( container.getCollectionMemberId());
        
        return SqlUtils.getInsertSql("container", fieldNames, values);
    }
     
    private void saveObjects() throws LocalException
    {
        String sql;  // this variable is going to be re-used.

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
        
        collectingEvent  = null;
        collector        = null;
        collectionObject = null;
        preparations.clear();
        otherIdentifiers.clear();
        exsiccataItems.clear();
    }
}
