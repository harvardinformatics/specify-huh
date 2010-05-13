package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.PublicationNumber;
import edu.harvard.huh.asa.PublicationNumber.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.ReferenceWorkIdentifier;

// Run this class after PublicationLoader.

public class PublicationNumberLoader extends CsvToSqlLoader
{
    private PublicationLookup publicationLookup;
    
	public PublicationNumberLoader(File csvFile, Statement sqlStatement, PublicationLookup publicationLookup)
	    throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.publicationLookup = publicationLookup;
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		PublicationNumber publNumber = parse(columns);

		Integer publicationId = publNumber.getPublicationId();
		setCurrentRecordId(publicationId);
		
        // convert PublicationNumber into ReferenceWorkIdentifier
        ReferenceWorkIdentifier refWorkIdentifier = getRefWorkIdentifier(publNumber);

        // convert ReferenceWorkIdentifier to sql and insert
        String sql = getInsertSql(refWorkIdentifier);
        insert(sql);
	}
	
    // publicationId, type, text
    private PublicationNumber parse(String[] columns) throws LocalException
    {
        if (columns.length < 3)
        {
            throw new LocalException("Not enough columns");
        }

        PublicationNumber publNumber = new PublicationNumber();        
        try
        {
            publNumber.setPublicationId(  SqlUtils.parseInt( columns[0] ));
            publNumber.setType( PublicationNumber.parseType( columns[1] ));
            publNumber.setText(                              columns[2] );

        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }
        catch (AsaException e)
        {
        	throw new LocalException("Couldn't parse name type", e);
        }

        return publNumber;
    }

    private ReferenceWorkIdentifier getRefWorkIdentifier(PublicationNumber publNumber) throws LocalException
    {
        ReferenceWorkIdentifier refWorkIdentifier = new ReferenceWorkIdentifier();
        
        // Identifier
        String text = publNumber.getText();
        checkNull(text, "text");

        text = truncate(text, 32, "text");
        refWorkIdentifier.setIdentifier(text);
        
        // ReferenceWork
        Integer publicationId = publNumber.getPublicationId();
        checkNull(publicationId, "id");

        ReferenceWork referenceWork = lookup(publicationId);
        refWorkIdentifier.setReferenceWork(referenceWork);
        
        // Type
        TYPE type = publNumber.getType();
        refWorkIdentifier.setType(type.name());
        
        return refWorkIdentifier;
    }
	
    private ReferenceWork lookup(Integer publicationId) throws LocalException
    {
        return publicationLookup.getById(publicationId);
    }

    private String getInsertSql(ReferenceWorkIdentifier refWorkIdentifier)
    {
        String fieldNames = "Identifier, ReferenceWorkID, TimestampCreated, Type, Version";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( refWorkIdentifier.getIdentifier());
        values[1] = SqlUtils.sqlString( refWorkIdentifier.getReferenceWork().getId());
        values[2] = SqlUtils.now();
        values[3] = SqlUtils.sqlString( refWorkIdentifier.getType());
        values[4] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("referenceworkidentifier", fieldNames, values);
    }
 
}
