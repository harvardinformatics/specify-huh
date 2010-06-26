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
package edu.harvard.huh.specify.ui;

import java.util.Vector;

import edu.ku.brc.af.ui.db.PickListIFace;
import edu.ku.brc.af.ui.db.PickListItemIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatter;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterField;
import edu.ku.brc.specify.ui.db.PickListDBAdapterFactory;


/**
 * This base class is intended to be used for express search results formatting of fields
 * that are integer values with corresponding picklist labels.  The formatToUI method
 * looks up the integer value and returns the corresponding label.  The object passed to
 * formatToUI may be an integer or a string representation of an integer.
 * 
 * Since the format manager class only keeps one instance of a UIFIeldFormatter object
 * in memory at a time (per name), and because it is difficult to determine the name of
 * the required pick list dynamically from the express search code, it is expected that
 * this class be subclassed: one subclass per picklist.
 * 
 * The subclass obj should initialize itself with the proper picklist, and it should also
 * have a distinct name in uiformatters.xml so that it can be referenced in the appropriate
 * place in search.config.xml.
 * 
 * This is not ideal but good enough for the time being.
 * 
 * @author mmk
 *
 * @code_status Alpha
 *
 * Jun 24, 2010
 *
 */
public class PickListFormatter extends UIFieldFormatter
{
    
    PickListIFace pickList = null;
    
    /**
     * 
     */
    public PickListFormatter()
    {
        super();
    }

    /**
     * @param name
     * @param isSystem
     * @param fieldName
     * @param type
     * @param partialDateType
     * @param dataClass
     * @param isDefault
     * @param isIncrementer
     * @param fields
     */
    public PickListFormatter(final String name, 
                                final boolean isSystem, 
                                final String fieldName,
                                final FormatterType type, 
                                final PartialDateEnum partialDateType, 
                                final Class<?> dataClass,
                                final boolean isDefault, 
                                final boolean isIncrementer, 
                                final Vector<UIFieldFormatterField> fields)
    {
        super(name, isSystem, fieldName, type, partialDateType, dataClass, isDefault, isIncrementer, fields);

    }

    protected PickListIFace getPickList()
    {
        return this.pickList;
    }

    protected void setPickList(PickListIFace pickList)
    {
        this.pickList = pickList;
    }

    protected PickListIFace getPickList(String pickListName) 
    {
        if (pickListName != null)
        {
            return PickListDBAdapterFactory.getInstance().create(pickListName, false).getPickList();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.UIFieldFormatter#formatToUI(java.lang.Object[])
     */
    @Override
    public Object formatToUI(Object... datas)
    {
        Object value = datas[0];

        if (value == null) return "";
        
        Integer i = null;
        
        if (value instanceof String)
        {
            try
            {
                i = Integer.parseInt(value.toString());
            }
            catch (NumberFormatException nfe)
            {
                ;
            }
        }
        else if (value instanceof Integer)
        {
            i = (Integer) value;
        }
        
        if (this.pickList != null && i != null)
        {
            for (PickListItemIFace item : this.pickList.getItems())
            {
                Integer itemValue = Integer.parseInt(item.getValue());
                if (itemValue.equals(i)) return item.getTitle();
            }
        }
        
        return value.toString();

    }

    @Override
    public boolean isInBoundFormatter()
    {
        return true;
    }
    
    @Override
    public String getName()
    {
        return "PickListFormatter";
    }
}
