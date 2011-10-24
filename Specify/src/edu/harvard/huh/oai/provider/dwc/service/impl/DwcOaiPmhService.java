package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
 
import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import au.org.tern.ecoinformatics.oai.provider.model.IdentifierRecord;
import au.org.tern.ecoinformatics.oai.provider.model.ListIdentifiersResponse;
import au.org.tern.ecoinformatics.oai.provider.model.Record;
import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import au.org.tern.ecoinformatics.oai.provider.service.OaiPmhService;

public class DwcOaiPmhService implements OaiPmhService {

	// TODO: slf4j?
	private static Logger logger = Logger.getLogger(DwcOaiPmhService.class);
	
	private String dateFormatString;
	private SimpleDateFormat dateFormat;
	private List<String> metadataFormats;
	private List<String> sets;
	
	private RecordFactory recordFactory;
	private NativeObjectService nativeObjectService;
	
	public DwcOaiPmhService() {
		this.dateFormatString = "yyyy-MM-dd";
		this.dateFormat = new SimpleDateFormat(dateFormatString);
		this.metadataFormats = new ArrayList<String>();
		this.metadataFormats.add("dwc"); // TODO: how do we find these?
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

		Long id = null;
		try {
			String internalIdentifier = getRecordFactory().fromOAIIdentifier(identifier);
			id = Long.valueOf(internalIdentifier);
		}
		catch (NumberFormatException e) {
			logger.debug(e);
			return null; // TODO: handle bad identifier format
			// throw new IdDoesNotExistException(identifier);
		}

		Object nativeObject = getNativeObjectService().getObject(id);
		if (nativeObject == null) {
			logger.debug("NativeObjectService did not find an object with id " + id);
			return null;
		}
		
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
			logger.debug(e);
			return null;
		}
		catch (CannotDisseminateFormatException e) {
			// TODO Auto-generated catch block
			logger.debug(e);
			return null;
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
		// TODO Do we check the logic of the dates here?
		
		if (!getMetadataFormats().contains(metadataPrefix)) {
			return null;
		}

		List<IdentifierRecord> identifierRecords = new ArrayList<IdentifierRecord>();
		
		for (Object nativeObject : getNativeObjectService().getObjects(from, until)) {

			// TODO identify for each object whether it supports the given metadata format.
			// TODO identify for each object whether it matches the given setSpec.
			// TODO seems strange to be parsing dates here

			String identifier = getRecordFactory().getOAIIdentifier(nativeObject);
			Date date = parseDate(getRecordFactory().getDatestamp(nativeObject));
			boolean isDeleted = getRecordFactory().isDeleted(nativeObject);
			
			IdentifierRecord identifierRecord = new IdentifierRecord();
			identifierRecord.setDate(date);
			identifierRecord.setIdentifier(identifier);
			identifierRecord.setDeleted(isDeleted);
			
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
