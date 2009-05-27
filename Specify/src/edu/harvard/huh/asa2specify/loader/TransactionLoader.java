package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import edu.harvard.huh.asa.AsaException;
import edu.harvard.huh.asa.Transaction;
import edu.harvard.huh.asa.Transaction.PURPOSE;
import edu.harvard.huh.asa.Transaction.TYPE;
import edu.harvard.huh.asa2specify.AsaIdMapper;
import edu.harvard.huh.asa2specify.DateUtils;
import edu.harvard.huh.asa2specify.LocalException;
import edu.harvard.huh.asa2specify.SqlUtils;
import edu.harvard.huh.asa2specify.lookup.BotanistLookup;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionAgent;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Borrow;
import edu.ku.brc.specify.datamodel.BorrowAgent;
import edu.ku.brc.specify.datamodel.Deaccession;
import edu.ku.brc.specify.datamodel.DeaccessionAgent;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.ExchangeIn;
import edu.ku.brc.specify.datamodel.ExchangeOut;
import edu.ku.brc.specify.datamodel.Gift;
import edu.ku.brc.specify.datamodel.GiftAgent;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanAgent;

// Run this class after AffiliateLoader, AgentLoader, and OrganizationLoader
public class TransactionLoader extends AuditedObjectLoader
{
    private static final String DEFAULT_BORROW_NUMBER      = "none";
	private static final String DEFAULT_LOAN_NUMBER        = "none";
	private static final String DEFAULT_GIFT_NUMBER        = "none";
    private static final String DEFAULT_ACCESSION_NUMBER   = "none";
    private static final String DEFAULT_DEACCESSION_NUMBER = "none";
	
	// config/common/picklist
	private enum ROLE { borrower, benefactor, collector, contributor, donor, guest, lender, other, preparer, receiver, reviewer, sponsor, staff, student };
	
	private enum ACCESSION_TYPE { gift, cln, disposal, field_work, lost, other, purchase };

	private Discipline discipline;
	private Division division;
	
	private AsaIdMapper botanistsByAffiliate;
	private AsaIdMapper botanistsByAgent;
	
	private BotanistLookup botanistLookup;
	
	public TransactionLoader(File csvFile,
	                         Statement sqlStatement,
	                         File affiliateBotanists,
	                         File agentBotanists,
	                         BotanistLookup botanistLookup) throws LocalException
	{
		super(csvFile, sqlStatement);
		
		this.discipline = getBotanyDiscipline();
		this.division   = getBotanyDivision();
		
		this.botanistsByAffiliate = new AsaIdMapper(affiliateBotanists);
		this.botanistsByAgent     = new AsaIdMapper(agentBotanists);
		
		this.botanistLookup = botanistLookup;
	}
	
	private Agent lookup(Integer botanistId) throws LocalException
	{
	    return botanistLookup.getByBotanistId(botanistId);
	}

