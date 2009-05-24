package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.PURPOSE;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.ExchangeIn;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.Loan;

public class TransactionLoader extends CsvToSqlLoader
{
	private Discipline discipline;
	private Division division;
	
	private static final String DEFAULT_BORROW_NUMBER = "none";
	private static final String DEFAULT_LOAN_NUMBER = "none";
	private static final String DEFAULT_GIFT_NUMBER = "none";
	private static final String BORROWER_ROLE = "Borrower"; // TODO: determine the string we want to use for the asa agent role
	private static final String LENDER_ROLE = "Lender"; // TODO: determine the string we want to use for the asa affiliate role
	private static final String GIFTER_ROLE = "Donor";
	private static final String GIFTEE_ROLE = "Recipient";
	
	public TransactionLoader(File csvFile, Statement sqlStatement) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.discipline = getBotanyDiscipline();
		this.division   = getBotanyDivision();
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
		Transaction transaction = parse(columns);
		
		TYPE type = transaction.getType();
		if (type == null)
		{
			throw new LocalException("No transaction type", transaction.getId());
		}
		
		if (type.equals(TYPE.Loan))
		{
			Loan loan = getLoan(transaction);
			
			String sql = getInsertSql(loan);
			insert(sql);
		}
		else if (type.equals(TYPE.Borrow))
		{
			Borrow borrow = getBorrow(transaction);
			
			String sql = getInsertSql(borrow);
			insert(sql);
		}
		else if (type.equals(TYPE.InExchange))
		{
			ExchangeIn exchangeIn = getExchangeIn(transaction);
			
			String sql = getInsertSql(exchangeIn);
			insert(sql);
		}
		else if (type.equals(TYPE.OutExchange))
		{
			ExchangeOut exchangeOut = getExchangeOut(transaction);
			
			String sql = getInsertSql(exchangeOut);
			insert(sql);
		}
		else if (type.equals(TYPE.InGift))
		{
			Gift gift = getGift(transaction);
			
			String sql = getInsertSql(gift);
			insert(sql);
		}
		else if (type.equals(TYPE.OutGift))
		{
			// TODO: is an outgoing gift the same as a deaccession?
		}
		else if (type.equals(TYPE.InSpecial))
		{
			// TODO: figure out how to deal with special incoming transactions
		}
		else if (type.equals(TYPE.OutSpecial))
		{
			// TODO: figure out how to deal with special outgoing transactions
		}
		else if (type.equals(TYPE.StaffCollection))
		{
			// TODO: is a staff collection the same as an accession?
		}
	}

	private Transaction parse(String[] columns) throws LocalException
	{
    	if (columns.length < 20)
    	{
    		throw new LocalException("Wrong number of columns");
    	}
    	
		Transaction transaction = new Transaction();
		
		try
		{
			transaction.setId(                      Integer.parseInt( StringUtils.trimToNull( columns[0] )));
			transaction.setType(               Transaction.parseType( StringUtils.trimToNull( columns[1] )));
			transaction.setAgentId(                 Integer.parseInt( StringUtils.trimToNull( columns[2] )));
			transaction.setLocalUnit(                                 StringUtils.trimToNull( columns[3] ));
			transaction.setRequestType( Transaction.parseRequestType( StringUtils.trimToNull( columns[4] )));
			transaction.setPurpose(         Transaction.parsePurpose( StringUtils.trimToNull( columns[5] )));
			transaction.setAffiliateId(             Integer.parseInt( StringUtils.trimToNull( columns[6] )));
			transaction.setUserType(       Transaction.parseUserType( StringUtils.trimToNull( columns[7] )));
			transaction.setIsAcknowledged(      Boolean.parseBoolean( StringUtils.trimToNull( columns[8] )));

			String openDateString = StringUtils.trimToNull( columns[9] );
			Date openDate = SqlUtils.parseDate(openDateString);
			transaction.setOpenDate(openDate);
			
			String closeDateString = StringUtils.trimToNull( columns[10] );
			Date closeDate = SqlUtils.parseDate(closeDateString);
			transaction.setCloseDate(closeDate);
			
			transaction.setTransactionNo(                 StringUtils.trimToNull( columns[11] ));
			transaction.setForUseBy(                      StringUtils.trimToNull( columns[12] ));
			transaction.setBoxCount(                      StringUtils.trimToNull( columns[13] ));
			transaction.setDescription(                   StringUtils.trimToNull( columns[14] ));
			transaction.setRemarks(                       StringUtils.trimToNull( columns[15] ));
			transaction.setCreatedById( Integer.parseInt( StringUtils.trimToNull( columns[16] )));

			String createDateString = StringUtils.trimToNull( columns[17] );
			Date createDate = SqlUtils.parseDate(createDateString);
			transaction.setDateCreated(createDate);
			
			String originalDueDateString = StringUtils.trimToNull( columns[18] );
			Date originalDueDate = SqlUtils.parseDate(originalDueDateString);
			transaction.setOriginalDueDate(originalDueDate);
			
			String currentDueDateString = StringUtils.trimToNull( columns[19] );
			Date currentDueDate = SqlUtils.parseDate(currentDueDateString);
			transaction.setCurrentDueDate(currentDueDate);
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		catch (NullPointerException e)
		{
			throw new LocalException("Missing required field", e);
		}
		
		return transaction;
	}
	
	private Loan getLoan(Transaction transaction) throws LocalException
	{
		Loan loan = new Loan();
		
		// Number1 (id) TODO: temporary!! remove when done!
		Integer transactionId = transaction.getId();
		if (transactionId == null)
		{
			throw new LocalException("No transaction id");
		}
		loan.setNumber1((float) transactionId);
		
		// TimestampCreated
		Date dateCreated = transaction.getDateCreated();
        loan.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		// CurrentDueDate
		Date currentDueDate = transaction.getCurrentDueDate();
		if (currentDueDate != null)
		{
			loan.setCurrentDueDate(DateUtils.toCalendar(currentDueDate));
		}
		
		// DateClosed
		Date closeDate = transaction.getCloseDate();
		if (closeDate != null)
		{
			loan.setDateClosed(DateUtils.toCalendar(closeDate));
		}

		// DateReceived

		// IsClosed
		loan.setIsClosed(closeDate != null);
		
		// LoanDate
		Date openDate = transaction.getOpenDate();
		if (openDate != null)
		{
			loan.setLoanDate(DateUtils.toCalendar(openDate));
		}
		
		// LoanNumber
		String transactionNo = transaction.getTransactionNo();
		if ( transactionNo == null)
		{
			transactionNo = DEFAULT_LOAN_NUMBER;
		}
		if (transactionNo.length() > 50)
		{
			warn("Truncating loan number", transaction.getId(), transactionNo);
			transactionNo = transactionNo.substring(0, 50);
		}
		loan.setLoanNumber(transactionNo);
		
		// OriginalDueDate
		Date originalDueDate = transaction.getOriginalDueDate();
		if (originalDueDate != null)
		{
			loan.setOriginalDueDate(DateUtils.toCalendar(originalDueDate));
		}
		
		// PurposeOfLoan
		PURPOSE purpose = transaction.getPurpose();
		String purposeOfLoan = Transaction.toString(purpose);
		loan.setPurposeOfLoan(purposeOfLoan);
		
		// ReceivedComments
		
		// Remarks
		String remarks = transaction.getRemarks();
		loan.setRemarks(remarks);
		
		// SpecialConditions
		
		// SrcGeography
		
		// SrcTaxonomy
		
		// Text1 (description)
		String description = transaction.getDescription();
		loan.setText1(description);
		
		// Text2
		String forUseBy = transaction.getForUseBy();
		loan.setText2(forUseBy);
		
		// YesNo1 (isAcknowledged)
		Boolean isAcknowledged = transaction.isAcknowledged();
		loan.setYesNo1(isAcknowledged);
		
		// DisciplineID
		loan.setDiscipline(discipline);
		
		// CreatedByAgentID
		Integer creatorOptrId = transaction.getCreatedById();
        Agent  createdByAgent = getAgentByOptrId(creatorOptrId);
        loan.setCreatedByAgent(createdByAgent);
        
		// DivisionID
		loan.setDivision(division);
		
		// AddressOfRecordID
		
		return loan;
	}
	
	private Borrow getBorrow(Transaction transaction)
	{
		Borrow borrow = new Borrow();
		// TODO: implement
		return borrow;
	}
	
	private ExchangeIn getExchangeIn(Transaction transaction)
	{
		ExchangeIn exchangeIn = new ExchangeIn();
		// TODO: implement
		return exchangeIn;
	}
	
	private ExchangeOut getExchangeOut(Transaction transaction)
	{
		ExchangeOut exchangeOut = new ExchangeOut();
		// TODO: implement
		return exchangeOut;
	}
	
	private Gift getGift(Transaction transaction)
	{
		Gift gift = new Gift();
		// TODO: implement
		return gift;
	}
	
	private String getInsertSql(Loan loan)
	{
		String fieldNames = "CreatedByAgentID, CurrentDueDate, DateClosed, DisciplineId, DivisionId, " +
				            "IsClosed, LoanDate, LoanNumber, Number1, OriginalDueDate, PurposeOfLoan, " +
				            "Remarks, Text1, Text2, TimestampCreated, YesNo1";
		
		String[] values = new String[16];
		
		values[0]  = SqlUtils.sqlString( loan.getCreatedByAgent().getId());
		values[1]  = SqlUtils.sqlString( loan.getCurrentDueDate());
		values[2]  = SqlUtils.sqlString( loan.getDateClosed());
		values[3]  = SqlUtils.sqlString( loan.getDiscipline().getId());
		values[4]  = SqlUtils.sqlString( loan.getDivision().getId());
		values[5]  = SqlUtils.sqlString( loan.getIsClosed());
		values[6]  = SqlUtils.sqlString( loan.getLoanDate());
		values[7]  = SqlUtils.sqlString( loan.getLoanNumber());
		values[8]  = SqlUtils.sqlString( loan.getNumber1());
		values[9]  = SqlUtils.sqlString( loan.getOriginalDueDate());
		values[10] = SqlUtils.sqlString( loan.getPurposeOfLoan());
		values[11] = SqlUtils.sqlString( loan.getRemarks());
		values[12] = SqlUtils.sqlString( loan.getText1());
		values[13] = SqlUtils.sqlString( loan.getText2());
		values[14] = SqlUtils.sqlString( loan.getTimestampCreated());
		values[15] = SqlUtils.sqlString( loan.getYesNo1());
		
		return SqlUtils.getInsertSql("loan", fieldNames, values);
	}
	
	private String getInsertSql(Borrow borrow)
	{
		// TODO: implement
		String fieldNames = "";
		
		String[] values = new String[0];
		
		return SqlUtils.getInsertSql("borrow", fieldNames, values);
	}
	
	private String getInsertSql(ExchangeIn exchangeIn)
	{
		// TODO: implement
		String fieldNames = "";
		
		String[] values = new String[0];
		
		return SqlUtils.getInsertSql("exchangein", fieldNames, values);
	}
	
	private String getInsertSql(ExchangeOut exchangeOut)
	{
		// TODO: implement
		String fieldNames = "";
		
		String[] values = new String[0];
		
		return SqlUtils.getInsertSql("exchangeout", fieldNames, values);
	}
	
	private String getInsertSql(Gift gift)
	{
		// TODO: implement
		String fieldNames = "";
		
		String[] values = new String[0];
		
		return SqlUtils.getInsertSql("gift", fieldNames, values);
	}
}
