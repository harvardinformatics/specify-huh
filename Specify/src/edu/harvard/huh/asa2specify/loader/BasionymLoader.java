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
import java.sql.Statement;

import edu.harvard.huh.asa.AsaTaxon;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.Taxon;

// Run this class after TaxonLoader and SpecimenItemLoader.

public class BasionymLoader extends CsvToSqlLoader
{
    private TaxonLookup    taxonLookup;
    
    public BasionymLoader(File csvFile,
                          Statement      sqlStatement,
                          TaxonLookup    taxonLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.taxonLookup    = taxonLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaTaxon asaTaxon = parse(columns);

        Integer asaTaxonId = asaTaxon.getId();
        setCurrentRecordId(asaTaxonId);
        
        Taxon specifyTaxon = lookupTaxon(asaTaxonId);
        Taxon basionymTaxon = lookupTaxon(asaTaxon.getBasionymId());
        
        String basionym = this.getString("taxon", "FullName", "TaxonID", basionymTaxon.getId());
        String sql = getUpdateSql(specifyTaxon, denormalize("basionym",basionym));

        update(sql);
    }

    private AsaTaxon parse(String[] columns) throws LocalException
    {
        if (columns.length < 2)
        {
            throw new LocalException("Not enough columns");
        }
        
        AsaTaxon asaTaxon = new AsaTaxon();
        try
        {
            asaTaxon.setId(         SqlUtils.parseInt( columns[0] ));
            asaTaxon.setBasionymId( SqlUtils.parseInt( columns[1] ));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        return asaTaxon;
    }
    
    private Taxon lookupTaxon(Integer asaTaxonId) throws LocalException
    {
        return taxonLookup.getById(asaTaxonId);
    }

    private String getUpdateSql(Taxon taxon, String basionym)
    {
    	return SqlUtils.getAppendUpdateSql("taxon", "Remarks", basionym, "TaxonID", taxon.getId());
    }
}
