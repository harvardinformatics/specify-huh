package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Optr;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.ui.ProgressFrame;

public abstract class CsvToSqlLoader
{
	private static final Logger log  = Logger.getLogger(CsvToSqlLoader.class);

	private static Hashtable<Integer, Agent>       agentsByOptrId = new Hashtable<Integer, Agent>();
    private static Hashtable<String, Integer> collectionIdsByCode = new Hashtable<String, Integer>();
	private static Hashtable<String, PrepType>    prepTypesByName = new Hashtable<String, PrepType>();

	private LineIterator lineIterator;
	private File csvFile;
	private Statement sqlStatement;
	protected ProgressFrame frame;

	public CsvToSqlLoader(File csvFile, Statement sqlStatement)
	{
		this.csvFile = csvFile;
		this.sqlStatement = sqlStatement;
	}

	public int loadRecords() throws LocalException
	{
		int records = countRecords();
		log.info(records + " records");

		// initialize progress frame
		initializeProgressFrame(records);

		// iterate over lines, creating locality objects for each via sql
		int counter = 0;

		while (true)
		{
			String line = null;
			try {
				line = getNextLine();
			}
			catch (LocalException e) {
				log.error("Couldn't read line", e);
				continue;
			}

			if (line == null) break;

			counter++;

			if (counter % 1000 == 0)
			{
			    log.info("Processed " + counter + " records");
			}

			updateProgressFrame(counter);

			String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
			try {
				loadRecord(columns);
			}
			catch (LocalException e) {
				log.error("Couldn't insert record for line " + counter + "\n" + line);
				continue;
			}
		}

		return counter;
	}

	public abstract void loadRecord(String[] columns) throws LocalException;

	protected void numberNodes(String tableName, String idField) throws LocalException
	{
		String tempTableName = "tempTable";
		try {
			String resetHighestChildNodeNumber = "update " + tableName + " set HighestChildNodeNumber=null";
			sqlStatement.executeUpdate(resetHighestChildNodeNumber);

			String createTableLikeTaxon = "create table " + tempTableName + " like " + tableName;
			sqlStatement.execute(createTableLikeTaxon);

			String copyTaxonTable = "insert into " + tempTableName + " select * from " + tableName;
			sqlStatement.executeUpdate(copyTaxonTable);

			String updateLeafNodes = "update " + tableName + " a " +
			"set a.HighestChildNodeNumber=a.NodeNumber " +
			"where not exists " +
			"(select null from " + tempTableName + " b where a." + idField + "=b.ParentID)";

			int updatedNodes = sqlStatement.executeUpdate(updateLeafNodes);

			String updateReftaxon = "update " + tempTableName + " a " +
			"set a.HighestChildNodeNumber=" +
			"(select b.HighestChildNodeNumber from " + tableName + " b " +
			"where b." + idField + "=a." + idField + ")";

			sqlStatement.executeUpdate(updateReftaxon);

			String updateNextLevel = "update " + tableName + " a " +
			"set a.HighestChildNodeNumber=" +
			"(select max(b.HighestChildNodeNumber) from " + tempTableName + " b " +
			"where b.ParentID=a." + idField + ") " +
			"where  a." + idField + " in (select c.ParentID from " + tempTableName + " c " +
			"where c.HighestChildNodeNumber is not null) order by a." + idField + " desc";

			while (updatedNodes > 0) {               
				updatedNodes = sqlStatement.executeUpdate(updateNextLevel);
				sqlStatement.executeUpdate(updateReftaxon);
			}
			
			String dropTempTable = "drop table " + tempTableName;
			sqlStatement.execute(dropTempTable);
			
		}
		catch (SQLException e)
		{
			throw new LocalException("Problem numbering nodes", e);
		}
	}

	protected Agent getAgentByOptrId(Integer optrId) throws LocalException
	{
	    Agent agent = agentsByOptrId.get(optrId);

	    if (agent == null)
	    {
	        Optr optr = new Optr();
	        optr.setId(optrId);

	        Integer agentId = getIntByField("agent", "AgentID", "GUID", optr.getGuid());
	        
	        agent = new Agent();
	        agent.setAgentId(agentId);
	        agentsByOptrId.put(optrId, agent);
	    } 
	    
        return agent;
	}

	protected PrepType getPrepType(String format) throws LocalException
	{
        PrepType prepType = prepTypesByName.get(format);
        if (prepType == null)
        {
            Integer prepTypeId = getIntByField("preptype", "PrepTypeID", "Name", format);

            prepType = new PrepType();
            prepType.setPrepTypeId(prepTypeId);
            prepTypesByName.put(format, prepType);
        }
        return prepType;
	}
	
	protected Integer getCollectionId(String code) throws LocalException
	{
	    Integer collectionId = collectionIdsByCode.get(code);
	    if (collectionId == null)
	    {
	        collectionId = getIntByField("collection", "CollectionID", "Code", code);

	        collectionIdsByCode.put(code, collectionId);
	    }
	    return collectionId;
	}
	
	protected Discipline getBotanyDiscipline() throws LocalException
	{
	    Discipline d = new Discipline();
	    
        Integer disciplineId = getIntByField("discipline", "DisciplineID", "Name", "Botany");

        d.setDisciplineId(disciplineId);

        return d;
	}

	protected Division getBotanyDivision() throws LocalException
	{
	    Division d = new Division();

	    Integer divisionId = getIntByField("division", "DivisionID", "Name", "Botany");
        
	    d.setDivisionId(divisionId);

	    return d;
	}

