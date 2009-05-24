package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.LoanItem;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Preparation;

public class LoanItemLoader extends CsvToSqlLoader
{
	public LoanItemLoader(File csvFile, Statement sqlStatement)
	{
		super(csvFile, sqlStatement);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		LoanItem loanItem = parse(columns);
		
		LoanPreparation loanPreparation = getLoanPreparation(loanItem);
		
		String sql = getInsertSql(loanPreparation);
		Integer loanPreparationId = insert(sql);
		loanPreparation.setLoanPreparationId(loanPreparationId);
		
		LoanReturnPreparation loanReturnPreparation = getLoanReturnPreparation(loanItem);
		if (loanReturnPreparation != null)
		{
			loanReturnPreparation.setCollectionMemberId(loanPreparation.getCollectionMemberId());
			loanReturnPreparation.setLoanPreparation(loanPreparation);
			
			sql = getInsertSql(loanReturnPreparation);
			insert(sql);
		}
	}

	private LoanItem parse(String[] columns) throws LocalException
	{
    	if (columns.length < 7)
    	{
    		throw new LocalException("Wrong number of columns");
    	}

    	LoanItem loanItem = new LoanItem();
    	
    	try
    	{
    		loanItem.setId( Integer.parseInt(           StringUtils.trimToNull( columns[0] )));
    		loanItem.setLoanId( Integer.parseInt(       StringUtils.trimToNull( columns[1] )));
    		loanItem.setReturnDate( SqlUtils.parseDate( StringUtils.trimToNull( columns[2] )));
    		loanItem.setBarcode(                        StringUtils.trimToNull( columns[3] ));
    		loanItem.setTransferredFrom(                StringUtils.trimToNull( columns[4] ));
    		loanItem.setTransferredTo(                  StringUtils.trimToNull( columns[5] ));
    		loanItem.setCollection(                     StringUtils.trimToNull( columns[6] ));
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	catch (NullPointerException e)
    	{
    		throw new LocalException("Missing required field", e);
    	}
    	
    	return loanItem;
	}

	private LoanPreparation getLoanPreparation(LoanItem loanItem) throws LocalException
	{
		LoanPreparation loanPreparation = new LoanPreparation();
		
		// CollectionMemberID
		String code = loanItem.getCollection();
		Integer collectionMemberId = getCollectionId(code);
		loanPreparation.setCollectionMemberId(collectionMemberId);
		
		// IsResolved
		loanPreparation.setIsResolved(loanItem.getReturnDate() != null);
		
		// LoanID
		Integer transactionId = loanItem.getLoanId();
		Integer loanId = getIntByField("loan", "LoanID", "Number1", transactionId);
		
		Loan loan = new Loan();
		loan.setLoanId(loanId);
		
		loanPreparation.setLoan(loan);
		
		// OutComments
		StringBuffer outComments = new StringBuffer();
		
		String transferredFrom = loanItem.getTransferredFrom();
		if (transferredFrom != null) 
		{
			outComments.append("Transferred from: ");
			outComments.append(transferredFrom);
		}
		
		String transferredTo = loanItem.getTransferredTo();
		if (transferredTo != null)
		{
			if (outComments.length() > 0) outComments.append("; ");
			outComments.append("Transferred to: ");
			outComments.append(transferredTo);
		}
		
		loanPreparation.setOutComments(outComments.toString());
		
		// PreparationID
		String barcode = formatBarcode(loanItem.getBarcode());
		Integer preparationId = getIntByField("preparation", "PreparationID", "SampleNumber", barcode);
		
		Preparation preparation = new Preparation();
		preparation.setPreparationId(preparationId);
		
		loanPreparation.setPreparation(preparation);
		
		return loanPreparation;
	}
	
	private LoanReturnPreparation getLoanReturnPreparation(LoanItem loanItem)
	{
		if (loanItem.getReturnDate() == null) return null;
		
		LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
		
		// Quantity TODO: resolved?  returned?
		int quantityReturned = 1;
		loanReturnPreparation.setQuantityReturned(quantityReturned);
		
		// ReturnedDate
		Date returnDate = loanItem.getReturnDate();
		loanReturnPreparation.setReturnedDate(DateUtils.toCalendar(returnDate));
		
		return loanReturnPreparation;
	}

	private String getInsertSql(LoanPreparation loanPreparation)
	{
		String fieldNames = "CollectionMemberID, IsResolved, LoanID, OutComments, PreparationID, " +
				            "TimestampCreated";
		
		String[] values = new String[6];
		
		values[0] = SqlUtils.sqlString( loanPreparation.getCollectionMemberId());
		values[1] = SqlUtils.sqlString( loanPreparation.getIsResolved());
		values[2] = SqlUtils.sqlString( loanPreparation.getLoan().getId());
		values[3] = SqlUtils.sqlString( loanPreparation.getOutComments());
		values[4] = SqlUtils.sqlString( loanPreparation.getPreparation().getId());
		values[5] = SqlUtils.now();

		return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
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
