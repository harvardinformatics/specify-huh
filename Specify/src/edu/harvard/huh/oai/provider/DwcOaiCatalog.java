package edu.harvard.huh.oai.provider;

import java.util.HashMap;
import java.util.Map;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.crosswalk.CrosswalkItem;
import ORG.oclc.oai.server.crosswalk.Crosswalks;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
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
}
