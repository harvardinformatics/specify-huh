package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Affiliate;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Division;

public class AffiliateLoader extends CsvToSqlLoader {

	private final Logger log  = Logger.getLogger(AffiliateLoader.class);

	private Division division;
	
	public AffiliateLoader(File csvFile, Statement specifySqlStatement, Division division)
	{
		super(csvFile, specifySqlStatement);
		
		this.division = division;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Affiliate affiliate = parseAffiliateRecord(columns);

		// convert organization into agent ...
		Agent agent = convert(affiliate);

		// convert organization to sql and insert
		String sql = getInsertSql(agent);
		Integer agentId = insert(sql);
		
		if (affiliate.getPhone() != null || affiliate.getAddress() != null)
		{
		    agent.setAgentId(agentId);
		    Address address = getAddress(affiliate);
		    address.setAgent(agent);
		    
		    sql = getInsertSql(address);
		    insert(sql);
		}
	}

	private Affiliate parseAffiliateRecord(String[] columns) throws LocalException
	{
		if (columns.length < 8)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Optr object
		Affiliate affiliate = new Affiliate();

		try {
		    affiliate.setId(           Integer.parseInt(StringUtils.trimToNull(columns[0])));
		    affiliate.setSurname(                       StringUtils.trimToNull(columns[1]));
		    affiliate.setGivenName(                     StringUtils.trimToNull(columns[2]));
		    affiliate.setPosition(                      StringUtils.trimToNull(columns[3]));
		    affiliate.setPhone(                         StringUtils.trimToNull(columns[4]));
		    affiliate.setEmail(                         StringUtils.trimToNull(columns[5]));
		    affiliate.setAddress(                       StringUtils.trimToNull(columns[6]));
		    affiliate.setRemarks(SqlUtils.iso8859toUtf8(StringUtils.trimToNull(columns[7])));
		}
		catch (NumberFormatException e) {
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return affiliate;
	}

	public Agent convert(Affiliate affiliate) throws LocalException
	{

		Agent agent = new Agent();

		// Division
		agent.setDivision(division);
		
		// AgentType
		agent.setAgentType(Agent.PERSON);

		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		agent.setGuid(affiliate.getGuid());

		// LastName
		String lastName = affiliate.getSurname();
		if (lastName.length() > 50) {
			log.warn( "truncating last name" );
			lastName = lastName.substring( 0, 50);
		}
		agent.setLastName(lastName);

		// FirstName
		String firstName = affiliate.getGivenName();
		if (firstName != null && firstName.length() > 50 ) {
		    log.warn( "truncating first name" );
		    firstName = firstName.substring( 0, 50);
		}
		agent.setFirstName(firstName);
	        
		// JobTitle
		String position = affiliate.getPosition();
		if (position != null && position.length() > 50)
		{
		    log.warn( "truncating job title" );
		    firstName = position.substring( 0, 50);
		}
		agent.setJobTitle(position);
		  
        // Email TODO
		String email = affiliate.getEmail();
		if (email != null && email.length() > 50)
		{
		    log.warn("Truncating email");
		    email = email.substring(0, 50);
		}
		agent.setEmail(email);

        
        // Remarks
        String remarks = affiliate.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks(remarks);
        }
        
		return agent;
	}

	private Address getAddress(Affiliate affiliate)
	{
	    Address address = new Address();
        
        // Phone
        String phone = affiliate.getPhone();
        if (phone != null && phone.length() > 50)
        {
            log.warn("Truncating phone 1");
            phone = phone.substring(0, 50);
        }
        address.setPhone1(phone);
        
        // Address TODO: only 17 records have data, and it all needs post-import cleaning
        String addressString = affiliate.getAddress();
        if (addressString != null && addressString.length() > 255)
        {
            log.warn("Truncating address");
            addressString = addressString.substring(0, 255);
        }
        address.setAddress(addressString);

        return address;
	}

	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, FirstName, LastName, DivisionID, TimestampCreated, Remarks";

		List<String> values = new ArrayList<String>(6);

		values.add(    String.valueOf(agent.getAgentType()               ));
		values.add(SqlUtils.sqlString(agent.getGuid()                    ));
		values.add(SqlUtils.sqlString(agent.getFirstName()               ));
		values.add(SqlUtils.sqlString(agent.getLastName()                ));
		values.add(    String.valueOf(agent.getDivision().getDivisionId()));
		values.add("now()" );
		values.add(SqlUtils.sqlString(agent.getRemarks()                 ));

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "Address, Phone1, AgentID, TimestampCreated";
        
        List<String> values = new ArrayList<String>(4);
        
        values.add(SqlUtils.sqlString(address.getAddress()));
        values.add(SqlUtils.sqlString(address.getPhone1()));
        values.add(    String.valueOf(address.getAgent().getAgentId()));
        values.add("now");
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
