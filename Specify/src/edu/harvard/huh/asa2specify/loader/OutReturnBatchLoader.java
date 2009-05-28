package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.AsaShipment;
import edu.harvard.huh.asa.OutReturnBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;

public class OutReturnBatchLoader extends CsvToSqlLoader {

	public OutReturnBatchLoader(File csvFile, Statement sqlStatement)
			throws LocalException {
		super(csvFile, sqlStatement);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException {
		// TODO Auto-generated method stub

	}

	private OutReturnBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 14)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		OutReturnBatch outReturnBatch = new OutReturnBatch();
		try
		{
			outReturnBatch.setId(                 SqlUtils.parseInt( columns[0]  ));
			outReturnBatch.setTransactionId(      SqlUtils.parseInt( columns[1]  ));
			outReturnBatch.setType(           Transaction.parseType( columns[2]  ));
			outReturnBatch.setItemCount(          SqlUtils.parseInt( columns[3]  ));
			outReturnBatch.setTypeCount(          SqlUtils.parseInt( columns[4]  ));
			outReturnBatch.setNonSpecimenCount(   SqlUtils.parseInt( columns[5]  ));	
			outReturnBatch.setBoxCount(                              columns[6]  );
			outReturnBatch.setIsAcknowledged(  Boolean.parseBoolean( columns[7]  ));
			outReturnBatch.setActionDate(        SqlUtils.parseDate( columns[8]  ));
			outReturnBatch.setCarrier(     AsaShipment.parseCarrier( columns[9]  ));
			outReturnBatch.setMethod(       AsaShipment.parseMethod( columns[10] ));
			outReturnBatch.setCost(             SqlUtils.parseFloat( columns[11] ));
			outReturnBatch.setIsEstimatedCost( Boolean.parseBoolean( columns[12] ));
			outReturnBatch.setNote(                                  columns[13] );
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return outReturnBatch;
	}
}
