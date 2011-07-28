package edu.harvard.huh.specify.reports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.stream.StreamSource;

import edu.harvard.huh.binding.DataModelObjectMarshaller;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.datamodel.Taxon;

public class ReportLoanTest {
	public static void main(String[] args) {		
		System.out.println("TestCase01: new empty Loan object");
		Loan loan = new Loan();
		test(loan);
		
		System.out.println("TestCase02: add empty shipment to new Loan");
		loan = new Loan();
		Set<Shipment> shipments = new HashSet<Shipment>();
		shipments.add(new Shipment());
		loan.setShipments(shipments);
		test(loan);
		
		System.out.println("TestCase03: add empty LoanPreparation to new Loan");
		loan = new Loan();
		Set<LoanPreparation> loanPreparations = new HashSet<LoanPreparation>();
		loanPreparations.add(new LoanPreparation());
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase04: add shipment w/shippedTo and shippedBy agents to new Loan");
		loan = new Loan();
		shipments = new HashSet<Shipment>();
		Shipment shipment = new Shipment();
		shipment.setShippedBy(new Agent());
		shipment.setShippedTo(new Agent());
		shipments.add(shipment);
		loan.setShipments(shipments);
		test(loan);
		
		System.out.println("TestCase05: add empty Organization and Address to shippedTo agent");
		loan = new Loan();
		shipments = new HashSet<Shipment>();
		shipment.getShippedTo().setOrganization(new Agent());
		Set<Address> addresses = new HashSet<Address>();
		addresses.add(new Address());
		shipment.getShippedTo().setAddresses(addresses);
		shipments.add(shipment);
		loan.setShipments(shipments);
		test(loan);
		
		System.out.println("TestCase06: add PrepType \"Lot\" to empty Preparation in LoanPreparation");
		loan = new Loan();
		loanPreparations = new HashSet<LoanPreparation>();
		PrepType prepTypeLot = new PrepType();
		prepTypeLot.setName("Lot");
		LoanPreparation loanPreparation = new LoanPreparation();
		loanPreparation.setPreparation(new Preparation());
		loanPreparation.getPreparation().setPrepType(prepTypeLot);
		loanPreparations.add(loanPreparation);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase07: add empty Fragment to Preparation with PrepType \"Sheet\"");
		loan = new Loan();
		loanPreparations = new HashSet<LoanPreparation>();
		PrepType prepTypeSheet = new PrepType();
		prepTypeSheet.setName("Sheet");
		loanPreparation = new LoanPreparation();
		Preparation preparation = new Preparation();
		preparation.setPrepType(prepTypeSheet);
		Set<Fragment> fragments = new HashSet<Fragment>();
		Fragment fragment = new Fragment();
		fragments.add(fragment);
		preparation.setFragments(fragments);
		loanPreparation.setPreparation(preparation);
		loanPreparations.add(loanPreparation);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase08: add empty Determination to Fragment in Preparation");
		loan = new Loan();
		Set<Determination> determinations = new HashSet<Determination>();
		determinations.add(new Determination());
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setDeterminations(determinations);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase09: add Determination marked isCurrent to Fragment in Preparation");
		loan = new Loan();
		determinations = new HashSet<Determination>();
		Determination determination = new Determination();
		determination.setIsCurrent(true);
		determinations.add(determination);
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setDeterminations(determinations);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase10: add Taxon to Determination marked isCurrent");
		loan = new Loan();
		determinations = new HashSet<Determination>();
		determination.setTaxon(new Taxon());
		determinations.add(determination);
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setDeterminations(determinations);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase11: add empty CollectionObject to Fragment in Preparation");
		loan = new Loan();
		CollectionObject collectionObject = new CollectionObject();
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setCollectionObject(collectionObject);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase12: add empty CollectingEvent to CollectionObject in Fragment");
		loan = new Loan();
		CollectingEvent collectingEvent = new CollectingEvent();
		collectionObject.setCollectingEvent(collectingEvent);
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setCollectionObject(collectionObject);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase11: add empty Collector to CollectingEvent in CollectionObject");
		loan = new Loan();
		Set<Collector> collectors = new HashSet<Collector>();
		collectors.add(new Collector());
		collectingEvent.setCollectors(collectors);
		collectionObject.setCollectingEvent(collectingEvent);
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setCollectionObject(collectionObject);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
		
		System.out.println("TestCase12: add empty Locality to CollectingEvent in CollectionObject");
		loan = new Loan();
		Locality locality = new Locality();
		collectingEvent.setLocality(locality);
		collectionObject.setCollectingEvent(collectingEvent);
		for (LoanPreparation lp : loanPreparations)
			for (Fragment f : lp.getPreparation().getFragments())
				f.setCollectionObject(collectionObject);
		loan.setLoanPreparations(loanPreparations);
		test(loan);
	}
	
	private static void test(Loan loan) {
		try {
			new ReportLoan(loan);
			System.out.println("ReportLoan object successfully created.");
		} catch (Exception e) {
			e.printStackTrace();
		}
        try {
            ReportLoan rp = new ReportLoan(loan);
            File file = new File(ReportXslFiles.REPORTS_DIR + "out.fo");
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
            
			DataModelObjectMarshaller<ReportLoan> marshaller = new DataModelObjectMarshaller<ReportLoan>(rp);
        	marshaller.transform(new StreamSource(ReportXslFiles.LOAN_REPORT), out);
            DataModelObjectReport report = new DataModelObjectReport(new StreamSource(new ByteArrayInputStream(out.toByteArray())));
            report.generatePDF();

			File reportxml = new File(ReportXslFiles.REPORTS_DIR + "LoanReport.xml");
            marshaller.marshal(new FileOutputStream(reportxml));
			System.out.println("Report pdf successfully generated.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
