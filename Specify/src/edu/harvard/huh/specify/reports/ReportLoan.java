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

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Institution;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Shipment;

/** Java object representation of a Loan Report for use with HUH loans.
 * To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author lowery
 *
 */
public class ReportLoan {
	private static final int REGION_RANK_ID = 150;
	
	private String nameOfContact;
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
	
	private int generalCollectionCount;
	private int nonSpecimenCount;
	private int barcodedSpecimenCount;
	private int totalCount;
	private String description;
	private String loanInventory;
	
	private Set<BarcodedSpecimen> barcodedSpecimens = new HashSet<BarcodedSpecimen>();
	private Set<UnbarcodedSpecimen> unbarcodedSpecimens = new HashSet<UnbarcodedSpecimen>();;
	
	/** Default constructor explicitly declared for the puposes of JibX binding requirements
	 * 
	 */
	public ReportLoan() { }
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param loan
	 */
	public ReportLoan(Loan loan) {
		processShipment(loan.getShipments());
		
		loanNumber = loan.getLoanNumber();
		dateDue = loan.getCurrentDueDate();
		forUseBy = loan.getText1();
		description = loan.getSpecialConditions();
		loanInventory = loan.getLoanInventory();
		
		Set<LoanPreparation> loanPreparations = loan.getLoanPreparations();
		//initializeCounts(loanPreparations);
		
		for (LoanPreparation lp : loanPreparations) {
			if (lp.getPreparation().getPrepType().getName().equals("Lot")) {
				UnbarcodedSpecimen lot = new UnbarcodedSpecimen();
				lot.sheetCount = (lp.getItemCount() != null ? lp.getItemCount() : 0) +
				                 (lp.getTypeCount() != null ? lp.getTypeCount() : 0) +
				                 (lp.getNonSpecimenCount() != null ? lp.getNonSpecimenCount() : 0);
				lot.taxon = lp.getSrcTaxonomy();
				lot.description = lp.getDescriptionOfMaterial();
			
				unbarcodedSpecimens.add(lot);
				
				generalCollectionCount = (lp.getItemCount() != null ? lp.getItemCount() : 0) +
                                          (lp.getTypeCount() != null ? lp.getTypeCount() : 0);
			} else {
				BarcodedSpecimen barcoded = new BarcodedSpecimen();
				for (Fragment f : lp.getPreparation().getFragments()) {
					BarcodedItem item = new BarcodedItem();
					item.identifier = f.getIdentifier();
					for (Determination d : f.getDeterminations()) {
						if (d.isCurrentDet()) {
							item.taxon = d.getTaxon().getName();
						}
						if (d.getTypeStatusName() != null) {
							item.type = "type";
						}
					}
					for (Collector c : f.getCollectionObject().getCollectingEvent().getCollectors()) {
						item.collectorName = c.getAgent().getCollectorName();
						item.collectorNumber = c.getId();
					}
					
					Geography geography = f.getCollectionObject().getCollectingEvent().getLocality().getGeography();
					
					while(geography.getRankId() != REGION_RANK_ID) {
						geography = geography.getParent();
					}
					
					item.region = geography.getName();
					barcoded.items.add(item);
				}
				barcodedSpecimens.add(barcoded);
				
				barcodedSpecimenCount += (lp.getItemCount() != null ? lp.getItemCount() : 0)
				                       + (lp.getTypeCount() != null ? lp.getTypeCount() : 0);
			}
			nonSpecimenCount += lp.getNonSpecimenCount() != null ? lp.getNonSpecimenCount() : 0;
		}
		totalCount = generalCollectionCount + nonSpecimenCount + barcodedSpecimenCount;
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
		dateSent = curr.getShipmentDate();
		numberOfPackages = curr.getNumberOfPackages();
		nameOfContact = curr.getShippedTo().getFirstName() + " " + curr.getShippedTo().getLastName();
		nameOfShippedBy = curr.getShippedBy().getFirstName() + " " + curr.getShippedBy().getLastName();
		
		institution =  curr.getShippedTo().getOrganization().getLastName();
		acronym = curr.getShippedTo().getOrganization().getAbbreviation();
		
		for (Address a : curr.getShippedTo().getAddresses()) {
			if (a.getIsShipping()) {
				address1 = a.getAddress();
				address2 = a.getAddress2();
				city = a.getCity();
				state = a.getState();
				zip = a.getPostalCode();
				country = a.getCountry();
			}
		}
	}
	
	/** Inner class that contains a HashSet representation of all the 
	 * LoanPreparation fragments associated with barcoded loan preparations
	 * 
	 * @author lowery
	 *
	 */
	private class BarcodedSpecimen {
		private Set<BarcodedItem> items = new HashSet<BarcodedItem>();
	}
	
	/** Inner class representation of fragments associated with a barcoded loan preparation.
	 * @author lowery
	 *
	 */
	private class BarcodedItem {
		private String identifier;
		private String taxon;
		private String type;
		private String collectorName;
		private Integer collectorNumber;
		private String region;
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