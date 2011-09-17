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
import edu.harvard.huh.asa.GeoUnit;
import edu.harvard.huh.asa.OutGeoBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingExchangeLookup;
import edu.harvard.huh.asa2specify.lookup.OutgoingGiftLookup;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.ExchangeOutPrep;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftPreparation;

public class OutGeoBatchLoader extends CsvToSqlLoader
{
    private OutgoingExchangeLookup outExchangeLookup;
    private OutgoingGiftLookup outGiftLookup;
    private GeoUnitLookup geoUnitLookup;
    
    public OutGeoBatchLoader(File csvFile,
                             Statement sqlStatement,
                             OutgoingExchangeLookup outExchangeLookup,
                             OutgoingGiftLookup outGiftLookup,
                             GeoUnitLookup geoUnitLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.outExchangeLookup = outExchangeLookup;
        this.outGiftLookup = outGiftLookup;
        this.geoUnitLookup = geoUnitLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        OutGeoBatch outGeoBatch = parse(columns);
        
        Integer outGeoBatchId = outGeoBatch.getId();
        setCurrentRecordId(outGeoBatchId);

        // ExchangeOut/Gift/Loan (Loan, OutExchange, OutGift, OutSpecialExch)
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        TYPE type = outGeoBatch.getType();
        
        if (type.equals(TYPE.OutExchange) || type.equals(TYPE.OutSpecial))
        {
            ExchangeOutPrep exchOutPrep = getExchangeOutPreparation(outGeoBatch);
            
            String sql = getInsertSql(exchOutPrep);
            insert(sql);
        }
        else if (type.equals(TYPE.OutGift))
        {
            GiftPreparation giftPrep = getGiftPreparation(outGeoBatch);
            
            String sql = getInsertSql(giftPrep);
            insert(sql);
        }
        else
        {
            throw new LocalException("Invalid transaction type " + Transaction.toString(type));
        }
    }

