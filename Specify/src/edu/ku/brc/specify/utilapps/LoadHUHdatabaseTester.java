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
import edu.harvard.huh.asa2specify.loader.AffiliateLoader;
import edu.harvard.huh.asa2specify.loader.AgentLoader;
import edu.harvard.huh.asa2specify.loader.BasionymLoader;
import edu.harvard.huh.asa2specify.loader.BorrowLoader;
import edu.harvard.huh.asa2specify.loader.BotanistCitationLoader;
import edu.harvard.huh.asa2specify.loader.BotanistCountryLoader;
import edu.harvard.huh.asa2specify.loader.BotanistLoader;
import edu.harvard.huh.asa2specify.loader.BotanistNameLoader;
import edu.harvard.huh.asa2specify.loader.BotanistSpecialtyLoader;
import edu.harvard.huh.asa2specify.loader.BotanistTeamLoader;
import edu.harvard.huh.asa2specify.loader.DeterminationLoader;
import edu.harvard.huh.asa2specify.loader.GeoUnitLoader;
import edu.harvard.huh.asa2specify.loader.InReturnBatchLoader;
import edu.harvard.huh.asa2specify.loader.IncomingExchangeLoader;
import edu.harvard.huh.asa2specify.loader.IncomingGiftLoader;
import edu.harvard.huh.asa2specify.loader.LoanItemLoader;
import edu.harvard.huh.asa2specify.loader.LoanLoader;
import edu.harvard.huh.asa2specify.loader.OptrLoader;
import edu.harvard.huh.asa2specify.loader.OrganizationLoader;
import edu.harvard.huh.asa2specify.loader.OutGeoBatchLoader;
import edu.harvard.huh.asa2specify.loader.OutReturnBatchLoader;
import edu.harvard.huh.asa2specify.loader.OutgoingExchangeLoader;
import edu.harvard.huh.asa2specify.loader.OutgoingGiftLoader;
import edu.harvard.huh.asa2specify.loader.OutgoingMiscLoader;
import edu.harvard.huh.asa2specify.loader.PublAuthorLoader;
import edu.harvard.huh.asa2specify.loader.PublicationLoader;
import edu.harvard.huh.asa2specify.loader.PublicationNumberLoader;
import edu.harvard.huh.asa2specify.loader.PublicationTitleLoader;
import edu.harvard.huh.asa2specify.loader.PurchaseLoader;
import edu.harvard.huh.asa2specify.loader.RelatedPublicationLoader;
import edu.harvard.huh.asa2specify.loader.SeriesLoader;
import edu.harvard.huh.asa2specify.loader.ShipmentLoader;
import edu.harvard.huh.asa2specify.loader.SiteLoader;
import edu.harvard.huh.asa2specify.loader.SpecimenItemLoader;
import edu.harvard.huh.asa2specify.loader.StaffCollectionLoader;
import edu.harvard.huh.asa2specify.loader.SubcollectionLoader;
import edu.harvard.huh.asa2specify.loader.TaxonLoader;
import edu.harvard.huh.asa2specify.loader.TypeSpecimenLoader;
import edu.harvard.huh.asa2specify.lookup.AffiliateLookup;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowLookup;
import edu.harvard.huh.asa2specify.lookup.BorrowMaterialLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.CarrierLookup;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.lookup.LoanPreparationLookup;
import edu.harvard.huh.asa2specify.lookup.OptrLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.harvard.huh.asa2specify.lookup.SeriesLookup;
import edu.harvard.huh.asa2specify.lookup.SiteLookup;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;

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
    private static final Logger log = Logger.getLogger(LoadHUHdatabaseTester.class);

    protected static Vector<DatabaseDriverInfo> driverList;
   
    private String specifyDbName = "specify";
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        log.setLevel(Level.ALL);

        //log.debug("debug");
        //log.info("info");
        //log.warn("warn");
        //log.error("error");
        
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
        
        File dir = new File("/home/maureen/load");
        
        boolean doOptr        = false; // 1 DID YOU REMEMBER TO GRANT PRIVILEGES TO THE SPECIFY ADM USER?
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
        boolean doPubNumber   = false; // 15
        boolean doPubTitle    = false; // 16
        boolean doRelatedPub  = false; // 17
        boolean doBotCit      = false; // 18
        
        boolean doTax         = false; // 19

        boolean doBasionym    = false; // 20
        boolean doSubcoll     = false; // 21
        
        boolean doSpec        = false; // 22
        
        boolean doDet         = false; // 23
        
        boolean doType        = false; // 24
        
        boolean doBorrow      = false; // 25
        boolean doInEx        = false; // 26
        boolean doInGift      = false; // 27
        boolean doLoan        = false; // 28
        boolean doOutEx       = false; // 29
        boolean doOutGift     = false; // 30
        boolean doOutGeoBatch = false; // 31
        boolean doPurch       = false; // 32
        boolean doStaffColl   = false; // 33
        boolean doShip        = false; // 34
        boolean doInRetBatch  = false; // 35
        boolean doLoanIt      = false; // 36
        boolean doOutRetBatch = false; // 37
        boolean doOutMisc     = false; // 38
        
        try
        {
            OptrLoader optrLoader = new OptrLoader(new File(dir, "optr.csv"), statement);
            if (doOptr)
            {
                int optrRecords = optrLoader.loadRecords();
                log.info("Processed " + optrRecords + " optr records");
            }
            OptrLookup optrLookup = optrLoader.getOptrLookup();

            GeoUnitLoader geoUnitLoader = new GeoUnitLoader(new File(dir, "geo_unit.csv"),
                                                            statement,
                                                            optrLookup);
            if (doGeo)
            {
                int geoUnitRecords = geoUnitLoader.loadRecords();
                log.info("Processed " + geoUnitRecords + " geo_unit records");
                log.info("Numbering geography tree...");
                geoUnitLoader.numberNodes();
                log.info("Numbered geography tree");
            }

            GeoUnitLookup geoLookup = geoUnitLoader.getGeographyLookup();

            SiteLoader siteLoader = new SiteLoader(new File(dir, "site.csv"), statement, geoLookup);
            if (doSite)
            {
                int localityRecords = siteLoader.loadRecords();
                log.info("Processed " + localityRecords + " site records");
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
                log.info("Processed " + botanistRecords + " botanist records");
            }
            BotanistLookup botanistLookup = botanistLoader.getBotanistLookup();

            BotanistNameLoader botanistNameLoader = new BotanistNameLoader(new File(dir, "botanist_name.csv"),
                                                                           statement,
                                                                           botanistLookup);
            if (doBotName)
            {
                int botanistNameRecords = botanistNameLoader.loadRecords();
                log.info("Processed " + botanistNameRecords + " botanist_name records");
            }

            BotanistTeamLoader botanistTeamLoader = new BotanistTeamLoader(new File(dir, "botanist_team.csv"),
                                                                           statement,
                                                                           botanistLookup);
            if (doBotTeam)
            {
                int botanistTeamRecords = botanistTeamLoader.loadRecords();
                log.info("Processed " + botanistTeamRecords + " botanist_team records");
            }
            
            BotanistCountryLoader botanistCountryLoader = new BotanistCountryLoader(new File(dir, "botanist_role_country.csv"),
                                                                                    statement,
                                                                                    geoLookup,
                                                                                    botanistLookup);
            if (doBotCountry)
            {
                int botanistCountryRecords = botanistCountryLoader.loadRecords();
                log.info("Processed " + botanistCountryRecords + " botanist_country records");
            }
            
            BotanistSpecialtyLoader botanistSpecialtyLoader = new BotanistSpecialtyLoader(new File(dir, "botanist_role_specialty.csv"),
                                                                                          statement,
                                                                                          botanistLookup);
            if (doBotSpec)
            {
                int botanistSpecialtyRecords = botanistSpecialtyLoader.loadRecords();
                log.info("Processed " + botanistSpecialtyRecords + " botanist_specialty records");
            }

            OrganizationLoader organizationLoader = new OrganizationLoader(new File(dir, "organization.csv"),
                                                                           statement,
                                                                           new File(dir, "org_botanist.csv"),
                                                                           botanistLookup);
            if (doOrg)
            {
                int organizationRecords = organizationLoader.loadRecords();
                log.info("Processed " + organizationRecords + " organization records");
            }
            OrganizationLookup orgLookup = organizationLoader.getOrganizationLookup();
            
            SeriesLoader seriesLoader = new SeriesLoader(new File(dir, "series.csv"),
                                                         statement,
                                                         new File(dir, "series_botanist.csv"),
                                                         botanistLookup);
            if (doSeries)
            {
                int seriesRecords = seriesLoader.loadRecords();
                log.info("Processed " + seriesRecords + " series records");
            }
         
            SeriesLookup seriesLookup = seriesLoader.getSeriesLookup();
            
            AffiliateLoader affiliateLoader = new AffiliateLoader(new File(dir, "affiliate.csv"),
                                                                  statement,
                                                                  new File(dir, "affiliate_botanist.csv"),
                                                                  botanistLookup);
            if (doAff)
            {
                int affiliateRecords = affiliateLoader.loadRecords();
                log.info("Processed " + affiliateRecords + " affiliate records");
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
                log.info("Processed " + agentRecords + " agent records");
            }
            AgentLookup agentLookup = agentLoader.getAgentLookup();
            
            PublicationLoader publicationLoader = new PublicationLoader(new File(dir, "publication.csv"),
                                                                        statement);
            if (doPub)
            {
                int publicationRecords = publicationLoader.loadRecords();
                log.info("Processed " + publicationRecords + " publication records");
            }
            
            PublicationLookup pubLookup = publicationLoader.getReferenceWorkLookup();
            
            PublAuthorLoader publAuthorLoader = new PublAuthorLoader(new File(dir, "publ_author.csv"),
                                                                     statement,
                                                                     pubLookup,
                                                                     botanistLookup);
            if (doPubAuth)
            {
                int publAuthorRecords = publAuthorLoader.loadRecords();
                log.info("Processed " + publAuthorRecords + " publ_author records");
            }
            
            PublicationNumberLoader publNumberLoader = new PublicationNumberLoader(new File(dir, "publ_number.csv"),
                                                                                   statement,
                                                                                   pubLookup);
            if (doPubNumber)
            {
                int publNumberRecords = publNumberLoader.loadRecords();
                log.info("Processed " + publNumberRecords + " publ_number records");
            }

            PublicationTitleLoader publTitleLoader = new PublicationTitleLoader(new File(dir, "publ_title.csv"),
                                                                                statement,
                                                                                pubLookup);
            if (doPubTitle)
            {
                int publTitleRecords = publTitleLoader.loadRecords();
                log.info("Processed " + publTitleRecords + " publ_title records");
            }

            RelatedPublicationLoader relatedPubLoader = new RelatedPublicationLoader(new File(dir, "related_pub.csv"),
                                                                                     statement,
                                                                                     pubLookup);
            if (doRelatedPub)
            {
                int relatedPubRecords = relatedPubLoader.loadRecords();
                log.info("Processed " + relatedPubRecords + " related publication records");
            }

            BotanistCitationLoader botCitLoader = new BotanistCitationLoader(new File(dir, "bot_cit.csv"),
                                                                             statement,
                                                                             pubLookup,
                                                                             botanistLookup);
            if (doBotCit)
            {
                int botCitRecords = botCitLoader.loadRecords();
                log.info("Processed " + botCitRecords + " botanist_role_citation records");
            }
            
            TaxonLoader taxonLoader = new TaxonLoader(new File(dir, "taxon.csv"),
                                                      statement,
                                                      pubLookup,
                                                      botanistLookup);

            if (doTax)
            {
                int taxonRecords = taxonLoader.loadRecords(); 
                log.info("Processed " + taxonRecords + " taxon records");

                log.info("Numbering taxon tree...");
                taxonLoader.numberNodes();
                log.info("Numbered taxon tree");
            }
            
            TaxonLookup taxonLookup = taxonLoader.getTaxonLookup();
            
            BasionymLoader basionymLoader = new BasionymLoader(new File(dir, "basionym.csv"),
                                                               statement,
                                                               taxonLookup);
            
            if (doBasionym)
            {
                int basionymRecords = basionymLoader.loadRecords();
                log.info("Processed " + basionymRecords + " basionym records");
            }

            SubcollectionLoader subcollectionLoader = new SubcollectionLoader(new File(dir, "subcollection.csv"),
                                                                              statement,
                                                                              new File(dir, "subcollection_botanist.csv"),
                                                                              botanistLookup);
            if (doSubcoll)
            {
                int subcollectionRecords = subcollectionLoader.loadRecords();
                log.info("Processed " + subcollectionRecords + " subcollection records");
                log.info("Numbering storage tree...");
                subcollectionLoader.numberNodes();
                log.info("Numbered storage tree");
            }

            SubcollectionLookup subcollLookup = subcollectionLoader.getSubcollectionLookup();
            
            SpecimenItemLoader specimenItemLoader = new SpecimenItemLoader(new File(dir, "specimen_item.csv"),
                                                                           statement,
                                                                           new File(dir, "series_botanist.csv"),
                                                                           new File(dir, "specimen_item_id_barcode.csv"),
                                                                           botanistLookup,
                                                                           subcollLookup,
                                                                           seriesLookup,
                                                                           siteLookup);
            if (doSpec)
            {
                int specimenItemRecords = specimenItemLoader.loadRecords();
                log.info("Processed " + specimenItemRecords + " specimen_item records");
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
                log.info("Processed " + determinationRecords + " determination records");
            }
            
            TypeSpecimenLoader typeLoader = new TypeSpecimenLoader(new File(dir, "type_specimen.csv"),
                                                                       statement,
                                                                       specimenLookup,
                                                                       taxonLookup,
                                                                       pubLookup,
                                                                       botanistLookup);
            if (doType)
            {
                int typeRecords = typeLoader.loadRecords();
                log.info("Processed " + typeRecords + " type specimen records");
            }
            
            BorrowLoader borrowLoader = new BorrowLoader(new File(dir, "borrow.csv"),
                                                         statement,
                                                         botanistLookup,
                                                         affiliateLookup,
                                                         agentLookup,
                                                         orgLookup);
            if (doBorrow)
            {
                int borrowRecords = borrowLoader.loadRecords();
                log.info("Processed " + borrowRecords + " borrow records");
            }
            
            BorrowLookup borrowLookup = borrowLoader.getBorrowLookup();
            BorrowMaterialLookup borrowMaterialLookup = borrowLoader.getBorrowMaterialLookup();

            IncomingExchangeLoader inExchangeLoader =
                new IncomingExchangeLoader(new File(dir, "incoming_exchange.csv"), statement);

            if (doInEx)
            {
                int inExchangeRecords = inExchangeLoader.loadRecords();
                log.info("Processed " + inExchangeRecords + " incoming exchange records");
            }
            
            IncomingGiftLoader inGiftLoader =
                new IncomingGiftLoader(new File(dir, "incoming_gift.csv"), statement);
            
            if (doInGift)
            {
                int inGiftRecords = inGiftLoader.loadRecords();
                log.info("Processed " + inGiftRecords + " incoming gift records");
            }
            
            LoanLoader loanLoader = new LoanLoader(new File(dir, "loan.csv"),
                                                   statement,
                                                   botanistLookup,
                                                   affiliateLookup,
                                                   agentLookup,
                                                   orgLookup,
                                                   new File(dir, "loan_botanist.csv"));
            
            if (doLoan)
            {
                int loanLoaderRecords = loanLoader.loadRecords();
                log.info("Processed " + loanLoaderRecords + " loan records");
            }

            LoanLookup loanLookup = loanLoader.getLoanLookup();
            LoanPreparationLookup loanPrepLookup = loanLoader.getLoanPrepLookup();
            
            OutgoingExchangeLoader outExchangeLoader =
                new OutgoingExchangeLoader(new File(dir, "outgoing_exchange.csv"), statement);
            
            if (doOutEx)
            {
                int outExchangeRecords = outExchangeLoader.loadRecords();
                log.info("Processed " + outExchangeRecords + " outgoing exchange records");
            }
            
            OutgoingExchangeLookup outExchangeLookup = outExchangeLoader.getOutgoingExchangeLookup();
            
            OutgoingGiftLoader outGiftLoader =
                new OutgoingGiftLoader(new File(dir, "outgoing_gift.csv"),
                                       statement,
                                       botanistLookup,
                                       new File(dir, "out_gift_botanist.csv"));
            
            if (doOutGift)
            {
                int outGiftRecords = outGiftLoader.loadRecords();
                log.info("Processed " + outGiftRecords + " outgoing gift records");
            }
            
            OutgoingGiftLookup outGiftLookup = outGiftLoader.getOutGoingGiftLookup();
            
            OutGeoBatchLoader outGeoBatchLoader =
                new OutGeoBatchLoader(new File(dir, "out_geo_batch.csv"),
                                      statement,
                                      outExchangeLookup,
                                      outGiftLookup);
            
            if (doOutGeoBatch)
            {
                int outGeoBatchRecords = outGeoBatchLoader.loadRecords();
                log.info("Processed " + outGeoBatchRecords + " out geo batch records");
            }
            
            PurchaseLoader purchaseLoader =
                new PurchaseLoader(new File(dir, "purchase.csv"), statement);
            
            if (doPurch)
            {
                int purchaseRecords = purchaseLoader.loadRecords();
                log.info("Processed " + purchaseRecords + " purchase records");
            }
            
            StaffCollectionLoader staffCollLoader =
                new StaffCollectionLoader(new File(dir, "staff_collection.csv"), statement);
            
            if (doStaffColl)
            {
                int staffCollRecords = staffCollLoader.loadRecords();
                log.info("Processed " + staffCollRecords + " staff collection records");
            }
            
            ShipmentLoader shipmentLoader = new ShipmentLoader(new File(dir, "shipment.csv"),
                                                               statement,
                                                               loanLookup,
                                                               outExchangeLookup,
                                                               outGiftLookup);
            if (doShip)
            {
                int shipmentRecords = shipmentLoader.loadRecords();
                log.info("Processed " + shipmentRecords + " shipment records");
            }
            
            CarrierLookup carrierLookup = shipmentLoader.getCarrierLookup();
            
            InReturnBatchLoader inReturnBatchLoader = new InReturnBatchLoader(new File(dir, "in_return_batch.csv"),
                                                                              statement,
                                                                              loanPrepLookup);
            if (doInRetBatch)
            {
                int inReturnBatchRecords = inReturnBatchLoader.loadRecords();
                log.info("Processed " + inReturnBatchRecords + " in return batch records");
            }
            
            LoanItemLoader loanItemLoader = new LoanItemLoader(new File(dir, "loan_item.csv"),
                                                               statement,
                                                               prepLookup,
                                                               loanLookup,
                                                               new File(dir, "barcode_specimen_item_id.csv"));
            if (doLoanIt)
            {
                int loanItemRecords = loanItemLoader.loadRecords();
                log.info("Processed " + loanItemRecords + " loan item records");
            }
            
            OutReturnBatchLoader outReturnBatchLoader = new OutReturnBatchLoader(new File(dir, "out_return_batch.csv"),
                                                                                 statement,
                                                                                 borrowMaterialLookup,
                                                                                 carrierLookup,
                                                                                 borrowLookup);
            if (doOutRetBatch)
            {
                int outReturnBatchRecords = outReturnBatchLoader.loadRecords();
                log.info("Processed " + outReturnBatchRecords + " out return batch records");
            }
            
            OutgoingMiscLoader outMiscLoader = new OutgoingMiscLoader(new File (dir, "out_misc.csv"),
                                                                      statement,
                                                                      carrierLookup);
            
            if (doOutMisc)
            {
                int outMiscRecords = outMiscLoader.loadRecords();
                log.info("Processed " + outMiscRecords + " outgoing misc records");
            }

        }
        catch (LocalException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    void setUpConnection() {
        String dbName = specifyDbName;

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