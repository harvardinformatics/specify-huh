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
        
        // Remarks
        
        // ... boxCount
        String boxCount = inReturnBatch.getBoxCount();
        
        // ... item count
        int itemCount = inReturnBatch.getItemCount();
        String items = denormalize("items", String.valueOf(itemCount));
        
        // ... type count
        int typeCount = inReturnBatch.getTypeCount();
        String types = denormalize("types", String.valueOf(typeCount));
        
        // ... non-specimen count
        int nonSpecimenCount = inReturnBatch.getNonSpecimenCount();
        String nonSpecimens = denormalize("non-specimens", String.valueOf(nonSpecimenCount));
        
        // ... acknowledged?
        String isAcknowledged = denormalize("acknowledged?", inReturnBatch.isAcknowledged() ? "yes" : "no");
        loanReturnPreparation.setRemarks(concatenate(boxCount, items, types, nonSpecimens, isAcknowledged));
        
        // QuantityReturned
        loanReturnPreparation.setQuantityReturned(itemCount + typeCount + nonSpecimenCount);

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
        String fieldNames = "DisciplineID, LoanPreparationID, QuantityReturned, " +
        		            "Remarks, ReturnedDate, TimestampCreated, Version";
        
        String[] values = {
        		SqlUtils.sqlString( loanReturnPreparation.getDiscipline().getId()),
        		SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId()),
        		SqlUtils.sqlString( loanReturnPreparation.getQuantityReturned()),
        		SqlUtils.sqlString( loanReturnPreparation.getRemarks()),
        		SqlUtils.sqlString( loanReturnPreparation.getReturnedDate()),
        		SqlUtils.now(),
        		SqlUtils.one()
        };
        
        return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
    }
    
    private String getUpdateSql(LoanPreparation loanPrep,  String transferredTo) throws LocalException
    {
        String[] fieldNames = { "InComments" };

        String[] values = { SqlUtils.sqlString( transferredTo) };
        
        return SqlUtils.getUpdateSql("loanpreparation", fieldNames, values, "LoanPreparationID", SqlUtils.sqlString(loanPrep.getId()));
    }
}
