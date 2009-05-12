package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Affiliate;
import edu.harvard.huh.asa.AsaAgent;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Division;

public class AgentLoader extends CsvToSqlLoader {

	private final Logger log  = Logger.getLogger(AgentLoader.class);

	private Division division;
	
	public AgentLoader(File csvFile, Statement specifySqlStatement) throws LocalException
	{
		super(csvFile, specifySqlStatement);
		
		this.division = getBotanyDivision();
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		AsaAgent asaAgent = parseAgentRecord(columns);

		// convert organization into agent ...
		Agent agent = convert(asaAgent);

		// convert organization to sql and insert
		String sql = getInsertSql(agent);
		Integer agentId = insert(sql);
        agent.setAgentId(agentId);

        if (asaAgent.getPhone() != null || 
		    asaAgent.getCorrespAddress() != null || 
		    asaAgent.getFax() != null
		        )
		{
		    Address address = getCorrespAddress(asaAgent);
		    address.setAgent(agent);
		    
		    sql = getInsertSql(address);
		    insert(sql);
		}
		
		if (asaAgent.getShippingAddress() != null)
		{
	          Address address = getShippingAddress(asaAgent);
	            address.setAgent(agent);
	            
	            sql = getInsertSql(address);
	            insert(sql);
		}
	}

	private AsaAgent parseAgentRecord(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Optr object
		AsaAgent agent = new AsaAgent();

		try {
		    agent.setId(             Integer.parseInt( StringUtils.trimToNull( columns[0]  )));
		    agent.setOrganizationId( Integer.parseInt( StringUtils.trimToNull( columns[1]  )));
		    agent.setActive(     Boolean.parseBoolean( StringUtils.trimToNull( columns[2]  )));
		    agent.setPrefix(                           StringUtils.trimToNull( columns[3]  ));
		    agent.setName(                             StringUtils.trimToNull( columns[4]  ));
		    agent.setTitle(                            StringUtils.trimToNull( columns[5]  ));
		    agent.setSpecialty(                        StringUtils.trimToNull( columns[6]  ));
		    agent.setCorrespAddress(                   StringUtils.trimToNull( columns[7]  ));
		    agent.setShippingAddress(                  StringUtils.trimToNull( columns[8]  ));
            agent.setEmail(                            StringUtils.trimToNull( columns[9]  ));
		    agent.setPhone(                            StringUtils.trimToNull( columns[10] ));
            agent.setFax(                              StringUtils.trimToNull( columns[11] ));
		    agent.setUri(                              StringUtils.trimToNull( columns[12] ));
		    agent.setRemarks(  SqlUtils.iso8859toUtf8( StringUtils.trimToNull( columns[13] )));
		}
		catch (NumberFormatException e) {
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return agent;
	}

	public Agent convert(AsaAgent asaAgent) throws LocalException
	{

		Agent agent = new Agent();

		// Division
		agent.setDivision(division);
		
		// AgentType
		agent.setAgentType(Agent.PERSON);

		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration
		agent.setGuid(asaAgent.getGuid());

		// LastName
		String lastName = asaAgent.getName();
		if (lastName.length() > 50) {
			log.warn( "truncating last name" );
			lastName = lastName.substring( 0, 50);
		}
		agent.setLastName(lastName);

		// FirstName TODO: parse name?
		String firstName = asaAgent.getName();
		if (firstName != null && firstName.length() > 50 ) {
		    log.warn( "truncating first name" );
		    firstName = firstName.substring( 0, 50);
		}
		agent.setFirstName(firstName);
	        
		// Position
		String position = asaAgent.getTitle();
		if (position != null && position.length() > 50)
		{
		    log.warn( "truncating job title" );
		    firstName = position.substring( 0, 50);
		}
		agent.setJobTitle(position);
		  
        // Email
		String email = asaAgent.getEmail();
		if (email != null && email.length() > 50)
		{
		    log.warn("Truncating email");
		    email = email.substring(0, 50);
		}
		agent.setEmail(email);

        // URL
		String uri = asaAgent.getUri();
		agent.setUrl(uri);
		
		// Interests
		String specialty = asaAgent.getSpecialty();
		if (specialty != null && specialty.length() > 255)
		{
		    log.warn("Truncating specialty");
		    specialty = specialty.substring(0, 255);
		}
		agent.setInterests(specialty);
		
		// Title
		String prefix = asaAgent.getPrefix();
		if (prefix != null && prefix.length() > 50)
		{
		    log.warn("Truncating prefix");
		    prefix = prefix.substring(0, 50);
		}
		agent.setTitle(prefix);
		
        // Remarks
        String remarks = asaAgent.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks(remarks);
        }
        
		return agent;
	}

	private Address getCorrespAddress(AsaAgent asaAgent)
	{
	    Address address = new Address();
        
        // Phone
        String phone = asaAgent.getPhone();
        if (phone != null && phone.length() > 50)
        {
            log.warn("Truncating phone 1");
            phone = phone.substring(0, 50);
        }
        address.setPhone1(phone);
        
        // Address TODO
        String addressString = asaAgent.getCorrespAddress();
        if (addressString != null && addressString.length() > 255)
        {
            log.warn("Truncating address");
            addressString = addressString.substring(0, 255);
        }
        address.setAddress(addressString);

        return address;
	}

	private Address getShippingAddress(AsaAgent asaAgent)
	{
	    Address address = new Address();
	    address.setIsShipping(true);

	    String addressString = asaAgent.getShippingAddress();
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
        String fieldNames = "Address, IsShipping, Phone1, Fax, AgentID, TimestampCreated";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( address.getAddress());
        values[1] =     String.valueOf( address.getIsShipping());
        values[2] = SqlUtils.sqlString( address.getPhone1());
        values[3] = SqlUtils.sqlString( address.getFax());
        values[4] =     String.valueOf( address.getAgent().getAgentId());
        values[5] = "now";
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
}