	public void setFrame(ProgressFrame frame)
	{
		this.frame = frame;
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
			}
		}
	}
	private int countRecords() throws LocalException
	{
		// count the lines in the file and set up an iterator for them
		int lastLine = 0;
		LineIterator lines = null;
		try
		{
			lines = FileUtils.lineIterator(csvFile);
		}
		catch (IOException e)
		{
			throw new LocalException("CsvToSqlLoader: Couldn't create LineIterator", e);
		}

		while (lines.hasNext())
		{
			lastLine++;
			lines.nextLine();
		}

		LineIterator.closeQuietly(lines);

		return lastLine;
	}

	private String getNextLine() throws LocalException
	{
		try
		{
			if (lineIterator == null)
			{
				lineIterator = FileUtils.lineIterator(csvFile);
			}
		} catch (IOException e)
		{
			throw new LocalException("CsvToSqlLoader: Couldn't create LineIterator", e);
		}

		if (lineIterator.hasNext())
		{
			return lineIterator.nextLine();
		}
		else
		{
			LineIterator.closeQuietly(lineIterator);
			return null;
		}
	}

	protected Integer insert(String sql) throws LocalException
	{
	    log.debug(sql);
	    
		try
		{
			sqlStatement.executeUpdate(sql);
		}
		catch (SQLException e)
		{
			throw new LocalException(e);
		}

		Integer id = BasicSQLUtils.getInsertedId(sqlStatement);
		if (id == null)
		{
			throw new LocalException("CsvToSqlLoader: Couldn't get inserted record ID");
		}

		return id;
	}

	private Integer queryForInt(String sql) throws LocalException
	{
	    log.debug(sql);

	    ResultSet result = null;
		Integer id = null;
		try
		{
			result = sqlStatement.executeQuery(sql);

			if (result.next())
			{
				id = result.getInt(1);
			}

		} catch (SQLException e)
		{
			throw new LocalException("CsvToSqlLoader: Couldn't execute query", e);
		}

		return id;
	}

	/**
     * Compose and execute a query to return an Integer.  Returns null if not found.
     * @throws LocalException
     */
	protected Integer queryForInt(String table, String intField, String field, String value)
	    throws LocalException
	{
	    String sql = SqlUtils.getQueryIdByFieldSql(table, intField, field, value);
	    
	    Integer id = queryForInt(sql);
	    
	    return id;
	}

	/**
	 * Compose and execute a query to return an Integer.  Returns null if not found.
	 * @throws LocalException
	 */
	protected Integer queryForInt(String table, String intField, String field, Integer value)
	    throws LocalException
	{
	    String sql = SqlUtils.getQueryIdByFieldSql(table, intField, field, String.valueOf(value));

	    Integer id = queryForInt(sql);

	    return id;
	}

	/**
     * Compose and execute a query to return an Integer.  Throws exception if not found.
     * @throws LocalException
     */
	protected Integer getIntByField(String table, String intField, String field, String value)
	    throws LocalException
	{
	    Integer id = queryForInt(table, intField, field, value);
	    
	    if (id == null)
	    {
	        throw new LocalException("Couldn't find " + intField + " for " + field + "=" + value);
	    }
	    
	    return id;
	}
	
	/**
     * Compose and execute a query to return an Integer.  Throws exception if not found.
     * @throws LocalException
     */
	protected Integer getIntByField(String table, String intField, String field, Integer value)
	    throws LocalException
	{
	    return getIntByField(table, intField, field, String.valueOf(value));
	}

	/**
     * Compose and execute a query to return an String.  Throws exception if not found.
     * @throws LocalException
     */
	protected String getStringByField(String table, String returnField, String matchField, String value)
	    throws LocalException
	{
	    String string = queryForString(table, returnField, matchField, value);
	    
	    if (string == null)
	    {
	        throw new LocalException("Couldn't find " + returnField + " for " + matchField + "=" + value);
	    }
	    
	    return string;
	}

	private String queryForString(String table, String returnField, String matchField, String value)
	    throws LocalException
	{
	    String sql = SqlUtils.getQueryIdByFieldSql(table, returnField, matchField, value);
	    
	    return queryForString(sql);
	}

	private String queryForString(String sql) throws LocalException
	{
	    log.debug(sql);

	    ResultSet result = null;
	    String string = null;
	    try
	    {
	        result = sqlStatement.executeQuery(sql);

	        if (result.next())
	        {
	            string = result.getString(1);
	        }

	    } catch (SQLException e)
	    {
	        throw new LocalException("CsvToSqlLoader: Couldn't execute query", e);
	    }

	    return string;
	}
	   
	protected boolean update(String sql) throws LocalException
	{
	    log.debug(sql);
	    
		try {
			int success = sqlStatement.executeUpdate(sql);
			if (success != 1)
			{
				return false;
			}
		}
		catch (SQLException e)
		{
			throw new LocalException("CsvToSqlLoader: Couldn't execute update", e);
		}

		return true;
	} 

	protected String formatBarcode(Integer barcode) throws LocalException
	{
	    if (barcode == null)
	    {
	        throw new LocalException("Null barcode");
	    }
	    
        try
        {
            return (new DecimalFormat( "000000000" ) ).format( barcode );
        }
        catch (IllegalArgumentException e)
        {
            throw new LocalException("Couldn't parse barcode");
        }
	}
	
	protected String formatBarcode(String barcode) throws LocalException
	{
		if (barcode == null)
		{
			throw new LocalException("Null barcode");
		}
		
		Integer intBarcode = null;
		try
		{
			intBarcode = Integer.parseInt(barcode);
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse barcode");
		}
		
		return formatBarcode(intBarcode);
	}
	
	protected void warn(String message, Integer id, String item)
	{
	    log.warn(message + " " + id + " " + item);
	}
}
