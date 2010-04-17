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

public class OutGeoBatchLoader extends CsvToSqlLoader
{
    private OutgoingExchangeLookup outExchangeLookup;
    private OutgoingGiftLookup outGiftLookup;
    private GeoUnitLookup geoUnitLookup;
    
    private static final Geography NullGeography = new Geography();
    
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
            ExchangeOutPreparation exchOutPrep = getExchangeOutPreparation(outGeoBatch);
            
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

        // Discipline
        giftPrep.setDiscipline(getBotanyDiscipline());
        
        // Geography
        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        Geography geography = NullGeography;
        
        if (geoUnitId != null) geography = lookupGeography(geoUnitId);
        giftPrep.setGeography(geography);
        
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
    
    private ExchangeOutPreparation getExchangeOutPreparation(OutGeoBatch outGeoBatch) throws LocalException
    {
        ExchangeOutPreparation exchOutPrep = new ExchangeOutPreparation();

        // Discipline
        exchOutPrep.setDiscipline(getBotanyDiscipline());
        
        // Geography
        Integer geoUnitId = outGeoBatch.getGeoUnitId();
        Geography geography = NullGeography;
        
        if (geoUnitId != null) geography = lookupGeography(geoUnitId);
        exchOutPrep.setGeography(geography);
        
        // Gift
        Integer transactionId = outGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        ExchangeOut exchangeOut = lookupExchange(transactionId);
        exchOutPrep.setExchangeOut(exchangeOut);
        
        // NonSpecimenCount
        int nonSpecimenCount = outGeoBatch.getNonSpecimenCount();
        exchOutPrep.setNonSpecimenCount(nonSpecimenCount);

        // Quantity (itemCount + typeCount + nonSpecimenCount)
        int quantity = outGeoBatch.getBatchQuantity();
        exchOutPrep.setQuantity(quantity);
        
        // TypeCount
        int typeCount = outGeoBatch.getTypeCount();
        exchOutPrep.setTypeCount(typeCount);
        
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
        String fieldNames = "DisciplineID, GeographyID, GiftID, " +
                            "NonSpecimenCount, Quantity, TimestampCreated, TypeCount, " +
                            "Version";
        
        String[] values = new String[8];
        
        values[0] = SqlUtils.sqlString( giftPrep.getDescriptionOfMaterial());
        values[1] = SqlUtils.sqlString( giftPrep.getGeography().getId());
        values[2] = SqlUtils.sqlString( giftPrep.getDiscipline().getId());
        values[3] = SqlUtils.sqlString( giftPrep.getGift().getId());
        values[4] = SqlUtils.sqlString( giftPrep.getNonSpecimenCount());
        values[5] = SqlUtils.sqlString( giftPrep.getQuantity());
        values[6] = SqlUtils.now();
        values[7] = SqlUtils.sqlString( giftPrep.getTypeCount());
        values[8] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("giftpreparation", fieldNames, values);
    }
    
    private String getInsertSql(ExchangeOutPreparation exchOutPrep)
    {
        String fieldNames = "DisciplineID, ExchangeOutID, GeographyID, " +
                            "NonSpecimenCount, Quantity, TimestampCreated, TypeCount, " +
                            "Version";
        
        String[] values = new String[8];
        
        values[0] = SqlUtils.sqlString( exchOutPrep.getDiscipline().getId());
        values[1] = SqlUtils.sqlString( exchOutPrep.getExchangeOut().getId());
        values[2] = SqlUtils.sqlString( exchOutPrep.getGeography().getId());
        values[3] = SqlUtils.sqlString( exchOutPrep.getNonSpecimenCount());
        values[4] = SqlUtils.sqlString( exchOutPrep.getQuantity());
        values[5] = SqlUtils.now();
        values[6] = SqlUtils.sqlString( exchOutPrep.getTypeCount());
        values[7] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("exchangeoutpreparation", fieldNames, values);
    }
}
