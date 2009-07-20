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

import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.loader.*;
import edu.harvard.huh.asa2specify.lookup.*;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.specify.dbsupport.SpecifyDeleteHelper;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.ProgressFrame;
import edu.ku.brc.ui.UIHelper;

public class LoadHUHdatabase
{
    // TODO: insert into agent_discipline(AgentID, DisciplineID) select a.AgentID, 3 from agent a where a.AgentID > 1;
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
        frame.setOverall(0, 34);
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
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.getProcessProgress().setIndeterminate(true);
                    frame.getProcessProgress().setString("");
                    frame.setDesc("Loading data....");
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
            
            frame.setDesc("Loading optrs...");
            OptrLoader optrLoader = new OptrLoader(new File(dir, "optr.csv"), statement);
            optrLoader.setFrame(frame);
            int optrRecords = optrLoader.loadRecords();
            log.info("Loaded " + optrRecords + " optr records");
            frame.setOverall(steps++);
            
            OptrLookup optrLookup = optrLoader.getOptrLookup();

            frame.setDesc("Loading geo units...");
            GeoUnitLoader geoUnitLoader = new GeoUnitLoader(new File(dir, "geo_unit.csv"),
                                                            statement,
                                                            optrLookup);
            geoUnitLoader.setFrame(frame);
            int geoUnitRecords = geoUnitLoader.loadRecords();
            log.info("Loaded " + geoUnitRecords + " geo_unit records");
            frame.setOverall(steps++);
            
            frame.setDesc("Numbering Geography Tree...");
            geoUnitLoader.numberNodes();
            log.info("Numbered geography tree");
            
            frame.setDesc("Loading sites...");
            GeoUnitLookup geoLookup = geoUnitLoader.getGeographyLookup();
            SiteLoader siteLoader = new SiteLoader(new File(dir, "site.csv"), statement, geoLookup);
            siteLoader.setFrame(frame);
            int localityRecords = siteLoader.loadRecords();
            log.info("Loaded " + localityRecords + " site records");
            frame.setOverall(steps++);
            
            SiteLookup siteLookup = siteLoader.getSiteLookup();
            
            frame.setDesc("Loading botanists...");
            BotanistLoader botanistLoader = new BotanistLoader(new File(dir, "botanist.csv"),
                                                               statement,
                                                               new File(dir, "botanist_optr.csv"),
                                                               new File(dir, "optr_botanist.csv"),
                                                               optrLookup);
            botanistLoader.setFrame(frame);
            int botanistRecords = botanistLoader.loadRecords();
            log.info("Loaded " + botanistRecords + " botanist records");
            frame.setOverall(steps++);
            
            BotanistLookup botanistLookup = botanistLoader.getBotanistLookup();
            
            frame.setDesc("Loading botanist names...");
            BotanistNameLoader botanistNameLoader = new BotanistNameLoader(new File(dir, "botanist_name.csv"),
                                                                           statement,
                                                                           botanistLookup);
            botanistNameLoader.setFrame(frame);
            int botanistNameRecords = botanistNameLoader.loadRecords();
            log.info("Loaded " + botanistNameRecords + " botanist_name records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading botanist teams...");
            BotanistTeamLoader botanistTeamLoader = new BotanistTeamLoader(new File(dir, "botanist_team.csv"),
                                                                           statement,
                                                                           botanistLookup);
            botanistTeamLoader.setFrame(frame);
            int botanistTeamRecords = botanistTeamLoader.loadRecords();
            log.info("Loaded " + botanistTeamRecords + " botanist_team records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading botanist role countries...");
            BotanistCountryLoader botanistCountryLoader = new BotanistCountryLoader(new File(dir, "botanist_role_country.csv"),
                                                                                    statement,
                                                                                    geoLookup,
                                                                                    botanistLookup);
            botanistCountryLoader.setFrame(frame);
            int botanistCountryRecords = botanistCountryLoader.loadRecords();
            log.info("Loaded " + botanistCountryRecords + " botanist_country records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading botanist role specialties...");
            BotanistSpecialtyLoader botanistSpecialtyLoader = new BotanistSpecialtyLoader(new File(dir, "botanist_role_specialty.csv"),
                                                                                          statement,
                                                                                          botanistLookup);
            botanistSpecialtyLoader.setFrame(frame);
            int botanistSpecialtyRecords = botanistSpecialtyLoader.loadRecords();
            log.info("Loaded " + botanistSpecialtyRecords + " botanist_specialty records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading organizations...");
            OrganizationLoader organizationLoader = new OrganizationLoader(new File(dir, "organization.csv"),
                                                                           statement,
                                                                           new File(dir, "org_botanist.csv"),
                                                                           botanistLookup);
            organizationLoader.setFrame(frame);
            int organizationRecords = organizationLoader.loadRecords();
            log.info("Loaded " + organizationRecords + " organization records");
            frame.setOverall(steps++);
            
