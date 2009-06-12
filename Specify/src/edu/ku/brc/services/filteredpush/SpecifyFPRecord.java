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

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.util.Set;

import edu.ku.brc.specify.datamodel.WorkbenchDataItem;
import edu.ku.brc.specify.datamodel.WorkbenchRow;
import edu.ku.brc.specify.datamodel.WorkbenchTemplateMappingItem;

public class SpecifyFPRecord implements FilteredPushRecordIFace
{
    WorkbenchRow workbenchRow;
    WorkbenchTemplateMappingItem id, barcode, collector, collectorNumber, genus, species, locality, latitude, longitude;
  
    String serverName = getResourceString("FilteredPush.NodeName");
    
    public SpecifyFPRecord(WorkbenchRow workbenchRow)
    {
        this.workbenchRow = workbenchRow;
        
        Set<WorkbenchTemplateMappingItem> wbtmis = workbenchRow.getWorkbench().getWorkbenchTemplate().getWorkbenchTemplateMappingItems();

        for (WorkbenchTemplateMappingItem wbtmi : wbtmis) {

            String workbenchColCaption = wbtmi.getCaption().toLowerCase();

            if (workbenchColCaption.equals("catalognumber"))
            {
                id = wbtmi;
            }
            else if (workbenchColCaption.equals("barcode"))
            {
                barcode = wbtmi;
            }
            else if (workbenchColCaption.equals("collectors"))
            {
                collector = wbtmi;
            }
            else if (workbenchColCaption.equals("collectornumber"))
            {
                collectorNumber = wbtmi;
            }
            else if (workbenchColCaption.equals("genus"))
            {
                genus = wbtmi;
            }
            else if (workbenchColCaption.equals("species"))
            {
                species = wbtmi;
            }
            else if (workbenchColCaption.equals("locality"))
            {
                locality = wbtmi;
            }
            else if (workbenchColCaption.equals("latitude1"))
            {
                latitude = wbtmi;
            }
            else if (workbenchColCaption.equals("longitude1"))
            {
                longitude = wbtmi;
            }
        }
    }
    
    private String getCellData(WorkbenchTemplateMappingItem wbtmi)
    {
        if (wbtmi == null) return null;
        
        WorkbenchDataItem dataItem = this.workbenchRow.getItems().get(wbtmi.getViewOrder());
        if (dataItem == null) return null;
        
        return dataItem.getCellData();
    }

    private void setCellData(WorkbenchTemplateMappingItem wbtmi, String data)
    {
        if (wbtmi == null) throw new RuntimeException("Maureen, fix this");

        this.workbenchRow.setData(data, wbtmi.getViewOrder(), true);
    }

    @Override
    public String getId()
    {
        return getCellData(this.id);
    }
    
    @Override
    public String getBarcode()
    {
        return getCellData(this.barcode);
    }

    @Override
    public String getCollector()
    {
        return getCellData(this.collector);
    }

    @Override
    public String getCollectorNumber()
    {
        return getCellData(this.collectorNumber);
    }

    @Override
    public String getGenus()
    {
        return getCellData(this.genus);
    }

    @Override
    public String getSpecies()
    {
        return getCellData(this.species);
    }

    @Override
    public String getLocality()
    {
        return getCellData(this.locality);
    }
    
    @Override
    public String getLatitude()
    {
        return getCellData(this.latitude);
    }
    
    @Override
    public String getLongitude()
    {
        return getCellData(this.longitude);
    }
    
    @Override
    public String getServerName()
    {
        return this.serverName;
    }
    
    @Override
    public void setId(String id)
    {
        setCellData(this.id, id);
    }
    
    @Override
    public void setBarcode(String barcode)
    {
        setCellData(this.barcode, barcode);
    }

    @Override
    public void setCollector(String collector)
    {
        setCellData(this.collector, collector);
    }

    @Override
    public void setCollectorNumber(String collectorNumber)
    {
        setCellData(this.collectorNumber, collectorNumber);
    }

    @Override
    public void setGenus(String genus)
    {
        setCellData(this.genus, genus);
    }

    @Override
    public void setSpecies(String species)
    {
        setCellData(this.species, species);
    }
    
    @Override
    public void setLocality(String locality)
    {
        setCellData(this.locality, locality);
    }

    @Override
    public void setLatitude(String latitude)
    {
        setCellData(this.latitude, latitude);
    }

    @Override
    public void setLongitude(String longitude)
    {
        setCellData(this.longitude, longitude);
    }

    @Override
    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }
}
