package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.PublicationTitle;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.ReferenceWork;

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
		checkNull(publicationId, "id");
		setCurrentRecordId(publicationId);

		ReferenceWork referenceWork = lookup(publicationId);
        
        // convert PublicationTitle into ReferenceWorkVariant
        String refWorkVariant = getRefWorkVariant(publTitle);

        // convert ReferenceWorkVariant to sql and insert
        String sql = getUpdateSql(referenceWork, refWorkVariant);
        update(sql);
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

    private String getRefWorkVariant(PublicationTitle publTitle) throws LocalException
    {
    	String title = publTitle.getTitle();
        checkNull(title, "title");
        
        return denormalize("alt. title", title);
    }
	
    private ReferenceWork lookup(Integer publicationId) throws LocalException
    {
        return publicationLookup.getById(publicationId);
    }

    private String getUpdateSql(ReferenceWork refWork, String refWorkTitle)
    {
    	return SqlUtils.getAppendUpdateSql("referencework", "Remarks", refWorkTitle, "ReferenceWorkID", refWork.getId());
    }
 
}
