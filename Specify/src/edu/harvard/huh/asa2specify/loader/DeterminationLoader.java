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
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Taxon;

public class DeterminationLoader extends CsvToSqlLoader
{

    public DeterminationLoader(File csvFile, Statement sqlStatement)
    {
        super(csvFile, sqlStatement);
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaDetermination asaDetermination = parse(columns);

        Determination determination = convert(asaDetermination);
        
        String sql = getInsertSql(determination);
        insert(sql);
    }

    private AsaDetermination parse(String[] columns) throws LocalException
    {
        if (columns.length < 13)
        {
            throw new LocalException("Wrong number of columns");
        }
        
        AsaDetermination determination = new AsaDetermination();
        
        try
        {
            determination.setId(         Integer.parseInt( StringUtils.trimToNull( columns[0] )));
            determination.setSpecimenId( Integer.parseInt( StringUtils.trimToNull( columns[1] )));
            determination.setTaxonId(    Integer.parseInt( StringUtils.trimToNull( columns[2] )));
            determination.setQualifier(                    StringUtils.trimToNull( columns[3] ));
            
            BDate bdate = new BDate();
            determination.setDate( bdate );

            String startYearStr = StringUtils.trimToNull( columns[4] );
            if (startYearStr != null)
            {
                bdate.setStartYear( Integer.parseInt( startYearStr ));
            }

            String startMonthStr = StringUtils.trimToNull( columns[5] );
            if (startMonthStr != null)
            {
                bdate.setStartMonth( Integer.parseInt( startMonthStr ));
            }
            
            String startDayStr = StringUtils.trimToNull( columns[6] );
            if (startDayStr != null)
            {
                bdate.setStartDay( Integer.parseInt( startDayStr ));
            }

            determination.setCurrent(   Boolean.parseBoolean( StringUtils.trimToNull( columns[7]  )));
            determination.setIsLabel(   Boolean.parseBoolean( StringUtils.trimToNull( columns[8]  )));
            determination.setDeterminedBy(                    StringUtils.trimToNull( columns[9]  ));
            determination.setLabelText(                       StringUtils.trimToNull( columns[10] ));
            determination.setOrdinal(       Integer.parseInt( StringUtils.trimToNull( columns[11] )));
            determination.setRemarks( SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[12] )));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        
        return determination;
    }
    
    private Determination convert(AsaDetermination asaDet) throws LocalException
    {
        Determination determination = new Determination();
        
        // CollectionObject
        Integer specimenId = asaDet.getSpecimenId();
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
        Integer asaTaxonId = asaDet.getTaxonId();
        if (asaTaxonId == null)
        {
            throw new LocalException("No taxon id");
        }

        String taxonSerNumber = String.valueOf(asaTaxonId);
        
        Integer taxonId = getIntByField("taxon", "TaxonID", "TaxonomicSerialNumber", taxonSerNumber);

        Taxon taxon = new Taxon();
        taxon.setTaxonId(taxonId);
        
        determination.setTaxon( taxon ); 
        
        // Confidence
        String qualifier = asaDet.getQualifier();
        if ( qualifier != null && qualifier.length() > 50 )
        {
            warn("Truncating confidence", asaDet.getId(), qualifier);
        }
        determination.setConfidence(qualifier);
        
        // DeterminedDate
        BDate bdate = asaDet.getDate();

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
            warn("Invalid start date", asaDet.getId(),
                    String.valueOf(startYear) + " " + String.valueOf(startMonth) + " " +String.valueOf(startDay));
        }

        // IsCurrent
        determination.setIsCurrent( asaDet.isCurrent() );
        
        // YesNo1 (isLabel)
        determination.setYesNo1( asaDet.isLabel() );
        
        // Text1 (determinedBy) TODO: assign DeterminerID to collector if isLabel == true? determined_by often does not match collector in these cases
        // Determiner TODO: Ugh.  We have a string.  They have a relation.  Putting it in Text1 for now.
        String determinedBy = asaDet.getDeterminedBy();
        determination.setText1( determinedBy );
        
        // Text2 (labelText)
        String labelText = asaDet.getLabelText();
        determination.setText2( labelText );
        
        // Number1 (ordinal)
        Integer ordinal = asaDet.getOrdinal();
        determination.setNumber1((float) ordinal);

        // Remarks TODO: Maureen, check your notes on this field and label_text
        String remarks = asaDet.getRemarks();
        determination.setRemarks(remarks);

        return determination;
    }
    
    private String getInsertSql(Determination determination)
    {
        String fieldNames = "CollectionObjectID, CollectionMemberID, TaxonID, Confidence, DeterminedDate, " +
        		            "DeterminedDatePrecision, IsCurrent, YesNo1, Text1, Text2, Number1, Remarks, TimestampCreated";

        String[] values = new String[12];
        
        values[0]  = SqlUtils.sqlString( determination.getCollectionObject().getId());
        values[1]  = SqlUtils.sqlString( determination.getCollectionMemberId());
        values[2]  = SqlUtils.sqlString( determination.getTaxon().getId());
        values[3]  = SqlUtils.sqlString( determination.getConfidence());
        values[4]  = SqlUtils.sqlString( determination.getDeterminedDate());
        values[5]  = SqlUtils.sqlString( determination.getDeterminedDatePrecision());
        values[6]  = SqlUtils.sqlString( determination.getIsCurrent());
        values[7]  = SqlUtils.sqlString( determination.getText1());
        values[8]  = SqlUtils.sqlString( determination.getText2());
        values[9]  = SqlUtils.sqlString( determination.getNumber1());
        values[10] = SqlUtils.sqlString( determination.getRemarks());
        values[11] = SqlUtils.now();

        return SqlUtils.getInsertSql("determination", fieldNames, values);
    }
}
