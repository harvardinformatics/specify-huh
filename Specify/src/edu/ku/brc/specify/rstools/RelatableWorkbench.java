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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ku.brc.specify.datamodel.Workbench;
import edu.ku.brc.specify.datamodel.WorkbenchDataItem;
import edu.ku.brc.specify.datamodel.WorkbenchRow;
import edu.ku.brc.specify.datamodel.WorkbenchTemplateMappingItem;
import edu.ku.brc.specify.tasks.WorkbenchTask;

public class RelatableWorkbench
{
    Workbench workbench;
    Set<WorkbenchRelation> relations;
    
    public RelatableWorkbench(Workbench workbench)
    {
        this.workbench = workbench;
        this.relations = new HashSet<WorkbenchRelation>();
    }
    
    public void relate(WorkbenchTemplateMappingItem keyColumn, Workbench relatedWorkbench,
                       WorkbenchTemplateMappingItem foreignKeyColumn)
    {
        this.relations.add(new WorkbenchRelation(this.workbench, keyColumn, relatedWorkbench, foreignKeyColumn));
    }
    
    public Workbench getWorkbench()
    {
        return this.workbench;
    }
    
    public List<Workbench> getRelatedWorkbenches()
    {
        List<Workbench> results = new ArrayList<Workbench>();
        
        for (WorkbenchRelation relation : this.relations)
        {
            results.add(relation.getRelatedWorkbench());
        }
        
        return results;
    }
    
    private WorkbenchRelation getRelation(Workbench relatedWorkbench)
    {
        for (WorkbenchRelation relation : this.relations)
        {
            if (relation.getRelatedWorkbench().equals(relatedWorkbench))
            {
                return relation;
            }
        }
        return null;
    }

    public List<WorkbenchRow> getRelatedRows(WorkbenchRow row, Workbench relatedWorkbench)
    {
        List<WorkbenchRow> results = new ArrayList<WorkbenchRow>();
        
        WorkbenchRelation relation = getRelation(relatedWorkbench);
        
        if (relation != null)
        {
            String key = null; 
            for (WorkbenchDataItem item : row.getWorkbenchDataItems())
            {
                if (item.getWorkbenchTemplateMappingItem().equals(relation.getKeyColumn()))
                {
                    key = item.getCellData();
                }
            }
            
            if (key != null)
            {
                for (WorkbenchRow rRow : relatedWorkbench.getWorkbenchRowsAsList())
                {
                    for (WorkbenchDataItem rItem : rRow.getWorkbenchDataItems())
                    {
                        if (rItem.getWorkbenchTemplateMappingItem().equals(relation.getForeignKeyColumn()))
                        {
                            if (rItem.getCellData().equals(key))
                            {
                                results.add(rRow);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }
    
    
    public static ExportRow createExportRow(WorkbenchRow row)
    {
        List<WorkbenchDataItem> items = new ArrayList<WorkbenchDataItem>();
        items.addAll(row.getWorkbenchDataItems());
        Collections.sort(items);
        
        ExportRow eRow = new ExportRow();

        for (WorkbenchDataItem item : items)
        {
            eRow.addExportField(createExportField(item));
        }
        
         return eRow;
    }
    
    public static ExportField createExportField(WorkbenchDataItem item)
    {
        WorkbenchTemplateMappingItem mapping = item.getWorkbenchTemplateMappingItem(); 
        
        ExportField field = new ExportField();
        
        field.setData(item.getCellData());                      
        field.setCaption(mapping.getCaption());
        field.setFieldName(mapping.getFieldName());
        field.setTableName(mapping.getTableName());
        field.setType(WorkbenchTask.getDataType(mapping));
        
        return field;
    }
    
    public static ExportDataSet createExportDataSet(RelatableWorkbench rwb)
    {
        Workbench workbench = rwb.getWorkbench();
        
        ExportDataSet dataSet = new ExportDataSet();
        dataSet.setName(workbench.getName());

        for (WorkbenchRow wbRow : workbench.getWorkbenchRowsAsList())
        {
            ExportRow eRow = createExportRow(wbRow);

            dataSet.addExportRow(eRow);

            for (Workbench relatedWorkbench : rwb.getRelatedWorkbenches())
            {
                ExportDataSet rDataSet = new ExportDataSet();
                rDataSet.setName(relatedWorkbench.getName());

                for (WorkbenchRow rwbRow : rwb.getRelatedRows(wbRow, relatedWorkbench))
                {
                    rDataSet.addExportRow(createExportRow(rwbRow));
                }

                eRow.addRelatedSet(rDataSet);
            }
        }
        return dataSet;
    }

    public static ExportDataSet createExportDataSet(Workbench workbench)
    {
        ExportDataSet dataSet = new ExportDataSet();
        dataSet.setName(workbench.getName());

        for (WorkbenchRow wbRow : workbench.getWorkbenchRowsAsList())
        {
            dataSet.addExportRow(createExportRow(wbRow));
        }
        return dataSet;
    }
    
    public static ExportDataSet createExportDataSet(List<WorkbenchRow> wbRows) {
        ExportDataSet dataSet = new ExportDataSet();
        
        if (wbRows.size() > 0)
        {
            dataSet.setName(wbRows.get(0).getWorkbench().getName());

            for (WorkbenchRow wbRow : wbRows)
            {
                dataSet.addExportRow(createExportRow(wbRow));
            }
        }
        return dataSet;
    }
}
