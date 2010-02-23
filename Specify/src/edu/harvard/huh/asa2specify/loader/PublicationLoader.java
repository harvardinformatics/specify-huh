package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Publication;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PublicationLookup;
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
        Publication publication = new Publication();
        
        int i = super.parse(columns, publication);
        
		if (columns.length < i + 11)
		{
			throw new LocalException("Not enough columns");
		}
		try
		{
			publication.setIsbn(                            columns[i + 0]  );
			publication.setPubPlace(                        columns[i + 1]  );
			publication.setPublisher(                       columns[i + 2]  );
			publication.setUrl(                             columns[i + 3]  );
			publication.setTitle(                           columns[i + 4]  );
			publication.setPubDate(                         columns[i + 5]  );
			publication.setIsJournal( Boolean.parseBoolean( columns[i + 6]  ));
			publication.setIssn(                            columns[i + 7]  );
			publication.setBph(                             columns[i + 8]  );
			publication.setAbbreviation(                    columns[i + 9]  );
			publication.setRemarks( SqlUtils.iso8859toUtf8( columns[i + 10] ));
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
		
		// GUID: temporarily hold asa publication.id TODO: don't forget to unset this after migration
		Integer publicationId = publication.getId();
		checkNull(publicationId, "id");
		
        String guid = getGuid(publicationId);
		referenceWork.setGuid(guid);
		
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
		
		setAuditFields(publication, referenceWork);
		
		return referenceWork;
	}

    private String getGuid(Integer publicationId)
	{
		return publicationId + " publication";
	}

	private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "CreatedByAgentID, GUID, ModifiedByAgentID, PlaceOfPublication, " +
				            "Publisher, ReferenceWorkType, Remarks, Text1, Text2, TimestampCreated, " +
				            "TimestampModified, Title, URL, Version, WorkDate";

		String[] values = new String[15];

		values[0]  = SqlUtils.sqlString( referenceWork.getCreatedByAgent().getId());
		values[1]  = SqlUtils.sqlString( referenceWork.getGuid());
		values[2]  = SqlUtils.sqlString( referenceWork.getModifiedByAgent().getId());
		values[3]  = SqlUtils.sqlString( referenceWork.getPlaceOfPublication());
		values[4]  = SqlUtils.sqlString( referenceWork.getPublisher());
		values[5]  = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[6]  = SqlUtils.sqlString( referenceWork.getRemarks());
		values[7]  = SqlUtils.sqlString( referenceWork.getText1());
		values[8]  = SqlUtils.sqlString( referenceWork.getText2());
        values[9]  = SqlUtils.sqlString( referenceWork.getTimestampCreated());
        values[10] = SqlUtils.sqlString( referenceWork.getTimestampModified());
        values[11] = SqlUtils.sqlString( referenceWork.getTitle());
		values[12] = SqlUtils.sqlString( referenceWork.getUrl());
		values[13] = SqlUtils.zero();
		values[14] = SqlUtils.sqlString( referenceWork.getWorkDate());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
