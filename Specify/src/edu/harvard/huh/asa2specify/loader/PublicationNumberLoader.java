package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.PublicationNumber;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.ReferenceWork;

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
        checkNull(publicationId, "id");
		setCurrentRecordId(publicationId);

        ReferenceWork referenceWork = lookup(publicationId);
        
        // convert PublicationNumber into ReferenceWorkIdentifier
        String refWorkIdentifier = getRefWorkIdentifier(publNumber);

        // convert ReferenceWorkIdentifier to sql and insert
        String sql = getUpdateSql(referenceWork, refWorkIdentifier);
        update(sql);
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

    private String getRefWorkIdentifier(PublicationNumber publNumber) throws LocalException
    {
        // Identifier
        String text = publNumber.getText();
        checkNull(text, "text");
        
        return denormalize(publNumber.getType().name(), text);
    }
	
    private ReferenceWork lookup(Integer publicationId) throws LocalException
    {
        return publicationLookup.getById(publicationId);
    }

    private String getUpdateSql(ReferenceWork refWork, String refWorkIdentifier)
    {        
    	return SqlUtils.getAppendUpdateSql("referencework", "Remarks", refWorkIdentifier, "ReferenceWorkID", refWork.getId());
    }
 
}
