package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.PublicationTitle;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.ReferenceWorkVariant;

// Run this class after PublicationLoader.

public class PublicationTitleLoader extends CsvToSqlLoader
{
    private PublicationLookup publicationLookup;
    
	public PublicationTitleLoader(File csvFile, Statement sqlStatement, PublicationLookup publicationLookup)
	    throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.publicationLookup = publicationLookup;
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		PublicationTitle publTitle = parse(columns);

		Integer publicationId = publTitle.getPublicationId();
		setCurrentRecordId(publicationId);
		
        // convert PublicationTitle into ReferenceWorkVariant
        ReferenceWorkVariant refWorkVariant = getRefWorkVariant(publTitle);

        // convert ReferenceWorkVariant to sql and insert
        String sql = getInsertSql(refWorkVariant);
        insert(sql);
	}
	
    // botanistId, nameType, name
    private PublicationTitle parse(String[] columns) throws LocalException
    {
        if (columns.length < 2)
        {
            throw new LocalException("Not enough columns");
        }

        PublicationTitle publTitle = new PublicationTitle();        
        try
        {
            publTitle.setPublicationId( SqlUtils.parseInt( columns[0] ));
            publTitle.setTitle(                            columns[1] );

        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return publTitle;
    }

    private ReferenceWorkVariant getRefWorkVariant(PublicationTitle publTitle) throws LocalException
    {
        ReferenceWorkVariant refWorkVariant = new ReferenceWorkVariant();

        // Name
        String title = publTitle.getTitle();
        checkNull(title, "title");

        title = truncate(title, 255, "title");
        refWorkVariant.setName(title);
        
        // ReferenceWork
        Integer publicationId = publTitle.getPublicationId();
        checkNull(publicationId, "id");

        ReferenceWork referenceWork = lookup(publicationId);
        refWorkVariant.setReferenceWork(referenceWork);
        
        // Type
        Byte varType = ReferenceWorkVariant.VARIANT;
        
        refWorkVariant.setVarType(varType);
        
        return refWorkVariant;
    }
	
    private ReferenceWork lookup(Integer publicationId) throws LocalException
    {
        return publicationLookup.getById(publicationId);
    }

    private String getInsertSql(ReferenceWorkVariant refWorkVariant)
    {
        String fieldNames = "Name, ReferenceWorkID, TimestampCreated, VarType, Version";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( refWorkVariant.getName());
        values[1] = SqlUtils.sqlString( refWorkVariant.getReferenceWork().getId());
        values[2] = SqlUtils.now();
        values[3] = SqlUtils.sqlString( refWorkVariant.getVarType());
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("referenceworkvariant", fieldNames, values);
    }
 
}
