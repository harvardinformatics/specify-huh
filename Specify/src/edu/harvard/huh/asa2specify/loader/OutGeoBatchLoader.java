package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.OutGeoBatch;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;

public class OutGeoBatchLoader extends CsvToSqlLoader {

	public OutGeoBatchLoader(File csvFile, Statement sqlStatement)
			throws LocalException {
		super(csvFile, sqlStatement);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		// TODO Auto-generated method stub

	}

	private OutGeoBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 6)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		OutGeoBatch outGeoBatch = new OutGeoBatch();
		try
		{			
			outGeoBatch.setId(               SqlUtils.parseInt( columns[0] ));
			outGeoBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
			outGeoBatch.setGeoUnit(                             columns[2] );
			outGeoBatch.setItemCount(        SqlUtils.parseInt( columns[3] ));
			outGeoBatch.setTypeCount(        SqlUtils.parseInt( columns[4] ));
			outGeoBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[5] ));			
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		
		return outGeoBatch;
	}
}
