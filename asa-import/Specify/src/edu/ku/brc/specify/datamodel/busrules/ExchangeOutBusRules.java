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
package edu.ku.brc.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.DraggableRecordIdentifier;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;

/**
 * @author rod; revised by mmk
 *
 * @code_status Alpha
 *
 * Apr 3, 2009
 *
 */
public class ExchangeOutBusRules extends BaseBusRules
{
    public static final String CMDTYPE   = "Interactions";
    public final String NEW_EXCHANGE_OUT = "NEW_EXCHANGE_OUT";

    /**
     * @param dataClasses
     */
    public ExchangeOutBusRules()
    {
        super(ExchangeOut.class);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#addChildrenToNewDataObjects(java.lang.Object)
     */
    @Override
    public void addChildrenToNewDataObjects(Object newDataObj)
    {
        ((ExchangeOut)newDataObj).setDivision(AppContextMgr.getInstance().getClassObject(Division.class));
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(Object dataObj)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof ExchangeOut))
        {
            return STATUS.Error;
        }
        
        STATUS duplicateNumberStatus = isCheckDuplicateNumberOK("exchangeNumber", 
                                                                (FormDataObjIFace)dataObj, 
                                                                ExchangeOut.class, 
                                                                "exchangeOutId");

        return duplicateNumberStatus;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#deleteMsg(java.lang.Object)
     */
    public String getDeleteMsg(final Object dataObj)
    {
        if (dataObj instanceof Accession)
        {
            return getLocalizedMessage("LOAN_DELETED", ((ExchangeOut)dataObj).getExchangeNumber());
        }
        // else
        return super.getDeleteMsg(dataObj);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#setObjectIdentity(java.lang.Object, edu.ku.brc.ui.DraggableIcon)
     */
    public void setObjectIdentity(final Object dataObj, 
                                  final DraggableRecordIdentifier draggableIcon)
    {
        if (dataObj == null)
        {
            draggableIcon.setLabel("");
        }
        
        if (dataObj instanceof ExchangeOut)
        {
            ExchangeOut exchangeOut = (ExchangeOut)dataObj;
            
            draggableIcon.setLabel(exchangeOut.getExchangeNumber());
            
            Object data = draggableIcon.getData();
            if (data == null)
            {
                RecordSet rs = new RecordSet();
                rs.initialize();
                rs.addItem(exchangeOut.getExchangeOutId());
                data = rs;
                draggableIcon.setData(data);
                
            } else if (data instanceof RecordSetIFace)
            {
                RecordSetIFace rs = (RecordSetIFace)data;
                rs.clearItems();
                rs.addItem(exchangeOut.getExchangeOutId());
            }
        }
     }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#canCreateNewDataObject()
     */
    @Override
    public boolean canCreateNewDataObject()
    {
        return true;
    }
}
