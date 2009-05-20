package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Publication;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublicationLoader extends CsvToSqlLoader
{
    public PublicationLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		Publication publication = parse(columns);

        // convert Publication into ReferenceWork
        ReferenceWork referenceWork = convertToReferenceWork(publication);

        // find matching record creator
        Integer creatorOptrId = publication.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        referenceWork.setCreatedByAgent(createdByAgent);
        
        // convert referencework to sql and insert
        Integer journalId = null;

         if (publication.isJournal())
         {
        	 Journal journal = referenceWork.getJournal();
             journal.setCreatedByAgent(createdByAgent);
        	 String sql = getInsertSql(journal);
        	 journalId = insert(sql);
        	 journal.setJournalId(journalId);
         }
         
         String sql = getInsertSql(referenceWork);
         insert(sql);
	}

	private Publication parse(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Wrong number of columns");
		}

		Publication publication = new Publication();
		try
		{
			publication.setId(Integer.parseInt(StringUtils.trimToNull(columns[0])));

			String isbn = StringUtils.trimToNull(columns[1]);
			publication.setIsbn(isbn);

			String pubPlace = StringUtils.trimToNull(columns[2]);
			publication.setPubPlace(pubPlace);

			String publisher = StringUtils.trimToNull(columns[3]);
			publication.setPublisher(publisher);

			String url = StringUtils.trimToNull(columns[4]);
			publication.setUrl(url);

			String title = StringUtils.trimToNull(columns[5]);
			publication.setTitle(title);

			String pubDate = StringUtils.trimToNull(columns[6]);
			publication.setPubDate(pubDate);

			String isJournal = StringUtils.trimToNull(columns[7]);
			publication.setJournal(isJournal != null);
			
			String issn = StringUtils.trimToNull(columns[8]);
			publication.setIsbn(issn);

			String bph = StringUtils.trimToNull(columns[9]);
			publication.setBph(bph);

			String abbreviation = StringUtils.trimToNull(columns[10]);
			publication.setAbbreviation(abbreviation);

            Integer optrId =   Integer.parseInt(StringUtils.trimToNull( columns[11] ));
            publication.setCreatedById(optrId);
            
            String createDateString =           StringUtils.trimToNull( columns[12] );
            Date createDate = SqlUtils.parseDate(createDateString);
            publication.setDateCreated(createDate);
            
			String remarks = SqlUtils.iso8859toUtf8(StringUtils.trimToNull(columns[13]));
			publication.setRemarks(remarks);
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return publication;
	}

	private ReferenceWork convertToReferenceWork(Publication publication) throws LocalException
	{

		ReferenceWork referenceWork = new ReferenceWork();

		// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
		referenceWork.setGuid(String.valueOf(publication.getId()));

		String isbn = publication.getIsbn();
		if (isbn != null && isbn.length() > 16)
		{
			warn("Truncating isbn", publication.getId(), isbn);
			isbn = isbn.substring(0, 16);
		}
		referenceWork.setIsbn(isbn);

		String pubPlace = publication.getPubPlace();
		if (pubPlace != null && pubPlace.length() > 50)
		{
			warn("Truncating publication place", publication.getId(), pubPlace);
			pubPlace = pubPlace.substring(0, 50);
		}
		referenceWork.setPlaceOfPublication(pubPlace);

		String publisher = publication.getPublisher();
		if (publisher != null && publisher.length() > 50)
		{
			warn("Truncating publisher", publication.getId(), publisher);
			publisher = publisher.substring(0, 50);
		}
		referenceWork.setPublisher(publisher);

		String pubDate = publication.getPubDate();
		if (pubDate != null && pubDate.length() > 25)
		{
			warn("Truncating publication date", publication.getId(), pubDate);
			pubDate = pubDate.substring(0, 25);
		}
		referenceWork.setWorkDate(pubDate);

		referenceWork.setUrl(publication.getUrl());

		String title = publication.getTitle();
		if (title == null)
		{
		    throw new LocalException("No title");
		}
		if (title.length() > 255)
		{
			warn("Truncating title", publication.getId(), title);
			title = title.substring(0, 255);
		}
		referenceWork.setTitle(title);

		referenceWork.setReferenceWorkType(ReferenceWork.BOOK);
		
        // TimestampCreated
        Date dateCreated = publication.getDateCreated();
        referenceWork.setTimestampCreated(DateUtils.toTimestamp(dateCreated));

		Journal journal = new Journal();
		
		if (publication.isJournal()) {

			// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
			journal.setGuid(String.valueOf(publication.getId()));

			String issn = publication.getIssn();
			if (issn != null && issn.length() > 16)
			{
				warn("Truncating issn", publication.getId(), issn);
				issn = issn.substring(0, 16);
			}
			journal.setIssn(issn);

			String abbreviation = publication.getAbbreviation();
			if (abbreviation != null && abbreviation.length() > 50)
			{
				warn("Truncating abbreviation", publication.getId(), abbreviation);
				abbreviation = abbreviation.substring(0, 50);
			}
			journal.setJournalAbbreviation(abbreviation);

			journal.setJournalName(title);

			journal.setText1(publication.getBph());
			
			journal.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
		}
		referenceWork.setJournal(journal);
		
		return referenceWork;
	}

	private String getInsertSql(Journal journal) throws LocalException
	{
		String fieldNames = "GUID, ISSN, JournalAbbreviation, JournalName, Text1, CreatedByAgentID, TimestampCreated";

		String[] values = new String[7];

		values[0] = SqlUtils.sqlString( journal.getGuid());
		values[1] = SqlUtils.sqlString( journal.getIssn());
		values[2] = SqlUtils.sqlString( journal.getJournalAbbreviation());
		values[3] = SqlUtils.sqlString( journal.getJournalName());
		values[4] = SqlUtils.sqlString( journal.getText1());
		values[5] = SqlUtils.sqlString( journal.getCreatedByAgent().getId());
        values[6] = SqlUtils.sqlString( journal.getTimestampCreated());

		return SqlUtils.getInsertSql("journal", fieldNames, values);
	}

	private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "GUID, ISBN, PlaceOfPublication, Publisher, ReferenceWorkType, " +
		"Title, URL, WorkDate, JournalID, CreatedByAgentID, TimestampCreated, Remarks";

		String[] values = new String[12];

		values[0]  = SqlUtils.sqlString( referenceWork.getGuid());
		values[1]  = SqlUtils.sqlString( referenceWork.getIsbn());
		values[2]  = SqlUtils.sqlString( referenceWork.getPlaceOfPublication());
		values[3]  = SqlUtils.sqlString( referenceWork.getPublisher());
		values[4]  = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[5]  = SqlUtils.sqlString( referenceWork.getTitle());
		values[6]  = SqlUtils.sqlString( referenceWork.getUrl());
		values[7]  = SqlUtils.sqlString( referenceWork.getWorkDate());
		values[8]  = SqlUtils.sqlString( referenceWork.getJournal().getJournalId());
		values[9]  = SqlUtils.sqlString( referenceWork.getCreatedByAgent().getId());
        values[10] = SqlUtils.sqlString( referenceWork.getTimestampCreated());
		values[11] = SqlUtils.sqlString( referenceWork.getRemarks());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
