package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaAgent;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.AgentLookup;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.OrganizationLookup;
import edu.ku.brc.specify.datamodel.Address;
import edu.ku.brc.specify.datamodel.Agent;

//Run this class after AffiliateLoader.

public class AgentLoader extends CsvToSqlLoader
{
    private static final Logger log  = Logger.getLogger(AgentLoader.class);
    
	private AgentLookup        agentLookup;
	
	private OrganizationLookup organizationLookup;
	private AsaIdMapper        agents;
	private BotanistLookup     botanistLookup;
	
    private final static Pattern usPattern  = Pattern.compile("^(u\\.\\s*s\\.\\s*a\\.|usa|united\\s+states|puerto\\s+rico)$", Pattern.CASE_INSENSITIVE);
    private final static Pattern auPattern  = Pattern.compile("^(australia)$", Pattern.CASE_INSENSITIVE);
    private final static Pattern caPattern  = Pattern.compile("^(canada)$", Pattern.CASE_INSENSITIVE);

    private final static Pattern countryPattern = Pattern.compile("^\\s*(argentina|armenia|australia|austria|barbados|bolivia|belgium|(bra[sz]il(ia)?)|brunei|bulgaria|cambodia|canada|chile|colombia|costa\\s+rica|cuba|(czech(oslovakia|\\s+republic)|denmark|dominican\\s+republic|ecuador|egypt|((england|scotland)(,? u\\.?k\\.?)?)|estonia|finland|fiji|france|german\\s+democratic\\s+republic|((federal\\s+republic\\s+of\\s+)?germany)|great\\s+britain|french\\s+guiana|guam|guyana|honduras|hong\\s+kong|hungary|iceland|india|indonesia|ireland|israel|italy|jamaica|japan|((s(\\.|outh)\\s+)?korea(,\\s+south)?)|luxembourg|malaysia|mexico|((the\\s+)?netherlands)|(new\\s+(caledonia|zealand))|norway|pakistan|panama|papua\\s+new\\s+guinea|((people'?s\\s+)?republic\\s+of\\s+)?china)|p\\.\\s+r\\.\\s+china|peru|philippines|poland|portugal|puerto\\s+rico|r\\.o\\.c\\.|romania|russia|singapore|slovakia|solomon\\s+islands|south\\s+africa|spain|sri\\s+lanka|sweden|switzerland|(taiwan(\\s+r\\.o\\.c\\.)?)|thailand|(u\\.?k\\.?)|united\\s+kingdom|(u\\.\\s*s\\.\\s*a\\.|usa|united\\s+states)|uraguay|venezuela|vietnam|zimbabwe)\\s*$", Pattern.CASE_INSENSITIVE);
    private final static Pattern zipPattern     = Pattern.compile("^\\s*(\\d{5}(-?\\d{4})?)\\s*$");
    private final static Pattern cszPattern     = Pattern.compile("^\\s*([a-z ]+),\\s+([a-z\\s]+)\\s+(\\d{5}(-?\\d{4})?)\\s*$", Pattern.CASE_INSENSITIVE);
    private final static Pattern csPattern      = Pattern.compile("^\\s*([a-z ]+),\\s+([a-z\\s]+)\\s*$", Pattern.CASE_INSENSITIVE);
    private final static Pattern statePattern   = Pattern.compile("^(alabama|alaska|arkansas|arizona|california|colorado|connecticut|delaware|florida|georgia|guam|hawaii|iowa|idaho|illinois|indiana|kansas|kentucky|louisiana|massachusetts|maryland|maine|michigan|minnesota|mississippi|missouri|montana|nebraska|nevada|(new\\s+(hampshire|jersey|mexico|york))|(north\\s+(carolina|dakota))|ohio|oklahoma|oregon|pennsylvania|puerto\\s+rico|rhode\\s+island|(south\\s+(carolina|dakota))|tennessee|texas|utah|virginia|vermont|washington|wisconsin|west\\s+virginia|wyoming)\\s*$", Pattern.CASE_INSENSITIVE);
    private final static Pattern stPattern      = Pattern.compile("^((a[lkrz])|(c[aot])|(d[ce])|d\\.c\\.|fl|ga|hi|(i[adln])|(k[sy])|la|(m[adeinost])|(n[cdehjmvy])|(o[hkr])|(p[ar])|ri|(s[cd])|(t[nx])|ut|(v[at])|(w[aivy]))\\s*$", Pattern.CASE_INSENSITIVE);
    
