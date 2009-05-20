package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Optr;
import edu.ku.brc.specify.datamodel.Agent;

public class BotanistLoader extends CsvToSqlLoader
{
    private static Hashtable<Integer, Integer> optrIdsByBotanistId = new Hashtable<Integer, Integer>();
	
    public BotanistLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
		
        optrIdsByBotanistId.put(Botanist.BFRANZONE,  Optr.BFRANZONE);
        optrIdsByBotanistId.put(Botanist.BRACH,      Optr.BRACH);
        optrIdsByBotanistId.put(Botanist.BTAN,       Optr.BTAN);
        optrIdsByBotanistId.put(Botanist.CBEANS,     Optr.CBEANS);
        optrIdsByBotanistId.put(Botanist.DBOUFFORD,  Optr.DBOUFFORD);
        optrIdsByBotanistId.put(Botanist.DPFISTER,   Optr.DPFISTER);
        optrIdsByBotanistId.put(Botanist.EPFISTER,   Optr.EPFISTER);
        optrIdsByBotanistId.put(Botanist.ESHAW1,     Optr.ESHAW);
        optrIdsByBotanistId.put(Botanist.ESHAW2,     Optr.ESHAW);
        optrIdsByBotanistId.put(Botanist.EWOOD,      Optr.EWOOD);
        optrIdsByBotanistId.put(Botanist.EZACHARIAS, Optr.EZACHARIAS);
        optrIdsByBotanistId.put(Botanist.GLEWISG,    Optr.GLEWISG);
        optrIdsByBotanistId.put(Botanist.HALLING,    Optr.HALLING);
        optrIdsByBotanistId.put(Botanist.HKESNER,    Optr.HKESNER);
        optrIdsByBotanistId.put(Botanist.IHAY,       Optr.IHAY);
        optrIdsByBotanistId.put(Botanist.JCACAVIO,   Optr.JCACAVIO);
        optrIdsByBotanistId.put(Botanist.JDOLAN,     Optr.JDOLAN);
        optrIdsByBotanistId.put(Botanist.JMACKLIN,   Optr.JMACKLIN);
        optrIdsByBotanistId.put(Botanist.KGANDHI,    Optr.KGANDHI);
        optrIdsByBotanistId.put(Botanist.KITTREDGE,  Optr.KITTREDGE);
        optrIdsByBotanistId.put(Botanist.LLUKAS,     Optr.LLUKAS);
        optrIdsByBotanistId.put(Botanist.MACKLILN,   Optr.MACKLILN);
        optrIdsByBotanistId.put(Botanist.MPETERS,    Optr.MPETERS);
        optrIdsByBotanistId.put(Botanist.MSCHMULL,   Optr.MSCHMULL);
        optrIdsByBotanistId.put(Botanist.PWHITE,     Optr.PWHITE);
        optrIdsByBotanistId.put(Botanist.ROMERO,     Optr.ROMERO);
        optrIdsByBotanistId.put(Botanist.SDAVIES,    Optr.SDAVIES);
        optrIdsByBotanistId.put(Botanist.SHULTZ1,    Optr.SHULTZ);
        optrIdsByBotanistId.put(Botanist.SHULTZ2,    Optr.SHULTZ);
        optrIdsByBotanistId.put(Botanist.SKELLEY,    Optr.SKELLEY);
        optrIdsByBotanistId.put(Botanist.SLAGRECA,   Optr.SLAGRECA); 
        optrIdsByBotanistId.put(Botanist.SLINDSAY,   Optr.SLINDSAY);
        optrIdsByBotanistId.put(Botanist.SZABEL,     Optr.SZABEL);
        optrIdsByBotanistId.put(Botanist.THIERS,     Optr.THIERS);
        optrIdsByBotanistId.put(Botanist.ZANONI,     Optr.ZANONI);
	}

	public void loadRecord(String[] columns) throws LocalException {

		Botanist botanist = parse(columns);

        // convert botanist into agent ...
        Agent botanistAgent = convert( botanist );

        // find matching record creator
        Integer creatorOptrId = botanist.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        botanistAgent.setCreatedByAgent(createdByAgent);
        
        // convert agent to sql and insert or update (if there was an optr record for this botanist)
        Integer botanistOptrId = optrIdsByBotanistId.get(botanist.getId());
        if (botanistOptrId == null)
        {
            String sql = getInsertSql(botanistAgent);
            insert(sql);
        }
        else
        {
            Optr botanistOptr = new Optr();
            botanistOptr.setId(botanistOptrId);

            String sql = getUpdateSql(botanistAgent, botanistOptr.getGuid());
            update(sql);
        }
	}

    // id, isTeam, isCorporate, name, datesType, startYear, startPrecision, endYear, endPrecision, remarks
    private Botanist parse(String[] columns) throws LocalException
    {
        if (columns.length < 12)
        {
            throw new LocalException("Wrong number of columns");
        }

        // assign values to Botanist object
        Botanist botanist = new Botanist();
        try {
            botanist.setId(Integer.parseInt(StringUtils.trimToNull( columns[0] )));

            String isTeamStr = StringUtils.trimToNull( columns[1] );
            boolean isTeam = isTeamStr != null && isTeamStr.equals( "true" );
            botanist.setTeam( isTeam );

            String isCorporateStr = StringUtils.trimToNull( columns[2] );
            boolean isCorporate = isCorporateStr != null && isCorporateStr.equals( "true" );
            botanist.setCorporate( isCorporate );

            String name = StringUtils.trimToNull( columns[3] );
            botanist.setName( name );

            // no place to put this at the moment: birth/death, flourished, collected, received specimens
            String datesType = StringUtils.trimToNull( columns[4] );
            botanist.setDatesType( datesType );

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
            botanist.setRemarks(SqlUtils.iso8859toUtf8(remarks));
            
            Integer optrId = Integer.parseInt(StringUtils.trimToNull( columns[10] ));
            botanist.setCreatedById(optrId);
            
            String createDateString = StringUtils.trimToNull( columns[11] );
            Date createDate = SqlUtils.parseDate(createDateString);
            botanist.setDateCreated(createDate);
        }
        catch (NumberFormatException e) {
            throw new LocalException("Couldn't parse numeric field", e);
        }

        return botanist;
    }

    private Agent convert( Botanist botanist ) throws LocalException
    {    
		Agent agent = new Agent();

		// NOTE: as of 11:46 am Feb 27 2009, each botanist record that lacks a full name
		// has both an author name and collector name and they are equal; further, the
		// author/collector name contains an ampersand (and a positive team flag)

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
        
        String name = botanist.getName();
        if (name == null)
        {
            throw new LocalException("Name is null");
        }

        // LastName
		String lastName = botanist.getLastName();
		if (lastName == null)
		{
		    throw new LocalException("No last name in botanist record " + botanist.getId());
		}

		if (lastName.length() > 50)
		{
		    warn("Truncating last name", botanist.getId(), lastName);
            lastName = lastName.substring(0, 50);
        }
        agent.setLastName(lastName);
        
        // FirstName
		String firstName = botanist.getFirstName();
		if (firstName != null && firstName.length() > 50)
		{
		    warn("Truncating first name", botanist.getId(), firstName);
            firstName = firstName.substring(0, 50);
        }
        agent.setFirstName(firstName);

        // AgentType
        if (botanist.isOrganization() ) agent.setAgentType( Agent.ORG );
        else if (botanist.isGroup()) agent.setAgentType( Agent.GROUP );
        else agent.setAgentType( Agent.PERSON );
        
		// Remarks
        String remarks = botanist.getRemarks();
        if (remarks != null)
        {
            agent.setRemarks(remarks);
        }

        // TimestampCreated
        Date dateCreated = botanist.getDateCreated();
        agent.setTimestampCreated(DateUtils.toTimestamp(dateCreated));

        return agent;
	}
	
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, DateOfBirth, DateOfDeath, FirstName, LastName, Remarks, CreatedByAgentID, TimestampCreated";

		String[] values = new String[9];

		values[0] = SqlUtils.sqlString( agent.getAgentType());
		values[1] = SqlUtils.sqlString( agent.getGuid());
		values[2] = SqlUtils.sqlString( agent.getDateOfBirth());
		values[3] = SqlUtils.sqlString( agent.getDateOfDeath());
		values[4] = SqlUtils.sqlString( agent.getFirstName());
		values[5] = SqlUtils.sqlString( agent.getLastName());
	    values[6] = SqlUtils.sqlString( agent.getRemarks());
	    values[7] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
		values[8] = SqlUtils.sqlString( agent.getTimestampCreated());


		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}

	private String getUpdateSql(Agent agent, String agentGuid) throws LocalException
	{
	    String[] fieldNames = { "AgentType", "GUID", "DateOfBirth", "DateOfDeath", "FirstName", 
	            "LastName", "Remarks", "CreatedByAgentID", "TimestampCreated" };

	    String[] values = new String[9];

	    values[0] = SqlUtils.sqlString( agent.getAgentType());
	    values[1] = SqlUtils.sqlString( agent.getGuid());
	    values[2] = SqlUtils.sqlString( agent.getDateOfBirth());
	    values[3] = SqlUtils.sqlString( agent.getDateOfDeath());
	    values[4] = SqlUtils.sqlString( agent.getFirstName());
	    values[5] = SqlUtils.sqlString( agent.getLastName());
	    values[6] = SqlUtils.sqlString( agent.getRemarks());
	    values[7] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
	    values[8] = SqlUtils.sqlString( agent.getTimestampCreated());

	    return SqlUtils.getUpdateSql("agent", fieldNames, values, "GUID", SqlUtils.sqlString(agentGuid));
	}
}
