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
package edu.ku.brc.specify.conversion;

import static edu.ku.brc.specify.conversion.BasicSQLUtils.query;
import static edu.ku.brc.specify.conversion.BasicSQLUtils.setFieldsToIgnoreWhenMappingNames;
import static edu.ku.brc.specify.conversion.BasicSQLUtils.setIdentityInsertOFFCommandForSQLServer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.treeutils.NodeNumberer;
import edu.ku.brc.ui.ProgressFrame;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.Pair;

public class ConvertTaxonHelper
{
    protected static final Logger            log         = Logger.getLogger(ConvertTaxonHelper.class);
    
    protected static SimpleDateFormat        dateTimeFormatter      = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    protected static SimpleDateFormat        dateFormatter          = new SimpleDateFormat("yyyy-MM-dd");
    protected static Timestamp               now                    = new Timestamp(System .currentTimeMillis());
    protected static String                  nowStr                 = dateTimeFormatter.format(now);
    
    protected Connection                     oldDBConn;
    protected Connection                     newDBConn;
    protected String                         oldDBName;
    protected TableWriter                    tblWriter;
    protected IdMapperIndexIncrementerIFace  indexIncremeter;
    protected GenericDBConversion            conversion;
    protected ProgressFrame                  frame;
    
    protected String                         taxonomyTypeIdInClause = null;
    protected String                         taxonFromClause        = null;
    
    protected Vector<CollectionInfo>         collectionInfoList;
    protected HashMap<Integer, Vector<CollectionInfo>> collDispHash;
    
    protected HashMap<Integer, TaxonTreeDef> newTaxonInfoHash = new HashMap<Integer, TaxonTreeDef>();
    
    protected HashSet<Integer>               taxonTypesInUse  = new HashSet<Integer>();
    protected HashMap<Integer, TaxonTreeDef> taxonTreeDefHash = new HashMap<Integer, TaxonTreeDef>(); // Key is old TaxonTreeTypeID
    protected HashMap<Integer, Taxon>        taxonTreeHash    = new HashMap<Integer, Taxon>();        // Key is old TaxonTreeTypeID
    
    ///////////////////////////////////////////////////////////////////
    // for TaxonName Row Processing
    ///////////////////////////////////////////////////////////////////
    protected IdMapperIFace txMapper        = null;
    protected IdMapperIFace txTypMapper     = null;
    protected IdMapperIFace txUnitTypMapper = null;
    protected IdMapperIFace[] mappers       = null;
    
    protected  String[] oldCols = {"TaxonNameID", "ParentTaxonNameID", "TaxonomyTypeID", "AcceptedID", "TaxonomicUnitTypeID", "TaxonomicSerialNumber", "TaxonName", "UnitInd1", "UnitName1", 
                        "UnitInd2", "UnitName2", "UnitInd3", "UnitName3", "UnitInd4", "UnitName4", "FullTaxonName", "CommonName", "Author", "Source", "GroupPermittedToView", 
                        "EnvironmentalProtectionStatus", "Remarks", "NodeNumber", "HighestChildNodeNumber", "LastEditedBy", "Accepted", 
                        "RankID", "GroupNumber", "TimestampCreated", "TimestampModified"};

    protected String[] cols = {"TaxonID", "Author", "CitesStatus", "COLStatus", "CommonName", "CultivarName", "EnvironmentalProtectionStatus",
                     "EsaStatus", "FullName", "GroupNumber", "GUID", "HighestChildNodeNumber", "IsAccepted", "IsHybrid", "IsisNumber", "LabelFormat", "Name", "NcbiTaxonNumber", "NodeNumber", "Number1", "Number2",
                     "RankID", "Remarks", "Source", "TaxonomicSerialNumber", "Text1", "Text2", "UnitInd1", "UnitInd2", "UnitInd3", "UnitInd4", "UnitName1", "UnitName2", "UnitName3", "UnitName4", "UsfwsCode", "Visibility",
                     "ParentID", "AcceptedID", "ModifiedByAgentID", "TaxonTreeDefItemID", "VisibilitySetByID", "CreatedByAgentID", "HybridParent1ID", "TaxonTreeDefID", "HybridParent2ID",
                     "TimestampCreated", "TimestampModified", "Version"};
    
    protected int[] colTypes = null;
    
    protected Hashtable<String, String> newToOldColMap    = new Hashtable<String, String>();
    protected Hashtable<String, String> oldToNewColMap    = new Hashtable<String, String>();
    protected HashMap<String, Integer>  fieldToColHash    = new HashMap<String, Integer>();
    protected HashMap<Integer, String>  colToFieldHash    = new HashMap<Integer, String>();
    protected HashMap<String, Integer>  oldFieldToColHash = new HashMap<String, Integer>();
    
    protected PreparedStatement pStmtTx = null;
    protected Statement         stmtTx  = null;
    
