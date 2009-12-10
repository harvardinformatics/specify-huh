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
package edu.harvard.huh.specify.ui.forms;

import edu.ku.brc.af.ui.forms.DataSetterForObj;
import edu.ku.brc.specify.datamodel.AgentVariant;

public class AgentVarForAgentSetter extends DataSetterForObj
{
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.DataObjectSettable#setFieldValue(java.lang.Object, java.lang.String, java.lang.Object)
     */
    @Override
    public void setFieldValue(Object dataObj, String fieldName, Object data)
    {
        if (fieldName.equals("agent") && data instanceof AgentVariant)
        {
            super.setFieldValue(dataObj, fieldName, ((AgentVariant) data).getAgent());
        }
        else
        {
            super.setFieldValue(dataObj, fieldName, data);
        }
    }

}