            OrganizationLookup orgLookup = organizationLoader.getOrganizationLookup();
            
            frame.setDesc("Loading series...");
            SeriesLoader seriesLoader = new SeriesLoader(new File(dir, "series.csv"),
                                                         statement,
                                                         new File(dir, "series_botanist.csv"),
                                                         botanistLookup);
            seriesLoader.setFrame(frame);
            int seriesRecords = seriesLoader.loadRecords();
            log.info("Loaded " + seriesRecords + " series records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading affiliates...");
            AffiliateLoader affiliateLoader = new AffiliateLoader(new File(dir, "affiliate.csv"),
                                                                  statement,
                                                                  new File(dir, "affiliate_botanist.csv"),
                                                                  botanistLookup);
            affiliateLoader.setFrame(frame);
            int affiliateRecords = affiliateLoader.loadRecords();
            log.info("Loaded " + affiliateRecords + " affiliate records");
            frame.setOverall(steps++);
            
            AffiliateLookup affiliateLookup = affiliateLoader.getAffiliateLookup();
            
            frame.setDesc("Loading agents...");
            AgentLoader agentLoader = new AgentLoader(new File(dir, "agent.csv"),
                                                      statement,
                                                      new File(dir, "agent_botanist.csv"),
                                                      botanistLookup, 
                                                      orgLookup);
            agentLoader.setFrame(frame);
            int agentRecords = agentLoader.loadRecords();
            log.info("Loaded " + agentRecords + " agent records");
            frame.setOverall(steps++);
            
            AgentLookup agentLookup = agentLoader.getAgentLookup();
            
            frame.setDesc("Loading publications...");
            PublicationLoader publicationLoader = new PublicationLoader(new File(dir, "publication.csv"),
                                                                        statement);
            publicationLoader.setFrame(frame);
            int publicationRecords = publicationLoader.loadRecords();
            log.info("Loaded " + publicationRecords + " publication records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading publ authors...");
            PublicationLookup pubLookup = publicationLoader.getReferenceWorkLookup();
            PublAuthorLoader publAuthorLoader = new PublAuthorLoader(new File(dir, "publ_author.csv"),
                                                                     statement,
                                                                     pubLookup,
                                                                     botanistLookup);
            publAuthorLoader.setFrame(frame);
            int publAuthorRecords = publAuthorLoader.loadRecords();
            log.info("Loaded " + publAuthorRecords + " publ_author records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading taxa...");
            TaxonLoader taxonLoader = new TaxonLoader(new File(dir, "taxon.csv"),
                                                      statement,
                                                      pubLookup);
            taxonLoader.setFrame(frame);
            int taxonRecords = taxonLoader.loadRecords(); 
            log.info("Loaded " + taxonRecords + " taxonRecords");
            frame.setOverall(steps++);
            
            frame.setDesc("Numbering Taxonomy Tree...");
            taxonLoader.numberNodes();
            log.info("Numbered taxon tree");
 
            TaxonLookup taxonLookup = taxonLoader.getTaxonLookup();
            
            frame.setDesc("Loading subcollections...");
            SubcollectionLoader subcollectionLoader = new SubcollectionLoader(new File(dir, "subcollection.csv"),
                                                                              statement,
                                                                              new File(dir, "subcollection_botanist.csv"),
                                                                              botanistLookup);
            int subcollectionRecords = subcollectionLoader.loadRecords();
            log.info("Loaded " + subcollectionRecords + " subcollection records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading specimen items and specimens...");

