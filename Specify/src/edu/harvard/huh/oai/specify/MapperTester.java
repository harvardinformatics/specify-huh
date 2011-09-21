package edu.harvard.huh.oai.specify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;

import edu.harvard.huh.oai.specify.SpecifyFieldMappingDesc.PathSegment;
import edu.ku.brc.af.auth.JaasContext;
import edu.ku.brc.af.auth.SecurityMgr;
import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBRelationshipInfo;
import edu.ku.brc.af.core.db.DBRelationshipInfo.RelationshipType;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.DataObjectGettable;
import edu.ku.brc.af.ui.forms.DataObjectGettableFactory;
import edu.ku.brc.af.ui.forms.formatters.DataObjFieldFormatMgr;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.exceptions.ConfigurationException;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.GeoCoordDetail;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.ui.UIRegistry;
import junit.framework.TestCase;

public class MapperTester extends TestCase {

	String workingPath = "/home/mkelly/workspace-fp/specify-fp/Specify";

	String driver = "com.mysql.jdbc.Driver";
	String dialect = "org.hibernate.dialect.MySQLDialect";
	String databaseName = "specify";
	String connectionUrl = "jdbc:mysql://localhost/specify?characterEncoding=UTF-8";
	String dbUserName = "specify";
	String dbPassword = "specify";
	String specifyUserName = "mschmull"; //
	String specifyPassword = "mschmull"; //

	Session session = null;

	DBTableIdMgr tableMgr = null;
	String gettableClassName = "edu.ku.brc.af.ui.forms.DataGetterForObj";

	@Override
	public void setUp() {

		System.setProperty(DBTableIdMgr.factoryName, "edu.ku.brc.specify.config.SpecifyDBTableIdMgr");
		System.setProperty(AppContextMgr.factoryName, "edu.ku.brc.specify.config.SpecifyAppContextMgr");
		System.setProperty(SecurityMgr.factoryName, "edu.ku.brc.af.auth.specify.SpecifySecurityMgr"); // AppContextMgr needs this one
		
		UIRegistry.setDefaultWorkingPath(workingPath); // we need to be able to find specify_tableid_listing.xml from here for DBTableIdMgr

		AppPreferences localPrefs = AppPreferences.getLocalPrefs();
		localPrefs.setDirPath(UIRegistry.getAppDataDir()); // AppPreferences.dirPath needs to be set in order to find formatters

		DBConnection.getInstance().setConnectionStr(connectionUrl); // SecurityMgr needs the connection; this is how you need to initialize it
		DBConnection.getInstance().setDatabaseName(databaseName);
		DBConnection.getInstance().setDialect(dialect);
		DBConnection.getInstance().setDriver(driver);  // not "setDriverName!"  ha!
		DBConnection.getInstance().setUsernamePassword(dbUserName, dbPassword);
		DBConnection.getInstance().getConnection();

		UserAndMasterPasswordMgr.getInstance().setUsersUserName(specifyUserName); //
        UserAndMasterPasswordMgr.getInstance().setUsersPassword(specifyPassword); //
		(new JaasContext()).jaasLogin(specifyUserName, specifyPassword, connectionUrl, driver, dbUserName, dbPassword); //

        //Configuration config = new AnnotationConfiguration().configure();

		//config.setProperty("hibernate.connection.username", userName);
		//config.setProperty("hibernate.connection.password", password);
		//config.setProperty("hibernate.connection.url", connectionUrl);
		//config.setProperty("hibernate.dialect", dialect);
		//config.setProperty("hibernate.connection.driver_class", driver);

		//session = config.buildSessionFactory().openSession();

		tableMgr = DBTableIdMgr.getInstance();
	}

	@Override
	public void tearDown() {
		DBConnection.shutdown();
		if (session != null && session.isOpen()) session.close();
	}

	// test walking a tableid join tree
	public void testJoinWalk1() {

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
		SpecifyFieldMappingDesc mapItem =
				new SpecifyFieldMappingDesc("geoRefDetBy", "1,10,2,123-geoCoordDetails,5-geoRefDetBy", false);

		// now go find it
		Object mappedValue = getMappedValue(collObj, mapItem);
		assertEquals(lastName, mappedValue.toString().trim());
	}

	public void testJoinWalk2() {

		// set up the CollectionObject object graph with the target value
		Double longitude = 34.802;

		CollectionObject collObj = new CollectionObject();
		CollectingEvent collEvent = new CollectingEvent();
		Locality locality = new Locality();

		collObj.setCollectingEvent(collEvent);
		collEvent.setLocality(locality);
		locality.setLongitude1(BigDecimal.valueOf(longitude));

		// set up the info we will have for finding that value
		SpecifyFieldMappingDesc mapItem =
				new SpecifyFieldMappingDesc("longitude1", "1,10,2", false);

		// now go find it
		Object mappedValue = getMappedValue(collObj, mapItem);
		assertEquals(longitude, ((BigDecimal) mappedValue).doubleValue(), 0.00001);
	}

	public void testJoinWalk3() {
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
		SpecifyFieldMappingDesc mapItem =
				new SpecifyFieldMappingDesc("fullName", "1,9-determinations,4-preferredTaxon", false);

		// don't find it when it's not there
		Object noMappedValue = getMappedValue(collObj, mapItem);
		assertNull(noMappedValue);

		// do find it when it is.
		collObj.getDeterminations().add(oldDet);
		collObj.getDeterminations().add(newDet);
		Object mappedValue = getMappedValue(collObj, mapItem);
		assertEquals(asterSerrataName, mappedValue.toString().trim());
	}

