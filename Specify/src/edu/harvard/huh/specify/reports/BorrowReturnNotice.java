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

import edu.harvard.huh.asa.Transaction.ROLE;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowAgent;
import edu.ku.brc.specify.datamodel.BorrowMaterial;
import edu.ku.brc.specify.datamodel.BorrowReturnMaterial;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.ui.UIRegistry;

/** Java object representation of a Loan Report for use with HUH loans.
 * To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author lowery
 *
 */
public class BorrowReturnNotice {
	
	// borrow
	private String borrowNumber;
	private Calendar currentDueDate;
	private Calendar dateClosed;
	private String invoiceNumber;
	private Boolean isClosed;
	private Calendar originalDueDate;
	private Calendar receivedDate;
	private String remarks;
	private String text1;   // purpose
	private String text2;   // local unit
	private String text3;   // user type: student/staff
	private Boolean yesNo1; // is acknowledged
	private Boolean yesNo2; // is their request
	private Boolean yesNo3; // is visitor
	
	private List<BorrowMaterialDesc> borrowMaterialDescs;
	
	// borrow materials running totals
	private int generalCollectionsCount;
	private int nonSpecimensCount;
	private int typesCount;
	
	private int generalCollectionsReturnedCount;
	private int nonSpecimensReturnedCount;
	private int typesReturnedCount;
	
	private int generalCollectionsBalanceDueCount;
	private int nonSpecimensBalanceDueCount;
	private int typesBalanceDueCount;
	
	private int totalBorrowedSum;
	private int totalReturnedSum;
	private int totalBalanceDueSum;
	
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
	private String forUseBy;
	
	private Calendar dateSent;	
	private int numberOfPackages;
	
	
	/** Default constructor explicitly declared for the purposes of JibX binding requirements
	 * 
	 */
	public BorrowReturnNotice() { }
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param loan
	 */
	public BorrowReturnNotice(Borrow borrow) {
		if(borrow.getShipments() != null)
			processShipment(borrow.getShipments());
		
		borrowNumber = borrow.getBorrowNumber();
		currentDueDate = borrow.getCurrentDueDate();
		dateClosed = borrow.getDateClosed();
		invoiceNumber = borrow.getInvoiceNumber();
		isClosed = borrow.getIsClosed();
		originalDueDate = borrow.getOriginalDueDate();
		receivedDate = borrow.getReceivedDate();
		forUseBy = extractForUseBy(borrow);
		
		remarks = borrow.getRemarks();
		text1 = borrow.getText1();   // purpose
		text2 = borrow.getText2();   // local unit
		text3 = borrow.getText3();   // user type: student/staff
		yesNo1 = borrow.getYesNo1(); // is acknowledged
		yesNo2 = borrow.getYesNo2(); // is their request
		yesNo3 = borrow.getYesNo3(); // is visitor
		
		this.generalCollectionsCount = 0;
		this.nonSpecimensCount = 0;
		this.typesCount = 0;
		
		this.generalCollectionsReturnedCount = 0;
		this.nonSpecimensReturnedCount = 0;
		this.typesReturnedCount = 0;
		
		this.generalCollectionsBalanceDueCount = 0;
		this.nonSpecimensBalanceDueCount = 0;
		this.typesBalanceDueCount  = 0;
		
		this.totalBorrowedSum = 0;
		this.totalReturnedSum = 0;
		this.totalBalanceDueSum = 0;
		
		borrowMaterialDescs = new ArrayList<BorrowMaterialDesc>();
		
		for (BorrowMaterial bm : borrow.getBorrowMaterials()) {

			BorrowMaterialDesc desc = new BorrowMaterialDesc();
			String description = bm.getDescription();
			if (description != null) desc.description = description;

			String srcTaxonomy = bm.getSrcTaxonomy();
			if (srcTaxonomy != null) desc.taxon = srcTaxonomy;
			
			Taxon taxon = bm.getTaxon();
			if (taxon != null) {
				String higherTaxon = taxon.getFullName();
				if (higherTaxon != null) desc.higherTaxon = higherTaxon;
			}
			
			Short itemCount = bm.getItemCount();
			if (itemCount == null) itemCount = 0;
			desc.borrowedGeneralCollections = itemCount;
			
			Short nonSpecimenCount = bm.getNonSpecimenCount();
			if (nonSpecimenCount == null) nonSpecimenCount = 0;
			desc.borrowedNonSpecimens = nonSpecimenCount;
			
			Short typeCount = bm.getTypeCount();
			if (typeCount == null) typeCount = 0;
			desc.borrowedTypes = typeCount;
			
			this.generalCollectionsCount += itemCount;
			this.nonSpecimensCount += nonSpecimenCount;
			this.typesCount += typeCount;
			
			this.totalBorrowedSum += (itemCount + nonSpecimenCount + typeCount);
			
			for (BorrowReturnMaterial brm : bm.getBorrowReturnMaterials()) {

				Short returnedItemCount = brm.getItemCount();
				if (returnedItemCount == null) returnedItemCount = 0;
				desc.returnedGeneralCollections += returnedItemCount;
				this.generalCollectionsBalanceDueCount -= returnedItemCount;
				
				Short returnedNonSpecimenCount = brm.getNonSpecimenCount();
				if (returnedNonSpecimenCount == null) returnedNonSpecimenCount = 0;
				desc.returnedNonSpecimens += returnedNonSpecimenCount;
				this.nonSpecimensBalanceDueCount -= returnedNonSpecimenCount;
				
				Short returnedTypeCount = brm.getTypeCount();
				if (returnedTypeCount == null) returnedTypeCount = 0;
				desc.returnedTypes += returnedTypeCount;
				this.typesBalanceDueCount -= returnedTypeCount;
				
				this.totalReturnedSum += (returnedItemCount + returnedNonSpecimenCount + returnedTypeCount);
				this.totalBalanceDueSum -= (returnedItemCount + returnedNonSpecimenCount + returnedTypeCount);
			}
		}
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
	
	private String extractForUseBy(Borrow borrow) {

		String forUseBy = null;
		
		for (BorrowAgent borrowAgent : borrow.getBorrowAgents()) {
			if (ROLE.Receiver.equals(borrowAgent.getRole())) {
				forUseBy = borrowAgent.getAgent().getLastName();
			}
		}

		return forUseBy;
	}
	
	private class BorrowMaterialDesc {
		int borrowedGeneralCollections = 0;
		int borrowedTypes = 0;
		int borrowedNonSpecimens = 0;
		
		int returnedGeneralCollections = 0;
		int returnedTypes = 0;
		int returnedNonSpecimens = 0;
		
		int generalCollectionsBalanceDue = 0;
		int typesBalanceDue = 0;
		int nonSpecimensBalanceDue = 0;
		
		String description = "";		
		String taxon = "";
		String higherTaxon = "";
	}
}