            SubcollectionLookup subcollLookup = subcollectionLoader.getSubcollectionLookup();
            
            SpecimenItemLoader specimenItemLoader = new SpecimenItemLoader(new File(dir, "specimen_item.csv"),
                                                                           statement,
                                                                           new File(dir, "series_botanist.csv"),
                                                                           new File(dir, "specimen_item_id_barcode.csv"),
                                                                           botanistLookup,
                                                                           subcollLookup,
                                                                           siteLookup);
            specimenItemLoader.setFrame(frame);
            int specimenItemRecords = specimenItemLoader.loadRecords();
            log.info("Loaded " + specimenItemRecords + " specimen_item records");
            frame.setOverall(steps++);
            
            SpecimenLookup specimenLookup = specimenItemLoader.getSpecimenLookup();
            PreparationLookup prepLookup  = specimenItemLoader.getPreparationLookup();
            
            frame.setDesc("Loading determinations...");
            DeterminationLoader determinationLoader = new DeterminationLoader(new File(dir, "determination.csv"),
                                                                              statement,
                                                                              specimenLookup,
                                                                              taxonLookup);
            determinationLoader.setFrame(frame);
            int determinationRecords = determinationLoader.loadRecords();
            log.info("Loaded " + determinationRecords + " determination records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading type specimens...");
            TypeSpecimenLoader typeLoader = new TypeSpecimenLoader(new File(dir, "type_specimen.csv"),
                                                                       statement,
                                                                       specimenLookup,
                                                                       taxonLookup,
                                                                       pubLookup);
            typeLoader.setFrame(frame);
            int typeRecords = typeLoader.loadRecords();
            log.info("Loaded " + typeRecords + " type specimen records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading borrows...");
            BorrowLoader borrowLoader = new BorrowLoader(new File(dir, "borrow.csv"),
                                                         statement,
                                                         botanistLookup,
                                                         affiliateLookup,
                                                         agentLookup);
            borrowLoader.setFrame(frame);
            int borrowRecords = borrowLoader.loadRecords();
            log.info("Loaded " + borrowRecords + " borrow records");
            frame.setOverall(steps++);
            
            BorrowLookup borrowLookup = borrowLoader.getBorrowLookup();
            
            frame.setDesc("Loading incoming exchanges...");
            IncomingExchangeLoader inExchangeLoader =
                new IncomingExchangeLoader(new File(dir, "incoming_exchange.csv"), statement);
            inExchangeLoader.setFrame(frame);
            int inExchangeRecords = inExchangeLoader.loadRecords();
            log.info("Loaded " + inExchangeRecords + " incoming exchange records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading incoming gifts...");
            IncomingGiftLoader inGiftLoader =
                new IncomingGiftLoader(new File(dir, "incoming_gift.csv"), statement);
            inGiftLoader.setFrame(frame);
            int inGiftRecords = inGiftLoader.loadRecords();
            log.info("Loaded " + inGiftRecords + " incoming gift records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading loans...");
            LoanLoader loanLoader =
                new LoanLoader(new File(dir, "loan.csv"), statement);
            loanLoader.setFrame(frame);
            int loanLoaderRecords = loanLoader.loadRecords();
            log.info("Loaded " + loanLoaderRecords + " loan records");
            frame.setOverall(steps++);
            
            LoanLookup loanLookup = loanLoader.getLoanLookup();
            
            frame.setDesc("Loading outgoing exchanges...");
            OutgoingExchangeLoader outExchangeLoader =
                new OutgoingExchangeLoader(new File(dir, "outgoing_exchange.csv"), statement);
            outExchangeLoader.setFrame(frame);
            int outExchangeRecords = outExchangeLoader.loadRecords();
            log.info("Loaded " + outExchangeRecords + " outgoing exchange records");
            frame.setOverall(steps++);
            
            OutgoingExchangeLookup outExchangeLookup = outExchangeLoader.getOutgoingExchangeLookup();
            
