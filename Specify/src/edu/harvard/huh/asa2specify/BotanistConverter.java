package edu.harvard.huh.asa2specify;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Botanist;
import edu.ku.brc.specify.datamodel.Agent;

public class BotanistConverter {

    private static final Logger log           = Logger.getLogger( BotanistConverter.class );
    
    private static final Pattern groupPattern = Pattern.compile("&|et al| and ");
    private static final Pattern orgPattern   = Pattern.compile("Bureau of|Commission|Committee|Consortium|Department|Expedition|Group|Herbarium|Missionaries|Museum|National|Nurseries|Nursery|Program|Research|School|Scientific|Service|Society|Survey|University");

    private static BotanistConverter instance = new BotanistConverter();
    
    private BotanistConverter() {
        ;
    }
    
    public static BotanistConverter getInstance() {
        return instance;
    }

    public Agent convert( Botanist botanist ) {
        
		Agent agent = new Agent();

		// NOTE: as of 11:46 am Feb 27 2009, each botanist record that lacks a full name
		// has both an author name and collector name and they are equal; further, the
		// author/collector name contains an ampersand (and a positive team flag)
		String fullName = botanist.getName();
		
		// AgentType
		// TODO: some groups are not marked as such; handle that somehow
		boolean isCorporate = botanist.isCorporate() || isOrg( fullName );
		boolean isGroup = botanist.isTeam() || isGroup( fullName );
		boolean isPerson = !isCorporate && !isGroup;
		
		if ( isCorporate ) agent.setAgentType( Agent.ORG );
		else if ( isGroup ) agent.setAgentType( Agent.GROUP );
		else agent.setAgentType( Agent.PERSON );

		// GUID: temporarily hold asa botanist.id TODO: don't forget to unset this after migration
		agent.setGuid( botanist.getGuid() );

		// DateOfBirth: this is going to hold start dates no matter what the type for the time being
		Integer startYear = botanist.getStartYear();
		if (startYear != null) {
		    Calendar c = Calendar.getInstance();
		    c.clear();
		    c.set(Calendar.YEAR, startYear);
		    
		    agent.setDateOfBirth(c);
		}
		
		// DateOfDeath: this is going to hold end dates no matter what the type for the time being
        Integer endYear = botanist.getEndYear();
        if (endYear != null) {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Calendar.YEAR, endYear);
            
            agent.setDateOfDeath(c);
        }
        
		// LastName
		String lastName = null;
		
		if ( isPerson && fullName.contains( "," ) ) {

		    int commaIndex = fullName.indexOf( ',' );

		    // If there's at least one comma, put the first bit until the comma into last name

		    lastName = fullName.substring( 0, commaIndex ).trim();

		    if ( lastName.length() > 50 ) {
		        log.warn( "truncating last name" );
		        lastName = lastName.substring( 0, 50 );
		    }
		    agent.setLastName( lastName );

		    String firstName = fullName.substring( commaIndex + 1 ).trim();
		    if ( firstName.length() > 50 ) {
		        log.warn( "truncating last name" );
		        firstName = firstName.substring( 0, 50 );
		    }
		    agent.setFirstName( firstName );
		}

		else {
		    // otherwise, put the whole thing into last name
		    if ( fullName.length() > 50 ) {
		        log.warn( "truncating last name" );
		        fullName = fullName.substring( 0, 50 );
		    }
		    agent.setLastName( fullName );
		}
	    // Remarks
        String remarks = botanist.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks( remarks );
        }

        return agent;
	}

    private boolean isGroup( String name ) {
		
		Matcher m = groupPattern.matcher( name );
		return m.matches();
	}
	
	private boolean isOrg( String name ) {
	    Matcher m = orgPattern.matcher( name );
	    return m.matches();
	}
}
