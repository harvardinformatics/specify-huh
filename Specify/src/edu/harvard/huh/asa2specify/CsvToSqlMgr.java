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
package edu.harvard.huh.asa2specify;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.specify.conversion.BasicSQLUtils;

public class CsvToSqlMgr
{
    private Connection   connection;
    private Statement    statement;
    private File         file;
    private LineIterator lineIterator;

    public enum TYPE  {Text, Number, Date, Remarks, Function};

    public CsvToSqlMgr(String fileName) throws LocalException
    {
        // set up a database connection for direct sql
        try {
            connection = DBConnection.getInstance().createConnection();
            statement  = connection.createStatement();
        }
        catch (SQLException e)
        {
            throw new LocalException("Couldn't create Statement", e);
        }
        
        // get file to load from
        file = new File(fileName);
        if (file == null || !file.exists())
        {
            throw new LocalException("CsvToSqlMgr: Couldn't find file[" + file.getAbsolutePath() + "]");
        }
    }

    Connection getConnection()
    {
        return this.connection;
    }
    Statement getStatement()
    {
        return this.statement;
    }
    
    public int countRecords() throws LocalException
    {
        // count the lines in the file and set up an iterator for them
        int lastLine = 0;
        LineIterator lines = null;
        try
        {
            lines = FileUtils.lineIterator(file);
        }
        catch (IOException e)
        {
            throw new LocalException("CsvToSqlMgr: Couldn't create LineIterator", e);
        }

        while (lines.hasNext())
        {
            lastLine++;
            lines.nextLine();
        }

        LineIterator.closeQuietly(lines);
        
        return lastLine;
    }

    public String getNextLine() throws LocalException
    {
        try
        {
            if (lineIterator == null)
            {
                lineIterator = FileUtils.lineIterator(file);
            }
        } catch (IOException e)
        {
            throw new LocalException("CsvToSqlMgr: Couldn't create LineIterator", e);
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
    
    public Integer insert(String sql) throws LocalException
    {
        try
        {
            statement.executeUpdate(sql);
        }
        catch (SQLException e)
        {
            throw new LocalException(e);
        }

        Integer id = BasicSQLUtils.getInsertedId(statement);
        if (id == null)
        {
            throw new LocalException("CsvToSqlMgr: Couldn't get the Locality's inserted ID");
        }
        
        return id;
    }

    public Integer queryForId(String sql) throws LocalException
    {
        ResultSet result = null;
        Integer id = null;
        try
        {
            result = statement.executeQuery(sql);
            
            if (result.next())
            {
                id = result.getInt(1);
            }
            
        } catch (SQLException e)
        {
            throw new LocalException("CsvToSqlMgr: Couldn't execute query", e);
        }

        return id;
    }

    public boolean update(String sql) throws LocalException
    {
        try {
            int success = statement.executeUpdate(sql);
            if (success != 1)
            {
                return false;
            }
        }
        catch (SQLException e)
        {
            throw new LocalException("CsvToSqlMgr: Couldn't execute update", e);
        }
        
        return true;
    }

    public void closeQuietly()
    {
        try {
            connection.close();
        }
        catch (SQLException e)
        {
            ;
        }

        LineIterator.closeQuietly(lineIterator);
    }
}
