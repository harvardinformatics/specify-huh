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
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


public class AsaStringMapper
{
    private static final Logger log  = Logger.getLogger(AsaStringMapper.class);

    private File csvFile;
    private LineIterator lineIterator;
    private Hashtable<String, Integer> toIds;

    public AsaStringMapper(File csvFile) throws LocalException
    {
        if (csvFile == null || ! csvFile.exists() || ! csvFile.canRead() )
        {
            throw new LocalException("Couldn't read file: " + (csvFile == null ? "null" : csvFile.getName()));
        }
        
        this.csvFile = csvFile;
        this.toIds = new Hashtable<String, Integer>();

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

            String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
            try
            {
                if (columns.length < 2) throw new LocalException("Not enough columns");
                String name = columns[0];
                Integer botanistId = Integer.parseInt(columns[1]);

                toIds.put(name, botanistId);
            }
            catch (NumberFormatException e)
            {
                throw new LocalException("Couldn't parse number " + line);
            }
        }
    }

    void clear()
    {
        toIds.clear();
    }

    public Integer map(String name)
    {
        return toIds.get(name);
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
}
