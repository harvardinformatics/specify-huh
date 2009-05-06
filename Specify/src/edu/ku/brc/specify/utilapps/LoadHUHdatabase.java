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
import edu.harvard.huh.asa2specify.loader.BotanistCountryLoader;
import edu.harvard.huh.asa2specify.loader.BotanistLoader;
import edu.harvard.huh.asa2specify.loader.BotanistNameLoader;
import edu.harvard.huh.asa2specify.loader.BotanistSpecialtyLoader;
import edu.harvard.huh.asa2specify.loader.BotanistTeamLoader;
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
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
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
                    frame.setDesc("Loading localities and botanists....");
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
            Discipline discipline = null; // TODO: implement
            Division division = null;     // TODO: implement
            TaxonTreeDef taxonTreeDef = null; // TODO: implement
            GeographyTreeDef geoTreeDef = null; // TODO: implement
            
            // This will load agent records with GUID="{optr.id} optr"
            OptrLoader optrLoader = new OptrLoader(new File("demo_files/optr.csv"), statement);
            optrLoader.setFrame(frame);
            int optrRecords = optrLoader.loadRecords();
            
            // This will load agent records with GUID="{botanist.id} botanist".
            // Pre-existing agent records for people who have both optr and botanist entries will be updated.
            BotanistLoader botanistLoader = new BotanistLoader(new File("demo_files/botanist.csv"), statement);
            botanistLoader.setFrame(frame);
            int botanistRecords = botanistLoader.loadRecords();
            
            TaxonLoader taxonLoader = new TaxonLoader(new File("demo_files/taxon.csv"), statement, taxonTreeDef);
            taxonLoader.setFrame(frame);
            int taxonRecords = taxonLoader.loadRecords(); 

            frame.setDesc("Numbering Taxonomy Tree...");
            taxonLoader.numberNodes();
            
            GeoUnitLoader geoUnitLoader = new GeoUnitLoader(new File("demo_files/geo_unit.csv"), statement, geoTreeDef);
            geoUnitLoader.setFrame(frame);
            int geoUnitRecords = geoUnitLoader.loadRecords();
            
            frame.setDesc("Numbering Geography Tree...");
            geoUnitLoader.numberNodes();
            
            LocalityLoader localityLoader = new LocalityLoader(new File("demo_files/site.csv"), statement, discipline);
            localityLoader.setFrame(frame);
            int localityRecords = localityLoader.loadRecords();
            
            OrganizationLoader organizationLoader = new OrganizationLoader(new File("demo_files/organizations.csv"), statement);
            organizationLoader.setFrame(frame);
            int organizationRecords = organizationLoader.loadRecords();
            
            AffiliateLoader affiliateLoader = new AffiliateLoader(new File("demo_files/affiliate.csv"), statement, division);
            affiliateLoader.setFrame(frame);
            int affiliateRecords = affiliateLoader.loadRecords();
            
            BotanistNameLoader botanistNameLoader = new BotanistNameLoader(new File("demo_files/botanist_name.csv"), statement);
            botanistNameLoader.setFrame(frame);
            int botanistNameRecords = botanistNameLoader.loadRecords();
            
            BotanistTeamLoader botanistTeamLoader = new BotanistTeamLoader(new File("demo_files/botanist_team.csv"), statement);
            botanistTeamLoader.setFrame(frame);
            int botanistTeamRecords = botanistTeamLoader.loadRecords();
            
            BotanistCountryLoader botanistCountryLoader = new BotanistCountryLoader(new File("demo_files/botanist_country.csv"), statement);
            botanistCountryLoader.setFrame(frame);
            int botanistCountryRecords = botanistCountryLoader.loadRecords();
            
            BotanistSpecialtyLoader botanistSpecialtyLoader = new BotanistSpecialtyLoader(new File("demo_files/botanist_specialty.csv"), statement);
            botanistSpecialtyLoader.setFrame(frame);
            int botanistSpecialtyRecords = botanistSpecialtyLoader.loadRecords();
            
            PublicationLoader publicationLoader = new PublicationLoader(new File("demo_files/publication.csv"), statement);
            publicationLoader.setFrame(frame);
            int publicationRecords = publicationLoader.loadRecords();
            
            PublAuthorLoader publAuthorLoader = new PublAuthorLoader(new File("demo_files/publ_author.csv"), statement);
            publAuthorLoader.setFrame(frame);
            int publAuthorRecords = publAuthorLoader.loadRecords();
            
            SeriesLoader seriesLoader = new SeriesLoader(new File("demo_files/series.csv"), statement);
            seriesLoader.setFrame(frame);
            int seriesRecords = seriesLoader.loadRecords();
                        
            SpecimenItemLoader specimenItemLoader = new SpecimenItemLoader(new File("demo_files/specimen_item.csv"), statement, discipline, division);
            specimenItemLoader.setFrame(frame);
            int specimenItemRecords = specimenItemLoader.loadRecords();
            
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
