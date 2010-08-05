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
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.TypeSpecimen;
import edu.harvard.huh.asa.TypeSpecimen.CONDITIONALITY;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.DeterminationCitation;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;

// Run this class after TaxonLoader, SpecimenItemLoader, and PublicationLoader.

public class TypeSpecimenLoader extends CsvToSqlLoader
{
    private SpecimenLookup      specimenLookup;
    private TaxonLookup         taxonLookup;
    private PublicationLookup   publicationLookup;
    private BotanistLookup      botanistLookup;
    
    // TODO: is the verifier the determiner?  is the designator the determiner?
    public TypeSpecimenLoader(File csvFile,
                              Statement sqlStatement,
                              SpecimenLookup specimenLookup,
                              TaxonLookup taxonLookup,
                              PublicationLookup publicationLookup,
                              BotanistLookup botanistLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.specimenLookup    = specimenLookup;
        this.taxonLookup       = taxonLookup;
        this.publicationLookup = publicationLookup;
        this.botanistLookup    = botanistLookup;
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
        String status = TypeSpecimen.toString(typeSpecimen.getTypeStatus());
        
        Integer nle1PublicationId = typeSpecimen.getNle1PublicationId();
        if (nle1PublicationId != null)
        {
            String collation = typeSpecimen.getNle1Collation();
            String date = typeSpecimen.getNle1Date();
            String remarks = status + " designation";
            
            ReferenceWork referenceWork = getPublicationLookup().getById(nle1PublicationId);
            
            Agent nle1DesignatorAgent = NullAgent();
            Integer nle1DesignatorId = typeSpecimen.getNle1DesignatorId();
            if (nle1DesignatorId != null)
            {
                nle1DesignatorAgent = getBotanistLookup().getById(nle1DesignatorId);
            }
            else
            {
                getLogger().warn(rec() + "No nle1 designator for publication");
            }
            DeterminationCitation determinationCitation1 =
                getDeterminationCitation(determination, collectionMemberId, referenceWork, nle1DesignatorAgent, collation, date, remarks);

            if (determinationCitation1 != null)
            {
                sql = getInsertSql(determinationCitation1);
                insert(sql);
            }
        }
        
        Integer nle2PublicationId = typeSpecimen.getNle2PublicationId();
        if (nle2PublicationId != null)
        {
            String collation = typeSpecimen.getNle2Collation();
            String date = typeSpecimen.getNle2Date();
            String remarks = status + " designation clarification/refinement";
            
            ReferenceWork referenceWork = getPublicationLookup().getById(nle2PublicationId);
            
            Agent nle2DesignatorAgent = NullAgent();
            Integer nle2DesignatorId = typeSpecimen.getNle2DesignatorId();
            if (nle2DesignatorId != null)
            {
                nle2DesignatorAgent = getBotanistLookup().getById(nle2DesignatorId);
            }
            else
            {
                getLogger().warn(rec() + "No nle2 designator for publication");
            }
            
            DeterminationCitation determinationCitation2 =
                getDeterminationCitation(determination, collectionMemberId, referenceWork, nle2DesignatorAgent, collation, date, remarks);
            
            if (determinationCitation2 != null)
            {
                sql = getInsertSql(determinationCitation2);
                insert(sql);
            }
        }
    }
    
