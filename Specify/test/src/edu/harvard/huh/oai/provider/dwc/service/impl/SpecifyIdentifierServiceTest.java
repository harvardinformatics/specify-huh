package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.ui.UIRegistry;

public class SpecifyIdentifierServiceTest extends TestCase {

	DBTableIdMgr dbTableIdMgr = null;
	SpecifyIdentifierService idService = null;

	@Before
	public void setUp() {
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("/test.properties"));
		}
		catch (IOException e) {
			fail(e.getMessage());
		}
		String workingPath = properties.getProperty("workingPath");	
		UIRegistry.setDefaultWorkingPath(workingPath); // we need to be able to find specify_tableid_listing.xml from here for DBTableIdMgr
		
		dbTableIdMgr = DBTableIdMgr.getInstance();
		idService = new SpecifyIdentifierService(dbTableIdMgr);
	}

	@Test
	public void testCreateOaiIdentifier() {
		
		// should fail on non-Specify objects
		try {
			Object object = new Integer(0);
			idService.createOaiIdentifier(object);
			fail("Shouldn't be able to create oai identifiers for non-DataModelObjBase objects");
		}
		catch (IllegalArgumentException e) {
			;
		}

		CollectionObject collObj = new CollectionObject();
		
		// should fail on objects without an id
		try {
			idService.createOaiIdentifier(collObj);
			fail("Shouldn't be able to create oai identifiers for objects without id");
		}
		catch (IllegalArgumentException e) {
			;
		}
		
		// identifier should match table abbrev + id
		collObj.setCollectionObjectId(100001);
		String idString = String.valueOf(collObj.getId());
		String tableAbbrev = getTableAbbrev(collObj.getClass());
		String oaiId = tableAbbrev + "." + idString;
		
		assertEquals(oaiId, idService.createOaiIdentifier(collObj));
	}
	
	private String getTableAbbrev(Class<?> clss) {
		int tableId = dbTableIdMgr.getIdByClassName(clss.getName());
		return dbTableIdMgr.getInfoById(tableId).getAbbrev();
	}
	
	@Test
	public void testExtractInternalIdentifier() {
		
		// should return null unless matches table abbrev + "." + id format
		String badFormat = "foo1";
		String oaiId = idService.extractInternalIdentifier(badFormat);
		assertNull(oaiId);
		
		// should return null unless there's a recognizable table abbreviation
		String badTableAbbrevFormat = "xyz.123";
		oaiId = idService.extractInternalIdentifier(badTableAbbrevFormat);
		assertNull(oaiId);
		
		String collObjTableAbbrev = getTableAbbrev(CollectionObject.class);
		String idString = "1001";
		String goodOaiId = collObjTableAbbrev + "." + "1001";

		assertEquals(idString, idService.extractInternalIdentifier(goodOaiId));
	}
	
	@Test
	public void testGetNamespace() {
		// This doesn't test much, it's just here as documentation.  :)
		String ns = "http://rs.tdwg.org/dwc/xsd/simpledarwincore/";
		
		assertEquals(ns, idService.getNamespace());
	}
}
