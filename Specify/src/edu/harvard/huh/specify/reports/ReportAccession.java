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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.AccessionPreparation;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.ui.UIRegistry;

/** Java object representation of a Loan Report for use with HUH loans.
 * To be marshaled into xml and then transformed into a report document
 * (PDF, HTML, etc) via xslt and Xalan.
 * 
 * @author mkelly
 * @author lowery
 *
 */
public class ReportAccession {
	private static final int REGION_RANK_ID = 150;
	
	private String reportsDir = UIRegistry.getDefaultWorkingPath() + File.separator + ReportXslFiles.REPORTS_DIR;
	
	private String accessionNumber;
	private String institution; // Text1
	private String accessionDate;
	private String recipientName; // accessionAgent(role="Receiver")

	private String reportTitle;

	private String staffName; // accessionAgent(role="Collector") but also see getStaff(Accession)
	private String affiliation; // Text1
	
	private String description;
	
	private List<Region> regions;

	private int nonTypeCount;
	private int typeCount;
	private int nonSpecimenCount;
	private int discardCount;
	private int distributeCount;
	private int returnCount;
	private int net;
	private int total;
	
	/** Default constructor explicitly declared for the purposes of JibX binding requirements
	 * 
	 */
	public ReportAccession() { }
	
	/** The constructor that takes a Loan object as an argument populates the
	 * ReportLoan object fields with the correct values to be used on the report.
	 * 
	 * @param loan
	 */
	public ReportAccession(Accession accession) {
		
		// upper right summary
		accessionNumber = accession.getAccessionNumber();
		institution = accession.getText1();
		accessionDate = getAccessionDate(accession);
		recipientName = getRecipient(accession);

		// title
		reportTitle = getReportTitle(accession);
		
		// staff, affiliation
		staffName = getStaff(accession);
		affiliation = institution;
		
		// description
		description = accession.getAccessionCondition();
		
		// report
		regions = getRegions(accession);
		
		// totals
		updateTotals(regions);
	}
	
	private void updateTotals(List<Region> regions2) {
		for (Region r : regions) {
			nonTypeCount += r.nonTypeCount;
			typeCount += r.typeCount;
			nonSpecimenCount += r.nonSpecimenCount;
			discardCount += r.discardCount;
			distributeCount += r.distributeCount;
			returnCount += r.returnCount;
			net += r.net;
			total += r.total;
		}
		
	}

	/**
	 * Inner class that represents the aggregate counts of accession
	 * preparations by region.
	 * 
	 * @author mkelly
	 *
	 */
	private class Region implements Comparable<Region> {
		private String name = "";
		private int nonTypeCount;
		private int typeCount;
		private int nonSpecimenCount;
		private int discardCount;
		private int distributeCount;
		private int returnCount;
		private int net;
		private int total;
		
		public int compareTo(Region region) {
			return name.compareTo(region.name);
		}
	}
	
	private String getReportTitle(Accession accession) {
		
		String type = accession.getType();
		if (type.equals("FieldWork")) {
			return "Incoming Staff Collection";
		}

		if (type.equals("Gift") ||
				type.equals("Exchange") ||
				type.equals("Purchase")) {
			return "Incoming Exchange, Gift, or Purchase";
		}

		return "Accession";
	}
	
	private String getRecipient(Accession accession) {
		
		Agent a = getAgent(accession, "Receiver");
		
		String name = toString(a);
		return name;
	}

	private String getStaff(Accession accession) {

		Agent student = getAgent(accession, "Student");
		Agent collector = getAgent(accession, "Collector");
		Agent staff = getAgent(accession, "Staff");
		Agent sponsor = getAgent(accession, "Sponsor");
		
		String name = "";
		String advisorName = "";
		
		if (student != null) {
			name = toString(student);
			
			Agent advisor = collector;
			if (advisor == null) advisor = staff;
			if (advisor == null) advisor = sponsor;
			if (advisor != null) advisorName = ", advisor: " + toString(advisor);
		}
		else {
			if (collector != null) name = toString(collector);
			else if (staff != null) name = toString(staff);
		}

		return name + advisorName;
	}
	
	private List<Region> getRegions(Accession accession) {
		List<Region> regions = new ArrayList<Region>();
		
		for (AccessionPreparation ap : accession.getAccessionPreparations()) {
			
			Region r = new Region();
			
			Preparation p = ap.getPreparation();
			if (p != null) {
				Geography g = p.getGeography();
				
				if (g != null) {
					Geography parent = g.getParent();
					Integer rankId = g.getRankId();

						while (rankId > REGION_RANK_ID && p != null) {
							
							rankId = g.getRankId();
							g = parent;
							parent = g.getParent();
						}
						r.name = g.getName();
				}
			}
			
			r.nonTypeCount = ap.getItemCount();
			r.typeCount = ap.getTypeCount();
			r.nonSpecimenCount = ap.getNonSpecimenCount();
			r.discardCount = ap.getDiscardCount();
			r.distributeCount = ap.getDistributeCount();
			r.returnCount = ap.getReturnCount();
			r.total = r.nonTypeCount + r.typeCount + r.nonSpecimenCount;
			r.net = r.total - r.discardCount - r.distributeCount - r.returnCount;
			
			regions.add(r);
		}
		
		return regions;
	}
	
	private Agent getAgent(Accession accession, String role) {
		Agent a = null;
		for (AccessionAgent aa : accession.getAccessionAgents()) {
			if (role.equals(aa.getRole())) {
				a = aa.getAgent();
				break;
			}
		}
		return a;
	}

	private String toString(Agent a) {
		if (a == null) return "";
		
		String firstName = a.getFirstName();
		String lastName = a.getLastName();
		
		if (firstName != null && lastName != null) return firstName + " " + lastName;
		else if (firstName == null && lastName != null) return lastName;
		else if (firstName != null && lastName == null) return firstName;
		else return "";
	}

	private String getAccessionDate(Accession accession) {
		Calendar c = accession.getDateAccessioned();
		if (c == null) return "";
		
		DateFormat f = new SimpleDateFormat("dd MMM YYYY");
		String accessionDate = f.format(c.getTime());
		return accessionDate;
	}
}