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
package edu.harvard.huh.specify.datamodel.busrules;

import java.util.Calendar;
import java.util.GregorianCalendar;

import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.busrules.LoanGiftShipmentBusRules;

public class HUHShipmentBusRules extends LoanGiftShipmentBusRules
{
    public HUHShipmentBusRules()
    {
        super();
    }
    
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#beforeFormFill()
     */
    @Override
    public void beforeFormFill()
    {
        super.beforeFormFill();
        
        if (formViewObj != null && formViewObj.getDataObj() instanceof Shipment)
        {
            MultiView mvParent = formViewObj.getMVParent();
            boolean   isNewObj = MultiView.isOptionOn(mvParent.getOptions(), MultiView.IS_NEW_OBJECT);
            if (isNewObj)
            {
                Shipment shipment = (Shipment) formViewObj.getDataObj();
                if (shipment.getShipmentDate() == null)
                {
                    Calendar shipmentDate = new GregorianCalendar();
                    do
                    {
                        shipmentDate.add(Calendar.DATE, 1);
                    }
                    while (shipmentDate.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY);
                    
                    shipment.setShipmentDate(shipmentDate);
                }
            }
        }
    }
}
