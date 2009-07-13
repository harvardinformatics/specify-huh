package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.TaxonBatch;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BorrowLookup;
import edu.harvard.huh.asa2specify.lookup.TaxonBatchLookup;
import edu.harvard.huh.asa2specify.lookup.LoanLookup;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowMaterial;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;

public class TaxonBatchLoader extends CsvToSqlLoader
{
    private static final Logger log  = Logger.getLogger(TaxonBatch.class);
    
    private TaxonBatchLookup taxonBatchLookup;
    
    private BorrowLookup borrowLookup;
    private LoanLookup loanLookup;

    public TaxonBatchLoader(File csvFile,
	                        Statement sqlStatement,
	                        BorrowLookup borrowLookup,
	                        LoanLookup loanLookup) throws LocalException
	{
		super(csvFile, sqlStatement);

		this.borrowLookup = borrowLookup;
		this.loanLookup = loanLookup;
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
	    TaxonBatch taxonBatch = parse(columns);
	    
	    Integer taxonBatchId = taxonBatch.getId();
	    setCurrentRecordId(taxonBatchId);
	    
	    TYPE type = taxonBatch.getType();
	    checkNull(type, "transaction type");

	    if (type.equals(TYPE.Borrow))
	    {
	        BorrowMaterial borrowMaterial = getBorrowMaterial(taxonBatch);
	        String sql = getInsertSql(borrowMaterial);
	        insert(sql);
	    }
	    else if (type.equals(TYPE.Loan))
	    {
	        LoanPreparation loanPreparation = getLoanPreparation(taxonBatch);
	        String sql = getInsertSql(loanPreparation);
	        insert(sql);
	    }
	    else
	    {
	        throw new LocalException("Invalid type for taxon batch: " + type.name());
	    }
	}

	public Logger getLogger()
    {
        return log;
    }
	
	public TaxonBatchLookup getTaxonBatchLookup()
	{
	    if (taxonBatchLookup == null)
	    {
	        taxonBatchLookup = new TaxonBatchLookup() {
	            
	            public LoanPreparation getLoanPreparation(Integer transactionId) throws LocalException
	            {
	                LoanPreparation loanPreparation = new LoanPreparation();
	                
	                String inComments = String.valueOf(transactionId);
	                
	                Integer loanPreparationId = getInt("loanpreparation", "LoanID", "InComments", inComments);
	                
	                loanPreparation.setLoanPreparationId(loanPreparationId);
	                
	                return loanPreparation;
	            }
	            
	            public BorrowMaterial getBorrowMaterial(Integer transactionId) throws LocalException
	            {
	            	BorrowMaterial borrowMaterial = new BorrowMaterial();
	            	
	            	String materialNumber = String.valueOf(transactionId);
	            	
	            	Integer borrowMaterialId = getInt("borrowmaterial", "BorrowMaterialID", "MaterialNumber", materialNumber);
	            	
	            	borrowMaterial.setBorrowMaterialId(borrowMaterialId);
	            	
	            	return borrowMaterial;
	            }
	        };
	    }
	    
	    return taxonBatchLookup;
	}
	
