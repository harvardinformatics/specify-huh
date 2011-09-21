/**
 * This class modeled after the class of a similar name in the
 * au.org.tern.ecoinformatics.oai.provider.sample.dao.impl package by
 * Terrestrial Ecosystem Research Network.  Their copyright statement is included
 * below.  --mmk 2011-09-20
 * 
 * Copyright 2010 Terrestrial Ecosystem Research Network, licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.harvard.huh.oai.specify;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import edu.ku.brc.af.auth.UserAndMasterPasswordMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.exceptions.ConfigurationException;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.util.Pair;

/**
 * This example simply serves up newly created objects. Obviously a real implementation will 
 * perform a lookup of persistent data, either from file system or database.
 * 
 * @author vhobbs
 *
 */
public class SpecifyOccurrenceDaoImpl implements SpecifyOccurrenceDao {

	private HashMap<String, SpecifyFieldMappingDesc> dwcMappings = null;
	private List<String> dwcConcepts = null;
	private Session session = null;
	private List<SpecifyOccurrence> occurrences = null;
	private DBTableIdMgr tableMgr = null;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public SpecifyOccurrence get(Long id) {

		return getOccurrences(null, null).get(id.intValue() - 1);
	}

	
	@Override
	public List<SpecifyOccurrence> getOccurrences(Date from, Date until) {
		if (occurrences == null) {

            // given the mapping, we need to know what the root table is.  no way to do this in Specify right now
			Class<CollectionObject> rootClass = CollectionObject.class;
			int rootTableId = getTableMgr().getIdByClassName(rootClass.getName());

			// given the root table, we need to get all root objects modified between "from" and "until"
			List<CollectionObject> collObjs = getSession().createCriteria(rootClass).add( Restrictions.between("timestampModified", from, until)).list();
			
			// for each object, we need to populate the dwc occurrence object according to the mapping
			for (CollectionObject collObj : collObjs) {
				SpecifyOccurrence occurrence = new SpecifyOccurrence();
				occurrence.setId((long) collObj.getId());

				// Just for now
				MapperTester mt = new MapperTester();
				
				for (String dwcConcept : getDwcConcepts()) {
					SpecifyFieldMappingDesc mapItem = getDwcMappings().get(dwcConcept);
					if (!mapItem.getIsActive()) continue;

					Object value = mt.getMappedValue(collObj, mapItem);
					if (mapItem.getName().equals(dwcConcept)) {
						// use reflection to set the value on the SpecifyOccurrence?
						
					}
				}
				occurrences.add(occurrence);
			}
		}
		
		return occurrences;
	}
	
	// we need a particular schema mapping.  which one? read this in from a bean?
	private HashMap<String, SpecifyFieldMappingDesc> getDwcMappings() {
		if (dwcMappings == null) dwcMappings = SpecifyFieldMappingDesc.getDefaultMappings();
		return dwcMappings;
	}

	private List<String> getDwcConcepts() {
		if (dwcConcepts == null) {
			dwcConcepts = new ArrayList<String>();
			dwcConcepts.addAll(getDwcMappings().keySet());
			Collections.sort(dwcConcepts);
		}
		return dwcConcepts;
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
	
	private DBTableIdMgr getTableMgr() {
		if (tableMgr == null) {
			System.setProperty(DBTableIdMgr.factoryName, "edu.ku.brc.specify.config.SpecifyDBTableIdMgr"); 
			tableMgr = DBTableIdMgr.getInstance();
		}
		return tableMgr;
	}
}
