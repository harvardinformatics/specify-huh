package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.InReturnBatch;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.LoanPreparationLookup;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;

public class InReturnBatchLoader extends ReturnBatchLoader
{
    private static final Logger log  = Logger.getLogger(InReturnBatchLoader.class);
    
	private LoanPreparationLookup loanPrepLookup;
	
	public InReturnBatchLoader(File csvFile,
			                   Statement sqlStatement,
			                   LoanPreparationLookup loanPrepLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.loanPrepLookup = loanPrepLookup;
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

	public Logger getLogger()
	{
	    return log;
	}
	
	private InReturnBatch parse(String[] columns) throws LocalException
	{
        InReturnBatch inReturnBatch = new InReturnBatch();

        int i = super.parse(columns, inReturnBatch);

	    if (columns.length < i + 1)
		{
			throw new LocalException("Not enough columns");
		}

	    inReturnBatch.setTransferredTo( columns[i + 0] );
		
		return inReturnBatch;
	}
	
    private LoanReturnPreparation getLoanReturnPreparation(InReturnBatch inReturnBatch) throws LocalException
    {
    	
        LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
        
        // LoanPreparation
        Integer transactionId = inReturnBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        LoanPreparation loanPreparation = lookupLoanPrep(transactionId);
        loanReturnPreparation.setLoanPreparation(loanPreparation);
        
        // QuantityResolved/QuantityReturned (itemCount)
        int quantity = inReturnBatch.getBatchQuantity();
        loanReturnPreparation.setQuantityResolved(quantity);
        loanReturnPreparation.setQuantityReturned(quantity);
        
        // Remarks (item/type/nonsp counts, boxCount, isAcknowledged, transferredTo)
        String remarks = getRemarks(inReturnBatch);
        String transferNote = inReturnBatch.getTransferNote();
        
        if (remarks != null || transferNote != null)
        {
            if (remarks == null) remarks = transferNote;
            else remarks = remarks + "  " + transferNote;
        }
        loanReturnPreparation.setRemarks(remarks);
        
        // ReturnedDate
        Date actionDate = inReturnBatch.getActionDate();
        if (actionDate != null)
        {
        	Calendar returnedDate = DateUtils.toCalendar(actionDate);
        	loanReturnPreparation.setReturnedDate(returnedDate);
        }
        
        return loanReturnPreparation;
    }
    
    private LoanPreparation lookupLoanPrep(Integer transactionId) throws LocalException
    {
    	return loanPrepLookup.getLotById(transactionId);
    }

    private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
    {
        String fieldNames = "LoanPreparationID, QuantityResolved, " +
        		            "QuantityReturned, ReturnedDate, TimestampCreated, Version";
        
        String[] values = new String[6];
        
        values[0] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
        values[1] = SqlUtils.sqlString( loanReturnPreparation.getQuantityResolved());
        values[2] = SqlUtils.sqlString( loanReturnPreparation.getQuantityReturned());
        values[3] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
        values[4] = SqlUtils.now();
        values[5] = SqlUtils.zero();
        
        return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
    }
}
