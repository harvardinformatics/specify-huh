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
/**
 * 
 */
package edu.ku.brc.specify.conversion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Created Date: Oct 14, 2009
 *
 */
public class ConvScopeFixer
{
    protected static final Logger log = Logger.getLogger(ConvScopeFixer.class);
    
    protected static final int NONE    = 0;
    protected static final int BORROWS = 1;
    
    
    protected static SimpleDateFormat        dateTimeFormatter      = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    protected static SimpleDateFormat        dateFormatter          = new SimpleDateFormat("yyyy-MM-dd");
    protected static Timestamp               now                    = new Timestamp(System .currentTimeMillis());
    protected static String                  nowStr                 = dateTimeFormatter.format(now);
    
    protected Connection                     oldDBConn;
    protected Connection                     newDBConn;
    protected String                         oldDBName;
    protected TableWriter   tblWriter;
    
    protected HashMap<Integer, Integer>      taxonTypesInUse = new HashMap<Integer, Integer>();
    protected HashMap<Integer, TaxonTreeDef> taxonTreeDefHash = new HashMap<Integer, TaxonTreeDef>(); // Key is old TaxonTreeTypeID
    
    protected HashMap<Integer, Integer>      colObjTypeToCollMemId = new HashMap<Integer, Integer>();
    protected HashMap<Integer, Integer>      catSerTypeToCollMemId = new HashMap<Integer, Integer>();
    protected HashMap<Integer, Integer>      colObjTypeCount       = new HashMap<Integer, Integer>();

    
    String[] collectionMembers = new String[] {
            
        "BorrowAgent", 
        "Borrow", 
        "BorrowMaterial",
        "BorrowReturnMaterial",
        
        "CollectingEventAttribute",  // Done
        
        "CollectionObjectAttribute", // Done
        "CollectionObjectCitation",  // Done 
        "CollectionObject",          // Done
        
        "Collector", 
        
        "DeterminationCitation",     // Done
        "Determination",             // Done
        
        "OtherIdentifier",           // Done
        
        "PaleoContext",              // Done
        
        "PreparationAttribute",      // Done
        "Preparation",               // Done
         
        "Project",                   // Done
    };
    
    String[] disciplineMembers = new String[] {
            "CollectingEvent", 
            "CollectingTrip", 
            "Gift",
            "GiftAgent",
            "GiftPreparation", 
            "Loan",
            "LoanAgent",
            "LoanPreparation", 
            "LoanReturnPreparation", 
            "Locality", 
            "LocalityCitation", 
            "Shipment", 
            };
    /**
     * @param oldDBConn
     * @param newDBConn
     * @param tblWriter
     */
    public ConvScopeFixer(final Connection oldDBConn, 
                          final Connection newDBConn,
                          final String     oldDBName,
                          final TableWriter tblWriter)
    {
        super();
        this.oldDBConn = oldDBConn;
        this.newDBConn = newDBConn;
        this.oldDBName = oldDBName;
        this.tblWriter = tblWriter;
        
        for (CollectionInfo ci : CollectionInfo.getFilteredCollectionInfoList())
        {
            colObjTypeToCollMemId.put(ci.getColObjTypeId(), ci.getCollectionId());
            
            Integer cnt = colObjTypeCount.get(ci.getColObjTypeId());
            if (cnt == null)
            {
                colObjTypeCount.put(ci.getColObjTypeId(), 1);
            } else
            {
                colObjTypeCount.put(ci.getColObjTypeId(), ++cnt);
            }
        }
        
        tblWriter.log("Collection Object Type Counts");
        for (Integer key : colObjTypeCount.keySet())
        {
            tblWriter.log(key+" -> " + colObjTypeCount.get(key));   
        }
        tblWriter.log("");
        
        tblWriter.log("CatSeries to Collection Id");
        for (CollectionInfo ci : CollectionInfo.getFilteredCollectionInfoList())
        {
            if (ci.getCatSeriesId() != null)
            {
                catSerTypeToCollMemId.put(ci.getCatSeriesId(), ci.getCollectionId());
                tblWriter.log(String.format("Cat Series: %d  -> CollectionId %d", ci.getCatSeriesId(), ci.getCollectionId()));
            }
        }
    }
    
