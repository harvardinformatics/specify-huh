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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistName;
import edu.harvard.huh.asa.BotanistRoleCountry;
import edu.harvard.huh.asa.BotanistRoleSpecialty;
import edu.harvard.huh.asa.Publication;
import edu.harvard.huh.asa.Site;
import edu.harvard.huh.asa2specify.BotanistConverter;
import edu.harvard.huh.asa2specify.BotanistNameConverter;
import edu.harvard.huh.asa2specify.BotanistRoleCountryConverter;
import edu.harvard.huh.asa2specify.BotanistRoleSpecialtyConverter;
import edu.harvard.huh.asa2specify.CsvToSqlMgr;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.PublicationConverter;
import edu.harvard.huh.asa2specify.SiteConverter;
import edu.harvard.huh.asa2specify.SqlUtils;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentGeography;
import edu.ku.brc.specify.datamodel.AgentSpecialty;
import edu.ku.brc.specify.datamodel.AgentVariant;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.dbsupport.SpecifyDeleteHelper;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.ProgressFrame;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.util.LatLonConverter.FORMAT;

public class LoadHUHdatabase
{
    private static final Logger  log      = Logger.getLogger(LoadHUHdatabase.class);

    protected Session            session;
    protected int                steps = 0; 
    protected ProgressFrame      frame;
    protected boolean            hideFrame = false;

    // set a default srcLatLongUnit
    private static final byte srcLatLongUnit = (byte) FORMAT.DDDDDD.ordinal();
    
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
            
            Discipline discipline = null;

            loadLocalities(discipline);
            loadBotanists();
            loadBotanistNames();
            loadBotanistCountries();
            loadBotanistSpecialties();
            
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
    
    // see BuildSampleDatabase.convertLithoStratFromCSV
    @SuppressWarnings("unchecked")
    protected int loadLocalities(Discipline discipline) throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/sites.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating locality objects for each via sql
        int counter = 0;

        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            Site site = null;
            try {
                site = parseSiteRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }

            // skip this record if it only has SRE initial values set
            if (! site.hasData()) continue;

            // get a converter
            SiteConverter siteConverter = SiteConverter.getInstance();

            // convert site into locality
            Locality locality = siteConverter.convert(site);
            locality.setSrcLatLongUnit(srcLatLongUnit);
            locality.setDiscipline(discipline);

            // convert locality to sql and insert
            Integer id = null;
            try {
                String sql = getInsertSql(locality);
                
                id = csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert locality record for line " + counter, e);
                continue;
            }

