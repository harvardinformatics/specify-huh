package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.InReturnBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;

public class InReturnBatchLoader extends CsvToSqlLoader
{
	public InReturnBatchLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
		super(csvFile, sqlStatement);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		InReturnBatch inReturnBatch = parse(columns);
		
		Integer inReturnBatchId = inReturnBatch.getId();
		setCurrentRecordId(inReturnBatchId);

		LoanReturnPreparation loanReturnPreparation = getLoanReturnPreparation(inReturnBatch);
		
		String sql = getInsertSql(loanReturnPreparation);
		insert(sql);
	}

	private InReturnBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 8)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		InReturnBatch inReturnBatch = new InReturnBatch();
		try
		{	
			inReturnBatch.setId(                SqlUtils.parseInt( columns[0] ));
			inReturnBatch.setTransactionId(     SqlUtils.parseInt( columns[1] ));
			inReturnBatch.setType(          Transaction.parseType( columns[2] ));
			inReturnBatch.setItemCount(         SqlUtils.parseInt( columns[3] ));
			inReturnBatch.setBoxCount(                             columns[4] );
			inReturnBatch.setIsAcknowledged( Boolean.parseBoolean( columns[5] ));
			inReturnBatch.setActionDate(       SqlUtils.parseDate( columns[6] ));
			inReturnBatch.setTransferredTo(                        columns[7] );
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return inReturnBatch;
	}
	
    private LoanReturnPreparation getLoanReturnPreparation(InReturnBatch inReturnBatch)
    {
        LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
        
        // TODO: taxon batch needs to go first
        
        return loanReturnPreparation;
    }
	
    private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
    {
        String fieldNames = "CollectionMemberID, LoanPreparationID, Quantity, ReturnedDate, " +
                            "TimestampCreated";
        
        String[] values = new String[5];
        
        values[0] = SqlUtils.sqlString( loanReturnPreparation.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
        values[2] = SqlUtils.sqlString( loanReturnPreparation.getQuantityReturned());
        values[3] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
        values[4] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
    }
}
