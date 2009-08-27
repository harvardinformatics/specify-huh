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
package edu.ku.brc.specify.conversion;

import static edu.ku.brc.specify.conversion.BasicSQLUtils.buildSelectFieldList;
import static edu.ku.brc.specify.conversion.BasicSQLUtils.getFieldNamesFromSchema;
import static edu.ku.brc.ui.UIRegistry.showError;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.dbsupport.HibernateUtil;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.util.Pair;
import edu.ku.brc.util.Triple;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Jun 11, 2009
 *
 */
public class AgentConverter
{
    
    private enum ParseAgentType {LastNameOnlyLF,  // Last Name Field only with names in Last Name then First Name order
                                 LastNameOnlyFL,  // Last Name Field only with names in First Name then Last Name order
                                 LastThenFirstLF, // The first Last Name is in the First Name Field the rest of the data is in the Last Name field and the order is Last Name, Comma first Name
    }
    protected static final Logger                           log                    = Logger.getLogger(AgentConverter.class);
    protected static Integer nextAddressId = 0;

    protected IdMapperMgr                                   idMapperMgr;

    protected Connection                                    oldDBConn;
    protected Connection                                    newDBConn;
    protected static boolean                                shouldCreateMapTables  = true;
    protected GenericDBConversion                           conv;
    
    protected Statement                                     gStmt;
    protected Statement                                     updateStmtNewDB;
    
    protected Hashtable<Integer, AgentInfo>                 agentHash  = new Hashtable<Integer, AgentInfo>();
    protected Pair<String, String>                          namePair   = new Pair<String, String>();
    protected Triple<String, String, String>                nameTriple = new Triple<String, String, String>();

    protected ConversionLogger.TableWriter                  tblWriter;
    
    // For Name Parsing
    protected List<AgentNameInfo>                           names    = new Vector<AgentNameInfo>();
    protected Stack<AgentNameInfo>                          recycler = new Stack<AgentNameInfo>();

    /**
     * @param conv
     * @param idMapperMgr
     * @param shouldCreateMapTables
     * @throws SQLException 
     */
    public AgentConverter(final GenericDBConversion conv,
                          final IdMapperMgr idMapperMgr,
                          final boolean shouldCreateMapTables) throws SQLException
    {
        super();
        this.conv        = conv;
        this.idMapperMgr = idMapperMgr;
        this.oldDBConn   = conv.getOldDBConn();
        this.newDBConn   = conv.getNewDBConn();
        
        this.gStmt       = newDBConn.createStatement();
        this.updateStmtNewDB = newDBConn.createStatement();
        
        tblWriter = conv.getConvLogger().getWriter("AgentConv.html", "Agents");
    }


