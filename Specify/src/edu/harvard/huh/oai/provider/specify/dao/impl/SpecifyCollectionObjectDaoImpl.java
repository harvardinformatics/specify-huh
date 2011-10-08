package edu.harvard.huh.oai.provider.specify.dao.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

	private Session session;
	
	public SpecifyCollectionObjectDaoImpl() {
		initialize();
	}
	
	@Override
	public CollectionObject get(Long id) {
		
		CollectionObject collObj = (CollectionObject) getSession().get(CollectionObject.class, id);		
		getSession().close();

		return collObj;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionObject> getCollectionObjects(Date from, Date until) {

		List<CollectionObject> collectionObjects = (List<CollectionObject>) getSession().createCriteria(CollectionObject.class).add( Restrictions.between("timestampModified", from, until)).list();
		getSession().close();
		
		return collectionObjects;
	}
	
	// from HibernateUtil
	private Session getSession() {
		if (session == null) {
			// TODO: find the appropriate place to store these connection values for non-UI connections, maybe a Spring context
			
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
