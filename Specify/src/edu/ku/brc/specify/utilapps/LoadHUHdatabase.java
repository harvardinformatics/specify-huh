/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ku.brc.specify.utilapps;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import edu.harvard.huh.asa2specify.loader.AffiliateLoader;
import edu.harvard.huh.asa2specify.loader.AgentLoader;
import edu.harvard.huh.asa2specify.loader.BotanistCountryLoader;
import edu.harvard.huh.asa2specify.loader.BotanistLoader;
import edu.harvard.huh.asa2specify.loader.BotanistNameLoader;
import edu.harvard.huh.asa2specify.loader.BotanistSpecialtyLoader;
import edu.harvard.huh.asa2specify.loader.BotanistTeamLoader;
import edu.harvard.huh.asa2specify.loader.DeterminationLoader;
import edu.harvard.huh.asa2specify.loader.GeoUnitLoader;
import edu.harvard.huh.asa2specify.loader.LocalException;
import edu.harvard.huh.asa2specify.loader.LocalityLoader;
import edu.harvard.huh.asa2specify.loader.OptrLoader;
import edu.harvard.huh.asa2specify.loader.OrganizationLoader;
import edu.harvard.huh.asa2specify.loader.PublAuthorLoader;
import edu.harvard.huh.asa2specify.loader.PublicationLoader;
import edu.harvard.huh.asa2specify.loader.SeriesLoader;
import edu.harvard.huh.asa2specify.loader.SpecimenItemLoader;
import edu.harvard.huh.asa2specify.loader.TaxonLoader;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.SpLocaleContainer;
import edu.ku.brc.specify.datamodel.SpLocaleContainerItem;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.dbsupport.SpecifyDeleteHelper;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.ProgressFrame;
import edu.ku.brc.ui.UIHelper;

public class LoadHUHdatabase
{
    private static final Logger  log      = Logger.getLogger(LoadHUHdatabase.class);

    protected Session            session;
    protected int                steps = 0; 
    protected ProgressFrame      frame;
    protected boolean            hideFrame = false;
    
    /**
     * 
     */
    public LoadHUHdatabase()
    {
        super();
    }
    
    
    // From BuildSampleDatabase.
    public ProgressFrame createProgressFrame(final String title)
    {
        if (frame == null)
        {
            frame = new ProgressFrame(title, "SpecifyLargeIcon");
            frame.pack();
        } 
        return frame;
    }
    
    /**
     * Pack and then sets the width to 500px.
     */
    public void adjustProgressFrame()
    {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.pack();
        Dimension size = frame.getSize();
        size.width = Math.max(size.width, 500);
        frame.setSize(size);
    }
    
    public void setSession(Session s)
    {
        session = s;
    }
    
