package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.PublAuthor;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublAuthorLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(PublAuthorLoader.class);

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
		PublAuthor publAuthor = parsePublAuthorRecord(columns);

		// convert BotanistRoleCountry into AgentGeography
		Author author = convert(publAuthor);

		// find the matching agent record
		Agent agent = new Agent();
		Integer authorId = publAuthor.getAuthorId();

		if (authorId != null)
		{
			throw new LocalException("No author id");
		}
		Botanist botanist = new Botanist();
		botanist.setId(authorId);

		String guid = SqlUtils.sqlString(botanist.getGuid());
		String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

		Integer agentId = queryForId(sql);

		if (agentId == null)
		{
			throw new LocalException("Couldn't find AgentID with GUID " + guid);
		}
		agent.setAgentId(agentId);
		author.setAgent(agent);

		// find the matching referencework record
		ReferenceWork referenceWork = new ReferenceWork();
		Integer publicationId = publAuthor.getPublicationId();

		if (publicationId != null)
		{
			throw new LocalException("No publication id");
		}
		guid = SqlUtils.sqlString(String.valueOf(publicationId));
		sql = SqlUtils.getQueryIdByFieldSql("referencework", "ReferenceWorkID", "GUID", guid);

		Integer refWorkId = queryForId(sql);

		if (refWorkId == null)
		{
			throw new LocalException("Couldn't find ReferenceWorkID with GUID " + guid);
		}
		referenceWork.setReferenceWorkId(refWorkId);        
		author.setReferenceWork(referenceWork);

		if (refWorkId != lastRefWorkId) {
			orderNumber = 1;
			lastRefWorkId = refWorkId;
		}
		else {
			orderNumber ++;
		}
		author.setOrderNumber((short) orderNumber);

		// convert agentspecialty to sql and insert
		sql = getInsertSql(author);
		insert(sql);
	}

	private PublAuthor parsePublAuthorRecord(String[] columns) throws LocalException
	{
		if (columns.length < 3)
		{
			throw new LocalException("Wrong number of columns");
		}

		PublAuthor publAuthor = new PublAuthor();
		try {
			publAuthor.setPublicationId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );
			publAuthor.setAuthorId(Integer.parseInt(StringUtils.trimToNull( columns[1] ) ) );
			publAuthor.setOrdinal(Integer.parseInt(StringUtils.trimToNull( columns[2] ) ) );
		}
		catch (NumberFormatException e) {
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

		List<String> values = new ArrayList<String>(4);

		values.add(String.valueOf(author.getAgent().getId()        ));
		values.add(String.valueOf(author.getReferenceWork().getId()));
		values.add(String.valueOf(author.getOrderNumber()          ));
		values.add("now()");

		return SqlUtils.getInsertSql("author", fieldNames, values);
	}
}
