package edu.harvard.huh.oai.provider.dwc.service.impl;

import edu.ku.brc.specify.datamodel.CollectionObject;
import au.org.tern.ecoinformatics.oai.provider.service.IdentifierService;

public class SpecifyIdentifierService implements IdentifierService {

	@Override
	public String createOaiIdentifier(Object object) {
		if (object instanceof CollectionObject) {
			return "co" + ((CollectionObject) object).getId();
		}
		return null;
	}

	@Override
	public String extractInternalIdentifier(String string) {
		if (string.startsWith("co")) {
			return string.substring(2);
		}
		return null;
	}

	@Override
	public String extractSetSpec(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNamespace() {
		// TODO Auto-generated method stub
		return null;
	}

}