    protected int missingParentTaxonCount = 0;
    protected int lastEditedByInx;
    protected int modifiedByAgentInx;
    protected int rankIdOldDBInx;

    
    
    
    /**
     * @param oldDBConn
     * @param newDBConn
     * @param oldDBName
     * @param tblWriter
     */
    public ConvertTaxonHelper(final Connection oldDBConn, 
                              final Connection newDBConn,
                              final String     oldDBName,
                              final ProgressFrame frame,
                              final TableWriter tblWriter,
                              final GenericDBConversion conversion,
                              final IdMapperIndexIncrementerIFace indexIncremeter)
    {
        super();
        this.oldDBConn = oldDBConn;
        this.newDBConn = newDBConn;
        this.oldDBName = oldDBName;
        this.frame     = frame;
        this.tblWriter = tblWriter;
        this.indexIncremeter = indexIncremeter;
        this.conversion = conversion;
        
        
        CollectionInfo.getCollectionInfoList(oldDBConn);
        
        collectionInfoList = CollectionInfo.getFilteredCollectionInfoList();
        
        // Create a Hashed List of CollectionInfo for each unique TaxonomyTypeId
        // where the TaxonomyTypeId is a Discipline
        collDispHash = new HashMap<Integer, Vector<CollectionInfo>>();
        for (CollectionInfo info : collectionInfoList)
        {
            if (info.isTaxonomicUnitTypeInUse())
            {
                Vector<CollectionInfo> colInfoList = collDispHash.get(info.getTaxonomyTypeId());
                if (colInfoList == null)
                {
                    colInfoList = new Vector<CollectionInfo>();
                    collDispHash.put(info.getTaxonomyTypeId(), colInfoList);
                }
                colInfoList.add(info);
            }
        }
    }
    
    /**
     * 
     */
    public void doForeignKeyMappings()
    {
        
        // When you run in to this table1.field, go to that table2 and look up the id
        String[] mappings = {

                "Determination",
                "TaxonNameID",
                "TaxonName",
                "TaxonNameID",
                
                "Preparation",
                "ParasiteTaxonNameID",
                "TaxonName",
                "TaxonNameID",
                
                "Habitat",
                "HostTaxonID",
                "TaxonName",
                "TaxonNameID",
                
                "TaxonCitation",
                "ReferenceWorkID",
                "ReferenceWork",
                "ReferenceWorkID",
                
                "TaxonCitation",
                "TaxonNameID",
                "TaxonName",
                "TaxonNameID",
                
                // taxonname ID mappings
                "TaxonName", "ParentTaxonNameID", "TaxonName", "TaxonNameID", "TaxonName",
                "TaxonomicUnitTypeID", "TaxonomicUnitType", "TaxonomicUnitTypeID", "TaxonName",
                "TaxonomyTypeID", "TaxonomyType", "TaxonomyTypeID", "TaxonName", "AcceptedID",
                "TaxonName", "TaxonNameID",

                // taxonomytype ID mappings
                // NONE

                // taxonomicunittype ID mappings
                "TaxonomicUnitType", "TaxonomyTypeID", "TaxonomyType", "TaxonomyTypeID" };

        for (int i = 0; i < mappings.length; i += 4)
        {
            IdMapperMgr.getInstance().mapForeignKey(mappings[i], mappings[i + 1], mappings[i + 2], mappings[i + 3]);
        }
    }
    
    /**
     * 
     */
    public void createTaxonIdMappings()
    {
        IdMapperMgr idMapperMgr = IdMapperMgr.getInstance();
        
        // These are the names as they occur in the old datamodel
        String[] tableNames = {
                "Habitat", 
                "TaxonCitation", 
                "TaxonomicUnitType",  // Added Only 
        };
        
        int i = 0;
        IdTableMapper idMapper = null;
        for (String tableName : tableNames)
        {
            idMapper = idMapperMgr.addTableMapper(tableName, tableName + "ID");
            log.debug("mapIds() for table" + tableName);
            
            if (i < tableNames.length - 1)
            {
                idMapper.mapAllIds();
            }
            i++;
        }
        
        //---------------------------------
        // TaxonomyType
        //---------------------------------
        
        HashSet<Integer> txTypHashSet = new HashSet<Integer>();
        StringBuilder    inSB         = new StringBuilder();
        
        //HashMap<Integer, StringBuilder> txTypToKgdmHash = new HashMap<Integer, StringBuilder>();
        for (CollectionInfo ci : CollectionInfo.getFilteredCollectionInfoList())
        {
            log.debug("For Collection["+ci.getCatSeriesName()+"]  TaxonomyTypeId: "+ci.getTaxonomyTypeId() +"  "+ (txTypHashSet.contains(ci.getTaxonomyTypeId()) ? "Done" : "not Done."));
            if (!txTypHashSet.contains(ci.getTaxonomyTypeId()))
            {
                log.debug("Mapping TaxonomyTypeId ["+ci.getTaxonomyTypeId()+"]  For Collection["+ci.getCatSeriesName()+"]");
                if (inSB.length() > 0) inSB.append(',');
                inSB.append(ci.getTaxonomyTypeId());
                txTypHashSet.add(ci.getTaxonomyTypeId());
            }
        }
        
        taxonomyTypeIdInClause = " in (" + inSB.toString() + ")";
        
        idMapperMgr.addTableMapper("TaxonomyType", "TaxonomyTypeID", true);
        
        //---------------------------------
        // TaxonName
        //---------------------------------
        
        taxonFromClause = String.format(" FROM taxonname tx Inner Join taxonomicunittype tu ON tx.TaxonomicUnitTypeID = tu.TaxonomicUnitTypeID " +
        		                        "WHERE tx.TaxonomyTypeId %s ORDER BY tx.RankID", 
        		                        taxonomyTypeIdInClause);
        String sql      = "SELECT COUNT(*)" + taxonFromClause;
        log.debug(sql);
        int    count    = BasicSQLUtils.getCountAsInt(oldDBConn, sql);
        
        sql   = "SELECT tx.TaxonNameID" + taxonFromClause;
        log.debug(count+" - " + sql);
        
        // This mapping is used by Discipline
        idMapper = idMapperMgr.addTableMapper("TaxonName", "TaxonNameID", sql, false);
        idMapper.mapAllIdsWithSQL();
    }
    