    private final static Pattern caPostalCodePattern   = Pattern.compile("^\\s*([A-Z][O\\d][A-Z]\\s*[O\\d][A-Z][O\\d])\\s*$");
    private final static Pattern caCodeProvincePattern = Pattern.compile("^\\s*([A-Z][O\\d][A-Z]\\s*[O\\d][A-Z][O\\d]),\\s*(Alberta|BC|British\\s+Columbia|New\\s+Brunswick|NS|Nova\\s+Scotia|Ontario|Qu[eé]bec|SK|Saskatchewan)\\s*$");
    private final static Pattern caCityProvCodePattern = Pattern.compile("^\\s*([a-zA-Z ]+),\\s+(Alberta|BC|British\\s+Columbia|New\\s+Brunswick|NS|Nova\\s+Scotia|Ontario|Qu[eé]bec|SK|Saskatchewan)\\s+([A-Z][O\\d][A-Z]\\s*[O\\d][A-Z][O\\d])\\s*$");
    private final static Pattern caProvincePattern     = Pattern.compile("Alberta|BC|British\\s+Columbia|New\\s+Brunswick|NS|Nova\\s+Scotia|Ontario|Qu[eé]bec|SK|Saskatchewan");
    
    private final static Pattern auPostalCodePattern   = Pattern.compile("^\\s*(\\d{4})\\s*$");
    private final static Pattern auTerritoryPattern    = Pattern.compile("(A\\.C\\.T\\.|(N\\.?S\\.?W\\.?)|New\\s+South\\s+Wales\\s+|Northern\\s+Territory|Tasmania|QLD|Queensland|Victoria|WA|Western\\s+Australia)\\s*$");
    private final static Pattern auCityTerrPattern     = Pattern.compile("^\\s*([a-zA-Z ]+),?\\s+(A\\.C\\.T\\.|(N\\.?S\\.?W\\.?)|New\\s+South\\s+Wales\\s+|Northern\\s+Territory|Tasmania|QLD|Queensland|Victoria|WA|Western\\s+Australia)\\s*$");
    private final static Pattern auCityTerrCodePattern = Pattern.compile("^\\s*([a-zA-Z ]+),?\\s+(A\\.C\\.T\\.|(N\\.?S\\.?W\\.?)|New\\s+South\\s+Wales\\s+|Northern\\s+Territory|Tasmania|QLD|Queensland|Victoria|WA|Western\\s+Australia)\\s+(\\d{4})\\s*$");

	public AgentLoader(File csvFile,
	                   Statement specifySqlStatement,
	                   File agentBotanists,
	                   BotanistLookup botanistLookup,
	                   OrganizationLookup organizationLookup) throws LocalException
	{
		super(csvFile, specifySqlStatement);
		
		this.agents = new AsaIdMapper(agentBotanists);

		this.botanistLookup = botanistLookup;
		this.organizationLookup = organizationLookup;
	}

	public AgentLookup getAgentLookup()
	{
		if (agentLookup == null)
		{
			agentLookup = new AgentLookup() {
				public Agent getById(Integer asaAgentId) throws LocalException
				{
					Agent agent = new Agent();
					
					Integer botanistId = getBotanistId(asaAgentId);
					
                    String guid = botanistId != null ? BotanistLoader.getGuid(botanistId) : getGuid(asaAgentId);

					Integer agentId = getInt("agent", "AgentID", "GUID", guid);

					agent.setAgentId(agentId);

					return agent;
				}
			};
		}
		return agentLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		AsaAgent asaAgent = parse(columns);

		Integer asaAgentId = asaAgent.getId();
		setCurrentRecordId(asaAgentId);
		
		Agent agent = getAgent(asaAgent);

		Integer botanistId = getBotanistId(asaAgentId);
		
        if (botanistId != null) // retain botanist id in guid
        {
            Agent botanistAgent = lookup(botanistId);
            Integer agentId = botanistAgent.getId();
            agent.setAgentId(agentId);
            
        	if (agent.getRemarks() != null)
            {
                getLogger().warn(rec() + "Ignoring remarks: " + asaAgent.getRemarks());
            }
        	
            String sql = getUpdateSql(agent, agentId);
            update(sql);
        }
        else
        {
            String sql = getInsertSql(agent);
            Integer agentId = insert(sql);
            agent.setAgentId(agentId);
        }

        // Correspondence address
        Address correspAddress = getCorrespAddress(asaAgent, agent);
        if (correspAddress != null)
        {
		    String sql = getInsertSql(correspAddress);
		    insert(sql);
        }
		
        // Shipping address
        Address shippingAddress = getShippingAddress(asaAgent, agent);
        if (shippingAddress != null)
        {
        	String sql = getInsertSql(shippingAddress);
        	insert(sql);
        }
	}
	
