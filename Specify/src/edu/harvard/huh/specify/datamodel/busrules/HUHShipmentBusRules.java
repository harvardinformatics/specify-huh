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
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.busrules.LoanGiftShipmentBusRules;
import edu.ku.brc.ui.UIRegistry;

public class HUHShipmentBusRules extends LoanGiftShipmentBusRules
{
    private final String ISACKNOWLEDGED  = "isAcknowledged";
	
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
            if (formViewObj.getParentDataObj() != null && formViewObj.getParentDataObj() instanceof Loan) {
            	formViewObj.getControlById(ISACKNOWLEDGED).setVisible(false);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
    	STATUS status = super.processBusinessRules(dataObj);
    	
    	if (!STATUS.OK.equals(status)) return status;
    	
        reasonList.clear();

        Shipment shipment = (Shipment) dataObj;
        
        Agent shippedToAgent = shipment.getShippedTo();
        if (shippedToAgent != null && shippedToAgent.getOrganization() != null) shippedToAgent = shippedToAgent.getOrganization();
        
        if (!areAddressesOK(shippedToAgent))
        {
        	reasonList.add(UIRegistry.getLocalizedMessage("SHIPPEDTO_MULT_ADDR"));
        	return STATUS.Error;
        }
        
        return STATUS.OK;
    }
    
    private boolean areAddressesOK(Agent shippedToAgent)
    {
    	if (shippedToAgent != null)
        {
        	int currentShippingAddressCount = 0;
        	for (Address address : shippedToAgent.getAddresses())
        	{
        		if (address.getIsCurrent() != null && address.getIsCurrent() &&
        		        address.getIsShipping() != null && address.getIsShipping()) currentShippingAddressCount++;

        		if (currentShippingAddressCount > 1) return false;
        	}
        }
    	return true;
    }
}