    /**
     * Specify 5.x points at AgentAdress instead of an Agent. The idea was that to point at an Agent
     * and possibly a differnt address that represents what that person does. This was really
     * confusing so we are changing it to point at an Agent instead.
     * 
     * So that means we need to pull apart these relationships and have all foreign keys that point
     * to an AgentAddress now point at an Agent and we then need to add in the Agents and then add
     * the Address to the Agents.
     * 
     * The AgentAdress, Agent and Address (triple) can have a NULL Address but it cannot have a NULL
     * Agent. If there is a NULL Agent then this method will throw a RuntimeException.
     */
    public boolean convertAgents(final boolean doFixAgents)
    {
        boolean debugAgents = false;

        log.debug("convert Agents");

        BasicSQLUtils.removeForeignKeyConstraints(newDBConn, BasicSQLUtils.myDestinationServerType);

        // Create the mappers here, but fill them in during the AgentAddress Process
        IdTableMapper agentIDMapper     = idMapperMgr.addTableMapper("agent",        "AgentID");
        IdTableMapper addrIDMapper      = idMapperMgr.addTableMapper("address",      "AddressID");
        IdTableMapper agentAddrIDMapper = idMapperMgr.addTableMapper("agentaddress", "AgentAddressID");

        agentIDMapper.setInitialIndex(4);

        if (shouldCreateMapTables)
        {
            log.info("Mapping Agent Ids");
            agentIDMapper.mapAllIds("select AgentID from agent order by AgentID");

            log.info("Mapping Address Ids");
            addrIDMapper.mapAllIds("select AddressID from address order by AddressID");
        }
        
        //createCollectorsTable();

        // Just like in the conversion of the CollectionObjects we
        // need to build up our own select clause because the MetaData of columns names returned
        // from
        // a query doesn't include the table names for all columns, this is far more predictable
        List<String> oldFieldNames = new ArrayList<String>();

        StringBuilder sql = new StringBuilder("select ");
        log.debug(sql);
        List<String> agentAddrFieldNames = new ArrayList<String>();
        getFieldNamesFromSchema(oldDBConn, "agentaddress", agentAddrFieldNames, BasicSQLUtils.mySourceServerType);
        sql.append(buildSelectFieldList(agentAddrFieldNames, "agentaddress"));
        sql.append(", ");
        GenericDBConversion.addNamesWithTableName(oldFieldNames, agentAddrFieldNames, "agentaddress");

        List<String> agentFieldNames = new ArrayList<String>();
        getFieldNamesFromSchema(oldDBConn, "agent", agentFieldNames, BasicSQLUtils.mySourceServerType);
        sql.append(buildSelectFieldList(agentFieldNames, "agent"));
        log.debug(sql);
        sql.append(", ");
        GenericDBConversion.addNamesWithTableName(oldFieldNames, agentFieldNames, "agent");

        List<String> addrFieldNames = new ArrayList<String>();
        getFieldNamesFromSchema(oldDBConn, "address", addrFieldNames, BasicSQLUtils.mySourceServerType);
        log.debug(sql);
        sql.append(buildSelectFieldList(addrFieldNames, "address"));
        GenericDBConversion.addNamesWithTableName(oldFieldNames, addrFieldNames, "address");

        // Create a Map from the full table/fieldname to the index in the resultset (start at 1 not
        // zero)
        Hashtable<String, Integer> indexFromNameMap = new Hashtable<String, Integer>();

        sql.append(" From agent Inner Join agentaddress ON agentaddress.AgentID = agent.AgentID Inner Join address ON agentaddress.AddressID = address.AddressID Order By agentaddress.AgentAddressID Asc");

        // These represent the New columns of Agent Table
        // So the order of the names are for the new table
        // the names reference the old table
        String[] agentColumns = { "agent.AgentID", "agent.TimestampModified", "agent.AgentType",
                                  "agentaddress.JobTitle", "agent.FirstName", "agent.LastName",
                                  "agent.MiddleInitial", "agent.Title", "agent.Interests", "agent.Abbreviation",
                                  "agentaddress.Email", "agentaddress.URL", "agent.Remarks",
                                  "agent.TimestampCreated",// User/Security changes
                                  "agent.ParentOrganizationID" };

        // See comments for agent Columns
        String[] addressColumns = { "address.AddressID", "address.TimestampModified",
                                    "address.Address", "address.Address2", "address.City", "address.State",
                                    "address.Country", "address.Postalcode", "address.Remarks",
                                    "address.TimestampCreated", "agentaddress.IsCurrent", "agentaddress.Phone1",
                                    "agentaddress.Phone2", "agentaddress.Fax", "agentaddress.RoomOrBuilding",
                                    "address.AgentID" };

        Hashtable<Integer, AddressInfo> addressHash = new Hashtable<Integer, AddressInfo>();

        // Create a Hashtable to track which IDs have been handled during the conversion process
        try
        {
            log.info("Hashing Address Ids");

            Integer agentCnt = BasicSQLUtils.getCount(oldDBConn, "select count(AddressID) from address order by AddressID");
            
            // So first we hash each AddressID and the value is set to 0 (false)
            Statement stmtX = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rsX   = stmtX.executeQuery("select AddressID from address order by AddressID");

            conv.setProcess(0, agentCnt);
            
            int cnt = 0;
            // Needed to add in case AgentAddress table wasn't used.
            while (rsX.next())
            {
                int addrId = rsX.getInt(1);
                addressHash.put(addrId, new AddressInfo(addrId, addrIDMapper.get(addrId)));

                if (cnt % 100 == 0)
                {
                    conv.setProcess(0, cnt);
                }
                cnt++;
            }
            rsX.close();
            stmtX.close();

            conv.setProcess(0, 0);

            // Next we hash all the Agents and set their values to 0 (false)
            log.info("Hashing Agent Ids");
            stmtX    = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            agentCnt = BasicSQLUtils.getCount(oldDBConn, "select count(*) from agent order by AgentID");
            rsX      = stmtX.executeQuery("select AgentID, AgentType, LastName, Name, FirstName from agent order by AgentID");

            conv.setProcess(0, agentCnt);
            
            cnt = 0;
            while (rsX.next())
            {
                int agentId = rsX.getInt(1);
                agentHash.put(agentId, new AgentInfo(agentId, agentIDMapper.get(agentId), rsX.getByte(2), rsX.getString(3), rsX.getString(4), rsX.getString(5)));
                if (cnt % 100 == 0)
                {
                    conv.setProcess(0, cnt);
                }
                cnt++;
            }

            rsX.close();
            stmtX.close();

            conv.setProcess(0, 0);

            // Now we map all the Agents to their Addresses AND
            // All the Addresses to their Agents.
            //
            // NOTE: A single Address Record Mat be used by more than one Agent so
            // we will need to Duplicate the Address records later
            //
            log.info("Cross Mapping Agents and Addresses");

            agentCnt = BasicSQLUtils.getCount(oldDBConn, "SELECT count(AgentAddressID) FROM agentaddress a where AddressID is not null and AgentID is not null");
            stmtX    = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rsX      = stmtX.executeQuery("SELECT AgentAddressID, AddressID, AgentID FROM agentaddress a where AddressID is not null and AgentID is not null");

            conv.setProcess(0, agentCnt);
            cnt = 0;
            // Needed to add in case AgentAddress table wasn't used.
            while (rsX.next())
            {
                int agentAddrId = rsX.getInt(1);
                int addrId      = rsX.getInt(2);
                int agentId     = rsX.getInt(3);

                // ///////////////////////
                // Add Address to Agent
                // ///////////////////////
                AgentInfo agentInfo = agentHash.get(agentId);
                if (agentInfo == null) 
                { 
                    throw new RuntimeException("The AgentID [" + agentId + "]in AgentAddress table id[" + agentAddrId + "] desn't exist");
                }
                agentInfo.getAddrs().put(addrId, true);

                AddressInfo addrInfo = addressHash.get(addrId);
                if (addrInfo == null) 
                { 
                    throw new RuntimeException("The AddressID [" + addrId + "] in AgentAddress table id[" + agentAddrId + "] desn't exist");
                }
                agentInfo.getAddrs().put(addrId, true);

                if (cnt % 100 == 0)
                {
                    conv.setProcess(0, cnt);
                }
                cnt++;
            }
            rsX.close();
            stmtX.close();

            conv.setProcess(0, 0);

            // It OK if the address is NULL, but the Agent CANNOT be NULL
            log.info("Checking for null Agents");

            agentCnt = BasicSQLUtils.getCount(oldDBConn, "SELECT count(AgentAddressID) FROM agentaddress a where AddressID is not null and AgentID is null");
            // If there is a Single Record With a NULL Agent this would be BAD!
            if (agentCnt != null && agentCnt > 0)
            {
                showError("There are "+agentCnt+" AgentAddress Records where the AgentID is null and the AddressId is not null!");
            }

            nextAddressId = BasicSQLUtils.getNumRecords(oldDBConn, "address") + 1;

            // ////////////////////////////////////////////////////////////////////////////////
            // This does the part of AgentAddress where it has both an Address AND an Agent
            // ////////////////////////////////////////////////////////////////////////////////

            log.info(sql.toString());

            // Example of the Query
            //
            // select agentaddress.AgentAddressID, agentaddress.TypeOfAgentAddressed,
            // agentaddress.AddressID, agentaddress.AgentID, agentaddress.OrganizationID,
            // agentaddress.JobTitle, agentaddress.Phone1, agentaddress.Phone2, agentaddress.Fax,
            // agentaddress.RoomOrBuilding, agentaddress.Email, agentaddress.URL,
            // agentaddress.Remarks, agentaddress.TimestampModified, agentaddress.TimestampCreated,
            // agentaddress.LastEditedBy, agentaddress.IsCurrent, agent.AgentID,
            // agent.AgentType, agent.FirstName, agent.LastName, agent.MiddleInitial, agent.Title,
            // agent.Interests, agent.Abbreviation, agent.Name, agent.ParentOrganizationID,
            // agent.Remarks, agent.TimestampModified, agent.TimestampCreated, agent.LastEditedBy,
            // address.AddressID, address.Address, address.City, address.State, address.Country,
            // address.Postalcode, address.Remarks, address.TimestampModified,
            // address.TimestampCreated, address.LastEditedBy From agent
            // Inner Join agentaddress ON agentaddress.AgentID = agent.AgentID Inner Join address ON
            // agentaddress.AddressID = address.AddressID Order By agentaddress.AgentAddressID Asc

            // select agentaddress.AgentAddressID, agentaddress.TypeOfAgentAddressed,
            // agentaddress.AddressID, agentaddress.AgentID, agentaddress.OrganizationID,
            // agentaddress.JobTitle, agentaddress.Phone1, agentaddress.Phone2, agentaddress.Fax,
            // agentaddress.RoomOrBuilding, agentaddress.Email, agentaddress.URL,
            // agentaddress.Remarks, agentaddress.TimestampModified, agentaddress.TimestampCreated,
            // agentaddress.LastEditedBy, agentaddress.IsCurrent, agent.AgentID, agent.AgentType,
            // agent.FirstName, agent.LastName, agent.MiddleInitial, agent.Title, agent.Interests,
            // agent.Abbreviation, agent.Name, agent.ParentOrganizationID, agent.Remarks,
            // agent.TimestampModified, agent.TimestampCreated, agent.LastEditedBy,
            // address.AddressID, address.Address, address.City, address.State, address.Country,
            // address.Postalcode, address.Remarks, address.TimestampModified,
            // address.TimestampCreated, address.LastEditedBy From agent Inner Join agentaddress ON
            // agentaddress.AgentID = agent.AgentID Inner Join address ON agentaddress.AddressID =
            // address.AddressID Order By agentaddress.AgentAddressID Asc

            Statement stmt = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = stmt.executeQuery(sql.toString());

            // Create Map of column name to column index number
            int inx = 1;
            for (String fldName : oldFieldNames)
            {
                // log.info("["+fldName+"] "+inx+" ["+rsmd.getColumnName(inx)+"]");
                indexFromNameMap.put(fldName, inx++);
            }

            // Figure out certain icolumn indexes we will need ater
            int agentIdInx   = indexFromNameMap.get("agent.AgentID");
            int addrIdInx    = indexFromNameMap.get("address.AddressID");
            int agentTypeInx = indexFromNameMap.get("agent.AgentType");
            int lastEditInx  = indexFromNameMap.get("agent.LastEditedBy");
            int nameInx      = indexFromNameMap.get("agent.Name");
            int lastNameInx  = indexFromNameMap.get("agent.LastName");
            int firstNameInx = indexFromNameMap.get("agent.FirstName");
            
            Pair<String, String> namePair = new Pair<String, String>();
            
            int recordCnt = 0;
            while (rs.next())
            {
                byte agentType      = rs.getByte(agentTypeInx);
                int agentAddressId  = rs.getInt(1);
                int agentId         = rs.getInt(agentIdInx);
                int addrId          = rs.getInt(addrIdInx);
                String lastEditedBy = rs.getString(lastEditInx);

                AddressInfo addrInfo  = addressHash.get(addrId);
                AgentInfo   agentInfo = agentHash.get(agentId);

                // Deal with Agent FirstName, LastName and Name
                int    srcColInx = agentType != 1 ? nameInx : lastNameInx;
                namePair.second  = rs.getString(srcColInx);
                namePair.first   = rs.getString(firstNameInx);

                // Now tell the AgentAddress Mapper the New ID to the Old AgentAddressID
                if (shouldCreateMapTables)
                {
                    agentAddrIDMapper.put(agentAddressId, agentInfo.getNewAgentId());
                }

                // Because of the old DB relationships we want to make sure we only add each agent
                // in one time
                // So start by checking the Hashtable to see if it has already been added
                if (!agentInfo.wasAdded())
                {
                    agentInfo.setWasAdded(true);

                    BasicSQLUtils.setIdentityInsertONCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);
                    
                    // It has not been added yet so Add it
                    StringBuilder sqlStr = new StringBuilder();
                    sqlStr.append("INSERT INTO agent ");
                    sqlStr.append("(AgentID, DivisionId, TimestampModified, AgentType, JobTitle, FirstName, LastName, MiddleInitial, ");
                    sqlStr.append("Title, Interests, Abbreviation, Email, URL, Remarks, TimestampCreated, ");
                    sqlStr.append("ParentOrganizationID, CreatedByAgentID, ModifiedByAgentID, Version)");
                    sqlStr.append(" VALUES (");

                    for (int i = 0; i < agentColumns.length; i++)
                    {
                        if (i > 0)
                        {
                            sqlStr.append(",");
                        }

                        if (i == 0)
                        {
                            if (debugAgents)log.info("Adding: "+agentColumns[i]);
                            sqlStr.append(agentInfo.getNewAgentId());
                            sqlStr.append(",");
                            sqlStr.append(conv.getCurDivisionID());

                        } else if (agentColumns[i].equals("agent.ParentOrganizationID"))
                        {
                            if (debugAgents)log.info("Adding: "+agentColumns[i]);
                            Object obj = rs.getObject(indexFromNameMap.get(agentColumns[i]));
                            if (obj != null)
                            {
                                int oldId = rs.getInt(agentColumns[i]);
                                Integer newID = agentIDMapper.get(oldId);
                                if (newID == null)
                                {
                                    log.error("Couldn't map ParentOrganizationID [" + oldId + "]");
                                }
                                sqlStr.append(BasicSQLUtils.getStrValue(newID));

                            } else
                            {
                                sqlStr.append("NULL");
                            }

                        } else if (agentColumns[i].equals("agent.LastName") || agentColumns[i].equals("LastName"))
                        {
                            sqlStr.append(BasicSQLUtils.getStrValue(namePair.second));

                        } else if (agentColumns[i].equals("agent.FirstName") || agentColumns[i].equals("FirstName"))
                        {
                            sqlStr.append(BasicSQLUtils.getStrValue(namePair.first));

                        } else
                        {
                            if (debugAgents)
                            {
                                log.info("Adding: "+agentColumns[i]);
                            }
                            inx = indexFromNameMap.get(agentColumns[i]);
                            sqlStr.append(BasicSQLUtils.getStrValue(rs.getObject(inx)));
                        }
                    }
                    sqlStr.append("," + conv.getCreatorAgentId(lastEditedBy) + "," + conv.getModifiedByAgentId(lastEditedBy) + ",0");
                    sqlStr.append(")");
                    
                    //AddressID, TimestampModified,    Address,    Address2,     City, State, Country, PostalCode, Remarks, TimestampCreated, IsPrimary, Phone1, Phone2, Fax, RoomOrBuilding, AgentID, CreatedByAgentID, ModifiedByAgentID, Version) VALUES 
                    //(485,      "2004-07-29 04:30:41","J Wilson Aquatics",'',"Lawrence","KS","USA",   NULL,       NULL,"2004-07-29 04:30:41",  1,       NULL,   NULL,   NULL,NULL,           495,     1,                1,                 2,0)

                    try
                    {
                        Statement updateStatement = newDBConn.createStatement();
                        // updateStatement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                        if (debugAgents)
                        {
                            log.info(sqlStr.toString());
                        }
                        updateStatement.executeUpdate(sqlStr.toString());
                        
                        Integer newAgentId = BasicSQLUtils.getInsertedId(updateStatement);
                        if (newAgentId == null)
                        {
                            throw new RuntimeException("Couldn't get the Agent's inserted ID");
                        }
                        updateStatement.clearBatch();
                        updateStatement.close();
                        updateStatement = null;
                        
                        conv.addAgentDisciplineJoin(newAgentId, conv.getDisciplineId());

                    } catch (SQLException e)
                    {
                        log.error(sqlStr.toString());
                        log.error("Count: " + recordCnt);
                        e.printStackTrace();
                        log.error(e);
                        System.exit(0);
                        throw new RuntimeException(e);
                    }

                } else
                {
                    // The Agent has already been added so we use the tracker Hashtable
                    // to find out the new Id for the old Agent Id
                    // log.info("Agent already Used
                    // ["+BasicSQLUtils.getStrValue(rs.getObject(indexFromNameMap.get("agent.LastName")))+"]");
                }
                
                BasicSQLUtils.setIdentityInsertOFFCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);
                // Now make sure we only add an address in one
                if (!addrInfo.wasAdded())
                {
                    addrInfo.setWasAdded(true);
                    BasicSQLUtils.setIdentityInsertONCommandForSQLServer(newDBConn, "address", BasicSQLUtils.myDestinationServerType);
                    StringBuilder sqlStr = new StringBuilder("INSERT INTO address ");
                    sqlStr.append("(AddressID, TimestampModified, Address, Address2, City, State, Country, PostalCode, Remarks, TimestampCreated, ");
                    sqlStr.append("IsPrimary, IsCurrent, Phone1, Phone2, Fax, RoomOrBuilding, AgentID, CreatedByAgentID, ModifiedByAgentID, Version)");
                    sqlStr.append(" VALUES (");
                    
                    for (int i = 0; i < addressColumns.length; i++)
                    {
                        if (i > 0)
                        {
                            sqlStr.append(",");
                        }

                        if (i == addressColumns.length - 1)
                        {
                            if (debugAgents)log.info("Adding: "+addressColumns[i]);
                            sqlStr.append(agentInfo.getNewAgentId());

                        } else
                        {
                            Integer inxInt = indexFromNameMap.get(addressColumns[i]);
                            String value;
                            if (i == 0)
                            {
                                value = addrInfo.getNewAddrId().toString();

                            } else if (inxInt == null && addressColumns[i].equals("address.Address2"))
                            {
                                value = "''";

                            } else if (addressColumns[i].equals("agentaddress.IsCurrent"))
                            {
                                value = rs.getInt(inxInt) == 0 ? "0" : "1"; // mapping a boolean
                                
                                sqlStr.append(value); // this is 'IsPrimary' (gets same value as 'IsCurrent'
                                sqlStr.append(",");
                                
                                // Note: 'value' will be for 'IsCurrent'

                            } else if (addressColumns[i].equals("address.Address"))
                            {
                                value = BasicSQLUtils.getStrValue(StringEscapeUtils.escapeJava(rs.getString(inxInt)));

                            } else
                            {
                                // log.info(addressColumns[i]);
                                value = BasicSQLUtils.getStrValue(rs.getObject(inxInt));
                            }
                            if (debugAgents)log.info("Adding: "+addressColumns[i]);
                            sqlStr.append(value);
                        }
                    }
                    sqlStr.append("," + conv.getCreatorAgentId(lastEditedBy) + "," + conv.getModifiedByAgentId(lastEditedBy) + ", 0");
                    sqlStr.append(")");

                    try
                    {
                        Statement updateStatement = newDBConn.createStatement();
                        // updateStatement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                        if (debugAgents)
                        {
                            log.info(sqlStr.toString());
                        }
                        updateStatement.executeUpdate(sqlStr.toString());
                        updateStatement.clearBatch();
                        updateStatement.close();
                        updateStatement = null;

                    } catch (SQLException e)
                    {
                        log.error(sqlStr.toString());
                        log.error("Count: " + recordCnt);
                        e.printStackTrace();
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }
                BasicSQLUtils.setIdentityInsertOFFCommandForSQLServer(newDBConn, "address",
                        BasicSQLUtils.myDestinationServerType);

                if (recordCnt % 250 == 0)
                {
                    log.info("AgentAddress Records: " + recordCnt);
                }
                recordCnt++;
            } // while

            log.info("AgentAddress Records: " + recordCnt);
            rs.close();
            stmt.close();

            // Now duplicate the Address Records
            for (Integer oldAddrId : addressHash.keySet())
            {
                AddressInfo addrInfo = addressHash.get(oldAddrId);

                for (Integer newAddrId : addrInfo.getNewIdsToDuplicate())
                {
                    duplicateAddress(newDBConn, addrInfo.getNewAddrId(), newAddrId);
                }
            }

            // ////////////////////////////////////////////////////////////////////////////////
            // This does the part of AgentAddress where it has JUST Agent
            // ////////////////////////////////////////////////////////////////////////////////
            log.info("******** Doing AgentAddress JUST Agent");

            int newRecordsAdded = 0;

            sql.setLength(0);
            sql.append("SELECT ");
            sql.append(buildSelectFieldList(agentAddrFieldNames, "agentaddress"));
            sql.append(", ");

            getFieldNamesFromSchema(oldDBConn, "agent", agentFieldNames, BasicSQLUtils.mySourceServerType);
            sql.append(buildSelectFieldList(agentFieldNames, "agent"));

            sql.append(" FROM agent Inner Join agentaddress ON agentaddress.AgentID = agent.AgentID where agentaddress.AddressID is null Order By agentaddress.AgentAddressID Asc");

            log.info(sql.toString());

            // Example Query

            // select agentaddress.AgentAddressID, agentaddress.TypeOfAgentAddressed,
            // agentaddress.AddressID, agentaddress.AgentID, agentaddress.OrganizationID,
            // agentaddress.JobTitle, agentaddress.Phone1, agentaddress.Phone2, agentaddress.Fax,
            // agentaddress.RoomOrBuilding, agentaddress.Email, agentaddress.URL,
            // agentaddress.Remarks, agentaddress.TimestampModified, agentaddress.TimestampCreated,
            // agentaddress.LastEditedBy, agentaddress.IsCurrent, agent.AgentID,
            // agent.AgentType, agent.FirstName, agent.LastName, agent.MiddleInitial, agent.Title,
            // agent.Interests, agent.Abbreviation, agent.Name, agent.ParentOrganizationID,
            // agent.Remarks, agent.TimestampModified, agent.TimestampCreated, agent.LastEditedBy,
            // agent.AgentID, agent.AgentType, agent.FirstName, agent.LastName, agent.MiddleInitial,
            // agent.Title, agent.Interests, agent.Abbreviation, agent.Name,
            // agent.ParentOrganizationID, agent.Remarks, agent.TimestampModified,
            // agent.TimestampCreated, agent.LastEditedBy
            // From agent Inner Join agentaddress ON agentaddress.AgentID = agent.AgentID Order By
            // agentaddress.AgentAddressID Asc

            // select agentaddress.AgentAddressID, agentaddress.TypeOfAgentAddressed,
            // agentaddress.AddressID, agentaddress.AgentID, agentaddress.OrganizationID,
            // agentaddress.JobTitle, agentaddress.Phone1, agentaddress.Phone2, agentaddress.Fax,
            // agentaddress.RoomOrBuilding, agentaddress.Email, agentaddress.URL,
            // agentaddress.Remarks, agentaddress.TimestampModified, agentaddress.TimestampCreated,
            // agentaddress.LastEditedBy, agentaddress.IsCurrent, agent.AgentID, agent.AgentType,
            // agent.FirstName, agent.LastName, agent.MiddleInitial, agent.Title, agent.Interests,
            // agent.Abbreviation, agent.Name, agent.ParentOrganizationID, agent.Remarks,
            // agent.TimestampModified, agent.TimestampCreated, agent.LastEditedBy, agent.AgentID,
            // agent.AgentType, agent.FirstName, agent.LastName, agent.MiddleInitial, agent.Title,
            // agent.Interests, agent.Abbreviation, agent.Name, agent.ParentOrganizationID,
            // agent.Remarks, agent.TimestampModified, agent.TimestampCreated, agent.LastEditedBy
            // From agent Inner Join agentaddress ON agentaddress.AgentID = agent.AgentID Order By
            // agentaddress.AgentAddressID Asc

            stmt = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql.toString());