	@Override
	public void loadRecord(String[] columns) throws LocalException
	{
	    //       in_geo_batch     -> in.gift/in.exch/sp.exch/staff.coll/purchase
	    //       out_geo_batch    -> out.gift/out.exch
	    //       in_return_batch  -> loan
	    //       out_return_batch -> borrow
	    //       shipment         -> out.gift/out.misc/out.exch/loan
	    //       loan_item        -> loan
	    //       taxon_batch      -> borrow/loan
	    //       due_date         -> borrow/loan
	    
	    // TODO: go through all the xAgents to determine if the affiliates/asaAgents match their assigned roles.

	    // TODO: go through all Text1 fields to determine if there's a more appropriate field to put description into
	    
	    // TODO: go through all the date fields to determine if open/close dates have been appropriately applied
	    
	    // TODO: get list of conditions to raise warnings and exceptions
	    
	    // TODO: assign user_type
	    
	    // TODO: assign box_count
	    
	    Transaction transaction = parse(columns);
	    
	    Integer transactionId = transaction.getId();
	    setCurrentRecordId(transactionId);
	    
        String code = transaction.getLocalUnit();
        checkNull(code, "local unit");

        Integer collectionMemberId = getCollectionId(code);
        
		TYPE type = transaction.getType();
		if (type == null)
		{
			throw new LocalException("No transaction type", transaction.getId());
		}
		
		if (type.equals(TYPE.Loan))
		{
			Loan loan = getLoan(transaction);
			
			String sql = getInsertSql(loan);
			Integer loanId = insert(sql);
			loan.setLoanId(loanId);
			
			LoanAgent borrower = getLoanAgent(transaction, loan, ROLE.borrower, collectionMemberId); // "contact"
			if (borrower != null)
			{
			    sql = getInsertSql(borrower);
			    insert(sql);
			}
		}
		else if (type.equals(TYPE.Borrow))
		{
			Borrow borrow = getBorrow(transaction, collectionMemberId);
			
			String sql = getInsertSql(borrow);
			Integer borrowId = insert(sql);
			borrow.setBorrowId(borrowId);
			
			BorrowAgent borrower = getBorrowAgent(transaction, borrow, ROLE.borrower, collectionMemberId); // "for use by"
			if (borrower != null)
			{
			    sql = getInsertSql(borrower);
			    insert(sql);
			}
			
			BorrowAgent lender = getBorrowAgent(transaction, borrow, ROLE.lender, collectionMemberId); // "contact"
			if (lender != null)
			{
			    sql = getInsertSql(lender);
			    insert(sql);
			}
		}
		else if (type.equals(TYPE.InExchange) || type.equals(TYPE.InSpecial))
		{
			ExchangeIn exchangeIn = getExchangeIn(transaction);
			
			String sql = getInsertSql(exchangeIn);
			insert(sql);
		}
		else if (type.equals(TYPE.OutExchange) || type.equals(TYPE.OutSpecial))
		{
			ExchangeOut exchangeOut = getExchangeOut(transaction);
			
			String sql = getInsertSql(exchangeOut);
			insert(sql);
		}
		else if (type.equals(TYPE.InGift))
		{
			Gift gift = getGift(transaction);
			
			String sql = getInsertSql(gift);
			Integer giftId = insert(sql);
			gift.setGiftId(giftId);
			
			GiftAgent receiver = getGiftAgent(transaction, gift, ROLE.receiver, collectionMemberId);
			if (receiver != null)
			{
			    sql = getInsertSql(receiver);
			    insert(sql);
			}
			GiftAgent donor = getGiftAgent(transaction, gift, ROLE.donor, collectionMemberId);
			if (donor != null)
			{
			    sql = getInsertSql(donor);
			    insert(sql);
			}
		}
		else if (type.equals(TYPE.OutGift))
		{
			Deaccession deaccession = getDeaccession(transaction, ACCESSION_TYPE.gift);
			
			String sql = getInsertSql(deaccession);
			Integer deaccessionId = insert(sql);
			deaccession.setDeaccessionId(deaccessionId);
			
			DeaccessionAgent donor = getDeaccessionAgent(transaction, deaccession, ROLE.donor);
			if (donor != null)
			{
			    sql = getInsertSql(donor);
			    insert(sql);
			}

			DeaccessionAgent receiver = getDeaccessionAgent(transaction, deaccession, ROLE.receiver);
			if (receiver != null)
			{
			    sql = getInsertSql(receiver);
			    insert(sql);
			}
		}
		else if (type.equals(TYPE.Purchase))
		{
		    Accession accession = getAccession(transaction, ACCESSION_TYPE.purchase);
		    
		    String sql = getInsertSql(accession);
		    Integer accessionId = insert(sql);
		    accession.setAccessionId(accessionId);
		    
		    AccessionAgent preparer = getAccessionAgent(transaction, accession, ROLE.preparer);
		    if (preparer != null)
		    {
		        sql = getInsertSql(preparer);
		        insert(sql);
		    }
		    
		    AccessionAgent contributor = getAccessionAgent(transaction, accession, ROLE.contributor);
		    if (contributor != null)
		    {
		        sql = getInsertSql(contributor);
		        insert(sql);
		    }
		}
		else if (type.equals(TYPE.StaffCollection))
		{
		    Accession accession = getAccession(transaction, ACCESSION_TYPE.cln);
		    
		    String sql = getInsertSql(accession);
		    Integer accessionId = insert(sql);
		    accession.setAccessionId(accessionId);
		    
		    AccessionAgent collector = getAccessionAgent(transaction, accession, ROLE.collector);
		    if (collector != null)
		    {
		        sql = getInsertSql(collector);
		        insert(sql);
		    }
		}
		else if (type.equals(TYPE.OutMiscellaneous))
		{
		    // TODO: figure out how to deal with outgoing miscellaneous transactions
		    warn("Outgoing miscellaneous transaction, do this by hand", transaction.getTransactionNo());
		}
	}

	private Transaction parse(String[] columns) throws LocalException
	{
    	if (columns.length < 22)
    	{
    		throw new LocalException("Wrong number of columns");
    	}
    	
		Transaction transaction = new Transaction();
		
		try
		{
			transaction.setId(                     SqlUtils.parseInt( StringUtils.trimToNull( columns[0] )));
			transaction.setType(               Transaction.parseType( StringUtils.trimToNull( columns[1] )));
			transaction.setAgentId(                SqlUtils.parseInt( StringUtils.trimToNull( columns[2] )));
			transaction.setLocalUnit(                                 StringUtils.trimToNull( columns[3] ));
			transaction.setRequestType( Transaction.parseRequestType( StringUtils.trimToNull( columns[4] )));
			transaction.setPurpose(         Transaction.parsePurpose( StringUtils.trimToNull( columns[5] )));
			transaction.setAffiliateId(            SqlUtils.parseInt( StringUtils.trimToNull( columns[6] )));
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
			
			transaction.setHigherTaxon( StringUtils.trimToNull( columns[20] ));
			transaction.setTaxon(       StringUtils.trimToNull( columns[21] ));          
		}
		catch (NumberFormatException e)
		{
			throw new LocalException("Couldn't parse numeric field", e);
		}
		catch (AsaException e)
		{
			throw new LocalException("Couldn't parse field", e);
		}
		
		return transaction;
	}
	
