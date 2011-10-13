package edu.harvard.huh.oai.provider;

import java.util.HashMap;
import java.util.Map;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.crosswalk.CrosswalkItem;
import ORG.oclc.oai.server.crosswalk.Crosswalks;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import au.org.tern.ecoinformatics.oai.provider.BasicOaiCatalog;

public class DwcOaiCatalog extends BasicOaiCatalog {

	private Crosswalks crosswalks;
	
	public DwcOaiCatalog() {
		; // removed parent class's code for setting crosswalks to getCrosswalks()  --mmk
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Crosswalks getCrosswalks() {
		if (crosswalks == null) {
			// modified superclass's version by replacing "eml" with "dwc."  --mmk
			Crosswalk crosswalk = new Crosswalk("dwc") {

				@Override
				public boolean isAvailableFor(Object nativeItem) {
					return true;
				}

				@Override
				public String createMetadata(Object nativeItem)
						throws CannotDisseminateFormatException {
					return null;
				}
				
			};
			CrosswalkItem item = new CrosswalkItem("dwc", "dwc", "dwc", "dwc", crosswalk, 0);
			Map crosswalkMap = new HashMap();
			crosswalkMap.put("dwc", item);
			crosswalks = new Crosswalks(crosswalkMap);
		}
		return crosswalks;
	}
	
	/**
	 * Return the appropriate record format for the given OAI identifier.
	 */
	@Override
	public String getRecord(String identifier, String metadataPrefix) throws IdDoesNotExistException, CannotDisseminateFormatException, OAIInternalServerError {
		try {
			return super.getRecord(identifier, metadataPrefix);
		}
		catch (NullPointerException e) {
			throw new IdDoesNotExistException(identifier); // TODO: move this check to the parent class
		}
	}
}
