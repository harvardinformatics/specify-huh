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

public class ExportField
{
    String caption, data, tableName, fieldName;
    Class<?> type;
    
    public ExportField()
    {
        ;
    }
    
    public void setCaption(String caption)
    {
        this.caption = caption;
    }
    
    public void setData(String data)
    {
        this.data = data;
    }

    public void setType(Class<?> type)
    {
        this.type = type;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getCaption()
    {
        return caption;
    }
    
    public String getData()
    {
        return data;
    }

    public Class<?> getType()
    {
        return type;
    }
    
    public String getTableName()
    {
        return tableName;
    }
    
    public String getFieldName()
    {
        return fieldName;
    }
}
