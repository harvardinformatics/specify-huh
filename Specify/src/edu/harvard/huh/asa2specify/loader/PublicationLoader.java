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
		
		ReferenceWork referenceWork = getReferenceWork(publication);

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

	private ReferenceWork getReferenceWork(Publication publication) throws LocalException
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
		
		// PlaceOfPublication
		String pubPlace = publication.getPubPlace();
		if (pubPlace != null)
		{
			pubPlace = truncate(pubPlace, 64, "place of publication");
			referenceWork.setPlaceOfPublication(pubPlace);
		}

		// Publisher
		String publisher = publication.getPublisher();
		if (publisher != null)
		{
			publisher = truncate(publisher, 64, "publisher");
			referenceWork.setPublisher(publisher);
		}

		// ReferenceWorkType
		referenceWork.setReferenceWorkType(ReferenceWork.BOOK);
		
		// Remarks
		String remarks = publication.getRemarks();
		referenceWork.setRemarks(remarks);
		
		// Text1 (title)
		String title = publication.getTitle();
		referenceWork.setText1(title);

		// Text2 (publ date)
		String pubDate = publication.getPubDate();
		referenceWork.setText2(pubDate);
	      
		// TimestampCreated
        Date dateCreated = publication.getDateCreated();
        referenceWork.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		// Title (abbreviation)
        String abbrev = publication.getAbbreviation();
        if (abbrev == null)
        {
            getLogger().warn(rec() + "Empty abbreviation");
            abbrev = EMPTY;
            publication.setAbbreviation(abbrev);
        }
        referenceWork.setTitle(abbrev);

		// URL
		String url = publication.getUrl();
		referenceWork.setUrl(url);

		// WorkDate
		
		return referenceWork;
	}

    private String getGuid(Integer publicationId)
	{
		return publicationId + " publication";
	}

	private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "CreatedByAgentID, GUID, ISBN, PlaceOfPublication, Publisher, " +
				            "ReferenceWorkType, Remarks, Text1, Text2, TimestampCreated, " +
				            "Title, URL, Version, WorkDate";

		String[] values = new String[14];

		values[0]  = SqlUtils.sqlString( referenceWork.getCreatedByAgent().getId());
		values[1]  = SqlUtils.sqlString( referenceWork.getGuid());
		values[2]  = SqlUtils.sqlString( referenceWork.getIsbn());
		values[3]  = SqlUtils.sqlString( referenceWork.getPlaceOfPublication());
		values[4]  = SqlUtils.sqlString( referenceWork.getPublisher());
		values[5]  = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[6]  = SqlUtils.sqlString( referenceWork.getRemarks());
		values[7]  = SqlUtils.sqlString( referenceWork.getText1());
		values[8]  = SqlUtils.sqlString( referenceWork.getText2());
        values[9]  = SqlUtils.sqlString( referenceWork.getTimestampCreated());
        values[10] = SqlUtils.sqlString( referenceWork.getTitle());
		values[11] = SqlUtils.sqlString( referenceWork.getUrl());
		values[12] = SqlUtils.zero();
		values[13] = SqlUtils.sqlString( referenceWork.getWorkDate());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
