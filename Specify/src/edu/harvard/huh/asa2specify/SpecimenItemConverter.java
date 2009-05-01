package edu.harvard.huh.asa2specify;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.DateUtils;
import edu.harvard.huh.asa.SpecimenItem;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.util.Pair;

public class SpecimenItemConverter {
    
    private static final Logger log       = Logger.getLogger( SpecimenItemConverter.class );

    private static SpecimenItemConverter instance = new SpecimenItemConverter();
    
    private SpecimenItemConverter() {
        ;
    }

    public static SpecimenItemConverter getInstance() {
        return instance;
    }

    public CollectionObject convertToCollectionObject(SpecimenItem specimenItem) {
        
        CollectionObject collectionObject = new CollectionObject();
        CollectingEvent collectingEvent = createCollectingEvent(specimenItem.getCollDate());

        Integer barcode = specimenItem.getBarcode();
        String catalogNumber = (new DecimalFormat( "000000000" ) ).format( String.valueOf(barcode) );
        collectionObject.setCatalogNumber(catalogNumber);

        String collectorNo = specimenItem.getCollectorNo();
        if (collectorNo != null && collectorNo.length() > 50) {
            log.warn("truncating collector number");
            collectorNo = collectorNo.substring(0, 50);
        }
        collectionObject.setFieldNumber(collectorNo);
        
        return collectionObject;
    }
    
    public CollectingEvent convertToCollectingEvent(SpecimenItem specimenItem) {
        return null;
    }

    public Preparation convertToPreparation(SpecimenItem specimenItem) {
        return null;
    }

    // TODO: is this how we want to use the DateVerbatim fields? probably some more validity checking would be good here, too
    private CollectingEvent createCollectingEvent(BDate bdate) {
        CollectingEvent collEvent = new CollectingEvent();
        
        Integer startYear = bdate.getStartYear();
        Integer startMonth = bdate.getStartMonth();
        Integer startDay = bdate.getStartDay();
        
        if ( DateUtils.isValidSpecifyDate( startYear, startMonth, startDay ) ) {
            collEvent.setStartDate( DateUtils.getSpecifyStartDate( bdate ) );
            collEvent.setStartDatePrecision( DateUtils.getDatePrecision( startYear, startMonth, startDay ) );
        }
        else if ( DateUtils.isValidCollectionDate( startYear, startMonth, startDay ) ) {
            String startDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
            if (startDateVerbatim != null && startDateVerbatim.length() > 50) {
                log.warn("truncating start date verbatim");
                startDateVerbatim = startDateVerbatim.substring(0, 50);
            }
            collEvent.setStartDateVerbatim(startDateVerbatim);
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
            collEvent.setEndDate( DateUtils.getSpecifyEndDate( bdate ) );
            collEvent.setEndDatePrecision( DateUtils.getDatePrecision( endYear, endMonth, endDay ) );
        }
        else if ( DateUtils.isValidCollectionDate( endYear, endMonth, endDay ) ) {
            String endDateVerbatim = DateUtils.getSpecifyStartDateVerbatim( bdate );
            if (endDateVerbatim != null && endDateVerbatim.length() > 50) {
                log.warn("truncating end date verbatim");
                endDateVerbatim = endDateVerbatim.substring(0, 50);
            }
            collEvent.setStartDateVerbatim(endDateVerbatim);
        }
        
        return collEvent;
    }
}
