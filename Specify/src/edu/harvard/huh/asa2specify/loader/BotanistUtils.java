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
package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class BotanistUtils
{
    private static final Logger log  = Logger.getLogger(BotanistUtils.class);
    
    private static Hashtable<Integer, Integer> botanistIdsByAffiliateId = new Hashtable<Integer, Integer>();
    
    class AffiliateBotanistMatcher
    {
        File csvFile;
        private LineIterator lineIterator;
        
        AffiliateBotanistMatcher(File csvFile) throws LocalException
        {
            this.csvFile = csvFile;

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
                try {
                    if (columns.length < 2) throw new LocalException("Not enough columns");
                    Integer affiliateId = Integer.parseInt(columns[0]);
                    Integer botanistId = Integer.parseInt(columns[1]);

                    botanistIdsByAffiliateId.put(affiliateId, botanistId);
                }
                catch (NumberFormatException e) {
                    throw new LocalException("Couldn't parse number " + line);
                }
            }
        }
        
        Integer getBotanistIdByAffiliateId(Integer affiliateId)
        {
            return botanistIdsByAffiliateId.get(affiliateId);
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
}
