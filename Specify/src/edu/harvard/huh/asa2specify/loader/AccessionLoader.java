package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Division;

public class AccessionLoader extends CsvToSqlLoader {

	private final Logger log = Logger.getLogger(AccessionLoader.class);
	private Division division;
	
	public AccessionLoader(File csvFile, Statement sqlStatement, Division division) {
		super(csvFile, sqlStatement);
		this.division = division;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Accession accession = parseAccessionRecord(columns);
        accession.setDivision(division);
        
        // convert accession to sql and insert
        String sql = getInsertSql(accession);
        insert(sql);
	}

    private Accession parseAccessionRecord(String[] columns) throws LocalException
    {
    	if (columns.length < 1) // TODO: provenance?
        {
            throw new LocalException("Wrong number of columns");
        }
    	
    	Accession accession = new Accession();
    	
    	String accessionNumber = StringUtils.trimToNull(columns[0]);
    	if (accessionNumber == null) {
    		throw new LocalException("Couldn't parse accession number");
    	}
    	accession.setAccessionNumber(accessionNumber);

    	return accession;
    }
    
    private String getInsertSql(Accession accession)
    {
    	String fieldNames = "AccessionNumber, DivisionID, TimestampCreated";
    	
    	List<String> values = new ArrayList<String>(3);
    	
    	values.add(SqlUtils.sqlString(accession.getAccessionNumber() ));
    	values.add(    String.valueOf(accession.getDivision().getId()));
    	values.add("now()");
    	
    	return SqlUtils.getInsertSql("accession", fieldNames, values);
    }
}
