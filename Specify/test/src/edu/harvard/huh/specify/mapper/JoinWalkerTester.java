package edu.harvard.huh.specify.mapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import edu.ku.brc.af.auth.JaasContext;
import edu.ku.brc.af.auth.SecurityMgr;
import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.formatters.DataObjFieldFormatMgr;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.GeoCoordDetail;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.ui.UIRegistry;

public class JoinWalkerTester extends TestCase {

	JoinWalker joinWalker = null;

	@Override
	public void setUp() {
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("/test.properties"));
		}
		catch (IOException e) {
			fail(e.getMessage());
		}
		
		String workingPath = properties.getProperty("workingPath");		
		String driver = properties.getProperty("driver");
		String dialect = properties.getProperty("dialect");
		String databaseName = properties.getProperty("databaseName");
		String connectionUrl = properties.getProperty("connectionUrl");
		String dbUserName = properties.getProperty("dbUserName");
		String dbPassword = properties.getProperty("dbPassword");
		String specifyUserName = properties.getProperty("specifyUserName");
		String specifyPassword = properties.getProperty("specifyPassword");
		
		System.setProperty(DBTableIdMgr.factoryName,  "edu.ku.brc.specify.config.SpecifyDBTableIdMgr");
		System.setProperty(AppContextMgr.factoryName, "edu.ku.brc.specify.config.SpecifyAppContextMgr");
		System.setProperty(SecurityMgr.factoryName,   "edu.ku.brc.af.auth.specify.SpecifySecurityMgr"); // AppContextMgr needs this one
        
		UIRegistry.setDefaultWorkingPath(workingPath); // we need to be able to find specify_tableid_listing.xml from here for DBTableIdMgr
		
		AppPreferences localPrefs = AppPreferences.getLocalPrefs();
        localPrefs.setDirPath(UIRegistry.getAppDataDir()); // AppPreferences.dirPath needs to be set in order to find formatters
		
        DBConnection.getInstance().setConnectionStr(connectionUrl); // SecurityMgr needs this; this is how you need to initialize it
        DBConnection.getInstance().setDatabaseName(databaseName);
        DBConnection.getInstance().setDialect(dialect);
        DBConnection.getInstance().setDriver(driver);  // not "setDriverName!"  ha!
        DBConnection.getInstance().setUsernamePassword(dbUserName, dbPassword);
        DBConnection.getInstance().getConnection();

        // the following need to be set so that there will be a table permission structure to use in testing
        String specifyKey = UserAndMasterPasswordMgr.encrypt(dbUserName, dbPassword, specifyPassword);
        
        // without the these being set, the JaasContext login will put up a window asking for credentials
        AppPreferences.getLocalPrefs().putBoolean(databaseName + "_" + specifyUserName + "_master.islocal", true);
        AppPreferences.getLocalPrefs().put(databaseName + "_" + specifyUserName + "_master.path", specifyKey);

        // DatabaseService, which is used for permissions lookups, creates a connection with the Jaas url
        // and user/password from the UserAndMasterPasswordMgr; make sure they're there
        UserAndMasterPasswordMgr.getInstance().set(specifyUserName, specifyPassword, databaseName);
        
        // without a Specify user (the Jaas "subject') we can't test permissions, this creates and sets the subject
		(new JaasContext()).jaasLogin(specifyUserName, specifyPassword, connectionUrl, driver, dbUserName, dbPassword);
		
		// permissions lookups fail unless this is set to true
		AppContextMgr.getInstance().setHasContext(true);

		// Permissions lookups fail unless there's a collection selected.
		// If you use the setup wizard to create your database, your collection's id is 4.
		// To set the Collection object's id field, you have to use this constructor.
		edu.ku.brc.specify.datamodel.Collection collection = new edu.ku.brc.specify.datamodel.Collection(4);
		
		AppContextMgr.getInstance().setClassObject(edu.ku.brc.specify.datamodel.Collection.class, collection);

		joinWalker = new JoinWalker(DBTableIdMgr.getInstance());
	}
	
	@Override
	public void tearDown() {
		DBConnection.shutdown();
	}

	// test walking a tableid join tree
	public void testGetPathValueForNamedRelationships() {
		
		// set up the CollectionObject object graph with the target value
		String lastName = "Kelly";

		CollectionObject collObj = new CollectionObject();
		CollectingEvent collEvent = new CollectingEvent();
		Locality locality = new Locality();
		GeoCoordDetail geoCoordDetail = new GeoCoordDetail();
		Agent agent = new Agent();
		
		collObj.setCollectingEvent(collEvent);
		collEvent.setLocality(locality);
		locality.setGeoCoordDetails(new HashSet<GeoCoordDetail>());
		locality.getGeoCoordDetails().add(geoCoordDetail);
		geoCoordDetail.setGeoRefDetBy(agent);
		agent.setLastName(lastName);
		agent.setAgentType(Agent.PERSON);  // type needs to be set for formatter to be called in Agent.toString()
		
		// set up the info we will have for finding that value
		String fieldName = "geoRefDetBy";
		String tablePath = "1,10,2,123-geoCoordDetails,5-geoRefDetBy";
		boolean isRelationship = false;

		// now go find it
		Object mappedValue = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);
		assertEquals(lastName, mappedValue.toString().trim());
		
		// try again without permission
		DBTableIdMgr.getInstance().getInfoById(2).getPermissions().setCanView(false);
		Object unviewable = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);
		assertNull(unviewable);
		DBTableIdMgr.getInstance().getInfoById(2).getPermissions().setCanView(true);
	}

	public void testGetPathValueForNumericPath() {

		// set up the CollectionObject object graph with the target value
		Double longitude = 34.802;

		CollectionObject collObj = new CollectionObject();
		CollectingEvent collEvent = new CollectingEvent();
		Locality locality = new Locality();

		collObj.setCollectingEvent(collEvent);
		collEvent.setLocality(locality);
		locality.setLongitude1(BigDecimal.valueOf(longitude));

		// set up the info we will have for finding that value
		String fieldName = "longitude1";
		String tablePath = "1,10,2";
		boolean isRelationship = false;

		// now go find it
		Object mappedValue = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);
		assertEquals(longitude, ((BigDecimal) mappedValue).doubleValue(), 0.00001);
	}

	public void testGetPathValueForOneOfManyDets() {
		// determinations are comparables!  yay!  HUH should modify compareTo.

		// set up the CollectionObject object graph with the target value
		String asterAlbaName = "Aster alba";
		String asterSerrataName = "Aster serrata";

		CollectionObject collObj = new CollectionObject();

		Taxon asterAlba = new Taxon();
		asterAlba.setFullName(asterAlbaName);
		asterAlba.setIsAccepted(true);

		Taxon asterSerrata = new Taxon();
		asterSerrata.setFullName(asterSerrataName);
		asterSerrata.setIsAccepted(true);

		Determination oldDet = new Determination();
		oldDet.setCollectionObject(collObj);
		oldDet.setTaxon(asterAlba);

		Determination newDet = new Determination();
		newDet.setCollectionObject(collObj);
		newDet.setTaxon(asterSerrata);
		newDet.setIsCurrent(true);

		collObj.setDeterminations(new HashSet<Determination>());

		// set up the info we will have for finding that value
		String fieldName = "fullName";
		String tablePath = "1,9-determinations,4-preferredTaxon";
		boolean isRelationship = false;

		// don't find it when it's not there
		Object noMappedValue = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);
		assertNull(noMappedValue);

		// do find it when it is.
		collObj.getDeterminations().add(oldDet);
		collObj.getDeterminations().add(newDet);
		Object mappedValue = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);
		assertEquals(asterSerrataName, mappedValue.toString().trim());
	}

	public void testGetPathValueForIsRelationship() {
		
		// set up the CollectionObject object graph with the target values
		String[] collectorNames = { "Kelly", "Clifton" };

		CollectionObject collObj = new CollectionObject();
		CollectingEvent collEvent = new CollectingEvent();

		Collector primaryCollector = new Collector();
		Agent primaryAgent = new Agent();

		Collector secondaryCollector = new Collector();
		Agent secondaryAgent = new Agent();

		primaryAgent.setLastName(collectorNames[0]);
		primaryAgent.setAgentType(Agent.PERSON);

		secondaryAgent.setLastName(collectorNames[1]);
		secondaryAgent.setAgentType(Agent.PERSON);

		primaryCollector.setAgent(primaryAgent);
		primaryCollector.setOrderNumber(1);
		primaryCollector.setCollectingEvent(collEvent);

		secondaryCollector.setAgent(secondaryAgent);
		secondaryCollector.setOrderNumber(2);
		secondaryCollector.setCollectingEvent(collEvent);

		collEvent.setCollectors(new HashSet<Collector>());
		collEvent.getCollectors().add(primaryCollector);
		collEvent.getCollectors().add(secondaryCollector);

		collObj.setCollectingEvent(collEvent);

		// set up the info we will have for finding that value
		String fieldName = "collectors";
		String tablePath = "1,10,30-collectors";
		boolean isRelationship = true;

		Object mappedValue = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);

		assertTrue(mappedValue instanceof List<?>);
		assertEquals(2, ((List<?>) mappedValue).size());

		Object firstObject = ((List<?>) mappedValue).get(0);
		assertTrue(firstObject instanceof Collector);

		// Collectors aren't formatted by default by the Collector.toString() method (Agents are)
		Collector firstCollector = (Collector) firstObject;
		String firstCollectorName = DataObjFieldFormatMgr.getInstance().format(firstCollector, Collector.class);
		assertEquals(collectorNames[0], firstCollectorName.trim());

		Object secondObject = ((List<?>) mappedValue).get(1);
		assertTrue(secondObject instanceof Collector);

		Collector secondCollector = (Collector) secondObject;
		String secondCollectorName = DataObjFieldFormatMgr.getInstance().format(secondCollector, Collector.class);
		assertEquals(collectorNames[1], secondCollectorName.trim());
	}
	
	public void testGetPathWithTreeLevel() {
		// set up the CollectionObject object graph with the target value
		String asteraceaeName = "Asteraceae";
		String asterName = "Aster";
		String asterAlbaName = "Aster alba";

		CollectionObject collObj = new CollectionObject();

		Taxon asterAlba = new Taxon();
		asterAlba.setFullName(asterAlbaName);
		asterAlba.setIsAccepted(true);
		asterAlba.setRankId(TaxonTreeDef.SPECIES);
		
		Taxon aster = new Taxon();
		aster.setFullName(asterName);
		aster.setIsAccepted(true);
		aster.setRankId(TaxonTreeDef.GENUS);
		
		Taxon asteraceae = new Taxon();
		asteraceae.setFullName(asteraceaeName);
		asteraceae.setIsAccepted(true);
		asteraceae.setRankId(TaxonTreeDef.FAMILY);
		
		Determination det = new Determination();
		det.setCollectionObject(collObj);
		det.setTaxon(asterAlba);

		asterAlba.setParent(aster);
		aster.setParent(asteraceae);
		
		collObj.setDeterminations(new HashSet<Determination>());
		collObj.getDeterminations().add(det);

		// set up the info we will have for finding that value
		String fieldName = "Family";
		String tablePath = "1,9-determinations,4-preferredTaxon";
		boolean isRelationship = false;

		Object mappedValue = joinWalker.getPathValue(collObj, tablePath, fieldName, isRelationship);
		assertEquals(asteraceaeName, mappedValue.toString().trim());
	}
}