            oldFieldNames.clear();
            GenericDBConversion.addNamesWithTableName(oldFieldNames, agentAddrFieldNames, "agentaddress");
            GenericDBConversion.addNamesWithTableName(oldFieldNames, agentFieldNames,     "agent");

            indexFromNameMap.clear();
            inx = 1;
            for (String fldName : oldFieldNames)
            {
                // log.info("["+fldName+"] "+inx+" ["+rsmd.getColumnName(inx)+"]");
                indexFromNameMap.put(fldName, inx++);
            }

            agentIdInx   = indexFromNameMap.get("agent.AgentID");
            lastEditInx  = indexFromNameMap.get("agent.LastEditedBy");
            agentTypeInx = indexFromNameMap.get("agent.AgentType");
            
            recordCnt = 0;
            while (rs.next())
            {
                byte agentType      = rs.getByte(agentTypeInx);
                int agentAddressId  = rs.getInt(1);
                int agentId         = rs.getInt(agentIdInx);
                String lastEditedBy = rs.getString(lastEditInx);

                AgentInfo agentInfo = agentHash.get(agentId);

                // Now tell the AgentAddress Mapper the New ID to the Old AgentAddressID
                if (shouldCreateMapTables)
                {
                    agentAddrIDMapper.put(agentAddressId, agentInfo.getNewAgentId());
                }

                recordCnt++;

                if (!agentInfo.wasAdded())
                {
                    agentInfo.setWasAdded(true);
                    BasicSQLUtils.setIdentityInsertONCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);
                    
                    // Create Agent
                    StringBuilder sqlStr = new StringBuilder("INSERT INTO agent ");
                    sqlStr.append("(AgentID, DivisionID, TimestampModified, AgentType, JobTitle, FirstName, LastName, MiddleInitial, Title, Interests, ");
                    sqlStr.append("Abbreviation, Email, URL, Remarks, TimestampCreated, ParentOrganizationID, ");
                    sqlStr.append("CreatedByAgentID, ModifiedByAgentID, Version)");
                    sqlStr.append(" VALUES (");
                    for (int i = 0; i < agentColumns.length; i++)
                    {
                        if (i > 0) sqlStr.append(",");
                        
                        if (i == 0)
                        {
                            if (debugAgents) log.info(agentColumns[i]);
                            sqlStr.append(agentInfo.getNewAgentId());
                            sqlStr.append(",");
                            sqlStr.append(conv.getCurDivisionID());

                        } else if (i == lastEditInx)
                        {
                            // Skip the field

                        } else if (agentColumns[i].equals("agent.LastName"))
                        {
                            if (debugAgents) log.info(agentColumns[i]);
                            int    srcColInx = agentType != 1 ? nameInx : lastNameInx;
                            String lName     = BasicSQLUtils.getStrValue(rs.getObject(srcColInx));
                            sqlStr.append(lName);

                        } else
                        {
                            if (debugAgents) log.info(agentColumns[i]);
                            inx = indexFromNameMap.get(agentColumns[i]);
                            sqlStr.append(BasicSQLUtils.getStrValue(rs.getObject(inx)));
                        }
                    }
                    sqlStr.append("," + conv.getCreatorAgentId(lastEditedBy) + "," + conv.getModifiedByAgentId(lastEditedBy) + ", 0"); // '0' is Version
                    sqlStr.append(")");

                    try
                    {
                        Statement updateStatement = newDBConn.createStatement();
                        // updateStatement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                        if (debugAgents)
                        {
                            log.info(sqlStr.toString());
                        }
                        updateStatement.executeUpdate(sqlStr.toString());
                        
                        Integer newAgentId = BasicSQLUtils.getInsertedId(updateStatement);
                        if (newAgentId == null)
                        {
                            throw new RuntimeException("Couldn't get the Agent's inserted ID");
                        }
                        updateStatement.clearBatch();
                        updateStatement.close();
                        updateStatement = null;
                        
                        conv.addAgentDisciplineJoin(newAgentId, conv.getDisciplineId());

                        newRecordsAdded++;

                    } catch (SQLException e)
                    {
                        log.error(sqlStr.toString());
                        log.error("Count: " + recordCnt);
                        e.printStackTrace();
                        log.error(e);
                        throw new RuntimeException(e);
                    }

                }

