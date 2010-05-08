package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.Botanist;
import edu.harvard.huh.asa.BotanistName;
import edu.harvard.huh.asa.Subcollection;
import edu.harvard.huh.asa.BotanistName.TYPE;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.harvard.huh.asa2specify.lookup.SubcollectionLookup;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentVariant;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.ReferenceWork;
import edu.ku.brc.specify.datamodel.Storage;
import edu.ku.brc.specify.datamodel.StorageTreeDef;
import edu.ku.brc.specify.datamodel.StorageTreeDefItem;

// Run this class after TaxonLoader.

public class SubcollectionLoader extends TreeLoader
{
    public static Integer FRUIT_SUBCOLL    = 1001;
    public static Integer BURT_SUBCOLL     = 3101;
    public static Integer CURTIS_SUBCOLL   = 3142;
    public static Integer DIATOM_SUBCOLL   = 3156;
    public static Integer DODGE_SUBCOLL    = 3170;
    public static Integer FLEISCH_SUBCOLL  = 3187;
    public static Integer LABOUL_SUBCOLL   = 3314;
    public static Integer PATOUIL_SUBCOLL  = 3521;
    public static Integer SCHIFF_SUBCOLL   = 3563;
    public static Integer SULLIV_SUBCOLL   = 3585;
    public static Integer TAYLOR_SUBCOLL   = 3590;
    public static Integer TRICHO_SUBCOLL   = 3671;
    public static Integer TUCKER_SUBCOLL   = 3596;
    
    public static Integer BURT_SLIDE_SUBCOLL   = 9000; // burt slide collection"
    public static Integer FARLOW_SLIDE_SUBCOLL = 9001; // farlow (microscope) slide collection"
    public static Integer GEN_FUN_SUBCOLL      = 9002; // "general fungus herbarium type collection"
    public static Integer GLYCERINE_SUBCOLL    = 9003; // "in glycerine collection"
    public static Integer HYMENO_SUBCOLL       = 9004; // "hymenomycetes) boxed type(s)"
    public static Integer THEISSEN_SUBCOLL     = 9005; // "theissen collection"
    public static Integer TRICHO_TYPE_SUBCOLL  = 9006; // "trichomycete type slide collection"
    
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
        disableKeys("storage");
        
        StorageTreeDefItem buildingDefItem = getDefItem("Building");
        buildingDefItem.setRankId(buildingRankId);

        StorageTreeDefItem collectionDefItem = getDefItem("Collection");
        collectionDefItem.setRankId(collectionRankId);

        this.subcollDefItem = getDefItem("Subcollection");
        this.subcollDefItem.setRankId(subcollRankId);

        Storage site = new Storage();
        site.setStorageId(1);
        
        Storage building = createNode("HUH", null, site, buildingDefItem);
        
