package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.RelatedPublication;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.ReferenceWork;

// Run this class after PublicationLoader.

public class RelatedPublicationLoader extends CsvToSqlLoader
{
    private PublicationLookup publicationLookup;
    
    private ReferenceWork nullRefWork = new ReferenceWork();
    
	public RelatedPublicationLoader(File csvFile, Statement sqlStatement, PublicationLookup publicationLookup)
	    throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.publicationLookup = publicationLookup;
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		RelatedPublication relatedPub = parse(columns);

		Integer publicationId = relatedPub.getPublicationId();
		setCurrentRecordId(publicationId);
		
		ReferenceWork referenceWork = lookup(publicationId);
		
		ReferenceWork precedingWork = nullRefWork;
		Integer precedingId = relatedPub.getPrecedingId();
		if (precedingId != null) precedingWork = lookup(precedingId);
		referenceWork.setPrecedingWork(precedingWork);
		
		ReferenceWork succeedingWork = nullRefWork;
		Integer succeedingId = relatedPub.getSucceedingId();
		if (succeedingId != null) succeedingWork = lookup(succeedingId);
		referenceWork.setSucceedingWork(succeedingWork);
		
        // convert agentvariant to sql and insert
        String sql = getUpdateSql(referenceWork);
        update(sql);
	}
	
    // botanistId, nameType, name
    private RelatedPublication parse(String[] columns) throws LocalException
    {
        if (columns.length < 3)
        {
            throw new LocalException("Not enough columns");
        }

        RelatedPublication relatedPub = new RelatedPublication();        
        try
        {
            relatedPub.setPublicationId( SqlUtils.parseInt( columns[0] ));
            relatedPub.setPrecedingId(   SqlUtils.parseInt( columns[1] ));
            relatedPub.setSucceedingId(  SqlUtils.parseInt( columns[2] ));
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return relatedPub;
    }

    private ReferenceWork lookup(Integer publicationId) throws LocalException
    {
        return publicationLookup.getById(publicationId);
    }

    private String getUpdateSql(ReferenceWork referenceWork)
    {
        String[] fieldNames = { "PrecedingWorkID", "SucceedingWorkID" };

        String[] values = new String[2];

        values[0]  = SqlUtils.sqlString(referenceWork.getPrecedingWork().getId());
        values[1]  = SqlUtils.sqlString(referenceWork.getSucceedingWork().getId());

        return SqlUtils.getUpdateSql("referencework", fieldNames, values, "ReferenceWorkID", SqlUtils.sqlString(referenceWork.getId()));
    }
 
}
