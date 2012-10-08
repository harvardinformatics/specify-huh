/*
 * Created on 2011 June 29th
 *
 * Copyright © 2011 President and Fellows of Harvard College
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */

package edu.harvard.huh.specify.reports;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.ui.UIRegistry;

/** Java object representation of a Loan Report for use with HUH loans.
 * To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author lowery
 *
 */
public class ReportLoan {
	private static final int REGION_RANK_ID = 150;
	private String reportsDir = UIRegistry.getDefaultWorkingPath() + File.separator + ReportXslFiles.REPORTS_DIR;
	private String nameOfContact;
	private String title;
	private String jobTitle;
	private String institution;
	private String acronym;
	private String address1;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String country;
	private String nameOfShippedBy;
	private String loanNumber;
	private Calendar dateSent;
	private Calendar dateDue;
	private String forUseBy;
	private int numberOfPackages;
	
	private int fragmentCount;
	private int generalCollectionCount;
	private int nonSpecimenCount;
	private int barcodedSpecimenCount;
	private int unbarcodedTotalCount;
	private int preparationCount;
	private int totalCount;
	private String description;
	private String loanInventory;
	
	
	private List<BarcodedSpecimen> barcodedSpecimens = new ArrayList<BarcodedSpecimen>();
	private Set<UnbarcodedSpecimen> unbarcodedSpecimens = new HashSet<UnbarcodedSpecimen>();;
	
	/** Default constructor explicitly declared for the purposes of JibX binding requirements
	 * 
	 */
	public ReportLoan() { }
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param loan
	 */
	public ReportLoan(Loan loan) {
		if(loan.getShipments() != null)
			processShipment(loan.getShipments());
		
		loanNumber = loan.getLoanNumber();
		dateDue = loan.getCurrentDueDate();
		forUseBy = loan.getText1();
		description = loan.getSpecialConditions();
		loanInventory = loan.getLoanInventory();
		
		Set<LoanPreparation> loanPreparations = new HashSet<LoanPreparation>();
		
		if(loan.getLoanPreparations() != null)
			loanPreparations = loan.getLoanPreparations();

		//initializeCounts(loanPreparations);
		
		for (LoanPreparation lp : loanPreparations) {
			
			if (lp.getPreparation() != null) {
				if (lp.getPreparation().getPrepType() != null && lp.getPreparation().getPrepType().getName().equals("Lot")) {
					UnbarcodedSpecimen lot = new UnbarcodedSpecimen();
					lot.sheetCount = (lp.getItemCount() != null ? lp.getItemCount() : 0) +
					                 (lp.getTypeCount() != null ? lp.getTypeCount() : 0) +
					                 (lp.getNonSpecimenCount() != null ? lp.getNonSpecimenCount() : 0);
					if (lot.taxon != null) {
						lot.taxon = lp.getPreparation().getTaxon().getFullName();
					}
					
					
					lot.description = lp.getDescriptionOfMaterial();
				
					unbarcodedSpecimens.add(lot);
					
					generalCollectionCount += (lp.getItemCount() != null ? lp.getItemCount() : 0) +
	                                          (lp.getTypeCount() != null ? lp.getTypeCount() : 0);
				} else {
					preparationCount++;
					BarcodedSpecimen barcoded = new BarcodedSpecimen();
					Boolean firstPrepBarcode = false;
					for (Fragment f : lp.getPreparation().getFragments()) {
						fragmentCount++;
						BarcodedItem item = new BarcodedItem();
						if (f.getIdentifier() != null) {
							item.identifier = f.getIdentifier();
							barcodedSpecimenCount++; // count barcoded items only
						} else if (lp.getPreparation().getIdentifier() != null && !firstPrepBarcode) {
							item.identifier = lp.getPreparation().getIdentifier();
							firstPrepBarcode = true;
							barcodedSpecimenCount++; // count barcoded items only
						}
						if (f.getDeterminations() != null) {
							String currentName = null;
							String filedUnderName = null;
							for (Determination d : f.getDeterminations()) {
								if (d.isCurrentDet()) {
									if (d.getTaxon() != null) {
										currentName = d.getTaxon().getFullName();
									} else {
										currentName = d.getAlternateName();
									}
								}
								Boolean isFiledUnder = d.getYesNo3();
								if (isFiledUnder != null && isFiledUnder) {
									if (d.getTaxon() != null) {
										filedUnderName = d.getTaxon().getFullName();
									} else {
										filedUnderName = d.getAlternateName();
									}
								}
								if (d.getTypeStatusName() != null) {
									item.type = "Type";
								}
							}
							item.taxon = filedUnderName != null ? filedUnderName : currentName;
						}
						if (f.getCollectionObject() != null && f.getCollectionObject().getCollectingEvent() != null) {
							if (f.getCollectionObject().getCollectingEvent().getCollectors() != null) {
								for (Collector c : f.getCollectionObject().getCollectingEvent().getCollectors()) {
									if (c.getAgent() != null)
										item.collectorName = c.getAgent().getCollectorName();
								}
							}
							if (f.getCollectionObject().getCollectingEvent().getStationFieldNumber() != null && f.getCollectionObject().getCollectingEvent().getStationFieldNumber() != "") {
								item.collectorNumber = f.getCollectionObject().getCollectingEvent().getStationFieldNumber();
							} else {
								item.collectorNumber = f.getCollectionObject().getFieldNumber();
							}
							
							/* Loan report no longer displaying region. This code does not account for a geography with a rank that is 
							 * initially higher than region. In that case the while loop will attempt to obtain a rankId on the root node 
							 * which is null
							  
								if (f.getCollectionObject().getCollectingEvent().getLocality() != null &&
									f.getCollectionObject().getCollectingEvent().getLocality().getGeography() != null) {
								Geography geography = f.getCollectionObject().getCollectingEvent().getLocality().getGeography();
								
								while(geography.getRankId() != REGION_RANK_ID) {
									geography = geography.getParent();
								}
								
								item.region = geography.getName();
							} */
							
							barcoded.items.add(item);
						}
					}
					barcodedSpecimens.add(barcoded);
					
					/* fragmentCount += (lp.getItemCount() != null ? lp.getItemCount() : 0)
					                       + (lp.getTypeCount() != null ? lp.getTypeCount() : 0); */
				}
			}
			nonSpecimenCount += lp.getNonSpecimenCount() != null ? lp.getNonSpecimenCount() : 0;
		}
		Collections.sort(barcodedSpecimens);
		for(BarcodedSpecimen specimen : barcodedSpecimens) {
			Collections.sort(specimen.items);
		}
		
		totalCount = generalCollectionCount + nonSpecimenCount + barcodedSpecimenCount;
		unbarcodedTotalCount = generalCollectionCount + nonSpecimenCount;
	}
	
