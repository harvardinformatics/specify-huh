/* Copyright (C) 2009, University of Kansas Center for Research
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
package edu.ku.brc.af.core.db;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import edu.ku.brc.specify.conversion.BasicSQLUtils;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Created Date: Sep 2, 2008
 *
 */
public abstract class BackupServiceFactory
{
    public static final String factoryName = "edu.ku.brc.af.core.db.BackupServiceFactory"; //$NON-NLS-1$
    
    
    //private static final Logger log = Logger.getLogger(SecurityMgr.class);
    
    protected static BackupServiceFactory instance = null;
    
    /**
     * Protected Constructor
     */
    protected BackupServiceFactory()
    {
        
    }
    
    /**
     * Returns the instance to the singleton
     * @return  the instance to the singleton
     */
    public static BackupServiceFactory getInstance()
    {
        if (instance != null)
        {
            return instance;
        }
        
        // else
        String factoryNameStr = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(factoryName);
                    }
                });
            
        if (isNotEmpty(factoryNameStr)) 
        {
            try 
            {
                return instance = (BackupServiceFactory)Class.forName(factoryNameStr).newInstance();
                 
            } catch (Exception e) 
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(BackupServiceFactory.class, e);
                InternalError error = new InternalError("Can't instantiate RecordSet factory " + factoryNameStr); //$NON-NLS-1$
                error.initCause(e);
                throw error;
            }
        }
        return null;
    }
    
    public abstract int getNumberofTables();
   
    /**
     * @return
     */
    public abstract void doBackUp();

    /**
     * @return
     */
    public abstract void doRestore();
    
    /**
     * @param doSendAppExit whether it should send an applications exit command
     * @return true if it is doing a backup
     */
    public abstract boolean checkForBackUp(boolean doSendAppExit);
    
    /**
     * @return a list of the table names in the database
     */
    public abstract Vector<String> getTableNames();
    
    /**
     * @param restoreName
     * @return
     */
    protected String getStatsName(final String restoreName)
    {
        String baseFileName = FilenameUtils.getBaseName(restoreName);
        String path         = FilenameUtils.getFullPath(restoreName);
        return FilenameUtils.concat(path, baseFileName+".stats");
    }
    
    /**
     * @param backFileName
     * @param newBackupName
     * @return
     */
    public Vector<Object[]> doCompare(final Vector<String> tableNames, final String restoreFilePath)
    {
        String xmlName = getStatsName(restoreFilePath);
        File   xmlFile = new File(xmlName);
        
        Properties oldStats = new Properties();
        if (xmlFile.exists())
        {
            oldStats = readStats(xmlName);
        }
        
        Properties newStats = getCollectionStats(tableNames);
        if (oldStats != null && oldStats.size() > 0 && newStats != null && newStats.size() > 0)
        {
            return compareStats(tableNames, oldStats, newStats);
        }
        
        return null;   
    }
    
    /**
     * @return
     */
    protected Properties getCollectionStats(final Vector<String> tableNames)
    {
        Properties stats = new Properties();
        for (String tableName : tableNames)
        {
            int count = BasicSQLUtils.getCount("SELECT COUNT(*) FROM "+tableName);
            stats.put(tableName, Integer.toString(count));
        }
        return stats;
    }
    
    /**
     * @param fullName
     * @return
     */
    protected Properties readStats(final String fullName)
    {
        if (StringUtils.isNotEmpty(fullName))
        {
            Properties properties = new Properties();
            
            if ((new File(fullName)).exists())
            {
                try
                {
                    properties.load(new FileInputStream(fullName));
                    return properties;
                    
                } catch (IOException ex)
                {
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(BackupServiceFactory.class, ex);
                    throw new RuntimeException(ex);
                }
            }
        }
        return null;
    }

    /**
     * @param stats
     * @param fullName
     * @return
     */
    protected boolean writeStats(final Properties stats, final String fullName)
    {
        if (StringUtils.isNotEmpty(fullName))
        {
            try
            {
                
                stats.store(new FileOutputStream(fullName), "DB TableStatistics"); //$NON-NLS-1$
                return true;
                
            } catch (IOException ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(BackupServiceFactory.class, ex);
                ex.printStackTrace();
                //throw new BackingStoreException(ex);
            }
        }
        return false;
    }
    
    /**
     * @param newStats
     * @param oldStats
     * @return
     */
    protected Vector<Object[]> compareStats(final Vector<String> tablesNames,
                                            final Properties oldStats, 
                                            final Properties newStats)
    {
        
        Vector<Object[]> compareStatsList = new Vector<Object[]>();
        for (String tableName : tablesNames)
        {
            String newCountStr = newStats.getProperty(tableName);
            String oldCountStr = oldStats.getProperty(tableName);
            if (newCountStr != null && oldCountStr != null)
            {
                int oldCnt = Integer.parseInt(oldCountStr);
                int newCnt = Integer.parseInt(newCountStr);
                int diff = newCnt - oldCnt;
                if (diff > 0)
                {
                    DBTableInfo ti = DBTableIdMgr.getInstance().getInfoByTableName(tableName);
                    Object[] row = new Object[4];
                    row[0] = ti != null ? ti.getTitle() : tableName;
                    row[1] = oldCnt;
                    row[2] = newCnt;
                    row[3] = diff;
                    compareStatsList.add(row);
                }
            }
        }
        return compareStatsList;
    }
}


