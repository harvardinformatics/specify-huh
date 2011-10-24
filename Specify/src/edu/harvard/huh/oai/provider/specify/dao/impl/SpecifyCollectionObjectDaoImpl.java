package edu.harvard.huh.oai.provider.specify.dao.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import edu.ku.brc.af.auth.JaasContext;
import edu.ku.brc.af.auth.SecurityMgr;
import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.ui.UIRegistry;

import edu.harvard.huh.oai.provider.dwc.dao.CollectionObjectDao;

public class SpecifyCollectionObjectDaoImpl implements CollectionObjectDao {

	private static Logger logger = Logger.getLogger(SpecifyCollectionObjectDaoImpl.class);
	
	private Session session; // TODO: create a read-only session, try to figure out how to remove used objects
	
	private int MAX_LIST_SIZE = 100; // TODO: move this to somewhere upstream

	public SpecifyCollectionObjectDaoImpl() {
		initialize();
	}
	
	@Override
	public CollectionObject get(Long id) {
		
		CollectionObject collObj = (CollectionObject) getSession().get(CollectionObject.class, id.intValue());
		
		return collObj;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionObject> getCollectionObjects(Date from, Date until) {

		List<CollectionObject> collectionObjects = null;

		try {
			// on the inclusiveness of OAI-PMH date ranges, see
			// http://www.openarchives.org/OAI/openarchivesprotocol.html#SelectiveHarvestingandDatestamps
			
			if (from == null) {
				if (until == null) {
					collectionObjects =
							(List<CollectionObject>) getSession().createCriteria(CollectionObject.class).list();
				}
				else {
					// include unmodified objects created before "until" and modified objects modified before "until"
					collectionObjects =
							(List<CollectionObject>) getSession().createCriteria(CollectionObject.class)
								.add(Restrictions.or(
										Restrictions.and(Restrictions.isNull("timestampModified"), Restrictions.le("timestampCreated", until)),
										Restrictions.and(Restrictions.isNotNull("timestampModified"), Restrictions.le("timestampModified", until))))
								.setMaxResults(MAX_LIST_SIZE)
								.list();
				}
			}
			else {
				if (until == null) {
					// include unmodified objects created after "from" and modified objects modified after "from"
					collectionObjects =
							(List<CollectionObject>) getSession().createCriteria(CollectionObject.class)
								.add(Restrictions.or(
										Restrictions.and(Restrictions.isNull("timestampModified"), Restrictions.ge("timestampCreated", from)),
										Restrictions.and(Restrictions.isNotNull("timestampModified"), Restrictions.ge("timestampModified", from))))
								.setMaxResults(MAX_LIST_SIZE)
								.list();
				}
				else {
					// include unmodified objects created between "from" and "until"
					// include modified objects modified between "from" and "until"
					collectionObjects =
							(List<CollectionObject>) getSession().createCriteria(CollectionObject.class)
								.add(Restrictions.or(
										Restrictions.and(Restrictions.isNull("timestampModified"), Restrictions.and(Restrictions.ge("timestampCreated", from), Restrictions.le("timestampCreated", until))),
										Restrictions.and(Restrictions.isNotNull("timestampModified"), Restrictions.and(Restrictions.ge("timestampModified", from), Restrictions.le("timestampModified", until)))))
								.setMaxResults(MAX_LIST_SIZE)
								.list();
				}
			}

		// TODO: sort results by date?  would probably require going to hql or sql
		}
		catch (HibernateException e) {
			// TODO: find an appropriate exception to convert to
			logger.debug(e);
		}
		
		return collectionObjects;
	}
	
	// from HibernateUtil
	private Session getSession() {
		if (session == null) {
			// TODO: find the appropriate place to store these connection values for non-UI connections, maybe a Spring context
			// TODO: separate the Hibernate from the Specify so we can deal with their exceptions separately?
			DBConnection.getInstance();
			HibernateUtil.setHibernateLogonConfig(new Configuration());
			session = HibernateUtil.getSessionFactory().openSession();
		}
		else if (!session.isOpen()) {
			session = null;
			session = getSession();
		}
		return session;
	}
	
	private void initialize() {
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("/dao.properties"));
		}
		catch (IOException e) {
			logger.error(e);
			throw new RuntimeException("Couldn't load properties from dao.properties, is it on the classpath?", e); // TODO: error handling
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
	}
}
