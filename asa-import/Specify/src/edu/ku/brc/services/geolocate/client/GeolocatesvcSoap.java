/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.services.geolocate.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.WebParam.Mode;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b59-fcs
 * Generated source version: 2.0
 * 
 */
@WebService(name = "geolocatesvcSoap", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
public interface GeolocatesvcSoap {


    /**
     * 
     * @param findWaterbody
     * @param hwyX
     * @param localityDescription
     * @return
     *     returns edu.ku.brc.services.geolocate.client.GeorefResultSet
     */
    @WebMethod(operationName = "Georef", action = "http://www.museum.tulane.edu/webservices/Georef") //$NON-NLS-1$ //$NON-NLS-2$
    @WebResult(name = "Result", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
    @RequestWrapper(localName = "Georef", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.Georef") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    @ResponseWrapper(localName = "GeorefResponse", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.GeorefResponse") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public GeorefResultSet georef(
        @WebParam(name = "LocalityDescription", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        LocalityDescription localityDescription,
        @WebParam(name = "HwyX", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        boolean hwyX,
        @WebParam(name = "FindWaterbody", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        boolean findWaterbody);

    /**
     * 
     * @param vLocality
     * @param findWaterbody
     * @param hwyX
     * @param vGeography
     * @return
     *     returns edu.ku.brc.services.geolocate.client.GeorefResultSet
     */
    @WebMethod(operationName = "Georef3", action = "http://www.museum.tulane.edu/webservices/Georef3") //$NON-NLS-1$ //$NON-NLS-2$
    @WebResult(name = "Result", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
    @RequestWrapper(localName = "Georef3", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.Georef3") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    @ResponseWrapper(localName = "Georef3Response", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.Georef3Response") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public GeorefResultSet georef3(
        @WebParam(name = "vLocality", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String vLocality,
        @WebParam(name = "vGeography", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String vGeography,
        @WebParam(name = "HwyX", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        boolean hwyX,
        @WebParam(name = "FindWaterbody", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        boolean findWaterbody);

    /**
     * 
     * @param county
     * @param localityString
     * @param findWaterbody
     * @param hwyX
     * @param state
     * @param country
     * @return
     *     returns edu.ku.brc.services.geolocate.client.GeorefResultSet
     */
    @WebMethod(operationName = "Georef2", action = "http://www.museum.tulane.edu/webservices/Georef2") //$NON-NLS-1$ //$NON-NLS-2$
    @WebResult(name = "Result", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
    @RequestWrapper(localName = "Georef2", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.Georef2") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    @ResponseWrapper(localName = "Georef2Response", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.Georef2Response") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public GeorefResultSet georef2(
        @WebParam(name = "Country", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String country,
        @WebParam(name = "State", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String state,
        @WebParam(name = "County", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String county,
        @WebParam(name = "LocalityString", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String localityString,
        @WebParam(name = "HwyX", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        boolean hwyX,
        @WebParam(name = "FindWaterbody", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        boolean findWaterbody);

    /**
     * 
     * @param localityDescription
     * @return
     *     returns edu.ku.brc.services.geolocate.client.ArrayOfString
     */
    @WebMethod(operationName = "FindWaterBodiesWithinLocality", action = "http://www.museum.tulane.edu/webservices/FindWaterBodiesWithinLocality") //$NON-NLS-1$ //$NON-NLS-2$
    @WebResult(name = "FindWaterBodiesWithinLocalityResult", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
    @RequestWrapper(localName = "FindWaterBodiesWithinLocality", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.FindWaterBodiesWithinLocality") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    @ResponseWrapper(localName = "FindWaterBodiesWithinLocalityResponse", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.FindWaterBodiesWithinLocalityResponse") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public ArrayOfString findWaterBodiesWithinLocality(
        @WebParam(name = "LocalityDescription", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        LocalityDescription localityDescription);

    /**
     * 
     * @param wgs84Coordinate
     * @param localityDescription
     */
    @WebMethod(operationName = "SnapPointToNearestFoundWaterBody", action = "http://www.museum.tulane.edu/webservices/SnapPointToNearestFoundWaterBody") //$NON-NLS-1$ //$NON-NLS-2$
    @RequestWrapper(localName = "SnapPointToNearestFoundWaterBody", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.SnapPointToNearestFoundWaterBody") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    @ResponseWrapper(localName = "SnapPointToNearestFoundWaterBodyResponse", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.SnapPointToNearestFoundWaterBodyResponse") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public void snapPointToNearestFoundWaterBody(
        @WebParam(name = "LocalityDescription", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        LocalityDescription localityDescription,
        @WebParam(name = "WGS84Coordinate", targetNamespace = "http://www.museum.tulane.edu/webservices/", mode = Mode.INOUT) //$NON-NLS-1$ //$NON-NLS-2$
        Holder<GeographicPoint> wgs84Coordinate);

    /**
     * 
     * @param wgs84Longitude
     * @param county
     * @param localityString
     * @param state
     * @param wgs84Latitude
     * @param country
     * @return
     *     returns edu.ku.brc.services.geolocate.client.GeographicPoint
     */
    @WebMethod(operationName = "SnapPointToNearestFoundWaterBody2", action = "http://www.museum.tulane.edu/webservices/SnapPointToNearestFoundWaterBody2") //$NON-NLS-1$ //$NON-NLS-2$
    @WebResult(name = "WGS84Coordinate", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
    @RequestWrapper(localName = "SnapPointToNearestFoundWaterBody2", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.SnapPointToNearestFoundWaterBody2") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    @ResponseWrapper(localName = "SnapPointToNearestFoundWaterBody2Response", targetNamespace = "http://www.museum.tulane.edu/webservices/", className = "edu.ku.brc.services.geolocate.client.SnapPointToNearestFoundWaterBody2Response") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public GeographicPoint snapPointToNearestFoundWaterBody2(
        @WebParam(name = "Country", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String country,
        @WebParam(name = "State", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String state,
        @WebParam(name = "County", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String county,
        @WebParam(name = "LocalityString", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        String localityString,
        @WebParam(name = "WGS84Latitude", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        double wgs84Latitude,
        @WebParam(name = "WGS84Longitude", targetNamespace = "http://www.museum.tulane.edu/webservices/") //$NON-NLS-1$ //$NON-NLS-2$
        double wgs84Longitude);

}