    private TypeSpecimen parse(String[] columns) throws LocalException
    {
        if (columns.length < 22)
        {
            throw new LocalException("Not enough columns");
        }
        
        TypeSpecimen typeSpecimen = new TypeSpecimen();     
        try
        {
            typeSpecimen.setId(                            SqlUtils.parseInt( columns[0] ));
            typeSpecimen.setSpecimenId(                    SqlUtils.parseInt( columns[1] ));
            typeSpecimen.setCollectionCode(                                   columns[2] );
            typeSpecimen.setTaxonId(                       SqlUtils.parseInt( columns[3] ));
            typeSpecimen.setTaxon(                                            columns[4] );
            typeSpecimen.setTypeStatus(             TypeSpecimen.parseStatus( columns[5] ));
            typeSpecimen.setConditionality( TypeSpecimen.parseConditionality( columns[6] ));
            typeSpecimen.setIsFragment(                 Boolean.parseBoolean( columns[7] ));

            BDate bdate = new BDate();
            typeSpecimen.setDate( bdate );

            bdate.setStartYear(  SqlUtils.parseInt( columns[8]  ));
            bdate.setStartMonth( SqlUtils.parseInt( columns[9]  ));
            bdate.setStartDay(   SqlUtils.parseInt( columns[10] ));

            typeSpecimen.setVerifiedBy(                           columns[11] );
            typeSpecimen.setNle1DesignatorId(  SqlUtils.parseInt( columns[12] ));
            typeSpecimen.setNle1PublicationId( SqlUtils.parseInt( columns[13] ));
            typeSpecimen.setNle1Collation(                        columns[14] );
            typeSpecimen.setNle1Date(                             columns[15] );
            typeSpecimen.setNle2DesignatorId(  SqlUtils.parseInt( columns[16] ));
            typeSpecimen.setNle2PublicationId( SqlUtils.parseInt( columns[17] ));
            typeSpecimen.setNle2Collation(                        columns[18] );
            typeSpecimen.setNle2Date(                             columns[19] );
            typeSpecimen.setRemarks(      SqlUtils.iso8859toUtf8( columns[20] ));
            typeSpecimen.setOrdinal(           SqlUtils.parseInt( columns[21] ));
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
        
        Fragment fragment = getSpecimenLookup().getById(specimenId);
        determination.setFragment(fragment);
        
        // Confidence
        CONDITIONALITY conditionality = typeSpecimen.getConditionality();
        if (conditionality != null) determination.setConfidence(TypeSpecimen.toString(conditionality));
        
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
        determination.setIsCurrent(false);

        // Number1 (ordinal)
        Integer ordinal = typeSpecimen.getOrdinal();
        if (ordinal != null) determination.setNumber1((float) ordinal);
        
        // Qualifier
        
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
        
        // Text2 (labelText, for determinations)
        
        // TypeStatusName
        String typeStatus = TypeSpecimen.toString(typeSpecimen.getTypeStatus());
        
        typeStatus = truncate(typeStatus, 50, "type status");
        determination.setTypeStatusName(typeStatus);
        
        // YesNo1 (isLabel)
        
        // YesNo2 (isFragment)
        determination.setYesNo2( typeSpecimen.isFragment() );

        return determination;
    }
        
    private DeterminationCitation getDeterminationCitation(Determination determination,
    		                                               Integer       collectionMemberId,
    		                                               ReferenceWork referenceWork,
    		                                               Agent         designator,
    		                                               String        collation,
    		                                               String        date,
    		                                               String        remarks)
    	throws LocalException
    {
        DeterminationCitation determinationCitation = new DeterminationCitation();
        
        // CollectionMemberId
        determinationCitation.setCollectionMemberId(collectionMemberId);
        
        // Designator
        determinationCitation.setDesignator(designator);
        
        // Determination
        determinationCitation.setDetermination(determination);

        // ReferenceWork
        determinationCitation.setReferenceWork(referenceWork);
        
        // Remarks
        determinationCitation.setRemarks(remarks);

        // Text1 (collation)
        determinationCitation.setText1(collation);
        
        // Text2 (date)
        determinationCitation.setText2(date);
        
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

    private BotanistLookup getBotanistLookup()
    {
        return this.botanistLookup;
    }

    private String getInsertSql(Determination determination)
    {
        String fieldNames = "CollectionMemberID, FragmentID, Confidence, DeterminedDate, " +
        		            "DeterminedDatePrecision, IsCurrent, Remarks, TaxonID, Text1, Text2, " +
        		            "TimestampCreated, TypeStatusName, Version, YesNo2";

        String[] values = new String[14];
        
        values[0]  = SqlUtils.sqlString( determination.getCollectionMemberId());
        values[1]  = SqlUtils.sqlString( determination.getFragment().getId());
        values[2]  = SqlUtils.sqlString( determination.getConfidence());
        values[3]  = SqlUtils.sqlString( determination.getDeterminedDate());
        values[4]  = SqlUtils.sqlString( determination.getDeterminedDatePrecision());
        values[5]  = SqlUtils.sqlString( determination.getIsCurrent());
        values[6]  = SqlUtils.sqlString( determination.getRemarks());
        values[7]  = SqlUtils.sqlString( determination.getTaxon().getId());
        values[8]  = SqlUtils.sqlString( determination.getText1());
        values[9]  = SqlUtils.sqlString( determination.getText2());
        values[10] = SqlUtils.now();
        values[11] = SqlUtils.sqlString( determination.getTypeStatusName());
        values[12] = SqlUtils.one();
        values[13] = SqlUtils.sqlString( determination.getYesNo2());

        return SqlUtils.getInsertSql("determination", fieldNames, values);
    }
    
    private String getInsertSql(DeterminationCitation determinationCitation)
    {
        String fieldNames = "CollectionMemberID, DesignatorID, DeterminationID, ReferenceWorkID, Remarks, " +
        		            "Text1, Text2, TimestampCreated, Version";
        
        String[] values = new String[9];
        
        values[0] = SqlUtils.sqlString( determinationCitation.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( determinationCitation.getDesignator().getId());
        values[2] = SqlUtils.sqlString( determinationCitation.getDetermination().getId());
        values[3] = SqlUtils.sqlString( determinationCitation.getReferenceWork().getId());
        values[4] = SqlUtils.sqlString( determinationCitation.getRemarks());
        values[5] = SqlUtils.sqlString( determinationCitation.getText1());
        values[6] = SqlUtils.sqlString( determinationCitation.getText2());
        values[7] = SqlUtils.now();
        values[8] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("determinationcitation", fieldNames, values);
    }
}
