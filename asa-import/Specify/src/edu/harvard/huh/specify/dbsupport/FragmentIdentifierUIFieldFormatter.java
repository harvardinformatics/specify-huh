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
package edu.harvard.huh.specify.dbsupport;

import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterField;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.ui.BaseUIFieldFormatter;
import edu.ku.brc.ui.UIRegistry;

/**
 * This class is used for formatting barcodes.
 * 
 * @author mmk
 *
 * @code_status Beta
 *
 * Apr 1, 2010
 *
 */
public class FragmentIdentifierUIFieldFormatter extends BaseUIFieldFormatter implements UIFieldFormatterIFace
{
    /**
     * 
     */
    public FragmentIdentifierUIFieldFormatter()
    {
        super();
        this.name          = "HUHFragmentBarcode"; //$NON-NLS-1$
        this.title         = UIRegistry.getResourceString("FragmentIdentifierUIFieldFormatter.NumericFormatter"); //$NON-NLS-1$;
        this.isIncrementer = false;
        this.length        = 8;
        this.uiLength      = length;
        this.isNumericCatalogNumber = false;
        this.autoNumber    = null;
        
        pattern = UIFieldFormatterMgr.getFormatterPattern(isIncrementer, UIFieldFormatterField.FieldType.numeric, length);
        
        field      = new UIFieldFormatterField(UIFieldFormatterField.FieldType.numeric, length, pattern, false, false); 
        fields     = new Vector<UIFieldFormatterField>();
        fields.add(field);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#formatOutBound(java.lang.Object)
     */
    public Object formatFromUI(final Object data)
    {
        if (data != null && data instanceof String && StringUtils.isNumeric((String)data))
        {
            String dataStr = (String)data;
            if (StringUtils.isNotEmpty(dataStr))
            {
                if (dataStr.equals(pattern))
                {
                    return pattern;
                }
                String fmtStr = "%0" + length + "d"; //$NON-NLS-1$ //$NON-NLS-2$
                return String.format(fmtStr, Integer.parseInt((String)data));
            }
        }
        return data;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.ui.BaseUIFieldFormatter#getDataClass()
     */
    @Override
    public Class<?> getDataClass()
    {
        return Fragment.class;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#getFieldName()
     */
    @Override
    public String getFieldName()
    {
        return "identifier"; //$NON-NLS-1$
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#getMaxValue()
     */
    @Override
    public Number getMaxValue()
    {
        return 99999999;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#getMinValue()
     */
    @Override
    public Number getMinValue()
    {
        return 0;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#getSample()
     */
    @Override
    public String getSample()
    {
        return "01234567"; //$NON-NLS-1$
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#isInBoundFormatter()
     */
    @Override
    public boolean isInBoundFormatter()
    {
        return true;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#isNumeric()
     */
    @Override
    public boolean isNumeric()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#isUserInputNeeded()
     */
    @Override
    public boolean isUserInputNeeded()
    {
        return true;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.UIFieldFormatterIFace#isValid(java.lang.String)
     */
    @Override
    public boolean isValid(String value)
    {
        return StringUtils.isNotEmpty(value) && 
        value.length() == length && 
        StringUtils.isNumeric(value);
    }
}