	public void testJoinWalk4() {
		
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
		SpecifyFieldMappingDesc mapItem =
				new SpecifyFieldMappingDesc("collectors", "1,10,30-collectors", true);

		Object mappedValue = getMappedValue(collObj, mapItem);

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

	public Object getMappedValue(Object object, SpecifyFieldMappingDesc mapItem) throws ConfigurationException {
		List<PathSegment> joins = mapItem.getPathSegments();
		PathSegment lastJoin = joins.get(joins.size() - 1);
		String relationshipName = null;

		DBTableInfo fromTableInfo = null;
		for (PathSegment join : joins) {

			Integer tableId = join.getTableId();
			DBTableInfo toTableInfo = tableMgr.getInfoById(tableId);
			if (toTableInfo == null) throw new ConfigurationException("Couldn't find table for id: " + tableId);

			// if this is the first in the series, just get the next one
			if (fromTableInfo == null) {

				fromTableInfo = toTableInfo;
				continue;
			}

			DBRelationshipInfo relationshipInfo = null;
			relationshipName = join.getRelationshipName();
			if (relationshipName != null) {
				// get the relationship by relationship name
				relationshipInfo = fromTableInfo.getRelationshipByName(relationshipName);
			}
			else {
				// get the relationship by table id
				for (DBRelationshipInfo candidateRelInfo : fromTableInfo.getRelationships()) {

					if (candidateRelInfo.getClassName().equals(toTableInfo.getClassName())) {
						relationshipInfo = candidateRelInfo;
						break;
					}
				}
			}
			if (relationshipInfo == null) throw new ConfigurationException("Couldn't find relationship from table with id " +
					fromTableInfo.getTableId() + " to table with id " + tableId + " for mapping with name '" + mapItem.getName() + '"');

			// get the joined object by relationship
			DataObjectGettable getter = DataObjectGettableFactory.get(object.getClass().getName(), gettableClassName);
			object = getter.getFieldValue(object, relationshipInfo.getName());

			// this object graph didn't contain the field we were looking for.
			if (object == null) return null;

			RelationshipType relationshipType = relationshipInfo.getType();

			// Is our object really a collection?  We may have to take only the first member.
			if (relationshipType.equals(RelationshipType.ZeroOrOne) ||
					relationshipType.equals(RelationshipType.OneToMany) ||
						relationshipType.equals(RelationshipType.ManyToMany)) {

				if (!join.equals(lastJoin)) {
					
					if (! (object instanceof Collection<?>)) {
						
						// The data model configuration did not match the object graph
						throw new ConfigurationException("Found single object when collection expected for " +
								"table with id " + fromTableInfo.getTableId() + ", relationship '" + relationshipInfo.getName() + "': " + object.getClass().getName() + "");
					}
					
					Collection<?> collection = (Collection<?>) object;

					if (collection.size() < 1) {
						return null;
					}
					else {
						Object firstObject = collection.iterator().next();

						if (collection.size() == 1) {
							object = firstObject;
						}
						else {
							// Silently pruning the tree of objects, taking only the first in this collection with us
							// as we walk the rest of the table path
							
							if (firstObject instanceof Comparable) { // really any DataModelObjBase object could be, since they have timestamps, but that is not yet implemented
								List<Comparable<?>> list = new ArrayList<Comparable<?>>();
								for (Object comparableObj : collection) {
									list.add((Comparable<?>) comparableObj);
								}
								Collections.sort((List<? extends Comparable<? super Comparable<?>>>) list);
								object = list.get(0);
							}
							else {
								// punt.
								object = collection.iterator().next();
							}
						}
					}
				}
			}
			else if (object instanceof Collection<?>) {
				// The data model configuration did not match the object graph
				throw new ConfigurationException("Found collection when single object expected for " +
						"table with id " + fromTableInfo.getTableId() + ", relationship '" + relationshipInfo.getName() + "': " + object.getClass().getName() + "");
			}
			fromTableInfo = toTableInfo;
		}

		// we might still have to get the last field.
		String fieldName = mapItem.getFieldName();
		if (!fieldName.equals(relationshipName)) {
			DataObjectGettable getter = DataObjectGettableFactory.get(object.getClass().getName(), gettableClassName);
			object = getter.getFieldValue(object, fieldName);
		}

		if (object instanceof Collection<?>) {

			Collection<?> collection = (Collection<?>) object;

			if (collection.size() >= 1) {
				Object firstObject = collection.iterator().next();

				if (firstObject instanceof Comparable) {
					List<Comparable<?>> orderedList = new ArrayList<Comparable<?>>();
					for (Object comparableObj : collection) {
						orderedList.add((Comparable<?>) comparableObj);
					}
					Collections.sort((List<? extends Comparable<? super Comparable<?>>>) orderedList);
					return mapItem.getIsRelationship() ? orderedList : orderedList.get(0);  // silently pruning the list if a collection was not expected
				}
			}
			return null; // empty collection
		}
		else {
			if (mapItem.getIsRelationship()) {
				List<Object> list = new ArrayList<Object>();
				list.add(object);
				return list;
			}
			else {
				return object;
			}
		}
	}
}