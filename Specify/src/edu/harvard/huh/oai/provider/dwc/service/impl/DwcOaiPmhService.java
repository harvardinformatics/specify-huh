package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import au.org.tern.ecoinformatics.oai.provider.model.ListIdentifiersResponse;
import au.org.tern.ecoinformatics.oai.provider.model.Record;
import au.org.tern.ecoinformatics.oai.provider.service.IdentifierService;
import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import au.org.tern.ecoinformatics.oai.provider.service.OaiPmhService;

public class DwcOaiPmhService implements OaiPmhService {

	private String dateFormat;
	private List<String> metadataFormats;
	private List<String> sets;
	
	private RecordFactory recordFactory;
	private IdentifierService identifierService;
	private NativeObjectService nativeObjectService;
	
	public DwcOaiPmhService() {
		this.metadataFormats = new ArrayList<String>();
	}

	@Override
	public String getDateFormat() {
		return dateFormat;
	}

	@Override
	public List<String> getMetadataFormats() {
		return metadataFormats;
	}

	@Override
	public Record getRecord(String identifier, String metadataPrefix) {

		String internalIdentifier = getIdentifierService().extractInternalIdentifier(identifier);

		Long id = null;
		try {
			Long.valueOf(internalIdentifier);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException(identifier); // TODO: handle bad identifier format
		}

		Object nativeObject = getNativeObjectService().getObject(id);

		String schemaUrl = null; // TODO: find schema url for metadata prefix
		
		String datestamp = getRecordFactory().getDatestamp(nativeObject);
		boolean deleted = getRecordFactory().isDeleted(nativeObject);

		String setSpec = getIdentifierService().extractSetSpec(identifier);
		
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
		List<Object> nativeObjects = getNativeObjectService().getObjects(from, until);
		
		return null;
	}

	private IdentifierService getIdentifierService() {
		return identifierService;
	}
	
	private NativeObjectService getNativeObjectService() {
		return nativeObjectService;
	}
	
	private RecordFactory getRecordFactory() {
		return recordFactory;
	}
}
