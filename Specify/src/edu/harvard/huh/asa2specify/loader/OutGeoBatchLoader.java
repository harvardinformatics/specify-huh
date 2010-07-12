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
import edu.ku.brc.specify.datamodel.ExchangeOutPreparation;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftPreparation;
import edu.ku.brc.specify.datamodel.Preparation;

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
        
        // Preparation
        Preparation prep = getPreparation(outGeoBatch);
        String sql = getInsertSql(prep);
        Integer preparationId = insert(sql);
        prep.setPreparationId(preparationId);
        
        TYPE type = outGeoBatch.getType();
        
        if (type.equals(TYPE.OutExchange) || type.equals(TYPE.OutSpecial))
        {
            ExchangeOutPreparation exchOutPrep = getExchangeOutPreparation(outGeoBatch, prep);
            
            sql = getInsertSql(exchOutPrep);
            insert(sql);
        }
        else if (type.equals(TYPE.OutGift))
        {
            GiftPreparation giftPrep = getGiftPreparation(outGeoBatch, prep);
            
            sql = getInsertSql(giftPrep);
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

    private GiftPreparation getGiftPreparation(OutGeoBatch outGeoBatch, Preparation prep) throws LocalException
    {
        GiftPreparation giftPrep = new GiftPreparation();

        // DescriptionOfMaterial
        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        if (geoUnitId == GeoUnit.CultRegionId) giftPrep.setDescriptionOfMaterial(GeoUnit.Cultivated);
        else if (geoUnitId == GeoUnit.MiscRegionId) giftPrep.setDescriptionOfMaterial(GeoUnit.Miscellaneous);
        
        // Discipline
        giftPrep.setDiscipline(getBotanyDiscipline());
        
        // Gift
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        Gift gift = lookupGift(transactionId);
        giftPrep.setGift(gift);
        
        // ItemCount
        int itemCount = outGeoBatch.getItemCount();
        giftPrep.setItemCount(itemCount);
        
        // NonSpecimenCount
        int nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
        giftPrep.setNonSpecimenCount(nonSpecimenCount);
        
        // Preparation
        giftPrep.setPreparation(prep);
        
        // TypeCount
        int typeCount = outGeoBatch.getTypeCount();
        giftPrep.setTypeCount(typeCount);
        
        return giftPrep;
    }
    
    private ExchangeOutPreparation getExchangeOutPreparation(OutGeoBatch outGeoBatch, Preparation prep) throws LocalException
    {
        ExchangeOutPreparation exchOutPrep = new ExchangeOutPreparation();

        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        if (geoUnitId == GeoUnit.CultRegionId) exchOutPrep.setDescriptionOfMaterial(GeoUnit.Cultivated);
        else if (geoUnitId == GeoUnit.MiscRegionId) exchOutPrep.setDescriptionOfMaterial(GeoUnit.Miscellaneous);
        
        // Discipline
        exchOutPrep.setDiscipline(getBotanyDiscipline());
        
        // Gift
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        ExchangeOut exchangeOut = lookupExchange(transactionId);
        exchOutPrep.setExchangeOut(exchangeOut);
        
        // ItemCount
        int itemCount = outGeoBatch.getItemCount();
        exchOutPrep.setItemCount(itemCount);

        // NonSpecimenCount
        int nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
        exchOutPrep.setNonSpecimenCount(nonSpecimenCount);
        
        // Preparation
        exchOutPrep.setPreparation(prep);

        // TypeCount
        int typeCount = outGeoBatch.getTypeCount();
        exchOutPrep.setTypeCount(typeCount);
        
        return exchOutPrep;
    }
    
    private Preparation getPreparation(OutGeoBatch outGeoBatch) throws LocalException
    {
        Preparation preparation = new Preparation();
        
        // CollectionMemberID
        Integer collectionMemberId = this.getCollectionId(null);
        preparation.setCollectionMemberId(collectionMemberId);

        // CountAmt
        preparation.setCountAmt(1);
        
        // Geography
        Geography geography = CsvToSqlLoader.NullGeography;
        
        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        
        if (geoUnitId != null && geoUnitId != GeoUnit.CultRegionId && geoUnitId != GeoUnit.MiscRegionId) geography = lookupGeography(geoUnitId);
        
        preparation.setGeography(geography);
        
        // PrepType
        preparation.setPrepType(getLotPrepType());
        
        return preparation;
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
        String fieldNames = "DescriptionOfMaterial, DisciplineID, GiftID, " +
                            "ItemCount, NonSpecimenCount, PreparationID, TimestampCreated, TypeCount, Version";
        
        String[] values = new String[9];
        
        values[0] = SqlUtils.sqlString( giftPrep.getDescriptionOfMaterial());
        values[1] = SqlUtils.sqlString( giftPrep.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( giftPrep.getGift().getId());
        values[3] = SqlUtils.sqlString( giftPrep.getItemCount());
        values[4] = SqlUtils.sqlString( giftPrep.getNonSpecimenCount());
        values[5] = SqlUtils.sqlString( giftPrep.getPreparation().getId());
        values[6] = SqlUtils.now();
        values[7] = SqlUtils.sqlString( giftPrep.getTypeCount());
        values[8] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("giftpreparation", fieldNames, values);
    }
    
    private String getInsertSql(ExchangeOutPreparation exchOutPrep)
    {
        String fieldNames = "DescriptionOfMaterial, DisciplineID, ExchangeOutID, ItemCount, " +
                            "NonSpecimenCount, PreparationID, TimestampCreated, TypeCount, Version";
        
        String[] values = new String[9];
        
        values[0] = SqlUtils.sqlString( exchOutPrep.getDescriptionOfMaterial());
        values[1] = SqlUtils.sqlString( exchOutPrep.getDiscipline().getId());
        values[2] = SqlUtils.sqlString( exchOutPrep.getExchangeOut().getId());
        values[3] = SqlUtils.sqlString( exchOutPrep.getItemCount());
        values[4] = SqlUtils.sqlString( exchOutPrep.getNonSpecimenCount());
        values[5] = SqlUtils.sqlString( exchOutPrep.getPreparation().getId());
        values[6] = SqlUtils.now();
        values[7] = SqlUtils.sqlString( exchOutPrep.getTypeCount());
        values[8] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("exchangeoutpreparation", fieldNames, values);
    }
}
