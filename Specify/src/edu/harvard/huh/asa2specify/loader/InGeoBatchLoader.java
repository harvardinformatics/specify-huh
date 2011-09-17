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

import edu.harvard.huh.asa.GeoUnit;
import edu.harvard.huh.asa.InGeoBatch;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AccessionLookup;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Geography;

public class InGeoBatchLoader extends CsvToSqlLoader
{
    private AccessionLookup accessionLookup;
    private GeoUnitLookup   geographyLookup;
    
    public InGeoBatchLoader(File csvFile,
                            Statement sqlStatement,
                            AccessionLookup accessionLookup,
                            GeoUnitLookup geographyLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.accessionLookup = accessionLookup;
        this.geographyLookup   = geographyLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        InGeoBatch inGeoBatch = parse(columns);
        
        Integer inGeoBatchId = inGeoBatch.getId();
        setCurrentRecordId(inGeoBatchId);

        // ExchangeOut/Gift/Loan (Loan, OutExchange, OutGift, OutSpecialExch)
        Integer transactionId = inGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");

        // Accession
        Accession accession = lookupAccession(transactionId);
        
        String batchNote = getAccessionBatchNote(inGeoBatch);

        String sql = getUpdateSql(accession, batchNote);
        update(sql);
    }

    private InGeoBatch parse(String[] columns) throws LocalException
    {
        InGeoBatch inGeoBatch = new InGeoBatch();
        
        if (columns.length < 10)
        {
            throw new LocalException("Not enough columns");
        }
        
        try
        {
            inGeoBatch.setId(               SqlUtils.parseInt( columns[0] ));
            inGeoBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
            inGeoBatch.setItemCount(        SqlUtils.parseInt( columns[2] ));
            inGeoBatch.setTypeCount(        SqlUtils.parseInt( columns[3] ));
            inGeoBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[4] ));
            inGeoBatch.setGeoUnitId(        SqlUtils.parseInt( columns[5] ));
            inGeoBatch.setDiscardCount(     SqlUtils.parseInt( columns[6] ));
            inGeoBatch.setDistributeCount(  SqlUtils.parseInt( columns[7] ));
            inGeoBatch.setReturnCount(      SqlUtils.parseInt( columns[8] ));
            inGeoBatch.setCost(           SqlUtils.parseFloat( columns[9] ));  // all zero in Asa
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return inGeoBatch;
    }

    private String getAccessionBatchNote(InGeoBatch inGeoBatch) throws LocalException
    {         
        // DescriptionOfMaterial
        String geoUnit = null;
        Integer geoUnitId = inGeoBatch.getGeoUnitId();
        if (geoUnitId == GeoUnit.CultRegionId)
        {
        	geoUnit = GeoUnit.Cultivated;
        }
        else if (geoUnitId == GeoUnit.MiscRegionId)
        {
        	geoUnit = GeoUnit.Miscellaneous;
        }
        else
        {
        	Geography geography = lookupGeography(geoUnitId);
        	geoUnit = this.getString("taxon", "FullName", "GeographyID", geography.getId());
        }
                
        // ... item count
        String items = denormalize("items", String.valueOf(inGeoBatch.getItemCount()));
        
        // ... type count
        String types = denormalize("types", String.valueOf(inGeoBatch.getTypeCount()));
        
        // ... non-specimen count
        String nonSpecimens = denormalize("non-specimens", String.valueOf(inGeoBatch.getNonSpecimenCount()));

        // ... discard count
        String discards = denormalize("discarded", String.valueOf(inGeoBatch.getDiscardCount()));
        
        // ... distribute count
        String distributes = denormalize("distributed", String.valueOf(inGeoBatch.getDistributeCount()));
        
        // ... return count
        String returns = denormalize("returned", String.valueOf(inGeoBatch.getReturnCount()));
        
        return denormalize("batch", concatenate(geoUnit, items, types, nonSpecimens, discards, distributes, returns));
    }
    
    private Accession lookupAccession(Integer transactionId) throws LocalException
    {
        return accessionLookup.getById(transactionId);
    }
    
    private Geography lookupGeography(Integer transactionId) throws LocalException
    {
        return geographyLookup.getById(transactionId);
    }

    private String getUpdateSql(Accession accession, String batchNote)
    {
    	return SqlUtils.getAppendUpdateSql("accession", "Remarks", batchNote, "AccessionID", accession.getId());
    }
}
