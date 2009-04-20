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
package edu.ku.brc.specify.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Jul 9, 2008
 *
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "spexportschemaitem")
@org.hibernate.annotations.Table(appliesTo="spexportschemaitem")
public class SpExportSchemaItem extends DataModelObjBase
{
    protected Integer               spExportSchemaItemId;
    protected String                fieldName;
    protected String                dataType;
    protected String                description;
    protected String                formatter;
    protected SpLocaleContainerItem spLocaleContainerItem;
    protected SpExportSchema        spExportSchema;
    
    /**
     * 
     */
    public SpExportSchemaItem()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#initialize()
     */
    @Override
    public void initialize()
    {
        super.init();
        spExportSchemaItemId  = null;
        fieldName             = null;
        dataType              = null;
        description           = null;
        formatter             = null;
        spLocaleContainerItem = null;
        spExportSchema        = null;
    }

    /**
     * @return the exportSchemaItemId
     */
    @Id
    @GeneratedValue
    @Column(name = "SpExportSchemaItemID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getSpExportSchemaItemId()
    {
        return spExportSchemaItemId;
    }

    /**
     * @return the fieldName
     */
    @Column(name = "FieldName", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getFieldName()
    {
        return fieldName;
    }

    /**
     * @return the dataType
     */
    @Column(name = "DataType", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getDataType()
    {
        return dataType;
    }

    /**
     * @return the description
     */
    @Column(name = "Description", unique = false, nullable = true, insertable = true, updatable = true, length = 255)
    public String getDescription()
    {
        return description;
    }

    /**
     * @return the formatter
     */
    @Column(name = "Formatter", unique = false, nullable = true, insertable = true, updatable = true, length = 32)
    public String getFormatter()
    {
        return formatter;
    }

    /**
     * @return the schemaItem
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "SpLocaleContainerItemID", unique = false, nullable = true, insertable = true, updatable = true)
    public SpLocaleContainerItem getSpLocaleContainerItem()
    {
        return spLocaleContainerItem;
    }

    /**
     * @return the exportSchema
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "SpExportSchemaID", unique = false, nullable = false, insertable = true, updatable = true)
    public SpExportSchema getSpExportSchema()
    {
        return spExportSchema;
    }

    /**
     * @param exportSchemaItemId the exportSchemaItemId to set
     */
    public void setSpExportSchemaItemId(Integer exportSchemaItemId)
    {
        this.spExportSchemaItemId = exportSchemaItemId;
    }

    /**
     * @param fieldName the fieldName to set
     */
    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    /**
     * @param dataType the dataType to set
     */
    public void setDataType(String dataType)
    {
        this.dataType = dataType;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @param formatter the formatter to set
     */
    public void setFormatter(String formatter)
    {
        this.formatter = formatter;
    }

    /**
     * @param schemaItem the schemaItem to set
     */
    public void setSpLocaleContainerItem(SpLocaleContainerItem schemaItem)
    {
        this.spLocaleContainerItem = schemaItem;
    }

    /**
     * @param spExportSchema the spExportSchema to set
     */
    public void setSpExportSchema(SpExportSchema spExportSchema)
    {
        this.spExportSchema = spExportSchema;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getDataClass()
     */
    @Override
    @Transient
    public Class<?> getDataClass()
    {
        return SpExportSchemaItem.class;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getId()
     */
    @Override
    @Transient
    public Integer getId()
    {
        return spExportSchemaItemId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getTableId()
     */
    @Override
    @Transient
    public int getTableId()
    {
        return getClassTableId();
    }
    
    /**
     * @return the Table ID for the class.
     */
    public static int getClassTableId()
    {
        return 525;
    }
}
