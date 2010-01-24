package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.Subcollection;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Storage;
import edu.ku.brc.specify.datamodel.StorageTreeDef;
import edu.ku.brc.specify.datamodel.StorageTreeDefItem;

// Run this class after TaxonLoader.

public class SubcollectionLoader extends TreeLoader
{
    private String getGuid(Integer subcollectionId)
	{
		return subcollectionId + " subcoll";
	}
	
    private AsaIdMapper subcollMapper;
    private BotanistLookup botanistLookup;
    
    private SubcollectionLookup subcollLookup;
    
    private Storage vascular;
    private Storage cryptogams;
    
    private StorageTreeDef treeDef;
    private StorageTreeDefItem subcollDefItem;

    private int buildingRankId   = 100;
    private int collectionRankId = 150;
    private int subcollRankId    = 175;

    public SubcollectionLoader(File csvFile,
	                           Statement sqlStatement,
	                           File subcollToBotanist,
	                           BotanistLookup botanistLookup)
	    throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.subcollMapper = new AsaIdMapper(subcollToBotanist);
		
		this.botanistLookup = botanistLookup;
		
		this.treeDef = getStorageTreeDef();
	}

    @Override
    protected void preLoad() throws LocalException
    {
        StorageTreeDefItem buildingDefItem = getDefItem("Building");
        buildingDefItem.setRankId(buildingRankId);

        StorageTreeDefItem collectionDefItem = getDefItem("Collection");
        collectionDefItem.setRankId(collectionRankId);

        this.subcollDefItem = getDefItem("Subcollection");
        this.subcollDefItem.setRankId(subcollRankId);

        Storage site = new Storage();
        site.setStorageId(1);
        
        Storage building = createNode("HUH", site, buildingDefItem);
        
        vascular = createNode("Vascular", building, collectionDefItem);
        cryptogams = createNode("Cryptogams", building, collectionDefItem);
    }

    private StorageTreeDef getStorageTreeDef() throws LocalException
    {
        StorageTreeDef s = new StorageTreeDef();

        Integer storageTreeDefId = getId("storagetreedef", "StorageTreeDefID", "Name", "Storage");

        s.setStorageTreeDefId(storageTreeDefId);

        return s;
    }
	
    private StorageTreeDefItem getDefItem(String name) throws LocalException
    {
        Integer storageTreeDefItemId = getId("storagetreedefitem", "StorageTreeDefItemID", "Name", name);

        StorageTreeDefItem treeDefItem = new StorageTreeDefItem();
        treeDefItem.setStorageTreeDefItemId(storageTreeDefItemId);
        
        return treeDefItem;
    }

    private Storage createNode(String name, Storage parent, StorageTreeDefItem defItem) throws LocalException
    {
        Storage storage = new Storage();

        // Abbrev
        storage.setAbbrev(name);
        
        // FullName
        storage.setFullName(name);

        // IsAccepted
        storage.setIsAccepted(true);

        // Name
        storage.setName(name);
        
        // Parent
        storage.setParent(parent);

        // RankID
        storage.setRankId(defItem.getRankId());
        
        // StorageTreeDef
        storage.setDefinition(treeDef);

        // StorageTreeDefItem
        storage.setDefinitionItem(defItem);
        
        String sql = getInsertSql(storage);
        Integer storageId = insert(sql);
        
        storage.setStorageId(storageId);
        
        return storage;
    }

    @Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Subcollection subcollection = parse(columns);

		Integer subcollectionId = subcollection.getId();
		setCurrentRecordId(subcollectionId);
		
        // if this subcollection represents an exsiccata...
        if (subcollection.isExsiccata())
        {
            // get ReferenceWork object
            ReferenceWork referenceWork = getReferenceWork(subcollection);

            // convert referencework to sql and insert
            String sql = getInsertSql(referenceWork);
            Integer referenceWorkId = insert(sql);
            referenceWork.setReferenceWorkId(referenceWorkId);

            // get an Exsiccata object
            Exsiccata exsiccata = getExsiccata(subcollection, referenceWork);
            sql = getInsertSql(exsiccata);
            Integer exsiccataId = insert(sql);
            exsiccata.setExsiccataId(exsiccataId);

            // find matching agent for author or create one
            Agent agent = null;

            Integer botanistId = getBotanistId(subcollection.getId());
            if (botanistId != null)
            {
                // find an agent
                agent = lookupBotanist(botanistId);
            }
            else
            {
                // create an agent
                agent = getAgent(subcollection);
                
                if (agent != null)
                {
                    // convert agent to sql and insert
                    sql = getInsertSql(agent);
                    Integer authorAgentId = insert(sql);
                    agent.setAgentId(authorAgentId);
                }
            }

            if (agent != null)
            {
                // create an author
                Author author = getAuthor(subcollection, agent, referenceWork);
                sql = getInsertSql(author);
                insert(sql);
            }
        }

        // create StorageLocation object
        Storage storage = getStorage(subcollection);

        // convert storage to sql and insert
        String sql = getInsertSql(storage);
        Integer storageId = insert(sql);
        storage.setStorageId(storageId);

	}

	public void numberNodes() throws LocalException
	{
	    numberNodes("storage", "StorageID");
	}

    public SubcollectionLookup getSubcollectionLookup()
    {
        if (subcollLookup == null)
        {
            subcollLookup = new SubcollectionLookup() {
                
                public Exsiccata getExsiccataById(Integer subcollectionId) throws LocalException
                {
                    Exsiccata exsiccata = new Exsiccata();
                    
                    String guid = getGuid(subcollectionId);

                    String sql = "select ExsiccataID from exsiccata where ReferenceWorkID=(select ReferenceWorkID from referencework where GUID=\"" +  guid + "\")";
                    
                    Integer exsiccataId = getInt(sql);

                    exsiccata.setExsiccataId(exsiccataId);
                    
                    return exsiccata;
                }
                
                public Storage getStorageById(Integer subcollectionId) throws LocalException
                {
                    Storage storage = new Storage();
                    
                    Integer storageId = getInt("storage", "StorageID", "Number1", subcollectionId);

                    storage.setStorageId(storageId);
                    
                    return storage;
                }
            };
        }
        return subcollLookup;
    }

	private Integer getBotanistId(Integer subcollectionId)
	{
	    return subcollMapper.map(subcollectionId);
	}

	private Agent lookupBotanist(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getById(botanistId);
	}
	
	private Subcollection parse(String[] columns) throws LocalException
    {
	    Subcollection subcollection = new Subcollection();
	    
	    int i = super.parse(columns, subcollection);
	    
    	if (columns.length < i + 8)
    	{
    		throw new LocalException("Not enough columns");
    	}

    	try
    	{
    	    subcollection.setCollectionCode(                  columns[i + 0]  );
    	    subcollection.setTaxonGroup(                      columns[i + 1]  );
    	    subcollection.setName(                            columns[i + 2]  );
    	    subcollection.setAuthor(                          columns[i + 3]  );
    	    subcollection.setSpecimenCount(                   columns[i + 4]  );
    	    subcollection.setLocation(                        columns[i + 5]  );
    	    subcollection.setCabinet(                         columns[i + 6]  );        
    		subcollection.setRemarks(                         columns[i + 7] );
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	
    	return subcollection;
    }
    
    private Storage getStorage(Subcollection subcollection) throws LocalException
    {    
        Storage storage = new Storage();

        // FullName
        String fullName = subcollection.getName();
        if (fullName.length() > 255) truncate(fullName, 255, "full name");
        storage.setFullName(fullName);

        // IsAccepted
        storage.setIsAccepted(true);
        
        // Name
        String name = fullName;
        if (name.length() > 64)
        {
            getLogger().warn(rec() + "truncating name: " + fullName);
            name = name.substring(0, 64);
        }
        storage.setName(name);

        // Number1 this is how we will match the specimen item records to the storage records
        Integer subcollectionId = subcollection.getId();
        checkNull(subcollectionId, "id");

        storage.setNumber1(subcollectionId);
        
        // Parent
        String code = subcollection.getCollectionCode();
        if (code.equals("A")) storage.setParent(vascular);
        else if (code.equals("FH")) storage.setParent(cryptogams);
        else throw new LocalException(rec() + "Unrecognized collection code: " + code);
        
        // RankId
        storage.setRankId(subcollRankId);

        // Remarks
        String description = getStorageDescription(subcollection);
        if (description != null)
        { 
            description = truncate(description, 255, "count/location/cabinet/remarks");
            storage.setRemarks(description);
        }
        
        // StorageTreeDefItem
        storage.setDefinition(treeDef);
        
        // subcollTreeDefItem
        storage.setDefinitionItem(subcollDefItem);
        
        // Text1
        String author = subcollection.getAuthor();
        if (author != null) author = truncate(author, 50, "author");
        storage.setText1(author);
        
        // Text2
        String group = subcollection.getTaxonGroup();
        if (group != null) group = truncate(group, 32, "group");
        storage.setText2(group);
        
        setAuditFields(subcollection, storage);
        
        return storage;
	}
    
    private ReferenceWork getReferenceWork(Subcollection subcollection) throws LocalException
    {
    	ReferenceWork referenceWork = new ReferenceWork();
        
        // GUID
    	Integer subcollectionId = subcollection.getId();
    	checkNull(subcollectionId, "id");
    	
        String guid = getGuid(subcollectionId);
        referenceWork.setGuid(guid);
        
        // ReferenceWorkType
        referenceWork.setReferenceWorkType(ReferenceWork.EXSICCATA);
     
        // Remarks
        String remarks = getExsiccataDescription(subcollection);
        referenceWork.setRemarks(remarks);
        
        // Title
        String title = subcollection.getName();
        checkNull(title, "title");
        title = truncate(title, 255, "title");
        referenceWork.setTitle(title);
        
        setAuditFields(subcollection, referenceWork);
        
        return referenceWork;
    }
    
    private String getExsiccataDescription(Subcollection subcollection)
    {
        String    taxonGroup = subcollection.getTaxonGroup();
        String specimenCount = subcollection.getSpecimenCount();
        String      location = subcollection.getLocation();
        String       cabinet = subcollection.getCabinet();
        String       remarks = subcollection.getRemarks();
        
        if (specimenCount != null || location != null || cabinet != null || remarks != null)
        {            
            if (taxonGroup != null)    taxonGroup    = "Taxon group: "    + taxonGroup;
            if (specimenCount != null) specimenCount = "Specimen count: " + specimenCount;
            if (location != null)      location      = "Location: "       + location;
            if (cabinet != null)       cabinet       = "Cabinet: "        + cabinet;
            if (remarks != null)       remarks       = "Remarks: "        + remarks;

            String description = taxonGroup;

            if (specimenCount != null)
            {
                description = (description == null ? specimenCount : description + "; " + specimenCount);
            }
            if (location != null)
            {
                description = (description == null ? location : description + "; " + location);
            }
            if (cabinet != null)
            {
                description = (description == null ? cabinet : description + "; " + cabinet);
            }
            if (remarks != null)
            {
                description = (description == null ? remarks : description + "; " + remarks);
            }
            
            return description;
        }
        
        return null;
    }

    private String getStorageDescription(Subcollection subcollection)
    {
        String specimenCount = subcollection.getSpecimenCount();
        String      location = subcollection.getLocation();
        String       cabinet = subcollection.getCabinet();
        String       remarks = subcollection.getRemarks();
        
        if (specimenCount != null || location != null || cabinet != null || remarks != null)
        {            
            if (specimenCount != null) specimenCount = "Specimen count: " + specimenCount;
            if (location != null)      location      = "Location: "       + location;
            if (cabinet != null)       cabinet       = "Cabinet: "        + cabinet;
            if (remarks != null)       remarks       = "Remarks: "        + remarks;

            String description = null;

            if (specimenCount != null)
            {
                description = (description == null ? specimenCount : description + "; " + specimenCount);
            }
            if (location != null)
            {
                description = (description == null ? location : description + "; " + location);
            }
            if (cabinet != null)
            {
                description = (description == null ? cabinet : description + "; " + cabinet);
            }
            if (remarks != null)
            {
                description = (description == null ? remarks : description + "; " + remarks);
            }
            
            return description;
        }
        
        return null;
    }
    
    private Exsiccata getExsiccata(Subcollection subcollection, ReferenceWork referenceWork) throws LocalException
    {	
        Exsiccata exsiccata = new Exsiccata();
        
        // ReferenceWork
        exsiccata.setReferenceWork(referenceWork);
        
        // Title
        String title = subcollection.getName();
        checkNull(title, "title");
        title = truncate(title, 255, "title");
        exsiccata.setTitle(title);
        
        setAuditFields(subcollection, exsiccata);
        
        return exsiccata;
    }
    
    private Agent getAgent(Subcollection subcollection) throws LocalException
    {
    	Agent agent = new Agent();
                
        String author = subcollection.getAuthor();
        if (author == null) return null;
        
        Botanist botanist = new Botanist();
        botanist.setName(author);
        botanist.setIsCorporate(false); // TODO: can we assume all subcoll authors are not corporate?
        botanist.setIsTeam(false);      // TODO: this is possibly not going to work for all
        
        // AgentType
        if (botanist.isOrganization() ) agent.setAgentType( Agent.ORG );
        else if (botanist.isGroup()) agent.setAgentType( Agent.GROUP );
        else agent.setAgentType( Agent.PERSON );
        
        // FirstName
        String firstName = botanist.getFirstName();
        if (firstName != null)
        {
            firstName = truncate(firstName, 50, "first name");
            agent.setFirstName(firstName);
        }
        
        // LastName
        String lastName = botanist.getLastName();
        checkNull(lastName, "last name");
        lastName = truncate(lastName, 50, "last name");
        agent.setLastName(lastName);

        setAuditFields(subcollection, agent);
        
        return agent;
    }
    
    private Author getAuthor(Subcollection subcollection, Agent agent, ReferenceWork referenceWork)
    {
    	Author author = new Author();

    	// Agent
        author.setAgent(agent);

        // OrderNumber
        author.setOrderNumber((short) 1);
        
        // ReferenceWork
        author.setReferenceWork(referenceWork);
        
    	return author;
    }
    
    private String getInsertSql(Storage storage) throws LocalException
    {
        String fieldNames = "Abbrev, CreatedByAgentID, FullName, IsAccepted, ModifiedByAgentID, Name, " +
        		            "Number1, Number2, ParentID, RankID, Remarks, StorageTreeDefID, " +
        		            "StorageTreeDefItemID, Text1, Text2, TimestampCreated, TimestampModified, Version";
        
        String[] values = new String[18];
        
        values[0]  = SqlUtils.sqlString( storage.getAbbrev());
        values[1]  = SqlUtils.sqlString( storage.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( storage.getFullName());
        values[3]  = SqlUtils.sqlString( storage.getIsAccepted());
        values[4]  = SqlUtils.sqlString( storage.getModifiedByAgent().getId());
        values[5]  = SqlUtils.sqlString( storage.getName());
        values[6]  = SqlUtils.sqlString( storage.getNumber1());
        values[7]  = SqlUtils.sqlString( storage.getNumber2());
        values[8]  = SqlUtils.sqlString( storage.getParent().getId());
        values[9]  = SqlUtils.sqlString( storage.getRankId());
        values[10] = SqlUtils.sqlString( storage.getRemarks());
        values[11] = SqlUtils.sqlString( storage.getDefinition().getId());
        values[12] = SqlUtils.sqlString( storage.getDefinitionItem().getId());
        values[13] = SqlUtils.sqlString( storage.getText1());
        values[14] = SqlUtils.sqlString( storage.getText2());
        values[15] = SqlUtils.sqlString( storage.getTimestampCreated());
        values[16] = SqlUtils.sqlString( storage.getTimestampModified());
        values[17] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("storage", fieldNames, values);
    }
    
    private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "GUID, ReferenceWorkType, Remarks, TimestampCreated, Title, Version";

		String[] values = new String[6];

		values[0] = SqlUtils.sqlString( referenceWork.getGuid());
		values[1] = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[2] = SqlUtils.sqlString( referenceWork.getRemarks());
		values[3] = SqlUtils.now();
		values[4] = SqlUtils.sqlString( referenceWork.getTitle());
		values[5] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
    
    private String getInsertSql(Agent agent) throws LocalException
    {
        String fieldNames = "AgentType, FirstName, GUID, LastName, " +
        		            "TimestampCreated, Version";

        String[] values = new String[6];

        values[0] = SqlUtils.sqlString( agent.getAgentType());
        values[1] = SqlUtils.sqlString( agent.getFirstName());
        values[2] = SqlUtils.sqlString( agent.getGuid());
        values[3] = SqlUtils.sqlString( agent.getLastName());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("agent", fieldNames, values);
    }

    private String getInsertSql(Exsiccata exsiccata) throws LocalException
    {
    	String fieldNames = "ReferenceWorkID, TimestampCreated, Title, Version";
 
    	String[] values = new String[4];
    	
    	values[0] = SqlUtils.sqlString( exsiccata.getReferenceWork().getReferenceWorkId());
    	values[1] = SqlUtils.now();
    	values[2] = SqlUtils.sqlString( exsiccata.getTitle());
    	values[3] = SqlUtils.zero();
    	
    	return SqlUtils.getInsertSql("exsiccata", fieldNames, values);
    }
    
    private String getInsertSql(Author author)
	{
		String fieldNames = "AgentId, ReferenceWorkId, OrderNumber, TimestampCreated, Version";

		String[] values = new String[5];

		values[0] = String.valueOf( author.getAgent().getId());
		values[1] = String.valueOf( author.getReferenceWork().getId());
		values[2] = String.valueOf( author.getOrderNumber());
		values[3] = SqlUtils.now();
		values[4] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("author", fieldNames, values);
	}
}
