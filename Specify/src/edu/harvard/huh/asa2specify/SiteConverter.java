package edu.harvard.huh.asa2specify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Site;
import edu.ku.brc.specify.datamodel.Locality;

public class SiteConverter extends Converter {

    private static final Logger log       = Logger.getLogger( SiteConverter.class );
    private static final String UNNAMED   = "Unnamed locality";

    private static SiteConverter instance = new SiteConverter();
    
    private SiteConverter() {
        ;
    }

    public static SiteConverter getInstance() {
        return instance;
    }

    public Locality convert( Site site ) {

        if ( ! site.hasData() ) return null;

        Locality locality = new Locality();

        String elevMethod = site.getElevMethod();
        if ( elevMethod != null ) {
            if ( elevMethod.length() > 50 ) {
                log.warn( "truncating elev method" );
                elevMethod = elevMethod.substring( 0, 50 );
            }
            locality.setElevationMethod( elevMethod );
        }

        // GUID TODO: temporary, remove after import
        locality.setGuid( String.valueOf( site.getId() ) );

        // Lat1Text, Latitude1 TODO: any validity checks?
        BigDecimal latitudeA = site.getLatitudeA();
        if ( latitudeA != null ) {
            locality.setLatitude1( latitudeA );
            locality.setLat1text( String.valueOf( latitudeA ) );
        }

        // Lat2Text, Latitude2 TODO: any validity checks?
        BigDecimal latitudeB = site.getLatitudeB();
        if ( latitudeB != null ) {
            locality.setLatitude2( latitudeB );
            locality.setLat2text( String.valueOf( latitudeB ) );
        }
        
        // LatLongMethod TODO: do we want this?
        String method = site.getLatLongMethod();
        if (method != null) {
            if (method.length() > 50) {
                log.warn( "truncating lat/long method" );
                method = method.substring(0, 50);
            }
            locality.setLatLongMethod( method );
        }

        // LocalityName is a required field, but we don't use it
        locality.setLocalityName( UNNAMED );

        // Long1Text, Longitude1 TODO: any validty checks?
        BigDecimal longitudeA = site.getLongitudeA();
        if (longitudeA != null) {
            locality.setLongitude1( longitudeA );
            locality.setLong1text( String.valueOf( longitudeA ) );
        }

        // Long2Text, Longitude2 TODO: any validty checks?
        BigDecimal longitudeB = site.getLongitudeB();
        if ( longitudeB != null ) {
            locality.setLongitude2( longitudeB );
            locality.setLong2text( String.valueOf( longitudeB ) );
        }
        
        // MaxElevation TODO: validity checks?
        Integer elevTo = site.getElevTo();
        if ( elevTo != null ) {
            locality.setMaxElevation( elevTo.doubleValue() );
        }

        // MinElevation
        Integer elevFrom = site.getElevFrom();
        if ( elevFrom != null ) {
            locality.setMinElevation( elevFrom.doubleValue() );
        } 

        // Remarks seems to be the equivalent of the asa site.locality
        String remarks = site.getLocality();
        if (remarks != null) {
            locality.setRemarks( remarks );
        }

        return locality;
    }

    public String getInsertSql(Locality locality) {

        List<String> fieldNames = new ArrayList<String>(18);
        List<String>     values = new ArrayList<String>(18);

        fieldNames.add( "ElevationMethod" );
        values.add( sqlEscape(locality.getElevationMethod(), '"' ) );

        fieldNames.add( "GUID" );
        values.add( sqlEscape( locality.getGuid(), '"' ) );

        fieldNames.add( "LatLongMethod" );
        values.add( sqlEscape( locality.getLatLongMethod(), '"' ) );

        fieldNames.add( "Lat1Text" );
        values.add( sqlEscape(locality.getLat1text(), '"' ) );

        fieldNames.add( "Lat2Text" );
        values.add( sqlEscape( locality.getLat2text(), '"' ) );

        fieldNames.add( "Latitude1" );
        values.add( String.valueOf( locality.getLat1() ) );

        fieldNames.add( "Latitude2" );
        values.add( String.valueOf( locality.getLat2() ) );

        fieldNames.add( "Long1Text" );
        values.add( sqlEscape( locality.getLong1text(), '"' ) );

        fieldNames.add( "Long2Text" );
        values.add( sqlEscape( locality.getLong2text(), '"' ) );

        fieldNames.add( "Longitude1" );
        values.add( String.valueOf( locality.getLong1() ) );

        fieldNames.add( "Longitude2" );
        values.add( String.valueOf( locality.getLong2()) );

        fieldNames.add( "LocalityName" );
        values.add( sqlEscape( locality.getLocalityName(), '"' ) );

        fieldNames.add( "MaxElevation" );
        values.add( String.valueOf( locality.getMaxElevation() ) );

        fieldNames.add( "MinElevation" );
        values.add( String.valueOf( locality.getMinElevation() ) );

        fieldNames.add( "SrcLatLongUnit" );
        values.add( String.valueOf( locality.getSrcLatLongUnit() ) );

        fieldNames.add( "Remarks" );
        values.add( sqlEscape( iso8859toUtf8( locality.getRemarks() ), '"' ) );

        fieldNames.add( "DisciplineID" );
        values.add( String.valueOf( locality.getDiscipline().getId() ) );

        fieldNames.add( "TimestampCreated" );
        values.add( "now()" );

        return getInsertSql( "locality", fieldNames, values );
    }
}
