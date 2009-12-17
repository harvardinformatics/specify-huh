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

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.OutGeoBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftPreparation;

public class OutGeoBatchLoader extends CsvToSqlLoader
{
    private OutgoingGiftLookup outGiftLookup;
    
    public OutGeoBatchLoader(File csvFile,
                             Statement sqlStatement,
                             OutgoingGiftLookup outGiftLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.outGiftLookup = outGiftLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        OutGeoBatch outGeoBatch = parse(columns);
        
        Integer outGeoBatchId = outGeoBatch.getId();
        setCurrentRecordId(outGeoBatchId);

        GiftPreparation giftPrep = getGiftPreparation(outGeoBatch);
        
        String sql = getInsertSql(giftPrep);
        insert(sql);
    }

    private OutGeoBatch parse(String[] columns) throws LocalException
    {
        OutGeoBatch outGeoBatch = new OutGeoBatch();
        
        if (columns.length < 8)
        {
            throw new LocalException("Not enough columns");
        }
        
        try
        {
            outGeoBatch.setId(               SqlUtils.parseInt( columns[0] ));
            outGeoBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
            outGeoBatch.setType(         Transaction.parseType( columns[2] ));
            outGeoBatch.setCollectionCode(                      columns[3] );
            outGeoBatch.setItemCount(        SqlUtils.parseInt( columns[4] ));
            outGeoBatch.setTypeCount(        SqlUtils.parseInt( columns[5] ));
            outGeoBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[6] ));
            outGeoBatch.setGeoUnit(                             columns[7] );
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
            throw new LocalException("Couldn't parse carrier/method field", e);
        }
        
        return outGeoBatch;
    }

    private GiftPreparation getGiftPreparation(OutGeoBatch outGeoBatch) throws LocalException
    {
        GiftPreparation giftPrep = new GiftPreparation();

        // DescriptionOfMaterial (geoUnit)
        String geoUnit = outGeoBatch.getGeoUnit();
        if (geoUnit != null) geoUnit = truncate(geoUnit, 255, "src geography");
        giftPrep.setDescriptionOfMaterial(geoUnit);
        
        // Discipline
        giftPrep.setDiscipline(getBotanyDiscipline());
        
        // Gift
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        Gift gift = lookupGift(transactionId);
        giftPrep.setGift(gift);
        
        // NonSpecimenCount
        int nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
        giftPrep.setNonSpecimenCount(nonSpecimenCount);

        // Quantity (itemCount + typeCount + nonSpecimenCount)
        int quantity = outGeoBatch.getBatchQuantity();
        giftPrep.setQuantity(quantity);
        
        // TypeCount
        int typeCount = outGeoBatch.getTypeCount();
        giftPrep.setTypeCount(typeCount);
        
        return giftPrep;
    }
    
    private Gift lookupGift(Integer transactionId) throws LocalException
    {
        return outGiftLookup.getById(transactionId);
    }
    
    private String getInsertSql(GiftPreparation giftPrep)
    {
        String fieldNames = "DescriptionOfMaterial, DisciplineID, GiftID, " +
                            "NonSpecimenCount, Quantity, TimestampCreated, TypeCount, " +
                            "Version";
        
        String[] values = new String[8];
        
        values[0] = SqlUtils.sqlString( giftPrep.getDescriptionOfMaterial());
        values[1] = SqlUtils.sqlString( giftPrep.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( giftPrep.getGift().getId());
        values[3] = SqlUtils.sqlString( giftPrep.getNonSpecimenCount());
        values[4] = SqlUtils.sqlString( giftPrep.getQuantity());
        values[5] = SqlUtils.now();
        values[6] = SqlUtils.sqlString( giftPrep.getTypeCount());
        values[7] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("giftpreparation", fieldNames, values);
    }
}
