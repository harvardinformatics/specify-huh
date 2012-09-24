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

import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionPreparation;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.ExchangeOutPreparation;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.dbsupport.HibernateDataProviderSession.HibernateQuery;
import edu.ku.brc.ui.UIRegistry;

/** Java object representation of a Loan Report for use with HUH loans.
 * To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author lowery
 *
 */
public class ExchangeOutNotice {
	
	private static final Logger log = Logger.getLogger(ExchangeOutNotice.class);
	
	private static final int REGION_RANK_ID = 150;
	
	private String reportsDir = UIRegistry.getDefaultWorkingPath() + File.separator + ReportXslFiles.REPORTS_DIR;
	
	// exchange out
	private String   descriptionOfMaterial;
	private Calendar exchangeDate;
	private String   exchangeNumber;
	private String   forUseBy;       // text1
	private String   herbarium;      // text2
	private Boolean  isAcknowledged; // yesNo1
	private Boolean  isTheirRequest; // yesNo2  
	private String   purpose;        // restrictions
	private String   remarks;
	
	private List<OutGeoBatchDesc> outGeoBatchDescs;
	
	// exchange running totals
	
	private int generalCollectionsOutCount;
	private int nonSpecimensOutCount;
	private int typesOutCount;
		
	private int totalExchangedOutSum;
	
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
	public ExchangeOutNotice() {
		outGeoBatchDescs = new ArrayList<OutGeoBatchDesc>();
	}
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param loan
	 */
	public ExchangeOutNotice(ExchangeOut exchangeOut) {
		if(exchangeOut.getShipments() != null)
			processShipment(exchangeOut.getShipments());
		
		descriptionOfMaterial = exchangeOut.getDescriptionOfMaterial();
		exchangeDate = exchangeOut.getExchangeDate();
		exchangeNumber = exchangeOut.getExchangeNumber();
		forUseBy = exchangeOut.getText1();
		herbarium = exchangeOut.getText2();
		isAcknowledged = exchangeOut.getYesNo1();
		isTheirRequest = exchangeOut.getYesNo2();
		purpose = exchangeOut.getRestrictions();
		remarks = exchangeOut.getRemarks();
		
		outGeoBatchDescs = new ArrayList<OutGeoBatchDesc>();
		
		for (ExchangeOutPreparation eop : exchangeOut.getExchangeOutPreparations()) {

			OutGeoBatchDesc desc = new OutGeoBatchDesc();
			String description = eop.getDescriptionOfMaterial();
			if (description != null) desc.description = description;

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
			
			this.totalExchangedOutSum += (itemCount + typeCount + nonSpecimenCount);
			desc.total = (itemCount + nonSpecimenCount + typeCount);
			
			outGeoBatchDescs.add(desc);
			Agent sentTo = exchangeOut.getAgentSentTo();
			
			log.warn(desc);
			System.out.println(desc);
		}
		Collections.sort(outGeoBatchDescs, new Comparator<OutGeoBatchDesc>() {

			@Override
			public int compare(OutGeoBatchDesc o1, OutGeoBatchDesc o2) {
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
	
	private class OutGeoBatchDesc {
		
		int generalCollectionsOut = 0;
		int typesOut = 0;
		int nonSpecimensOut = 0;
		
		int total = 0;
		
		String description = "";		
		String geography = "";
		
		public String toString() {
			return "items: " + generalCollectionsOut + " typesOut: " + typesOut + " nonSpecimens: " + nonSpecimensOut + " " + geography;
		}
	}
}