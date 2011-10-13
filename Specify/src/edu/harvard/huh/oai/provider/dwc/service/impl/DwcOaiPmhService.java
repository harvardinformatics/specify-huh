package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import au.org.tern.ecoinformatics.oai.provider.model.IdentifierRecord;
import au.org.tern.ecoinformatics.oai.provider.model.ListIdentifiersResponse;
import au.org.tern.ecoinformatics.oai.provider.model.Record;
import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import au.org.tern.ecoinformatics.oai.provider.service.OaiPmhService;

public class DwcOaiPmhService implements OaiPmhService {

	private String dateFormatString;
	private SimpleDateFormat dateFormat;
	private List<String> metadataFormats;
	private List<String> sets;
	
	private RecordFactory recordFactory;
	private NativeObjectService nativeObjectService;
	
	public DwcOaiPmhService() {
		this.dateFormatString = "YYYY-MM-dd";
		this.dateFormat = new SimpleDateFormat(dateFormatString);
		this.metadataFormats = new ArrayList<String>();
		this.sets = new ArrayList<String>();
	}

	
	public void setNativeObjectService(NativeObjectService nativeObjectService) {
		this.nativeObjectService = nativeObjectService;
	}
	
	public void setRecordFactory(RecordFactory recordFactory) {
		this.recordFactory = recordFactory;
	}

	@Override
	public String getDateFormat() {
		return dateFormatString;
	}

	@Override
	public List<String> getMetadataFormats() {
		return metadataFormats;
	}

	@Override
	public Record getRecord(String identifier, String metadataPrefix) {

		String internalIdentifier = getRecordFactory().fromOAIIdentifier(identifier);

		Long id = null;
		try {
			Long.valueOf(internalIdentifier);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException(identifier); // TODO: handle bad identifier format
		}

		Object nativeObject = getNativeObjectService().getObject(id);
		if (nativeObject == null) return null;

		String schemaUrl = null; // TODO: find schema url for metadata prefix
		
		String datestamp = getRecordFactory().getDatestamp(nativeObject);
		boolean deleted = getRecordFactory().isDeleted(nativeObject);

		String setSpec = null; // TODO: implement sets?
		
		String namespacedRecordXml = null;
		try {
			namespacedRecordXml = getRecordFactory().quickCreate(nativeObject, schemaUrl, metadataPrefix);
		}
		catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (CannotDisseminateFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new Record(identifier, datestamp, deleted, setSpec, namespacedRecordXml);
	}

	@Override
	public List<String> getSets() {
		return sets;
	}

	@Override
	public ListIdentifiersResponse listIdentifiers(String resumptionToken) {
		// TODO Find exception to be thrown if resumption tokens not supported, or ignore?
		return listIdentifiers(null, null, "dwc", null);
	}

	@Override
	public ListIdentifiersResponse listIdentifiers(Date from, Date until, String metadataPrefix, String setSpec) {
		// TODO Throw an exception if sets aren't supported?  Or ignore?
		if (!getMetadataFormats().contains(metadataPrefix)) {
			return null;
		}

		List<IdentifierRecord> identifierRecords = new ArrayList<IdentifierRecord>();
		
		for (Object nativeObject : getNativeObjectService().getObjects(from, until)) {

			String identifier = getRecordFactory().getOAIIdentifier(nativeObject);
			Date date = parseDate(getRecordFactory().getDatestamp(nativeObject));
			
			IdentifierRecord identifierRecord = new IdentifierRecord();
			identifierRecord.setDate(date);
			identifierRecord.setIdentifier(identifier);
			identifierRecord.setDeleted(false);
			
			identifierRecords.add(identifierRecord);
		}
		
		ListIdentifiersResponse response = new ListIdentifiersResponse();
		response.setIdentifierRecords(identifierRecords);

		return response;
	}
	
	private NativeObjectService getNativeObjectService() {
		return nativeObjectService;
	}
	
	private RecordFactory getRecordFactory() {
		return recordFactory;
	}
	
	private Date parseDate(String date) {
		try {
			return dateFormat.parse(date);
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
}
