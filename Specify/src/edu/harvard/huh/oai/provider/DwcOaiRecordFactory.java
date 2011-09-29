package edu.harvard.huh.oai.provider;

import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import au.org.tern.ecoinformatics.oai.provider.BasicOaiRecordFactory;
import au.org.tern.ecoinformatics.oai.provider.crosswalk.Crosswalk;
import au.org.tern.ecoinformatics.oai.provider.util.StaticApplicationContextHelper;

public class DwcOaiRecordFactory extends BasicOaiRecordFactory {

	@Override
	public String quickCreate(Object nativeItem, String schemaURL, String metadataPrefix)
			throws IllegalArgumentException, CannotDisseminateFormatException {
		
		if (!"dwc".equals(metadataPrefix)) throw new CannotDisseminateFormatException(metadataPrefix);

		return getCrosswalkFromSpringAppContext().crosswalkToString(nativeItem);
	}
	
	// this method is taken directly from the superclass  --mmk
	/**
	 * Grabs a reference to the Crosswalk singleton from the Spring applicationContext
	 * @return
	 */
	private Crosswalk getCrosswalkFromSpringAppContext() {
		
		return (Crosswalk) StaticApplicationContextHelper.getApplicationContext().getBean("crosswalk");
	}
}
