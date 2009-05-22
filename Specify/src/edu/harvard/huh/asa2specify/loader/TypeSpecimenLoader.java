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

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.AsaDetermination;
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.TypeSpecimen;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonCitation;

public class TypeSpecimenLoader extends CsvToSqlLoader
{

    public TypeSpecimenLoader(File csvFile, Statement sqlStatement)
    {
        super(csvFile, sqlStatement);
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        TypeSpecimen typeSpecimen = parse(columns);

        Determination determination = getDetermination(typeSpecimen);
        String sql = getInsertSql(determination);
        insert(sql);
        
        // create a taxon citations
        Integer publicationId = typeSpecimen.getNle1PublicationId();
        Integer designatorId = typeSpecimen.getNle1DesignatorId();
        


 
        TaxonCitation taxonCitation1 = new TaxonCitation();
        
        
        String guid = String.valueOf(publicationId);

        Integer referenceWorkId = getIntByField("referencework", "ReferenceWorkID", "GUID", guid);
        ReferenceWork referenceWork = new ReferenceWork();
        referenceWork.setReferenceWorkId(referenceWorkId);

        taxonCitation1.setReferenceWork(referenceWork);
        taxonCitation1.setTaxon(determination.getTaxon());
            
        sql = getInsertSql(taxonCitation1);
        insert(sql);
    }

