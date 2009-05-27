package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Publication;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.ReferenceWorkLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublicationLoader extends AuditedObjectLoader
{
    public ReferenceWorkLookup getReferenceWorkLookup()
    {
        if (refWorkLookup == null)
        {
            refWorkLookup = new ReferenceWorkLookup() {
                
                public  ReferenceWork getByPublicationId(Integer publicationId) throws LocalException
                {
                    ReferenceWork referenceWork = new ReferenceWork();
                    
                    String guid = getGuid(publicationId);
                    
                    Integer referenceWorkId = getInt("referenceWork", "ReferenceWorkID", "GUID", guid);
                    
                    referenceWork.setReferenceWorkId(referenceWorkId);
                    
                    return referenceWork;
                }
                
            };
        }
        return refWorkLookup;
    }

    private String getGuid(Integer publicationId)
	{
		return publicationId + " publication";
	}

	private ReferenceWorkLookup refWorkLookup;
	
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

	private Publication parse(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Wrong number of columns");
		}

		Publication publication = new Publication();
		try
		{
			publication.setId(           SqlUtils.parseInt( StringUtils.trimToNull( columns[0] )));
			publication.setIsbn(                            StringUtils.trimToNull( columns[1] ));
			publication.setPubPlace(                        StringUtils.trimToNull( columns[2] ));
			publication.setPublisher(                       StringUtils.trimToNull( columns[3] ));
			publication.setUrl(                             StringUtils.trimToNull( columns[4] ));
			publication.setTitle(                           StringUtils.trimToNull( columns[5] ));
			publication.setPubDate(                         StringUtils.trimToNull( columns[6] ));
			publication.setJournal(   Boolean.parseBoolean( StringUtils.trimToNull( columns[7] )));
			publication.setIsbn(                            StringUtils.trimToNull( columns[8] ));
			publication.setBph(                             StringUtils.trimToNull( columns[9] ));
			publication.setAbbreviation(                    StringUtils.trimToNull( columns[10] ));
            publication.setCreatedById(  SqlUtils.parseInt( StringUtils.trimToNull( columns[11] )));
            publication.setDateCreated( SqlUtils.parseDate( StringUtils.trimToNull( columns[12] )));
			publication.setRemarks( SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[13] )));
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
		String remarks = publication.getRemarks();
		referenceWork.setRemarks(remarks);
		
        // TimestampCreated
        Date dateCreated = publication.getDateCreated();
        referenceWork.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		// Title
		String title = publication.getTitle();
		checkNull(title, "title");
		title = truncate(title, 255, "title");
		referenceWork.setTitle(title);

		// URL
		String url = publication.getUrl();
		referenceWork.setUrl(url);

		// WorkDate
		String pubDate = publication.getPubDate();
		if (pubDate != null)
		{
			pubDate = truncate(pubDate, 25, "date of publication");
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

		// ISBN
		String issn = publication.getIssn();
		if (issn != null)
		{
			issn = truncate(issn, 16, "ISBN");
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
		checkNull(title, "title");
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

	private String getInsertSql(Journal journal) throws LocalException
	{
		String fieldNames = "GUID, ISSN, JournalAbbreviation, JournalName, Text1, TimestampCreated";

		String[] values = new String[6];

		values[0] = SqlUtils.sqlString( journal.getGuid());
		values[1] = SqlUtils.sqlString( journal.getIssn());
		values[2] = SqlUtils.sqlString( journal.getJournalAbbreviation());
		values[3] = SqlUtils.sqlString( journal.getJournalName());
		values[4] = SqlUtils.sqlString( journal.getText1());
        values[5] = SqlUtils.sqlString( journal.getTimestampCreated());

		return SqlUtils.getInsertSql("journal", fieldNames, values);
	}

	private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "CreatedByAgentID, GUID, ISBN, JournalID, PlaceOfPublication, Publisher, " +
				            "ReferenceWorkType, Remarks, TimestampCreated, Title, URL, WorkDate";

		String[] values = new String[12];

		values[0]  = SqlUtils.sqlString( referenceWork.getCreatedByAgent().getId());
		values[1]  = SqlUtils.sqlString( referenceWork.getGuid());
		values[2]  = SqlUtils.sqlString( referenceWork.getIsbn());
		values[3]  = SqlUtils.sqlString( referenceWork.getJournal().getJournalId());
		values[4]  = SqlUtils.sqlString( referenceWork.getPlaceOfPublication());
		values[5]  = SqlUtils.sqlString( referenceWork.getPublisher());
		values[6]  = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[7]  = SqlUtils.sqlString( referenceWork.getRemarks());
        values[8]  = SqlUtils.sqlString( referenceWork.getTimestampCreated());
        values[9]  = SqlUtils.sqlString( referenceWork.getTitle());
		values[10] = SqlUtils.sqlString( referenceWork.getUrl());
		values[11] = SqlUtils.sqlString( referenceWork.getWorkDate());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