    /** 
     * Drops, Creates and Builds the Database.
     * 
     * @throws SQLException
     * @throws IOException
     */
    public boolean loadHUHdatabase(final Properties props)
    {
        createProgressFrame("Loading HUH Specify Database");

        final String dbName = props.getProperty("dbName");
        
        adjustProgressFrame();
        
        frame.setTitle("Loading HUH Specify Database");
        if (!hideFrame)
        {
            UIHelper.centerWindow(frame);
            frame.setVisible(true);
            ImageIcon imgIcon = IconManager.getIcon("AppIcon", IconManager.IconSize.Std16);
            if (imgIcon != null)
            {
                frame.setIconImage(imgIcon.getImage());
            }
            
        } else
        {
            System.out.println("Loading Specify Database Username["+props.getProperty("dbUserName")+"]");
        }
        
        frame.setProcessPercent(true);
        frame.setOverall(0, 4);
        frame.getCloseBtn().setVisible(false);

        steps = 0;
                
        DatabaseDriverInfo driverInfo = (DatabaseDriverInfo)props.get("driver");
        
        try
        {            
            String saUserName = props.getProperty("dbUserName"); // Master Username
            String saPassword = props.getProperty("dbPassword"); // Master Password
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.getProcessProgress().setIndeterminate(true);
                    frame.getProcessProgress().setString("");
                    frame.setDesc("Logging into "+dbName+"....");
                    frame.setOverall(steps++);
                }
            });
            
            String connStr = driverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Create, 
                                                         props.getProperty("hostName"), 
                                                         dbName);
            if (connStr == null)
            {
                connStr = driverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Open, props.getProperty("hostName"),  dbName);
            }
            
            if (!UIHelper.tryLogin(driverInfo.getDriverClassName(), 
                                    driverInfo.getDialectClassName(), 
                                    dbName, 
                                    connStr, 
                                    saUserName, 
                                    saPassword))
            {
                if (hideFrame) System.out.println("Login Failed!");
                return false;
            }
                        
            setSession(HibernateUtil.getCurrentSession());
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.getProcessProgress().setIndeterminate(true);
                    frame.getProcessProgress().setString("");
                    frame.setDesc("Loading data....");
                    frame.setOverall(steps++);
                }
            });
            
            Connection connection = null;
            Statement statement = null;
            
            // set up a database connection for direct sql TODO: manage statement, connection?
            try {
                connection = DBConnection.getInstance().createConnection();
                statement  = connection.createStatement();
            }
            catch (SQLException e)
            {
                throw new LocalException("Couldn't create Statement", e);
            }
            
            File dir = new File("/home/maureen/load");
            
            // This will load agent records with GUID="{optr.id} optr"
            frame.setDesc("Loading optr...");
            OptrLoader optrLoader = new OptrLoader(new File(dir, "optr.csv"), statement);
            optrLoader.setFrame(frame);
            int optrRecords = optrLoader.loadRecords();
            log.info("Loaded " + optrRecords + " optr records");
            
            // This will load agent records with GUID="{botanist.id} botanist".
            // Pre-existing agent records for people who have both optr and botanist entries will be updated.
            frame.setDesc("Loading botanist...");
            BotanistLoader botanistLoader = new BotanistLoader(new File(dir, "botanist.csv"), statement);
            botanistLoader.setFrame(frame);
            int botanistRecords = botanistLoader.loadRecords();
            log.info("Loaded " + botanistRecords + " botanist records");
            
            frame.setDesc("Loading taxon...");
            TaxonLoader taxonLoader = new TaxonLoader(new File(dir, "taxon.csv"), statement);
            taxonLoader.setFrame(frame);
            int taxonRecords = taxonLoader.loadRecords(); 
            log.info("Loaded " + taxonRecords + " taxonRecords");

            frame.setDesc("Numbering Taxonomy Tree...");
            taxonLoader.numberNodes();
            log.info("Numbered taxon tree");
            
            frame.setDesc("Loading geo_unit...");
            GeoUnitLoader geoUnitLoader = new GeoUnitLoader(new File(dir, "geo_unit.csv"), statement);
            geoUnitLoader.setFrame(frame);
            int geoUnitRecords = geoUnitLoader.loadRecords();
            log.info("Loaded " + geoUnitRecords + " geo_unit records");
            
            frame.setDesc("Numbering Geography Tree...");
            geoUnitLoader.numberNodes();
            log.info("Numbered geography tree");
            
            frame.setDesc("Loading site...");
            LocalityLoader localityLoader = new LocalityLoader(new File(dir, "site.csv"), statement);
            localityLoader.setFrame(frame);
            int localityRecords = localityLoader.loadRecords();
            log.info("Loaded " + localityRecords + " site records");
            
            frame.setDesc("Loading organization...");
            OrganizationLoader organizationLoader = new OrganizationLoader(new File(dir, "organizations.csv"), statement, new File(dir, "organization_botanist.csv"));
            organizationLoader.setFrame(frame);
            int organizationRecords = organizationLoader.loadRecords();
            log.info("Loaded " + organizationRecords + " organization records");

            frame.setDesc("Loading affiliate...");
            AffiliateLoader affiliateLoader = new AffiliateLoader(new File(dir, "affiliate.csv"), statement, new File(dir, "affiliate_botanist.csv"));
            affiliateLoader.setFrame(frame);
            int affiliateRecords = affiliateLoader.loadRecords();
            log.info("Loaded " + affiliateRecords + " affiliate records");
            
            frame.setDesc("Loading botanist_name...");
            BotanistNameLoader botanistNameLoader = new BotanistNameLoader(new File(dir, "botanist_name.csv"), statement);
            botanistNameLoader.setFrame(frame);
            int botanistNameRecords = botanistNameLoader.loadRecords();
            log.info("Loaded " + botanistNameRecords + " botanist_name records");
            
            frame.setDesc("Loading botanist_team...");
            BotanistTeamLoader botanistTeamLoader = new BotanistTeamLoader(new File(dir, "botanist_team.csv"), statement);
            botanistTeamLoader.setFrame(frame);
            int botanistTeamRecords = botanistTeamLoader.loadRecords();
            log.info("Loaded " + botanistTeamRecords + " botanist_team records");
            
            frame.setDesc("Loading botanist_role_country...");
            BotanistCountryLoader botanistCountryLoader = new BotanistCountryLoader(new File(dir, "botanist_country.csv"), statement);
            botanistCountryLoader.setFrame(frame);
            int botanistCountryRecords = botanistCountryLoader.loadRecords();
            log.info("Loaded " + botanistCountryRecords + " botanist_country records");
            
            frame.setDesc("Loading botanist_role_specialty...");
            BotanistSpecialtyLoader botanistSpecialtyLoader = new BotanistSpecialtyLoader(new File(dir, "botanist_specialty.csv"), statement);
            botanistSpecialtyLoader.setFrame(frame);
            int botanistSpecialtyRecords = botanistSpecialtyLoader.loadRecords();
            log.info("Loaded " + botanistSpecialtyRecords + " botanist_specialty records");
            
            frame.setDesc("Loading publication...");
            PublicationLoader publicationLoader = new PublicationLoader(new File(dir, "publication.csv"), statement);
            publicationLoader.setFrame(frame);
            int publicationRecords = publicationLoader.loadRecords();
            log.info("Loaded " + publicationRecords + " publication records");
            
            frame.setDesc("Loading publ_author...");
            PublAuthorLoader publAuthorLoader = new PublAuthorLoader(new File(dir, "publ_author.csv"), statement);
            publAuthorLoader.setFrame(frame);
            int publAuthorRecords = publAuthorLoader.loadRecords();
            log.info("Loaded " + publAuthorRecords + " publ_author records");
            
