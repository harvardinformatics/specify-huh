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

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.prefs.AppPrefsCache;
import edu.ku.brc.af.ui.forms.DraggableRecordIdentifier;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.UIValidatable;
import edu.ku.brc.af.ui.forms.validation.ValFormattedTextFieldSingle;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.busrules.AttachmentOwnerBaseBusRules;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.DateWrapper;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.UIRegistry;

/**
 * Business rules for validating a Loan.
 * 
 * @code_status Alpha
 *
 * @author rods
 * @author mmk
 */
public class HUHLoanBusRules extends AttachmentOwnerBaseBusRules
{  
    public static final String DUEINMONTHS = "loans.dueinmons"; 
    
    /**
     * Constructor.
     */
    public HUHLoanBusRules()
    {
        super(Loan.class);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        super.initialize(viewableArg);
        
        formViewObj.setSkippingAttach(true);

        if (isEditMode())
        {
            Component closedComp = formViewObj.getControlByName("isClosed");
            if (closedComp instanceof JCheckBox)
            {
                ((JCheckBox)closedComp).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        if (((JCheckBox)e.getSource()).isSelected())
                        {
                            Component dateComp = formViewObj.getControlByName("dateClosed");
                            if (dateComp != null && dateComp instanceof ValFormattedTextFieldSingle)
                            {
                                ValFormattedTextFieldSingle loanDateComp = (ValFormattedTextFieldSingle)dateComp;
                                
                                if (StringUtils.isEmpty(loanDateComp.getText()))
                                {
                                    DateWrapper scrDateFormat = AppPrefsCache.getDateWrapper("ui", "formatting", "scrdateformat");
                                    loanDateComp.setText(scrDateFormat.format(Calendar.getInstance()));
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#beforeFormFill()
     */
    @Override
    public void beforeFormFill()
    {
        Loan loan = (Loan)formViewObj.getDataObj();
        
        if (formViewObj != null && loan != null)
        {
            if (loan.getId() != null)
            {
                DataProviderSessionIFace session = formViewObj.getSession();
                
                try
                {
                    if (session == null)
                    {
                        session = DataProviderFactory.getInstance().createSession();
                    }
                
                    session.attach(loan);
                    loan.forceLoad();
                    
                } catch (Exception ex)
                {
                    //UsageTracker.incrHandledUsageCount();
                    //edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(DataEntryTask.class, ex);
                    //log.error(ex);
                    ex.printStackTrace();
                    
                } finally
                {
                    if (session != null && formViewObj.getSession() == null)
                    {
                        session.close();
                    }
                } 
            } else 
            {
                Integer dueInMonths = AppPreferences.getRemote().getInt(DUEINMONTHS, 6);
                if (dueInMonths != null)
                {
                    Calendar date = Calendar.getInstance();
                    date.add(Calendar.MONTH, dueInMonths);
                    loan.setCurrentDueDate(date);
                }
            }
        }
            
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#afterFillForm(java.lang.Object)
     */
    @Override
    public void afterFillForm(final Object dataObj)
    {
        if (formViewObj != null && formViewObj.getDataObj() instanceof Loan)
        {
            formViewObj.setSkippingAttach(true);

            MultiView mvParent = formViewObj.getMVParent();
            Loan      loan     = (Loan) formViewObj.getDataObj();
            boolean   isNewObj = loan.getId() == null;
            boolean   isEdit   = mvParent.isEditable();

            Component comp     = formViewObj.getControlByName("generateInvoice");
            if (comp instanceof JCheckBox)
            {
                ((JCheckBox)comp).setVisible(isEdit);
            }
            
            boolean allResolved = true;
            for (LoanPreparation loanPrep : loan.getLoanPreparations())
            {
                Boolean isResolved = loanPrep.getIsResolved();
                if (isResolved == null || (isResolved != null && !isResolved))
                {
                    allResolved = false;
                    break;
                }
            }
            
            comp = formViewObj.getControlByName("ReturnLoan");
            if (comp instanceof JButton)
            {
                comp.setVisible(isEdit);
                Boolean isClosed = loan.getIsClosed();
                comp.setEnabled(!isNewObj && (isClosed != null ? !loan.getIsClosed() : false) && !allResolved);
                
                if (allResolved)
                {
                    ((JButton)comp).setText(UIRegistry.getResourceString("LOAN_ALL_PREPS_RETURNED"));
                }
            }
            
            if (isNewObj)
            {
                Component shipComp = formViewObj.getControlByName("shipmentNumber");
                comp = formViewObj.getControlByName("loanNumber");
                if (comp instanceof JTextField && shipComp instanceof JTextField)
                {
                    JTextField loanTxt = (JTextField) comp;
                    if (shipComp instanceof GetSetValueIFace)
                    {
                        GetSetValueIFace gsv = (GetSetValueIFace) shipComp;
                        gsv.setValue(loanTxt.getText(), loanTxt.getText());
                        
                    } else if (shipComp instanceof JTextField)
                    {
                        ((JTextField)shipComp).setText(loanTxt.getText());
                    }
                    
                    if (shipComp instanceof UIValidatable)
                    {
                        UIValidatable uiv = (UIValidatable) shipComp;
                        uiv.setChanged(true);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#processBusiessRules(java.lang.Object)
     */
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        
        if (dataObj == null || !(dataObj instanceof Loan))
        {
            return STATUS.Error;
        }
        Loan loan = (Loan)dataObj;
        
        if (loan.getId() == null)
        {
            STATUS duplicateNumberStatus = isCheckDuplicateNumberOK("loanNumber", 
                                                                    (FormDataObjIFace)dataObj, 
                                                                    Loan.class, 
                                                                    "loanId");
            
            return duplicateNumberStatus;
        }
        return STATUS.OK;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BusinessRulesIFace#deleteMsg(java.lang.Object)
     */
    public String getDeleteMsg(final Object dataObj)
    {
        if (dataObj instanceof Accession)
        {
            return getLocalizedMessage("LOAN_DELETED", ((Loan)dataObj).getLoanNumber());
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
        
        if (dataObj instanceof Loan)
        {
            Loan loan = (Loan)dataObj;
            
            draggableIcon.setLabel(loan.getLoanNumber());
            
            Object data = draggableIcon.getData();
            if (data == null)
            {
                RecordSet rs = new RecordSet();
                rs.initialize();
                rs.addItem(loan.getLoanId());
                data = rs;
                draggableIcon.setData(data);
                
            } else if (data instanceof RecordSetIFace)
            {
                RecordSetIFace rs = (RecordSetIFace)data;
                rs.clearItems();
                rs.addItem(loan.getLoanId());
            }
        }
     }
}
