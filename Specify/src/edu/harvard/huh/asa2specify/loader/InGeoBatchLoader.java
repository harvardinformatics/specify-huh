package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.InGeoBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;

public class InGeoBatchLoader extends CsvToSqlLoader {

	public InGeoBatchLoader(File csvFile, Statement sqlStatement)
			throws LocalException {
		super(csvFile, sqlStatement);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		// TODO Auto-generated method stub

	}

	private InGeoBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 10)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		InGeoBatch inGeoBatch = new InGeoBatch();
		try
		{
			inGeoBatch.setId(               SqlUtils.parseInt( columns[0] ));
			inGeoBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
			inGeoBatch.setType(         Transaction.parseType( columns[2] ));
			inGeoBatch.setGeoUnit(                             columns[3] );
			inGeoBatch.setItemCount(        SqlUtils.parseInt( columns[4] ));
			inGeoBatch.setTypeCount(        SqlUtils.parseInt( columns[5] ));
			inGeoBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[6] ));
			inGeoBatch.setDiscardCount(     SqlUtils.parseInt( columns[7] ));
			inGeoBatch.setReturnCount(      SqlUtils.parseInt( columns[8] ));
			inGeoBatch.setCost(           SqlUtils.parseFloat( columns[9] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return inGeoBatch;
	}
}