    private TypeSpecimen parse(String[] columns) throws LocalException
    {
        if (columns.length < 18)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        TypeSpecimen typeSpecimen = new TypeSpecimen();
        
        try
        {
            typeSpecimen.setId(             Integer.parseInt( StringUtils.trimToNull( columns[0] )));
            typeSpecimen.setSpecimenId(     Integer.parseInt( StringUtils.trimToNull( columns[1] )));
            typeSpecimen.setTaxonId(        Integer.parseInt( StringUtils.trimToNull( columns[2] )));
            typeSpecimen.setTypeStatus(                       StringUtils.trimToNull( columns[3] ));
            typeSpecimen.setConditionality(                   StringUtils.trimToNull( columns[4] ));
            typeSpecimen.setIsFragment( Boolean.parseBoolean( StringUtils.trimToNull( columns[5] )));

            BDate bdate = new BDate();
            typeSpecimen.setDate( bdate );

            String startYearStr = StringUtils.trimToNull( columns[6] );
            if (startYearStr != null)
            {
                bdate.setStartYear( Integer.parseInt( startYearStr ));
            }

            String startMonthStr = StringUtils.trimToNull( columns[7] );
            if (startMonthStr != null)
            {
                bdate.setStartMonth( Integer.parseInt( startMonthStr ));
            }
            
            String startDayStr = StringUtils.trimToNull( columns[8] );
            if (startDayStr != null)
            {
                bdate.setStartDay( Integer.parseInt( startDayStr ));
            }
            
            String nle1DesignatorIdStr = StringUtils.trimToNull( columns[9] );
            if (nle1DesignatorIdStr != null)
            {
                typeSpecimen.setNle1DesignatorId( Integer.parseInt( nle1DesignatorIdStr ));
            }
            
            String nle1PublicationIdStr = StringUtils.trimToNull( columns[10] );
            if (nle1PublicationIdStr != null)
            {
                typeSpecimen.setNle1PublicationId( Integer.parseInt( nle1PublicationIdStr ));
            }
            
            typeSpecimen.setNle1Collation( StringUtils.trimToNull( columns[11] ));
            typeSpecimen.setNle1Date(      StringUtils.trimToNull( columns[12] ));

            String nle2DesignatorIdStr = StringUtils.trimToNull( columns[13]  );
            if (nle2DesignatorIdStr != null)
            {
                typeSpecimen.setNle2DesignatorId( Integer.parseInt( nle2DesignatorIdStr ));
            }
            
            String nle2PublicationIdStr = StringUtils.trimToNull( columns[14]  );
            if (nle2PublicationIdStr != null)
            {
                typeSpecimen.setNle2PublicationId( Integer.parseInt( nle2PublicationIdStr ));
            }
            
            typeSpecimen.setNle2Collation(                   StringUtils.trimToNull( columns[15] ));
            typeSpecimen.setNle2Date(                        StringUtils.trimToNull( columns[16] ));
            typeSpecimen.setRemarks( SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[17] )));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return typeSpecimen;
    }
    
    private Determination getDetermination(TypeSpecimen typeSpecimen) throws LocalException
    {
        Determination determination = new Determination();
        
        // CollectionObject
        Integer specimenId = typeSpecimen.getSpecimenId();
        if (specimenId == null)
        {
            throw new LocalException("No specimen id");
        }
        
        String guid = String.valueOf(specimenId);
        
        Integer collectionObjectId = getIntByField("collectionobject", "CollectionObjectID", "GUID", guid);

        CollectionObject collectionObject = new CollectionObject();
        collectionObject.setCollectionObjectId(collectionObjectId);

        determination.setCollectionObject(collectionObject);

        // CollectionMemberID
        Integer collectionMemberId = getIntByField("collectionobject", "CollectionMemberID", "GUID", guid);
        determination.setCollectionMemberId(collectionMemberId);

        // Taxon
        Integer asaTaxonId = typeSpecimen.getTaxonId();
        if (asaTaxonId == null)
        {
            throw new LocalException("No taxon id");
        }

        String taxonSerNumber = String.valueOf(asaTaxonId);
        
        Integer taxonId = getIntByField("taxon", "TaxonID", "TaxonomicSerialNumber", taxonSerNumber);

        Taxon taxon = new Taxon();
        taxon.setTaxonId(taxonId);
        
        determination.setTaxon( taxon ); 
        
        // TypeStatusName
        String typeStatus = typeSpecimen.getTypeStatus();
        determination.setTypeStatusName(typeStatus);
        
        // Conditionality
        String conditionality = typeSpecimen.getConditionality();
        if ( conditionality != null && conditionality.length() > 50 )
        {
            warn("Truncating confidence", typeSpecimen.getId(), conditionality);
        }
        determination.setConfidence(conditionality);
        
        // YesNo2 (isFragment)
        determination.setYesNo1( typeSpecimen.isFragment() );
        
        // DeterminedDate
        BDate bdate = typeSpecimen.getDate();

        Integer startYear  = bdate.getStartYear();
        Integer startMonth = bdate.getStartMonth();
        Integer startDay   = bdate.getStartDay();

        // DeterminedDate and DeterminedDatePrecision
        if ( DateUtils.isValidSpecifyDate( startYear, startMonth, startDay ) )
        {
            determination.setDeterminedDate( DateUtils.getSpecifyStartDate( bdate ) );
            determination.setDeterminedDatePrecision( DateUtils.getDatePrecision( startYear, startMonth, startDay ) );
        }
        else
        {
            warn("Invalid determination date", typeSpecimen.getId(),
                    String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay));
        }

        // Text1 (verifiedBy) TODO: normalize determiner name to botanist agent?  does this even go here?
        String verifiedBy = typeSpecimen.getVerifiedBy();
        determination.setText1( verifiedBy );
        
        // Remarks TODO: Maureen, check your notes on this field and label_text
        String remarks = typeSpecimen.getRemarks();
        determination.setRemarks(remarks);

        return determination;
    }
        
    private String getInsertSql(Determination determination)
    {
        String fieldNames = "CollectionObjectID, CollectionMemberID, TaxonID, TypeStatusName, Confidence, YesNo2, DeterminedDate, " +
        		            "DeterminedDatePrecision, Text1, Remarks, CollectionMemberID, TimestampCreated";

        String[] values = new String[12];
        
        values[0]  = SqlUtils.sqlString( determination.getCollectionObject().getId());
        values[1]  = SqlUtils.sqlString( determination.getCollectionMemberId());
        values[2]  = SqlUtils.sqlString( determination.getTaxon().getId());
        values[3]  = SqlUtils.sqlString( determination.getTypeStatusName());
        values[4]  = SqlUtils.sqlString( determination.getConfidence());
        values[5]  = SqlUtils.sqlString( determination.getYesNo2());
        values[6]  = SqlUtils.sqlString( determination.getDeterminedDate());
        values[7]  = SqlUtils.sqlString( determination.getDeterminedDatePrecision());
        values[8]  = SqlUtils.sqlString( determination.getIsCurrent());
        values[9]  = SqlUtils.sqlString( determination.getText1());
        values[10] = SqlUtils.sqlString( determination.getRemarks());
        values[11] = SqlUtils.now();

        return SqlUtils.getInsertSql("determination", fieldNames, values);
    }
    
    private String getInsertSql(TaxonCitation taxonCitation)
    {
        String fieldNames = "TaxonID, ReferenceWorkID, Remarks, TimestampCreated";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( taxonCitation.getTaxon().getId());
        values[1] = SqlUtils.sqlString( taxonCitation.getReferenceWork().getId());
        values[2] = SqlUtils.sqlString( taxonCitation.getRemarks());
        values[3] = SqlUtils.now();

        return SqlUtils.getInsertSql("taxoncitation", fieldNames, values);
    }
}