	public Logger getLogger()
	{
	    return log;
	}

	private String getGuid(Integer agentId)
	{
		return agentId + " agent";
	}

	private Agent lookup(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}

	private Integer getBotanistId(Integer agentId)
	{
	    return agents.map(agentId);
	}
	
	private AsaAgent parse(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Not enough columns");
		}
		
		AsaAgent agent = new AsaAgent();
		try
		{
		    agent.setId(             SqlUtils.parseInt( columns[0]  ));
		    agent.setOrganizationId( SqlUtils.parseInt( columns[1]  ));
		    agent.setActive(      Boolean.parseBoolean( columns[2]  ));
		    agent.setPrefix(                            columns[3]  );
		    agent.setName(                              columns[4]  );
		    agent.setTitle(                             columns[5]  );
		    agent.setSpecialty(                         columns[6]  );
		    agent.setCorrespAddress(                    columns[7]  );
		    agent.setShippingAddress(                   columns[8]  );
            agent.setEmail(                             columns[9]  );
		    agent.setPhone(                             columns[10] );
            agent.setFax(                               columns[11] );
		    agent.setUri(                               columns[12] );
		    agent.setRemarks(   SqlUtils.iso8859toUtf8( columns[13] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}

		return agent;
	}

	private Agent getAgent(AsaAgent asaAgent) throws LocalException
	{		
		Agent agent = new Agent();
		
		// AgentType
		agent.setAgentType(Agent.PERSON);

        // Email
		String email = asaAgent.getEmail();
		if (email != null )
		{
		    email = truncate(email, 50, "email");
			agent.setEmail(email);
		}

		// FirstName TODO: go back over agent names, this mostly works but not always.
		String firstName = asaAgent.getFirstName();
		if (firstName != null)
		{
		    firstName = truncate(firstName, 50, "first name");
			agent.setFirstName(firstName);
		}
	    
		// GUID: temporarily hold asa organization.id TODO: don't forget to unset this after migration		Integer asaAgentId = asaAgent.getId();
		Integer asaAgentId = asaAgent.getId();
		checkNull(asaAgentId, "id");

		String guid = getGuid(asaAgentId);
		agent.setGuid(guid);

		// Interests
		String specialty = asaAgent.getSpecialty();
		if (specialty != null)
		{
			specialty = truncate(specialty, 255, "interests");
			agent.setInterests(specialty);
		}
		
		// JobTitle
		String position = asaAgent.getTitle();
		if (position != null)
		{
		    position = truncate(position, 50, "position");
			agent.setJobTitle(position);
		}
		
		// LastName
		String lastName = asaAgent.getLastName();
		checkNull(lastName, "last name");
		lastName = truncate(lastName, 50, "last name");
		agent.setLastName(lastName);
  
		// ParentOrganziation
		Integer organizationId = asaAgent.getOrganizationId();
		checkNull(organizationId, "organization id");
		
		Agent organization = lookupOrganization(organizationId);
		agent.setOrganization(organization);
		
        // Remarks
        String remarks = asaAgent.getRemarks();
        if ( remarks != null ) {
            agent.setRemarks(remarks);
        }
		
		// Title
		String prefix = asaAgent.getPrefix();
		if (prefix != null)
		{
		    prefix = truncate(prefix, 50, "prefix");
		}
		agent.setTitle(prefix);
		
        // URL
		String uri = asaAgent.getUri();
		agent.setUrl(uri);
		
		return agent;
	}

	private Agent lookupOrganization(Integer organizationId) throws LocalException
	{
		return organizationLookup.getById(organizationId);
	}
	
	private Address getCorrespAddress(AsaAgent asaAgent, Agent agent)
	{
		String addressString = asaAgent.getCorrespAddress();
		String fax = asaAgent.getFax();
		String phone = asaAgent.getPhone();
		
		if (addressString == null && fax == null && phone == null) return null;
		
	    Address address = parseAddress(addressString);
	    
	    if (address == null) address = new Address();
        
        // Agent
        address.setAgent(agent);
        
	    // Fax
        if (fax != null)
        {
        	fax = truncate(fax, 50, "fax");
        	address.setFax(fax);
        }
        
        // IsShipping
        address.setIsShipping(false);
        
        // Phone
        if (phone != null)
        {
            phone = truncate(phone, 50, "phone");
            address.setPhone1(phone);
        }
        
        return address;
	}

	private Address getShippingAddress(AsaAgent asaAgent, Agent agent)
	{
		String addressString = asaAgent.getShippingAddress();
		
		if (addressString == null) return null;
		
	    Address address = parseAddress(addressString);
	    
	    if (address == null) return null;

	    // Agent
	    address.setAgent(agent);
	    
	    // Fax
	    
	    // IsShipping
	    address.setIsShipping(true);

	    // Phone
	    
	    return address;
	}
       
	private String getInsertSql(Agent agent) throws LocalException
	{
		String fieldNames = "AgentType, Email, FirstName, GUID, Interests, JobTitle, " +
				            "LastName, ParentOrganizationID, Remarks, TimestampCreated, " +
				            "Title, URL, Version";

		String[] values = new String[13];

		values[0]  = SqlUtils.sqlString( agent.getAgentType());
		values[1]  = SqlUtils.sqlString( agent.getEmail());
		values[2]  = SqlUtils.sqlString( agent.getFirstName());
		values[3]  = SqlUtils.sqlString( agent.getGuid());
		values[4]  = SqlUtils.sqlString( agent.getInterests());
		values[5]  = SqlUtils.sqlString( agent.getJobTitle());
		values[6]  = SqlUtils.sqlString( agent.getLastName());
		values[7]  = SqlUtils.sqlString( agent.getOrganization().getId());
		values[8]  = SqlUtils.sqlString( agent.getRemarks());
		values[9]  = SqlUtils.now();
		values[10] = SqlUtils.sqlString( agent.getTitle());
		values[11] = SqlUtils.sqlString( agent.getUrl());
		values[12] = SqlUtils.one();
		
		return SqlUtils.getInsertSql("agent", fieldNames, values);
	}
	
	private String getUpdateSql(Agent agent, Integer agentId)
	{
		String[] fields = { "Email", "Interests", "JobTitle", "ParentOrganizationID",
				            "Title", "URL" };
		
		String[] values = new String[6];
		
		values[0] = SqlUtils.sqlString( agent.getEmail());
		values[1] = SqlUtils.sqlString( agent.getInterests());
		values[2] = SqlUtils.sqlString( agent.getJobTitle());
		values[3] = SqlUtils.sqlString( agent.getOrganization().getId());
		values[4] = SqlUtils.sqlString( agent.getTitle());
		values[5] = SqlUtils.sqlString( agent.getUrl());
		
		return SqlUtils.getUpdateSql("agent", fields, values, "AgentID", agentId);
	}
	
    private String getInsertSql(Address address) throws LocalException
    {
        String fieldNames = "Address, Address2, AgentID, City, Country, Fax, IsShipping, Phone1, PostalCode, Remarks, TimestampCreated, Version";
        
        String[] values = new String[12];
        
        values[0]  = SqlUtils.sqlString( address.getAddress());
        values[1]  = SqlUtils.sqlString( address.getAddress2());
        values[2]  = SqlUtils.sqlString( address.getAgent().getAgentId());
        values[3]  = SqlUtils.sqlString( address.getCity());
        values[4]  = SqlUtils.sqlString( address.getCountry());
        values[5]  = SqlUtils.sqlString( address.getFax());
        values[6]  = SqlUtils.sqlString( address.getIsShipping());
        values[7]  = SqlUtils.sqlString( address.getPhone1());
        values[8]  = SqlUtils.sqlString( address.getPostalCode());
        values[9]  = SqlUtils.sqlString( address.getRemarks());
        values[10] = SqlUtils.now();
        values[11] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("address", fieldNames, values);
    }
    
    private Address parseAddress(String addressString)
    {
        if (addressString == null) return null;
        
        Address address = new Address();

        String country  = null;
        String state    = null;
        String city     = null;
        String zip      = null;

        String[] lines = addressString.split("\\^");
        
        if (lines.length == 0)
        {
            // warn about this
            getLogger().warn("No address lines: " + addressString);
            return null;
        }

        String lastLine = lines[lines.length - 1];

        if (lines.length == 1)
        {
            // there is only one line, warn about this
            getLogger().warn("One line address: " + addressString);
            fillAddressLines(lines, 1, address);
        }
        else
        {
            // there are at least two lines
            String nextToLastLine = lines[lines.length - 2];
            
            // try to match country or a zip code on the last line
            Matcher lastLineCountryMatcher      = countryPattern.matcher(lastLine);
            Matcher lastLineZipMatcher          = zipPattern.matcher(lastLine);
            Matcher lastLineCityStateZipMatcher = cszPattern.matcher(lastLine);
            
            if (lastLineCountryMatcher.matches())
            {
                // matched country
                country = lastLineCountryMatcher.group(1);
                
                // try to match country to USA, Canada, or Australia
                Matcher usMatcher = usPattern.matcher(country);
                Matcher caMatcher = caPattern.matcher(country);
                Matcher auMatcher = auPattern.matcher(country);

                if (usMatcher.matches())
                {
                    Matcher zipMatcher = zipPattern.matcher(nextToLastLine);
                    Matcher cityStateZipMatcher = cszPattern.matcher(nextToLastLine);
                    
                    // try to match a zip code on the next-to-last line
                    if (zipMatcher.matches())
                    {
                        // second to last line is zip
                        zip = zipMatcher.group(1);

                        // make sure there is a line to match for city, state
                        if (lines.length < 3)
                        {
                            getLogger().warn("Only zip and country present: " + addressString);
                        }
                        else
                        {
                            String thirdToLastLine = lines[lines.length - 3];

                            Matcher cityStateMatcher = csPattern.matcher(thirdToLastLine);

                            // try to match city, state on the third-to-last-line
                            if (cityStateMatcher.matches())
                            {
                                city = cityStateMatcher.group(1);
                                state = cityStateMatcher.group(2);

                                Matcher stateMatcher = statePattern.matcher(state);
                                Matcher stMatcher = stPattern.matcher(state);

                                if (stateMatcher.matches())
                                {
                                    state = stateMatcher.group(1);
                                }
                                else if (stMatcher.matches())
                                {
                                    state = stMatcher.group(1);
                                }
                                else
                                {
                                    getLogger().warn("Couldn't match city and state: " + addressString);
                                    city = null;
                                    state = null;
                                }

                                if (city != null)
                                {
                                    fillAddressLines(lines, lines.length - 3, address);
                                }
                                else
                                {
                                    fillAddressLines(lines, lines.length - 2, address);
                                }
                            }
                            // did not match city, state on the third-to-last line
                            else
                            {
                                fillAddressLines(lines, lines.length - 2, address);
                            }
                        }
                    }
                    else if (cityStateZipMatcher.matches())
                    {
                        city  = cityStateZipMatcher.group(1);
                        state = cityStateZipMatcher.group(2);
                        zip   = cityStateZipMatcher.group(3);

                        Matcher stateMatcher = statePattern.matcher(state);
                        Matcher stMatcher = stPattern.matcher(state);

                        if (stateMatcher.matches())
                        {
                            state = stateMatcher.group(1);
                        }
                        else if (stMatcher.matches())
                        {
                            state = stMatcher.group(1);
                        }
                        else
                        {
                            getLogger().warn("Couldn't match city and state: " + addressString);
                            city = null;
                            state = null;
                        }
                        
                        // any previous lines are address, address2
                        if (city != null)
                        {
                            fillAddressLines(lines, lines.length - 2, address);
                        }
                        else
                        {
                            fillAddressLines(lines, lines.length - 1, address);
                        }
                    }
                    // did not match city, state, zip on next-to-last line
                    else
                    {
                        fillAddressLines(lines, lines.length - 1, address);
                    }
                }
                else if (caMatcher.matches())
                {
                    // matched Canada
                    Matcher postalCodeMatcher   = caPostalCodePattern.matcher(nextToLastLine);
                    Matcher codeProvinceMatcher = caCodeProvincePattern.matcher(nextToLastLine);
                    Matcher cityProvCodeMatcher = caCityProvCodePattern.matcher(nextToLastLine);

                    if (postalCodeMatcher.matches())
                    {
                        zip = postalCodeMatcher.group(1);
                        
                        if (lines.length < 3)
                        {
                            getLogger().warn("Only postal code and province present: " + addressString);
                        }
                        else
                        {
                            String thirdToLastLine = lines[lines.length - 3];
                            Matcher cityProvMatcher = csPattern.matcher(thirdToLastLine);
                            
                            if (cityProvMatcher.matches())
                            {
                                city = cityProvMatcher.group(1);
                                state = cityProvMatcher.group(2);
                                
                                Matcher provMatcher = caProvincePattern.matcher(state);
                                if (provMatcher.matches())
                                {
                                    fillAddressLines(lines, lines.length - 3, address);
                                }
                                else
                                {
                                    fillAddressLines(lines, lines.length - 2, address);
                                }
                            }
                            else
                            {
                                fillAddressLines(lines, lines.length - 2, address);
                            }
                        }
                    }
                    // didn't match just a postal code, try to match a code, province
                    else if (codeProvinceMatcher.matches())
                    {
                        zip = codeProvinceMatcher.group(1);
                        state = codeProvinceMatcher.group(2);
                        
                        fillAddressLines(lines, lines.length - 2, address);
                    }
                    else if (cityProvCodeMatcher.matches())
                    {
                        city = cityProvCodeMatcher.group(1);
                        state = cityProvCodeMatcher.group(2);
                        zip = cityProvCodeMatcher.group(3);
                        
                        fillAddressLines(lines, lines.length - 2, address);
                    }
                    else
                    {
                        fillAddressLines(lines, lines.length - 1, address);
                    }
                }
                else if (auMatcher.matches())
                {
                    // matched Australia
                    Matcher postalCodeMatcher   = auPostalCodePattern.matcher(nextToLastLine);
                    Matcher territoryMatcher = auTerritoryPattern.matcher(nextToLastLine);
                    Matcher cityTerrCodeMatcher = auCityTerrCodePattern.matcher(nextToLastLine);

                    if (postalCodeMatcher.matches())
                    {
                        zip = postalCodeMatcher.group(1);

                        if (lines.length < 3)
                        {
                            getLogger().warn("Only matched country and postal code");
                        }
                        else
                        {
                            String thirdToLastLine = lines[lines.length - 3];
                            Matcher cityTerrMatcher = auCityTerrPattern.matcher(thirdToLastLine);

                            if (cityTerrMatcher.matches())
                            {
                                city = cityTerrMatcher.group(1);
                                state = cityTerrMatcher.group(2);

                                fillAddressLines(lines, lines.length - 3, address);
                            }
                            else
                            {
                                fillAddressLines(lines, lines.length - 2, address);
                            }
                        }
                    }
                    // didn't match just a postal code, try to match a territory
                    else if (territoryMatcher.matches())
                    {
                        state = territoryMatcher.group(1);
                        
                        fillAddressLines(lines, lines.length - 2, address);
                    }
                    else if (cityTerrCodeMatcher.matches())
                    {
                        city = cityTerrCodeMatcher.group(1);
                        state = cityTerrCodeMatcher.group(2);
                        zip = cityTerrCodeMatcher.group(4);
                        
                        fillAddressLines(lines, lines.length - 2, address);
                    }
                    else
                    {
                        fillAddressLines(lines, lines.length - 1, address);
                    }
                }
                else
                {
                    // matched country other than US/Canada/Australia;
                    // put everything in address1 and address2
                    fillAddressLines(lines, lines.length - 1, address);
                }
            }
            else if (lastLineZipMatcher.matches()) // assume US
            {
                zip = lastLineZipMatcher.group(1);

                Matcher cityStateMatcher = csPattern.matcher(nextToLastLine);

                // try to match city, state on the next-to-last-line
                if (cityStateMatcher.matches())
                {
                    city = cityStateMatcher.group(1);
                    state = cityStateMatcher.group(2);

                    Matcher stateMatcher = statePattern.matcher(state);
                    Matcher stMatcher = stPattern.matcher(state);

                    if (stateMatcher.matches())
                    {
                        state = stateMatcher.group(1);
                    }
                    else if (stMatcher.matches())
                    {
                        state = stMatcher.group(1);
                    }
                    else
                    {
                        getLogger().warn("Couldn't match city and state: " + addressString);
                        city = null;
                        state = null;
                    }
                    
                    if (city != null)
                    {
                        fillAddressLines(lines, lines.length - 2, address);
                    }
                    else
                    {
                        fillAddressLines(lines, lines.length - 1, address);
                    }
                }
                // did not match city, state on the next-to-last line
                else
                {
                    fillAddressLines(lines, lines.length - 1, address);
                }
            }
            else if (lastLineCityStateZipMatcher.matches())
            {
                city  = lastLineCityStateZipMatcher.group(1);
                state = lastLineCityStateZipMatcher.group(2);
                zip   = lastLineCityStateZipMatcher.group(3);

                Matcher stateMatcher = statePattern.matcher(state);
                Matcher stMatcher = stPattern.matcher(state);

                if (stateMatcher.matches())
                {
                    state = stateMatcher.group(1);
                }
                else if (stMatcher.matches())
                {
                    state = stMatcher.group(1);
                }
                else
                {
                    getLogger().warn("Couldn't match city and state: " + addressString);
                    city = null;
                    state = null;
                }

                // any previous lines are address, address2
                if (city != null)
                {
                    fillAddressLines(lines, lines.length - 1, address);
                }
                else
                {
                    fillAddressLines(lines, lines.length - 0, address);
                }
            }
            else if (lastLine.contains(","))
            {
                String maybeCountry = lastLine.substring(lastLine.indexOf(',') + 1);
                Matcher countryMatcher = countryPattern.matcher(maybeCountry);
                
                if (countryMatcher.matches())
                {
                    country = countryMatcher.group(1);
                    
                    lines[lines.length -1] = lastLine.substring(0, lastLine.indexOf(','));
                    
                }

                fillAddressLines(lines, lines.length - 1, address);
            }
            else
            {
                // matched neither a country nor a zip nor a city/state on the last line, warn about this
                getLogger().warn("Didn't match country or zip code on last line: " + addressString);
                fillAddressLines(lines, lines.length, address);
            }
        }

        if (city != null) city = truncate(city, 64, "city");
        address.setCity(city);

        if (country != null) country = truncate(country, 64, "country");
        address.setCountry(country);

        if (state !=  null) state = truncate(state, 64, "state");
        address.setState(state);

        if (zip != null) zip = truncate(zip, 32, "zip code");
        address.setPostalCode(zip);

        // put the original here for safekeeping
        address.setRemarks(addressString.replace('^', '\n'));

        return address;
    }
    
    /**
     * If there is one string in the array, put it in address1; if there are two, put the first in address1
     * and the second in address2; if there are more, join them all and put them in address1.  Consider all
     * elements of the array including and after endIndex as out of bounds.
     */
    private void fillAddressLines(String[] lines, int endIndex, Address address)
    {
        String address1 = null;
        String address2 = null;
        
        if (endIndex <= 0 || lines.length == 0)
        {
            // no lines left
            getLogger().warn("No address line");
        }
        else if (endIndex == 1)
        {
            address1 = lines[0];
        }
        else if (endIndex == 2 && lines.length >= 2)
        {
            address1 = lines[0];
            address2 = lines[1];
        }
        else if (endIndex >= lines.length)
        {
            address1 = join(lines, "\n", 0, lines.length);
        }
        else
        {
            address1 = join(lines, "\n", 0, endIndex);
        }

        if (address1 != null) address1 = truncate(address1, 255, "address line");
        address.setAddress(address1);

        if (address2 != null) address2 = truncate(address2, 255, "second address line");
        address.setAddress2(address2);
    }

    /**
     * Join the strings on the glue beginning with beginIndex up to and not including endIndex
     */
    private String join(String[] strings, String glue, int beginIndex, int endIndex)
    {
        if (glue == null) glue = "";
        if (strings.length < beginIndex) return "";
        
        StringBuilder result = new StringBuilder(strings[beginIndex]);
        
        for (int i = beginIndex + 1; i < strings.length && i < endIndex; i++)
        {
            result.append(glue + strings[i]);
        }

        return result.toString();
    }
}
