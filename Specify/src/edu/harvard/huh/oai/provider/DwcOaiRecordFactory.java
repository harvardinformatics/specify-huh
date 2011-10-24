package edu.harvard.huh.oai.provider;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.harvard.huh.oai.provider.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import au.org.tern.ecoinformatics.oai.provider.BasicOaiRecordFactory;
import au.org.tern.ecoinformatics.oai.provider.util.StaticApplicationContextHelper;

public class DwcOaiRecordFactory extends BasicOaiRecordFactory {

	private static Logger logger = Logger.getLogger(DwcOaiRecordFactory.class);
	
	@Override
	public String quickCreate(Object nativeItem, String schemaURL, String metadataPrefix)
			throws IllegalArgumentException, CannotDisseminateFormatException {
		
		if (!"dwc".equals(metadataPrefix)) throw new CannotDisseminateFormatException(metadataPrefix);

		return getCrosswalkFromSpringAppContext().crosswalkToString(nativeItem);
	}
	
	// this method is taken directly from the superclass; it only returns a different type of Crosswalk.  --mmk
	/**
	 * Grabs a reference to the Crosswalk singleton from the Spring applicationContext
	 * @return
	 */
	private Crosswalk getCrosswalkFromSpringAppContext() {
		return (Crosswalk) StaticApplicationContextHelper.getApplicationContext().getBean("crosswalk");
	}
	
	// this method is taken directly from the superclass; the parent class would have the wrong type of Crosswalk;
	// I suppose this means TODO: find another way of dealing with the EML vs. DWC object types in Crosswalk  --mmk
	@Override
	public String getDatestamp(Object nativeItem) {
		return getCrosswalkFromSpringAppContext().getDatestamp(nativeItem);
	}
	
	@Override
	public Iterator getSetSpecs(Object nativeItem) throws IllegalArgumentException {
		// top-level sets: Specimens, Botanists, Taxa
		// subsets of Specimens: by collection
		// subsets of Botanists: by division
		// subsets of Taxa: by discipline; for botany, for major group; and by family
		// TODO Auto-generated method stub
		return null;
	}
}
