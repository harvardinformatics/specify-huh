package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Optr;
import edu.ku.brc.specify.datamodel.Agent;

public class OptrLoader extends CsvToSqlLoader {

	private final Logger log  = Logger.getLogger(OptrLoader.class);

	public OptrLoader(File csvFile, Statement specifySqlStatement)
	{
		super(csvFile, specifySqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Optr optr = parseOptrRecord(columns);

		// convert optr into agent ...
		Agent agent = convert(optr);

		// convert organization to sql and insert
		String sql = getInsertSql(agent);
		insert(sql);
	}

	private Optr parseOptrRecord(String[] columns) throws LocalException
	{
		if (columns.length < 3)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Optr object
		Optr optr = new Optr();

		try {
		    optr.setId(            Integer.parseInt(StringUtils.trimToNull(columns[0])));
		    optr.setFullName(                       StringUtils.trimToNull(columns[1]));
		    optr.setRemarks( SqlUtils.iso8859toUtf8(StringUtils.trimToNull(columns[2])));
		}
		catch (NumberFormatException e) {
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return optr;
	}

	public Agent convert(Optr optr) throws LocalException
	{

		Agent agent = new Agent();

		// AgentType
		agent.setAgentType(Agent.PERSON);

		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		agent.setGuid(optr.getGuid());

		// LastName
		String lastName = optr.getLastName();
		if ( lastName.length() > 50 ) {
			log.warn( "truncating last name" );
			lastName = lastName.substring( 0, 50);
		}
		agent.setLastName(lastName);

		// FirstName
		String firstName = optr.getFirstName();
		if ( firstName.length() > 50 ) {
		    log.warn( "truncating first name" );
		    firstName = firstName.substring( 0, 50);
		}
		agent.setFirstName(firstName);
	        
		// Remarks
		String remarks = optr.getRemarks();
		if ( remarks != null ) {
			agent.setRemarks(remarks);
		}

		return agent;
	}

	public String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, FirstName, LastName, TimestampCreated, Remarks";

		List<String> values = new ArrayList<String>(6);

		values.add(    String.valueOf(agent.getAgentType()   ));
		values.add(SqlUtils.sqlString(agent.getGuid()        ));
		values.add(SqlUtils.sqlString(agent.getFirstName()));
		values.add(SqlUtils.sqlString(agent.getLastName()    ));
		values.add("now()" );
		values.add(SqlUtils.sqlString(agent.getRemarks()     ));

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
}