	private Integer getBotanistIdByAffiliateId(Integer affiliateId)
	{
		return botanistsByAffiliate.map(affiliateId);
	}
	
	private Integer getBotanistIdByAgentId(Integer agentId)
	{
		return botanistsByAgent.map(agentId);
	}
	
	private Loan getLoan(Transaction transaction) throws LocalException
	{
		Loan loan = new Loan();
        
		// TODO: their are some loans with forUseBy = ours, do we note that somewhere?
		
	    // TODO: AddressOfRecordID
		
	    // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        loan.setCreatedByAgent(createdByAgent);
        
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

	    // DisciplineID
        loan.setDiscipline(discipline);

        // DivisionID
        loan.setDivision(division);
        
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
			warn("Truncating loan number", transactionNo);
			transactionNo = transactionNo.substring(0, 50);
		}
		loan.setLoanNumber(transactionNo);

        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        loan.setNumber1((float) transactionId);
        
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
		
		// TODO: SrcGeography
		
		// SrcTaxonomy
		String higherTaxon = transaction.getHigherTaxon();
		String taxon = transaction.getTaxon();
		
		String srcTaxonomy = null;
		if (higherTaxon != null)
		{
		    if (taxon != null) srcTaxonomy = higherTaxon + " " + taxon;
		    else srcTaxonomy = higherTaxon;
		}
		else
		{
		    if (higherTaxon != null) srcTaxonomy = higherTaxon;
		}
		loan.setSrcTaxonomy(srcTaxonomy);
		
		// Text1 (description)
		String description = transaction.getDescription();
		loan.setText1(description);
		
		// Text2
		String forUseBy = transaction.getForUseBy();
		loan.setText2(forUseBy);
		
	    // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        loan.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
		// YesNo1 (isAcknowledged)
		Boolean isAcknowledged = transaction.isAcknowledged();
		loan.setYesNo1(isAcknowledged);
						
