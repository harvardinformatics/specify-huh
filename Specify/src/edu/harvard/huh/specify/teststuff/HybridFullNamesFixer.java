package edu.harvard.huh.specify.teststuff;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import edu.harvard.huh.specify.datamodel.busrules.HUHTaxonBusRules;
import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.Specify;
import edu.ku.brc.specify.datamodel.Taxon;

/**
 * Fixes the full names of all hybrid taxa.
 * 
 * To run, change the connection settings in the main method and then run the
 * class.
 * 
 * @author lchan
 * 
 */
public class HybridFullNamesFixer {

    public static void main(String[] args) {
        DBConnection dbc = DBConnection.getInstance();
        dbc.setConnectionStr("jdbc:mysql://127.0.0.1:3307/specify?characterEncoding=UTF-8&autoReconnect=true");
        dbc.setDatabaseName("specify");
        dbc.setDialect("org.hibernate.dialect.MySQLDialect");
        dbc.setDriver("com.mysql.jdbc.Driver");
        dbc.setServerName("127.0.0.1");
        dbc.setUsernamePassword("lchan", "1579ASDF");

        Specify.setUpSystemProperties();

        Session session = HibernateUtil.getNewSession();
        session.getTransaction().begin();

        Query query = session
                .createQuery("select t from Taxon t where t.isHybrid = true");
        List<Taxon> taxons = query.list();

        for (Taxon t : taxons) {
            AppContextMgr.getInstance().setHasContext(true);

            HUHTaxonBusRules taxonBr = new HUHTaxonBusRules();
            taxonBr.beforeSave(t, null);

            session.update(t);
        }

        session.getTransaction().commit();
        session.close();
    }
}