	/** Finds the current shipment and pulls the agent address values, date and other
	 * information to be used on the report
	 * 
	 * @param shipments
	 */
	private void processShipment(Set<Shipment> shipments) {
		Shipment curr = null;
		for (Shipment s : shipments) {
			if (curr == null || curr.getShipmentDate().before(s.getShipmentDate())) {
				curr = s;
			}
		}
		if (curr != null) {
			dateSent = curr.getShipmentDate();
			numberOfPackages = curr.getNumberOfPackages() != null ? curr.getNumberOfPackages() : 0;
			
			if (curr.getShippedTo() != null) {
				nameOfContact = curr.getShippedTo().getFirstName() + " " + curr.getShippedTo().getLastName();
				if (curr.getShippedTo().getTitle() != null) {
					title = curr.getShippedTo().getTitle();
					title = title.substring(0,1).toUpperCase() + title.substring(1);
				}
				jobTitle = curr.getShippedTo().getJobTitle() != null ? curr.getShippedTo().getJobTitle() : "";
				if (curr.getShippedTo().getOrganization() != null) {
					institution =  curr.getShippedTo().getOrganization().getLastName();
					acronym = curr.getShippedTo().getOrganization().getAbbreviation();
				}
				
				if (curr.getShippedTo().getAddresses() != null) {
					for (Address a : curr.getShippedTo().getAddresses()) {
						if (a.getIsShipping() != null && a.getIsShipping()) {
							address1 = a.getAddress();
							address2 = a.getAddress2();
							city = a.getCity();
							state = a.getState();
							zip = a.getPostalCode();
							country = a.getCountry();
						}
					}
				}
			}
			
			if (curr.getShippedBy() != null)
				nameOfShippedBy = curr.getShippedBy().getFirstName() + " " + curr.getShippedBy().getLastName();
		}
	}
	
	/** Inner class that contains a HashSet representation of all the 
	 * LoanPreparation fragments associated with barcoded loan preparations
	 * 
	 * @author lowery
	 *
	 */
	private class BarcodedSpecimen implements Comparable<BarcodedSpecimen> {
		private List<BarcodedItem> items = new ArrayList<BarcodedItem>();
		
		private BarcodedItem getSortItem() {
			BarcodedItem sortItem = null;
			for (BarcodedItem item : items) {
				if (sortItem == null || sortItem.compareTo(item) == 1)
					sortItem = item;
			}
			return sortItem;
		}
		
		public int compareTo(BarcodedSpecimen specimen) {
			
			if (!getSortItem().identifier.equals("") && !specimen.getSortItem().identifier.equals("") && Double.parseDouble(getSortItem().identifier) > Double.parseDouble(specimen.getSortItem().identifier)) return 1;
			else return -1;
		}
	}
	
	/** Inner class representation of fragments associated with a barcoded loan preparation.
	 * @author lowery
	 *
	 */
	private class BarcodedItem implements Comparable<BarcodedItem> {
		private String identifier = "";
		private String taxon;
		private String type;
		private String collectorName;
		private String collectorNumber;
		private String region;
		
		public int compareTo(BarcodedItem item) {
			if (!identifier.equals("") && !item.identifier.equals("") && Double.parseDouble(identifier) > Double.parseDouble(item.identifier)) return 1;
			else return -1;
		}
	}
	
	/** Inner class representation of an unbarcoded loan preparation
	 * 
	 * @author lowery
	 *
	 */
	private class UnbarcodedSpecimen {
		private int sheetCount;
		private String taxon;
		private String description;
	}
}