        vascular = createNode("Vascular", null, building, collectionDefItem);
        cryptogams = createNode("Cryptogams", null, building, collectionDefItem);
    }

    @Override
    protected void postLoad() throws LocalException
    {
        String[] cryptoNames    = { "Burt Slide Collection", "Farlow Slide Collection", "General Fungus Herbarium Type Collection", "Hymenomycete Boxed Type Collection", "Theissen Collection", "Trichomycete Type Slide Collection" };
        Integer[] cryptoNumbers = { BURT_SLIDE_SUBCOLL,      FARLOW_SLIDE_SUBCOLL,      GEN_FUN_SUBCOLL,                            HYMENO_SUBCOLL,                       THEISSEN_SUBCOLL,      TRICHO_TYPE_SUBCOLL                  };

        for (int i = 0; i < cryptoNames.length; i++)
        {
            createNode(cryptoNames[i], cryptoNumbers[i], cryptogams, subcollDefItem);
        }
        
        String[] vascNames    = { "Glycerine Collection" };
        Integer[] vascNumbers = { GLYCERINE_SUBCOLL      };

        for (int i = 0; i < vascNames.length; i++)
        {
            createNode(vascNames[i], vascNumbers[i], vascular, subcollDefItem);
        }
        
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

    Storage createNode(String name, Integer number1, Storage parent, StorageTreeDefItem defItem) throws LocalException
    {
        Storage storage = new Storage();

        // Abbrev
        storage.setAbbrev(null); // set aside for barcode
        
        // FullName
        storage.setFullName(truncate(name, 255, "full name"));

        // IsAccepted
        storage.setIsAccepted(true);

        // Name
        storage.setName(name);
        
        // Number1
        storage.setNumber1(number1);
        
        // Parent
        storage.setParent(parent);

        // RankID
        storage.setRankId(defItem.getRankId());
        
        // StorageTreeDef
        storage.setDefinition(treeDef);

        // StorageTreeDefItem
        storage.setDefinitionItem(defItem);
        
        setNullAuditFields(storage);

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
                    
                    // create an author name agent variant
                    AgentVariant agentVariant = getAgentVariant(subcollection, agent);

                    sql = getInsertSql(agentVariant);
                    insert(sql);
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
                
                public ReferenceWork queryExsiccataById(Integer subcollectionId) throws LocalException
                {
                    if (subcollectionId == null) return null;
                    
                    ReferenceWork exsiccata = new ReferenceWork();
                    
                    String guid = getGuid(subcollectionId);

                    String sql = "select ReferenceWorkID from referencework where GUID=\"" +  guid + "\"";
                    
                    Integer exsiccataId = queryForInt(sql);
                    
                    if (exsiccataId == null) return null;

                    exsiccata.setReferenceWorkId(exsiccataId);
                    
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
    		subcollection.setRemarks(                         columns[i + 7]  );
    		subcollection.setExsiccata( Boolean.parseBoolean( columns[i + 8] ));
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
        if (fullName.length() > 255) fullName = truncate(fullName, 255, "full name");
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
        
        // Text1 (the "Title" field is reserved for title abbreviations)
        String title = subcollection.getName();
        checkNull(title, "title");
        title = truncate(title, 255, "title");
        referenceWork.setText1(title);
        
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
    
    private Agent getAgent(Subcollection subcollection) throws LocalException
    {
    	Agent agent = new Agent();
                
        String author = subcollection.getAuthor();
        if (author == null) return null;
        
        Botanist botanist = new Botanist();
        botanist.setName(author);
        
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
    
    private AgentVariant getAgentVariant(Subcollection subcollection, Agent agent) throws LocalException
    {
        AgentVariant agentVariant = new AgentVariant();
        
        // Agent
        agentVariant.setAgent(agent);

        // Name
        String name = subcollection.getAuthor();
        checkNull(name, "name");

        name = truncate(name, 255, "name");
        agentVariant.setName(name);
        
        // Type
        agentVariant.setVarType(AgentVariant.AUTHOR);
        
        return agentVariant;
    }
    
    String getInsertSql(Storage storage) throws LocalException
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
		String fieldNames = "GUID, ReferenceWorkType, Remarks, Text1, TimestampCreated, Version";

		String[] values = new String[6];

		values[0] = SqlUtils.sqlString( referenceWork.getGuid());
		values[1] = SqlUtils.sqlString( referenceWork.getReferenceWorkType());
		values[2] = SqlUtils.sqlString( referenceWork.getRemarks());
        values[3] = SqlUtils.sqlString( referenceWork.getText1());
		values[4] = SqlUtils.now();
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
    
    private String getInsertSql(AgentVariant agentVariant)
    {
        String fieldNames = "AgentID, Name, VarType, TimestampCreated, Version";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( agentVariant.getAgent().getId());
        values[1] = SqlUtils.sqlString( agentVariant.getName());
        values[2] = SqlUtils.sqlString( agentVariant.getVarType());
        values[3] = SqlUtils.now();
        values[4] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("agentvariant", fieldNames, values);
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
