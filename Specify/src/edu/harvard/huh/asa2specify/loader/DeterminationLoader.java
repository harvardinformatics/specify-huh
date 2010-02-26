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

import edu.harvard.huh.asa.AsaDetermination;
import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.BDate;
import edu.harvard.huh.asa.AsaDetermination.QUALIFIER;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.SpecimenLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonLookup;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Taxon;

// Run this class after TaxonLoader and SpecimenItemLoader.

public class DeterminationLoader extends CsvToSqlLoader
{
    private SpecimenLookup specimenLookup;
    private TaxonLookup    taxonLookup;
    
    public DeterminationLoader(File           csvFile,
                               Statement      sqlStatement,
                               SpecimenLookup specimenLookup,
                               TaxonLookup    taxonLookup) throws LocalException
    {
        super(csvFile, sqlStatement);
        
        this.specimenLookup = specimenLookup;
        this.taxonLookup    = taxonLookup;
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaDetermination asaDetermination = parse(columns);

        Integer asaDetId = asaDetermination.getId();
        setCurrentRecordId(asaDetId);
        
        Determination determination = getDetermination(asaDetermination);
        
        String sql = getInsertSql(determination);
        insert(sql);
    }

    private AsaDetermination parse(String[] columns) throws LocalException
    {
        if (columns.length < 14)
        {
            throw new LocalException("Not enough columns");
        }
        
        AsaDetermination determination = new AsaDetermination();
        try
        {
            determination.setId(         SqlUtils.parseInt( columns[0] ));
            determination.setSpecimenId( SqlUtils.parseInt( columns[1] ));
            determination.setCollectionCode(                columns[2] );
            determination.setTaxonId(    SqlUtils.parseInt( columns[3] ));
            determination.setQualifier( AsaDetermination.parseQualifier( columns[4] ));
            
            BDate bdate = new BDate();
            determination.setDate( bdate );

            bdate.setStartYear(  SqlUtils.parseInt( columns[5] ));
            bdate.setStartMonth( SqlUtils.parseInt( columns[6] ));
            bdate.setStartDay(   SqlUtils.parseInt( columns[7] ));

            determination.setCurrent(   Boolean.parseBoolean( columns[8]  ));
            determination.setIsLabel(   Boolean.parseBoolean( columns[9]  ));
            determination.setDeterminedBy(                    columns[10] );
            determination.setLabelText(                       columns[11] );
            determination.setOrdinal(      SqlUtils.parseInt( columns[12] ));
            determination.setRemarks( SqlUtils.iso8859toUtf8( columns[13] ));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
            throw new LocalException("Couldn't parse qualifier type", e);
        }
        return determination;
    }
    
    private Determination getDetermination(AsaDetermination asaDet) throws LocalException
    {
        Determination determination = new Determination();
        
        // CollectionObject
        Integer specimenId = asaDet.getSpecimenId();
        checkNull(specimenId, "specimen id");
        
        CollectionObject collectionObject = lookupSpecimen(specimenId);
        Fragment fragment = null; // TODO implement fragment determination load
        determination.setFragment(fragment);

        // CollectionMemberID
        String collectionCode = asaDet.getCollectionCode();
        checkNull(collectionCode, "collection code");
        
        Integer collectionMemberId = getCollectionId(collectionCode);
        determination.setCollectionMemberId(collectionMemberId);

        // Taxon
        Integer asaTaxonId = asaDet.getTaxonId();
        checkNull(asaTaxonId, "taxon id");

        Taxon taxon = lookupTaxon(asaTaxonId);

        // this doesn't change anything; it is necessary that this field be non-null
        // for the call to determination.setTaxon TODO: check that this is still true
        taxon.setIsAccepted(true);
        determination.setTaxon(taxon); 
        
        // Confidence
        
        // DeterminedDate
        BDate bdate = asaDet.getDate();

        Integer startYear  = bdate.getStartYear();
        Integer startMonth = bdate.getStartMonth();
        Integer startDay   = bdate.getStartDay();

        // DeterminedDate and DeterminedDatePrecision
        if (DateUtils.isValidSpecifyDate( startYear, startMonth, startDay))
        {
            determination.setDeterminedDate(DateUtils.getSpecifyStartDate(bdate));
            determination.setDeterminedDatePrecision(DateUtils.getDatePrecision(startYear, startMonth, startDay));
        }
        else if (startYear != null)
        {
            getLogger().warn(rec() + "Invalid determination date: " +
                    String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay));
        }

        // IsCurrent
        Boolean isCurrent = asaDet.isCurrent();
        determination.setIsCurrent(isCurrent);
        
        // YesNo1 (isLabel)
        Boolean isLabel = asaDet.isLabel();
        determination.setYesNo1(isLabel);
        
        // Text1 (determinedBy) TODO: assign DeterminerID to collector if isLabel == true? determined_by often does not match collector in these cases
        // Determiner TODO: Ugh.  We have a string.  They have a relation.  Putting it in Text1 for now.
        String determinedBy = asaDet.getDeterminedBy();
        determination.setText1(determinedBy);
        
        // Text2 (labelText)
        String labelText = asaDet.getLabelText();
        determination.setText2(labelText);
        
        // Number1 (ordinal)
        Integer ordinal = asaDet.getOrdinal();
        if (ordinal != null) determination.setNumber1((float) ordinal);

        // Qualifier
        QUALIFIER qualifier = asaDet.getQualifier();
        if (qualifier != null)
        {
            determination.setQualifier(AsaDetermination.toString(qualifier));
        }
        
        // Remarks TODO: Maureen, check your notes on this field and label_text
        String remarks = asaDet.getRemarks();
        determination.setRemarks(remarks);

        return determination;
    }
    
    private CollectionObject lookupSpecimen(Integer specimenId) throws LocalException
    {
        return specimenLookup.getById(specimenId);
    }
    
    private Taxon lookupTaxon(Integer asaTaxonId) throws LocalException
    {
        return taxonLookup.getById(asaTaxonId);
    }

    private String getInsertSql(Determination determination)
    {
        String fieldNames = "FragmentID, CollectionMemberID, TaxonID, DeterminedDate, " +
        		            "DeterminedDatePrecision, IsCurrent, YesNo1, Text1, Text2, Number1, Qualifier, " +
        		            "Remarks, TimestampCreated, Version";

        String[] values = new String[14];
        
        values[0]  = SqlUtils.sqlString( determination.getFragment().getId());
        values[1]  = SqlUtils.sqlString( determination.getCollectionMemberId());
        values[2]  = SqlUtils.sqlString( determination.getTaxon().getId());
        values[3]  = SqlUtils.sqlString( determination.getDeterminedDate());
        values[4]  = SqlUtils.sqlString( determination.getDeterminedDatePrecision());
        values[5]  = SqlUtils.sqlString( determination.getIsCurrent());
        values[6]  = SqlUtils.sqlString( determination.getYesNo1());
        values[7]  = SqlUtils.sqlString( determination.getText1());
        values[8]  = SqlUtils.sqlString( determination.getText2());
        values[9]  = SqlUtils.sqlString( determination.getNumber1());
        values[10] = SqlUtils.sqlString( determination.getQualifier());
        values[11] = SqlUtils.sqlString( determination.getRemarks());
        values[12] = SqlUtils.now();
        values[13] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("determination", fieldNames, values);
    }
}
