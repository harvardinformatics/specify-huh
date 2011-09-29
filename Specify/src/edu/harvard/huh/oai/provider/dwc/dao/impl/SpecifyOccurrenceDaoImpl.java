package edu.harvard.huh.oai.provider.dwc.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.CollectionObject;

import edu.harvard.huh.dwc.model.Occurrence;
import edu.harvard.huh.oai.provider.dwc.dao.OccurrenceDao;
import edu.harvard.huh.specify.mapper.SpecifyMapper;

public class SpecifyOccurrenceDaoImpl implements OccurrenceDao {

	private Session session;
	private SpecifyMapper specifyMapper;
	
	@Override
	public Occurrence get(Long id) {
		
		CollectionObject collObj = (CollectionObject) getSession().get(CollectionObject.class, id);
		Occurrence occurrence = getSpecifyMapper().map(collObj);
		
		collObj = null;
		getSession().close();

		return occurrence;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Occurrence> getOccurrences(Date from, Date until) {

		List<CollectionObject> collectionObjects = (List<CollectionObject>) getSession().createCriteria(CollectionObject.class).add( Restrictions.between("timestampModified", from, until)).list();
		
		List<Occurrence> occurrences = new ArrayList<Occurrence>();
		for (CollectionObject collObj : collectionObjects) {
			occurrences.add(specifyMapper.map(collObj));
			collObj = null;
		}
		getSession().close();
		
		return occurrences;
	}
	
	// from HibernateUtil
	private Session getSession() {
		if (session == null) {
			DBConnection.createInstance("com.mysql.jdbc.Driver", "org.hibernate.dialect.MySQLDialect", "specify", "jdbc:mysql://localhost/specify", "specify", "specify");
			HibernateUtil.setHibernateLogonConfig(new Configuration());
			session = HibernateUtil.getSessionFactory().openSession();
		}
		else if (!session.isOpen()) {
			session = null;
			session = getSession();
		}
		return session;
	}
	
	private SpecifyMapper getSpecifyMapper() {
		return specifyMapper;
	}
}
