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

import edu.harvard.huh.asa.Site;
import edu.harvard.huh.asa2specify.SiteConverter;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DatabaseDriverInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Locality;
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
                    frame.setDesc("Loading localities ....");
                    frame.setOverall(steps++);
                }
            });
            
            Discipline discipline = null;
            loadLocalities(discipline);

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
    private int loadLocalities(Discipline discipline)
    {
        
        // set up a database connection for direct sql
        Connection conn = null;
        Statement  stmt = null;
        
        try {
            conn = DBConnection.getInstance().createConnection();
            stmt = conn.createStatement();
        }
        catch (SQLException e)
        {
            log.error("Couldn't create database connection");
            return 0;
        }

        // get file to load from
        File file = new File("demo_files/sites.csv");
        if (file == null || !file.exists())
        {
            log.error("Couldn't file[" + file.getAbsolutePath() + "]");
            return 0;
        }
        
        // count the lines in the file and set up an iterator for them
        LineIterator lines = null;
        int lastLine = 0;
        try
        {
            lines = FileUtils.lineIterator(file);
            
            while (lines.hasNext()) {
                lastLine++;
                lines.nextLine();
            }
            
            LineIterator.closeQuietly(lines);
            
            lines = FileUtils.lineIterator(file);
            
        } catch (IOException e)
        {
            log.error(e);
        }

        
        // initialize progress frame
        if (frame != null)
        {
            final int mx = lastLine;
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setProcess(0, mx);
                }
            });
        }

        // iterate over lines, creating locality objects for each via sql
        int counter = 0;

        while (lines.hasNext()) {
            if (frame != null) {
                if (counter % 100 == 0)
                {
                    frame.setProcess(counter);
                    log.info("Converted " + counter + " records");
                }
            }
            try {
                // read values for next record
                String line = lines.nextLine();
                counter++;
                
                loadLocalityRecord(line, stmt, srcLatLongUnit, discipline);
            }
            catch (Exception e)
            {
                log.info(e.getMessage()); // skip that line
            }
        }
        
        try {
            conn.close();
        }
        catch (SQLException e)
        {
            log.error("Couldn't close connection");
        }
        LineIterator.closeQuietly(lines);
        
        return counter;
    }
    
    private void loadLocalityRecord(String line, Statement stmt, byte srcLatLongUnit, Discipline discipline) throws SQLException
    {
        // id, geo_unit_id, locality, latlong_method, latitude_a, longitude_a, latitude_b, longitude_b, elev_from, elev_to, elev_method
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
        if (columns.length < 11)
        {
            log.error("Skipping[" + line + "]");
            return;
        }

        // assign values to Site object
        Site site = new Site();
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

        // skip this record if it only has SRE initial values set
        if (! site.hasData())
        {
            return;
        }
        
        // get a converter
        SiteConverter siteConverter = SiteConverter.getInstance();
        
        // convert site into locality ...
        Locality locality = siteConverter.convert(site);
        locality.setSrcLatLongUnit(srcLatLongUnit);
        locality.setDiscipline(discipline);
        
        // insert new locality
        String sql = siteConverter.getInsertSql(locality);
        stmt.executeUpdate(sql);

        // get new locality id
        Integer newId = BasicSQLUtils.getInsertedId(stmt);
        if (newId == null)
        {
            throw new RuntimeException("Couldn't get the Locality's inserted ID");
        }
        
        // update locality with geography
        if (geoUnitId != null)
        {
            int geographyId = 0;

            sql = "select GeographyID from geography where GUID=\"" + geoUnitId + "\"";
            ResultSet result = stmt.executeQuery(sql);

            if (! result.next()) {
                log.warn("Didn't find referenced geography (id " + geoUnitId + ")");
                return;
            }

            geographyId = result.getInt(1);

            if (geographyId > 0)
            {
                sql = "update locality set GeographyID=" + geographyId + " where LocalityID=" + newId;
                int success = stmt.executeUpdate(sql);
                if (success != 1)
                {
                    log.error("Couldn't set geography for [" + line + "]");
                }
            }
        }
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
    
}
