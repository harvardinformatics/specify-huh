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
 * 
*/
package edu.harvard.huh.specify.datamodel.busrules;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import edu.harvard.huh.specify.plugins.ItemCountsLabel;

import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.ku.brc.af.ui.db.TextFieldWithInfo;
import edu.ku.brc.af.ui.forms.EditViewCompSwitcherPanel;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.ResultSetController;
import edu.ku.brc.af.ui.forms.ResultSetControllerListener;
import edu.ku.brc.af.ui.forms.SubViewBtn;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.af.ui.forms.validation.ValSpinner;
import edu.ku.brc.af.ui.forms.validation.ValTextAreaBrief;
import edu.ku.brc.af.ui.forms.validation.ValTextField;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.StaleObjectException;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.busrules.LoanPreparationBusRules;
import edu.ku.brc.ui.CommandListener;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 * @author david lowery
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
    private final String DESCRIPTION  = "descriptionOfMaterial"; //dl: added for auto-populate
    private final String HIGHER_TAXON = "higherTaxon";
    private final String SRC_TAXONOMY = "srcTaxonomy";
    private final String TYPE_COUNT   = "typeCount";
    private final String ITEM_COUNT   = "itemCount";
    
    /**
     * Constructor.
     */
    public HUHLoanPreparationBusRules()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     * 
     * dl: initialize the busrules, add listeners to check for changes to any of the preparation item, type or non specimen
     * counts, and add listeners to auto populate the fields on the preparation form.
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
        	//dl: listens for an index change and performs an update to itemcountlabel plugin
        	final ItemCountsLabel itemCountsLabel = (ItemCountsLabel)formViewObj.getControlById("itemcountslabel");
        	final ResultSetControllerListener rsListener = new ResultSetControllerListener() {
    		@Override
        	public void indexChanged(int newIndex) {
        		System.out.println("index changed");
        		itemCountsLabel.doAccounting(formViewObj);
        		
        	}
        	
			@Override
        	public void newRecordAdded() {
        		//System.out.println("new record added");
        		//doAccounting();
        		
        	}

			@Override
			public boolean indexAboutToChange(int oldIndex, int newIndex) {
				//System.out.println("index about to change");
				// TODO Auto-generated method stub
				return true;
			} 
        	};
        	
		    ResultSetController rsController = formViewObj.getRsController();
		    rsController.addListener(rsListener);
        	
        	final SwingWorker worker = new SwingWorker() {
				//dl: This code must wait for the formViewObj to be updated before executing the update on item counts
				@Override
				public Object construct() {
					while (formViewObj.getDataObj() == null || formViewObj.getParentDataObj() == null) { try {
						Thread.sleep(100); // wait 100 ms and try again
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} }
					itemCountsLabel.doAccounting(formViewObj);
					return null;
				}};
				worker.start();
        	
            Component itemCnt = formViewObj.getControlByName("itemCount");
            Component typeCnt = formViewObj.getControlByName("typeCount");
            
            Component nonSpecimenCnt = formViewObj.getControlByName("nonSpecimenCount");
        	//MultiView mv = (MultiView)formViewObj.getControlByName("loanPreparations");
        	//FormViewObj formViewObj = mv.getCurrentViewAsFormViewObj();
            //FormViewObj loanForm = formViewObj.getMVParent().getMultiViewParent().getCurrentViewAsFormViewObj();
            //if (loanForm != null) {
	            
	            //dl: listener for the count value spinner components
	            ChangeListener countChangeListener = new ChangeListener() {
	                @Override
	                public void stateChanged(ChangeEvent e)
	                {
	                    itemCountsLabel.doAccounting(formViewObj);
	                }
	            };
	            
	            
	            if (itemCnt instanceof ValSpinner)
	            	((ValSpinner)itemCnt).addChangeListener(countChangeListener);
	            if (typeCnt instanceof ValSpinner)
	            	((ValSpinner)typeCnt).addChangeListener(countChangeListener);
	            if (nonSpecimenCnt instanceof ValSpinner)
	            	((ValSpinner)nonSpecimenCnt).addChangeListener(countChangeListener);
            //}
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
            
            
            //if (prepComp != null && prepComp instanceof ValComboBoxFromQuery)
            
            /*dl: an EditViewCompSwitcherPanel that contains a ValComboBoxFromQuery (displayed before preps
            are saved to a loan and  a TextFieldWithInfo, the uneditable field displayed after preps are saved */
            if (prepComp != null && prepComp instanceof EditViewCompSwitcherPanel)
            {	
                Component[] prepComps = ((EditViewCompSwitcherPanel)prepComp).getComponents();
                
            	ValComboBoxFromQuery comboBox = null;
            	TextFieldWithInfo textField = null;
            	
                for (Component c : prepComps) {
                	if (c != null && c instanceof ValComboBoxFromQuery)
                		comboBox = (ValComboBoxFromQuery)c;
                	if (c != null && c instanceof TextFieldWithInfo)
                		textField = (TextFieldWithInfo)c;
                }
                
            	final ValComboBoxFromQuery prepComboBox = comboBox;
            	final TextFieldWithInfo prepTextField = textField;
               // if (prepComp != null && prepComp instanceof ValComboBoxFromQuery) {
              //  	final ValComboBoxFromQuery prepComboBox = (ValComboBoxFromQuery) prepComp;
                //} else if (prepComp != null && prepComp instanceof TextFieldWithInfo) {
                //	final TextFieldWithInfo prepComboBox = (TextFieldWithInfo)prepComp;
               // }
            	//final EditViewCompSwitcherPanel prepComboBox = (EditViewCompSwitcherPanel) prepComp;
                
                final Component srcTaxonComp    = formViewObj.getControlById(SRC_TAXONOMY);
                //final Component higherTaxonComp = formViewObj.getControlById(HIGHER_TAXON);
                final Component typeCountComp   = formViewObj.getControlById(TYPE_COUNT);
                final Component itemCountComp   = formViewObj.getControlById(ITEM_COUNT);
                final Component descriptionComp   = formViewObj.getControlByName(DESCRIPTION);

                //if (srcTaxonComp != null && higherTaxonComp != null && srcTaxonComp instanceof JTextField && higherTaxonComp instanceof JTextField)
               // if (srcTaxonComp != null && srcTaxonComp instanceof JTextField)
                if (srcTaxonComp != null && srcTaxonComp instanceof ValTextField)
                {
                    final ValTextField srcTaxonTextField = (ValTextField) srcTaxonComp;
                   // final JTextField higherTaxonTextField = (JTextField) higherTaxonComp;
                    final ValSpinner  typeCount = (ValSpinner) typeCountComp; 
                    final ValSpinner  itemCount = (ValSpinner) itemCountComp;
                    final ValTextAreaBrief    description = (ValTextAreaBrief) descriptionComp;
                    
                    //if (srcTaxonTextField != null && higherTaxonTextField != null && typeCount != null)
                    /*if (srcTaxonTextField != null && typeCount != null)
                    {
                        prepComboBox.addFocusListener(new FocusListener() {

					@Override
					public void focusGained(FocusEvent arg0) {
			            LoanPreparation loanPreparation = (LoanPreparation)formViewObj.getDataObj();
			            itemCountsLabel.initLabels(loanPreparation, null);
						
					} 

					@Override
					public void focusLost(FocusEvent arg0) {
						// TODO Auto-generated method stub
						
					} });*/
					
                    	prepComboBox.addListSelectionListener(
                                new ListSelectionListener()
                                {
                                    @Override
                                    public void valueChanged(ListSelectionEvent e)
                                    {
                                        //if (e != null && !e.getValueIsAdjusting())  // Specify sometimes sends a null event for updating the display
                                        //{ dl: for some reason the event is always null now.
                                            Preparation prep  = (Preparation) prepComboBox.getValue();
                                            if (prep != null)
                                            {
                                                //adjustTaxonFields(prep, srcTaxonTextField, higherTaxonTextField, typeCount, itemCount);
                                            	//adjustTaxonFields(prep, srcTaxonTextField, description, typeCount, itemCount);
                                            }
                                            prep  = (Preparation) prepTextField.getValue();
                                            if (prep != null)
                                            {
                                                //adjustTaxonFields(prep, srcTaxonTextField, higherTaxonTextField, typeCount, itemCount);
                                            	//adjustTaxonFields(prep, srcTaxonTextField, typeCount, itemCount);
                                            }
                                        //}
                                    }
                                });
                    }
                }
            }
        }
    
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BusinessRulesIFace#isOkToAssociateSearchObject(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isOkToAssociateSearchObject(Object newParentDataObj, Object dataObjectFromSearch)
    {
        boolean isOK = super.isOkToAssociateSearchObject(newParentDataObj, dataObjectFromSearch);
        
        if (!isOK) return false;
        
        reasonList.clear();

        if (newParentDataObj instanceof LoanPreparation && dataObjectFromSearch instanceof Preparation)
        {
            LoanPreparation loanPrep = (LoanPreparation) newParentDataObj;
            Preparation prep = (Preparation) dataObjectFromSearch;
            
            Integer thisLoanPrepId = loanPrep.getLoanPreparationId();
            Integer thisPrepId = prep.getPreparationId();
            
            for (LoanPreparation lpo : loanPrep.getLoan().getLoanPreparations())
            {
                Preparation pp = lpo.getPreparation();
                
                Integer thatLoanPrepId = lpo.getLoanPreparationId();
                Integer thatPrepId = pp.getPreparationId();
                
                if (thisPrepId != null && thatPrepId != null && thisPrepId.equals(thatPrepId) &&
                        thisLoanPrepId != null && thatLoanPrepId != null && thisLoanPrepId.equals(thatLoanPrepId))
                {
                    reasonList.add(UIRegistry.getResourceString("LOAN_PREP_DUP_PREP"));
                    return false;
                }
            }
        }
        
        return true;
    }
    
    //private void adjustTaxonFields(Preparation prep, JTextField srcTaxonTextField, JTextField higherTaxonTextField, ValSpinner typeCount, ValSpinner itemCount)
    private void adjustTaxonFields(Preparation prep, JTextField srcTaxonTextField, ValTextAreaBrief description, ValSpinner typeCount, ValSpinner itemCount)
    {
        try
        {
            DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
            prep = session.merge(prep);
            Integer countAmt = prep.getCountAmt();
            String descriptionStr = "test description";//prep.getDescription();
            
            Determination det = null;

            for (Fragment f : prep.getFragments())
            {
                for (Determination d : f.getDeterminations())
                {
                    if (isType(d))
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
               //             higherTaxonTextField.setText(higherTaxonName);
                        }
                    }
                }
                if (isType(det))
                {
                    if (typeCount != null)
                    {
                        Object nextValue = typeCount.getNextValue();
                        typeCount.setValue(nextValue);
                    }
                }
            }
            
            if (itemCount != null && det != null && !isType(det))
            {
                itemCount.setValue(countAmt);
            }
            
            if (description != null && descriptionStr != null) {
            	description.setValue(descriptionStr, null);
            }

            session.close();
        }
        catch (StaleObjectException soe)
        {
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHLoanPreparationBusRules.class, soe);
            soe.printStackTrace();
        }
    }
    
    protected boolean isType(Determination d)
    {
        return d.getTypeStatusName() != null && !d.getTypeStatusName().toLowerCase().startsWith("not");
    }
}
