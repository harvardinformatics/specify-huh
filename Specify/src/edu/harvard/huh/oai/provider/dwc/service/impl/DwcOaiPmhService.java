package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.org.tern.ecoinformatics.oai.provider.model.ListIdentifiersResponse;
import au.org.tern.ecoinformatics.oai.provider.model.Record;
import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import au.org.tern.ecoinformatics.oai.provider.service.OaiPmhService;

public class DwcOaiPmhService implements OaiPmhService {

	private String dateFormat;
	private List<String> metadataFormats;
	private List<String> sets;
	
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
	public Record getRecord(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getSets() {
		return sets;
	}

	@Override
	public ListIdentifiersResponse listIdentifiers(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIdentifiersResponse listIdentifiers(Date arg0, Date arg1,
			String arg2, String arg3) {
		// TODO Auto-generated method stub
		return null;
	}

}
