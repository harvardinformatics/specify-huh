package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Publication;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublicationLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(PublicationLoader.class);

	public PublicationLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		Publication publication = parsePublicationRecord(columns);

        // convert Publication into ReferenceWork
        ReferenceWork referenceWork = convertToReferenceWork(publication);

        // convert referencework to sql and insert
        Integer journalId = null;

         if (publication.isJournal())
         {
        	 Journal journal = referenceWork.getJournal();
        	 String sql = getInsertSql(journal);
        	 journalId = insert(sql);
        	 journal.setJournalId(journalId);
         }
         
         String sql = getInsertSql(referenceWork);
         insert(sql);
	}

	private Publication parsePublicationRecord(String[] columns) throws LocalException
	{
		if (columns.length < 12)
		{
			throw new LocalException("Wrong number of columns");
		}

		Publication publication = new Publication();
		try {
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
			if (title == null) throw new LocalException("No title found in record");
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

			String remarks = SqlUtils.iso8859toUtf8(StringUtils.trimToNull(columns[11]));
			publication.setRemarks(remarks);
		}
		catch (NumberFormatException e) {
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return publication;
	}

	private ReferenceWork convertToReferenceWork(Publication publication) {

		ReferenceWork referenceWork = new ReferenceWork();

		// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
		referenceWork.setGuid(String.valueOf(publication.getId()));

		String isbn = publication.getIsbn();
		if ( isbn != null && isbn.length() > 16 ) {
			log.warn( "truncating isbn" );
			isbn = isbn.substring(0, 16);
		}
		referenceWork.setIsbn(isbn);

		String pubPlace = publication.getPubPlace();
		if ( pubPlace != null && pubPlace.length() > 50 ) {
			log.warn( "truncating publication place" );
			pubPlace = pubPlace.substring(0, 50);
		}
		referenceWork.setPlaceOfPublication(pubPlace);

		String publisher = publication.getPublisher();
		if ( publisher != null && publisher.length() > 50 ) {
			log.warn( "truncating publisher" );
			publisher = publisher.substring(0, 50);
		}
		referenceWork.setPublisher(publisher);

		String pubDate = publication.getPubDate();
		if ( pubDate != null && pubDate.length() > 25 ) {
			log.warn( "truncating publication date" );
			pubDate = pubDate.substring(0, 25);
		}
		referenceWork.setWorkDate(pubDate);

		referenceWork.setUrl(publication.getUrl());

		String title = publication.getTitle();
		if ( title != null && title.length() > 255) {
			log.warn( "truncating title" );
			title = title.substring(0, 255);
		}
		referenceWork.setTitle(title);

		referenceWork.setReferenceWorkType(ReferenceWork.BOOK);

		Journal journal = new Journal();
		
		if (publication.isJournal()) {

			// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
			journal.setGuid(String.valueOf(publication.getId()));

			String issn = publication.getIssn();
			if ( issn != null && issn.length() > 16 ) {
				log.warn( "truncating issn" );
				issn = issn.substring(0, 16);
			}
			journal.setIssn(issn);

			String abbreviation = publication.getAbbreviation();
			if ( abbreviation != null && abbreviation.length() > 50 ) {
				log.warn( "truncating abbreviation" );
				abbreviation = abbreviation.substring(0, 50);
			}
			journal.setJournalAbbreviation(abbreviation);

			journal.setJournalName(title);

			journal.setText1(publication.getBph());
		}
		referenceWork.setJournal(journal);
		
		return referenceWork;
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
		values[5] = "now()";

		return SqlUtils.getInsertSql("journal", fieldNames, values);
	}

	private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "GUID, ISBN, PlaceOfPublication, Publisher, ReferenceWorkType, " +
		"Title, URL, WorkDate, JournalID, TimestampCreated, Remarks";

		String[] values = new String[11];

		values[0]  = SqlUtils.sqlString( referenceWork.getGuid());
		values[1]  = SqlUtils.sqlString( referenceWork.getIsbn());
		values[2]  = SqlUtils.sqlString( referenceWork.getPlaceOfPublication());
		values[3]  = SqlUtils.sqlString( referenceWork.getPublisher());
		values[4]  =     String.valueOf( referenceWork.getReferenceWorkType());
		values[5]  = SqlUtils.sqlString( referenceWork.getTitle());
		values[6]  = SqlUtils.sqlString( referenceWork.getUrl());
		values[7]  = SqlUtils.sqlString( referenceWork.getWorkDate());
		values[8]  =     String.valueOf( referenceWork.getJournal().getJournalId());
		values[9]  = "now()";
		values[10] = SqlUtils.sqlString( referenceWork.getRemarks());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