	private TaxonBatch parse(String[] columns) throws LocalException
	{
		if (columns.length < 11)
		{
			throw new LocalException("Not enough columns");
		}
		
		TaxonBatch taxonBatch = new TaxonBatch();
		try
		{
			taxonBatch.setId(               SqlUtils.parseInt( columns[0]  ));
			taxonBatch.setTransactionId(    SqlUtils.parseInt( columns[1]  ));
			taxonBatch.setCollectionCode(                      columns[2]  );
			taxonBatch.setType(         Transaction.parseType( columns[3]  ));
			taxonBatch.setHigherTaxon(                         columns[4]  );
			taxonBatch.setItemCount(        SqlUtils.parseInt( columns[5]  ));
			taxonBatch.setTypeCount(        SqlUtils.parseInt( columns[6]  ));
			taxonBatch.setNonSpecimenCount( SqlUtils.parseInt( columns[7]  ));
			taxonBatch.setTaxon(                               columns[8]  );
			taxonBatch.setTransferredFrom(                     columns[9]  );
			taxonBatch.setQtyReturned(      SqlUtils.parseInt( columns[10] ));
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return taxonBatch;
	}
	
	private BorrowMaterial getBorrowMaterial(TaxonBatch taxonBatch) throws LocalException
	{
	    BorrowMaterial borrowMaterial = new BorrowMaterial();
	    
	    // Borrow
	    Integer transactionId = taxonBatch.getTransactionId();
	    checkNull(transactionId, "transaction id");
	    
	    Borrow borrow = lookupBorrow(transactionId);
	    borrowMaterial.setBorrow(borrow);
	    
	    // CollectionMemberID (collectionCode)
	    String collectionCode = taxonBatch.getCollectionCode();
	    checkNull(collectionCode, "collection code");
	    
	    Integer collectionMemberId = getCollectionId(collectionCode);
	    borrowMaterial.setCollectionMemberId(collectionMemberId);
	    
	    // Description (higherTaxon, taxon, itemCount, typeCount, nonSpecimenCount)
	    String description = getDescription(taxonBatch);
	    if (description != null)
	    {
	    	description = truncate(description, 50, "description");
		    borrowMaterial.setDescription(description);
	    }
   
        // InComments (transferredFrom)
        String transferredFrom = taxonBatch.getTransferredFrom();
        borrowMaterial.setInComments(transferredFrom);
        
	    // MaterialNumber (transactionId)
	    String materialNumber = String.valueOf(transactionId);
	    checkNull(materialNumber, "transaction id");
	    
	    borrowMaterial.setMaterialNumber(materialNumber);
	    
	    // Quantity (itemCount + typeCount + nonSpecimenCount)
	    short quantity = getQuantity(taxonBatch);
	    borrowMaterial.setQuantity(quantity);
	    
	    return borrowMaterial;
	}

	private LoanPreparation getLoanPreparation(TaxonBatch taxonBatch) throws LocalException
	{
        LoanPreparation loanPreparation = new LoanPreparation();
        
        // CollectionMemberID
        String code = taxonBatch.getCollectionCode();
        Integer collectionMemberId = getCollectionId(code);
        loanPreparation.setCollectionMemberId(collectionMemberId);
                
        // DescriptionOfMaterial
        String description = getDescription(taxonBatch);
        description = truncate(description, 255, "description");
        loanPreparation.setDescriptionOfMaterial(description);
        
        // IsResolved
        Boolean isResolved = isResolved(taxonBatch);        
        loanPreparation.setIsResolved(isResolved);
        
        // InComments (transactionId) TODO: temporary, remove this field!
        Integer transactionId = taxonBatch.getTransactionId();
        checkNull(transactionId, "transaction id");
        
        String inComments = String.valueOf(transactionId);
        loanPreparation.setInComments(inComments);
        
        // LoanID
        Loan loan = lookupLoan(transactionId);
        loanPreparation.setLoan(loan);
        
        // OutComments (transferredFrom)
        StringBuffer outComments = new StringBuffer();
        
        String transferredFrom = taxonBatch.getTransferredFrom();
        if (transferredFrom != null) 
        {
            outComments.append("Transferred from: ");
            outComments.append(transferredFrom);
        }        
        loanPreparation.setOutComments(outComments.toString());
        
        // Quantity (itemCount + typeCount + nonSpecimenCount)
        int quantity = getQuantity(taxonBatch);
        loanPreparation.setQuantity(quantity);
        
        // QuantityResolved/QuantityReturned
        int quantityReturned = deNullify(taxonBatch.getQtyReturned());
        
        loanPreparation.setQuantityResolved(quantityReturned);
        loanPreparation.setQuantityReturned(quantityReturned);

        return loanPreparation;
	}

	private String getDescription(TaxonBatch taxonBatch)
	{
	    String higherTaxon = taxonBatch.getHigherTaxon();
	    String taxon = taxonBatch.getTaxon();

	    if (higherTaxon == null)
	    {
	        if (taxon == null)
	        {
	            taxon = "";
	        }
	        else
	        {
	            taxon = taxon + ": ";
	        }
	    }
	    else
	    {
	        if (taxon == null)
	        {
	            taxon = higherTaxon + ": ";
	        }
	        else
	        {
	            taxon = higherTaxon + " " + taxon + ": ";
	        }
	    }
	        
	    Integer itemCount = taxonBatch.getItemCount();
	    Integer typeCount = taxonBatch.getTypeCount();
	    Integer nonSpecimenCount = taxonBatch.getNonSpecimenCount();

	    Object[] args = {taxon, itemCount, typeCount, nonSpecimenCount };
	    String pattern = "{0}{1} items, {2} types, {3} non-specimens";
	    
	    return MessageFormat.format(pattern, args);
	}

	private int deNullify(Integer i)
	{
	    return i == null ? 0 : i.intValue();
	}

	private short getQuantity(TaxonBatch taxonBatch)
	{
	    Integer itemCount = taxonBatch.getItemCount() ;
	    Integer typeCount = taxonBatch.getTypeCount();
	    Integer nonSpecimenCount = taxonBatch.getNonSpecimenCount();

	    return (short) (itemCount + typeCount + nonSpecimenCount);
	}

	private boolean isResolved(TaxonBatch taxonBatch)
	{
	    return getQuantity(taxonBatch) <= taxonBatch.getQtyReturned();
	}
	
	private Borrow lookupBorrow(Integer transactionId) throws LocalException
	{
	    return borrowLookup.getById(transactionId);
	}
	
	private Loan lookupLoan(Integer transactionId) throws LocalException
	{
	    return loanLookup.getById(transactionId);
	}
	
	private String getInsertSql(BorrowMaterial borrowMaterial)
	{
	    String fields = "BorrowID, CollectionMemberID, Description, InComments, " +
	    		        "MaterialNumber, Quantity, TimestampCreated, Version";
	        
	    String[] values = new String[8];
	    
	    values[0] = SqlUtils.sqlString( borrowMaterial.getBorrow().getId());
	    values[1] = SqlUtils.sqlString( borrowMaterial.getCollectionMemberId());
	    values[2] = SqlUtils.sqlString( borrowMaterial.getDescription());
	    values[3] = SqlUtils.sqlString( borrowMaterial.getInComments());
	    values[4] = SqlUtils.sqlString( borrowMaterial.getMaterialNumber());
	    values[5] = SqlUtils.sqlString( borrowMaterial.getQuantity());
	    values[6] = SqlUtils.now();
	    values[7] = SqlUtils.one();
	    
	    return SqlUtils.getInsertSql("borrowmaterial", fields, values);
	}
	
    private String getInsertSql(LoanPreparation loanPreparation)
    {
        String fieldNames = "CollectionMemberID, DescriptionOfMaterial, InComments, IsResolved, " +
        		            "LoanID, OutComments, Quantity, QuantityResolved, QuantityReturned, " +
        		            "TimestampCreated, Version";
        
        String[] values = new String[11];
        
        values[0]  = SqlUtils.sqlString( loanPreparation.getCollectionMemberId());
        values[1]  = SqlUtils.sqlString( loanPreparation.getDescriptionOfMaterial());
        values[2]  = SqlUtils.sqlString( loanPreparation.getInComments());
        values[3]  = SqlUtils.sqlString( loanPreparation.getIsResolved());
        values[4]  = SqlUtils.sqlString( loanPreparation.getLoan().getId());
        values[5]  = SqlUtils.sqlString( loanPreparation.getOutComments());
        values[6]  = SqlUtils.sqlString( loanPreparation.getQuantity());
        values[7]  = SqlUtils.sqlString( loanPreparation.getQuantityResolved());
        values[8]  = SqlUtils.sqlString( loanPreparation.getQuantityReturned());
        values[9]  = SqlUtils.now();
        values[10] = SqlUtils.one();
        
        return SqlUtils.getInsertSql("loanpreparation", fieldNames, values);
    }
}