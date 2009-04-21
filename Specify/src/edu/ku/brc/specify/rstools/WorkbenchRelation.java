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

import edu.ku.brc.specify.datamodel.Workbench;
import edu.ku.brc.specify.datamodel.WorkbenchTemplateMappingItem;

public class WorkbenchRelation
{
    private Workbench                    workbench;
    private WorkbenchTemplateMappingItem keyColumn;
    private Workbench                    relatedWorkbench;
    private WorkbenchTemplateMappingItem foreignKeyColumn;
    
    public WorkbenchRelation(Workbench workbench, WorkbenchTemplateMappingItem keyColumn,
                             Workbench relatedWorkbench, WorkbenchTemplateMappingItem foreignKeyColumn)
    {
        this.workbench        = workbench;
        this.keyColumn        = keyColumn;
        this.relatedWorkbench = relatedWorkbench;
        this.foreignKeyColumn = foreignKeyColumn;
    }
    
    public Workbench getWorkbench() {
        return this.workbench;
    }
    
    public WorkbenchTemplateMappingItem getKeyColumn() {
        return this.keyColumn;
    }

    public Workbench getRelatedWorkbench() {
        return this.relatedWorkbench;
    }
    
    public WorkbenchTemplateMappingItem getForeignKeyColumn() {
        return this.foreignKeyColumn;
    }
}
