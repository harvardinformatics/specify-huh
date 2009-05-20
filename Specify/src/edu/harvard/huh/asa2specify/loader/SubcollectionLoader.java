package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Subcollection;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.Container;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonCitation;

public class SubcollectionLoader extends CsvToSqlLoader {

    private AsaIdMapper subcollMapper;
    
	public SubcollectionLoader(File csvFile, Statement sqlStatement, File subcollToBotanist) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.subcollMapper = new AsaIdMapper(subcollToBotanist);
	}
	
	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Subcollection subcollection = parse(columns);

		// get Collection object
		String code = subcollection.getCollectionCode();
		Collection collection = getCollection(code);

		// get Container object
		Container container = getContainer(subcollection);
		container.setCollectionMemberId(collection.getId());

		// convert container to sql and insert
		String sql = getInsertSql(container);
        Integer containerId = insert(sql);
        container.setContainerId(containerId);

        // if this subcollection represents an exsiccata...
        String authorName = subcollection.getAuthor();
        if (authorName != null)
        {
            // get ReferenceWork object
            ReferenceWork referenceWork = getReferenceWork(subcollection);

            // convert referencework to sql and insert
            sql = getInsertSql(referenceWork);
            Integer referenceWorkId = insert(sql);
            referenceWork.setReferenceWorkId(referenceWorkId);

            // get Exsiccata object
            Exsiccata exsiccata = getExsiccata(subcollection);

            // create an exsiccata
            exsiccata.setReferenceWork(referenceWork);
            sql = getInsertSql(exsiccata);
            Integer exsiccataId = insert(sql);
            exsiccata.setExsiccataId(exsiccataId);
            
            // create a taxon citation
            Integer taxonGroupId = subcollection.getTaxonGroupId();
            if (taxonGroupId != null)
            {
                String taxonSerNumber = String.valueOf(taxonGroupId);
                
                sql = SqlUtils.getQueryIdByFieldSql("taxon", "TaxonID", "TaxonomicSerialNumber", taxonSerNumber);
                Integer taxonId = queryForId(sql);
                
                Taxon taxon = new Taxon();
                taxon.setTaxonId(taxonId);
                
                TaxonCitation taxonCitation = new TaxonCitation();
                taxonCitation.setReferenceWork(referenceWork);
                taxonCitation.setTaxon(taxon);
                taxonCitation.setRemarks("Created by SubcollectionLoader from asa subcollection id " + subcollection.getId());
                
                sql = getInsertSql(taxonCitation);
                insert(sql);
            }
            else
            {
                warn("No taxon group id", subcollection.getId(), null);
            }

            // find matching agent for author or create one
            Agent authorAgent = new Agent();

            Integer botanistId = getBotanistId(subcollection.getId());
            if (botanistId != null)
            {
                Botanist botanist = new Botanist();
                botanist.setId(botanistId);
                
                String guid = botanist.getGuid();
                
                sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);
                Integer authorAgentId = queryForId(sql);
                authorAgent.setAgentId(authorAgentId);
            }
            else
            {
                // create an agent
                Agent agent = getAgent(subcollection);
                
                // convert agent to sql and insert
                sql = getInsertSql(agent);
                Integer authorAgentId = insert(sql);
                authorAgent.setAgentId(authorAgentId);
            }

            // create an author
            Author author = new Author();

            author.setAgent(authorAgent);
            author.setReferenceWork(referenceWork);
            author.setOrderNumber((short) 1);
            getInsertSql(author);

            insert(sql);
        }
	}

	private Integer getBotanistId(Integer subcollectionId)
	{
	    return subcollMapper.map(subcollectionId);
	}

	private Subcollection parse(String[] columns) throws LocalException
    {
    	if (columns.length < 11)
    	{
    		throw new LocalException("Wrong number of columns");
    	}

    	Subcollection subcollection = new Subcollection();

    	try
    	{
    	    subcollection.setId(            Integer.parseInt( StringUtils.trimToNull( columns[0]  )));
    	    subcollection.setCollectionCode(                  StringUtils.trimToNull( columns[1]  ));
    	    subcollection.setTaxonGroupId(  Integer.parseInt( StringUtils.trimToNull( columns[2]  )));
    	    subcollection.setName(                            StringUtils.trimToNull( columns[3]  ));
    	    subcollection.setAuthor(                          StringUtils.trimToNull( columns[4]  ));
    	    subcollection.setSpecimenCount(                   StringUtils.trimToNull( columns[5]  ));
    	    subcollection.setLocation(                        StringUtils.trimToNull( columns[6]  ));
    	    subcollection.setCabinet(                         StringUtils.trimToNull( columns[7]  ));
    	    
            String createDateString =                         StringUtils.trimToNull( columns[8]  );
            Date createDate = SqlUtils.parseDate(createDateString);
            subcollection.setDateCreated(createDate);
            
            subcollection.setCreatedById( Integer.parseInt(   StringUtils.trimToNull( columns[9]  )));
    		subcollection.setRemarks(                         StringUtils.trimToNull( columns[10] ));
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	
    	return subcollection;
    }
    
    private Container getContainer(Subcollection subcollection) throws LocalException
    {    
        Container container = new Container();
        
        String name = subcollection.getName();
        if (name == null)
        {
            throw new LocalException("No name");
        }
        container.setName(name);
        
        // this is how we will match the specimen item records to the new containers
        Integer subcollectionId = subcollection.getId();
        if (subcollectionId == null)
        {
            throw new LocalException("No id");
        }
        container.setNumber(subcollectionId);
        
        String specimenCount = subcollection.getSpecimenCount();
        String      location = subcollection.getLocation();
        String       cabinet = subcollection.getCabinet();
        String       remarks = subcollection.getRemarks();
        
        if (specimenCount != null && location != null && cabinet != null && remarks != null)
        {
            String description = "Specimen count: " + specimenCount == null ? "" : specimenCount + ";" +
                                 "Location: "       +      location == null ? "" : location      + ";" +
                                 "Cabinet:"         +       cabinet == null ? "" : cabinet       + ";" +
                                 "Remarks:"         +       remarks == null ? "" : remarks       + ";";
            
            container.setDescription(description);
        }

        return container;
	}
    
    private ReferenceWork getReferenceWork(Subcollection subcollection) throws LocalException
    {
        ReferenceWork referenceWork = new ReferenceWork();
        
        referenceWork.setReferenceWorkType(ReferenceWork.BOOK);

        referenceWork.setGuid(subcollection.getGuid());
        
        String title = subcollection.getName();
        if (title == null)
        {
            throw new LocalException("No title");
        }
        referenceWork.setTitle(title);
        
        String remarks = subcollection.getRemarks();
        referenceWork.setRemarks(remarks);
        
        return referenceWork;
    }
    
    private Exsiccata getExsiccata(Subcollection subcollection) throws LocalException
    {
        Exsiccata exsiccata = new Exsiccata();
        
        String title = subcollection.getName();
        if (title == null)
        {
            throw new LocalException("No title");
        }
        exsiccata.setTitle(title);
        
        return exsiccata;
    }
    
    private Agent getAgent(Subcollection subcollection) throws LocalException
    {
        Agent agent = new Agent();
        
        String author = subcollection.getAuthor();
        if (author == null)
        {
            throw new LocalException("No author");
        }
        
        Botanist botanist = new Botanist();
        botanist.setName(author);
        
        // LastName
        String lastName = botanist.getLastName();
        if (lastName == null)
        {
            throw new LocalException("No last name in subcollection record " + subcollection.getId());
        }

        if (lastName.length() > 50)
        {
            warn("Truncating last name", subcollection.getId(), lastName);
            lastName = lastName.substring(0, 50);
        }
        agent.setLastName(lastName);
        
        // FirstName
        String firstName = botanist.getFirstName();
        if (firstName != null && firstName.length() > 50)
        {
            warn("Truncating first name", subcollection.getId(), firstName);
            firstName = firstName.substring(0, 50);
        }
        agent.setFirstName(firstName);

        // AgentType
        if (botanist.isOrganization() ) agent.setAgentType( Agent.ORG );
        else if (botanist.isGroup()) agent.setAgentType( Agent.GROUP );
        else agent.setAgentType( Agent.PERSON );

        // Remarks
        agent.setRemarks("Created by SubcollectionLoader from asa subcollection id " + subcollection.getId());

        return agent;
    }

    private String getInsertSql(Container container) throws LocalException
    {
        String fieldNames = "Name, Number, CollectionMemberID, Description";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( container.getName());
        values[1] = SqlUtils.sqlString( container.getNumber());
        values[2] = SqlUtils.sqlString( container.getCollectionMemberId());
        values[3] = SqlUtils.sqlString( container.getDescription());
        
        return SqlUtils.getInsertSql("container", fieldNames, values);
    }
    
    private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "GUID, ReferenceWorkType, Title, TimestampCreated, Remarks";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( referenceWork.getGuid());
		values[1] = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[2] = SqlUtils.sqlString( referenceWork.getTitle());
		values[3] = "now()";
		values[4] = SqlUtils.sqlString( referenceWork.getRemarks());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
    
    private String getInsertSql(Agent agent) throws LocalException
    {
        String fieldNames = 
            "AgentType, FirstName, LastName, TimestampCreated, Remarks";

        String[] values = new String[5];

        values[0] = SqlUtils.sqlString( agent.getAgentType());
        values[1] = SqlUtils.sqlString( agent.getFirstName());
        values[2] = SqlUtils.sqlString( agent.getLastName());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.sqlString( agent.getRemarks());

        return SqlUtils.getInsertSql("agent", fieldNames, values);
    }

    private String getInsertSql(Exsiccata exsiccata) throws LocalException
    {
    	String fieldNames = "Title, ReferenceWorkID, TimestampCreated";
 
    	String[] values = new String[3];
    	
    	values[0] = SqlUtils.sqlString( exsiccata.getTitle());
    	values[1] = SqlUtils.sqlString( exsiccata.getReferenceWork().getReferenceWorkId());
    	values[2] = SqlUtils.now();
    	
    	return SqlUtils.getInsertSql("exsiccata", fieldNames, values);
    }
    
    private String getInsertSql(Author author)
	{
		String fieldNames = "AgentId, ReferenceWorkId, OrderNumber, TimestampCreated";

		String[] values = new String[4];

		values[0] = String.valueOf( author.getAgent().getId());
		values[1] = String.valueOf( author.getReferenceWork().getId());
		values[2] = String.valueOf( author.getOrderNumber());
		values[3] = SqlUtils.now();

		return SqlUtils.getInsertSql("author", fieldNames, values);
	}
    
    private String getInsertSql(TaxonCitation taxonCitation)
    {
        String fieldNames = "TaxonID, ReferenceWorkID, Remarks, TimestampCreated";
        
        String[] values = new String[4];
        
        values[0] = SqlUtils.sqlString( taxonCitation.getTaxon().getId());
        values[1] = SqlUtils.sqlString( taxonCitation.getReferenceWork().getId());
        values[2] = SqlUtils.sqlString( taxonCitation.getRemarks());
        values[3] = SqlUtils.now();

        return SqlUtils.getInsertSql("taxoncitation", fieldNames, values);
    }
}
