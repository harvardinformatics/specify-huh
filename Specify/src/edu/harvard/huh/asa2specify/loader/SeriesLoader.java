package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.asa.Organization;
import edu.harvard.huh.asa.Series;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.Exsiccata;
import edu.ku.brc.specify.datamodel.Journal;
import edu.ku.brc.specify.datamodel.ReferenceWork;

public class SeriesLoader extends CsvToSqlLoader {

	public SeriesLoader(File csvFile, Statement sqlStatement) {
		super(csvFile, sqlStatement);	}

	private final Logger log = Logger.getLogger(SeriesLoader.class);
	
	@Override
	public void loadRecord(String[] columns) throws LocalException {
		Series series = parseSeriesRecord(columns);

        // convert series into referencework ...
        ReferenceWork referenceWork = convertToReferenceWork(series);
        referenceWork.setJournal(new Journal());

        // convert referencework to sql and insert
        String sql = getInsertSql(referenceWork);
        Integer referenceWorkId = insert(sql);
        referenceWork.setReferenceWorkId(referenceWorkId);
        
        // create an exsiccata
        Exsiccata exsiccata = convertToExsiccata(series);
        exsiccata.setReferenceWork(referenceWork);
        sql = getInsertSql(exsiccata);
        insert(sql);
        
        // find matching agent for author
        Integer institutionId = series.getInstitutionId();
        Author author = new Author();

        if (institutionId != null) {
        	
        	Organization organization = new Organization();
        	organization.setId(institutionId);
        	
        	String guid = organization.getGuid();
        	
        	sql = SqlUtils.getQueryIdByFieldSql("agent", "AgentID", "GUID", guid);

        	Integer agentId = queryForId(sql);

        	if (agentId != null)
        	{
        		Agent agent = new Agent();
        		agent.setAgentId(agentId);
        		author.setAgent(agent);
        		author.setReferenceWork(referenceWork);
        		author.setOrderNumber((short) 1);
                getInsertSql(author);

                insert(sql);
        	}
        }
	}

    private Series parseSeriesRecord(String[] columns) throws LocalException
    {
    	if (columns.length < 5) // TODO: 
    	{
    		throw new LocalException("Wrong number of columns");
    	}

    	Series series = new Series();

    	try {
    		series.setId(            Integer.parseInt( StringUtils.trimToNull( columns[0] )));
    		series.setName(                            StringUtils.trimToNull( columns[1] ));
    		series.setAbbreviation(                    StringUtils.trimToNull( columns[2] ));
    		series.setInstitutionId( Integer.parseInt( StringUtils.trimToNull( columns[3] )));
    		series.setNote(                            StringUtils.trimToNull( columns[4] ));
    	}
    	catch (NumberFormatException e) {
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	
    	return series;
    }
    

    private ReferenceWork convertToReferenceWork(Series series) throws LocalException {
        
		ReferenceWork referenceWork = new ReferenceWork();
		
		referenceWork.setReferenceWorkType(ReferenceWork.BOOK);

		referenceWork.setGuid(series.getGuid());
		
		String title = series.getName();
		if (title == null)
		{
			throw new LocalException("No title");
		}
		referenceWork.setTitle(title);
		
		String note = series.getNote();
		referenceWork.setRemarks(note);
		
		return referenceWork;
	}
    
    private Exsiccata convertToExsiccata(Series series) throws LocalException {
    	Exsiccata exsiccata = new Exsiccata();
    	
    	String title = series.getName();
		if (title == null) {
			throw new LocalException("No title");
		}
		exsiccata.setTitle(title);
		
		return exsiccata;
    }
    
    private String getInsertSql(ReferenceWork referenceWork) throws LocalException
	{
		String fieldNames = "GUID, ReferenceWorkType, Title, TimestampCreated, Remarks";

		String[] values = new String[5];

		values[0] = SqlUtils.sqlString( referenceWork.getGuid());
		values[1] =     String.valueOf( referenceWork.getReferenceWorkType());
		values[2] = SqlUtils.sqlString( referenceWork.getTitle());
		values[3] = "now()";
		values[4] = SqlUtils.sqlString( referenceWork.getRemarks());

		return SqlUtils.getInsertSql("referencework", fieldNames, values);    
	}
    
    private String getInsertSql(Exsiccata exsiccata) throws LocalException
    {
    	String fieldNames = "Title, ReferenceWorkID, TimestampCreated";
 
    	String[] values = new String[3];
    	
    	values[0] = SqlUtils.sqlString( exsiccata.getTitle());
    	values[1] =     String.valueOf( exsiccata.getReferenceWork().getReferenceWorkId());
    	values[2] = "now()";
    	
    	return SqlUtils.getInsertSql("exsiccata", fieldNames, values);
    }
    
    private String getInsertSql(Author author)
	{
		String fieldNames = "AgentId, ReferenceWorkId, OrderNumber, TimestampCreated";

		String[] values = new String[4];

		values[0] = String.valueOf(author.getAgent().getId());
		values[1] = String.valueOf(author.getReferenceWork().getId());
		values[2] = String.valueOf(author.getOrderNumber());
		values[3] = "now()";

		return SqlUtils.getInsertSql("author", fieldNames, values);
	}
}
