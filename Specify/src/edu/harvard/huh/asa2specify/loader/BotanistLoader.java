package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Botanist;
import edu.ku.brc.specify.datamodel.Agent;

public class BotanistLoader extends CsvToSqlLoader {
    private final Logger log = Logger.getLogger(BotanistLoader.class);

	public BotanistLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
	}

	public void loadRecord(String[] columns) throws LocalException {

		Botanist botanist = parseBotanistRecord(columns);

        // convert botanist into agent ...
        Agent agent = convert( botanist );

        // convert agent to sql and insert
        String sql = getInsertSql(agent);
        insert(sql);
	}

    // id, isTeam, isCorporate, name, datesType, startYear, startPrecision, endYear, endPrecision, remarks
    private Botanist parseBotanistRecord(String[] columns) throws LocalException
    {
        if (columns.length < 10)
        {
            throw new LocalException("Wrong number of columns");
        }

        // assign values to Botanist object
        Botanist botanist = new Botanist();
        try {
            botanist.setId(Integer.parseInt(StringUtils.trimToNull( columns[0] ) ) );

            String isTeamStr = StringUtils.trimToNull( columns[1] );
            boolean isTeam = isTeamStr != null && isTeamStr.equals( "true" );
            botanist.setTeam( isTeam );

            String isCorporateStr = StringUtils.trimToNull( columns[2] );
            boolean isCorporate = isCorporateStr != null && isCorporateStr.equals( "true" );
            botanist.setCorporate( isCorporate );

            String name = StringUtils.trimToNull( columns[3] );
            if (name != null)
            {
                botanist.setName( name );
            }
            else {
                throw new LocalException( "No name found in record ");
            }

            // no place to put this at the moment: birth/death, flourished, collected, received specimens
            String datesType = StringUtils.trimToNull( columns[4] );
            if ( datesType != null )
            {
                botanist.setDatesType( datesType );
            }

            String startYearStr = StringUtils.trimToNull( columns[5] );
            if ( startYearStr != null )
            {
                botanist.setStartYear( Integer.parseInt( startYearStr ) );
            }

            // doing nothing with this at the moment: ?, circa; null means default, exact
            String startPrecision = StringUtils.trimToNull( columns[6] );
            if ( startPrecision != null )
            {
                botanist.setStartPrecision( startPrecision );
            }

            String endYearStr = StringUtils.trimToNull( columns[7] );
            if ( endYearStr != null )
            {
                botanist.setEndYear( Integer.parseInt( endYearStr ) );
            }

            // no place to put this at the moment: ?, circa; null means default, exact
            String endPrecision = StringUtils.trimToNull( columns[8] );
            if ( endPrecision != null ) {
                botanist.setEndPrecision( startPrecision );
            }

            String remarks = StringUtils.trimToNull( columns[9] );
            if ( remarks != null )
            {
                botanist.setRemarks(SqlUtils.iso8859toUtf8(remarks));
            }
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return botanist;
    }

    public Agent convert( Botanist botanist ) {
        
		Agent agent = new Agent();

		// NOTE: as of 11:46 am Feb 27 2009, each botanist record that lacks a full name
		// has both an author name and collector name and they are equal; further, the
		// author/collector name contains an ampersand (and a positive team flag)
		
		// AgentType
		if (botanist.isOrganization() ) agent.setAgentType( Agent.ORG );
		else if (botanist.isGroup()) agent.setAgentType( Agent.GROUP );
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
		String lastName = botanist.getLastName();
		if ( lastName.length() > 50 ) {
            log.warn( "truncating last name" );
            lastName = lastName.substring( 0, 50 );
        }
        agent.setLastName( lastName );
        
        // FirstName
		String firstName = botanist.getFirstName();
		if ( firstName.length() > 50 ) {
            log.warn( "truncating last name" );
            firstName = firstName.substring( 0, 50 );
        }
        agent.setFirstName( firstName );

		// Remarks
        String remarks = botanist.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks( remarks );
        }

        return agent;
	}

	
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, DateOfBirth, DateOfDeath, FirstName, LastName, TimestampCreated, Remarks";

		List<String> values = new ArrayList<String>(8);

		values.add(    String.valueOf(agent.getAgentType()   ));
		values.add(SqlUtils.sqlString(agent.getGuid()        ));
		values.add(SqlUtils.sqlString(agent.getDateOfBirth() ));
		values.add(SqlUtils.sqlString(agent.getDateOfDeath() ));
		values.add(SqlUtils.sqlString(agent.getFirstName()   ));
		values.add(SqlUtils.sqlString(agent.getLastName()    ));
		values.add("now()" );
		values.add(SqlUtils.sqlString(agent.getRemarks()     ));

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
}
