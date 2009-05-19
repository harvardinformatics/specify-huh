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

import edu.harvard.huh.asa.Affiliate;
import edu.harvard.huh.asa.Optr;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.ui.ProgressFrame;

public abstract class CsvToSqlLoader {

	private static final Logger log  = Logger.getLogger(CsvToSqlLoader.class);

	private static Hashtable<Integer, Agent> agentsByOptrId = new Hashtable<Integer, Agent>();

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
			counter++;

			String line = null;
			try {
				line = getNextLine();
			}
			catch (LocalException e) {
				log.error("Couldn't read line", e);
				continue;
			}

			if (line == null) break;

			updateProgressFrame(counter);

			String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
			try {
				loadRecord(columns);
			}
			catch (LocalException e) {
				log.error("Couldn't insert record for line " + counter + "\n" + line, e);
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

	        String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", optr.getGuid());

	        Integer agentId = queryForId(sql);

	        agent = new Agent();
	        agent.setAgentId(agentId);
	        agentsByOptrId.put(optrId, agent);
	    } 
	    
        return agent;
	}

	protected Discipline getBotanyDiscipline() throws LocalException
	{
	    Discipline d = new Discipline();
	    String sql = SqlUtils.getQueryIdByFieldSql("discipline", "DisciplineID", "Name", "Botany");

        Integer disciplineId = queryForId(sql);
        d.setDisciplineId(disciplineId);

        return d;
	}

	protected Division getBotanyDivision() throws LocalException
	{
	    Division d = new Division();
	    String sql = SqlUtils.getQueryIdByFieldSql("division", "DivisionID", "Name", "Botany");

	    Integer divisionId = queryForId(sql);
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
				log.info("Converted " + counter + " records");
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

	protected Integer queryForId(String sql) throws LocalException
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

	protected void warn(String message, Integer id, String item)
	{
	    log.warn(message + " " + id + " " + item);
	}
}
