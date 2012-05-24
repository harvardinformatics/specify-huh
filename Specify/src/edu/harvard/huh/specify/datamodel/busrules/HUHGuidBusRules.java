/* Copyright (C) 2012, President and Fellows of Harvard College
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.harvard.huh.specify.datamodel.busrules;

import org.apache.log4j.Logger;
import org.flexdock.util.UUID;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.ku.brc.af.core.GenericLSIDGeneratorFactory;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.config.SpecifyLSIDGeneratorFactory;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Taxon;

/**
 * Business rule class providing support for HUH's implementation of GUIDs for
 * data objects.
 * @see edu.harvard.huh.specify.datamodel.Guids
 * 
 * @code_status Alpha
 * 
 * @author mole
 *
 * 2012 May 23
 */
public class HUHGuidBusRules {

    private static final Logger  log   = Logger.getLogger(HUHGuidBusRules.class);
	
	/**
	 * Given a data object, check to see if a UUID has been assigned to it in
	 * the guids table, if not, and it is a table for which guids are being 
	 * assigned, generate a UUID and store it with the table name and primary
	 * key of the data object in the guids table.
	 * 
	 * @param data the data object to check for guid assignment
	 * 
	 * @see edu.harvard.huh.specify.datamodel.Guids
	 */
    public static void setRecordGuid(final FormDataObjIFace data)
    {

        log.debug(data.getTableId());
        log.debug(data.getId());  // surrogate numeric primary key value
        
        String tablename = null;
        if (data.getTableId()==Agent.getClassTableId()) {
        	tablename = "agent";
        }
        if (data.getTableId()==Fragment.getClassTableId()) { 
        	tablename = "fragment";
        }
        if (data.getTableId()==Taxon.getClassTableId()) { 
        	tablename = "taxon";
        }
        if (tablename!=null) {
        	// check if guid for this record exists, if not, create one
        	String sql = "select count(*) from Guids where primarykey = " + Integer.toString(data.getId()) + " and tablename = '" + tablename + "' ";
        	System.out.println(sql);
        	// fire query 
        	Session session = HibernateUtil.getCurrentSession();
        	Transaction t = session.beginTransaction();
        	try { 
        		Integer result = ((Integer) session.createQuery(sql).iterate().next()).intValue();
        		System.out.println("Found: " + result.toString());
        		if (result.intValue()==0) { 
        			// no guid found for this row in this table, add one.
        			UUID guid = UUID.randomUUID();
        			String sqlinsert = "insert into guids (tablename,primarykey,uuid) values ('"+ tablename + "',"+ Integer.toString(data.getId()) +",'" + guid +"')  ";
        	        System.out.println(sqlinsert);
        			SQLQuery updateQuery = session.createSQLQuery(sqlinsert);
        			updateQuery.executeUpdate();
        		} 
        		t.commit();
        	    System.out.println("Committed");
        	} catch (Exception e) { 
        	    System.out.println(e.getMessage());
        		t.rollback();
        	}
        } 
        
    }	
	
}