    /**
     * 
     */
    protected void getTaxonTreesTypesInUse()
    {
        for (CollectionInfo colInfo : collectionInfoList)
        {
            //log.debug("TaxonType in use ["+taxonTypeId+"]");
            taxonTypesInUse.add(colInfo.getTaxonNameId());
        }
    }

    /**
     * 
     */
    public void convertAllTaxonTreeDefs()
    {
        IdMapperMgr.getInstance().get("TaxonomicUnitType", "TaxonomicUnitTypeID").reset();
        
        for (CollectionInfo colInfo : collectionInfoList)
        {
            convertTaxonTreeDefinition(colInfo);
        }
    }

    /**
     * Converts the taxonomy tree definition from the old taxonomicunittype table to the new table
     * pair: TaxonTreeDef & TaxonTreeDefItems.
     * 
     * @param taxonomyTypeId the tree def id in taxonomicunittype
     * @return the TaxonTreeDef object
     * @throws SQLException
     */
    public void convertTaxonTreeDefinition(final CollectionInfo colInfo)
    {
        if (!colInfo.isInUse())
        {
            return;
        }
        
        TaxonTreeDef taxonTreeDef = newTaxonInfoHash.get(colInfo.getTaxonNameId());
        if (taxonTreeDef != null)
        {
            colInfo.setTaxonTreeDef(taxonTreeDef);
            return;
        }
        
        Integer oldTaxonRootId = colInfo.getTaxonNameId();
        Integer taxonomyTypeId = colInfo.getTaxonomyTypeId();
        
        try
        {
            Statement st = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    
            taxonTreeDef = new TaxonTreeDef();
            taxonTreeDef.initialize();
    
            String sql = "SELECT TaxonomyTypeName, KingdomID FROM taxonomytype WHERE TaxonomyTypeID = " + taxonomyTypeId;
            log.debug(sql);
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            String taxonomyTypeName = rs.getString(1);
            int    kingdomID        = rs.getInt(2);
            rs.close();
    
            taxonTreeDef.setName(taxonomyTypeName + " taxonomy tree");
            taxonTreeDef.setRemarks("Tree converted from " + oldDBName);
            taxonTreeDef.setFullNameDirection(TreeDefIface.FORWARD);
    
            sql = String.format("SELECT RankID, RankName, RequiredParentRankID, TaxonomicUnitTypeID FROM taxonomicunittype " +
            		            "WHERE TaxonomyTypeID = %d AND (Kingdom = %d  OR RankID = 0) ORDER BY RankID", taxonomyTypeId, kingdomID);
            log.debug(sql);
            rs = st.executeQuery(sql);
    
            Hashtable<Integer, Integer> rankId2TxnUntTypId = new Hashtable<Integer, Integer>();
            int    rank;
            String name;
            int    requiredRank;
    
            Vector<TaxonTreeDefItem> items = new Vector<TaxonTreeDefItem>();
            Vector<Integer> enforcedRanks = new Vector<Integer>();
            
            while (rs.next())
            {
                rank         = rs.getInt(1);
                name         = rs.getString(2);
                requiredRank = rs.getInt(3);
                
                int taxUnitTypeId = rs.getInt(4);
                
                if (StringUtils.isEmpty(name) || (rank > 0 && requiredRank == 0))
                {
                    continue;
                }
                
                if (rankId2TxnUntTypId.get(rank) != null)
                {
                    String msg = String.format("Old TreeDef has two of the same Rank %d, throwing it out.", rank);
                    tblWriter.logError(msg);
                    log.debug(msg);
                    continue;
                }
                rankId2TxnUntTypId.put(rank, taxUnitTypeId);
                
                log.debug(rank + "  " + name+"  TaxonomicUnitTypeID: "+taxUnitTypeId);
                
                TaxonTreeDefItem ttdi = new TaxonTreeDefItem();
                ttdi.initialize();
                ttdi.setName(name);
                ttdi.setFullNameSeparator(" ");
                ttdi.setRankId(rank);
                ttdi.setTreeDef(taxonTreeDef);
                taxonTreeDef.getTreeDefItems().add(ttdi);
                
                ttdi.setIsInFullName(rank >= TaxonTreeDef.GENUS);
    
                // setup the parent/child relationship
                if (items.isEmpty())
                {
                    ttdi.setParent(null);
                } else
                {
                    ttdi.setParent(items.lastElement());
                }
                items.add(ttdi);
                enforcedRanks.add(requiredRank);
            }
            rs.close();
            
            for (TaxonTreeDefItem i : items)
            {
                i.setIsEnforced(enforcedRanks.contains(i.getRankId()));
            }
            
            try
            {
                Session     session = HibernateUtil.getNewSession();
                Transaction trans = session.beginTransaction();
                session.save(taxonTreeDef);
                trans.commit();
                session.close();
                
            } catch (Exception ex)
            {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
            
            IdMapperMgr   idMapperMgr        = IdMapperMgr.getInstance();
            IdMapperIFace tutMapper          = idMapperMgr.get("TaxonomicUnitType", "TaxonomicUnitTypeID");
            IdMapperIFace taxonomyTypeMapper = idMapperMgr.get("TaxonomyType",      "TaxonomyTypeID");
            
            //tutMapper.reset();
            
            taxonomyTypeMapper.put(taxonomyTypeId, taxonTreeDef.getId());
            
            for (TaxonTreeDefItem ttdi : taxonTreeDef.getTreeDefItems())
            {
                int ttdiId = rankId2TxnUntTypId.get(ttdi.getRankId());
                log.debug("Mapping "+ttdiId+" -> "+ttdi.getId() +"  RankId: "+ttdi.getRankId());
                tutMapper.put(ttdiId, ttdi.getId());
            }
            
            newTaxonInfoHash.put(oldTaxonRootId, taxonTreeDef);

            CollectionInfo ci = getCIByTaxonTypeId(taxonomyTypeId);
            ci.setTaxonTreeDef(taxonTreeDef);
            
            taxonTreeDefHash.put(taxonomyTypeId, taxonTreeDef);
            log.debug("Hashing taxonomyTypeId: "+taxonomyTypeId+" ->  taxonTreeDefId:"+taxonTreeDef.getId());
            
        } catch (SQLException ex)
        {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param taxonomyTypeId
     * @return
     */
    protected CollectionInfo getCIByTaxonTypeId(final int taxonomyTypeId)
    {
        for (CollectionInfo ci : CollectionInfo.getCollectionInfoList(oldDBConn))
        {
            if (ci.getTaxonomyTypeId() == taxonomyTypeId)
            {
                return ci;
            }
        } 
        log.error("Couldn't find ["+taxonomyTypeId+"] in CollectionInfo list");
        return null;
    }
    
    /** =============================================================================
     *                      Convert Taxon
     *  =============================================================================
     */
    private void convertTaxonRecords()
    {
        txMapper        = IdMapperMgr.getInstance().get("taxonname", "TaxonNameID");
        txTypMapper     = IdMapperMgr.getInstance().get("TaxonomyType", "TaxonomyTypeID");
        txUnitTypMapper = IdMapperMgr.getInstance().get("TaxonomicUnitType", "TaxonomicUnitTypeID");
        mappers         = new IdMapperIFace[] {txMapper, txMapper, txTypMapper, txMapper, txUnitTypMapper};
        
        newToOldColMap.put("TaxonID",            "TaxonNameID");
        newToOldColMap.put("ParentID",           "ParentTaxonNameID");
        newToOldColMap.put("TaxonTreeDefID",     "TaxonomyTypeID");
        newToOldColMap.put("TaxonTreeDefItemID", "TaxonomicUnitTypeID");
        newToOldColMap.put("Name",               "TaxonName");
        newToOldColMap.put("FullName",           "FullTaxonName");
        newToOldColMap.put("IsAccepted",         "Accepted");
        
        
        oldToNewColMap.put("TaxonNameID",         "TaxonID");
        oldToNewColMap.put("ParentTaxonNameID",   "ParentID");
        oldToNewColMap.put("TaxonomyTypeID",      "TaxonTreeDefID");
        oldToNewColMap.put("TaxonomicUnitTypeID", "TaxonTreeDefItemID");
        oldToNewColMap.put("TaxonName",           "Name");
        oldToNewColMap.put("FullTaxonName",       "FullName");
        oldToNewColMap.put("Accepted",            "IsAccepted");

        // Ignore new fields
        // These were added for supporting the new security model and hybrids
        /*String[] ignoredFields = { "GUID", "Visibility", "VisibilitySetBy", "IsHybrid",
                                    "HybridParent1ID", "HybridParent2ID", "EsaStatus", "CitesStatus", "UsfwsCode",
                                    "IsisNumber", "Text1", "Text2", "NcbiTaxonNumber", "Number1", "Number2",
                                    "CreatedByAgentID", "ModifiedByAgentID", "Version", "CultivarName", "LabelFormat", 
                                    "COLStatus", "VisibilitySetByID"};
        */
        
        StringBuilder newSB = new StringBuilder();
        StringBuilder vl = new StringBuilder();
        for (int i=0;i<cols.length;i++)
        {
            fieldToColHash.put(cols[i], i+1);
            colToFieldHash.put(i+1, cols[i]);
            
            if (newSB.length() > 0) newSB.append(", ");
            newSB.append(cols[i]);
            
            if (vl.length() > 0) vl.append(',');
            vl.append('?');
        }
        
        StringBuilder oldSB = new StringBuilder();
        for (int i=0;i<oldCols.length;i++)
        {
            oldFieldToColHash.put(oldCols[i], i+1);
            if (oldSB.length() > 0) oldSB.append(", ");
            oldSB.append("tx.");
            oldSB.append(oldCols[i]);
        }
        
        rankIdOldDBInx = oldFieldToColHash.get("RankID");
        
        String sqlStr = String.format("SELECT %s FROM taxon", newSB.toString());
        
        String sql = String.format("SELECT %s %s", oldSB.toString(), taxonFromClause);
        log.debug(sql);
        
        String cntSQL = String.format("SELECT COUNT(*) %s", taxonFromClause);
        log.debug(cntSQL);
        int txCnt = BasicSQLUtils.getCountAsInt(oldDBConn, cntSQL);
        if (frame != null)
        {
            frame.setProcess(0, txCnt);
        }
        
        String pStr = String.format("INSERT INTO taxon (%s) VALUES (%s)", newSB.toString(), vl.toString());
        log.debug(pStr);
        
        try
        {
            stmtTx = newDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet         rs1   = stmtTx.executeQuery(sqlStr);
            ResultSetMetaData rsmd1 = rs1.getMetaData();
            colTypes = new int[rsmd1.getColumnCount()];
            for (int i=0;i<colTypes.length;i++)
            {
                colTypes[i] = rsmd1.getColumnType(i+1); 
            }
            rs1.close();
            stmtTx.close();
            
            missingParentTaxonCount = 0;
            lastEditedByInx         = oldFieldToColHash.get("LastEditedBy");
            modifiedByAgentInx      = fieldToColHash.get("ModifiedByAgentID");
            stmtTx                  = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pStmtTx                 = newDBConn.prepareStatement(pStr);
            
            int               cnt  = 0;
            ResultSet         rs   = stmtTx.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next())
            {
                processRow(rs, rsmd, null);
                
                cnt++;
                if (cnt % 1000 == 0)
                {
                    log.debug(cnt);
                    if (frame != null)
                    {
                        frame.setProcess(cnt);
                    }
                }
            }
            rs.close();
            
            if (frame != null)
            {
                frame.setProcess(txCnt, txCnt);
            }
            
            String msg = String.format("Stranded Taxon (no parent): %d", missingParentTaxonCount);
            tblWriter.log(msg);
            log.debug(msg);
            
            if (missingParentTaxonCount > 0)
            {
                fixStrandedTaxon(oldSB);
                
                frame.setDesc("Renumbering the tree nodes, this may take a while...");
                
                HashSet<Integer> ttdHash = new HashSet<Integer>();
                for (CollectionInfo colInfo : CollectionInfo.getFilteredCollectionInfoList())
                {
                    if (!ttdHash.contains(colInfo.getTaxonTreeDef().getId()))
                    {
                        DataProviderSessionIFace session = null;
                        try
                        {
                            session = DataProviderFactory.getInstance().createSession();
                            
                            TaxonTreeDef taxonTreeDef = colInfo.getTaxonTreeDef();
                            taxonTreeDef = (TaxonTreeDef)session.getData("FROM TaxonTreeDef WHERE id = " + taxonTreeDef.getId());
                            
                            sql = "SELECT TaxonID FROM taxon WHERE RankID = 0 AND TaxonTreeDefID = " + taxonTreeDef.getId();
                            log.debug(sql);
                            Integer txRootId = BasicSQLUtils.getCount(sql);
                            Taxon   txRoot   = (Taxon)session.getData("FROM Taxon WHERE id = " + txRootId);
                            
                            NodeNumberer<Taxon,TaxonTreeDef,TaxonTreeDefItem> nodeNumberer = new NodeNumberer<Taxon,TaxonTreeDef,TaxonTreeDefItem>(txRoot.getDefinition());
                            nodeNumberer.doInBackground();
                            
                        } catch(Exception ex)
                        {
                            //session.rollback();
                            ex.printStackTrace();
                            
                        } finally
                        {
                            if (session != null)
                            {
                                session.close();
                            }
                        }
                        ttdHash.add(colInfo.getTaxonTreeDef().getId());
                    }
                }
                frame.setDesc("Renumbering done.");
            }
            missingParentTaxonCount = 0;
            
        } catch (SQLException ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                stmtTx.close();
                pStmtTx.close();
            } catch (Exception ex) {}
        }
    }

    /**
     * @param rs
     * @param rsmd
     * @param parentNodeId
     * @return
     * @throws SQLException
     */
    protected boolean processRow(final ResultSet         rs, 
                                 final ResultSetMetaData rsmd,
                                 final Integer           parentNodeId) throws SQLException
    {
        for (int colInx=1;colInx<=cols.length;colInx++)
        {
            pStmtTx.setNull(colInx, colTypes[colInx-1]);
        }
        
        Boolean isRoot = null;
        boolean skip   = false;
        for (int colInx=1;colInx<=oldCols.length && !skip;colInx++)
        {
            String  oldName = oldCols[colInx-1];
            Integer newInx  = fieldToColHash.get(oldName);
            if (newInx == null)
            {
                String newName = oldToNewColMap.get(oldName);
                if (newName != null)
                {
                    newInx = fieldToColHash.get(newName);
                    if (newInx == -1)
                    {
                        String msg = "Couldn't find column index for New Name["+newName+"]";
                        log.error(msg);
                        tblWriter.logError(msg);
                    }
                } else if (colInx == lastEditedByInx)
                {
                    String lastEditedByStr = rs.getString(colInx);
                    if (StringUtils.isNotEmpty(lastEditedByStr))
                    {
                        Integer agtId = conversion.getModifiedByAgentId(lastEditedByStr);
                        if (agtId != null)
                        {
                            pStmtTx.setInt(modifiedByAgentInx, agtId);
                            continue;
                        }
                    }
                    
                    pStmtTx.setInt(colInx, conversion.getCurAgentModifierID());
                    continue;
                    
                } else if (colInx != 20)
                {
                    String msg = "Couldn't find Old Name["+oldName+"]";
                    log.error(msg);
                    tblWriter.logError(msg);
                } else
                {
                    continue; // GroupToView
                }
            }
            
            if (colInx < 6)
            {
                if (isRoot == null)
                {
                    isRoot = rs.getInt(rankIdOldDBInx) == 0;
                }
                Integer oldID  = rs.getInt(colInx);
                if (!rs.wasNull() || (isRoot && colInx == 2))
                {
                    boolean skipError = false; 
                    Integer newID = mappers[colInx-1].get(oldID);
                    if (newID == null)
                    {
                        if (colInx == 3 || colInx == 5)
                        {
                            if (!isRoot)
                            {
                                skip = true;
                            }
                            
                        } else if (colInx == 2 && (parentNodeId != null || isRoot))
                        {
                            // Note for RankID == 0 the parent would be null because it is the root
                            newID = parentNodeId;
                            
                        } else
                        {
                            boolean wasInOldTaxonTable = BasicSQLUtils.getCountAsInt(oldDBConn, "SELECT COUNT(*) FROM taxonname WHERE TaxonNameID = " + oldID) > 0;
                            boolean isDetPointToTaxon  = BasicSQLUtils.getCountAsInt(oldDBConn, "SELECT COUNT(*) FROM determination WHERE TaxonNameID = " + oldID)  > 0;
                            if (isDetPointToTaxon)
                            {
                                String msg = String.format("***** Couldn't get %s NewID [%d] from mapper for colInx[%d] In Old taxonname table: %s  WasParentID: %s  Det Using: %s", 
                                        (colInx == 2 ? "Parent" : ""), oldID, colInx, (wasInOldTaxonTable ? "YES" : "no"), (colInx == 2 ? "YES" : "no"), (isDetPointToTaxon ? "YES" : "no"));
                                log.error(msg);
                                tblWriter.logError(msg);
                            }
                            skipError = true;
                            missingParentTaxonCount++;
                        }
                    }
                    
                    if (!skip)
                    {
                        if (newID != null)
                        {
                            //System.out.println("newInx["+newInx+"]  newID["+newID+"] oldID["+oldID+"]");
                            pStmtTx.setInt(newInx, newID);
                            
                    } else if (!skipError && !isRoot)
                        {
                            String msg = "Unable to map old TaxonNameID["+oldID+"]";
                            log.error(msg);
                            tblWriter.logError(msg);
                        }
                    }
                } else
                {
                    //log.error("***** Old ID Col ["+colInx+"] was null");
                    //skip = true;
                }
                continue;
            }
            
            switch (colTypes[newInx-1])
            {
                case java.sql.Types.BIT:
                {
                    boolean val = rs.getBoolean(colInx);
                    if (!rs.wasNull()) pStmtTx.setBoolean(newInx, val);
                    break;
                }
                case java.sql.Types.INTEGER:
                {
                    int val = rs.getInt(colInx);
                    if (!rs.wasNull()) pStmtTx.setInt(newInx, val);
                    
                    //System.out.println("newInx["+colInx+"]  newID["+val+"]");
                    break;
                }
                case java.sql.Types.SMALLINT:
                {
                    short val = rs.getShort(colInx);
                    if (!rs.wasNull()) pStmtTx.setShort(newInx, val);
                    break;
                }
                case java.sql.Types.TIMESTAMP:
                {
                    Timestamp val = rs.getTimestamp(colInx);
                    pStmtTx.setTimestamp(newInx, !rs.wasNull() ? val : now);
                    break;
                }
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.VARCHAR:
                {
                    String val = rs.getString(colInx);
                    if (!rs.wasNull()) 
                    {
                        pStmtTx.setString(newInx, val);
                        
                    } else if (colInx == 7)
                    {
                        pStmtTx.setString(newInx, "Empty");
                    }
                    break;
                }
                default:
                    log.error("Didn't support SQL Type: "+rsmd.getColumnType(colInx));
                    break;
            }
            
        }
        
        if (!skip)
        {
            if (parentNodeId != null)
            {
                int nxtId = BasicSQLUtils.getCountAsInt("SELECT TaxonID FROM taxon ORDER BY TaxonID DESC LIMIT 0,1") + 1;
                pStmtTx.setInt(1, nxtId);
            }
            
            pStmtTx.setInt(fieldToColHash.get("Version"), 0);
            try
            {
                //System.out.println("----------------------------------------");
                pStmtTx.execute();
                
            } catch (Exception ex)
            {
                UIRegistry.showError(ex.toString());
            }
        }

        return true;
    }
    
    /**
     * @param colDBColumns
     */
    private void fixStrandedTaxon(final StringBuilder colDBColumns)
    {
        String fromClause = String.format(" FROM taxonname tx LEFT JOIN taxonname t2 ON tx.ParentTaxonNameID = t2.TaxonNameID " +
                                          "Inner Join taxonomicunittype tu ON tx.TaxonomicUnitTypeID = tu.TaxonomicUnitTypeID " +
                                          "WHERE t2.TaxonNameID IS NULL AND tx.RankID IS NOT NULL AND tx.RankID > 0 AND tx.TaxonomyTypeID %s", 
                                          taxonomyTypeIdInClause);
        
        String sql = "SELECT COUNT(*)" + fromClause;
        int numStrandedTaxon = BasicSQLUtils.getCountAsInt(oldDBConn, sql);
        if (numStrandedTaxon > 0)
        {
            if (frame != null)
            {
                frame.setDesc(String.format("Fixing %d stranded Taxon records", numStrandedTaxon));
                frame.setProcess(0, numStrandedTaxon);
            }
            
            // process stranded rows
            String sqlStr = String.format("SELECT %s %s", colDBColumns.toString(), fromClause);
            log.debug(sqlStr);
            
            int rankIdInx       = oldFieldToColHash.get("RankID");
            //int parentIdInx     = oldFieldToColHash.get("ParentTaxonNameID");
            int taxonomyTypeInx = oldFieldToColHash.get("TaxonomyTypeID");
            
            try
            {
                int               cnt  = 0;
                ResultSet         rs   = stmtTx.executeQuery(sqlStr);
                ResultSetMetaData rsmd = rs.getMetaData();
                while (rs.next())
                {
                    int taxonomyTypeId = rs.getInt(taxonomyTypeInx);
                    
                    CollectionInfo colInfo = getCIByTaxonTypeId(taxonomyTypeId);
                    if (colInfo != null)
                    {
                        int              rankId       = rs.getInt(rankIdInx);
                        Integer          parentRankId = colInfo.getRankParentHash().get(rankId);
                        if (parentRankId != null)
                        {
                            TaxonTreeDefItem item = colInfo.getTreeDefItemHash().get(rankId); 
                            if (item != null)
                            {
                                Taxon taxonParent = colInfo.getPlaceHolderTreeHash().get(parentRankId);
                                if (taxonParent != null)
                                {
                                    processRow(rs, rsmd, taxonParent.getId());

                                } else
                                {
                                    log.error("Taxon PlaceHolder parent was missing for RankId: "+rankId);
                                }
                            } else
                            {
                                log.error("TaxonTreeDefItem was missing for RankId: "+rankId);
                            }
                        } else
                        {
                            log.error("No Parent RankID mapping for RankId: "+rankId);
                        }
                    } else
                    {
                        log.error("Couldn't find CollectionInfo for taxonomyTypeId: "+taxonomyTypeId);
                    }
                    cnt++;
                    if (frame != null)
                    {
                        frame.setProcess(cnt);
                    }
                }
                rs.close();
                if (frame != null)
                {
                    frame.setProcess(numStrandedTaxon);
                }

            } catch (SQLException ex)
            {
                ex.printStackTrace();
            }
        }
    }
    
    /**
    * 
    */
   private void convertTaxonTreeDefSeparators()
   {
       // fix the fullNameDirection field in each of the converted tree defs
       Session session = HibernateUtil.getCurrentSession();
       Query q = session.createQuery("FROM TaxonTreeDef");
       List<?> allTTDs = q.list();
       HibernateUtil.beginTransaction();
       for(Object o: allTTDs)
       {
           TaxonTreeDef ttd = (TaxonTreeDef)o;
           ttd.setFullNameDirection(TreeDefIface.FORWARD);
           session.saveOrUpdate(ttd);
       }
       try
       {
           HibernateUtil.commitTransaction();
       }
       catch(Exception ex)
       {
           log.error("Error while setting the fullname direction of taxonomy tree definitions.");
           ex.printStackTrace();
           throw new RuntimeException(ex);
       }
       
       // fix the fullNameSeparator field in each of the converted tree def items
       session = HibernateUtil.getCurrentSession();
       q = session.createQuery("FROM TaxonTreeDefItem");
       List<?> allTTDIs = q.list();
       HibernateUtil.beginTransaction();
       for(Object o : allTTDIs)
       {
           TaxonTreeDefItem ttdi = (TaxonTreeDefItem)o;
           ttdi.setFullNameSeparator(" ");
           session.saveOrUpdate(ttdi);
       }
       try
       {
           HibernateUtil.commitTransaction();
       }
       catch (Exception ex)
       {
           log.error("Error while setting the fullname separator of taxonomy tree definition items.");
           ex.printStackTrace();
           throw new RuntimeException(ex);
       }
   }
   
   

    /**
     * @param disciplineId
     * @param taxonRootId
     * @return
     */
    public Pair<TaxonTreeDef, Discipline> doTaxonForCollection(final int disciplineId, final int taxonRootId)
    {
        Pair<TaxonTreeDef, Discipline> dataForColInfo = null;
        
        int           newTaxonRootID = txMapper.get(taxonRootId);
        int taxonTreeDefId = BasicSQLUtils.getCountAsInt(newDBConn, "SELECT TaxonTreeDefID FROM taxon WHERE TaxonID = " + newTaxonRootID);
        
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            
            TaxonTreeDef ttd        = session.get(TaxonTreeDef.class, taxonTreeDefId);
            Discipline   discipline = (Discipline)session.getData("FROM Discipline WHERE id = " + disciplineId);
            session.beginTransaction();
            discipline.setTaxonTreeDef(ttd);
            session.saveOrUpdate(discipline);
            session.commit();
            
            dataForColInfo = new Pair<TaxonTreeDef, Discipline>(ttd, discipline);
                
        } catch(Exception ex)
        {
            session.rollback();
            
            log.error("Error while setting TaxonTreeDef into the Discipline.");
            ex.printStackTrace();
            throw new RuntimeException(ex);
            
        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
        
        tblWriter.append("<H3>Taxon with null RankIDs</H3>");
        tblWriter.startTable();
        String missingRankSQL = "SELECT * FROM taxonname WHERE RankID IS NULL";
        Vector<Object[]> rows = query(oldDBConn, missingRankSQL);
        for (Object[] row : rows)
        {
            tblWriter.append("<TR>");
            for (Object obj : row)
            {
                tblWriter.append("<TD>");
                tblWriter.append(obj != null ? obj.toString() : "null");
                tblWriter.append("</TD>");
            }
            tblWriter.append("</TR>");
        }
        tblWriter.endTable();
        tblWriter.append("<BR>");
        
        if (txMapper instanceof IdHashMapper)
        {
            for (Integer oldId : ((IdHashMapper)txMapper).getOldIdNullList())
            {
                tblWriter.println(ConvertVerifier.dumpSQL(oldDBConn, "SELECT * FROM taxonname WHERE ParentTaxonNameID = "+oldId));
            }
        }

        setFieldsToIgnoreWhenMappingNames(null);
        setIdentityInsertOFFCommandForSQLServer(newDBConn, "taxon", BasicSQLUtils.myDestinationServerType);
        
        IdHashMapper.setTblWriter(null);
        
        return dataForColInfo;
    }

    /**
     * 
     */
    public void convertTaxonCitationToTaxonImage()
    {
        String sql      = "SELECT tn.TaxonNameID, c.Text1 ";
        String fromStr  = " FROM taxonname AS tn Inner Join taxoncitation AS c ON tn.TaxonNameID = c.TaxonNameID";
        String whereStr = " WHERE c.Text1 IS NOT NULL";
        
        String updateStr = "UPDATE taxon SET GUID=? WHERE TaxonID = ?";

        int numTaxCit = BasicSQLUtils.getCountAsInt(oldDBConn, "SELECT COUNT(*) " + fromStr + whereStr);
        if (numTaxCit > 0)
        {
            if (frame != null)
            {
                frame.setDesc(String.format("Fixing Taxon Citations", numTaxCit));
                frame.setProcess(0, numTaxCit);
            }
            
            // process stranded rows
            String sqlStr = sql + fromStr + whereStr;
            log.debug(sqlStr);
            
            Statement         stmt  = null;
            PreparedStatement pStmt = null;
            try
            {
                stmt  = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                pStmt = newDBConn.prepareStatement(updateStr);
                
                int       cnt  = 0;
                ResultSet rs   = stmt.executeQuery(sqlStr);
                while (rs.next())
                {
                    int     oldTaxonId = rs.getInt(1);
                    Integer newTaxonID = txMapper.get(oldTaxonId);
                    if (newTaxonID != null)
                    {
                        String imgURL  = rs.getString(2);
                        
                        pStmt.setString(1, imgURL);
                        pStmt.setInt(2, newTaxonID);
                        
                        if (pStmt.executeUpdate() != 1)
                        {
                            String msg = String.format("Unable to update new taxonID %d with image url[%s].", newTaxonID, imgURL);
                            log.error(msg);
                            tblWriter.logError(msg);
                        }
                                               
                        cnt++;
                        if (frame != null)
                        {
                            frame.setProcess(cnt);
                        }
                        
                    } else
                    {
                        String msg = String.format("Unable to map old id [%d] to new taxonID.", oldTaxonId);
                        log.error(msg);
                        tblWriter.logError(msg);
                    }
                }
                rs.close();
                
                if (frame != null)
                {
                    frame.setProcess(numTaxCit);
                }

            } catch (SQLException ex)
            {
                ex.printStackTrace();
            } finally
            {
                try
                {
                    if (stmt != null) stmt.close();
                    if (pStmt != null) pStmt.close();
                } catch (Exception ex) {}
            }
        }
    }

    /**
     * 
     */
//    private void assignTreeDefToDiscipline()
//    {
//        DataProviderSessionIFace session = null;
//        try
//        {
//            session = DataProviderFactory.getInstance().createSession();
//            
//            for (Integer txTypeId : collDispHash.keySet())
//            {
//                Vector<CollectionInfo> collInfoList = collDispHash.get(txTypeId);
//                Integer                disciplineId = collInfoList.get(0).getDisciplineId();
//                if (disciplineId != null)
//                {
//                    TaxonTreeDef txnTreeDef = taxonTreeDefHash.get(txTypeId);
//                    String sql = "UPDATE discipline SET TaxonTreeDefID=" + txnTreeDef.getTaxonTreeDefId() + " WHERE DisciplineID = " + disciplineId;
//                    if (BasicSQLUtils.update(newDBConn, sql) != 1)
//                    {
//                        log.error("Error updating discipline["+disciplineId+"] with TaxonTreeDefID "+ txnTreeDef.getTaxonTreeDefId());
//                    } else
//                    {
//                        /*Discipline discipline = collInfoList.get(0).getDiscipline();
//                        if (discipline == null)
//                        {
//                            log.error("Error updating discipline["+collInfoList.get(0).getDisciplineId()+"] with TaxonTreeDefID "+ collInfoList.get(0).getTaxonTreeDef().getTaxonTreeDefId());
//                            continue;
//                        }
//                        
//                        discipline = session.load(Discipline.class, discipline.getId());
//                        for (CollectionInfo ci : collInfoList)
//                        {
//                            ci.setDiscipline(discipline);
//                        }*/
//                    }
//                } else
//                {
//                    log.error("Missing Discipline #");
//                }
//            }
//        } catch (Exception ex)
//        {
//            ex.printStackTrace();
//        } finally
//        {
//            if (session != null)
//            {
//                session.close();
//            }
//        }
//    }
    
    /**
     * 
     */
    public void doConvert()
    {
        getTaxonTreesTypesInUse();
        
        convertAllTaxonTreeDefs();
        convertTaxonTreeDefSeparators();
        
        convertTaxonRecords(); // converts all the taxon records
        
        HashMap<Integer, Integer> dispTxnRootHash = new HashMap<Integer, Integer>();
        for (CollectionInfo colInfo : collectionInfoList)
        {
            Integer txnRootId = dispTxnRootHash.get(colInfo.getDisciplineId());
            if (txnRootId == null)
            {
                dispTxnRootHash.put(colInfo.getDisciplineId(), colInfo.getTaxonNameId());
                
            } else if (!txnRootId.equals(colInfo.getTaxonNameId()))
            {
                UIRegistry.showError("Two (or more) Disciplines have different Taxon Root Id records.  Dsp["+colInfo.getDisciplineId()+"]  Prev RootId["+txnRootId+"] New RootId["+colInfo.getTaxonNameId()+"]");
            }
        }
        
        for (Integer dispId : dispTxnRootHash.keySet())
        {
            Pair<TaxonTreeDef, Discipline> dispTxn = doTaxonForCollection(dispId, dispTxnRootHash.get(dispId));
            if (dispTxn != null)
            {
                for (CollectionInfo colInfo : collectionInfoList)
                {
                    if (colInfo.getDisciplineId().equals(dispTxn.second.getId()))
                    {
                        colInfo.setTaxonTreeDef(dispTxn.first);
                    }
                }
            }
        }
        
        //assignTreeDefToDiscipline();
    }
    
}
