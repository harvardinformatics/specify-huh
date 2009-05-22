package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.PublAuthor;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublAuthorLoader extends CsvToSqlLoader
{
	private int lastRefWorkId;
	private int orderNumber;
	
	public PublAuthorLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
		lastRefWorkId = 0;
		orderNumber = 1;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		PublAuthor publAuthor = parse(columns);

		// convert BotanistRoleCountry into AgentGeography
		Author author = convert(publAuthor);

		// find the matching agent record
		Agent agent = new Agent();
		Integer authorId = publAuthor.getAuthorId();

		if (authorId == null)
		{
			throw new LocalException("No author id");
		}
		Botanist botanist = new Botanist();
		botanist.setId(authorId);

		String guid = botanist.getGuid();

		Integer agentId = getIdByField("agent", "AgentID", "GUID", guid);

		agent.setAgentId(agentId);
		author.setAgent(agent);

		// find the matching referencework record
		ReferenceWork referenceWork = new ReferenceWork();
		Integer publicationId = publAuthor.getPublicationId();

		if (publicationId == null)
		{
			throw new LocalException("No publication id");
		}
		guid = String.valueOf(publicationId);
		
		Integer refWorkId = getIdByField("referencework", "ReferenceWorkID", "GUID", guid);

		referenceWork.setReferenceWorkId(refWorkId);        
		author.setReferenceWork(referenceWork);

		if (refWorkId != lastRefWorkId)
		{
			orderNumber = 1;
			lastRefWorkId = refWorkId;
		}
		else
		{
			orderNumber ++;
		}
		author.setOrderNumber((short) orderNumber);

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
			publAuthor.setPublicationId( Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
			publAuthor.setAuthorId(      Integer.parseInt(StringUtils.trimToNull( columns[1] ) ) );
			publAuthor.setOrdinal(       Integer.parseInt(StringUtils.trimToNull( columns[2] ) ) );
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return publAuthor;
	}

	private Author convert(PublAuthor publAuthor)
	{
		Author author = new Author();

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
