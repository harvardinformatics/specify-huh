package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;

import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.ui.ProgressFrame;

public abstract class CsvToSqlLoader
{
	private static final Logger log  = Logger.getLogger(CsvToSqlLoader.class);

    private static Hashtable<String, Integer> collectionIdsByCode = new Hashtable<String, Integer>();

	private LineIterator lineIterator;
	private File csvFile;
	private Statement sqlStatement;
	protected ProgressFrame frame;

	protected Integer currentRecordId;
	
	private Discipline botanyDiscipline;
	private Division   botanyDivision;
	
	public CsvToSqlLoader(File csvFile, Statement sqlStatement) throws LocalException
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

	protected Statement getStatement()
	{
	    return sqlStatement;
	}

	protected Integer getCurrentRecordId()
	{
		return currentRecordId;
	}
	
	protected void setCurrentRecordId(Integer currentRecordId)
	{
		this.currentRecordId = currentRecordId;
	}

	protected void checkNull(Object o, String fieldName) throws LocalException
	{
		if (o == null) throw new LocalException("No " + fieldName, getCurrentRecordId());
	}
	
	protected String truncate(String s, int len, String fieldName)
	{
		if (s.length() > len)
		{
			warn("Truncating " + fieldName, s);
			s = s.substring(0, len);
		}
		return s;
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
	    if (botanyDiscipline == null)
	    {
	        botanyDiscipline = new Discipline();

	        Integer disciplineId = getIntByField("discipline", "DisciplineID", "Name", "Botany");

	        botanyDiscipline.setDisciplineId(disciplineId);
	    }
	    return botanyDiscipline;
	}

	protected Division getBotanyDivision() throws LocalException
	{
	    if (botanyDivision == null)
	    {
	        Division botanyDivision = new Division();

	        Integer divisionId = getIntByField("division", "DivisionID", "Name", "Botany");

	        botanyDivision.setDivisionId(divisionId);
	    }
	    return botanyDivision;
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
			getStatement().executeUpdate(sql);
		}
		catch (SQLException e)
		{
			throw new LocalException(e);
		}

		Integer id = BasicSQLUtils.getInsertedId(getStatement());
		if (id == null)
		{
			throw new LocalException("CsvToSqlLoader: Couldn't get inserted record ID");
		}

		return id;
	}

	// TODO: clean up all these query methods; most should not be called from outside this
	// class, and many probably aren't.  how many do we even need?
	private Integer queryForInt(String sql) throws LocalException
	{
	    log.debug(sql);

	    ResultSet result = null;
		Integer id = null;
		try
		{
			result = getStatement().executeQuery(sql);

			if (result.next())
			{
				id = result.getInt(1);
			}
			if (result.next())
			{
			    log.warn("Multiple results for query: " + sql);
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
	private String getStringByField(String table, String returnField, String matchField, String value)
	    throws LocalException
	{
	    String string = queryForString(table, returnField, matchField, value);
	    
	    if (string == null)
	    {
	        throw new LocalException("Couldn't find " + returnField + " for " + matchField + "=" + value);
	    }
	    
	    return string;
	}

	/**
     * Compose and execute a query to return an String.  Throws exception if not found.
     * @throws LocalException
     */
	protected String getStringByField(String table, String returnField, String matchField, Integer value)
	    throws LocalException
	{
	    return getStringByField(table, returnField, matchField, String.valueOf(value));
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
	        result = getStatement().executeQuery(sql);

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
			int success = getStatement().executeUpdate(sql);
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
		
	protected void warn(String message, String item)
	{
	    log.warn(message + " [" + getCurrentRecordId() + "] " + item);
	}
}
