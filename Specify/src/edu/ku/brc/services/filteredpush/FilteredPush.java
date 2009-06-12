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
package edu.ku.brc.services.filteredpush;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import edu.ku.brc.util.Pair;
import edu.umb.cs.filteredpush.client.QueryImpl;
import edu.umb.cs.filteredpush.network.CommunicationImpl;

public class FilteredPush
{
    final static String fpIdFieldName              = "id";
    final static String fpBarcodeFieldName         = "barcode";
    final static String fpCollectorNumberFieldName = "collectorNumber";
    final static String fpCollectorFieldName       = "collector";
    final static String fpTaxonFieldName           = "taxon";
    final static String fpLocalityFieldName        = "locality";
    final static String fpLatitudeFieldName        = "latitude";
    final static String fpLongitudeFieldName       = "longitude";
    

    protected static String[] filteredPushSelectFields = {
        fpIdFieldName,
        fpBarcodeFieldName,
        fpCollectorNumberFieldName,
        fpCollectorFieldName,
        fpTaxonFieldName,
        fpLocalityFieldName,
        fpLatitudeFieldName,
        fpLongitudeFieldName,
    };
    
    final static int fpIdFieldIndex              = 0;
    final static int fpBarcodeFieldIndex         = 1;
    final static int fpCollectorNumberFieldIndex = 2;
    final static int fpCollectorFieldIndex       = 3;
    final static int fpTaxonFieldIndex           = 4;
    final static int fpLocalityFieldIndex        = 5;
    final static int fpLatitudeFieldIndex        = 6;
    final static int fpLongitudeFieldIndex       = 7;

    final static int fpFieldCount = 8;
    
    public FilteredPush()
    {
        // empty
    }

    /**
     * This method inspects the response received from a FP call and determines
     * the number of possible results given. (as in BioGeomancer)
     * 
     * @param filteredPushResponseString the response from the FP service
     * @return the number of possible results found in the given response
     * @throws Exception if parsing the response string fails
     */
    public static int getResultsCount(final String filteredPushResponseString) throws Exception
    {
        StringTokenizer st = new StringTokenizer(filteredPushResponseString, "\\n");
        return st.countTokens();
    }
    
    /**
     * Sends a query request to the Filtered Push hadoop daemon.
     * 
     * @param List<Pair<String, String>> list of key-value String pairs
     * @return returns the response body content (as tab-delimited fields of newline-delimited records)
     * @throws IOException a network error occurred while contacting the BioGeomancer service
     */
    public static String getFilteredPushResponse(List<Pair<String, String>> keyValuePairs) throws IOException
    {
        // get the "where" fields (collector name and number)
        TreeMap<String, String> conditions = new TreeMap<String, String>();
        
        // TODO: what if fields are empty? include them, requiring the match to have that field empty?
        
        // TODO: does it make sense to send a query with no key-value pairs?
        
        // TODO: should Specify know about fields required by FP?
        
        // TODO: how does FP notify us of an error?  how does Specify translate that to the user?
        
        // TODO: should the Specify user even know that this is FP?  how thin of a wrapper is Specify's FP plugin?
        
        String collectorAndNumber = "Brown&RSC100"; // key for fuzzy matching
        
        /*for (Pair<String, String> keyValuePair : keyValuePairs)
        {
            String key = keyValuePair.getFirst();
            String value = keyValuePair.getSecond();
            
            if (key != null && value != null)
            {
                conditions.put(key, value);
            }
        }*/
        
        QueryImpl qi = new QueryImpl();
        
        //com.sendDuplicatesFindingMessage(message, collectorAndNumber);

        final InputStream is = qi.query(filteredPushSelectFields, conditions, collectorAndNumber);

        if (is == null)
        {
            return null;
        }

        final InputStreamReader isr = new InputStreamReader(is);
        if (isr == null)
        {
            return null;
        }

        final BufferedReader bfr = new BufferedReader(isr);
        if (bfr == null)
        {
            return null;
        }

        StringBuffer response = new StringBuffer();
        String record = bfr.readLine();
        
        while (record != null)
        {
            record.replaceAll("[\\n\\r]", " ");
            response.append(record + "\n");
            record = bfr.readLine();
        }
        
        return response.toString();

    }
    
    // note to self: this is tied to filteredPushSelectFields:
    // filteredPushSelectFields = new String[] {"id", "barcode", "collectorNumber", "collector", "taxon"};
    // is used to create the sql query
    public static List<FilteredPushResult> parse(String response) {
                
        List<FilteredPushResult> results = new ArrayList<FilteredPushResult>();

        if (response == null)
        {
            return results;
        }

        String[] fpRecords = response.split("\\n+");
        for (String fpRecord : fpRecords) {

            String[] fpFields = fpRecord.split("\\t");

            FilteredPushResult result = new FilteredPushResult();
            
            if (fpFields.length >= filteredPushSelectFields.length)
            {
                result.setId(fpFields[fpIdFieldIndex]);
                result.setBarcode(fpFields[fpBarcodeFieldIndex]);
                result.setCollectorNumber(fpFields[fpCollectorNumberFieldIndex]);
                result.setCollector(fpFields[fpCollectorFieldIndex]);
                result.setLocality(fpFields[fpLocalityFieldIndex]);
                result.setLatitude(fpFields[fpLatitudeFieldIndex]);
                result.setLongitude(fpFields[fpLongitudeFieldIndex]);

                String[] binomial = fpFields[fpTaxonFieldIndex].split("\\s");

                if (binomial.length > 0) result.setGenus(binomial[0]);
                if (binomial.length > 1) result.setSpecies(binomial[1]);

                if (fpFields.length >= filteredPushSelectFields.length + 1)
                {
                    result.setServerName(fpFields[filteredPushSelectFields.length]);
                }
                results.add(result);
            }
        }
        return results;
    }
}
