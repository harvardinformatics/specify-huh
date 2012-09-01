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
import java.util.Calendar;

import org.apache.log4j.Logger;

import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.ui.UIRegistry;

/**
 * Java object representation of a Shipment Notice for use with miscellaneous outgoing
 * HUH transactions. To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author mkelly
 * @author lowery
 *
 */
public class ShippingNotice {
	
	private static final Logger log = Logger.getLogger(ShippingNotice.class);
	
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
	private Calendar shipmentDate;
	private int numberOfPackages;
	private String shipmentNumber;
	private String remarks;
	
	/** Default constructor explicitly declared for the purposes of JibX binding requirements
	 * 
	 */
	public ShippingNotice() { }
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param loan
	 */
	public ShippingNotice(Shipment shipment) {
		
		log.debug("New ShippingNotice");
		
		remarks = shipment.getRemarks();
		log.debug("remarks: " + remarks);
		
		shipmentNumber = shipment.getShipmentNumber();
		log.debug("shipmentNumber: " + shipmentNumber);
		
		shipmentDate = shipment.getShipmentDate();
		log.debug("shipmentDate: " + shipmentDate);
		
		numberOfPackages = shipment.getNumberOfPackages() != null ? shipment.getNumberOfPackages() : 0;
		log.debug("numberOfPackages: " + numberOfPackages);
		
		if (shipment.getShippedTo() != null) {
			
			nameOfContact = shipment.getShippedTo().getFirstName() + " " + shipment.getShippedTo().getLastName();
			log.debug("nameOfContact: " + nameOfContact);
			
			if (shipment.getShippedTo().getTitle() != null) {
				
				title = shipment.getShippedTo().getTitle();
				log.debug("title: " + title);
				
				title = title.substring(0,1).toUpperCase() + title.substring(1);
			}
			
			jobTitle = shipment.getShippedTo().getJobTitle() != null ? shipment.getShippedTo().getJobTitle() : "";
			log.debug("jobTitle: " + jobTitle);
			
			if (shipment.getShippedTo().getOrganization() != null) {
				
				institution =  shipment.getShippedTo().getOrganization().getLastName();
				log.debug("institution: " + institution);
				
				acronym = shipment.getShippedTo().getOrganization().getAbbreviation();
				log.debug("acronym: " + acronym);
			}

			if (shipment.getShippedTo().getAddresses() != null) {
				boolean foundAddress = extractAddress(shipment.getShippedTo());
				if (! foundAddress) {
					extractAddress(shipment.getShippedTo().getOrganization());
				}
			}
		}

		if (shipment.getShippedBy() != null) {

			nameOfShippedBy = shipment.getShippedBy().getFirstName() + " " + shipment.getShippedBy().getLastName();
			log.debug("nameOfShippedBy: " + nameOfShippedBy);
		}
	}
	
	private boolean extractAddress(Agent agent) {
		
		boolean foundAddress = false;
		
		if (agent != null) {
			for (Address a : agent.getAddresses()) {
				if (a.getIsShipping() != null && a.getIsShipping()) {

					foundAddress = true;

					address1 = a.getAddress();
					address2 = a.getAddress2();
					city = a.getCity();
					state = a.getState();
					zip = a.getPostalCode();
					country = a.getCountry();

					log.debug("address:\n" + address1);
					log.debug(address2);
					log.debug(city + " " + state + " " + zip);
					log.debug(country);
				}
			}
		}
		return foundAddress;
	}
}