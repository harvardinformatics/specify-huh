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

import edu.harvard.huh.asa2specify.IdNotFoundException;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.ui.ProgressFrame;

public abstract class CsvToSqlLoader
{
    protected static final String EMPTY = "EMPTY";
    
    private static Hashtable<String, Integer> collectionIdsByCode = new Hashtable<String, Integer>();

    private LineIterator    lineIterator;
    private File            csvFile;
    private Statement       sqlStatement;
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
        preLoad();
        
        int records = countRecords();
        getLogger().info( this.getClass().getSimpleName() + ": " + records + " records input");

        // initialize progress frame
        initializeProgressFrame(records);

        // iterate over lines, creating locality objects for each via sql
        int counter   = 0;
        int errors    = 0;
        int successes = 0;
        
        while (true)
        {
            String line = null;
            try {
                line = getNextLine();
            }
            catch (LocalException e) {
                getLogger().error("Couldn't read line", e);
                continue;
            }

            if (line == null) break;

            counter++;

            if (counter % 1000 == 0)
            {
                getLogger().info("Processed " + counter + " records");
            }

            updateProgressFrame(counter);

            String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
            
            for (int i=0; i<columns.length; i++)
            {
            	columns[i] = StringUtils.trimToNull(columns[i]);
            }
            try {
                loadRecord(columns);
            }
            catch (LocalException e) {
                errors++;
                getLogger().error(rec() + e.getMessage());
                continue;
            }
            
            successes++;
        }
        
        getLogger().info(counter + " records processed");
        getLogger().info(successes + " successful imports");
        getLogger().info(errors + " errors");
        
        postLoad();
        
        return counter;
    }

    public abstract void loadRecord(String[] columns) throws LocalException;
    
    public abstract Logger getLogger();
    
    public void setFrame(ProgressFrame frame)
    {
        this.frame = frame;
    }
    
    protected void preLoad() throws LocalException
    {
        ;
    }

    protected void postLoad() throws LocalException
    {
        ;
    }

    protected Integer getCurrentRecordId()
    {
        return currentRecordId;
    }

    protected void setCurrentRecordId(Integer currentRecordId)
    {
        this.currentRecordId = currentRecordId;
    }

    protected Statement getStatement()
    {
        return sqlStatement;
    }

    protected void checkNull(Object o, String fieldName) throws LocalException
    {
        if (o == null) throw new LocalException("No " + fieldName, getCurrentRecordId());
    }

    protected String rec()
    {
        return " [" + getCurrentRecordId() + "] ";
    }
    
    protected String truncate(String s, int len, String fieldName)
    {
        if (s == null) return s;
        
        if (s.length() > len)
        {
            getLogger().warn(rec() + "Truncating " + fieldName + ": " + s);
            s = s.substring(0, len);
        }
        return s;
    }

    protected Integer getCollectionId(String code) throws LocalException
    {
        // TODO: decide on multiple/single collection loading
        code = "HUH";

        Integer collectionId = collectionIdsByCode.get(code);
        if (collectionId == null)
        {
            collectionId = getId("collection", "CollectionID", "Code", code);

            collectionIdsByCode.put(code, collectionId);
        }
        return collectionId;
    }

    protected Discipline getBotanyDiscipline() throws LocalException
    {
        if (botanyDiscipline == null)
        {
            botanyDiscipline = new Discipline();

            Integer disciplineId = getId("discipline", "DisciplineID", "Name", "Botany");

            botanyDiscipline.setDisciplineId(disciplineId);
        }
        return botanyDiscipline;
    }

    protected Division getBotanyDivision() throws LocalException
    {
        if (botanyDivision == null)
        {
            botanyDivision = new Division();

            Integer divisionId = getId("division", "DivisionID", "Name", "Botany");

            botanyDivision.setDivisionId(divisionId);
        }
        return botanyDivision;
    }

    protected void execute(String sql) throws LocalException
    {
        getLogger().debug(sql);
        
        try
        {
            getStatement().execute(sql);
        }
        catch (SQLException e)
        {
            throw new LocalException("Problem executing sql", e);
        }
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
    protected Integer getId(String table, String intField, String field, String value)
    throws LocalException
    {
        Integer id = queryForInt(table, intField, field, value);

        if (id == null)
        {
            throw new IdNotFoundException("Couldn't find " + intField + " for " + field + "=" + value);
        }

        return id;
    }

    /**
     * Compose and execute a query to return an Integer.  Throws exception if not found.
     * @throws LocalException
     */
    protected Integer getInt(String table, String intField, String field, Integer value)
    throws LocalException
    {
        return getId(table, intField, field, String.valueOf(value));
    }


    /**
     * Compose and execute a query to return an String.  Throws exception if not found.
     * @throws LocalException
     */
    protected String getString(String table, String returnField, String matchField, Integer value)
    throws LocalException
    {
        return getString(table, returnField, matchField, String.valueOf(value));
    }

    // TODO: clean up all these query methods; most should not be called from outside this
    // class, and many probably aren't.  how many do we even need?
    protected Integer queryForInt(String sql) throws LocalException
    {
        getLogger().debug(sql);

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
                getLogger().warn("Multiple results for query: " + sql);
            }

        } catch (SQLException e)
        {
            throw new LocalException("CsvToSqlLoader: Couldn't execute query", e);
        }

        return id;
    }

    protected Integer getInt(String sql) throws LocalException
    {
        getLogger().debug(sql);

        ResultSet result = null;
        Integer id = null;
        try
        {
            result = getStatement().executeQuery(sql);

            if (result.next())
            {
                id = result.getInt(1);
            }
            else
            {
                throw new LocalException("CsvToSqlLoader: Query returned no results");
            }
            if (result.next())
            {
                getLogger().warn("Multiple results for query: " + sql);
            }

        } catch (SQLException e)
        {
            throw new LocalException("CsvToSqlLoader: Couldn't execute query", e);
        }

        return id;
    }
    
    protected Integer insert(String sql) throws LocalException
    {
        getLogger().debug(sql);

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

    protected boolean update(String sql) throws LocalException
    {
        getLogger().debug(sql);

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

    /**
     * Compose and execute a query to return an String.  Throws exception if not found.
     * @throws LocalException
     */
    private String getString(String table, String returnField, String matchField, String value)
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
        getLogger().debug(sql);

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
}
