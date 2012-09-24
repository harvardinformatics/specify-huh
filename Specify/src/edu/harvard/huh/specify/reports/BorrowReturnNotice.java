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
import java.util.Comparator;
import java.util.GregorianCalendar;
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
	
	private static final int REGION_RANK_ID = 150;
	
	private String reportsDir = UIRegistry.getDefaultWorkingPath() + File.separator + ReportXslFiles.REPORTS_DIR;
	
	// borrow
	private String   borrowNumber;
	private Calendar currentDueDate;
	private Calendar dateClosed;
	private String   herbarium;       // text2: A/AMES/ECON/FH/G/NEBC
	private String   invoiceNumber;
	private Boolean  isAcknowledged;  // yesno1
	private Boolean  isClosed;
	private Boolean  isTheirRequest;  // yesNo2
	private Boolean  isVisitor;       // yesNo3
	private Calendar originalDueDate;
	private String   purpose;         // text1
	private Calendar receivedDate;
	private String   remarks;
	private String   userType;        // text3: student/staff

	// borrow agent
	private String forUseBy;
	
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
	
	// shipment
	private Calendar dateSent;	
	private int numberOfPackages;
	
	// shippedTo
	private String nameOfContact;
	private String title;
	private String jobTitle;
	
	// shippedTo organization
	private String institution;
	private String acronym;

	// shippedTo organization shipping address
	private String address1;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String country;
	
	// shippedBy
	private String nameOfShippedBy;
		
	
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
		purpose = borrow.getText1();   // purpose
		herbarium = borrow.getText2();   // herbarium / local unit
		userType = borrow.getText3();   // user type: student/staff
		isAcknowledged = borrow.getYesNo1(); // is acknowledged
		isTheirRequest = borrow.getYesNo2(); // is their request
		isVisitor = borrow.getYesNo3(); // is visitor
		
		generalCollectionsCount = 0;
		nonSpecimensCount = 0;
		typesCount = 0;
		
		generalCollectionsReturnedCount = 0;
		nonSpecimensReturnedCount = 0;
		typesReturnedCount = 0;
		
		generalCollectionsBalanceDueCount = 0;
		nonSpecimensBalanceDueCount = 0;
		typesBalanceDueCount  = 0;
		
		totalBorrowedSum = 0;
		totalReturnedSum = 0;
		totalBalanceDueSum = 0;
		
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
			this.generalCollectionsBalanceDueCount += itemCount;
			
			this.nonSpecimensCount += nonSpecimenCount;
			this.nonSpecimensBalanceDueCount += nonSpecimenCount;
			
			this.typesCount += typeCount;
			this.typesBalanceDueCount += typeCount;
			
			this.totalBorrowedSum += (itemCount + nonSpecimenCount + typeCount);
			this.totalBalanceDueSum += (itemCount + nonSpecimenCount + typeCount);

			for (BorrowReturnMaterial brm : bm.getBorrowReturnMaterials()) {

				BorrowReturnDesc returnDesc = new BorrowReturnDesc();
				
				returnDesc.dateReturned = brm.getReturnedDate();
				
				Short returnedItemCount = brm.getItemCount();
				if (returnedItemCount == null) returnedItemCount = 0;
				returnDesc.returnedGeneralCollections += returnedItemCount;
				this.generalCollectionsReturnedCount += returnedItemCount;
				this.generalCollectionsBalanceDueCount -= returnedItemCount;
				
				Short returnedNonSpecimenCount = brm.getNonSpecimenCount();
				if (returnedNonSpecimenCount == null) returnedNonSpecimenCount = 0;
				returnDesc.returnedNonSpecimens += returnedNonSpecimenCount;
				this.nonSpecimensReturnedCount += returnedNonSpecimenCount;
				this.nonSpecimensBalanceDueCount -= returnedNonSpecimenCount;
				
				Short returnedTypeCount = brm.getTypeCount();
				if (returnedTypeCount == null) returnedTypeCount = 0;
				returnDesc.returnedTypes += returnedTypeCount;
				this.typesReturnedCount += returnedTypeCount;
				this.typesBalanceDueCount -= returnedTypeCount;
				
				this.totalReturnedSum += (returnedItemCount + returnedNonSpecimenCount + returnedTypeCount);
				this.totalBalanceDueSum -= (returnedItemCount + returnedNonSpecimenCount + returnedTypeCount);
				
				desc.returnDescs.add(returnDesc);
			}
			
			Collections.sort(desc.returnDescs, new Comparator<BorrowReturnDesc>() {

				@Override
				public int compare(BorrowReturnDesc o1, BorrowReturnDesc o2) {
					// TODO Auto-generated method stub
					return o1.dateReturned.compareTo(o2.dateReturned);
				}
				
			});
			
			this.borrowMaterialDescs.add(desc);
		}
		
		Collections.sort(this.borrowMaterialDescs, new Comparator<BorrowMaterialDesc>() {

			@Override
			public int compare(BorrowMaterialDesc o1, BorrowMaterialDesc o2) {
				// TODO Auto-generated method stub
				return o1.higherTaxon.compareTo(o2.higherTaxon);
			}
			
		});
	}
	
	/** Finds the current shipment and pulls the agent address values, date and other
	 * information to be used on the report
	 * 
	 * @param shipments
	 */
	private void processShipment(Set<Shipment> shipments) {
		Shipment curr = null;
		for (Shipment s : shipments) {
			if (curr == null) curr = s;
			
			Calendar currShipmentDate = curr == null ? null : curr.getShipmentDate();
			Calendar shipmentDate = s.getShipmentDate();
			if (currShipmentDate == null || (shipmentDate != null && currShipmentDate.before(s.getShipmentDate()))) {
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
		
		int generalCollectionsBalanceDue = 0;
		int typesBalanceDue = 0;
		int nonSpecimensBalanceDue = 0;
		
		String description = "";		
		String taxon = "";
		String higherTaxon = "";
		
		List<BorrowReturnDesc> returnDescs = new ArrayList<BorrowReturnDesc>();
	}
	
	private class BorrowReturnDesc {
		
		Calendar dateReturned = GregorianCalendar.getInstance();
		
		int returnedGeneralCollections = 0;
		int returnedTypes = 0;
		int returnedNonSpecimens = 0;
	}
}