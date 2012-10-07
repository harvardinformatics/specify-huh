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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.ui.UIRegistry;

/** Java object representation of a Loan Report for use with HUH loans.
 * To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author lowery
 *
 */
public class OutgoingGiftNotice {
	
	private static final Logger log = Logger.getLogger(OutgoingGiftNotice.class);
	
	private static final int REGION_RANK_ID = 100;
	
	private String reportsDir = UIRegistry.getDefaultWorkingPath() + File.separator + ReportXslFiles.REPORTS_DIR;
	
	// gift
	private Calendar dateReceived;
	private String forUseBy;         // text1
	private Calendar giftDate;
	private String giftNumber;
	private String herbarium;        // text2
	private Boolean isAcknowledged; // yesNo1
	private Boolean isTheirRequest; // yesNo2
	private String purposeOfGift;
	private String receivedComments;
	private String remarks;
	private String specialConditions;
	private String srcGeography;
	private String srcTaxonomy;
	
	private List<OutGiftDesc> outGiftDescs;
	
	// gift running totals
	
	private int generalCollectionsOutCount;
	private int nonSpecimensOutCount;
	private int typesOutCount;
		
	private int totalGiftOutSum;
	
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
	public OutgoingGiftNotice() {
		outGiftDescs = new ArrayList<OutGiftDesc>();
	}
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param gift
	 */
	public OutgoingGiftNotice(Gift gift) {
		if(gift.getShipments() != null)
			processShipment(gift.getShipments());
		
		dateReceived = gift.getDateReceived();
		forUseBy = gift.getText1();
		giftDate = gift.getGiftDate();
		giftNumber = gift.getGiftNumber();
		herbarium = gift.getText2();
		purposeOfGift = gift.getPurposeOfGift();
		receivedComments = gift.getReceivedComments();
		remarks = gift.getRemarks();
		srcGeography = gift.getSrcGeography();
		srcTaxonomy = gift.getSrcTaxonomy();
		specialConditions = gift.getSpecialConditions();
		
		isAcknowledged = gift.getYesNo1();
		isTheirRequest = gift.getYesNo2();
		
		
		outGiftDescs = new ArrayList<OutGiftDesc>();
		
		for (GiftPreparation eop : gift.getGiftPreparations()) {

			OutGiftDesc desc = new OutGiftDesc();
			String description = eop.getDescriptionOfMaterial();
			if (description != null) desc.descriptionOfMaterial = description;

			Preparation prep = eop.getPreparation();
			if (prep != null) {
				Geography geography = prep.getGeography();
				if (geography != null) {
					if (geography.getRankId() > REGION_RANK_ID) {
						desc.geography = geography.getFullName();
					}
					else {
						desc.geography = geography.getName();
					}
					log.debug("ExchangeOutPreparation geography: " + desc.geography);
				}
				
				Taxon taxon = prep.getTaxon();
				if (taxon != null) {
					desc.taxon = taxon.getFullName();
				}
			}
			
			Integer itemCount = eop.getItemCount();
			if (itemCount == null) itemCount = 0;
			desc.generalCollectionsOut = itemCount;
			this.generalCollectionsOutCount += itemCount;
			
			Integer nonSpecimenCount = eop.getNonSpecimenCount();
			if (nonSpecimenCount == null) nonSpecimenCount = 0;
			desc.nonSpecimensOut = nonSpecimenCount;
			this.nonSpecimensOutCount += nonSpecimenCount;
			
			Integer typeCount = eop.getTypeCount();
			if (typeCount == null) typeCount = 0;
			desc.typesOut = typeCount;
			this.typesOutCount += typeCount;
			
			this.totalGiftOutSum += (itemCount + typeCount + nonSpecimenCount);
			desc.total = (itemCount + nonSpecimenCount + typeCount);
			
			outGiftDescs.add(desc);
			
			log.warn(desc);
			System.out.println(desc);
		}
		Collections.sort(outGiftDescs, new Comparator<OutGiftDesc>() {

			@Override
			public int compare(OutGiftDesc o1, OutGiftDesc o2) {
				// TODO Auto-generated method stub
				return o2.total - o1.total;
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
	
	private class OutGiftDesc {
		
		int generalCollectionsOut = 0;
		int typesOut = 0;
		int nonSpecimensOut = 0;
		
		int total = 0;
		
		String descriptionOfMaterial = "";
		String geography;
		String taxon;
		
		public String toString() {
			return "items: " + generalCollectionsOut + " typesOut: " + typesOut + " nonSpecimens: " + nonSpecimensOut + " " + geography;
		}
	}
}