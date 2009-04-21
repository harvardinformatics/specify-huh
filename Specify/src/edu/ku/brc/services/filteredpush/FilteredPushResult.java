/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ku.brc.services.filteredpush;

public class FilteredPushResult implements FilteredPushRecordIFace
{
        String id, barcode, collectorNumber, collector, genus, species, locality, latitude, longitude, serverName;
    
    public FilteredPushResult() {
        // empty
    }
    
    public String getId() { return id; }
    
    public String getBarcode() { return barcode; }
    
    public String getCollectorNumber() { return collectorNumber; }
    
    public String getCollector() { return collector; }
    
    public String getGenus() { return genus; }
    
    public String getSpecies() { return species; }
    
    public String getLocality() { return locality; }
    
    public String getLatitude() { return latitude; }
    
    public String getLongitude() { return longitude; }
    
    public String getServerName() { return serverName; }
    
    public void setId(String id) { this.id = id; }
    
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public void setCollectorNumber(String collectorNumber) { this.collectorNumber = collectorNumber; }
    
    public void setCollector(String collector) { this.collector = collector; }
    
    public void setGenus(String genus) { this.genus = genus; }
    
    public void setSpecies(String species) { this.species = species; }
    
    public void setLocality(String locality) { this.locality = locality; }
    
    public void setLatitude(String latitude) { this.latitude = latitude; }
    
    public void setLongitude(String longitude) { this.longitude = longitude; }
    
    public void setServerName(String serverName) { this.serverName = serverName; }
}