            // update locality with geography
            Integer geoUnitId = site.getGeoUnitId();
            try {
                if (geoUnitId != null)
                {
                    String guid = SqlUtils.sqlString(geoUnitId);

                    String sql = SqlUtils.getQueryIdByFieldSql("geography", "GeographyID", "GUID", guid);

                    Integer geographyId = csvToSqlMgr.queryForId(sql);

                    if (geographyId != null)
                    {

                        sql = SqlUtils.getUpdateSql("locality",
                                "GeographyID",
                                String.valueOf(geographyId),
                                "LocalityID",
                                String.valueOf(id));

                        if (! csvToSqlMgr.update(sql))
                        {
                            log.error("Couldn't set GeographyID for LocalityID " + id);
                        }
                    }
                    else
                    {
                        log.warn("Couldn't find GeographyID with GUID " + geoUnitId);
                    }
                }
            }
            catch (LocalException e)
            {
                log.error("Couldn't set GeographyID for LocalityID " + id, e);
            }
        }

        return counter;
    }

    protected int loadBotanists() throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/botanist.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);
        
        // iterate over lines, creating locality objects for each via sql
        int counter = 0;

        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            Botanist botanist = null;
            try {
                botanist = parseBotanistRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }


            // get a converter
            BotanistConverter botanistConverter = BotanistConverter.getInstance();

            // convert botanist into agent ...
            Agent agent = botanistConverter.convert( botanist );

            // convert locality to sql and insert
            try {
                String sql = getInsertSql(agent);

                csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert agent record for line " + counter, e);
                continue;
            }
        }
        return counter;
    }
    
    protected int loadBotanistNames() throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/botanist_name.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating locality objects for each via sql
        int counter = 0;

        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            BotanistName botanistName = null;
            try {
                botanistName = parseBotanistNameRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }

            // find the matching agent record
            Integer agentId = null;
            Integer botanistId = botanistName.getBotanistId();

            if (botanistId != null)
            {
                Botanist botanist = new Botanist();
                botanist.setId(botanistId);

                String guid = SqlUtils.sqlString(botanist.getGuid());

                String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

                agentId = csvToSqlMgr.queryForId(sql);

                if (agentId == null)
                {
                    log.error("Couldn't find AgentID with GUID " + guid);
                    continue;
                }
            }
            
            // get a converter
            BotanistNameConverter botanistNameConverter = BotanistNameConverter.getInstance();

            // convert BotanistName into AgentVariant
            AgentVariant agentVariant = botanistNameConverter.convert(botanistName);

            Agent agent = new Agent();
            agent.setAgentId(agentId);
            
            agentVariant.setAgent(agent);

            // convert agentvariant to sql and insert
            try {
                String sql = getInsertSql(agentVariant);
                
                csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert agentvariant record for line " + counter, e);
                continue;
            }
        }

        return counter;
    }
    
    protected int loadBotanistCountries() throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/botanist_role_country.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating agentgeography objects for each via sql
        int counter = 0;
        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            BotanistRoleCountry botanistRoleCountry = null;
            try {
                botanistRoleCountry = parseBotanistRoleCountryRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }

            // find the matching agent record
            Integer agentId = null;
            Integer botanistId = botanistRoleCountry.getBotanistId();

            if (botanistId != null)
            {
                Botanist botanist = new Botanist();
                botanist.setId(botanistId);

                String guid = SqlUtils.sqlString(botanist.getGuid());

                String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

                agentId = csvToSqlMgr.queryForId(sql);

                if (agentId == null)
                {
                    log.error("Couldn't find AgentID with GUID " + guid);
                    continue;
                }
            }
            
            // find the matching geography record
            Integer geographyId = null;
            Integer geoUnitId = botanistRoleCountry.getGeoUnitId();

            if (geoUnitId != null)
            {
                String guid = String.valueOf(geoUnitId);
                String  sql = SqlUtils.getQueryIdByFieldSql("geography", "GeographyID", "GUID", guid);

                geographyId = csvToSqlMgr.queryForId(sql);

                if (geographyId == null)
                {
                    log.error("Couldn't find GeographyID with GUID " + guid);
                    continue;
                }
            }
            
            // get a converter
            BotanistRoleCountryConverter botanistRoleCountryConverter =
                BotanistRoleCountryConverter.getInstance();

            // convert BotanistRoleCountry into AgentGeography
            AgentGeography agentGeography = botanistRoleCountryConverter.convert(botanistRoleCountry);

            Agent agent = new Agent();
            agent.setAgentId(agentId);
            
            agentGeography.setAgent(agent);
            
            Geography geography = new Geography();
            geography.setGeographyId(geographyId);
            
            agentGeography.setGeography(geography);

            // convert agentgeography to sql and insert
            try {
                String sql = getInsertSql(agentGeography);
                
                csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert agentvariant record for line " + counter, e);
                continue;
            }
        }

        return counter;
    }

    protected int loadBotanistSpecialties() throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/botanist_role_specialty.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating locality objects for each via sql
        int counter = 0;
        int lastAgentId = 0;
        int orderNumber = 1;

        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            BotanistRoleSpecialty botanistRoleSpecialty = null;
            try {
                botanistRoleSpecialty = parseBotanistRoleSpecialtyRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }

            // find the matching agent record
            Integer agentId = null;
            Integer botanistId = botanistRoleSpecialty.getBotanistId();

            if (botanistId != null)
            {
                Botanist botanist = new Botanist();
                botanist.setId(botanistId);

                String guid = SqlUtils.sqlString(botanist.getGuid());

                String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

                agentId = csvToSqlMgr.queryForId(sql);

                if (agentId == null)
                {
                    log.error("Couldn't find AgentID with GUID " + guid);
                    continue;
                }
            }
            
            // get a converter
            BotanistRoleSpecialtyConverter botanistRoleSpecialtyConverter =
                BotanistRoleSpecialtyConverter.getInstance();

            // convert BotanistRoleCountry into AgentGeography
            AgentSpecialty agentSpecialty = botanistRoleSpecialtyConverter.convert(botanistRoleSpecialty);

            Agent agent = new Agent();
            agent.setAgentId(agentId);
            
            agentSpecialty.setAgent(agent);
            
            if (agentId != lastAgentId) {
                orderNumber = 1;
                lastAgentId = agentId;
            }
            else {
                orderNumber ++;
            }
            agentSpecialty.setOrderNumber(orderNumber);

            // convert agentspecialty to sql and insert
            try {
                String sql = getInsertSql(agentSpecialty);
                
                csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert agentspecialty record for line " + counter, e);
                continue;
            }
        }

        return counter;
    }
    
    protected int loadReferenceWorks() throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/reference_work.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating locality objects for each via sql
        int counter = 0;

        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            Publication publication = null;
            try {
                publication = parsePublicationRefWorkRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }
            
            // get a converter
            PublicationConverter publicationConverter = PublicationConverter.getInstance();

            // convert Publication into ReferenceWork
            ReferenceWork referenceWork = publicationConverter.convertToReferenceWork(publication);

            // convert referencework to sql and insert
            try {
                String sql = getInsertSql(referenceWork);
                
                csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert referencework record for line " + counter, e);
                continue;
            }
        }

        return counter;
    }
    
    protected int loadJournals() throws LocalException
    {
        CsvToSqlMgr csvToSqlMgr = new CsvToSqlMgr("demo_files/journal.csv");
        int records = csvToSqlMgr.countRecords();
        
        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating locality objects for each via sql
        int counter = 0;

        while (true)
        {
            counter++;

            String line = null;
            try {
                line = csvToSqlMgr.getNextLine();
            }
            catch (LocalException e) {
                log.error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            updateProgressFrame(counter);

            Publication publication = null;
            try {
                publication = parsePublicationRefWorkRecord(line);
            }
            catch (LocalException e) {
                log.error("Couldn't parse line", e);
                continue;
            }
            
            // get a converter
            PublicationConverter publicationConverter = PublicationConverter.getInstance();

            // convert Publication into a Journal
            Journal journal = publicationConverter.convertToJournal(publication);
            
            // convert journal to sql and insert
            Integer id = null;
            try {
                String sql = getInsertSql(journal);
                
                id = csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert journal record for line " + counter, e);
            }
            journal.setJournalId(id);

            // convert Publication into ReferenceWork
            ReferenceWork referenceWork = publicationConverter.convertToReferenceWork(publication);
            referenceWork.setJournal(journal);

            // convert agentspecialty to sql and insert
            try {
                String sql = getInsertSql(referenceWork);
                
                csvToSqlMgr.insert(sql);
            }
            catch (LocalException e) {
                log.error("Couldn't insert referencework record for line " + counter, e);
                continue;
            }
        }

        return counter;
    }
    
    // id, geo_unit_id, locality, latlong_method, latitude_a, longitude_a, latitude_b, longitude_b, elev_from, elev_to, elev_method
    private Site parseSiteRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
        if (columns.length < 11)
        {
            throw new LocalException("Wrong number of columns");
        }

        // assign values to Site object
        Site site = new Site();

        try {
            site.setId(Integer.parseInt(StringUtils.trimToNull(columns[0])));

            Integer geoUnitId = Integer.parseInt(StringUtils.trimToNull(columns[1]));
            site.setGeoUnitId(geoUnitId);

            site.setLocality(StringUtils.trimToNull(columns[2]));
            site.setMethod(StringUtils.trimToNull(columns[3]));

            String lat1Str = StringUtils.trimToNull(columns[4]);
            if (lat1Str != null) {
                site.setLatitudeA(BigDecimal.valueOf(Double.parseDouble(lat1Str)));
            }

            String long1Str = StringUtils.trimToNull(columns[5]);
            if (long1Str != null) {
                site.setLongitudeA(BigDecimal.valueOf(Double.parseDouble(long1Str)));
            }

            String lat2Str = StringUtils.trimToNull(columns[6]);
            if (lat2Str != null) {
                site.setLatitudeB(BigDecimal.valueOf(Double.parseDouble(lat2Str)));
            }

            String long2Str = StringUtils.trimToNull(columns[7]);
            if (long2Str != null) {
                site.setLongitudeB(BigDecimal.valueOf(Double.parseDouble(long2Str)));
            }

            String elevFromStr = StringUtils.trimToNull(columns[8]);
            if (elevFromStr != null) {
                site.setElevFrom(Integer.parseInt(elevFromStr));
            }

            String elevToStr = StringUtils.trimToNull(columns[9]);
            if (elevToStr != null) {
                site.setElevTo(Integer.parseInt(elevToStr));
            }

            site.setElevMethod(StringUtils.trimToNull(columns[10]));
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return site;
    }

    // id, isTeam, isCorporate, name, datesType, startYear, startPrecision, endYear, endPrecision, remarks
    private Botanist parseBotanistRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
        if (columns.length < 10)
        {
            throw new LocalException("Wrong number of columns");
        }

        // assign values to Botanist object
        Botanist botanist = new Botanist();
        try {
            botanist.setId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );

            String isTeamStr = StringUtils.trimToNull( columns[1] );
            boolean isTeam = isTeamStr != null && isTeamStr.equals( "true" );
            botanist.setTeam( isTeam );

            String isCorporateStr = StringUtils.trimToNull( columns[2] );
            boolean isCorporate = isCorporateStr != null && isCorporateStr.equals( "true" );
            botanist.setCorporate( isCorporate );

            String name = StringUtils.trimToNull( columns[3] );
            if (name != null)
            {
                botanist.setName( name );
            }
            else {
                throw new LocalException( "No name found in record " + line );
            }

            // no place to put this at the moment: birth/death, flourished, collected, received specimens
            String datesType = StringUtils.trimToNull( columns[4] );
            if ( datesType != null )
            {
                botanist.setDatesType( datesType );
            }

            String startYearStr = StringUtils.trimToNull( columns[5] );
            if ( startYearStr != null )
            {
                botanist.setStartYear( Integer.parseInt( startYearStr ) );
            }

            // doing nothing with this at the moment: ?, circa; null means default, exact
            String startPrecision = StringUtils.trimToNull( columns[6] );
            if ( startPrecision != null )
            {
                botanist.setStartPrecision( startPrecision );
            }

            String endYearStr = StringUtils.trimToNull( columns[7] );
            if ( endYearStr != null )
            {
                botanist.setEndYear( Integer.parseInt( endYearStr ) );
            }

            // no place to put this at the moment: ?, circa; null means default, exact
            String endPrecision = StringUtils.trimToNull( columns[8] );
            if ( endPrecision != null ) {
                botanist.setEndPrecision( startPrecision );
            }

            String remarks = StringUtils.trimToNull( columns[9] );
            if ( remarks != null )
            {
                botanist.setRemarks( remarks );
            }
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return botanist;
    }
    
    // botanistId, nameType, name
    private BotanistName parseBotanistNameRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
        if (columns.length < 3)
        {
            throw new LocalException("Wrong number of columns");
        }

        // assign values to Botanist object
        BotanistName botanistName = new BotanistName();
        
        try {
            botanistName.setBotanistId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
            
            String type = StringUtils.trimToNull( columns[1] );
            if (type == null) throw new LocalException("No type found in record " + line );

            BotanistName.TYPE nameType;

            if      (type.equals("author name")   ) nameType = BotanistName.TYPE.Author;
            else if (type.equals("author abbrev") ) nameType = BotanistName.TYPE.AuthorAbbrev;
            else if (type.equals("collector name")) nameType = BotanistName.TYPE.Collector;
            else if (type.equals("variant")       ) nameType = BotanistName.TYPE.Variant;
            
            else throw new LocalException("Unrecognized botanist name type: " + type);

            botanistName.setType(nameType);

            String name = StringUtils.trimToNull( columns[2] );
            if (name != null)
            {
                botanistName.setName( name );
            }
            else {
                throw new LocalException( "No name found in record " + line );
            }
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistName;
    }
    
    private BotanistRoleCountry parseBotanistRoleCountryRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');

        if (columns.length < 4)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        BotanistRoleCountry botanistRoleCountry = new BotanistRoleCountry();
        try {
            botanistRoleCountry.setBotanistId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
            
            String role = StringUtils.trimToNull( columns[1] );
            if (role == null) throw new LocalException("No type found in record " + line );

            botanistRoleCountry.setRole(role);

            botanistRoleCountry.setGeoUnitId(Integer.parseInt(StringUtils.trimToNull( columns[2] ) ) );
            
            botanistRoleCountry.setOrdinal(Integer.parseInt(StringUtils.trimToNull( columns[3] ) ) );
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistRoleCountry;
    }

    private BotanistRoleSpecialty parseBotanistRoleSpecialtyRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');

        if (columns.length < 4)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        BotanistRoleSpecialty botanistRoleSpecialty = new BotanistRoleSpecialty();
        try {
            botanistRoleSpecialty.setBotanistId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
            
            String role = StringUtils.trimToNull( columns[1] );
            if (role == null) throw new LocalException("No type found in record " + line );

            botanistRoleSpecialty.setRole(role);

            String specialty = StringUtils.trimToNull( columns[2] );
            botanistRoleSpecialty.setSpecialty(specialty);
            
            botanistRoleSpecialty.setOrdinal(Integer.parseInt(StringUtils.trimToNull( columns[3] ) ) );
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return botanistRoleSpecialty;
    }
    
    private Publication parsePublicationRefWorkRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');

        if (columns.length < 8)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        Publication publication = new Publication();
        try {
            publication.setId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
            
            String isbn = StringUtils.trimToNull( columns[1] );
            publication.setIsbn(isbn);

            String pubPlace = StringUtils.trimToNull( columns[2] );
            publication.setPubPlace(pubPlace);
            
            String publisher = StringUtils.trimToNull( columns[3] );
            publication.setPublisher(publisher);
            
            String url = StringUtils.trimToNull( columns[4] );
            publication.setUrl(url);
            
            String title = StringUtils.trimToNull( columns[5] );
            if (title == null) throw new LocalException("No title found in record " + line);
            publication.setTitle(title);
            
            String pubDate = StringUtils.trimToNull( columns[6] );
            publication.setPubDate(pubDate);
            
            String remarks = StringUtils.trimToNull( columns[7] );
            publication.setRemarks(remarks);
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return publication;
    }
    
    private Publication parsePublicationJournalRecord(String line) throws LocalException
    {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');

        if (columns.length < 11)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        Publication publication = new Publication();
        try {
            publication.setId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
            
            String isbn = StringUtils.trimToNull( columns[1] );
            publication.setIsbn(isbn);

            String pubPlace = StringUtils.trimToNull( columns[2] );
            publication.setPubPlace(pubPlace);
            
            String publisher = StringUtils.trimToNull( columns[3] );
            publication.setPublisher(publisher);
            
            String url = StringUtils.trimToNull( columns[4] );
            publication.setUrl(url);
            
            String title = StringUtils.trimToNull( columns[5] );
            if (title == null) throw new LocalException("No title found in record " + line);
            publication.setTitle(title);
            
            String pubDate = StringUtils.trimToNull( columns[6] );
            publication.setPubDate(pubDate);
            
            String issn = StringUtils.trimToNull( columns[7] );
            publication.setIsbn(issn);
            
            String bph = StringUtils.trimToNull( columns[8] );
            publication.setBph(bph);
            
            String abbreviation = StringUtils.trimToNull( columns[9] );
            publication.setAbbreviation(abbreviation);
            
            String remarks = StringUtils.trimToNull( columns[10] );
            publication.setRemarks(remarks);
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return publication;
    }

    private String getInsertSql(Locality locality) throws LocalException
    {
        String fieldNames =
            "ElevationMethod, GUID, LatLongMethod, Lat1Text, Lat2Text, Latitude1, Latitude2, " +
            "Long1Text, Long2Text, Longitude1, Longitude2, LocalityName, MaxElevation, " +
            "MinElevation, SrcLatLongUnit, DisciplineID, TimestampCreated, Remarks";

        List<String> values = new ArrayList<String>(18);

        values.add(SqlUtils.sqlString(locality.getElevationMethod()    ));
        values.add(SqlUtils.sqlString(locality.getGuid()               ));
        values.add(SqlUtils.sqlString(locality.getLatLongMethod()      ));
        values.add(SqlUtils.sqlString(locality.getLat1text()           ));
        values.add(SqlUtils.sqlString(locality.getLat2text()           ));
        values.add(    String.valueOf(locality.getLat1()               ));
        values.add(    String.valueOf(locality.getLat2()               ));
        values.add(SqlUtils.sqlString(locality.getLong1text()          ));
        values.add(SqlUtils.sqlString(locality.getLong2text()          ));
        values.add(    String.valueOf(locality.getLong1()              ));
        values.add(    String.valueOf(locality.getLong2()              ));
        values.add(SqlUtils.sqlString(locality.getLocalityName()       ));
        values.add(    String.valueOf(locality.getMaxElevation()       ));
        values.add(    String.valueOf(locality.getMinElevation()       ));
        values.add(    String.valueOf(locality.getSrcLatLongUnit()     ));
        values.add(    String.valueOf(locality.getDiscipline().getId() ));
        values.add("now()");
        values.add(SqlUtils.sqlString(SqlUtils.iso8859toUtf8(locality.getRemarks())));
        
        return SqlUtils.getInsertSql("locality", fieldNames, values);
    }

    private String getInsertSql(Agent agent) throws LocalException
    {
        String fieldNames = 
            "AgentType, GUID, DateOfBirth, DateOfDeath, FirstName, LastName, TimestampCreated, Remarks";

        List<String> values = new ArrayList<String>(7);
        
        values.add(    String.valueOf(agent.getAgentType()   ));
        values.add(SqlUtils.sqlString(agent.getGuid()        ));
        values.add(SqlUtils.sqlString(agent.getDateOfBirth() ));
        values.add(SqlUtils.sqlString(agent.getDateOfBirth() ));
        values.add(SqlUtils.sqlString(agent.getFirstName()   ));
        values.add(SqlUtils.sqlString(agent.getLastName()    ));
        values.add("now()" );
        values.add(SqlUtils.sqlString(SqlUtils.iso8859toUtf8( agent.getRemarks())));
    
        return SqlUtils.getInsertSql("agent", fieldNames, values);
    }
    
    private String getInsertSql(AgentVariant agentVariant)
    {
        String fieldNames = "AgentID, VarType, Name, TimestampCreated";
        
        List<String> values = new ArrayList<String>(4);
        
        values.add(    String.valueOf(agentVariant.getAgent().getId()));
        values.add(SqlUtils.sqlString(agentVariant.getVarType()      ));
        values.add(SqlUtils.sqlString(agentVariant.getName()         ));
        values.add("now()" );
        
        return SqlUtils.getInsertSql("agentvariant", fieldNames, values);
    }
    
    private String getInsertSql(AgentGeography agentGeography)
    {
        String fieldNames = "AgentId, GeographyID, Role, TimestampCreated, Remarks";
        
        List<String> values = new ArrayList<String>(5);
        
        values.add(    String.valueOf(agentGeography.getAgent().getAgentId()        ));
        values.add(    String.valueOf(agentGeography.getGeography().getGeographyId()));
        values.add(SqlUtils.sqlString(agentGeography.getRole()                      ));
        values.add("now()" );
        values.add(SqlUtils.sqlString(agentGeography.getRemarks())); 
        
        return SqlUtils.getInsertSql("agentgeography", fieldNames, values);
    }
    
    private String getInsertSql(AgentSpecialty agentSpecialty)
    {
        String fieldNames = "AgentId, SpecialtyName, OrderNumber, TimestampCreated";
        
        List<String> values = new ArrayList<String>(4);
        
        String agentId = String.valueOf(agentSpecialty.getAgent().getAgentId());

        values.add(agentId);
        values.add(SqlUtils.sqlString(agentSpecialty.getSpecialtyName()));
        values.add(SqlUtils.sqlString(agentSpecialty.getOrderNumber()  ));
        values.add("now()");
        
        return SqlUtils.getInsertSql("agentspecialty", fieldNames, values);
    }
    
    private String getInsertSql(Journal journal) throws LocalException
    {
        String fieldNames = "GUID, ISSN, JournalAbbreviation, JournalName, Text1, TimestampCreated";
        
        List<String> values = new ArrayList<String>(6);
        
        values.add(SqlUtils.sqlString(journal.getGuid()               ));
        values.add(SqlUtils.sqlString(journal.getIssn()               ));
        values.add(SqlUtils.sqlString(journal.getJournalAbbreviation()));
        values.add(SqlUtils.sqlString(journal.getJournalName()        ));
        values.add(SqlUtils.sqlString(journal.getText1()              ));
        values.add("now()");
        
        return SqlUtils.getInsertSql("journal", fieldNames, values);
    }
    
    private String getInsertSql(ReferenceWork referenceWork) throws LocalException
    {
        String fieldNames = "GUID, ISBN, PlaceOfPublication, Publisher, ReferenceWorkType, " +
        		            "Title, URL, WorkDate, JournalID, TimestampCreated, Remarks";
        
        List<String> values = new ArrayList<String>(11);
        
        values.add(SqlUtils.sqlString(referenceWork.getGuid()              ));
        values.add(SqlUtils.sqlString(referenceWork.getIsbn()              ));
        values.add(SqlUtils.sqlString(referenceWork.getPlaceOfPublication()));
        values.add(SqlUtils.sqlString(referenceWork.getPublisher()         ));
        values.add(    String.valueOf(referenceWork.getReferenceWorkType() ));
        values.add(SqlUtils.sqlString(referenceWork.getTitle()             ));
        values.add(SqlUtils.sqlString(referenceWork.getUrl()               ));
        values.add(SqlUtils.sqlString(referenceWork.getWorkDate()          ));
        
        Integer journalId = referenceWork.getJournal() != null ? referenceWork.getJournal().getId() : null;
        values.add(String.valueOf(journalId ));
        
        values.add("now()");
        values.add(SqlUtils.sqlString(SqlUtils.iso8859toUtf8(referenceWork.getRemarks())));
        
        return SqlUtils.getInsertSql("referencework", fieldNames, values);    
    }

    private void initializeProgressFrame(final int records)
    {
        if (frame != null)
        {
            final int mx = records;
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setProcess(0, mx);
                }
            });
        }
    }
    
    private void updateProgressFrame(final int counter)
    {
        if (frame != null) {
            if (counter % 100 == 0)
            {
                frame.setProcess(counter);
                log.info("Converted " + counter + " records");
            }
        }
    }
}