                if (recordCnt % 250 == 0)
                {
                    log.info("AgentAddress (Agent Only) Records: " + recordCnt);
                }
            } // while
            log.info("AgentAddress (Agent Only) Records: " + recordCnt + "  newRecordsAdded " + newRecordsAdded);

            rs.close();
            stmt.close();
            

            conv.setProcess(0, BasicSQLUtils.getNumRecords(oldDBConn, "agent"));
            conv.setDesc("Adding Agents");

            // Now Copy all the Agents that where part of an Agent Address Conversions
            stmt = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("SELECT AgentID from agent");
            recordCnt = 0;
            while (rs.next())
            {
                Integer agentId = rs.getInt(1);
                AgentInfo agentInfo = agentHash.get(agentId);
                if (agentInfo == null || !agentInfo.wasAdded())
                {
                    copyAgentFromOldToNew(agentId, agentIDMapper);
                }
                recordCnt++;
                if (recordCnt % 50 == 0)
                {
                    conv.setProcess(recordCnt);
                }
            }
            
            conv.setProcess(recordCnt);
            BasicSQLUtils.setIdentityInsertOFFCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);
            
            //------------------------------------------------------------
            // Now Copy all the Agents that where missed
            //------------------------------------------------------------
            conv.setProcess(0);
            stmt = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs   = stmt.executeQuery("SELECT AgentID from agent");
            recordCnt = 0;
            while (rs.next())
            {
                Integer   agentId = rs.getInt(1);
                Integer   newId   = agentIDMapper.get(agentId);
                if (newId != null)
                {
                    Integer isThere = BasicSQLUtils.getCount(newDBConn, "SELECT COUNT(*) FROM agent WHERE AgentID = "+ newId);
                    if (isThere == null || isThere == 0)
                    {
                        copyAgentFromOldToNew(agentId, agentIDMapper);
                    }
                } else
                {
                    tblWriter.logError("Mapping missing for old Agent id["+agentId+"]");
                }
                recordCnt++;
                if (recordCnt % 50 == 0)
                {
                    conv.setProcess(recordCnt);
                }
            }
            conv.setProcess(recordCnt);
            
            if (doFixAgents)
            {
                fixAgentsLFirstLastName();
            }
            
            BasicSQLUtils.setIdentityInsertOFFCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);
            /*
             * if (oldAddrIds.size() > 0) { //log.info("Address Record IDs not used by
             * AgentAddress:");
             * 
             * StringBuilder sqlStr = new StringBuilder("select "); List<String> names = new
             * ArrayList<String>(); getFieldNamesFromSchema(oldDBConn, "address", names);
             * sqlStr.append(buildSelectFieldList(names, "address")); sqlStr.append(" from address
             * where AddressId in (");
             * 
             * cnt = 0; for (Enumeration<Integer> e=oldAddrIds.keys();e.hasMoreElements();) {
             * 
             * Integer id = e.nextElement(); Integer val = oldAddrIds.get(id); if (val == 0) {
             * addrIDMapper.put(id, newAddrId); newAddrId++;
             * 
             * if (cnt > 0) sqlStr.append(","); sqlStr.append(id); cnt++; } } sqlStr.append(")");
             * 
             * Hashtable<String, String> map = new Hashtable<String, String>();
             * map.put("PostalCode", "Postalcode"); String[] ignoredFields = {"IsPrimary",
             * "Address2", "Phone1", "Phone2", "Fax", "RoomOrBuilding", "AgentID"};
             * BasicSQLUtils.setFieldsToIgnoreWhenMappingNames(ignoredFields); copyTable(oldDBConn,
             * newDBConn, sqlStr.toString(), "address", "address", map, null); // closes the
             * oldDBConn automatically BasicSQLUtils.setFieldsToIgnoreWhenMappingNames( null); }
             * 
             * if (oldAgentIds.size() > 0) { StringBuilder sqlStr = new StringBuilder("select ");
             * List<String> names = new ArrayList<String>(); getFieldNamesFromSchema(oldDBConn,
             * "agent", names); sqlStr.append(buildSelectFieldList(names, "agent")); sqlStr.append("
             * from agent where AgentId in (");
             * 
             * cnt = 0; for (Enumeration<Integer> e=oldAgentIds.keys();e.hasMoreElements();) {
             * 
             * Integer id = e.nextElement(); Integer val = oldAgentIds.get(id); if (val == 0) {
             * agentIDMapper.put(id, newAgentId); newAgentId++;
             * 
             * if (cnt > 0) sqlStr.append(","); sqlStr.append(id); cnt++; } } sqlStr.append(")");
             * 
             * String[] ignoredFields = {"JobTitle", "Email", "URL", "Visibility",
             * "VisibilitySetBy"};//User/Security changes
             * BasicSQLUtils.setFieldsToIgnoreWhenMappingNames(ignoredFields); copyTable(oldDBConn,
             * newDBConn, sqlStr.toString(), "agent", "agent", null, null);
             * BasicSQLUtils.setFieldsToIgnoreWhenMappingNames(null);
             * 
             *  } log.info("Agent Address SQL recordCnt "+recordCnt);
             */
            return true;

        } catch (SQLException ex)
        {
            log.error(ex);
            ex.printStackTrace();
            System.exit(0);
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * @param name
     * @param np
     * @return
     */
    protected boolean parseName(final String name, final Triple<String, String, String> np)
    {
        np.first  = null;
        np.second = null;
        np.third  = null;
        
        String[] toks = StringUtils.split(name, ' ');
        switch (toks.length)
        {
            case 1 : 
                np.third = toks[0];
                break;
                
            case 2 : 
                np.first = toks[0];
                np.third = toks[1];
                break;
                
            case 3 : 
                np.first  = toks[0];
                np.second = toks[1];
                np.third  = toks[2];
                break;
                
            default:
                return false;
        }
        return true;
    }
    

    /**
     * @param newDBConnArg
     * @param oldId
     * @param newId
     */
    protected void duplicateAddress(final Connection newDBConnArg,
                                    final Integer oldId,
                                    final Integer newId)
    {
        log.info("Duplicating [" + oldId + "] to [" + newId + "]");

        List<String> agentAddrFieldNames = new ArrayList<String>();
        getFieldNamesFromSchema(newDBConnArg, "address", agentAddrFieldNames,
                BasicSQLUtils.myDestinationServerType);
        String fieldList = buildSelectFieldList(agentAddrFieldNames, "address");
        log.info(fieldList);

        StringBuilder sqlStr = new StringBuilder();
        sqlStr.append("SELECT ");
        sqlStr.append(fieldList);
        sqlStr.append(" from address where AddressID = " + oldId);

        if (true)
        {
            log.info(sqlStr.toString());
        }

        try
        {
            Statement stmt = newDBConnArg.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sqlStr.toString());

            if (rs.last())
            {
                conv.setProcess(0, rs.getRow());
                rs.first();
            }

            sqlStr.setLength(0);
            sqlStr.append("INSERT INTO address ");
            sqlStr.append("(" + fieldList + ")");
            sqlStr.append(" VALUES (");

            ResultSetMetaData metaData = rs.getMetaData();
            int cols = metaData.getColumnCount();
            for (int i = 1; i <= cols; i++)
            {
                if (i == 1)
                {
                    sqlStr.append(newId);

                } else
                {
                    sqlStr.append(",");
                    sqlStr.append(BasicSQLUtils.getStrValue(rs.getObject(i)));
                }
            }
            sqlStr.append(") ");

            rs.close();
            stmt.close();

            Statement updateStatement = newDBConnArg.createStatement();
            BasicSQLUtils.removeForeignKeyConstraints(newDBConn,
                    BasicSQLUtils.myDestinationServerType);
            // updateStatement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            if (true)
            {
                log.info(sqlStr.toString());
            }
            updateStatement.executeUpdate(sqlStr.toString());
            updateStatement.clearBatch();
            updateStatement.close();
            updateStatement = null;

        } catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

    }
 
    /**
     * @param oldAgentId
     * @param agentIDMapper
     * @param tblWriter
     */
    protected void copyAgentFromOldToNew(final Integer oldAgentId, 
                                         final IdTableMapper agentIDMapper)
    {
        boolean doDebug = false;
        
        DBTableInfo agentTI        = DBTableIdMgr.getInstance().getByShortClassName("Agent");
        DBFieldInfo lastNameField  = agentTI.getFieldByColumnName("LastName");
        DBFieldInfo firstNameField = agentTI.getFieldByColumnName("FirstName");
        
        StringBuilder sql = new StringBuilder("select ");
        if (BasicSQLUtils.myDestinationServerType != BasicSQLUtils.SERVERTYPE.MS_SQLServer)
        {
            BasicSQLUtils.removeForeignKeyConstraints(newDBConn, BasicSQLUtils.myDestinationServerType);
        }
        BasicSQLUtils.setIdentityInsertONCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);

        List<String> oldAgentFieldNames = new ArrayList<String>();
        getFieldNamesFromSchema(oldDBConn, "agent", oldAgentFieldNames, BasicSQLUtils.mySourceServerType);
        
        String oldFieldListStr = buildSelectFieldList(oldAgentFieldNames, "agent");
        sql.append(oldFieldListStr);
        sql.append(" FROM agent WHERE AgentID = " + oldAgentId);

        //log.info(oldFieldListStr);

        List<String> newAgentFieldNames = new ArrayList<String>();
        getFieldNamesFromSchema(newDBConn, "agent", newAgentFieldNames, BasicSQLUtils.myDestinationServerType);
        String newFieldListStr = buildSelectFieldList(newAgentFieldNames, "agent");

        //log.info(newFieldListStr);

        Hashtable<String, Integer> oldIndexFromNameMap = new Hashtable<String, Integer>();
        int inx = 1;
        for (String fldName : oldAgentFieldNames)
        {
            oldIndexFromNameMap.put(fldName, inx++);
        }

        Hashtable<String, Integer> newIndexFromNameMap = new Hashtable<String, Integer>();
        inx = 1;
        for (String fldName : newAgentFieldNames)
        {
            newIndexFromNameMap.put(fldName, inx++);
        }

        try
        {
            // So first we hash each AddressID and the value is set to 0 (false)
            Statement stmtX = oldDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rsX   = stmtX.executeQuery(sql.toString());
            
            int agentIDInx   = oldIndexFromNameMap.get("AgentID");
            int agentTypeInx = oldIndexFromNameMap.get("AgentType");
            int nameInx      = oldIndexFromNameMap.get("Name");
            int lastNameInx  = oldIndexFromNameMap.get("LastName");
            int firstNameInx = oldIndexFromNameMap.get("FirstName");

            // log.debug(sql.toString());

            int cnt = 0;
            while (rsX.next())
            {
                int agentId = rsX.getInt(1);

                StringBuilder sqlStr = new StringBuilder();
                sqlStr.append("INSERT INTO agent ");
                sqlStr.append("(" + newFieldListStr);
                sqlStr.append(")");
                sqlStr.append(" VALUES (");

                int fCnt = 0;
                for (String fieldName : newAgentFieldNames)
                {
                    if (fCnt > 0)
                        sqlStr.append(", ");

                    if (StringUtils.contains(fieldName.toLowerCase(), "disciplineid"))
                    {
                        sqlStr.append(conv.getDisciplineId());

                    } else if (StringUtils.contains(fieldName, "FirstName"))
                    {
                        String firstName = rsX.getString(firstNameInx);
                        if (firstName != null && firstName.length() > firstNameField.getLength())
                        {
                            String str = firstName.substring(0, firstNameField.getLength());
                            tblWriter.logError("Agent id: "+rsX.getString(agentIDInx)+" - Concatinating First Name from ["+firstName+"] to ["+str+"]");
                            firstName = str;
                        }
                        sqlStr.append(BasicSQLUtils.getStrValue(firstName));
                        
                    } else if (StringUtils.contains(fieldName, "LastName"))
                    {
                        int    srcColInx = rsX.getInt(agentTypeInx) != 1 ? nameInx : lastNameInx;
                        String lName     = rsX.getString(srcColInx);
                        
                        if (lName != null && lName.length() > lastNameField.getLength())
                        {
                            String str = lName.substring(0, firstNameField.getLength());
                            tblWriter.logError("Agent id: "+rsX.getString(agentIDInx)+" - Concatinating Last Name from ["+lName+"] to ["+str+"]");
                            lName = str;
                        }
                        sqlStr.append(BasicSQLUtils.getStrValue(lName));
                        
                    } else
                    {
                        String value = "";
                        Integer index = oldIndexFromNameMap.get(fieldName);
                        if (index == null)
                        {
                            // log.debug(fieldName);
                            value = "NULL";

                        } else if (fCnt == 0)
                        {
                            value = agentIDMapper.get(agentId).toString();

                        } else
                        {
                            value = BasicSQLUtils.getStrValue(rsX.getObject(index.intValue()));
                        }

                        BasicSQLUtilsMapValueIFace valueMapper = conv.getColumnValueMapper().get(fieldName);
                        if (valueMapper != null)
                        {
                            value = valueMapper.mapValue(value);
                        }
                        sqlStr.append(value);
                    }
                    fCnt++;
                }
                sqlStr.append(")");
                // log.info(sqlStr.toString());

                Statement updateStatement = newDBConn.createStatement();
                // updateStatement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                if (doDebug)
                {
                    log.info(sqlStr.toString());
                }
                updateStatement.executeUpdate(sqlStr.toString());
                Integer newAgentId = BasicSQLUtils.getInsertedId(updateStatement);
                if (newAgentId == null)
                {
                    throw new RuntimeException("Couldn't get the Agent's inserted ID");
                }
                updateStatement.clearBatch();
                updateStatement.close();
                updateStatement = null;
                
                conv.addAgentDisciplineJoin(newAgentId, conv.getDisciplineId());

                cnt++;
                BasicSQLUtils.setIdentityInsertOFFCommandForSQLServer(newDBConn, "agent", BasicSQLUtils.myDestinationServerType);

            }
        } catch (Exception ex)
        {
            log.error(ex);
            ex.printStackTrace();
            System.exit(0);
        }
    }
    
    /**
     * 
     */
    protected void dropCollectorsTable()
    {
        try
        {
            gStmt.executeUpdate("DROP TABLE agent_collectors");
            
        } catch (Exception ex)
        {
            // Exception may occur if table doesn't exist
        }
    }
    
    /**
     * 
     */
    protected void createCollectorsTable()
    {
        try
        {
            dropCollectorsTable();

            String sql = "CREATE TABLE `agent_collectors` ("+
                                "`KeyID` int(11) NOT NULL, "+
                                "`OldID` int(11) NOT NULL, "+
                                "`Name` varchar(255) DEFAULT NULL, "+
                                "`Ids` varchar(255) DEFAULT NULL, "+
                                " PRIMARY KEY (`KeyID`), KEY `AgentNameIDX` (`Name`), ) ENGINE=InnoDB DEFAULT CHARSET=latin1";
            log.info("sql query: " + sql);
            gStmt.executeUpdate(sql);

        } catch (SQLException ex)
        {
            log.error(ex);
            ex.printStackTrace();
        }
    }
    
    /**
     * @param name
     * @param ids
     */
    protected void addAgentCollectors(final String name, final int oldId, final int[] ids)
    {
        try
        {
            StringBuilder sb = new StringBuilder("INSERT INTO agent_collectors (Name, OldID, Ids) VALUES('"+name+"', "+oldId+",'");
            int i = 0;
            for (int id : ids)
            {
                if (i > 0) sb.append(",");
                sb.append(id);
                i++;
            }
            sb.append("')");
            
            gStmt.executeUpdate(sb.toString());

        } catch (SQLException ex)
        {
            log.error(ex);
            ex.printStackTrace();
        }
    }
    
    /**
     * @param name
     * @return array of new Ids for the new agents, but the first id is the original old Id
     */
    protected int[] getAgentCollectors(final String name)
    {
        try
        {
            String sql = "SELECT OldID, Ids FROM agent_collectors WHERE Name = '"+name+"'";
            ResultSet rs = gStmt.executeQuery(sql);
            if (rs.next())
            {
                int      oldId  = rs.getInt(1);
                String   idsStr = rs.getString(2);
                String[] toks   = StringUtils.split(idsStr, ",");
                int[]    ids    = new int[toks.length+1];
                
                ids[0] = oldId;
                for (int i=1;i<toks.length+1;i++)
                {
                    ids[i] = Integer.valueOf(toks[i]);
                }
                return ids;
            }
            rs.close();
            
        } catch (SQLException ex)
        {
            log.error(ex);
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     * 
     */
    public void fixAgentsLFirstLastName()
    {
        try
        {
            Integer numAgents    = BasicSQLUtils.getCount("SELECT COUNT(*) FROM agent WHERE AgentType = " + Agent.PERSON);
            Integer numBadAgents = BasicSQLUtils.getCount("SELECT COUNT(*) FROM agent WHERE FirstName is NULL AND LastName IS NOT NULL AND NOT (LastName LIKE '%&%') AND AgentType = " + Agent.PERSON);
            
            double percent = ((double)numBadAgents / (double)numAgents);
            if (percent > 0.25)
            {
                log.info("Bad Agents > 25% => " + percent);
            }
            
            tblWriter.log("<H4>Person Names not Fixed</H4>");
            tblWriter.startTable();
            tblWriter.logHdr("New Agent ID", "Old Last Name");
            String sql = "SELECT AgentID, LastName FROM agent WHERE FirstName is NULL AND LastName IS NOT NULL AND (LastName LIKE '%&%') AND AgentType = " + Agent.PERSON;
            ResultSet rs = gStmt.executeQuery(sql);
            while (rs.next())
            {
                Integer  id        = rs.getInt(1);
                String   lastName  = rs.getString(2);
                tblWriter.log(id.toString(), lastName);
            }
            rs.close();
            tblWriter.endTable();
            
            tblWriter.log("<BR><H4>Person Names not Fixed</H4>");
            tblWriter.startTable();
            tblWriter.logHdr("Old Last Name", "New First Name", "New Last Name");
            StringBuilder sb = new StringBuilder();
            sql = "SELECT AgentID, LastName FROM agent WHERE FirstName is NULL AND LastName IS NOT NULL AND NOT (LastName LIKE '%&%') AND AgentType = " + Agent.PERSON;
            rs  = gStmt.executeQuery(sql);
            while (rs.next())
            {
                int      id        = rs.getInt(1);
                String   lastName  = rs.getString(2);
                String[] toks   = StringUtils.split(lastName, " ");
                
                if (toks.length > 1)
                {
                    String firstName = toks[0];
                    sb.setLength(0);
                    for (int i=1;i<toks.length;i++)
                    {
                        sb.append(toks[i]);
                        if (i < toks.length-1) sb.append(' ');
                    }
                    
                    tblWriter.log(lastName, firstName, sb.toString());
                    sql = "UPDATE agent SET FirstName=\"" + firstName + "\", LastName=\"" + sb.toString() + "\" WHERE AgentID = " + id;
                    log.debug(sql);
                    updateStmtNewDB.executeUpdate(sql);
                }
                
            }
            rs.close();
            
        } catch (SQLException ex)
        {
            log.error(ex);
            ex.printStackTrace();
        }
        tblWriter.endTable();
    }
    
    /**
     * @param agentInfo
     * @param namePair
     */
    @SuppressWarnings("unchecked")
    protected void fixupForCollectors(final Division    divisionArg,
                                      final Discipline  disciplineArg)
    {
        ParseAgentType parseType = ParseAgentType.LastNameOnlyLF;

        
        Session     session = HibernateUtil.getNewSession();
        Transaction trans   = null;
        try
        {
            Division   division       = (Division)session.createQuery("FROM Division WHERE id = " + divisionArg.getId()).list().iterator().next();
            Discipline discipline     = (Discipline)session.createQuery("FROM Discipline WHERE id = " + disciplineArg.getId()).list().iterator().next();
            Agent      createdByAgent = (Agent)session.createQuery("FROM Agent WHERE id = " + conv.getCreatorAgentId(null)).list().iterator().next();
            
            tblWriter.log("<H4>Splitting Mutliple Collectors names into Multiple Agents</H4>");
            tblWriter.startTable();
            tblWriter.logHdr("New Agent ID", "Old Last Name", "Description");
            
            Vector<Integer> agentToDelete = new Vector<Integer>();
            
            conv.setProcess(0, agentHash.values().size());
            int cnt = 0;
            
            for (AgentInfo agentInfo : agentHash.values())
            {
                String lastNameText  = agentInfo.getAgentType() != Agent.PERSON ? agentInfo.getName() : agentInfo.getLastName();
                String firstNameText = agentInfo.getFirstName();

                if ((StringUtils.contains(lastNameText, ",") || StringUtils.contains(lastNameText, ";")) && !StringUtils.contains(lastNameText, "'"))
                {
                    String    sql = "SELECT c.CollectorID, c.CollectingEventID FROM collector c INNER JOIN agent ON c.AgentID = agent.AgentID WHERE agent.AgentID = " + agentInfo.getNewAgentId();
                    ResultSet rs  = gStmt.executeQuery(sql);
                    if (rs.next())
                    {
                        int      highestOrder = 0;
                        Integer  colID        = rs.getInt(1);
                        Integer  ceId         = rs.getInt(2);
                        
                        tblWriter.log(ceId +" / " + colID +" / " + agentInfo.getNewAgentId().toString(), lastNameText, "&nbsp;");
                        
                        sql = "SELECT ce.CollectingEventID, c.CollectorID, c.OrderNumber, c.IsPrimary FROM collector c INNER JOIN collectingevent ce ON c.CollectingEventID = ce.CollectingEventID " + 
                              "WHERE c.CollectorID = " + colID + " ORDER BY c.OrderNumber DESC";
                        ResultSet rs2 = gStmt.executeQuery(sql);
                        if (rs2.next())
                        {
                            highestOrder = rs2.getInt(3);
                        }
                        rs.close();
                        
                        CollectingEvent ce        = (CollectingEvent)session.createQuery("FROM CollectingEvent WHERE id = " + ceId).list().get(0);
                        Agent           origAgent = (Agent)session.createQuery("FROM Agent WHERE id = " + agentInfo.getNewAgentId()).list().get(0);
                        
                        // Now process the multiple Collectors
                        String[] lastNames  = StringUtils.split(lastNameText, ",;");
                        String[] firstNames = StringUtils.split(firstNameText, ",;");
                        for (int i=0;i<lastNames.length;i++)
                        {
                            if (parseName(lastNames[i], nameTriple))
                            {
                                String firstName = nameTriple.first;
                                String middle    = nameTriple.second;
                                String lastName  = nameTriple.third;
                                
                                List<Agent> agts          = (List<Agent>)session.createQuery("FROM Agent WHERE firstName = " + (firstName != null ? "'" + firstName + "'" : "NULL") 
                                                                         + " AND middleInitial = " + (middle != null ? "'"+middle+"'" : "NULL") 
                                                                         + " AND lastName = " + (lastName != null ? "'" + lastName + "'" : "NULL")).list();
                                Agent       existingAgent = agts != null && agts.size() > 0 ? agts.get(0) : null;
                                
                                trans = session.beginTransaction();
                                if (i == 0)
                                {
                                    if (existingAgent != null)
                                    {
                                        Collector collector = (Collector)session.createQuery("FROM Collector WHERE id = " + colID).list().get(0);
                                        collector.setAgent(existingAgent);
                                        existingAgent.getCollectors().add(collector);
                                        
                                        /*origAgent.getCollectors().clear();
                                        origAgent.getDisciplines().clear();
                                        
                                        for (Agent a : new ArrayList<Agent>(discipline.getAgents()))
                                        {
                                            if (a.getId().equals(origAgent.getId()))
                                            {
                                                discipline.getAgents().remove(a);
                                                break;
                                            }
                                        }
                                        for (Agent a : new ArrayList<Agent>(division.getMembers()))
                                        {
                                            if (a.getId().equals(origAgent.getId()))
                                            {
                                                division.getMembers().remove(a);
                                                break;
                                            }
                                        }
                                        origAgent.setDivision(null);
                                        origAgent.setCreatedByAgent(null);
                                        origAgent.setModifiedByAgent(null);
                                        
                                        //session.delete(origAgent);*/
                                        
                                        tblWriter.log(agentInfo.getNewAgentId().toString(), firstName+", "+lastName, "reusing collector,using existing agent");
                                        
                                        agentToDelete.add(origAgent.getId());
                                        
                                        session.saveOrUpdate(existingAgent);
                                        session.saveOrUpdate(collector);
                                        session.saveOrUpdate(division);
                                        session.saveOrUpdate(discipline);
                                        
                                    } else
                                    {
                                        origAgent.setFirstName(firstName);
                                        origAgent.setLastName(lastName);
                                        origAgent.setAgentType(Agent.PERSON);
                                        
                                        tblWriter.log(agentInfo.getNewAgentId().toString(), firstName+", "+lastName, "resetting agent names - reclaiming");
                                        session.saveOrUpdate(origAgent);
                                    }
                                    
                                } else
                                {
                                    highestOrder++;
                                    
                                    Agent agent;
                                    if (existingAgent == null)
                                    {
                                        agent = new Agent();
                                        agent.initialize();     
                                        agent.setAgentType(Agent.PERSON);
                                        agent.setCreatedByAgent(createdByAgent);
                                        agent.setDivision(division);
                                        agent.setFirstName(nameTriple.first);
                                        agent.setMiddleInitial(nameTriple.second);
                                        agent.setLastName(nameTriple.third);
                                        division.getMembers().add(agent);
                                        agent.getDisciplines().add(discipline);
                                        discipline.getAgents().add(agent);
                                        
                                        tblWriter.log(agentInfo.getNewAgentId().toString(), firstName+", "+lastName, "new agent, new collector");

                                        
                                    } else
                                    {
                                        agent = existingAgent;
                                        tblWriter.log(agentInfo.getNewAgentId().toString(), firstName+", "+lastName, "reusing, new collector");
                                    }
                                    
                                    Collector collector = new Collector();
                                    collector.initialize();
                                    collector.setCreatedByAgent(createdByAgent);
                                    collector.setOrderNumber(highestOrder);
                                    
                                    ce.getCollectors().add(collector);
                                    collector.setCollectingEvent(ce);
                                    collector.setAgent(agent);
                                    agent.getCollectors().add(collector);
                                    collector.setIsPrimary(false);
                                    
                                    session.saveOrUpdate(agent);
                                    session.saveOrUpdate(collector);
                                    session.saveOrUpdate(division);
                                    session.saveOrUpdate(discipline);
                                    
                                }
                                trans.commit();
                            }
                        }
                    }
                    rs.close();
                }
                conv.setProcess(++cnt);
            }
            tblWriter.endTable();
            
            Collections.sort(agentToDelete);
            
            tblWriter.log("<H4>Removing Original Agents</H4>");
            tblWriter.startTable();
            tblWriter.logHdr("New Agent ID", "LastName", "FirstName");
            for (Integer id : agentToDelete)
            {
                System.out.println("Deleting Agent["+id+"]");
                List<Object[]> rows = BasicSQLUtils.query("SELECT AgentID, LastName, MiddleInitial, FirstName FROM agent WHERE AgentID = " + id);
                Object[] row = rows.get(0);
                tblWriter.log(id.toString(), (row[1] == null ? "&nbsp;" : row[1].toString()) + (row[2] == null ? "" : " "+row[2].toString()), 
                                             row[3] == null ? "&nbsp;" : row[3].toString());
                
                updateStmtNewDB.executeUpdate("DELETE FROM agent_discipline WHERE AgentID = " + id);
                updateStmtNewDB.executeUpdate("DELETE FROM agent WHERE AgentID = " + id);
                
            }
            tblWriter.endTable();
            
        } catch (Exception ex)
        {
            try
            {
                if (trans != null) trans.rollback();
            } catch (Exception ex1) {}
            ex.printStackTrace();
            log.error(ex);
            
        } finally
        {
            session.close();
        }
    }
    
    //-------------------------------------------------------------------------
    //--
    //-------------------------------------------------------------------------
    class AddressInfo
    {
        Integer                     oldAddrId;
        Integer                     newAddrId;
        Hashtable<Integer, Boolean> agtHash           = new Hashtable<Integer, Boolean>();
        Vector<Integer>             newIdsToDuplicate = new Vector<Integer>();
        boolean                     isUsed            = false;
        boolean                     wasAdded          = false;

        public AddressInfo(final Integer oldAddrId, final Integer newAddrId, final Integer agentId)
        {
            this.oldAddrId = oldAddrId;
            this.newAddrId = newAddrId;
            agtHash.put(agentId, true);
        }

        public AddressInfo(final Integer oldAddrId, final Integer newAddrId)
        {
            this.oldAddrId = oldAddrId;
            this.newAddrId = newAddrId;
        }

        public Hashtable<Integer, Boolean> getAgentHash()
        {
            return agtHash;
        }

        public Integer getNewAddrId()
        {
            return newAddrId;
        }

        public Integer getOldAddrId()
        {
            return oldAddrId;
        }

        public boolean isUsed()
        {
            return isUsed;
        }

        public void setUsed(boolean isUsed)
        {
            this.isUsed = isUsed;
        }

        public Integer addAgent(Integer agentId)
        {
            agtHash.put(agentId, true);

            if (agtHash.size() > 1)
            {
                newIdsToDuplicate.add(nextAddressId);
                nextAddressId++;
                return nextAddressId;
            }
            return newAddrId;
        }

        public boolean wasAdded()
        {
            return wasAdded;
        }

        public void setWasAdded(boolean wasAddedArg)
        {
            this.wasAdded = wasAddedArg;
        }

        public Vector<Integer> getNewIdsToDuplicate()
        {
            return newIdsToDuplicate;
        }
    }
    
    public List<AgentNameInfo> parseName(final String    firstName, 
                                         final String    lastName,
                                         final char      innerSep,
                                         final char      nameSep,
                                         final boolean   isLastNameFieldFirst,
                                         final boolean   isLastNameFirst,
                                         final boolean   trimAfterComma)
    {
        recycler.addAll(names);
        names.clear();
        
        String   fullName = isLastNameFieldFirst ? lastName + (firstName != null ? " " + firstName : "") : (firstName != null ? (firstName + " ") : "") + lastName;
        String[] toks     = StringUtils.split(fullName, nameSep);
        
        for (String name : toks)
        {
            if (trimAfterComma)
            {
                int inx = name.indexOf(',');
                if (inx > -1)
                {
                    name = name.substring(0, inx);
                }
            }
            
            if (StringUtils.contains(name, nameSep))
            {
                String[] nameToks = StringUtils.split(name, innerSep);
            }
            
        }
        return names;
    }

    //-------------------------------------------------------------------------
    //--
    //-------------------------------------------------------------------------
    class AgentNameInfo
    {
        protected String firstName;
        protected String lastName;
        protected String middle;
        public AgentNameInfo(String firstName, String lastName, String middle)
        {
            super();
            this.firstName = firstName;
            this.lastName = lastName;
            this.middle = middle;
        }
        /**
         * @return the firstName
         */
        public String getFirstName()
        {
            return firstName;
        }
        /**
         * @return the lastName
         */
        public String getLastName()
        {
            return lastName;
        }
        /**
         * @return the middle
         */
        public String getMiddle()
        {
            return middle;
        }
        /**
         * @param firstName the firstName to set
         */
        public void setFirstName(String firstName)
        {
            this.firstName = firstName;
        }
        /**
         * @param lastName the lastName to set
         */
        public void setLastName(String lastName)
        {
            this.lastName = lastName;
        }
        /**
         * @param middle the middle to set
         */
        public void setMiddle(String middle)
        {
            this.middle = middle;
        }
        
    }
    
    //-------------------------------------------------------------------------
    //--
    //-------------------------------------------------------------------------
    class AgentInfo
    {
        Integer                     oldAgentId;
        Integer                     newAgentId;
        Byte                        agentType;
        Hashtable<Integer, Boolean> addrs    = new Hashtable<Integer, Boolean>();
        boolean                     isUsed   = false;
        boolean                     wasAdded = false;
        String                      lastName;
        String                      firstName;
        String                      name;

        public AgentInfo(Integer oldAgentId, 
                         Integer newAgentId,
                         byte    agentType,
                         String  lastName,
                         String  firstName,
                         String  name)
        {
            super();
            this.oldAgentId = oldAgentId;
            this.newAgentId = newAgentId;
            this.agentType  = agentType;
            this.lastName   = lastName;
            this.firstName  = firstName;
            this.name       = name;
        }

        public Hashtable<Integer, Boolean> getAddrs()
        {
            return addrs;
        }

        public Integer getNewAgentId()
        {
            return newAgentId;
        }

        public Integer getOldAgentId()
        {
            return oldAgentId;
        }

        public boolean isUsed()
        {
            return isUsed;
        }

        public void setUsed(boolean isUsed)
        {
            this.isUsed = isUsed;
        }

        public boolean wasAdded()
        {
            return wasAdded;
        }

        public void setWasAdded(boolean wasAddedArg)
        {
            this.wasAdded = wasAddedArg;
        }

        /**
         * @return the agentType
         */
        public Byte getAgentType()
        {
            return agentType;
        }

        /**
         * @return the lastName
         */
        public String getLastName()
        {
            return lastName;
        }

        /**
         * @return the firstName
         */
        public String getFirstName()
        {
            return firstName;
        }

        /**
         * @return the name
         */
        public String getName()
        {
            return name;
        }
        
    }
}