		return loan;
	}
	
	private LoanAgent getLoanAgent(Transaction transaction, Loan loan, ROLE role, Integer collectionMemberId) throws LocalException
	{
        LoanAgent loanAgent = new LoanAgent();
        
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.preparer))
	    {
	        agent = getAffiliateAgent(transaction);
	    }
	    else if (role.equals(ROLE.borrower))
	    {
	        agent = getAsaAgentAgent(transaction);
	    }

        if (agent.getId() == null) return null;
        
        loanAgent.setAgent(agent);
        
        // CollectionMemberID
        loanAgent.setCollectionMemberId(collectionMemberId);
        
        // LoanID
        loanAgent.setLoan(loan);
        
        // Remarks
        String forUseBy = transaction.getForUseBy();
        String userType = transaction.getUserType().name();
        
        if (role.equals(ROLE.borrower)) // "for use by"
        {
            String remarks = "For use by " + (forUseBy != null ? forUseBy : "") + "(" + userType + ")";
            loan.setRemarks(remarks);
        }
        
        // Role
        loanAgent.setRole(role.name());
        
        return loanAgent;
	}

	private Agent getAffiliateAgent(Transaction transaction) throws LocalException
	{
        Agent agent = null;
        
	    Integer affiliateId = transaction.getAffiliateId();

	    if (affiliateId != null)
	    {
	    	Integer botanistId = getBotanistIdByAffiliateId(affiliateId);
	    	if (botanistId != null)
	    	{
	    		agent = lookup(botanistId);
	    	}
	    	else
	    	{
	    		agent = getAgentByAffiliateId(affiliateId);
	    	}
	    }
	    
        return agent;
	}
	
	private Agent getAsaAgentAgent(Transaction transaction) throws LocalException
	{
	    Agent agent = new Agent();
	    
	    Integer asaAgentId = transaction.getAgentId();

	    if (asaAgentId != null)
	    {
	        Integer botanistId = getBotanistIdByAgentId(asaAgentId);
	        if (botanistId != null)
	        {
	        	agent = lookup(botanistId);
	        }
	        else
	        {
	        	agent = getAgentByAsaAgentId(asaAgentId);
	        }
	    }
	    return agent;
	}

	private Borrow getBorrow(Transaction transaction, Integer collectionMemberId) throws LocalException
	{
		Borrow borrow = new Borrow();
        
        // TODO: AddressOfRecordID
		
        // CollectionMemberID
        borrow.setCollectionMemberId(collectionMemberId);
        
        // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        borrow.setCreatedByAgent(createdByAgent);
        
        // CurrentDueDate
        Date currentDueDate = transaction.getCurrentDueDate();
        if (currentDueDate != null)
        {
            borrow.setCurrentDueDate(DateUtils.toCalendar(currentDueDate));
        }
        
        // DateClosed
        Date closeDate = transaction.getCloseDate();
        if (closeDate != null)
        {
            borrow.setDateClosed(DateUtils.toCalendar(closeDate));
        }

        // InvoiceNumber
        String transactionNo = transaction.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_BORROW_NUMBER;
        }
        if (transactionNo.length() > 50)
        {
            warn("Truncating invoice number", transactionNo);
            transactionNo = transactionNo.substring(0, 50);
        }
        borrow.setInvoiceNumber(transactionNo);
        
        // IsClosed
        borrow.setIsClosed(closeDate != null);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        borrow.setNumber1((float) transactionId);
        
        // OriginalDueDate
        Date originalDueDate = transaction.getOriginalDueDate();
        if (originalDueDate != null)
        {
            borrow.setOriginalDueDate(DateUtils.toCalendar(originalDueDate));
        }
        
        // ReceivedDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            borrow.setReceivedDate(DateUtils.toCalendar(openDate));
        }

        // Remarks
        String remarks = transaction.getRemarks();
        borrow.setRemarks(remarks);
                
        // Text1 (description)
        String description = transaction.getDescription();
        borrow.setText1(description);
        
        // Text2
        String forUseBy = transaction.getForUseBy();
        borrow.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        borrow.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        borrow.setYesNo1(isAcknowledged);
        
		return borrow;
	}
	
	private BorrowAgent getBorrowAgent(Transaction transaction, Borrow borrow, ROLE role, Integer collectionMemberId) throws LocalException
	{
	    BorrowAgent borrowAgent = new BorrowAgent();
        
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.borrower))
        {
            agent = getAffiliateAgent(transaction);
        }
        else if (role.equals(ROLE.lender))
        {
            agent = getAsaAgentAgent(transaction);
        }
        
        if (agent.getId() == null) return null;
        
        borrowAgent.setAgent(agent);
        
        // CollectionMemberID
        borrowAgent.setCollectionMemberId(collectionMemberId);
        
        // LoanID
        borrowAgent.setBorrow(borrow);
        
        // Remarks
        if (role.equals(ROLE.borrower))
        {
            String forUseBy = transaction.getForUseBy();
            String remarks = "(" + forUseBy + ")";
            borrowAgent.setRemarks(remarks);
        }
        
        // Role
        borrowAgent.setRole(role.name());
        
        return borrowAgent;
	}
	
	private ExchangeIn getExchangeIn(Transaction transaction) throws LocalException
	{
		ExchangeIn exchangeIn = new ExchangeIn();

		// TODO: AddressOfRecord
		
		// CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        exchangeIn.setCreatedByAgent(createdByAgent);
        
		// CatalogedByID ("for use by")
        Agent agentCatalogedBy = getAffiliateAgent(transaction);
        exchangeIn.setAgentCatalogedBy(agentCatalogedBy);

        // DivisionID
        exchangeIn.setDivision(division);
        
        // DescriptionOfMaterial
        String description = transaction.getDescription();
        exchangeIn.setDescriptionOfMaterial(description);
        
		// ExchangeDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            exchangeIn.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeIn.setNumber1((float) transactionId);
		
        // QuantityExchanged
        
        // ReceivedFromOrganization ("contact")
        Agent agentReceivedFrom = getAsaAgentAgent(transaction);
        exchangeIn.setAgentReceivedFrom(agentReceivedFrom);
        
        // Remarks
        String remarks = transaction.getRemarks();
        exchangeIn.setRemarks(remarks);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text2
        String forUseBy = transaction.getForUseBy();
        exchangeIn.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        exchangeIn.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        exchangeIn.setYesNo1(isAcknowledged);
		
		return exchangeIn;
	}
	
	private ExchangeOut getExchangeOut(Transaction transaction) throws LocalException
	{
		ExchangeOut exchangeOut = new ExchangeOut();

		// TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        exchangeOut.setCreatedByAgent(createdByAgent);
        
        // CatalogedByID
        Agent agentCatalogedBy = getAffiliateAgent(transaction);
        exchangeOut.setAgentCatalogedBy(agentCatalogedBy);

        // DescriptionOfMaterial
        String description = transaction.getDescription();
        exchangeOut.setDescriptionOfMaterial(description);
        
        // DivisionID
        exchangeOut.setDivision(division);
        
        // ExchangeDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            exchangeOut.setExchangeDate(DateUtils.toCalendar(openDate));
        }
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        exchangeOut.setNumber1((float) transactionId);
        
        // QuantityExchanged
        
        // Remarks
        String remarks = transaction.getRemarks();
        exchangeOut.setRemarks(remarks);
                
        // SentToOrganization
        Agent agentSentTo = getAsaAgentAgent(transaction);
        exchangeOut.setAgentSentTo(agentSentTo);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text2
        String forUseBy = transaction.getForUseBy();
        exchangeOut.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        exchangeOut.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        exchangeOut.setYesNo1(isAcknowledged);
        
		return exchangeOut;
	}
	
	private Gift getGift(Transaction transaction) throws LocalException
	{
		Gift gift = new Gift();

		// TODO: AddressOfRecord
        
        // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        gift.setCreatedByAgent(createdByAgent);
        
        // DisciplineID
        gift.setDiscipline(discipline);
        
        // DivisionID
        gift.setDivision(division);
        
        // GiftDate
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            gift.setGiftDate(DateUtils.toCalendar(openDate));
        }
        
        // GiftNumber
        String transactionNo = transaction.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_GIFT_NUMBER;
        }
        if (transactionNo.length() > 50)
        {
            warn("Truncating invoice number", transactionNo);
            transactionNo = transactionNo.substring(0, 50);
        }
        gift.setGiftNumber(transactionNo);
        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        gift.setNumber1((float) transactionId);
        
        // PurposeOfGift
        PURPOSE purpose = transaction.getPurpose();
        String purposeOfGift = Transaction.toString(purpose);
        gift.setPurposeOfGift(purposeOfGift);
        
        // Remarks
        String remarks = transaction.getRemarks();
        gift.setRemarks(remarks);
        
        // TODO: SrcGeography
        
        // TODO: SrcTaxonomy
        
        // Text1 (description)
        String description = transaction.getDescription();
        gift.setText1(description);
        
        // Text2
        String forUseBy = transaction.getForUseBy();
        gift.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        gift.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        gift.setYesNo1(isAcknowledged);
        
		return gift;
	}
	
	private GiftAgent getGiftAgent(Transaction transaction, Gift gift, ROLE role, Integer collectionMemberId) throws LocalException
	{
	    GiftAgent giftAgent = new GiftAgent();
	    
        // Agent
        Agent agent = null;

        if (role.equals(ROLE.receiver))
        {
            agent = getAffiliateAgent(transaction);
        }
        else if (role.equals(ROLE.donor))
        {
            agent = getAsaAgentAgent(transaction);
        }
        
        if (agent.getId() == null) return null;
        
        giftAgent.setAgent(agent);
        
        // CollectionMemberID
        giftAgent.setCollectionMemberId(collectionMemberId);
        
        // GiftID
        giftAgent.setGift(gift);
        
        // Remarks
        
        // Role
        giftAgent.setRole(role.name());
        
        return giftAgent;
	}

	private Deaccession getDeaccession(Transaction transaction, ACCESSION_TYPE type) throws LocalException
	{
	    Deaccession deaccession = new Deaccession();
	    
	    // CreatedByAgentID
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        deaccession.setCreatedByAgent(createdByAgent);
        
	    // DeaccessionDate
	    Date openDate = transaction.getOpenDate();
	    if (openDate != null)
	    {
	        deaccession.setDeaccessionDate(DateUtils.toCalendar(openDate));
	    }
	        
	    // DeaccessionNumber
	    String transactionNo = transaction.getTransactionNo();
	    if ( transactionNo == null)
	    {
	        transactionNo = DEFAULT_DEACCESSION_NUMBER;
	    }
	    if (transactionNo.length() > 50)
	    {
	        warn("Truncating loan number", transactionNo);
	        transactionNo = transactionNo.substring(0, 50);
	    }
	    deaccession.setDeaccessionNumber(transactionNo);
	        
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        if (transactionId == null)
        {
            throw new LocalException("No transaction id");
        }
        deaccession.setNumber1((float) transactionId);
	    
	    // Remarks
	    String remarks = transaction.getRemarks();
	    deaccession.setRemarks(remarks);
	    
        // Text1 (description)
        String description = transaction.getDescription();
        deaccession.setText1(description);
        
        // Text2
        String forUseBy = transaction.getForUseBy();
        deaccession.setText2(forUseBy);
        
        // TimestampCreated
        Date dateCreated = transaction.getDateCreated();
        deaccession.setTimestampCreated(DateUtils.toTimestamp(dateCreated));
        
        // Type
        deaccession.setType(type.name());

        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        deaccession.setYesNo1(isAcknowledged);
	    
	    return deaccession;
	}

	private DeaccessionAgent getDeaccessionAgent(Transaction transaction, Deaccession deaccession, ROLE role)
	    throws LocalException
	{
	    DeaccessionAgent deaccessionAgent = new DeaccessionAgent();

	    // Agent
	    Agent agent = null;

	    if (role.equals(ROLE.donor))
	    {
	        agent = getAffiliateAgent(transaction);
	    }
	    else if (role.equals(ROLE.receiver))
	    {
	        agent = getAsaAgentAgent(transaction);
	    }

	    if (agent.getId() == null) return null;

	    deaccessionAgent.setAgent(agent);

	    // Deaccession
	    deaccessionAgent.setDeaccession(deaccession);

	    // Remarks

	    // Role
	    deaccessionAgent.setRole(role.name());
	    
	    return deaccessionAgent;
	}

	private Accession getAccession(Transaction transaction, ACCESSION_TYPE type) throws LocalException
	{
	    Accession accession = new Accession();
	    
	    // TODO: AddressOfRecord
	    
	    // CreatedByAgent
        Integer creatorOptrId = transaction.getCreatedById();
        Agent createdByAgent = getAgentByOptrId(creatorOptrId);
        accession.setCreatedByAgent(createdByAgent);
        
	    // AccessionNumber
        String transactionNo = transaction.getTransactionNo();
        if ( transactionNo == null)
        {
            transactionNo = DEFAULT_ACCESSION_NUMBER;
        }
        if (transactionNo.length() > 50)
        {
            warn("Truncating invoice number", transactionNo);
            transactionNo = transactionNo.substring(0, 50);
        }
        accession.setAccessionNumber(transactionNo);
        
	    // DateAccessioned
        Date openDate = transaction.getOpenDate();
        if (openDate != null)
        {
            accession.setDateAccessioned(DateUtils.toCalendar(openDate));
        }
        
	    // Division
        accession.setDivision(division);
	    
        // Number1 (id) TODO: temporary!! remove when done!
        Integer transactionId = transaction.getId();
        checkNull(transactionId, "transaction id");
        
        accession.setNumber1((float) transactionId);
	    
	    // Remarks
	    String remarks = accession.getRemarks();
	    accession.setRemarks(remarks);
	    
        // Text1 (description)
        String description = transaction.getDescription();
        accession.setText1(description);
        
        // Text2 (forUseBy)
        String forUseBy = transaction.getForUseBy();
        accession.setText2(forUseBy);
	    
        // Text3 (boxCount)
        String boxCount = transaction.getBoxCount();
        accession.setText3(boxCount);
        
	    // Type
	    accession.setType(type.name());
	    
        // YesNo1 (isAcknowledged)
        Boolean isAcknowledged = transaction.isAcknowledged();
        accession.setYesNo1(isAcknowledged);
	    
	    return accession;
	}
	
	private AccessionAgent getAccessionAgent(Transaction transaction, Accession accession, ROLE role)
	    throws LocalException
	{
	       AccessionAgent accessionAgent = new AccessionAgent();

	        // Agent
	        Agent agent = null;

	        if (role.equals(ROLE.preparer) || role.equals(ROLE.collector))
	        {
	            agent = getAffiliateAgent(transaction);
	        }
	        else if (role.equals(ROLE.contributor))
	        {
	            agent = getAsaAgentAgent(transaction);
	        }

	        if (agent.getId() == null) return null;

	        accessionAgent.setAgent(agent);

	        // Deaccession
	        accessionAgent.setAccession(accession);

	        // Remarks

	        // Role
	        accessionAgent.setRole(role.name());
	        
	        return accessionAgent;
	}

	private String getInsertSql(Loan loan)
	{
		String fieldNames = "CreatedByAgentID, CurrentDueDate, DateClosed, DisciplineId, DivisionId, " +
				            "IsClosed, LoanDate, LoanNumber, Number1, OriginalDueDate, PurposeOfLoan, " +
				            "Remarks, SrcTaxonomy, Text1, Text2, TimestampCreated, YesNo1";
		
		String[] values = new String[17];
		
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
		values[12] = SqlUtils.sqlString( loan.getSrcTaxonomy());
		values[13] = SqlUtils.sqlString( loan.getText1());
		values[14] = SqlUtils.sqlString( loan.getText2());
		values[15] = SqlUtils.sqlString( loan.getTimestampCreated());
		values[16] = SqlUtils.sqlString( loan.getYesNo1());
		
		return SqlUtils.getInsertSql("loan", fieldNames, values);
	}
	
	private String getInsertSql(Borrow borrow)
	{
		String fieldNames = "CollectionMemberID, CreatedByAgentID, CurrentDueDate, DateClosed" +
				            "InvoiceNumber, IsClosed, Number1, OriginalDueDate, ReceivedDate" +
				            "Remarks, Text1, Text2,  TimestampCreated, YesNo1";
		
		String[] values = new String[14];
		
		values[0]  = SqlUtils.sqlString( borrow.getCollectionMemberId());
		values[1]  = SqlUtils.sqlString( borrow.getCreatedByAgent().getId());
		values[2]  = SqlUtils.sqlString( borrow.getCurrentDueDate());
		values[3]  = SqlUtils.sqlString( borrow.getDateClosed());
		values[4]  = SqlUtils.sqlString( borrow.getInvoiceNumber());
		values[5]  = SqlUtils.sqlString( borrow.getIsClosed());
		values[6]  = SqlUtils.sqlString( borrow.getNumber1());
		values[7]  = SqlUtils.sqlString( borrow.getOriginalDueDate());
		values[8]  = SqlUtils.sqlString( borrow.getReceivedDate());
		values[9]  = SqlUtils.sqlString( borrow.getRemarks());
		values[10] = SqlUtils.sqlString( borrow.getText1());
		values[11] = SqlUtils.sqlString( borrow.getText2());
		values[12] = SqlUtils.sqlString( borrow.getTimestampCreated());
		values[13] = SqlUtils.sqlString( borrow.getYesNo1());
	        
		return SqlUtils.getInsertSql("borrow", fieldNames, values);
	}
	
	private String getInsertSql(ExchangeIn exchangeIn)
	{
		String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, ExchangeDate, " +
				            "Number1, ReceivedFromOrganizationID, Remarks, Text2, TimestampCreated, YesNo1";
		
		String[] values = new String[11];
		
		values[0]  = SqlUtils.sqlString( exchangeIn.getAgentCatalogedBy().getId());
		values[1]  = SqlUtils.sqlString( exchangeIn.getCreatedByAgent().getId());
		values[2]  = SqlUtils.sqlString( exchangeIn.getDescriptionOfMaterial());
		values[3]  = SqlUtils.sqlString( exchangeIn.getDivision().getId());
		values[4]  = SqlUtils.sqlString( exchangeIn.getExchangeDate());
		values[5]  = SqlUtils.sqlString( exchangeIn.getNumber1());
		values[6]  = SqlUtils.sqlString( exchangeIn.getAgentReceivedFrom().getId());
		values[7]  = SqlUtils.sqlString( exchangeIn.getRemarks());
		values[8]  = SqlUtils.sqlString( exchangeIn.getText2());
		values[9]  = SqlUtils.sqlString( exchangeIn.getTimestampCreated());
		values[10] = SqlUtils.sqlString( exchangeIn.getYesNo1());
		
		return SqlUtils.getInsertSql("exchangein", fieldNames, values);
	}
	
	private String getInsertSql(ExchangeOut exchangeOut)
	{
        String fieldNames = "CatalogedByID, CreatedByAgentID, DescriptionOfMaterial, DivisionID, " +
                            "ExchangeDate, Number1, Remarks, SentToOrganizationID, Text2, " +
                            "TimestampCreated, YesNo1";

        String[] values = new String[11];

        values[0]  = SqlUtils.sqlString( exchangeOut.getAgentCatalogedBy().getId());
        values[1]  = SqlUtils.sqlString( exchangeOut.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( exchangeOut.getDescriptionOfMaterial());
        values[3]  = SqlUtils.sqlString( exchangeOut.getDivision().getId());
        values[4]  = SqlUtils.sqlString( exchangeOut.getExchangeDate());
        values[5]  = SqlUtils.sqlString( exchangeOut.getNumber1());
        values[6]  = SqlUtils.sqlString( exchangeOut.getRemarks());
        values[7]  = SqlUtils.sqlString( exchangeOut.getAgentSentTo().getId());
        values[8]  = SqlUtils.sqlString( exchangeOut.getText2());
        values[9]  = SqlUtils.sqlString( exchangeOut.getTimestampCreated());
        values[10] = SqlUtils.sqlString( exchangeOut.getYesNo1());
		
		return SqlUtils.getInsertSql("exchangeout", fieldNames, values);
	}
	
	private String getInsertSql(Gift gift)
	{
		String fieldNames = "CreatedByAgentID, DisciplineID, DivisionID, GiftDate, GiftNumber, " +
				            "Number1, PurposeOfGift, Remarks, Text1, Text2, TimestampCreated, YesNo1";
		
		String[] values = new String[12];
		
		values[0]  = SqlUtils.sqlString( gift.getCreatedByAgent().getId());
		values[1]  = SqlUtils.sqlString( gift.getDiscipline().getId());
		values[2]  = SqlUtils.sqlString( gift.getDivision().getId());
		values[3]  = SqlUtils.sqlString( gift.getGiftDate());
		values[4]  = SqlUtils.sqlString( gift.getGiftNumber());
		values[5]  = SqlUtils.sqlString( gift.getNumber1());
		values[6]  = SqlUtils.sqlString( gift.getPurposeOfGift());
		values[7]  = SqlUtils.sqlString( gift.getRemarks());
		values[8]  = SqlUtils.sqlString( gift.getText1());
		values[9]  = SqlUtils.sqlString( gift.getText2());
		values[10] = SqlUtils.sqlString( gift.getTimestampCreated());
		values[11] = SqlUtils.sqlString( gift.getYesNo1());
		
		return SqlUtils.getInsertSql("gift", fieldNames, values);
	}
	
	
	private String getInsertSql(LoanAgent loanAgent)
	{
	    String fieldNames = "AgentID, CollectionMemberID, LoanID, Role, TimestampCreated";
	        
	    String[] values = new String[5];
	    
	    values[0] = SqlUtils.sqlString( loanAgent.getAgent().getId());
	    values[1] = SqlUtils.sqlString( loanAgent.getCollectionMemberId());
	    values[2] = SqlUtils.sqlString( loanAgent.getLoan().getId());
	    values[3] = SqlUtils.sqlString( loanAgent.getRole());
	    values[4] = SqlUtils.now();
	    
	    return SqlUtils.getInsertSql("loanagent", fieldNames, values);
	}
	
	
	private String getInsertSql(BorrowAgent borrowAgent)
	{
	    String fieldNames = "AgentID, BorrowID, CollectionMemberID, Role, TimestampCreated";

	    String[] values = new String[5];

	    values[0] = SqlUtils.sqlString( borrowAgent.getAgent().getId());
	    values[1] = SqlUtils.sqlString( borrowAgent.getBorrow().getId());
	    values[2] = SqlUtils.sqlString( borrowAgent.getCollectionMemberId());
	    values[3] = SqlUtils.sqlString( borrowAgent.getRole());
	    values[4] = SqlUtils.now();

	    return SqlUtils.getInsertSql("borrowagent", fieldNames, values);
	}
	
	
	private String getInsertSql(GiftAgent borrowAgent)
	{
	    String fieldNames = "AgentID, CollectionMemberID, GiftID, Role, TimestampCreated";

	    String[] values = new String[5];

	    values[0] = SqlUtils.sqlString( borrowAgent.getAgent().getId());
	    values[1] = SqlUtils.sqlString( borrowAgent.getCollectionMemberId());
        values[2] = SqlUtils.sqlString( borrowAgent.getGift().getId());
        values[3] = SqlUtils.sqlString( borrowAgent.getRole());
	    values[4] = SqlUtils.now();

	    return SqlUtils.getInsertSql("giftagent", fieldNames, values);
	}
	
	
	private String getInsertSql(Deaccession deaccession)
	{
        String fieldNames = "CreatedByAgentID, DeaccessionDate, DeaccessionNumber, Number1, " +
                            "Remarks, Text1, Text2, Type, TimestampCreated, YesNo1";

        String[] values = new String[10];

        values[0] = SqlUtils.sqlString( deaccession.getCreatedByAgent().getId());
        values[1] = SqlUtils.sqlString( deaccession.getDeaccessionDate());
        values[2] = SqlUtils.sqlString( deaccession.getDeaccessionNumber());
        values[3] = SqlUtils.sqlString( deaccession.getNumber1());
        values[4] = SqlUtils.sqlString( deaccession.getRemarks());
        values[5] = SqlUtils.sqlString( deaccession.getText1());
        values[6] = SqlUtils.sqlString( deaccession.getText2());
        values[7] = SqlUtils.sqlString( deaccession.getType());
        values[8] = SqlUtils.sqlString( deaccession.getTimestampCreated());
        values[9] = SqlUtils.sqlString( deaccession.getYesNo1());
	    
	    return SqlUtils.getInsertSql("deaccession", fieldNames, values);
	}
	
	
	private String getInsertSql(DeaccessionAgent deaccessionAgent)
	{
        String fieldNames = "AgentID, DaccessionID, Role, TimestampCreated";

        String[] values = new String[4];

        values[0] = SqlUtils.sqlString( deaccessionAgent.getAgent().getId());
        values[1] = SqlUtils.sqlString( deaccessionAgent.getDeaccession().getId());
        values[2] = SqlUtils.sqlString( deaccessionAgent.getRole());
        values[3] = SqlUtils.now();
	    
	    return SqlUtils.getInsertSql("deaccessionagent", fieldNames, values);
	}
	
	
	private String getInsertSql(Accession deaccession)
	{
        String fieldNames = "AccessionNumber, CreatedByAgentID, DateAccessioned, Number1, " +
                            "Remarks, Text1, Text2, Text3, Type, TimestampCreated, YesNo1";

        String[] values = new String[11];

        values[0]  = SqlUtils.sqlString( deaccession.getAccessionNumber());
        values[1]  = SqlUtils.sqlString( deaccession.getCreatedByAgent().getId());
        values[2]  = SqlUtils.sqlString( deaccession.getDateAccessioned());
        values[3]  = SqlUtils.sqlString( deaccession.getNumber1());
        values[4]  = SqlUtils.sqlString( deaccession.getRemarks());
        values[5]  = SqlUtils.sqlString( deaccession.getText1());
        values[6]  = SqlUtils.sqlString( deaccession.getText2());
        values[7]  = SqlUtils.sqlString( deaccession.getText3());
        values[8]  = SqlUtils.sqlString( deaccession.getType());
        values[9]  = SqlUtils.sqlString( deaccession.getTimestampCreated());
        values[10] = SqlUtils.sqlString( deaccession.getYesNo1());

	    return SqlUtils.getInsertSql("accession", fieldNames, values);
	}

	private String getInsertSql(AccessionAgent accessionAgent)
	{
        String fieldNames = "AccessionID, AgentID, Role, TimestampCreated";

        String[] values = new String[4];

        values[0] = SqlUtils.sqlString( accessionAgent.getAccession().getId());
        values[1] = SqlUtils.sqlString( accessionAgent.getAgent().getId());
        values[2] = SqlUtils.sqlString( accessionAgent.getRole());
        values[3] = SqlUtils.now();

	    return SqlUtils.getInsertSql("accessionagent", fieldNames, values);
	}
	
    // TODO: move to interface
	private Agent getAgentByAffiliateId(Integer affiliateId) throws LocalException
	{
		Agent agent = new Agent();
		
		String guid = AffiliateLoader.getGuid(affiliateId);
		
        Integer agentId = getInt("agent", "AgentID", "GUID", guid);

        agent.setAgentId(agentId);
        
        return agent;
	}
    // TODO: move to interface
	private Agent getAgentByAsaAgentId(Integer asaAgentId) throws LocalException
	{
		Agent agent = new Agent();
		
		String guid = AgentLoader.getGuid(asaAgentId);
		
        Integer agentId = getInt("agent", "AgentID", "GUID", guid);

        agent.setAgentId(agentId);
        
        return agent;
	}
}
