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

public interface FilteredPushRecordIFace
{
    public String getId();
    
    public String getBarcode();
    
    public String getCollector();
    
    public String getCollectorNumber();
    
    public String getGenus();
    
    public String getSpecies();
    
    public String getLocality();
    
    public String getLatitude();
    
    public String getLongitude();
    
    public String getServerName();
    
    public void setId(String id);
    
    public void setBarcode(String barcode);
    
    public void setCollector(String collector);
    
    public void setCollectorNumber(String collectorNumber);
    
    public void setGenus(String genus);
    
    public void setSpecies(String species);
    
    public void setLocality(String locality);
    
    public void setLatitude(String latitude);
    
    public void setLongitude(String longitude);
    
    public void setServerName(String serverName);
}
