package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Affiliate;
import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Optr;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Division;

public class AffiliateLoader extends CsvToSqlLoader {

	private final Logger log  = Logger.getLogger(AffiliateLoader.class);

	private Division division;
	private BotanistMatcher affiliates;
	
	public AffiliateLoader(File csvFile, Statement specifySqlStatement, File affiliateBotanists) throws LocalException
	{
		super(csvFile, specifySqlStatement);
		
		this.division = getBotanyDivision();
		this.affiliates = new BotanistMatcher(affiliateBotanists);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Affiliate affiliate = parseAffiliateRecord(columns);

		// convert organization into agent ...
		Agent agent = convert(affiliate);

        // find matching record creator
        Integer creatorOptrId = affiliate.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        agent.setCreatedByAgent(createdByAgent);
        
        // find matching botanist
        Integer affiliateAgentId = null;
        Integer botanistId = getBotanistId(affiliate.getId());
        
        if (botanistId != null)
        {
            Botanist botanist = new Botanist();
            botanist.setId(botanistId);
            String guid = botanist.getGuid();

            String sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

            affiliateAgentId = queryForId(sql);
        }
        
        if (affiliateAgentId == null)
        {
            String sql = getInsertSql(agent);
            affiliateAgentId = insert(sql);
        }
        else
        {
            if (agent.getRemarks() != null)
            {
                warn("Ignoring remarks", affiliate.getId(), agent.getRemarks());
            }

            String sql = getUpdateSql(agent, affiliateAgentId);
            update(sql);
        }
		
		if (affiliate.getPhone() != null || affiliate.getAddress() != null)
		{
		    agent.setAgentId(affiliateAgentId);
		    Address address = getAddress(affiliate);
		    address.setAgent(agent);
		    
		    String sql = getInsertSql(address);
		    insert(sql);
		}
	}

	private Integer getBotanistId(Integer affiliateId)
	{
	    return affiliates.getBotanistId(affiliateId);
	}

	private Affiliate parseAffiliateRecord(String[] columns) throws LocalException
	{
		if (columns.length < 10)
		{
			throw new LocalException("Wrong number of columns");
		}

		// assign values to Optr object
		Affiliate affiliate = new Affiliate();

		try {
		    affiliate.setId(           Integer.parseInt(StringUtils.trimToNull( columns[0] )));
		    affiliate.setSurname(                       StringUtils.trimToNull( columns[1] ));
		    affiliate.setGivenName(                     StringUtils.trimToNull( columns[2] ));
		    affiliate.setPosition(                      StringUtils.trimToNull( columns[3] ));
		    affiliate.setPhone(                         StringUtils.trimToNull( columns[4] ));
		    affiliate.setEmail(                         StringUtils.trimToNull( columns[5] ));
		    affiliate.setAddress(                       StringUtils.trimToNull( columns[6] ));
	          
            Integer optrId =           Integer.parseInt(StringUtils.trimToNull( columns[7] ));
            affiliate.setCreatedById(optrId);
            
            String createDateString =                   StringUtils.trimToNull( columns[8] );
            Date createDate = SqlUtils.parseDate(createDateString);
            affiliate.setDateCreated(createDate);
            
		    affiliate.setRemarks(SqlUtils.iso8859toUtf8(StringUtils.trimToNull( columns[9] )));
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
			warn("Truncating last name ", affiliate.getId(), lastName);
			lastName = lastName.substring(0, 50);
		}
		agent.setLastName(lastName);

		// FirstName
		String firstName = affiliate.getGivenName();
		if (firstName != null && firstName.length() > 50) {
		    warn("Truncating first name", affiliate.getId(), firstName);
		    firstName = firstName.substring(0, 50);
		}
		agent.setFirstName(firstName);
	        
		// JobTitle
		String position = affiliate.getPosition();
		if (position != null && position.length() > 50)
		{
		    warn("Truncating position", affiliate.getId(), position);
		    firstName = position.substring(0, 50);
		}
		agent.setJobTitle(position);
		  
        // Email TODO
		String email = affiliate.getEmail();
		if (email != null && email.length() > 50)
		{
		    warn("Truncating last email", affiliate.getId(), position);
		    email = email.substring(0, 50);
		}
		agent.setEmail(email);

        
        // Remarks
        String remarks = affiliate.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks(remarks);
        }
        
        // TimestampCreated
        Date dateCreated = affiliate.getDateCreated();
        agent.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		return agent;
	}

	private Address getAddress(Affiliate affiliate)
	{
	    Address address = new Address();
        
        // Phone
        String phone = affiliate.getPhone();
        if (phone != null && phone.length() > 50)
        {
            warn("Truncating phone number", affiliate.getId(), phone);
            phone = phone.substring(0, 50);
        }
        address.setPhone1(phone);
        
        // Address TODO: only 17 records have data, and it all needs post-import cleaning
        String addressString = affiliate.getAddress();
        if (addressString != null && addressString.length() > 255)
        {
            warn("Truncating address", affiliate.getId(), addressString);
            addressString = addressString.substring(0, 255);
        }
        address.setAddress(addressString);

        return address;
	}

	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = 
			"AgentType, GUID, FirstName, LastName, DivisionID, CreatedByAgentID, TimestampCreated, Remarks";

		String[] values = new String[8];

		values[0] =     String.valueOf( agent.getAgentType());
		values[1] = SqlUtils.sqlString( agent.getGuid());
		values[2] = SqlUtils.sqlString( agent.getFirstName());
		values[3] = SqlUtils.sqlString( agent.getLastName());
		values[4] =     String.valueOf( agent.getDivision().getDivisionId());
        values[5] = SqlUtils.sqlString( agent.getCreatedByAgent().getId());
        values[6] = SqlUtils.sqlString( agent.getTimestampCreated());
		values[7] = SqlUtils.sqlString( agent.getRemarks());

		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "Address, Phone1, AgentID, TimestampCreated";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString(address.getAddress());
        values[1] = SqlUtils.sqlString(address.getPhone1());
        values[2] =     String.valueOf(address.getAgent().getAgentId());
        values[3] = "now()";
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
    
    private String getUpdateSql(Agent agent, Integer agentId) throws LocalException
    {
        String[] fieldNames = { "JobTitle", "Email" };

        String[] values = new String[2];

        values[0] = SqlUtils.sqlString( agent.getJobTitle());
        values[1] = SqlUtils.sqlString( agent.getEmail());

        return SqlUtils.getUpdateSql("agent", fieldNames, values, "AgentID", String.valueOf(agentId));
    }
}
