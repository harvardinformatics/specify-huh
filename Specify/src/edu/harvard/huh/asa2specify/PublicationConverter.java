package edu.harvard.huh.asa2specify;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Publication;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class PublicationConverter {

    private static final Logger log           = Logger.getLogger( PublicationConverter.class );
    
    private static PublicationConverter instance = new PublicationConverter();
    
    private PublicationConverter() {
        ;
    }
    
    public static PublicationConverter getInstance() {
        return instance;
    }

    public ReferenceWork convertToReferenceWork(Publication publication) {
        
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

        return referenceWork;
	}
    
    public Journal convertToJournal(Publication publication) {
        
        Journal journal = new Journal();
        
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
        
        String title = publication.getTitle();
        if ( title != null && title.length() > 255) {
            log.warn( "truncating title" );
            title = title.substring(0, 255);
        }
        journal.setJournalName(title);
        
        journal.setText1(publication.getBph());

        return journal;
    }
}
