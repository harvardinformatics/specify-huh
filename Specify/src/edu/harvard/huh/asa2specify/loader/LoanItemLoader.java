package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import edu.harvard.huh.asa.LoanItem;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.IdNotFoundException;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.harvard.huh.asa2specify.lookup.PreparationLookup;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Preparation;

// Run this class after SpecimenItemLoader and TransactionLoader
public class LoanItemLoader extends CsvToSqlLoader
{
    private PreparationLookup prepLookup;
    private LoanLookup        loanLookup;
    
    private AsaIdMapper barcodeSpecimenItemId;
    
	public LoanItemLoader(File csvFile,
	                      Statement sqlStatement,
	                      PreparationLookup prepLookup,
	                      LoanLookup loanLookup,
	                      File barcodeSpecimenItemIds) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.prepLookup = prepLookup;
		this.loanLookup = loanLookup;
		this.barcodeSpecimenItemId = new AsaIdMapper(barcodeSpecimenItemIds);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		LoanItem loanItem = parse(columns);
        
        Integer loanItemId = loanItem.getId();
        setCurrentRecordId(loanItemId);
        
        String code = loanItem.getLocalUnit();
        checkNull(code, "local unit");
        
        Integer collectionMemberId = getCollectionId(code);

		LoanPreparation loanPreparation = getLoanPreparation(loanItem, collectionMemberId);
		
		String sql = getInsertSql(loanPreparation);
		Integer loanPreparationId = insert(sql);
		loanPreparation.setLoanPreparationId(loanPreparationId);
		
		if (loanItem.getReturnDate() != null)
		{
		    LoanReturnPreparation loanReturnPreparation =
		        getLoanReturnPreparation(loanItem, loanPreparation, collectionMemberId);

		    loanReturnPreparation.setLoanPreparation(loanPreparation);

		    sql = getInsertSql(loanReturnPreparation);
		    insert(sql);
		}
	}
	
	private Integer lookupBarcode(Integer barcode)
    {
        return barcodeSpecimenItemId.map(barcode);
    }
	
	private String formatBarcode(Integer barcode) throws LocalException
	{
	    return prepLookup.formatPrepBarcode(barcode);
	}

	private Preparation lookupSpecimenItem(String barcode) throws LocalException
	{
	    return prepLookup.getByBarcode(barcode);
	}

	private Preparation lookupSpecimenItem(Integer specimenItemId) throws LocalException
	{
	    return prepLookup.getBySpecimenItemId(specimenItemId);
	}
	
	private Loan lookupLoan(Integer transactionId) throws LocalException
	{
	    return loanLookup.getById(transactionId);
	}
	
	private LoanItem parse(String[] columns) throws LocalException
	{
    	if (columns.length < 8)
    	{
    		throw new LocalException("Not enough columns");
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
    		loanItem.setLocalUnit(                      columns[7] );
    	}
    	catch (NumberFormatException e)
    	{
    		throw new LocalException("Couldn't parse numeric field", e);
    	}
    	
    	return loanItem;
	}

	private LoanPreparation getLoanPreparation(LoanItem loanItem, Integer collectionMemberId) throws LocalException
	{
		LoanPreparation loanPreparation = new LoanPreparation();
		
        // Discipline
        loanPreparation.setDiscipline(getBotanyDiscipline());

		// IsResolved
		boolean isResolved = loanItem.getReturnDate() != null;
		loanPreparation.setIsResolved(isResolved);
		
		// LoanID
		Integer transactionId = loanItem.getLoanId();
		Loan loan = lookupLoan(transactionId);
		loanPreparation.setLoan(loan);

		// PreparationID
		Preparation preparation = null;
		
		Integer barcode = loanItem.getBarcode();
		Integer specimenItemId = lookupBarcode(barcode);
		String catalogNumber = formatBarcode(barcode);

		if (specimenItemId != null)
		{
		    preparation = lookupSpecimenItem(specimenItemId);
		}
		else
		{
		    try
		    {
		        preparation = lookupSpecimenItem(catalogNumber);
		    }
		    catch (IdNotFoundException e)
		    {
		        getLogger().warn(rec() + "Item not found for barcode " + barcode);
		        loanPreparation.setDescriptionOfMaterial("This is a placeholder for the item with barcode " + barcode + 
		                                                 ", which was not found when this record was created.");
		        preparation = new Preparation();
		    }
		}
		
		loanPreparation.setPreparation(preparation);
		
		// Quantity
		loanPreparation.setQuantity(1);
		
		// QuantityResolved (quantity returned)
		loanPreparation.setQuantityResolved(isResolved ? 1 : 0);
		
		// QuantityReturned
		loanPreparation.setQuantityReturned(isResolved ? 1 : 0);

        // ReceivedComments (transferred from, transferred to)
		String receivedComments = loanItem.getReceivedComments();
		loanPreparation.setReceivedComments(receivedComments);

		return loanPreparation;
	}
	
	private LoanReturnPreparation getLoanReturnPreparation(LoanItem loanItem,
	                                                       LoanPreparation loanPreparation,
	                                                       Integer collectionMemberId) throws LocalException
	{		
		LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
		
		// DisciplineID
		loanReturnPreparation.setDiscipline(getBotanyDiscipline());
		
		// LoanPreparation
		loanReturnPreparation.setLoanPreparation(loanPreparation);
		
		// QuantityResolved (quantity returned)
        loanReturnPreparation.setQuantityResolved(1);
        
		// QuantityReturned
		loanReturnPreparation.setQuantityReturned(1);
		
		// ReturnedDate
		Date returnDate = loanItem.getReturnDate();
		loanReturnPreparation.setReturnedDate(DateUtils.toCalendar(returnDate));
		
		return loanReturnPreparation;
	}

	private String getInsertSql(LoanPreparation loanPreparation)
	{
		String fieldNames = "DescriptionOfMaterial, DisciplineID, IsResolved, LoanID, " +
				            "PreparationID, Quantity, QuantityResolved, QuantityReturned, " +
				            "ReceivedComments, TimestampCreated, Version";
		
		String[] values = new String[11];
		
		values[0]  = SqlUtils.sqlString( loanPreparation.getDescriptionOfMaterial());
		values[1]  = SqlUtils.sqlString( loanPreparation.getDiscipline().getId());
		values[2]  = SqlUtils.sqlString( loanPreparation.getIsResolved());
		values[3]  = SqlUtils.sqlString( loanPreparation.getLoan().getId());
		values[4]  = SqlUtils.sqlString( loanPreparation.getPreparation().getId());
		values[5]  = SqlUtils.sqlString( loanPreparation.getQuantity());
		values[6]  = SqlUtils.sqlString( loanPreparation.getQuantityResolved());
		values[7]  = SqlUtils.sqlString( loanPreparation.getQuantityReturned());
		values[8]  = SqlUtils.sqlString( loanPreparation.getReceivedComments());
		values[9]  = SqlUtils.now();
		values[10] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
	}
	
	private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
	{
		String fieldNames = "DisciplineID, LoanPreparationID, QuantityResolved, " +
				            "QuantityReturned, ReturnedDate, TimestampCreated, Version";
		
		String[] values = new String[7];
		
		values[0] = SqlUtils.sqlString( loanReturnPreparation.getDiscipline().getId());
		values[1] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
		values[2] = SqlUtils.sqlString( loanReturnPreparation.getQuantityResolved());
		values[3] = SqlUtils.sqlString( loanReturnPreparation.getQuantityReturned());
		values[4] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
		values[5] = SqlUtils.now();
		values[6] = SqlUtils.zero();
		
		return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
	}
}
