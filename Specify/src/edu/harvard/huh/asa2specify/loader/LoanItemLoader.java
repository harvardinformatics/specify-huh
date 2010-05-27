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
	    return prepLookup.formatCollObjBarcode(barcode);
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
    	if (columns.length < 9)
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
    		loanItem.setType(     Boolean.parseBoolean( columns[8] ));
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
		
		// ItemCount
        loanPreparation.setItemCount(loanItem.isType() ? 0 : 1);
        
		// LoanID
		Integer transactionId = loanItem.getLoanId();
		Loan loan = lookupLoan(transactionId);
		loanPreparation.setLoan(loan);

		// NonSpecimenCount
		loanPreparation.setNonSpecimenCount(0);
		
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

        // ReceivedComments (transferred from, transferred to)
		String receivedComments = loanItem.getReceivedComments();
		loanPreparation.setReceivedComments(receivedComments);

		// TypeCount
		loanPreparation.setTypeCount(loanItem.isType() ? 1 : 0);
		
		return loanPreparation;
	}
	
	private LoanReturnPreparation getLoanReturnPreparation(LoanItem loanItem,
	                                                       LoanPreparation loanPreparation,
	                                                       Integer collectionMemberId) throws LocalException
	{		
		LoanReturnPreparation loanReturnPreparation = new LoanReturnPreparation();
		
		// DisciplineID
		loanReturnPreparation.setDiscipline(getBotanyDiscipline());
		
		// ItemCount
        loanReturnPreparation.setItemCount(loanItem.isType() ? 0 : 1);
        
		// LoanPreparation
		loanReturnPreparation.setLoanPreparation(loanPreparation);
		
		// NonSpecimenCount
		loanReturnPreparation.setNonSpecimenCount(0);
		
		// ReturnedDate
		Date returnDate = loanItem.getReturnDate();
		loanReturnPreparation.setReturnedDate(DateUtils.toCalendar(returnDate));
		
		// TypeCount
        loanReturnPreparation.setTypeCount(loanItem.isType() ? 1 : 0);
        
		return loanReturnPreparation;
	}

	private String getInsertSql(LoanPreparation loanPreparation)
	{
		String fieldNames = "DescriptionOfMaterial, DisciplineID, IsResolved, ItemCount, LoanID, " +
				            "NonSpecimenCount, PreparationID, ReceivedComments, TimestampCreated, " +
				            "TypeCount, Version";
		
		String[] values = new String[11];
		
		values[0]  = SqlUtils.sqlString( loanPreparation.getDescriptionOfMaterial());
		values[1]  = SqlUtils.sqlString( loanPreparation.getDiscipline().getId());
		values[2]  = SqlUtils.sqlString( loanPreparation.getIsResolved());
	    values[3]  = SqlUtils.sqlString( loanPreparation.getItemCount());
		values[4]  = SqlUtils.sqlString( loanPreparation.getLoan().getId());
		values[5]  = SqlUtils.sqlString( loanPreparation.getNonSpecimenCount());
		values[6]  = SqlUtils.sqlString( loanPreparation.getPreparation().getId());
		values[7]  = SqlUtils.sqlString( loanPreparation.getReceivedComments());
		values[8]  = SqlUtils.now();
		values[9]  = SqlUtils.sqlString( loanPreparation.getTypeCount());
		values[10] = SqlUtils.one();
		
		return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
	}
	
	private String getInsertSql(LoanReturnPreparation loanReturnPreparation)
	{
		String fieldNames = "DisciplineID, ItemCount, LoanPreparationID, NonSpecimenCount, " +
				            "ReturnedDate, TimestampCreated, TypeCount, Version";
		
		String[] values = new String[8];
		
		values[0] = SqlUtils.sqlString( loanReturnPreparation.getDiscipline().getId());
		values[1] = SqlUtils.sqlString( loanReturnPreparation.getItemCount());
		values[2] = SqlUtils.sqlString( loanReturnPreparation.getLoanPreparation().getId());
		values[3] = SqlUtils.sqlString( loanReturnPreparation.getNonSpecimenCount());
		values[4] = SqlUtils.sqlString( loanReturnPreparation.getReturnedDate());
		values[5] = SqlUtils.now();
		values[6] = SqlUtils.sqlString( loanReturnPreparation.getTypeCount());
		values[7] = SqlUtils.one();
		
		return SqlUtils.getInsertSql("loanreturnpreparation", fieldNames, values);
	}
}
