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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.harvard.huh.specify.plugins.ItemCountsLabel;
import edu.harvard.huh.specify.plugins.PrepItemTable;
import edu.ku.brc.af.ui.db.TextFieldWithInfo;
import edu.ku.brc.af.ui.forms.EditViewCompSwitcherPanel;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.ResultSetController;
import edu.ku.brc.af.ui.forms.ResultSetControllerListener;
import edu.ku.brc.af.ui.forms.SubViewBtn;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.FormValidator;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.af.ui.forms.validation.ValSpinner;
import edu.ku.brc.af.ui.forms.validation.ValTextAreaBrief;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.StaleObjectException;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
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
    private final String DESCRIPTION  = "descriptionOfMaterial";
    private final String HIGHER_TAXON = "higherTaxon";
    private final String SRC_TAXONOMY = "srcTaxonomy";
    private final String TYPE_COUNT   = "typeCount";
    private final String ITEM_COUNT   = "itemCount";
    private final String PREP_ITEM_TABLE = "prepitemtable";
    private final String ITEM_COUNTS_LABEL = "itemcountslabel";
    private final String IS_RESOLVED = "isResolved";
    
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
            formViewObj = (FormViewObj)viewable;
        
        if (formViewObj != null)
        {
        	final ItemCountsLabel itemCountsLabel = (ItemCountsLabel)formViewObj.getControlById(ITEM_COUNTS_LABEL);
            final Component prepComp = formViewObj.getControlById(PREPARATION);
            final PrepItemTable prepItemTable = (PrepItemTable)formViewObj.getControlById(PREP_ITEM_TABLE);
            final Component taxonComp = formViewObj.getControlById(SRC_TAXONOMY);
            final Component higherTaxonComp = formViewObj.getControlById(HIGHER_TAXON);
            final Component  typeCountComp = formViewObj.getControlById(TYPE_COUNT);
            final Component  itemCountComp = formViewObj.getControlById(ITEM_COUNT);
            final Component descriptionComp = formViewObj.getControlByName(DESCRIPTION);
            final Component isResolvedComp = formViewObj.getControlByName(IS_RESOLVED);
            
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
            
        	//dl: listens for an index change and performs an update to itemcountlabel plugin
        	final ResultSetControllerListener rsListener = new ResultSetControllerListener() {
	    		@Override
	        	public void indexChanged(int newIndex) {
	    			Preparation prep  = (Preparation) prepComboBox.getValue();
					if (prep != null) {
						adjustTaxonFields(prep, taxonComp, descriptionComp, typeCountComp, itemCountComp, isResolvedComp);				
					}
	        		doAccounting(itemCountsLabel, (Loan)formViewObj.getParentDataObj());
	        		
	        	}

				@Override
				public boolean indexAboutToChange(int oldIndex, int newIndex) {
					return true;
				}

				@Override
				public void newRecordAdded() {
					Preparation prep  = (Preparation) prepComboBox.getValue();
					if (prep != null) {
						adjustTaxonFields(prep, taxonComp, descriptionComp, typeCountComp, itemCountComp, isResolvedComp);				
	                    ((FormViewObj)viewable).setDataIntoUI();
	                    FormValidator formValidator = viewable.getValidator();
	                    formValidator.setHasChanged(true);
	                    formValidator.updateSaveUIEnabledState();
	                    formValidator.validateForm();
					}
				}
        	};
        	
		    ResultSetController rsController = formViewObj.getRsController();
		    if (rsController != null) {
		    	rsController.addListener(rsListener);
		    }
		    
		  //dl: This code must wait for the formViewObj to be updated before executing the update on item counts
        	final SwingWorker worker = new SwingWorker() {
				@Override
				public Object construct() {
					while (formViewObj == null || formViewObj.getDataObj() == null || formViewObj.getParentDataObj() == null) {
					try {
						Thread.sleep(100); // wait 100 ms and try again
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				/* when the prep subform is first loaded, update the loan prepration object and the form
				components and then update the counts. */
					Preparation prep  = (Preparation) prepComboBox.getValue();
					if (prep != null) {
						adjustTaxonFields(prep, taxonComp, descriptionComp, typeCountComp, itemCountComp, isResolvedComp);				
	                    ((FormViewObj)viewable).setDataIntoUI();
	                    FormValidator formValidator = viewable.getValidator();
	                    formValidator.setHasChanged(true);
	                    formValidator.updateSaveUIEnabledState();
	                    formValidator.validateForm();
					}
				doAccounting(itemCountsLabel, (Loan)formViewObj.getParentDataObj());
				return null;
			}};
			
			worker.start();
        	
            Component itemCnt = formViewObj.getControlByName("itemCount");
            Component typeCnt = formViewObj.getControlByName("typeCount");
            Component nonSpecimenCnt = formViewObj.getControlByName("nonSpecimenCount");
	            
	            //dl: listener for the count value spinner components
	            ChangeListener countChangeListener = new ChangeListener() {
	                @Override
	                public void stateChanged(ChangeEvent e)
	                {
	                    doAccounting(itemCountsLabel, (Loan)formViewObj.getParentDataObj());
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
    
                    /* The document listener below will adjust taxon fields and auto-populate
                       when a change or update is made. */
								DocumentListener listener = new DocumentListener() {
									
									@Override
									public void changedUpdate(DocumentEvent arg0) {
										Preparation prep  = (Preparation) prepComboBox.getValue();
										if (prep != null) {
											adjustTaxonFields(prep, taxonComp, descriptionComp, typeCountComp, itemCountComp, isResolvedComp);
											prepItemTable.updateItems(prep);
										}
									}

									@Override
									public void insertUpdate(DocumentEvent arg0) {

										Preparation prep  = (Preparation) prepComboBox.getValue();
										if (prep != null) {
											adjustTaxonFields(prep, taxonComp, descriptionComp, typeCountComp, itemCountComp, isResolvedComp);
											prepItemTable.updateItems(prep);
										}
									}

									@Override
									public void removeUpdate(DocumentEvent arg0) {
									    prepItemTable.clearItems();
										//clearTaxonFields(taxonComp, descriptionComp, typeCountComp, itemCountComp);
										
									}
									
								};

		            			prepTextField.getTextField().getDocument().addDocumentListener(listener);
								prepComboBox.getTextWithQuery().getTextField().getDocument().addDocumentListener(listener);

						
						/* dl: Using Document listener above instead of this code now. It is more appropriate (the ListSelectionListener
						   doesnt get triggered when using batch add preps.
                    	
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
                                });*/
                    }
                }
            }
    
    @Override
    public boolean isOkToSave(Object dataObj, DataProviderSessionIFace session)
    {
        if (dataObj instanceof LoanPreparation) {
        	formViewObj.getDataFromUI();
        }
        return true;
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

    private void clearTaxonFields(Component srcTaxonTextField, Component description, Component typeCount, Component itemCount)
    {
		if (srcTaxonTextField instanceof JTextField)
			((JTextField)srcTaxonTextField).setText(null);
		if (description instanceof ValTextAreaBrief)
			((ValTextAreaBrief)description).setText(null);
		if (typeCount instanceof ValSpinner) {
			((ValSpinner)typeCount).setValue(0);
			typeCount.setEnabled(true);
		}
		if (itemCount instanceof ValSpinner) {
			((ValSpinner)itemCount).setValue(0);
			itemCount.setEnabled(true);
		}
    }
    
    public static void doAccounting(ItemCountsLabel itemCountsLabel, Loan loan) {
    	int itemCount = 0, typeCount = 0, nonSpecimenCount = 0;
    	int itemReturned = 0, typeReturned = 0, nonSpecimenReturned = 0;
		
		if (loan != null) {
        	for (LoanPreparation lp : loan.getLoanPreparations()) {
        		itemCount += lp.getItemCount() != null ? lp.getItemCount() : 0;
        		typeCount += lp.getTypeCount() != null ? lp.getTypeCount() : 0;
        		nonSpecimenCount += lp.getNonSpecimenCount() != null ? lp.getNonSpecimenCount() : 0;
        		for (LoanReturnPreparation lrp : lp.getLoanReturnPreparations()) {
        			itemReturned += lrp.getItemCount() != null ? lrp.getItemCount() : 0;
        			typeReturned += lrp.getTypeCount() != null ? lrp.getTypeCount() : 0;
        			nonSpecimenReturned += lrp.getNonSpecimenCount() != null ? lrp.getNonSpecimenCount() : 0;
        		}
        	}
		}
		
		itemCountsLabel.updateLabels(itemCount, typeCount, nonSpecimenCount, itemReturned, typeReturned, nonSpecimenReturned);
    }
    
    /** 
     * When this method is called, it obtains values to populate into the fields from the preparation object.
     * Taxon name is obtained from the determination object within the fragment in the preparation. If the determination
     * is a type, the preparation countAmt variable is included in the type count field. Otherwise, the countAmt is
     * set as the item count field value. Uses inner class PrepInfo to obtain the values used in the fields.
     */
    private void adjustTaxonFields(Preparation prep, Component taxonComp, Component descriptionComp, Component typeCountComp, Component itemCountComp, Component isResolvedComp)
    {
    	if (!prep.getPrepType().getName().equals("Lot")) {
    		clearTaxonFields(taxonComp, descriptionComp, typeCountComp, itemCountComp);
    		
    		LoanPrepInfo prepInfo = null;
        	itemCountComp.setEnabled(false);
            typeCountComp.setEnabled(false);
    	
	        try
	        {
	            DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
	            prep = session.merge(prep);
	            prepInfo = new LoanPrepInfo(prep);
	    	
	            
	            /* If the preparation not of type "Lot" then get the counts for types and items from prepInfo
	               otherwise, get the value from the LoanPreparation object. */
	            
		        if (typeCountComp != null && typeCountComp instanceof ValSpinner) {
		        		((ValSpinner)typeCountComp).setValue(prepInfo.getTypeCount());
		        		((LoanPreparation)formViewObj.getDataObj()).setTypeCount(prepInfo.getTypeCount());
		   	    }
		        
		        if (itemCountComp != null && itemCountComp instanceof ValSpinner) {
		        		((ValSpinner)itemCountComp).setValue(prepInfo.getItemCount());
		        		((LoanPreparation)formViewObj.getDataObj()).setItemCount(prepInfo.getItemCount());
		        }
		        
		        /*if (descriptionComp != null && descriptionComp instanceof ValTextAreaBrief) {
		        	((ValTextAreaBrief)descriptionComp).setText(prepInfo.getDescription());
		        }*/
		        
		        if (taxonComp != null && taxonComp instanceof JTextField) {
		        	((JTextField)taxonComp).setText(prepInfo.getTaxon());
		        }
		        
		        if (isResolvedComp != null && isResolvedComp instanceof JCheckBox) {
		        	((JCheckBox)isResolvedComp).setSelected(((LoanPreparation)formViewObj.getDataObj()).getIsResolved());
		        }
		        formViewObj.getDataFromUI();
		        
		        session.close();
	        
	        } catch (StaleObjectException soe) {
	        	edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHLoanPreparationBusRules.class, soe);
	        	soe.printStackTrace();
	        }  
    	}
    }
    	
        /* dl: after discussion with paul we determined that item count and type count should not be 
        editable on the loan form directly. If changes are made to fields on the loan form
        they will alter the associated LoanPreparation object and not progagate these changes to the
        preparation object. 
        
        however, in the case of lots, allow edits to the counts since the loan preparation values
        rather than the preparation CountAmt values are to be used */
        
    
    /** Inner class that processes a preparation obj and stores its fields within the 
     * class instance variables for use when adjusting preparation form fields,
     * updating the item counts, and propagating changes to the LoanPreparation
     * and Preparation objects.
     * 
     * @author lowery
     *
     */
    public class LoanPrepInfo {
    	private int typeCount, itemCount, nonSpecimenCount;
    	private String description, taxonName, higherTaxonName;
    	private boolean isType, isLot;
    	
    	private Preparation prep;
    	
    	/** Constructor takes a Preparation argument and stores its values in 
    	 * instance variables.
    	 * @param prep
    	 */
    	private LoanPrepInfo(Preparation prep) {
    		this.prep = prep;
    		
                Determination det = null;
                isLot = prep.getPrepType().getName().equals("Lot");
                
                for (Fragment f : prep.getFragments()) {
                    /* Find the current determination for each fragment in the preparation and
                	   determine if this preparation is a type */
                	for (Determination d : f.getDeterminations()) {
 
                        isType = d.getTypeStatusName() != null && !d.getTypeStatusName().toLowerCase().startsWith("not");
                    
                        if (isType) {
                            det = d;
                            break;
                        } else if (d.isCurrentDet()) {
                            det = d;
                        }
                    }
                    
                	/* If the determination for that fragment is a type, increment typeCount
                       otherwise it is an item. If it is not a lot, increment the count otherwise
                       use the values specified in the loan preparation */
                    if (isType) {
                    	typeCount++;
                    } else if (!isLot){
                    	itemCount++;
                    }
                }

                 // Obtain taxon and higher taxon name from the determination
                if (det != null && det.getTaxon() != null) {
                    Taxon t = det.getTaxon();
                    if (t != null) {
                        taxonName = t.getFullName();
    
                        if (t.getRankId() >= TaxonTreeDef.SUBGENUS) {
                            Taxon parent = t.getParent();
                            if (parent != null) {
                                Taxon higherTaxon = parent.getParent();
                                if (higherTaxon != null) {
                                    higherTaxonName = higherTaxon.getFullName();
                                }
                            }
                        }
                    }
                }
                
                description = prep.getDescription(); // Obtain description string from loan prep   
    	}
    	
    	public int getTypeCount() {
    		return typeCount;
    	}
    	
    	public int getItemCount() {
    		return itemCount;	
    	}
    	
    	public void setNonSpecimenCount(int nonSpecimenCount) {
    		this.nonSpecimenCount = nonSpecimenCount;
    	}
    	
    	public int getNonSpecimenCount() {
    		return nonSpecimenCount;
    	}
    	
    	public String getDescription() {
    		return description;
    	}
    	
    	public String getTaxon() {
    		return taxonName;
    	}
    	
    	public boolean isLot() {
    		return isLot;
    	}
    }
    
}