/*            frame.setDesc("Loading series...");
            SeriesLoader seriesLoader = new SeriesLoader(new File(dir, "series.csv"), statement);
            seriesLoader.setFrame(frame);
            int seriesRecords = seriesLoader.loadRecords();
            log.info("Loaded " + seriesRecords + " series records");*/
            
            frame.setDesc("Loading specimen_item and specimen...");
            SpecimenItemLoader specimenItemLoader = new SpecimenItemLoader(new File(dir, "specimen_item.csv"), statement);
            specimenItemLoader.setFrame(frame);
            int specimenItemRecords = specimenItemLoader.loadRecords();
            log.info("Loaded " + specimenItemRecords + " specimen_item records");
            
/*            frame.setDesc("Loading determination and type_specimen...");
            DeterminationLoader determinationLoader = new DeterminationLoader(new File(dir, "determination.csv"), statement);
            determinationLoader.setFrame(frame);
            int determinationRecords = determinationLoader.loadRecords();
            log.info("Loaded " + determinationRecords + " determination records");
            
            frame.setDesc("Loading agent...");
            AgentLoader agentLoader = new AgentLoader(new File(dir, "agent.csv"), statement);
            agentLoader.setFrame(frame);
            int agentRecords = agentLoader.loadRecords();
            log.info("Loaded " + agentRecords + " agent records");*/
            
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.getProcessProgress().setIndeterminate(true);
                    frame.getProcessProgress().setString("");
                    frame.setDesc("Saving data into "+dbName+"....");
                    frame.setOverall(steps++);
                }
            });
            
            if (hideFrame) System.out.println("Persisting Data...");
            
            HibernateUtil.getCurrentSession().close();
            
            if (hideFrame) System.out.println("Done.");
            
            frame.setVisible(false);
            frame.dispose();
            
            SpecifyDeleteHelper.showTableCounts("EmptyDB.txt", true);
            
            return true;
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
