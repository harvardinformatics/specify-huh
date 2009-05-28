package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.TaxonBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;

public class TaxonBatchLoader extends CsvToSqlLoader {

	public TaxonBatchLoader(File csvFile, Statement sqlStatement)
			throws LocalException {
		super(csvFile, sqlStatement);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		// TODO Auto-generated method stub

	}

	private TaxonBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 9)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		TaxonBatch taxonBatch = new TaxonBatch();
		try
		{
			taxonBatch.setId(               SqlUtils.parseInt( columns[0] ));
			taxonBatch.setTransactionId(    SqlUtils.parseInt( columns[1] ));
			taxonBatch.setType(         Transaction.parseType( columns[2] ));
			taxonBatch.setHigherTaxon(                         columns[3] );
			taxonBatch.setItemCount(        SqlUtils.parseInt( columns[4] ));
			taxonBatch.setTypeCount(        SqlUtils.parseInt( columns[5] ));
			taxonBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[6] ));
			taxonBatch.setTaxon(                               columns[7] );
			taxonBatch.setTransferredFrom(                     columns[8] );
			
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return taxonBatch;
	}
}