    private OutGeoBatch parse(String[] columns) throws LocalException
    {
        OutGeoBatch outGeoBatch = new OutGeoBatch();
        
        if (columns.length < 9)
        {
            throw new LocalException("Not enough columns");
        }
        
        try
        {
            outGeoBatch.setId(                     SqlUtils.parseInt( columns[0] ));
            outGeoBatch.setTransactionId(          SqlUtils.parseInt( columns[1] ));
            outGeoBatch.setType(               Transaction.parseType( columns[2] ));
            outGeoBatch.setRequestType( Transaction.parseRequestType( columns[3] ));
            outGeoBatch.setCollectionCode(                            columns[4] );
            outGeoBatch.setItemCount(              SqlUtils.parseInt( columns[5] ));
            outGeoBatch.setTypeCount(              SqlUtils.parseInt( columns[6] ));
            outGeoBatch.setNonSpecimenCount(       SqlUtils.parseInt( columns[7] ));
            outGeoBatch.setGeoUnitId(              SqlUtils.parseInt( columns[8] ));
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

        // DescriptionOfMaterial
        String descriptionOfMaterial = null;
        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        if (geoUnitId == GeoUnit.CultRegionId)
        {
        	descriptionOfMaterial = GeoUnit.Cultivated;
        }
        else if (geoUnitId == GeoUnit.MiscRegionId)
        {
        	descriptionOfMaterial = GeoUnit.Miscellaneous;
        }
        else
        {
        	Geography geography = lookupGeography(geoUnitId);
        	descriptionOfMaterial = this.getString("taxon", "FullName", "GeographyID", geography.getId());
        }
        giftPrep.setDescriptionOfMaterial(descriptionOfMaterial);
        
        // Discipline
        giftPrep.setDiscipline(getBotanyDiscipline());
        
        // Gift
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        Gift gift = lookupGift(transactionId);
        giftPrep.setGift(gift);

        // InComments
        // ... item count
        int itemCount = outGeoBatch.getItemCount();
        String items = denormalize("items", String.valueOf(itemCount));
        
        // ... type count
        int typeCount = outGeoBatch.getTypeCount();
        String types = denormalize("types", String.valueOf(typeCount));
        
        // ... non-specimen count
        int nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
        String nonSpecimens = denormalize("non-specimens", String.valueOf(nonSpecimenCount));
        
        giftPrep.setInComments(concatenate(items, types, nonSpecimens));

        // Quantity
        giftPrep.setQuantity(itemCount + typeCount + nonSpecimenCount);
        
        return giftPrep;
    }
    
    private ExchangeOutPrep getExchangeOutPreparation(OutGeoBatch outGeoBatch) throws LocalException
    {
        ExchangeOutPrep exchOutPrep = new ExchangeOutPrep();

        // Comments
        // ... item count
        int itemCount = outGeoBatch.getItemCount();
        String items = denormalize("items", String.valueOf(itemCount));
        
        // ... type count
        int typeCount = outGeoBatch.getTypeCount();
        String types = denormalize("types", String.valueOf(typeCount));
        
        // ... non-specimen count
        int nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
        String nonSpecimens = denormalize("non-specimens", String.valueOf(nonSpecimenCount));
        
        exchOutPrep.setComments(concatenate(items, types, nonSpecimens));
        
        // DescriptionOfMaterial
        String descriptionOfMaterial = null;
        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        if (geoUnitId == GeoUnit.CultRegionId)
        {
        	descriptionOfMaterial = GeoUnit.Cultivated;
        }
        else if (geoUnitId == GeoUnit.MiscRegionId)
        {
        	descriptionOfMaterial = GeoUnit.Miscellaneous;
        }
        else
        {
        	Geography geography = lookupGeography(geoUnitId);
        	descriptionOfMaterial = this.getString("taxon", "FullName", "GeographyID", geography.getId());
        }
        exchOutPrep.setDescriptionOfMaterial(descriptionOfMaterial);
        
        // Discipline
        exchOutPrep.setDiscipline(getBotanyDiscipline());
        
        // Gift
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        ExchangeOut exchangeOut = lookupExchange(transactionId);
        exchOutPrep.setExchangeOut(exchangeOut);
        
        // Quantity
        exchOutPrep.setQuantity(itemCount + typeCount + nonSpecimenCount);
        
        return exchOutPrep;
    }

    private Gift lookupGift(Integer transactionId) throws LocalException
    {
        return outGiftLookup.getById(transactionId);
    }
    
    private ExchangeOut lookupExchange(Integer transactionId) throws LocalException
    {
        return outExchangeLookup.getById(transactionId);
    }
    
    private Geography lookupGeography(Integer geoUnitId) throws LocalException
    {
        return geoUnitLookup.getById(geoUnitId);
    }

    private String getInsertSql(GiftPreparation giftPrep)
    {
        String fieldNames = "DescriptionOfMaterial, DisciplineID, GiftID, InComments, " +
                            "Quantity, TimestampCreated, Version";
        
        String[] values = {
        		SqlUtils.sqlString( giftPrep.getDescriptionOfMaterial()),
        		SqlUtils.sqlString( giftPrep.getDiscipline().getId()),
        		SqlUtils.sqlString( giftPrep.getGift().getId()),
        		SqlUtils.sqlString( giftPrep.getInComments()),
        		SqlUtils.sqlString( giftPrep.getQuantity()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("giftpreparation", fieldNames, values);
    }
    
    private String getInsertSql(ExchangeOutPrep exchOutPrep)
    {
        String fieldNames = "Comments, DescriptionOfMaterial, DisciplineID, ExchangeOutID, " +
                            "Quantity, TimestampCreated, Version";
        
        String[] values = {
        		SqlUtils.sqlString( exchOutPrep.getComments()),
        		SqlUtils.sqlString( exchOutPrep.getDescriptionOfMaterial()),
        		SqlUtils.sqlString( exchOutPrep.getDiscipline().getId()),
        		SqlUtils.sqlString( exchOutPrep.getExchangeOut().getId()),
        		SqlUtils.sqlString( exchOutPrep.getQuantity()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("exchangeoutprep", fieldNames, values);
    }
}
