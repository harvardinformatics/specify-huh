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
        
		if (columns.length < i + 7)
		{
			throw new LocalException("Not enough columns");
		}
		try
		{
			publication.setPubPlace(                        columns[i + 0]  );
			publication.setPublisher(                       columns[i + 1]  );
			publication.setUrl(                             columns[i + 2]  );
			publication.setTitle(                           columns[i + 3]  );
			publication.setPubDate(                         columns[i + 4]  );
			publication.setAbbreviation(                    columns[i + 5]  );
			publication.setRemarks( SqlUtils.iso8859toUtf8( columns[i + 6] ));
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
		
		// Text1 (title)
		String title = publication.getTitle();
		title = truncate(title, 255, "title");
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

		String[] values = {
				SqlUtils.sqlString( referenceWork.getCreatedByAgent().getId()),
				SqlUtils.sqlString( referenceWork.getGuid()),
				SqlUtils.sqlString( referenceWork.getModifiedByAgent().getId()),
				SqlUtils.sqlString( referenceWork.getPlaceOfPublication()),
				SqlUtils.sqlString( referenceWork.getPublisher()),
				SqlUtils.sqlString( referenceWork.getReferenceWorkType()),
				SqlUtils.sqlString( referenceWork.getRemarks()),
				SqlUtils.sqlString( referenceWork.getText1()),
				SqlUtils.sqlString( referenceWork.getText2()),
				SqlUtils.sqlString( referenceWork.getTimestampCreated()),
				SqlUtils.sqlString( referenceWork.getTimestampModified()),
				SqlUtils.sqlString( referenceWork.getTitle()),
				SqlUtils.sqlString( referenceWork.getUrl()),
				SqlUtils.one(),
				SqlUtils.sqlString( referenceWork.getWorkDate())
		};

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
}
