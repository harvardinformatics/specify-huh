package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.PublAuthor;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.ReferenceWorkLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.ReferenceWork;

// Run this class after BotanistLoader and PublicationLoader.

public class PublAuthorLoader extends CsvToSqlLoader
{
    private ReferenceWorkLookup refWorkLookup;
    private BotanistLookup      botanistLookup;
    
    private int lastRefWorkId;
	private int orderNumber;
	
	public PublAuthorLoader(File csvFile,
	                        Statement sqlStatement,
	                        ReferenceWorkLookup refWorkLookup,
	                        BotanistLookup botanistLookup) throws LocalException

	{
		super(csvFile, sqlStatement);
		
		this.refWorkLookup = refWorkLookup;
		this.botanistLookup = botanistLookup;
		
		lastRefWorkId = 0;
		orderNumber = 1;
	}

	private ReferenceWork lookupPub(Integer publicationId) throws LocalException
	{
	    return refWorkLookup.getByPublicationId(publicationId);
	}

	private Agent lookupBotanist(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getByBotanistId(botanistId);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		PublAuthor publAuthor = parse(columns);

		Integer publicationId = publAuthor.getPublicationId();
		setCurrentRecordId(publicationId);
		
		// convert BotanistRoleCountry into AgentGeography
		Author author = convert(publAuthor);

		// convert agentspecialty to sql and insert
		String sql = getInsertSql(author);
		insert(sql);
	}

	private PublAuthor parse(String[] columns) throws LocalException
	{
		if (columns.length < 3)
		{
			throw new LocalException("Wrong number of columns");
		}

		PublAuthor publAuthor = new PublAuthor();
		try
		{
			publAuthor.setPublicationId( Integer.parseInt( StringUtils.trimToNull( columns[0] ) ) );
			publAuthor.setAuthorId(      Integer.parseInt( StringUtils.trimToNull( columns[1] ) ) );
			publAuthor.setOrdinal(       Integer.parseInt( StringUtils.trimToNull( columns[2] ) ) );
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
		author.setOrderNumber((short) orderNumber);
		
		return author;
	}

	private String getInsertSql(Author author)
	{
		String fieldNames = "AgentId, ReferenceWorkId, OrderNumber, TimestampCreated";

		String[] values = new String[4];

		values[0] = SqlUtils.sqlString( author.getAgent().getId());
		values[1] = SqlUtils.sqlString( author.getReferenceWork().getId());
		values[2] = SqlUtils.sqlString( author.getOrderNumber());
		values[3] = SqlUtils.now();

		return SqlUtils.getInsertSql("author", fieldNames, values);
	}
}
