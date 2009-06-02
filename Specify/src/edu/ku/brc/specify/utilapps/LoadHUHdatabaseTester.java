package edu.ku.brc.specify.utilapps;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.loader.*;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.CarrierLookup;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.harvard.huh.asa2specify.lookup.IncomingGiftLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.harvard.huh.asa2specify.lookup.SiteLookup;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonBatchLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.specify.datamodel.Discipline;


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

public class LoadHUHdatabaseTester extends LoadHUHdatabase
{
    private static final Logger log = Logger.getLogger(LoadHUHdatabase.class);

    protected static Vector<DatabaseDriverInfo> driverList;
   
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        log.setLevel(Level.ALL);

        log.debug("debug");
        log.info("info");
        log.warn("warn");
        log.error("error");
 
        LoadHUHdatabaseTester test = new LoadHUHdatabaseTester();
        test.setUpConnection();

        // set up a database connection for direct sql TODO: manage statement, connection?
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DBConnection.getInstance().createConnection();
            statement  = connection.createStatement();
        }
        catch (SQLException e)
        {
            System.err.println("Couldn't create Statement: " + e.getMessage());
            return;
        }
        
        File dir = new File("/home/maureen/testload");
        
        int records = 0;
        
        boolean doOptr        = false; // 1
        boolean doGeo         = false; // 2
        boolean doSite        = false; // 3
        boolean doBot         = false; // 4
        boolean doBotName     = false; // 5
        boolean doBotTeam     = false; // 6
        boolean doBotCountry  = false; // 7
        boolean doBotSpec     = false; // 8
        boolean doOrg         = false; // 9
        boolean doSeries      = false; // 10
        boolean doAff         = false; // 11
        boolean doAgent       = false; // 12
        boolean doPub         = false; // 13
        boolean doPubAuth     = false; // 14
        boolean doTax         = false; // 15
        boolean doSubcoll     = false; // 16
        boolean doSpec        = false; // 17
        boolean doDet         = false; // 18
        boolean doType        = false; // 19
        boolean doBorrow      = false; // 20
        boolean doInEx        = false; // 21
        boolean doInGift      = false; // 22
        boolean doLoan        = false; // 23
        boolean doOutEx       = false; // 24
        boolean doOutGift     = false; // 25
        boolean doPurch       = false; // 26
        boolean doStaffColl   = false; // 27
        boolean doShip        = false; // 28
        boolean doTaxBatch    = false; // 29
        boolean doInGeoBatch  = false; // 30
        boolean doInRetBatch  = false; // 31
        boolean doLoanIt      = false; // 32
        boolean doOutGeoBatch = false; // 33
        boolean doOutRetBatch = true; // 34
        
        try
        {
            OptrLoader optrLoader = new OptrLoader(new File(dir, "optr.csv"), statement);
            if (doOptr)
            {
                int optrRecords = optrLoader.loadRecords();
                log.info("Loaded " + optrRecords + " optr records");
            }
            OptrLookup optrLookup = optrLoader.getOptrLookup();

            GeoUnitLoader geoUnitLoader = new GeoUnitLoader(new File(dir, "geo_unit.csv"),
                                                            statement,
                                                            optrLookup);
            if (doGeo)
            {
                int geoUnitRecords = geoUnitLoader.loadRecords();
                log.info("Loaded " + geoUnitRecords + " geo_unit records");
                geoUnitLoader.numberNodes();
                log.info("Numbered geography tree");
            }

            GeoUnitLookup geoLookup = geoUnitLoader.getGeographyLookup();

            SiteLoader siteLoader = new SiteLoader(new File(dir, "site.csv"), statement, geoLookup);
            if (doSite)
            {
                int localityRecords = siteLoader.loadRecords();
                log.info("Loaded " + localityRecords + " site records");
            }
            SiteLookup siteLookup = siteLoader.getSiteLookup();

            BotanistLoader botanistLoader = new BotanistLoader(new File(dir, "botanist.csv"),
                                                               statement,
                                                               new File(dir, "botanist_optr.csv"),
                                                               new File(dir, "optr_botanist.csv"),
                                                               optrLookup);
            if (doBot)
            {
                int botanistRecords = botanistLoader.loadRecords();
                log.info("Loaded " + botanistRecords + " botanist records");
            }
            BotanistLookup botanistLookup = botanistLoader.getBotanistLookup();

            BotanistNameLoader botanistNameLoader = new BotanistNameLoader(new File(dir, "botanist_name.csv"),
                                                                           statement,
                                                                           botanistLookup);
            if (doBotName)
            {
                int botanistNameRecords = botanistNameLoader.loadRecords();
                log.info("Loaded " + botanistNameRecords + " botanist_name records");
            }

            BotanistTeamLoader botanistTeamLoader = new BotanistTeamLoader(new File(dir, "botanist_team.csv"),
                                                                           statement,
                                                                           botanistLookup);
            if (doBotTeam)
            {
                int botanistTeamRecords = botanistTeamLoader.loadRecords();
                log.info("Loaded " + botanistTeamRecords + " botanist_team records");
            }
            
            BotanistCountryLoader botanistCountryLoader = new BotanistCountryLoader(new File(dir, "botanist_role_country.csv"),
                                                                                    statement,
                                                                                    geoLookup,
                                                                                    botanistLookup);
            if (doBotCountry)
            {
                int botanistCountryRecords = botanistCountryLoader.loadRecords();
                log.info("Loaded " + botanistCountryRecords + " botanist_country records");
            }
            
            BotanistSpecialtyLoader botanistSpecialtyLoader = new BotanistSpecialtyLoader(new File(dir, "botanist_role_specialty.csv"),
                                                                                          statement,
                                                                                          botanistLookup);
            if (doBotSpec)
            {
                int botanistSpecialtyRecords = botanistSpecialtyLoader.loadRecords();
                log.info("Loaded " + botanistSpecialtyRecords + " botanist_specialty records");
            }

            OrganizationLoader organizationLoader = new OrganizationLoader(new File(dir, "organization.csv"),
                                                                           statement,
                                                                           new File(dir, "org_botanist.csv"),
                                                                           botanistLookup);
            if (doOrg)
            {
                int organizationRecords = organizationLoader.loadRecords();
                log.info("Loaded " + organizationRecords + " organization records");
            }
            OrganizationLookup orgLookup = organizationLoader.getOrganizationLookup();
            
            SeriesLoader seriesLoader = new SeriesLoader(new File(dir, "series.csv"),
                                                         statement,
                                                         new File(dir, "series_botanist.csv"),
                                                         botanistLookup);
            if (doSeries)
            {
                int seriesRecords = seriesLoader.loadRecords();
                log.info("Loaded " + seriesRecords + " series records");
            }
         
            AffiliateLoader affiliateLoader = new AffiliateLoader(new File(dir, "affiliate.csv"),
                                                                  statement,
                                                                  new File(dir, "affiliate_botanist.csv"),
                                                                  botanistLookup);
            if (doAff)
            {
                int affiliateRecords = affiliateLoader.loadRecords();
                log.info("Loaded " + affiliateRecords + " affiliate records");
            }
            AffiliateLookup affiliateLookup = affiliateLoader.getAffiliateLookup();
            
            AgentLoader agentLoader = new AgentLoader(new File(dir, "agent.csv"),
                                                      statement,
                                                      new File(dir, "agent_botanist.csv"),
                                                      botanistLookup, 
                                                      orgLookup);
            if (doAgent)
            {
                int agentRecords = agentLoader.loadRecords();
                log.info("Loaded " + agentRecords + " agent records");
            }
            AgentLookup agentLookup = agentLoader.getAgentLookup();
            
            PublicationLoader publicationLoader = new PublicationLoader(new File(dir, "publication.csv"),
                                                                        statement);
            if (doPub)
            {
                int publicationRecords = publicationLoader.loadRecords();
                log.info("Loaded " + publicationRecords + " publication records");
            }
            
            PublicationLookup pubLookup = publicationLoader.getReferenceWorkLookup();
            
            PublAuthorLoader publAuthorLoader = new PublAuthorLoader(new File(dir, "publ_author.csv"),
                                                                     statement,
                                                                     pubLookup,
                                                                     botanistLookup);
            if (doPubAuth)
            {
                int publAuthorRecords = publAuthorLoader.loadRecords();
                log.info("Loaded " + publAuthorRecords + " publ_author records");
            }
            
            TaxonLoader taxonLoader = new TaxonLoader(new File(dir, "taxon.csv"),
                                                      statement,
                                                      pubLookup);
            if (doTax)
            {
                int taxonRecords = taxonLoader.loadRecords(); 
                log.info("Loaded " + taxonRecords + " taxonRecords");

                taxonLoader.numberNodes();
                log.info("Numbered taxon tree");
            }
            
            TaxonLookup taxonLookup = taxonLoader.getTaxonLookup();
            
            SubcollectionLoader subcollectionLoader = new SubcollectionLoader(new File(dir, "subcollection.csv"),
                                                                              statement,
                                                                              new File(dir, "subcollection_botanist.csv"),
                                                                              taxonLookup,
                                                                              botanistLookup);
            if (doSubcoll)
            {
                int subcollectionRecords = subcollectionLoader.loadRecords();
                log.info("Loaded " + subcollectionRecords + " subcollection records");
            }

            SubcollectionLookup subcollLookup = subcollectionLoader.getSubcollectionLookup();
            
            SpecimenItemLoader specimenItemLoader = new SpecimenItemLoader(new File(dir, "specimen_item.csv"),
                                                                           statement,
                                                                           new File(dir, "series_botanist.csv"),
                                                                           botanistLookup,
                                                                           subcollLookup,
                                                                           siteLookup);
            if (doSpec)
            {
                int specimenItemRecords = specimenItemLoader.loadRecords();
                log.info("Loaded " + specimenItemRecords + " specimen_item records");
            }
            
            SpecimenLookup specimenLookup = specimenItemLoader.getSpecimenLookup();
            PreparationLookup prepLookup  = specimenItemLoader.getPreparationLookup();
            
            DeterminationLoader determinationLoader = new DeterminationLoader(new File(dir, "determination.csv"),
                                                                              statement,
                                                                              specimenLookup,
                                                                              taxonLookup);
            if (doDet)
            {
                int determinationRecords = determinationLoader.loadRecords();
                log.info("Loaded " + determinationRecords + " determination records");
            }
            
            TypeSpecimenLoader typeLoader = new TypeSpecimenLoader(new File(dir, "type_specimen.csv"),
                                                                       statement,
                                                                       specimenLookup,
                                                                       taxonLookup,
                                                                       pubLookup);
            if (doType)
            {
                int typeRecords = typeLoader.loadRecords();
                log.info("Loaded " + typeRecords + " type specimen records");
            }
            
            BorrowLoader borrowLoader = new BorrowLoader(new File(dir, "borrow.csv"),
                                                         statement,
                                                         botanistLookup,
                                                         affiliateLookup,
                                                         agentLookup);
            if (doBorrow)
            {
                int borrowRecords = borrowLoader.loadRecords();
                log.info("Loaded " + borrowRecords + " borrow records");
            }
            
            BorrowLookup borrowLookup = borrowLoader.getBorrowLookup();
            
            IncomingExchangeLoader inExchangeLoader =
                new IncomingExchangeLoader(new File(dir, "incoming_exchange.csv"), statement);

            if (doInEx)
            {
                int inExchangeRecords = inExchangeLoader.loadRecords();
                log.info("Loaded " + inExchangeRecords + " incoming exchange records");
            }
            
            IncomingGiftLoader inGiftLoader =
                new IncomingGiftLoader(new File(dir, "incoming_gift.csv"), statement);
            
            if (doInGift)
            {
                int inGiftRecords = inGiftLoader.loadRecords();
                log.info("Loaded " + inGiftRecords + " incoming gift records");
            }
            
            IncomingGiftLookup inGiftLookup = inGiftLoader.getIncomingGiftLookup();
            
            LoanLoader loanLoader =
                new LoanLoader(new File(dir, "loan.csv"), statement);
            
            if (doLoan)
            {
                int loanLoaderRecords = loanLoader.loadRecords();
                log.info("Loaded " + loanLoaderRecords + " loan records");
            }

            LoanLookup loanLookup = loanLoader.getLoanLookup();
            
            OutgoingExchangeLoader outExchangeLoader =
                new OutgoingExchangeLoader(new File(dir, "outgoing_exchange.csv"), statement);
            
            if (doOutEx)
            {
                int outExchangeRecords = outExchangeLoader.loadRecords();
                log.info("Loaded " + outExchangeRecords + " outgoing exchange records");
            }
            
            OutgoingExchangeLookup outExchangeLookup = outExchangeLoader.getOutgoingExchangeLookup();
            
            OutgoingGiftLoader outGiftLoader =
                new OutgoingGiftLoader(new File(dir, "outgoing_gift.csv"), statement);
            
            if (doOutGift)
            {
                int outGiftRecords = outGiftLoader.loadRecords();
                log.info("Loaded " + outGiftRecords + " outgoing gift records");
            }
            
            OutgoingGiftLookup outGiftLookup = outGiftLoader.getOutGoingGiftLookup();
            
            PurchaseLoader purchaseLoader =
                new PurchaseLoader(new File(dir, "purchase.csv"), statement);
            
            if (doPurch)
            {
                int purchaseRecords = purchaseLoader.loadRecords();
                log.info("Loaded " + purchaseRecords + " purchase records");
            }
            
            StaffCollectionLoader staffCollLoader =
                new StaffCollectionLoader(new File(dir, "staff_collection.csv"), statement);
            
            if (doStaffColl)
            {
                int staffCollRecords = staffCollLoader.loadRecords();
                log.info("Loaded " + staffCollRecords + " staff collection records");
            }
            
            ShipmentLoader shipmentLoader = new ShipmentLoader(new File(dir, "shipment.csv"),
                                                               statement,
                                                               loanLookup,
                                                               outExchangeLookup);
            if (doShip)
            {
                int shipmentRecords = shipmentLoader.loadRecords();
                log.info("Loaded " + shipmentRecords + " shipment records");
            }
            
            CarrierLookup carrierLookup = shipmentLoader.getCarrierLookup();
            
            TaxonBatchLoader taxonBatchLoader = new TaxonBatchLoader(new File(dir, "taxon_batch.csv"),
                                                                     statement,
                                                                     borrowLookup,
                                                                     loanLookup);
            if (doTaxBatch)
            {
                int taxonBatchRecords = taxonBatchLoader.loadRecords();
                log.info("Loaded " + taxonBatchRecords + " taxon batch records");
            }
            
            TaxonBatchLookup taxonBatchLookup = taxonBatchLoader.getTaxonBatchLookup();
            
            InGeoBatchLoader inGeoBatchLoader = new InGeoBatchLoader(new File(dir, "in_geo_batch.csv"),
                                                                     statement,
                                                                     inGiftLookup);
            if (doInGeoBatch)
            {
                int inGeoBatchRecords = inGeoBatchLoader.loadRecords();
                log.info("Loaded " + inGeoBatchRecords + " in geo batch records");
            }
            
            InReturnBatchLoader inReturnBatchLoader = new InReturnBatchLoader(new File(dir, "in_return_batch.csv"),
                                                                              statement,
                                                                              taxonBatchLookup);
            if (doInRetBatch)
            {
                int inReturnBatchRecords = inReturnBatchLoader.loadRecords();
                log.info("Loaded " + inReturnBatchRecords + " in return batch records");
            }
            
            LoanItemLoader loanItemLoader = new LoanItemLoader(new File(dir, "loan_item.csv"),
                                                               statement,
                                                               prepLookup,
                                                               loanLookup);
            if (doLoanIt)
            {
                int loanItemRecords = loanItemLoader.loadRecords();
                log.info("Loaded " + loanItemRecords + " loan item records");
            }
            
            OutGeoBatchLoader outGeoBatchLoader = new OutGeoBatchLoader(new File(dir, "out_geo_batch.csv"),
                                                                        statement,
                                                                        outGiftLookup);
            if (doOutGeoBatch)
            {
                int outGeoBatchRecords = outGeoBatchLoader.loadRecords();
                log.info("Loaded " + outGeoBatchRecords + " out geo batch records");
            }
            
            OutReturnBatchLoader outReturnBatchLoader = new OutReturnBatchLoader(new File(dir, "out_return_batch.csv"),
                                                                                 statement,
                                                                                 taxonBatchLookup,
                                                                                 carrierLookup,
                                                                                 borrowLookup);
            if (doOutRetBatch)
            {
                int outReturnBatchRecords = outReturnBatchLoader.loadRecords();
                log.info("Loaded " + outReturnBatchRecords + " out return batch records");
            }
            
            log.info("Loaded " + records + " records");
        }
        catch (LocalException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Processed " + records + " records");
    }
    
    void setUpConnection() {
        String dbName = "specify";

        Vector<DatabaseDriverInfo> driverList = DatabaseDriverInfo.getDriversList();
        DatabaseDriverInfo driverInfo = DatabaseDriverInfo.getInfoByName(driverList, "MySQL");

        String userName = "Specify";
        String password = "Specify";
        String hostname = "localhost";

        String connStr = driverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Create, 
                hostname, 
                dbName);

        if (connStr == null)
        {
            connStr = driverInfo.getConnectionStr(DatabaseDriverInfo.ConnectionType.Open, hostname,  dbName);
        }
        else {
            System.err.println("Couldn't create connection");
        }

        DBConnection dbConn = DBConnection.getInstance();

        dbConn.setDriver(driverInfo.getDriverClassName());
        dbConn.setDialect(driverInfo.getDialectClassName());
        dbConn.setDatabaseName(dbName);
        dbConn.setConnectionStr(connStr);
        dbConn.setUsernamePassword(userName, password);

        Connection connection = dbConn.createConnection();
        if (connection != null)
        {
            try
            {
                connection.close();

            } catch (SQLException ex)
            {
                // do nothing
            }
        }
        else {
            System.err.println("Couldn't open connection");
        }
    }

    Discipline getBotanyDiscipline() {
        Discipline discipline = new Discipline();
        discipline.setDisciplineId(3);
        
        return discipline;
    }
    
}