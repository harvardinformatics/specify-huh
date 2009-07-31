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

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.TypeSpecimen;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.DeterminationCitation;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;

// Run this class after TaxonLoader, SpecimenItemLoader, and PublicationLoader.

public class TypeSpecimenLoader extends CsvToSqlLoader
{
    private static final Logger log  = Logger.getLogger(TypeSpecimenLoader.class);
    
    private SpecimenLookup      specimenLookup;
    private TaxonLookup         taxonLookup;
    private PublicationLookup   publicationLookup;
    
    // TODO: is the verifier the determiner?  is the designator the determiner?
    public TypeSpecimenLoader(File csvFile,
                              Statement sqlStatement,
                              SpecimenLookup specimenLookup,
                              TaxonLookup taxonLookup,
                              PublicationLookup publicationLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.specimenLookup    = specimenLookup;
        this.taxonLookup       = taxonLookup;
        this.publicationLookup = publicationLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        TypeSpecimen typeSpecimen = parse(columns);

        Integer typeSpecimenId = typeSpecimen.getId();
        setCurrentRecordId(typeSpecimenId);
        
        Determination determination = getDetermination(typeSpecimen);
        String sql = getInsertSql(determination);
        Integer determinationId = insert(sql);
        determination.setDeterminationId(determinationId);

        // DeterminationCitations
        Integer collectionMemberId = determination.getCollectionMemberId();
        
        DeterminationCitation determinationCitation1 = get1stDeterminationCitation(typeSpecimen, determination, collectionMemberId);
        if (determinationCitation1 != null)
        {
            sql = getInsertSql(determinationCitation1);
            insert(sql);
        }
                
        DeterminationCitation determinationCitation2 = get2ndDeterminationCitation(typeSpecimen, determination, collectionMemberId);
        if (determinationCitation2 != null)
        {
            sql = getInsertSql(determinationCitation2);
            insert(sql);
        }
    }

    public Logger getLogger()
    {
        return log;
    }
    
    private TypeSpecimen parse(String[] columns) throws LocalException
    {
        if (columns.length < 21)
        {
            throw new LocalException("Not enough columns");
        }
        
        TypeSpecimen typeSpecimen = new TypeSpecimen();     
        try
        {
            typeSpecimen.setId(                SqlUtils.parseInt( columns[0] ));
            typeSpecimen.setSpecimenId(        SqlUtils.parseInt( columns[1] ));
            typeSpecimen.setCollectionCode(                       columns[2] );
            typeSpecimen.setTaxonId(           SqlUtils.parseInt( columns[3] ));
            typeSpecimen.setTypeStatus( TypeSpecimen.parseStatus( columns[4] ));
            typeSpecimen.setConditionality(                       columns[5] );
            typeSpecimen.setIsFragment(     Boolean.parseBoolean( columns[6] ));

            BDate bdate = new BDate();
            typeSpecimen.setDate( bdate );

            bdate.setStartYear(  SqlUtils.parseInt( columns[7] ));
            bdate.setStartMonth( SqlUtils.parseInt( columns[8] ));
            bdate.setStartDay(   SqlUtils.parseInt( columns[9] ));

            typeSpecimen.setVerifiedBy(                           columns[10] );
            typeSpecimen.setNle1Designator(                       columns[11] );
            typeSpecimen.setNle1PublicationId( SqlUtils.parseInt( columns[12] ));
            typeSpecimen.setNle1Collation(                        columns[13] );
            typeSpecimen.setNle1Date(                             columns[14] );
            typeSpecimen.setNle2Designator(                       columns[15] );
            typeSpecimen.setNle2PublicationId( SqlUtils.parseInt( columns[16] ));
            typeSpecimen.setNle2Collation(                        columns[17] );
            typeSpecimen.setNle2Date(                             columns[18] );
            typeSpecimen.setRemarks(      SqlUtils.iso8859toUtf8( columns[19] ));
            typeSpecimen.setOrdinal(           SqlUtils.parseInt( columns[20] ));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
            throw new LocalException("Couldn't parse type status field", e);
        }
        
        return typeSpecimen;
    }
    
    private Determination getDetermination(TypeSpecimen typeSpecimen) throws LocalException
    {	
        Determination determination = new Determination();
        
        // CollectionMemberID
        String collectionCode = typeSpecimen.getCollectionCode();
        checkNull(collectionCode, "collection code");
        
        Integer collectionMemberId = getCollectionId(collectionCode);
        determination.setCollectionMemberId(collectionMemberId);

        // CollectionObject
        Integer specimenId = typeSpecimen.getSpecimenId();
        checkNull(specimenId, "specimen id");
        
        CollectionObject collectionObject = getSpecimenLookup().getById(specimenId);
        determination.setCollectionObject(collectionObject);
        
        // Confidence
        
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
        else if (startYear != null)
        {
            getLogger().warn(rec() + "Invalid verification date: " +
                    String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay));
        }
        
        // IsCurrent
        determination.setIsCurrent(true);
        
        // Number1 (ordinal)
        Integer ordinal = typeSpecimen.getOrdinal();
        if (ordinal != null) determination.setNumber1((float) ordinal);
        
        // Qualifier TODO: make enum for conditionality
        String conditionality = typeSpecimen.getConditionality();
        if (conditionality != null) conditionality = truncate(conditionality, 16, "conditionality");
        determination.setQualifier(conditionality);
        
        
        // Remarks TODO: Maureen, check your notes on this field and label_text
        String remarks = typeSpecimen.getRemarks();
        determination.setRemarks(remarks);
        
