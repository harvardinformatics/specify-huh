package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import edu.harvard.huh.asa.InReturnBatch;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.LoanPreparationLookup;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;

public class InReturnBatchLoader extends ReturnBatchLoader
{ 
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
		
		String transferredTo = inReturnBatch.getReceivedComments();
		if (transferredTo != null)
		{
		    sql = getUpdateSql(loanReturnPreparation.getLoanPreparation(), transferredTo);
		}
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
        
        // Discipline
        loanReturnPreparation.setDiscipline(getBotanyDiscipline());
        
        // LoanPreparation
        Integer transactionId = inReturnBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        LoanPreparation loanPreparation = lookupLoanPrep(transactionId);
        loanReturnPreparation.setLoanPreparation(loanPreparation);
        
        // NonSpecimenCount
        int nonSpecimenCount = inReturnBatch.getNonSpecimenCount();
        loanReturnPreparation.setNonSpecimenCount(nonSpecimenCount);
        
        // QuantityResolved (quantity returned)
        int quantity = inReturnBatch.getBatchQuantity();
        loanReturnPreparation.setQuantityResolved(quantity);
        
        // QuantityReturned
        loanReturnPreparation.setQuantityReturned(quantity);
        
        // Remarks (boxCount)
        String boxCount = inReturnBatch.getBoxCount();
        loanReturnPreparation.setRemarks(boxCount);
        
        // ReturnedDate
        Date actionDate = inReturnBatch.getActionDate();
        if (actionDate != null)
        {
        	Calendar returnedDate = DateUtils.toCalendar(actionDate);
        	loanReturnPreparation.setReturnedDate(returnedDate);
        }
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = inReturnBatch.isAcknowledged();
        loanReturnPreparation.setYesNo1(isAcknowledged);
        
        return loanReturnPreparation;
    }
    
    private LoanPreparation lookupLoanPrep(Integer transactionId) throws LocalException
    {
    	return loanPrepLookup.getLotById(transactionId);
    }

    private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
    {
        String fieldNames = "DisciplineID, LoanPreparationID, NonSpecimenCount, QuantityResolved, " +
        		            "QuantityReturned, ReturnedDate, TimestampCreated, Version, YesNo1";
        
        String[] values = new String[9];
        
        values[0] = SqlUtils.sqlString( loanReturnPreparation.getDiscipline().getId());
        values[1] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
        values[2] = SqlUtils.sqlString( loanReturnPreparation.getNonSpecimenCount());
        values[3] = SqlUtils.sqlString( loanReturnPreparation.getQuantityResolved());
        values[4] = SqlUtils.sqlString( loanReturnPreparation.getQuantityReturned());
        values[5] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
        values[6] = SqlUtils.now();
        values[7] = SqlUtils.zero();
        values[8] = SqlUtils.sqlString( loanReturnPreparation.getYesNo1());
        
        return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
    }
    
    private String getUpdateSql(LoanPreparation loanPrep,  String transferredTo) throws LocalException
    {
        String[] fieldNames = { "InComments" };

        String[] values = new String[1];

        values[0] = SqlUtils.sqlString( transferredTo);
        
        return SqlUtils.getUpdateSql("loanpreparation", fieldNames, values, "LoanPreparationID", SqlUtils.sqlString(loanPrep.getId()));
    }
}
