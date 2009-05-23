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

import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.TypeSpecimen;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.DeterminationCitation;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;

public class TypeSpecimenLoader extends CsvToSqlLoader
{
    // TODO: is the verifier the determiner?  is the designator the determiner?
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
        Integer determinationId = insert(sql);
        determination.setDeterminationId(determinationId);

        // create determination citations
        Integer publicationId1 = typeSpecimen.getNle1PublicationId();
        Integer designatorId1 = typeSpecimen.getNle1DesignatorId();
        String collation1 = typeSpecimen.getNle1Collation();
        String date1 = typeSpecimen.getNle1Date();
        
        DeterminationCitation determinationCitation1 = getDeterminationCitation(publicationId1, designatorId1, collation1, date1);
        if (determinationCitation1 != null)
        {
            determinationCitation1.setDetermination(determination);
            determinationCitation1.setCollectionMemberId(determination.getCollectionMemberId());
            sql = getInsertSql(determinationCitation1);
            insert(sql);
        }
        
        Integer publicationId2 = typeSpecimen.getNle2PublicationId();
        Integer designatorId2 = typeSpecimen.getNle2DesignatorId();
        String collation2 = typeSpecimen.getNle2Collation();
        String date2 = typeSpecimen.getNle2Date();
        
        DeterminationCitation determinationCitation2 = getDeterminationCitation(publicationId2, designatorId2, collation2, date2);
        if (determinationCitation2 != null)
        {
            determinationCitation2.setDetermination(determination);
            determinationCitation2.setCollectionMemberId(determination.getCollectionMemberId());
            sql = getInsertSql(determinationCitation2);
            insert(sql);
        }
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

            bdate.setStartYear(  SqlUtils.parseInt( StringUtils.trimToNull( columns[6] )));
            bdate.setStartMonth( SqlUtils.parseInt( StringUtils.trimToNull( columns[7] )));
            bdate.setStartDay(   SqlUtils.parseInt( StringUtils.trimToNull( columns[8] )));

            typeSpecimen.setNle1DesignatorId(  SqlUtils.parseInt( StringUtils.trimToNull( columns[9]  )));
            typeSpecimen.setNle1PublicationId( SqlUtils.parseInt( StringUtils.trimToNull( columns[10] )));
            typeSpecimen.setNle1Collation(                        StringUtils.trimToNull( columns[11] ));
            typeSpecimen.setNle1Date(                             StringUtils.trimToNull( columns[12] ));
            typeSpecimen.setNle2DesignatorId(  SqlUtils.parseInt( StringUtils.trimToNull( columns[13] )));
            typeSpecimen.setNle2PublicationId( SqlUtils.parseInt( StringUtils.trimToNull( columns[14] )));
            typeSpecimen.setNle2Collation(                        StringUtils.trimToNull( columns[15] ));
            typeSpecimen.setNle2Date(                             StringUtils.trimToNull( columns[16] ));
            typeSpecimen.setRemarks(      SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[17] )));
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
        
    private DeterminationCitation getDeterminationCitation(Integer publicationId, Integer designatorId, String collation, String date)
        throws LocalException
    {
        if (publicationId == null)
        {
            if (designatorId != null || collation != null || date != null)
            {
                warn("No publication for designator, collation, or date", null, null);
            }
            return null;
        }

        DeterminationCitation determinationCitation = new DeterminationCitation();
        
        // ReferenceWork
        String guid = String.valueOf(publicationId);

        Integer referenceWorkId = getIntByField("referencework", "ReferenceWorkID", "GUID", guid);
        ReferenceWork referenceWork = new ReferenceWork();
        referenceWork.setReferenceWorkId(referenceWorkId);

        determinationCitation.setReferenceWork(referenceWork);

        // Remarks
        StringBuffer remarks = new StringBuffer();
        
        // TODO: make designators into authors, too?
        if (designatorId != null)
        {
            Botanist author = new Botanist();
            author.setId(designatorId);
            guid = author.getGuid();

            String authorName = getStringByField("agent", "LastName", "GUID", guid);
            remarks.append(authorName);
        }
        else
        {
            warn("No designator for publication", null, null);
        }
        
        if (collation != null)
        {
            if (remarks.length() > 0) remarks.append("; ");
            remarks.append(collation);
        }

        if (date != null)
        {
            if (remarks.length() > 0) remarks.append(". ");
            remarks.append(date);
        }

        determinationCitation.setRemarks(remarks.toString());
        
        return determinationCitation;
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
    
    private String getInsertSql(DeterminationCitation determinationCitation)
    {
        String fieldNames = "DeterminationID, ReferenceWorkID, CollectionMemberID, Remarks, TimestampCreated";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( determinationCitation.getDetermination().getId());
        values[1] = SqlUtils.sqlString( determinationCitation.getReferenceWork().getId());
        values[2] = SqlUtils.sqlString( determinationCitation.getCollectionMemberId());
        values[3] = SqlUtils.sqlString( determinationCitation.getRemarks());
        values[4] = SqlUtils.now();

        return SqlUtils.getInsertSql("determinationcitation", fieldNames, values);
    }
}
