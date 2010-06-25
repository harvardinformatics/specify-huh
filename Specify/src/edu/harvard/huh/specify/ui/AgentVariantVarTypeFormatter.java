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
package edu.harvard.huh.specify.ui;

import java.util.Vector;

import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterField;

public class AgentVariantVarTypeFormatter extends PickListFormatter
{
    public AgentVariantVarTypeFormatter()
    {
        super();
        init();
    }
    
    public AgentVariantVarTypeFormatter(String name,
                                        boolean isSystem,
                                        String fieldName,
                                        FormatterType type,
                                        PartialDateEnum partialDateType,
                                        Class<?> dataClass,
                                        boolean isDefault,
                                        boolean isIncrementer,
                                        Vector<UIFieldFormatterField> fields)
    {
        super(name, isSystem, fieldName, type, partialDateType, dataClass, isDefault,
                isIncrementer, fields);
        
        init();
    }
    
    protected void init()
    {
        this.setPickList(this.getPickList("HUH Agent Variant"));
    }
}