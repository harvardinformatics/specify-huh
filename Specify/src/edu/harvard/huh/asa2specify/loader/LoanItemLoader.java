package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import edu.harvard.huh.asa.LoanItem;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.harvard.huh.asa2specify.lookup.TransactionLookup;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Preparation;

// Run this class after SpecimenItemLoader and TransactionLoader
public class LoanItemLoader extends CsvToSqlLoader
{
    private PreparationLookup prepLookup;
    private TransactionLookup loanLookup;
    
	public LoanItemLoader(File csvFile,
	                      Statement sqlStatement,
	                      PreparationLookup prepLookup,
	                      TransactionLookup loanLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.prepLookup = prepLookup;
		this.loanLookup = loanLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		LoanItem loanItem = parse(columns);
		
		Integer loanItemId = loanItem.getId();
		setCurrentRecordId(loanItemId);
		
		LoanPreparation loanPreparation = getLoanPreparation(loanItem);
		
		String sql = getInsertSql(loanPreparation);
		Integer loanPreparationId = insert(sql);
		loanPreparation.setLoanPreparationId(loanPreparationId);
		
		LoanReturnPreparation loanReturnPreparation = getLoanReturnPreparation(loanItem, loanPreparation);
		if (loanReturnPreparation != null)
		{
			loanReturnPreparation.setCollectionMemberId(loanPreparation.getCollectionMemberId());
			loanReturnPreparation.setLoanPreparation(loanPreparation);
			
			sql = getInsertSql(loanReturnPreparation);
			insert(sql);
		}
	}

	private String formatBarcode(Integer barcode) throws LocalException
	{
	    return prepLookup.formatBarcode(barcode);
	}

	private Preparation lookupSpecimenItem(String barcode) throws LocalException
	{
	    return prepLookup.getByBarcode(barcode);
	}

	private Loan lookupLoan(Integer transactionId) throws LocalException
	{
	    return loanLookup.getLoan(transactionId);
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
    		loanItem.setId(          SqlUtils.parseInt( columns[0] ));
    		loanItem.setLoanId(      SqlUtils.parseInt( columns[1] ));
    		loanItem.setReturnDate( SqlUtils.parseDate( columns[2] ));
    		loanItem.setBarcode(     SqlUtils.parseInt( columns[3] ));
    		loanItem.setTransferredFrom(                columns[4] );
    		loanItem.setTransferredTo(                  columns[5] );
    		loanItem.setCollection(                     columns[6] );
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
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
		boolean isResolved = loanItem.getReturnDate() != null;
		loanPreparation.setIsResolved(isResolved);
		
		// LoanID
		Integer transactionId = loanItem.getLoanId();
		Loan loan = lookupLoan(transactionId);
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
	    Preparation preparation = lookupSpecimenItem(barcode);
		
		loanPreparation.setPreparation(preparation);
		
		// Quantity
		loanPreparation.setQuantity(1);
		
		// QuantityResolved/QuantityReturned
		int q = isResolved? 1 : 0;

		loanPreparation.setQuantityResolved(q);
		loanPreparation.setQuantityReturned(q);

		return loanPreparation;
	}
	
	private LoanReturnPreparation getLoanReturnPreparation(LoanItem loanItem,
	                                                       LoanPreparation loanPreparation) throws LocalException
	{
		if (loanItem.getReturnDate() == null) return null;
		
		LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
		
		// CollectionMemberID
		String collectionCode = loanItem.getCollection();
		Integer collectionMemberId = getCollectionId(collectionCode);
		loanReturnPreparation.setCollectionMemberId(collectionMemberId);

		// LoanPreparation
		loanReturnPreparation.setLoanPreparation(loanPreparation);
		
		// QuantityResolved/QuantityReturned TODO: resolved?  returned?
		int q = 1;
		loanReturnPreparation.setQuantityResolved(q);
		loanReturnPreparation.setQuantityReturned(q);
		
		// ReturnedDate
		Date returnDate = loanItem.getReturnDate();
		loanReturnPreparation.setReturnedDate(DateUtils.toCalendar(returnDate));
		
		return loanReturnPreparation;
	}

	private String getInsertSql(LoanPreparation loanPreparation)
	{
		String fieldNames = "CollectionMemberID, IsResolved, LoanID, OutComments, PreparationID, " +
				            "Quantity, QuantityResolved, QuantityReturned, TimestampCreated";
		
		String[] values = new String[9];
		
		values[0] = SqlUtils.sqlString( loanPreparation.getCollectionMemberId());
		values[1] = SqlUtils.sqlString( loanPreparation.getIsResolved());
		values[2] = SqlUtils.sqlString( loanPreparation.getLoan().getId());
		values[3] = SqlUtils.sqlString( loanPreparation.getOutComments());
		values[4] = SqlUtils.sqlString( loanPreparation.getPreparation().getId());
		values[5] = SqlUtils.sqlString( loanPreparation.getQuantity());
		values[6] = SqlUtils.sqlString( loanPreparation.getQuantityResolved());
		values[7] = SqlUtils.sqlString( loanPreparation.getQuantityReturned());
		values[8] = SqlUtils.now();

		return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
	}
	
	private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
	{
		String fieldNames = "CollectionMemberID, LoanPreparationID, QuantityResolved, " +
				            "QuantityReturned, ReturnedDate, TimestampCreated";
		
		String[] values = new String[6];
		
		values[0] = SqlUtils.sqlString( loanReturnPreparation.getCollectionMemberId());
		values[1] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
		values[2] = SqlUtils.sqlString( loanReturnPreparation.getQuantityResolved());
		values[3] = SqlUtils.sqlString( loanReturnPreparation.getQuantityResolved());
		values[4] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
		values[5] = SqlUtils.now();
		
		return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
	}
}
