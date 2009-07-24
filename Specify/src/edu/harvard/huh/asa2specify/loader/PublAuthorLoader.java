package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.PublAuthor;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.ReferenceWork;

// Run this class after BotanistLoader and PublicationLoader.

public class PublAuthorLoader extends CsvToSqlLoader
{
    private static final Logger log  = Logger.getLogger(PublAuthorLoader.class);
    
    private PublicationLookup   publicationLookup;
    private BotanistLookup      botanistLookup;
    
    private int lastRefWorkId;
	private int orderNumber;
	
	public PublAuthorLoader(File csvFile,
	                        Statement sqlStatement,
	                        PublicationLookup publicationLookup,
	                        BotanistLookup botanistLookup) throws LocalException

	{
		super(csvFile, sqlStatement);
		
		this.publicationLookup = publicationLookup;
		this.botanistLookup = botanistLookup;
		
		lastRefWorkId = 0;
		orderNumber = 1;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		PublAuthor publAuthor = parse(columns);

		Integer publicationId = publAuthor.getPublicationId();
		setCurrentRecordId(publicationId);
		
		// convert PublAuthor into Author
		Author author = convert(publAuthor);

		// convert author to sql and insert
		String sql = getInsertSql(author);
		insert(sql);
	}

	public Logger getLogger()
    {
        return log;
    }
	
	private PublAuthor parse(String[] columns) throws LocalException
	{
		if (columns.length < 3)
		{
			throw new LocalException("Not enough columns");
		}

		PublAuthor publAuthor = new PublAuthor();
		try
		{
			publAuthor.setPublicationId( Integer.parseInt( columns[0] ));
			publAuthor.setAuthorId(      Integer.parseInt( columns[1] ));
			publAuthor.setOrdinal(       Integer.parseInt( columns[2] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return publAuthor;
	}

	private Author convert(PublAuthor publAuthor) throws LocalException
	{
		Author author = new Author();
		
		// Agent
		Integer authorId = publAuthor.getAuthorId();
		checkNull(authorId, "author id");

		Agent agent = lookupBotanist(authorId);
		author.setAgent(agent);

		// ReferenceWork
		Integer publicationId = publAuthor.getPublicationId();
		checkNull(publicationId, "publication id");
		
		ReferenceWork referenceWork = lookupPub(publicationId);
		author.setReferenceWork(referenceWork);

		// OrderNumber
		if (referenceWork.getId() != lastRefWorkId)
		{
			orderNumber = 1;
			lastRefWorkId = referenceWork.getId();
		}
		else
		{
			orderNumber ++;
		}
		author.setOrderNumber((short) orderNumber); // TODO: compare with ordinal, warn if different?
		
		return author;
	}

	private ReferenceWork lookupPub(Integer publicationId) throws LocalException
	{
	    return publicationLookup.getById(publicationId);
	}

	private Agent lookupBotanist(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}
	
	private String getInsertSql(Author author)
	{
		String fieldNames = "AgentId, ReferenceWorkId, OrderNumber, TimestampCreated, Version";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( author.getAgent().getId());
		values[1] = SqlUtils.sqlString( author.getReferenceWork().getId());
		values[2] = SqlUtils.sqlString( author.getOrderNumber());
		values[3] = SqlUtils.now();
		values[4] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("author", fieldNames, values);
	}
}
