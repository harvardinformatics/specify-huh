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
package edu.ku.brc.specify.rstools;

import java.util.ArrayList;
import java.util.List;

public class ExportRow
{
    List<ExportField>   fields;
    List<ExportDataSet> relatedSets;
    ExportDataSet       dataSet;
    
    public ExportRow()
    {
        this.fields = new ArrayList<ExportField>();
        this.relatedSets = new ArrayList<ExportDataSet>();
    }

    public void addExportField(ExportField field)
    {
        this.fields.add(field);
    }
    
    public void addRelatedSet(ExportDataSet relatedSet)
    {
        this.relatedSets.add(relatedSet);
    }

    public void setDataSet(ExportDataSet dataSet)
    {
        this.dataSet = dataSet;
    }
    
    public List<ExportField> getExportFields()
    {
        return fields;
    }

    public List<ExportDataSet> getRelatedSets()
    {
        return relatedSets;
    }
    
    public ExportDataSet getDataSet()
    {
        return dataSet;
    }
}