            frame.setDesc("Loading outgoing gifts...");
            OutgoingGiftLoader outGiftLoader =
                new OutgoingGiftLoader(new File(dir, "outgoing_gift.csv"), statement);
            outGiftLoader.setFrame(frame);
            int outGiftRecords = outGiftLoader.loadRecords();
            log.info("Loaded " + outGiftRecords + " outgoing gift records");
            frame.setOverall(steps++);
            
            OutgoingGiftLookup outGiftLookup = outGiftLoader.getOutGoingGiftLookup();
            
            frame.setDesc("Loading purchases...");
            PurchaseLoader purchaseLoader =
                new PurchaseLoader(new File(dir, "purchase.csv"), statement);
            purchaseLoader.setFrame(frame);
            int purchaseRecords = purchaseLoader.loadRecords();
            log.info("Loaded " + purchaseRecords + " purchase records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading staff collections...");
            StaffCollectionLoader staffCollLoader =
                new StaffCollectionLoader(new File(dir, "staff_collection.csv"), statement);
            staffCollLoader.setFrame(frame);
            int staffCollRecords = staffCollLoader.loadRecords();
            log.info("Loaded " + staffCollRecords + " staff collection records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading shipments...");
            ShipmentLoader shipmentLoader = new ShipmentLoader(new File(dir, "shipment.csv"),
                                                               statement,
                                                               loanLookup,
                                                               outExchangeLookup);
            shipmentLoader.setFrame(frame);
            int shipmentRecords = shipmentLoader.loadRecords();
            log.info("Loaded " + shipmentRecords + " shipment records");
            frame.setOverall(steps++);
            
            CarrierLookup carrierLookup = shipmentLoader.getCarrierLookup();
            
            frame.setDesc("Loading taxon batches...");
            TaxonBatchLoader taxonBatchLoader = new TaxonBatchLoader(new File(dir, "taxon_batch.csv"),
                                                                     statement,
                                                                     borrowLookup,
                                                                     loanLookup);
            taxonBatchLoader.setFrame(frame);
            int taxonBatchRecords = taxonBatchLoader.loadRecords();
            log.info("Loaded " + taxonBatchRecords + " taxon batch records");
            frame.setOverall(steps++);
            
            TaxonBatchLookup taxonBatchLookup = taxonBatchLoader.getTaxonBatchLookup();
                        
            frame.setDesc("Loading in_return batches...");
            InReturnBatchLoader inReturnBatchLoader = new InReturnBatchLoader(new File(dir, "in_return_batch.csv"),
                                                                              statement,
                                                                              taxonBatchLookup);
            inReturnBatchLoader.setFrame(frame);
            int inReturnBatchRecords = inReturnBatchLoader.loadRecords();
            log.info("Loaded " + inReturnBatchRecords + " in return batch records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading loan items...");
            LoanItemLoader loanItemLoader = new LoanItemLoader(new File(dir, "loan_item.csv"),
                                                               statement,
                                                               prepLookup,
                                                               loanLookup);
            loanItemLoader.setFrame(frame);
            int loanItemRecords = loanItemLoader.loadRecords();
            log.info("Loaded " + loanItemRecords + " loan item records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading out_geo batches...");
            OutGeoBatchLoader outGeoBatchLoader = new OutGeoBatchLoader(new File(dir, "out_geo_batch.csv"),
                                                                        statement,
                                                                        outGiftLookup);
            outGeoBatchLoader.setFrame(frame);
            int outGeoBatchRecords = outGeoBatchLoader.loadRecords();
            log.info("Loaded " + outGeoBatchRecords + " out geo batch records");
            frame.setOverall(steps++);
            
            frame.setDesc("Loading out_return batches...");
            OutReturnBatchLoader outReturnBatchLoader = new OutReturnBatchLoader(new File(dir, "out_return_batch.csv"),
                                                                                 statement,
                                                                                 taxonBatchLookup,
                                                                                 carrierLookup,
                                                                                 borrowLookup);
            outReturnBatchLoader.setFrame(frame);
            int outReturnBatchRecords = outReturnBatchLoader.loadRecords();
            log.info("Loaded " + outReturnBatchRecords + " out return batch records");
            frame.setOverall(steps++);
            
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
