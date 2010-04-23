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
package edu.harvard.huh.specify.datamodel.busrules;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.PreparationBusRules;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Feb 11, 2008
 *
 */
public class HUHPreparationBusRules extends PreparationBusRules
{
    public HUHPreparationBusRules()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(Object dataObj)
    {
        STATUS status = super.processBusinessRules(dataObj);
        
        if (!STATUS.OK.equals(status)) return status;
        
        return isBarcodeOK((FormDataObjIFace) dataObj);
    }
    
    /** Either the Fragment or its Preparation must have a barcode, and the barcode
     *  must be unique across the union of all Fragment and Preparation objects .
     * @param dataObj the Fragment to check
     * @return whether the Fragment or its Preparation has a unique barcode
     */
    protected STATUS isBarcodeOK(final FormDataObjIFace dataObj)
    {
        Class<?> dataClass = Preparation.class;
        
        String fieldName = "identifier";
        String fragmentBarcode = (String)FormHelper.getValue(dataObj, fieldName);

        // Let's check for duplicates 
        Integer fragmentCount = getCountSql(Fragment.class, "fragmentId", fieldName, fragmentBarcode, null);
        if (fragmentCount == null) fragmentCount = 0;

        Integer prepCount = getCountSql(dataClass, "preparationId", fieldName, fragmentBarcode, dataObj.getId());
        if (prepCount == null) prepCount = 0;

        if (fragmentCount + prepCount == 0)
        {
            return STATUS.OK;
        }
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getByClassName(dataClass.getName());
        DBFieldInfo fieldInfo = tableInfo.getFieldByName(fieldName);

        if (fieldInfo != null && fieldInfo.getFormatter() != null)
        {
            Object fmtObj = fieldInfo.getFormatter().formatToUI(fragmentBarcode);
            if (fmtObj != null)
            {
                fragmentBarcode = fmtObj.toString();
            }
        }
        reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", dataClass, fieldName, fragmentBarcode));
        return STATUS.Error;
    }
    
    // TODO: combine this with HUHFragmentBusRules
    private int getCountSql(final Class<?> dataClass, final String primaryFieldName, final String fieldName, final String fieldValue, final Integer id)
    {
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getByClassName(dataClass.getName());
        DBFieldInfo primaryFieldInfo = tableInfo.getFieldByName(primaryFieldName);
        
        String  countColumnName = null;
        if (primaryFieldInfo != null)
        {
           countColumnName = primaryFieldInfo.getColumn(); 
        } else
        {
            if (tableInfo.getIdFieldName().equals(primaryFieldName))
            {
                countColumnName = tableInfo.getIdColumnName();
            }
        }
        
        DBFieldInfo fieldInfo = tableInfo.getFieldByName(fieldName);
        String quote = fieldInfo.getDataClass() == String.class || fieldInfo.getDataClass() == Date.class ? "'" : "";
        
        String tableName = tableInfo.getName();
        String compareColumName = fieldInfo.getColumn();
        String sql = String.format("SELECT COUNT(%s) FROM %s WHERE %s = %s", countColumnName, tableName, compareColumName, quote + fieldValue + quote);

        if (id != null)
        {
            sql += " AND " + countColumnName + " <> " + id;
        }

        String special = QueryAdjusterForDomain.getInstance().getSpecialColumns(tableInfo, false);
        sql += StringUtils.isNotEmpty(special) ? (" AND "+special) : "";
        
        log.debug(sql);
        
        return BasicSQLUtils.getCount(sql);
    }
    
    @Override
    public boolean shouldCloneField(String fieldName)
    {
        // TODO Auto-generated method stub
        return super.shouldCloneField(fieldName);
    }
}
