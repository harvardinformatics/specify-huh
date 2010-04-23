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
import edu.harvard.huh.asa.SpecimenItem;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AccessionLookup;
import edu.harvard.huh.asa2specify.lookup.GeoUnitLookup;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionPreparation;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;

public class InGeoBatchLoader extends CsvToSqlLoader
{
    private AccessionLookup accessionLookup;
    private GeoUnitLookup   geographyLookup;
    
    private final PrepType prepType;
    
    public InGeoBatchLoader(File csvFile,
                            Statement sqlStatement,
                            AccessionLookup accessionLookup,
                            GeoUnitLookup geographyLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.accessionLookup = accessionLookup;
        this.geographyLookup   = geographyLookup;
        
        Integer collectionId = getCollectionId(null);
        String container = SpecimenItem.toString(SpecimenItem.CONTAINER_TYPE.Lot);

        String sql = "select PrepTypeID from preptype where CollectionID=" + collectionId + " and Name=" + SqlUtils.sqlString(container);
        Integer prepTypeId = queryForInt(sql);
        if (prepTypeId == null) throw new LocalException("Couldn't find prep type for " + container);

        prepType = new PrepType();
        prepType.setPrepTypeId(prepTypeId);
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

        Preparation prep = getPreparation();
        String sql = getInsertSql(prep);
        Integer preparationId = insert(sql);
        prep.setPreparationId(preparationId);
        
        AccessionPreparation accessionPrep = getAccessionPreparation(inGeoBatch, prep);

        sql = getInsertSql(accessionPrep);
        insert(sql);
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

    private Preparation getPreparation() throws LocalException
    {
        Preparation preparation = new Preparation();
        
        // CollectionMemberID
        Integer collectionMemberId = this.getCollectionId(null);
        preparation.setCollectionMemberId(collectionMemberId);

        // PrepType
        preparation.setPrepType(prepType);
        
        return preparation;
    }

    private AccessionPreparation getAccessionPreparation(InGeoBatch inGeoBatch, Preparation preparation) throws LocalException
    {
        AccessionPreparation accessionPrep = new AccessionPreparation();
        
        // Accession
        Integer transactionId = inGeoBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        Accession accession = lookupAccession(transactionId);
        accessionPrep.setAccession(accession);
        
        // DescriptionOfMaterial
        Integer geoUnitId = inGeoBatch.getGeoUnitId();
        checkNull(geoUnitId, "geo unit id");
        
        if (geoUnitId == GeoUnit.CultRegionId) accessionPrep.setDescriptionOfMaterial(GeoUnit.Cultivated);
        else if (geoUnitId == GeoUnit.MiscRegionId) accessionPrep.setDescriptionOfMaterial(GeoUnit.Miscellaneous);
        
        // Discipline
        accessionPrep.setDiscipline(getBotanyDiscipline());
        
        // DiscardCount
        int discardCount = inGeoBatch.getDiscardCount();
        accessionPrep.setDiscardCount((short) discardCount);
        
        // DistributeCount
        int distributeCount = inGeoBatch.getDistributeCount();
        accessionPrep.setDistributeCount((short) distributeCount);
        
        // Geography
        Geography geography = GeoUnit.NullGeography;
        if (geoUnitId != null && geoUnitId != GeoUnit.CultRegionId && geoUnitId != GeoUnit.MiscRegionId) geography = lookupGeography(geoUnitId);
        accessionPrep.setGeography(geography);
        
        // ItemCount
        int itemCount = inGeoBatch.getItemCount();
        accessionPrep.setItemCount((short) itemCount);
        
        // NonSpecimenCount
        int nonSpecimenCount = inGeoBatch.getNonSpecimenCount();
        accessionPrep.setNonSpecimenCount((short) nonSpecimenCount);
        
        // Preparation
        accessionPrep.setPreparation(preparation);

        // ReturnCount
        int returns = inGeoBatch.getReturnCount();
        accessionPrep.setReturnCount((short) returns);
        
        // TypeCount
        int typeCount = inGeoBatch.getTypeCount();
        accessionPrep.setTypeCount((short) typeCount);
        
        return accessionPrep;
    }
    
    private Accession lookupAccession(Integer transactionId) throws LocalException
    {
        return accessionLookup.getById(transactionId);
    }
    
    private Geography lookupGeography(Integer transactionId) throws LocalException
    {
        return geographyLookup.getById(transactionId);
    }
    
    private String getInsertSql(Preparation preparation) throws LocalException
    {
        String fieldNames = "CollectionMemberID, PrepTypeID, TimestampCreated, Version";

        String[] values = new String[4];
        
        values[0]  = SqlUtils.sqlString( preparation.getCollectionMemberId());
        values[1]  = SqlUtils.sqlString( preparation.getPrepType().getId());
        values[2]  = SqlUtils.now();
        values[3]  = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("preparation", fieldNames, values);
    }

    private String getInsertSql(AccessionPreparation accessionPrep)
    {
        String fieldNames = "AccessionID, DescriptionOfMaterial, DiscardCount, DisciplineID, " +
                            "DistributeCount, GeographyID, ItemCount, NonSpecimenCount, PreparationID, " +
                            "ReturnCount, TimestampCreated, TypeCount, Version";
        
        String[] values = new String[13];
        
        values[0]  = SqlUtils.sqlString( accessionPrep.getAccession().getId());
        values[1]  = SqlUtils.sqlString( accessionPrep.getDescriptionOfMaterial());
        values[2]  = SqlUtils.sqlString( accessionPrep.getDiscardCount());
        values[3]  = SqlUtils.sqlString( accessionPrep.getDiscipline().getId());
        values[4]  = SqlUtils.sqlString( accessionPrep.getDistributeCount());
        values[5]  = SqlUtils.sqlString( accessionPrep.getGeography().getId());
        values[6]  = SqlUtils.sqlString( accessionPrep.getItemCount());
        values[7]  = SqlUtils.sqlString( accessionPrep.getNonSpecimenCount());
        values[8]  = SqlUtils.sqlString( accessionPrep.getPreparation().getId());
        values[9]  = SqlUtils.sqlString( accessionPrep.getReturnCount());
        values[10] = SqlUtils.now();
        values[11] = SqlUtils.sqlString( accessionPrep.getTypeCount());
        values[12] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("accessionpreparation", fieldNames, values);
    }
}