        // Taxon
        Integer asaTaxonId = typeSpecimen.getTaxonId();
        checkNull(asaTaxonId, "taxon id");

        Taxon taxon = getTaxonLookup().getById(asaTaxonId);
        
        // this doesn't change anything; it is necessary that this field be non-null
        // for the call to determination.setTaxon TODO: check that this is still true
        taxon.setIsAccepted(true);

        determination.setTaxon( taxon ); 

        // Text1 (verifiedBy) TODO: normalize determiner name to botanist agent?  does this even go here?
        String verifiedBy = typeSpecimen.getVerifiedBy();
        determination.setText1(verifiedBy);
        
        // TypeStatusName
        String typeStatus = TypeSpecimen.toString(typeSpecimen.getTypeStatus());
        
        typeStatus = truncate(typeStatus, 50, "type status");
        determination.setTypeStatusName(typeStatus);
        
        // YesNo1 (isLabel)
        
        // YesNo2 (isFragment)
        determination.setYesNo1( typeSpecimen.isFragment() );

        return determination;
    }
        
    private DeterminationCitation get1stDeterminationCitation(TypeSpecimen typeSpecimen, Determination determination, Integer collectionMemberId)
        throws LocalException
    {    	
    	Integer publicationId = typeSpecimen.getNle1PublicationId();
    	String  designator    = typeSpecimen.getNle1Designator();
    	String  collation     = typeSpecimen.getNle1Collation();
    	String  date          = typeSpecimen.getNle1Date();
    	
    	return getDeterminationCitation(determination, collectionMemberId, publicationId, designator, collation, date);
    }
    
    private DeterminationCitation get2ndDeterminationCitation(TypeSpecimen typeSpecimen, Determination determination, Integer collectionMemberId)
    	throws LocalException
    {    	
    	Integer publicationId = typeSpecimen.getNle2PublicationId();
    	String  designator    = typeSpecimen.getNle2Designator();
    	String  collation     = typeSpecimen.getNle2Collation();
    	String  date          = typeSpecimen.getNle2Date();

    	return getDeterminationCitation(determination, collectionMemberId, publicationId, designator, collation, date);
    }
    
    private DeterminationCitation getDeterminationCitation(Determination determination,
    		                                               Integer       collectionMemberId,
    		                                               Integer       publicationId,
    		                                               String       designator,
    		                                               String        collation,
    		                                               String        date)
    	throws LocalException
    {
        if (publicationId == null) return null;
        
        DeterminationCitation determinationCitation = new DeterminationCitation();
        
        // CollectionMemberId
        determinationCitation.setCollectionMemberId(collectionMemberId);
        
        // Determination
        determinationCitation.setDetermination(determination);
        
        // ReferenceWork
        ReferenceWork referenceWork = getPublicationLookup().getById(publicationId);
        determinationCitation.setReferenceWork(referenceWork);

        // Remarks
        StringBuffer remarks = new StringBuffer();
        
        // TODO: retain relationship? make designators into authors?
        if (designator != null)
        {
            remarks.append("nle designator: " + designator);
        }
        else
        {
            getLogger().warn(rec() + "No designator for publication");
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
    
    private SpecimenLookup getSpecimenLookup()
    {
        return this.specimenLookup;
    }
    
    private TaxonLookup getTaxonLookup()
    {
        return this.taxonLookup;
    }

    private PublicationLookup getPublicationLookup()
    {
        return this.publicationLookup;
    }

    private String getInsertSql(Determination determination)
    {
        String fieldNames = "CollectionMemberID, CollectionObjectID, DeterminedDate, " +
        		            "DeterminedDatePrecision, IsCurrent, Qualifier, Remarks, TaxonID, Text1, " +
        		            "TimestampCreated, TypeStatusName, Version, YesNo2";

        String[] values = new String[13];
        
        values[0]  = SqlUtils.sqlString( determination.getCollectionMemberId());
        values[1]  = SqlUtils.sqlString( determination.getCollectionObject().getId());
        values[2]  = SqlUtils.sqlString( determination.getDeterminedDate());
        values[3]  = SqlUtils.sqlString( determination.getDeterminedDatePrecision());
        values[4]  = SqlUtils.sqlString( determination.getIsCurrent());
        values[5]  = SqlUtils.sqlString( determination.getQualifier());
        values[6]  = SqlUtils.sqlString( determination.getRemarks());
        values[7]  = SqlUtils.sqlString( determination.getTaxon().getId());
        values[8]  = SqlUtils.sqlString( determination.getText1());
        values[9]  = SqlUtils.now();
        values[10] = SqlUtils.sqlString( determination.getTypeStatusName());
        values[11] = SqlUtils.zero();
        values[12] = SqlUtils.sqlString( determination.getYesNo2());

        return SqlUtils.getInsertSql("determination", fieldNames, values);
    }
    
    private String getInsertSql(DeterminationCitation determinationCitation)
    {
        String fieldNames = "CollectionMemberID, DeterminationID, ReferenceWorkID, Remarks, " +
        		            "TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( determinationCitation.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( determinationCitation.getDetermination().getId());
        values[2] = SqlUtils.sqlString( determinationCitation.getReferenceWork().getId());
        values[3] = SqlUtils.sqlString( determinationCitation.getRemarks());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("determinationcitation", fieldNames, values);
    }
}
