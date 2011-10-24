package edu.harvard.huh.specify.mapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Properties;

import junit.framework.TestCase;

import edu.ku.brc.af.auth.JaasContext;
import edu.ku.brc.af.auth.SecurityMgr;
import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
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

public class JoinWalkerTest extends TestCase {

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
        System.setProperty(UIFieldFormatterMgr.factoryName, "edu.ku.brc.specify.ui.SpecifyUIFieldFormatterMgr"); // UIFieldFormatterMgr needs this to look up formatters by table and field
        System.setProperty(DataProviderFactory.factoryName, "edu.ku.brc.specify.dbsupport.HibernateDataProvider"); // UIFieldFormatterMgr needs this too
        
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
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("geoRefDetBy");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,10,2,123-geoCoordDetails,5-geoRefDetBy"));
		mapItem.setIsRelationship(false);

		// now go find it
		String mappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertEquals(lastName, mappedValue.trim());
		
		// try again without permission
		DBTableIdMgr.getInstance().getInfoById(2).getPermissions().setCanView(false);
		Object unviewable = joinWalker.getPathValue(collObj, mapItem);
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
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("longitude1");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,10,2"));
		mapItem.setIsRelationship(false);

		// now go find it
		String mappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertEquals(String.valueOf(longitude), mappedValue);
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
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("fullName");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,9-determinations,4-preferredTaxon"));
		mapItem.setIsRelationship(false);

		// don't find it when it's not there
		String noMappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertNull(noMappedValue);

		// do find it when it is.
		collObj.getDeterminations().add(oldDet);
		collObj.getDeterminations().add(newDet);
		String mappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertEquals(asterSerrataName, mappedValue.trim());
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
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("collectors");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,10,30-collectors"));
		mapItem.setIsRelationship(true);
		
		String mappedValue = joinWalker.getPathValue(collObj, mapItem);

		assertEquals(mappedValue, "Kelly; Clifton");
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
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("Family");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,9-determinations,4-preferredTaxon"));
		mapItem.setIsRelationship(false);

		String mappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertEquals(asteraceaeName, mappedValue.trim());
	}
	
	public void testGetPathWithNullResult() {
		CollectionObject collObj = new CollectionObject();
		
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("Family");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,9-determinations,4-preferredTaxon"));
		mapItem.setIsRelationship(false);

		String mappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertNull(mappedValue);
	}
	
	public void testDayCollectedPath() {
		// set up the CollectionObject object graph with the target value
		Calendar startDate = GregorianCalendar.getInstance();

		CollectionObject collObj = new CollectionObject();
		CollectingEvent collEvent = new CollectingEvent();

		collEvent.setStartDate(startDate);
		collObj.setCollectingEvent(collEvent);

		// set up the info we will have for finding that value
		SpecifyMapItem mapItem = new SpecifyMapItem();
		mapItem.setFieldName("startDate");
		mapItem.setPathSegments(JoinWalker.parseTablePath("1,10-collectingevent"));
		mapItem.setIsRelationship(false);

		joinWalker.setDayFormat(new SimpleDateFormat("yyyy-MM-dd"));
		// now go find it
		String mappedValue = joinWalker.getPathValue(collObj, mapItem);
		assertEquals(joinWalker.getDayFormat().format(startDate.getTime()), mappedValue);
	}
}