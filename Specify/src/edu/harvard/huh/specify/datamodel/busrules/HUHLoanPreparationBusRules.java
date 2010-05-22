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

import java.awt.Component;

import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.SubViewBtn;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.af.ui.forms.validation.ValSpinner;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.StaleObjectException;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.busrules.LoanPreparationBusRules;
import edu.ku.brc.ui.CommandListener;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Jan 29, 2007
 *
 */
public class HUHLoanPreparationBusRules extends LoanPreparationBusRules implements CommandListener
{
    private SubViewBtn loanRetBtn       = null;
    
    private final String PREPARATION  = "preparation";
    private final String HIGHER_TAXON = "higherTaxon";
    private final String SRC_TAXONOMY = "srcTaxonomy";
    private final String TYPE_COUNT   = "typeCount";
    /**
     * Constructor.
     */
    public HUHLoanPreparationBusRules()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        viewable = viewableArg;
        if (viewable instanceof FormViewObj)
        {
            formViewObj = (FormViewObj)viewable;
        }
        
        if (formViewObj != null)
        {
            formViewObj.setSkippingAttach(true);

            Component comp = formViewObj.getControlById("loanReturnPreparations");
            if (comp instanceof SubViewBtn)
            {
                loanRetBtn = (SubViewBtn)comp;
                loanRetBtn.getBtn().setIcon(null);
                loanRetBtn.getBtn().setText(UIRegistry.getResourceString("LOAN_RET_PREP"));
            }
            
            // listen for changes to preparation to auto-fill higher taxon and src taxonomy fields
            Component prepComp = formViewObj.getControlById(PREPARATION);
            
            if (prepComp != null && prepComp instanceof ValComboBoxFromQuery)
            {
                final ValComboBoxFromQuery prepComboBox = (ValComboBoxFromQuery) prepComp;
                
                final Component srcTaxonComp    = formViewObj.getControlById(SRC_TAXONOMY);
                final Component higherTaxonComp = formViewObj.getControlById(HIGHER_TAXON);
                final Component typeCountComp   = formViewObj.getControlById(TYPE_COUNT);

                if (srcTaxonComp != null && higherTaxonComp != null && srcTaxonComp instanceof JTextField && higherTaxonComp instanceof JTextField)
                {
                    final JTextField srcTaxonTextField = (JTextField) srcTaxonComp;
                    final JTextField higherTaxonTextField = (JTextField) higherTaxonComp;
                    final ValSpinner  typeCount = (ValSpinner) typeCountComp; 
                    
                    if (srcTaxonTextField != null && higherTaxonTextField != null && typeCount != null)
                    {
                        prepComboBox.addListSelectionListener(
                                new ListSelectionListener()
                                {
                                    @Override
                                    public void valueChanged(ListSelectionEvent e)
                                    {
                                        if (e != null && !e.getValueIsAdjusting())  // Specify sometimes sends a null event for updating the display
                                        {
                                            Preparation prep  = (Preparation) prepComboBox.getValue();
                                            if (prep != null)
                                            {
                                                adjustTaxonFields(prep, srcTaxonTextField, higherTaxonTextField, typeCount);
                                            }
                                        }
                                    }
                                });
                    }
                }
            }
        }
    }
    
    private void adjustTaxonFields(Preparation prep, JTextField srcTaxonTextField, JTextField higherTaxonTextField, ValSpinner typeCount)
    {
        try
        {
            DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
            prep = session.merge(prep);

            Determination det = null;

            for (Fragment f : prep.getFragments())
            {
                for (Determination d : f.getDeterminations())
                {
                    if (d.getTypeStatusName() != null)
                    {
                        det = d;
                        break;
                    }
                    else if (d.isCurrentDet())
                    {
                        det = d;
                    }
                }
            }

            if (det != null)
            {
                Taxon t = det.getTaxon();
                String srcTaxonName = t.getFullName();
                srcTaxonTextField.setText(srcTaxonName);

                if (t.getRankId() >= TaxonTreeDef.SUBGENUS)
                {
                    Taxon parent = t.getParent();
                    if (parent != null)
                    {
                        Taxon higherTaxon = parent.getParent();
                        if (higherTaxon != null)
                        {
                            String higherTaxonName = higherTaxon.getFullName();
                            higherTaxonTextField.setText(higherTaxonName);
                        }
                    }
                }
                if (det.getTypeStatusName() != null)
                {
                    if (typeCount != null)
                    {
                        Object nextValue = typeCount.getNextValue();
                        typeCount.setValue(nextValue);
                    }
                }
            }
            session.close();
        }
        catch (StaleObjectException soe)
        {
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHLoanPreparationBusRules.class, soe);
            soe.printStackTrace();
        }
    }
}
