package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.InReturnBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.TaxonBatchLookup;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;

public class InReturnBatchLoader extends CsvToSqlLoader
{
	private TaxonBatchLookup taxonBatchLookup;
	
	public InReturnBatchLoader(File csvFile,
			                   Statement sqlStatement,
			                   TaxonBatchLookup taxonBatchLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.taxonBatchLookup = taxonBatchLookup;
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
		if (columns.length < 9)
		{
			throw new LocalException("Wrong number of columns");
		}
		
		InReturnBatch inReturnBatch = new InReturnBatch();
		try
		{	
			inReturnBatch.setId(                SqlUtils.parseInt( columns[0] ));
			inReturnBatch.setTransactionId(     SqlUtils.parseInt( columns[1] ));
			inReturnBatch.setCollectionCode(                       columns[2] );
			inReturnBatch.setType(          Transaction.parseType( columns[3] ));
			inReturnBatch.setItemCount(         SqlUtils.parseInt( columns[4] ));
			inReturnBatch.setBoxCount(                             columns[5] );
			inReturnBatch.setIsAcknowledged( Boolean.parseBoolean( columns[6] ));
			inReturnBatch.setActionDate(       SqlUtils.parseDate( columns[7] ));
			inReturnBatch.setTransferredTo(                        columns[8] );
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
	
    private LoanReturnPreparation getLoanReturnPreparation(InReturnBatch inReturnBatch) throws LocalException
    {
    	// TODO: assign boxCount
    	
        LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
        
        // CollectionMemberID (collectionCode)
        String collectionCode = inReturnBatch.getCollectionCode();
        checkNull(collectionCode, "collection code");
        
        Integer collectionMemberId = getCollectionId(collectionCode);
        loanReturnPreparation.setCollectionMemberId(collectionMemberId);
        
        // LoanPreparation
        Integer transactionId = inReturnBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        LoanPreparation loanPreparation = lookupTaxonBatch(transactionId);
        loanReturnPreparation.setLoanPreparation(loanPreparation);
        
        // QuantityResolved/QuantityReturned (itemCount)
        Integer itemCount = inReturnBatch.getItemCount();
        loanReturnPreparation.setQuantityResolved(itemCount);
        loanReturnPreparation.setQuantityReturned(itemCount);
        
        // Remarks (transferredTo)
        String transferredTo = inReturnBatch.getTransferredTo();
        loanReturnPreparation.setRemarks(transferredTo);
        
        // ReturnedDate
        Date actionDate = inReturnBatch.getActionDate();
        if (actionDate != null)
        {
        	Calendar returnedDate = DateUtils.toCalendar(actionDate);
        	loanReturnPreparation.setReturnedDate(returnedDate);
        }
        
        return loanReturnPreparation;
    }
	
    private LoanPreparation lookupTaxonBatch(Integer transactionId) throws LocalException
    {
    	return taxonBatchLookup.getLoanPreparation(transactionId);
    }

    private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
    {
        String fieldNames = "CollectionMemberID, LoanPreparationID, QuantityResolved, " +
        		            "QuantityReturned, ReturnedDate, TimestampCreated";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( loanReturnPreparation.getCollectionMemberId());
        values[1] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
        values[2] = SqlUtils.sqlString( loanReturnPreparation.getQuantityResolved());
        values[3] = SqlUtils.sqlString( loanReturnPreparation.getQuantityReturned());
        values[4] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
        values[5] = SqlUtils.now();
        
        return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
    }
}
