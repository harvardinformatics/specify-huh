package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import edu.harvard.huh.asa.Publication;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublicationLoader extends AuditedObjectLoader
{
 	private PublicationLookup publicationLookup;
	
	public PublicationLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
		super(csvFile, sqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Publication publication = parse(columns);

		Integer publicationId = publication.getId();
		setCurrentRecordId(publicationId);
		
		Journal journal = null;

		if (publication.isJournal())
		{
			journal = getJournal(publication);
			String sql = getInsertSql(journal);
			Integer journalId = insert(sql);
			journal.setJournalId(journalId);
		}
		else
		{
			journal = new Journal();
		}

		ReferenceWork referenceWork = getReferenceWork(publication, journal);

		String sql = getInsertSql(referenceWork);
		insert(sql);
	}
	
	public PublicationLookup getReferenceWorkLookup()
	{
		if (publicationLookup == null)
		{
			publicationLookup = new PublicationLookup() {

				public  ReferenceWork getById(Integer publicationId) throws LocalException
				{
					ReferenceWork referenceWork = new ReferenceWork();

					String guid = getGuid(publicationId);

					Integer referenceWorkId = getId("referencework", "ReferenceWorkID", "GUID", guid);

					referenceWork.setReferenceWorkId(referenceWorkId);

					return referenceWork;
				}

			};
		}
		return publicationLookup;
	}

	private Publication parse(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Not enough columns");
		}

		Publication publication = new Publication();
		try
		{
			publication.setId(           SqlUtils.parseInt( columns[0]  ));
			publication.setIsbn(                            columns[1]  );
			publication.setPubPlace(                        columns[2]  );
			publication.setPublisher(                       columns[3]  );
			publication.setUrl(                             columns[4]  );
			publication.setTitle(                           columns[5]  );
			publication.setPubDate(                         columns[6]  );
			publication.setIsJournal( Boolean.parseBoolean( columns[7]  ));
			publication.setIssn(                            columns[8]  );
			publication.setBph(                             columns[9]  );
			publication.setAbbreviation(                    columns[10] );
            publication.setCreatedById(  SqlUtils.parseInt( columns[11] ));
            publication.setDateCreated( SqlUtils.parseDate( columns[12] ));
			publication.setRemarks( SqlUtils.iso8859toUtf8( columns[13] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return publication;
	}

	private ReferenceWork getReferenceWork(Publication publication, Journal journal) throws LocalException
	{		
		ReferenceWork referenceWork = new ReferenceWork();

		// CreatedByAgent
        Integer creatorOptrId = publication.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        referenceWork.setCreatedByAgent(createdByAgent);
        
		// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
		Integer publicationId = publication.getId();
		checkNull(publicationId, "id");
		
        String guid = getGuid(publicationId);
		referenceWork.setGuid(guid);

		// ISBN
		String isbn = publication.getIsbn();
		if (isbn != null)
		{
			isbn = truncate(isbn, 16, "ISBN");
			referenceWork.setIsbn(isbn);
		}
		
		// Journal
		referenceWork.setJournal(journal);
		
		// PlaceOfPublication
		String pubPlace = publication.getPubPlace();
		if (pubPlace != null)
		{
			pubPlace = truncate(pubPlace, 50, "place of publication");
			referenceWork.setPlaceOfPublication(pubPlace);
		}

		// Publisher
		String publisher = publication.getPublisher();
		if (publisher != null)
		{
			publisher = truncate(publisher, 50, "publisher");
			referenceWork.setPublisher(publisher);
		}

		// ReferenceWorkType
		referenceWork.setReferenceWorkType(ReferenceWork.BOOK);
		
		// Remarks
		String pubDate = publication.getPubDate();
		String remarks = publication.getRemarks();
		if (remarks == null) remarks = pubDate;
		else if (pubDate != null) remarks = remarks + "[publication date: " + pubDate + "]";
		
		referenceWork.setRemarks(remarks);
		
		// Text1 (abbreviation)
		String abbrev = publication.getAbbreviation();
		if (journal == null) referenceWork.setText1(abbrev);

		// Text2 (collation)
		
		// TimestampCreated
        Date dateCreated = publication.getDateCreated();
        referenceWork.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		// Title
		String title = publication.getTitle();
		if (title == null)
		{
		    getLogger().warn(rec() + "Empty title");
		    title = EMPTY;
		    publication.setTitle(title);
		}
		title = truncate(title, 255, "title");
		referenceWork.setTitle(title);

		// URL
		String url = publication.getUrl();
		referenceWork.setUrl(url);

		// WorkDate
		if (pubDate != null)
		{
			if (pubDate.length() > 25) pubDate = pubDate.substring(0, 25);  // too many are much longer than this to call truncate
			referenceWork.setWorkDate(pubDate);
		}
		
		return referenceWork;
	}

	private Journal getJournal(Publication publication) throws LocalException
	{
		Integer publicationId = publication.getId();
		checkNull(publicationId, "id");
		
		Journal journal = new Journal();
		
		// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
		String guid = getGuid(publicationId);
		journal.setGuid(guid);

		// ISSN
		String issn = publication.getIssn();
		if (issn != null)
		{
			issn = truncate(issn, 16, "ISSN");
			journal.setIssn(issn);
		}

		// JournalAbbreviation
		String abbreviation = publication.getAbbreviation();
		if (abbreviation != null)
		{
			abbreviation = truncate(abbreviation, 50, "abbreviation");
			journal.setJournalAbbreviation(abbreviation);
		}

		// JournalName
		String title = publication.getTitle();
        if (title == null)
        {
            getLogger().warn(rec() + "Empty title");
            title = EMPTY;
            publication.setTitle(title);
        }
		title = truncate(title, 255, "title");
		journal.setJournalName(title);

		// Text1 (BPH)
		String bph = publication.getBph();
		journal.setText1(bph);
		
		// TimestampCreated
		Date dateCreated = publication.getDateCreated();
		journal.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
		
		return journal;
	}

    private String getGuid(Integer publicationId)
	{
		return publicationId + " publication";
	}
    
	private String getInsertSql(Journal journal) throws LocalException
	{
		String fieldNames = "GUID, ISSN, JournalAbbreviation, JournalName, Text1, TimestampCreated, Version";

		String[] values = new String[7];

		values[0] = SqlUtils.sqlString( journal.getGuid());
		values[1] = SqlUtils.sqlString( journal.getIssn());
		values[2] = SqlUtils.sqlString( journal.getJournalAbbreviation());
		values[3] = SqlUtils.sqlString( journal.getJournalName());
		values[4] = SqlUtils.sqlString( journal.getText1());
        values[5] = SqlUtils.sqlString( journal.getTimestampCreated());
        values[6] = SqlUtils.zero();
        
		return SqlUtils.getInsertSql("journal", fieldNames, values);
	}

	private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "CreatedByAgentID, GUID, ISBN, JournalID, PlaceOfPublication, Publisher, " +
				            "ReferenceWorkType, Remarks, Text1, TimestampCreated, Title, URL, Version, WorkDate";

		String[] values = new String[14];

		values[0]  = SqlUtils.sqlString( referenceWork.getCreatedByAgent().getId());
		values[1]  = SqlUtils.sqlString( referenceWork.getGuid());
		values[2]  = SqlUtils.sqlString( referenceWork.getIsbn());
		values[3]  = SqlUtils.sqlString( referenceWork.getJournal().getJournalId());
		values[4]  = SqlUtils.sqlString( referenceWork.getPlaceOfPublication());
		values[5]  = SqlUtils.sqlString( referenceWork.getPublisher());
		values[6]  = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[7]  = SqlUtils.sqlString( referenceWork.getRemarks());
		values[8]  = SqlUtils.sqlString( referenceWork.getText1());
        values[9]  = SqlUtils.sqlString( referenceWork.getTimestampCreated());
        values[10] = SqlUtils.sqlString( referenceWork.getTitle());
		values[11] = SqlUtils.sqlString( referenceWork.getUrl());
		values[12] = SqlUtils.zero();
		values[13] = SqlUtils.sqlString( referenceWork.getWorkDate());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