    /**
     * @param oldDBConn
     * @return
     */
    public boolean checkForScopingConverters()
    {
        boolean isError = false;
        
        int cnt = BasicSQLUtils.getCountAsInt(oldDBConn, "SELECT COUNT(*) FROM borrow");
        if (cnt > 0)
        {
            UIRegistry.showError("There are %d Borrows and we don't have a converter for them.");
            isError = true;
        }
        
        return isError;
    }
    
    /**
     * @return
     */
    protected boolean fixCollectingEventAttributes()
    {
        String sql = "SELECT ce.DisciplineID, cea.CollectingEventAttributeID FROM collectingevent AS ce " +
                     "Inner Join collectingeventattribute AS cea ON ce.CollectingEventAttributeID = cea.CollectingEventAttributeID " +    
                     "WHERE ce.DisciplineID <> cea.DisciplineID";    
        
        Statement         stmt  = null;
        PreparedStatement pStmt = null;
        try
        {
            stmt  = newDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pStmt = newDBConn.prepareStatement("UPDATE collectingeventattribute SET DisciplineID=? WHERE CollectingEventAttributeID=?");
            log.debug(sql);
            ResultSet rs = stmt.executeQuery(sql);
            int count = 0;
            while (rs.next())
            {
                String msg = null;
                pStmt.setInt(1, rs.getInt(1));
                pStmt.setInt(2, rs.getInt(2));
                if (pStmt.executeUpdate() != 1)
                {
                    msg = String.format("Error updating CollectingEventAttributeID %d for Discipline %d", rs.getInt(2), rs.getInt(1));
                }
                
                if (msg != null)
                {
                    log.error(msg);
                    tblWriter.logError(msg);
                }
                
                count++;
            }
            rs.close();  
            
            log.debug("fixCollectingEventAttributes - Fixed: "+count);
            
            return true;
            

        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                if (stmt != null) stmt.close();
                if (pStmt != null) pStmt.close();
            } catch (SQLException ex){}
        }
        return false;
    }
    
    /**
     * @return
     */
    protected boolean fixCollectingObjectAttributesAfterConv()
    {
        String sql = "SELECT co.CollectionMemberID, coa.CollectionObjectAttributeID FROM collectionobject AS co " +
                     "Inner Join collectionobjectattribute AS coa ON co.CollectionObjectAttributeID = coa.CollectionObjectAttributeID " +    
                     "WHERE co.CollectionMemberID <> coa.CollectionMemberID"; 
        
        Statement         stmt  = null;
        PreparedStatement pStmt = null;
        try
        {
            stmt  = newDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pStmt = newDBConn.prepareStatement("UPDATE collectionobjectattribute SET CollectionMemberID=? WHERE CollectionObjectAttributeID=?");
            log.debug(sql);
            ResultSet rs = stmt.executeQuery(sql);
            int count = 0;
            while (rs.next())
            {
                String msg = null;
                pStmt.setInt(1, rs.getInt(1));
                pStmt.setInt(2, rs.getInt(2));
                if (pStmt.executeUpdate() != 1)
                {
                    msg = String.format("Error updating CollectionObjectAttributeID %d for CollectionMemberID %d", rs.getInt(2), rs.getInt(1));
                }
                
                if (msg != null)
                {
                    log.error(msg);
                    tblWriter.logError(msg);
                }
                
                count++;
            }
            rs.close();  
            
            log.debug("fixCollectingObjectAttributesAfterConv - Fixed: "+count);
            
            return true;
            

        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                if (stmt != null) stmt.close();
                if (pStmt != null) pStmt.close();
            } catch (SQLException ex){}
        }
        return false;
    }
    

    /**
     * @return
     */
    protected boolean fixCollectingEventAttributesHabitat()
    {
        int cnt = BasicSQLUtils.getCountAsInt(oldDBConn, "SELECT COUNT(*) FROM habitat");
        if (cnt == 0)
        {
           return true;
        }

        IdMapperIFace habitatMapper = IdMapperMgr.getInstance().get("Habitat", "HabitatID");
        if (habitatMapper == null)
        {
            habitatMapper = IdMapperMgr.getInstance().addTableMapper("Habitat", "HabitatID", false);
            if (habitatMapper == null || habitatMapper.size() == 0)
            {
                log.error("habitatMapper is null");
                return false;
            }
        }

        Statement         stmt  = null;
        PreparedStatement pStmt = null;
        try
        {
            stmt  = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pStmt = newDBConn.prepareStatement("UPDATE collectingeventattribute SET CollectionMemberID=? WHERE CollectingEventAttributeID=?");
            
            String sql = "SELECT h.HabitatID,cco.CatalogSeriesID FROM habitat AS h " +
                         "Inner Join collectionobject AS co ON h.HabitatID = co.CollectingEventID " +
                         "Inner Join collectionobjectcatalog AS cco ON co.CollectionObjectID = cco.CollectionObjectCatalogID ORDER BY h.HabitatID ASC";
            
            log.debug(sql);
            ResultSet rs = stmt.executeQuery(sql);
            int count = 0;
            while (rs.next())
            {
                String  msg   = null;
                Integer oldId = rs.getInt(1);
                
                if (rs.wasNull()) continue;
                
                Integer newId = habitatMapper.get(oldId);
                if (newId != null)
                {
                    Integer colMemId = catSerTypeToCollMemId.get(rs.getInt(2));
                    if (colMemId != null)
                    {
                        try
                        {
                            pStmt.setInt(1, colMemId);
                            pStmt.setInt(2, newId);
                            if (pStmt.executeUpdate() != 1)
                            {
                                msg = String.format("Error updating CollectingEventAttributeID %d for HabitatID %d", newId, rs.getInt(1));
                            }
                        } catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    } else
                    {
                        msg = String.format("The BiologicalObjectTypeCollectedID %d wasn't mapped to CollectionMemberID for HabitatID %d", rs.getInt(2), rs.getInt(1));
                    }
                } else
                {
                    msg = String.format("The old HabitatID %d wasn't mapped.", rs.getInt(1));
                }
                
                if (msg != null)
                {
                    log.error(msg);
                    tblWriter.logError(msg);
                }
                
                count++;
            }
            rs.close();
            
            
            // NOTE: In Specify5 CollectingEventID is a one-to-one with HabitatID
            sql = "SELECT T1.CollectingEventID, T1.BiologicalObjectTypeCollectedID " +
                  "FROM (select * from collectingevent WHERE CollectingEventID in (select HabitatID from habitat)) T1 " +
                  "Left Join collectionobject ON T1.CollectingEventID = collectionobject.CollectingEventID " +
                  "WHERE collectionobject.CollectionObjectID IS NULL";
            rs = stmt.executeQuery(sql);
            count = 0;
            while (rs.next())
            {
                String msg = null;
                Integer newId = habitatMapper.get(rs.getInt(1));
                if (newId != null)
                {
                    Integer colObjTypeCnt = colObjTypeCount.get(rs.getInt(2));
                    if (colObjTypeCnt != null && colObjTypeCnt == 1)
                    {
                        Integer colMemId = colObjTypeToCollMemId.get(rs.getInt(2));
                        if (colMemId != null)
                        {
                            pStmt.setInt(1, colMemId);
                            pStmt.setInt(2, newId);
                            if (pStmt.executeUpdate() != 1)
                            {
                                msg = String.format("Error updating CollectingEventAttributeID %d for HabitatID %d", newId, rs.getInt(1));
                            }
                        } else
                        {
                            msg = String.format("The BiologicalObjectTypeCollectedID %d wasn't mapped to CollectionMemberID for HabitatID %d", rs.getInt(2), rs.getInt(1));
                        }
                    } else
                    {
                        msg = String.format("The BiologicalObjectTypeCollectedID %d has more than one collection and wasn't mapped to CollectionMemberID for HabitatID %d", rs.getInt(2), rs.getInt(1));
                    }
                } else
                {
                    msg = String.format("The old HabitatID %d wasn't mapped.", rs.getInt(1));
                }
                
                if (msg != null)
                {
                    log.error(msg);
                    tblWriter.logError(msg);
                }
                
                count++;
            }
            rs.close();  
            
            return true;
            

        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                if (stmt != null) stmt.close();
                if (pStmt != null) pStmt.close();
            } catch (SQLException ex){}
        }
        return false;
    }
    
    /**
     * @return
     */
    protected boolean fixDeterminations()
    {
        String cntSQL = "SELECT COUNT(*) FROM determination";
        
        String qrySQL = "SELECT d.DeterminationID, cc.CatalogSeriesID " +
                         "FROM determination AS d " +
                         "Inner Join collectionobjectcatalog AS cc ON d.BiologicalObjectID = cc.CollectionObjectCatalogID";
                
        return fixTableWithColMemId(cntSQL, qrySQL, "Determination", "DeterminationID", "Determination", "DeterminationID");
    }
    
    /**
     * @return
     */
    protected boolean fixDeterminationCitations()
    {
        String cntSQL = "SELECT COUNT(*) FROM determinationcitation";
        
        String qrySQL = "SELECT dc.DeterminationCitationID, cc.CatalogSeriesID FROM determination AS d " +
                         "Inner Join collectionobjectcatalog AS cc ON d.BiologicalObjectID = cc.CollectionObjectCatalogID " +
                         "Inner Join determinationcitation AS dc ON d.DeterminationID = dc.DeterminationID";
                
        return fixTableWithColMemId(cntSQL, qrySQL, "DeterminationCitation", "DeterminationCitationID", "DeterminationCitation", "DeterminationCitationID");
    }
    
    /**
     * @return
     */
    protected boolean fixCollectionObjects()
    {
        String cntSQL = "SELECT COUNT(*) FROM collectionobjectcatalog WHERE CollectionObjectTypeID > 8 AND CollectionObjectTypeID < 20";
                
        String qrySQL = "SELECT CollectionObjectCatalogID, CatalogSeriesID FROM collectionobjectcatalog WHERE CollectionObjectTypeID > 8 AND CollectionObjectTypeID < 20";
                
        return fixTableWithColMemId(cntSQL, qrySQL, "CollectionObject", "CollectionObjectID", "CollectionObject", "CollectionObjectID", "collectionobjectcatalog_CollectionObjectCatalogID");
    }
    
    /**
     * @return
     */
    protected boolean fixCollectionObjectCitations()
    {
        String cntSQL = "SELECT COUNT(cit.CollectionObjectCitationID) FROM collectionobjectcatalog AS c " + 
                        "Inner Join collectionobjectcitation AS cit ON c.CollectionObjectCatalogID = cit.BiologicalObjectID " + 
                        "WHERE c.CollectionObjectTypeID > 8 AND c.CollectionObjectTypeID < 20";
            
        String qrySQL = "SELECT cit.CollectionObjectCitationID, c.CatalogSeriesID FROM collectionobjectcatalog AS c " + 
                        "Inner Join collectionobjectcitation AS cit ON c.CollectionObjectCatalogID = cit.BiologicalObjectID " + 
                        "WHERE c.CollectionObjectTypeID > 8 AND c.CollectionObjectTypeID < 20";
            
        return fixTableWithColMemId(cntSQL, qrySQL, "CollectionObjectCitation", "CollectionObjectCitationID", "CollectionObjectCitation", "CollectionObjectCitationID");
    }

    /**
     * @return
     */
    protected boolean fixCollectionObjectAttributes()
    {
        String cntSQL = "SELECT COUNT(b.BiologicalObjectAttributesID) FROM collectionobjectcatalog AS cc " + 
                        "Inner Join biologicalobjectattributes AS b ON cc.CollectionObjectCatalogID = b.BiologicalObjectTypeID " + 
                        "WHERE cc.CollectionObjectTypeID > 8 AND cc.CollectionObjectTypeID < 20";
        
        String qrySQL = "SELECT b.BiologicalObjectAttributesID, cc.CatalogSeriesID FROM collectionobjectcatalog AS cc " + 
                        "Inner Join biologicalobjectattributes AS b ON cc.CollectionObjectCatalogID = b.BiologicalObjectTypeID " + 
                        "WHERE cc.CollectionObjectTypeID > 8 AND cc.CollectionObjectTypeID < 20";
        
        return fixTableWithColMemId(cntSQL, qrySQL, "BiologicalObjectAttributes", "BiologicalObjectAttributesID", "CollectionObjectAttribute", "CollectionObjectAttributeID");
    }
    
    /**
     * @return
     */
    protected boolean fixPrepartions()
    {
        String cntSQL = "SELECT COUNT(co.CollectionObjectID) FROM collectionobject AS co Inner Join collectionobjectcatalog AS cc ON co.CollectionObjectID = cc.CollectionObjectCatalogID " + 
                        "WHERE co.CollectionObjectTypeID < 9 OR co.CollectionObjectTypeID > 19";
        
        String qrySQL = "SELECT co.CollectionObjectID, cc.CatalogSeriesID FROM collectionobject AS co Inner Join collectionobjectcatalog AS cc ON co.CollectionObjectID = cc.CollectionObjectCatalogID " + 
                        "WHERE co.CollectionObjectTypeID < 9 OR co.CollectionObjectTypeID > 19";
        
        return fixTableWithColMemId(cntSQL, qrySQL, "Preparation", "CollectionObjectID", "Preparation", "PreparationID", "collectionobject_CollectionObjectID");
    }
    
    /**
     * @return
     */
    protected boolean fixPrepartionAttributes()
    {
        String cntSQL = "SELECT COUNT(b.BiologicalObjectAttributesID) FROM collectionobjectcatalog AS cc " + 
                        "Inner Join biologicalobjectattributes AS b ON cc.CollectionObjectCatalogID = b.BiologicalObjectTypeID " + 
                        "WHERE cc.CollectionObjectTypeID >  8 AND cc.CollectionObjectTypeID <  20";
        
        String qrySQL = "SELECT b.BiologicalObjectAttributesID, cc.CatalogSeriesID FROM collectionobjectcatalog AS cc " + 
                        "Inner Join biologicalobjectattributes AS b ON cc.CollectionObjectCatalogID = b.BiologicalObjectTypeID " + 
                        "WHERE cc.CollectionObjectTypeID >  8 AND cc.CollectionObjectTypeID <  20";
        
        return fixTableWithColMemId(cntSQL, qrySQL, "BiologicalObjectAttributes", "BiologicalObjectTypeID", "CollectionObjectAttribute", "CollectionObjectAttributeID");
    }
    
    /**
     * @return
     */
    protected boolean fixOtherIdentifiers()
    {
        String cntSQL = "SELECT COUNT(oi.OtherIdentifierID) FROM otheridentifier AS oi " + 
                        "Inner Join collectionobjectcatalog AS cc ON oi.CollectionObjectID = cc.CollectionObjectCatalogID";
        
        String qrySQL = "SELECT oi.OtherIdentifierID, cc.CatalogSeriesID FROM otheridentifier AS oi " + 
                        "Inner Join collectionobjectcatalog AS cc ON oi.CollectionObjectID = cc.CollectionObjectCatalogID";
        
        return fixTableWithColMemId(cntSQL, qrySQL, "OtherIdentifier", "OtherIdentifierID", "OtherIdentifier", "OtherIdentifierID");
    }
    
    /**
     * @return
     */
    protected boolean fixProjects()
    {
        String cntSQL = "SELECT DISTINCT p.ProjectID, cc.CatalogSeriesID FROM projectcollectionobjects AS p " + 
                        "Inner Join collectionobjectcatalog AS cc ON p.CollectionObjectID = cc.CollectionObjectCatalogID";
        
        String qrySQL = "SELECT DISTINCT p.ProjectID, cc.CatalogSeriesID FROM projectcollectionobjects AS p " + 
                        "Inner Join collectionobjectcatalog AS cc ON p.CollectionObjectID = cc.CollectionObjectCatalogID";
        
        return fixTableWithColMemId(cntSQL, qrySQL, "Project", "ProjectID", "Project", "ProjectID");
    }
    
    /**
     * @return
     */
    protected boolean fixPaleoContext()
    {
        
        String cntSQL = "SELECT COUNT(PaleoContextID) FROM collectionobject WHERE PaleoContextID IS NOT NULL";
        
        int cnt = BasicSQLUtils.getCountAsInt(newDBConn, cntSQL);
        if (cnt == 0)
        {
           return true;
        }

        String qrySQL = "SELECT PaleoContextID, CollectionMemberID FROM collectionobject WHERE PaleoContextID IS NOT NULL";
        
        Statement         stmt  = null;
        PreparedStatement pStmt = null;
        try
        {
            stmt  = newDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pStmt = newDBConn.prepareStatement("UPDATE paleocontext SET CollectionMemberID=? WHERE PaleoContextID=?");
            
            ResultSet rs = stmt.executeQuery(qrySQL);
            int count = 0;
            while (rs.next())
            {
                String msg = null;
                pStmt.setInt(1, rs.getInt(2));
                pStmt.setInt(2, rs.getInt(1));
                if (pStmt.executeUpdate() != 1)
                {
                    msg = String.format("Error updating PaleoContextID %d", rs.getInt(1));
                }
                
                if (msg != null)
                {
                    log.error(msg);
                    tblWriter.logError(msg);
                }
                
                count++;
            }
            rs.close();
            
            tblWriter.log(String.format("Updated %d records in table PaleoContext", count));
            return true;
            

        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                if (stmt != null) stmt.close();
                if (pStmt != null) pStmt.close();
            } catch (SQLException ex){}
        }
        return false;
    }
    
    public void checkTables()
    {
        tblWriter.startTable();
        tblWriter.logHdr("Table", "Field", "Count");
        
        String[] fields = new String[] {"CollectionMemberID", "DisciplineID", "DivisionID", "CollectionID"};
        
        for (String field : fields)
        {
            for (DBTableInfo ti : DBTableIdMgr.getInstance().getTables())
            {
                if (ti.getFieldByColumnName(field) != null)
                {
                    Integer cnt = BasicSQLUtils.getCount("SELECT COUNT(*) FROM "+ti.getName()+" WHERE "+field+" IS NULL");
                    if (cnt != null && cnt > 0)
                    {
                        tblWriter.log(field, ti.getName(), cnt.toString());
                    }
                }
            }
        }
        
        tblWriter.endTable();
    }
    
    /**
     * @return true on success
     */
    public boolean doFixTables()
    {
        int cnt = 0;
        
        if (fixCollectingObjectAttributesAfterConv()) cnt++;
        /*if (fixCollectingEventAttributes()) cnt++;
        //if (fixCollectingEventAttributesHabitat()) cnt++;
        if (fixCollectionObjectAttributes()) cnt++;
        if (fixCollectionObjectCitations()) cnt++;
        if (fixCollectionObjects()) cnt++;
        
        if (fixDeterminationCitations()) cnt++;
        if (fixDeterminations()) cnt++;
        if (fixOtherIdentifiers()) cnt++;
        if (fixPrepartionAttributes()) cnt++;
        if (fixPrepartions()) cnt++;
        if (fixPaleoContext()) cnt++;
        
        if (fixProjects()) cnt++;
        */
        return cnt == 0;
    }

    /**
     * @param cntSQL the SQL that counts how many records
     * @param qrySQL the SQL that gets all the records (must have two columns)
     * @param oldClassName the Sp5 table Name
     * @param oldIdName the Sp5 primary key Column name
     * @param newClassName the Sp6 Class Name
     * @param newIdName the Sp6 primary key column name
     * @return true on success
     */
    protected boolean fixTableWithColMemId(final String cntSQL, 
                                           final String qrySQL, 
                                           final String oldClassName, 
                                           final String oldIdName, 
                                           final String newClassName, 
                                           final String newIdName)
    {
        return fixTable(cntSQL, qrySQL, oldClassName, oldIdName, newClassName, newIdName, "CollectionMemberID", null);
    }

    /**
     * @param cntSQL the SQL that counts how many records
     * @param qrySQL the SQL that gets all the records (must have two columns)
     * @param oldClassName the Sp5 table Name
     * @param oldIdName the Sp5 primary key Column name
     * @param newClassName the Sp6 Class Name
     * @param newIdName the Sp6 primary key column name
     * @param mapperName the name of the mapper to use
     * @return
     */
    protected boolean fixTableWithColMemId(final String cntSQL, 
                                           final String qrySQL, 
                                           final String oldClassName, 
                                           final String oldIdName, 
                                           final String newClassName, 
                                           final String newIdName, 
                                           final String mapperName)
    {
        return fixTable(cntSQL, qrySQL, oldClassName, oldIdName, newClassName, newIdName, "CollectionMemberID", mapperName);
    }

    /**
     * @param cntSQL the SQL that counts how many records
     * @param qrySQL the SQL that gets all the records (must have two columns)
     * @param oldClassName the Sp5 table Name
     * @param oldIdName the Sp5 primary key Column name
     * @param newClassName the Sp6 Class Name
     * @param newIdName the Sp6 primary key column name
     * @param mapperName the name of the mapper to use
     * @param mapperSQL the SQL to use to fill the mapper
     * @return
     */
    protected boolean fixTableWithColMemId(final String cntSQL, 
                                           final String qrySQL, 
                                           final String oldClassName, 
                                           final String oldIdName, 
                                           final String newClassName, 
                                           final String newIdName, 
                                           final String mapperName, 
                                           final String mapperSQL)
    {
        return fixTable(cntSQL, qrySQL, oldClassName, oldIdName, newClassName, newIdName, "CollectionMemberID", mapperName, mapperSQL);
    }

    /**
     * @param cntSQL the SQL that counts how many records
     * @param qrySQL the SQL that gets all the records (must have two columns)
     * @param oldClassName the Sp5 table Name
     * @param oldIdName the Sp5 primary key Column name
     * @param newClassName the Sp6 Class Name
     * @param newIdName the Sp6 primary key column name
     * @return true on success
     */
    protected boolean fixTableWithDisciplineId(final String cntSQL, 
                                               final String qrySQL, 
                                               final String oldClassName, 
                                               final String oldIdName, 
                                               final String newClassName, 
                                               final String newIdName)
    {
        return fixTable(cntSQL, qrySQL, oldClassName, oldIdName, newClassName, newIdName, "DisciplineID", null);
    }

    /**
     * @param cntSQL the SQL that counts how many records
     * @param qrySQL the SQL that gets all the records (must have two columns)
     * @param oldClassName the Sp5 table Name
     * @param oldIdName the Sp5 primary key Column name
     * @param newClassName the Sp6 Class Name
     * @param newIdName the Sp6 primary key column name
     * @param colToFix the column in the Sp6 database that needs fixing
     * @param mapperName the name of the mapper
     * @return true on success
     */
    protected boolean fixTable(final String cntSQL, 
                               final String qrySQL, 
                               final String oldClassName, 
                               final String oldIdName, 
                               final String newClassName, 
                               final String newIdName,
                               final String colToFix,
                               final String mapperName)
    {
        return fixTable(cntSQL, qrySQL, oldClassName, oldIdName, newClassName, newIdName, colToFix, mapperName, null);
    }

    /**
     * @param cntSQL the SQL that counts how many records
     * @param qrySQL the SQL that gets all the records (must have two columns)
     * @param oldClassName the Sp5 Class Name
     * @param oldIdName the Sp5 primary key Column name
     * @param newClassName the Sp6 Class Name
     * @param newIdName the Sp6 primary key column name
     * @param colToFix the column in the Sp6 database that needs fixing
     * @param mapperName the name of the mapper
     * @param mapperSQL the SQL to fill the mapper
     * @return true on success
     */
    protected boolean fixTable(final String cntSQL, 
                               final String qrySQL, 
                               final String oldClassName, 
                               final String oldIdName, 
                               final String newClassName, 
                               final String newIdName,
                               final String colToFix,
                               final String mapperName,
                               final String mapperSQL)
    {
        int cnt = BasicSQLUtils.getCountAsInt(oldDBConn, cntSQL);
        if (cnt == 0)
        {
           return true;
        }

        String newIdFieldName = newIdName == null ? oldIdName : newIdName;
            
        IdMapperIFace idMapper = mapperName == null ? IdMapperMgr.getInstance().get(oldClassName, oldIdName) : IdMapperMgr.getInstance().get(mapperName);
        if (idMapper == null)
        {
            idMapper = IdMapperMgr.getInstance().addTableMapper(oldClassName, oldIdName, mapperSQL, false);
            if (idMapper == null || idMapper.size() == 0)
            {
                log.error("**** No Mapper for["+oldClassName+"_"+oldIdName+"]");
                return false;
            }
            
        }

        Statement         stmt  = null;
        PreparedStatement pStmt = null;
        try
        {
            stmt  = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            
            String pStr = String.format("UPDATE %s SET %s=? WHERE %s=?", newClassName.toLowerCase(), colToFix, newIdFieldName);
            log.debug(pStr);
            
            pStmt = newDBConn.prepareStatement(pStr);
            
            log.debug(qrySQL);
            
            ResultSet rs = stmt.executeQuery(qrySQL);
            int count = 0;
            while (rs.next())
            {
                String   msg  = null;
                Integer recId = rs.getInt(1);
                if (recId != null)
                {
                    Integer newId = idMapper.get(recId);
                    if (newId != null)
                    {
                        Integer colMemId = catSerTypeToCollMemId.get(rs.getInt(2));
                        if (colMemId != null)
                        {
                            try
                            {
                                pStmt.setInt(1, colMemId);
                                pStmt.setInt(2, newId);
                                int upCnt = pStmt.executeUpdate();
                                if (upCnt != 1)
                                {
                                    msg = String.format("Error updating %s for Old %s with new ID %d", colToFix, oldIdName, newId);
                                }
                            } catch (Exception ex)
                            {
                                ex.printStackTrace();
                            }
                            
                        } else
                        {
                            msg = String.format("The CatalogSeriesID %d wasn't mapped to %s for Old %s %d", rs.getInt(2), colToFix, oldIdName, rs.getInt(1));
                        }
                    } else
                    {
                        msg = String.format("The old %s ID: %d wasn't mapped.", oldIdName, rs.getInt(1));
                    }
                } else
                {
                    msg = String.format("The old %s ID: is NULL", oldIdName);
                }
                
                if (msg != null)
                {
                    log.error(msg);
                    tblWriter.logError(msg);
                }
                
                count++;
            }
            rs.close();
            
            tblWriter.log(String.format("Updated %d records in table %s", count, oldClassName));
            
            return true;
            

        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                if (stmt != null) stmt.close();
                if (pStmt != null) pStmt.close();
            } catch (SQLException ex){}
        }
        return false;
    }


    
    protected boolean fixBorrows()
    {
        int cnt = BasicSQLUtils.getCountAsInt("SELECT COUNT(*) FROM borrow");
        if (cnt > 0)
        {
            UIRegistry.showError("There are %d Borrows and we don't have a converter for them.");
        }
        
        return false;
    }
   

}
