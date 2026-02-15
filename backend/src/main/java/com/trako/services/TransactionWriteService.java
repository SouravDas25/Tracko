package com.trako.services;

import com.trako.entities.Account;
import com.trako.entities.AccountMonthlySummary;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.UserCurrency;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.AccountMonthSummaryRepository;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UserCurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * Write-oriented service for {@link Transaction} mutations.
 *
 * <p>This is the single supported write-path for creating/updating/deleting transactions.
 * It is responsible for maintaining the pre-aggregated {@code account_month_summary} table
 * (via {@link AccountMonthSummaryRepository}) so read-side queries (month summary and rollover)
 * can avoid scanning the raw {@code transactions} table.
 */
@Service
public class TransactionWriteService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionWriteService.class);

    // Single write-path for Transaction mutations.
    // This service is responsible for keeping the pre-aggregated account_month_summary table
    // consistent, which allows TransactionService to answer month summaries + rollover queries
    // without scanning raw transactions.

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMonthSummaryRepository accountMonthSummaryRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final int TYPE_DEBIT = 1;
    private static final int TYPE_CREDIT = 2;

    @Transactional
    /**
     * Creates or updates a transaction for the given user.
     *
     * <p>On update, the existing transaction (if any) is loaded so we can apply an incremental
     * (old -&gt; new) delta to {@code account_month_summary}.
     *
     * <p>If {@link Transaction#getAmount()} is null, the amount is derived from the original
     * currency fields ({@code originalAmount/originalCurrency/exchangeRate}) before persistence.
     *
     * @param userId      authenticated user id
     * @param transaction transaction payload
     * @return persisted transaction
     */
    public Transaction saveForUser(String userId, Transaction transaction) {
        // For updates we need the previous state so we can apply a (old -> new) delta to the summary table.
        Transaction existing = null;
        if (transaction.getId() != null) {
            existing = transactionRepository.findById(transaction.getId()).orElse(null);
        }

        // Ensure Transaction.amount is populated (supports multi-currency inputs via originalAmount/currency/rate).
        computeAmountIfMissing(userId, transaction);

        Transaction saved = transactionRepository.save(transaction);

        // Summary maintenance: apply delta using (old -> new)
        applyDelta(userId, existing, saved);

        return saved;
    }

    @Transactional
    /**
     * Deletes a transaction for the given user and reverses its contribution from
     * {@code account_month_summary}.
     *
     * @param userId        authenticated user id
     * @param transactionId id of the transaction to delete
     */
    public void deleteForUser(String userId, Long transactionId) {
        // On delete we apply a delta of (old -> null) to reverse the summary contribution.
        Transaction existing = transactionRepository.findById(transactionId).orElse(null);
        if (existing == null) {
            return;
        }

        transactionRepository.deleteById(transactionId);
        applyDelta(userId, existing, null);
    }

    private void computeAmountIfMissing(String userId, Transaction transaction) {
        // Normalizes foreign-currency transactions into the user's base currency.
        // If amount is provided, we trust it; otherwise we derive it from originalAmount and either:
        // - exchangeRate (explicit), or
        // - originalCurrency (lookup in user's configured secondary currencies).
        if (transaction.getAmount() == null) {
            if (transaction.getOriginalAmount() != null && transaction.getExchangeRate() != null) {
                double calculatedAmount = transaction.getOriginalAmount() * transaction.getExchangeRate();
                transaction.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
            } else if (transaction.getOriginalAmount() != null && transaction.getOriginalCurrency() != null) {
                String uid = userId;
                String currencyCode = transaction.getOriginalCurrency().toUpperCase();
                UserCurrency uc = userCurrencyRepository.findByUserIdAndCurrencyCode(uid, currencyCode);
                if (uc != null && uc.getExchangeRate() != null) {
                    double calculatedAmount = transaction.getOriginalAmount() * uc.getExchangeRate();
                    transaction.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
                    transaction.setExchangeRate(uc.getExchangeRate());
                } else {
                    throw new IllegalArgumentException("No exchange rate configured for currency: " + currencyCode);
                }
            } else {
                throw new IllegalArgumentException(
                        "Amount cannot be null unless originalAmount and either exchangeRate or originalCurrency are provided");
            }
        }
    }

    private void applyDelta(String userId, Transaction oldTx, Transaction newTx) {
        logger.info("Applying delta for userId: {}, oldTx: {}, newTx: {}", userId, oldTx, newTx);

        // We only ever update summaries based on the difference between the old and new contribution.
        // Contribution rules:
        // - only isCountable == 1 contributes
        // - month bucket = transaction.date's (year, month)
        // - CREDIT adds to income, DEBIT adds to expense; net = income - expense
        SummaryContribution oldC = SummaryContribution.from(oldTx);
        SummaryContribution newC = SummaryContribution.from(newTx);

        logger.info("Derived contributions - oldC: {}, newC: {}", oldC, newC);

        if (oldC.isZero() && newC.isZero()) {
            logger.info("No changes to apply. Both contributions are zero.");
            return;
        }

        // Subtract old
        if (!oldC.isZero()) {
            logger.info("Subtracting old contribution: {}", oldC);
            updateSummaryRow(userId, oldC, -1);
        }

        // Add new
        if (!newC.isZero()) {
            logger.info("Adding new contribution: {}", newC);
            updateSummaryRow(userId, newC, +1);
        }
    }

    // Applies a signed delta from a SummaryContribution to the
    // corresponding row in account_month_summary.
    //
    // - userId: caller user id (used to enforce ownership of the account).
    // - c: per-transaction contribution for a specific (accountId, year, month).
    // - sign: +1 when adding a new contribution, -1 when removing a previous one.
    private void updateSummaryRow(String userId, SummaryContribution c, int sign) {
        if (c.accountId == null || c.year == null || c.month == null) {
            return;
        }

        // Ensure an account exists and belongs to the caller user before mutating summaries.
        Account acc = accountRepository.findById(c.accountId).orElse(null);
        if (acc == null) {
            return;
        }
        if (userId != null && !userId.equals(acc.getUserId())) {
            return;
        }

        // Upsert the month summary row, then apply the signed delta.
        AccountMonthlySummary row = accountMonthSummaryRepository
                .findByUserIdAndAccountIdAndYearAndMonth(acc.getUserId(), c.accountId, c.year, c.month)
                .orElse(null);
        
        boolean isNewRow = false;
        if (row == null) {
            row = new AccountMonthlySummary();
            row.setUserId(acc.getUserId());
            row.setAccountId(c.accountId);
            row.setYear(c.year);
            row.setMonth(c.month);
            row.setIncomeTotal(0.0);
            row.setExpenseTotal(0.0);
            row.setNetTotal(0.0);
            row.setCountCountable(0);
            isNewRow = true;
        }

        double income = safe(row.getIncomeTotal());
        double expense = safe(row.getExpenseTotal());
        double net = safe(row.getNetTotal());
        int cnt = row.getCountCountable() != null ? row.getCountCountable() : 0;

        income += sign * c.income;
        expense += sign * c.expense;
        net += sign * c.net;
        cnt += sign * c.count;

        // Defensive validation: Prevent negative values from data inconsistencies
        if (income < 0) {
            logger.warn("Detected negative income for userId={}, accountId={}, year={}, month={}: {}. This indicates a data inconsistency. Resetting to 0.",
                        userId, c.accountId, c.year, c.month, income);
            income = 0;
        }
        if (expense < 0) {
            logger.warn("Detected negative expense for userId={}, accountId={}, year={}, month={}: {}. This indicates a data inconsistency. Resetting to 0.",
                        userId, c.accountId, c.year, c.month, expense);
            expense = 0;
        }
        if (cnt < 0) {
            logger.warn("Detected negative count for userId={}, accountId={}, year={}, month={}: {}. This indicates a data inconsistency. Resetting to 0.",
                        userId, c.accountId, c.year, c.month, cnt);
            cnt = 0;
        }

        row.setIncomeTotal(income);
        row.setExpenseTotal(expense);
        row.setNetTotal(net);
        row.setCountCountable(cnt);

        accountMonthSummaryRepository.save(row);
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    // Value object describing how a single Transaction affects
    // a particular (accountId, year, month) row in account_month_summary.
    private record SummaryContribution(Long accountId, Integer year, Integer month, double income, double expense,
                                       double net, int count) {

        // Derive the monthly summary contribution for a single Transaction.
        // Returns a "zero" contribution when the transaction should not
        // affect summaries (e.g. not countable / missing data / unknown type).
        static SummaryContribution from(Transaction tx) {
            if (tx == null) {
                return new SummaryContribution(null, null, null, 0.0, 0.0, 0.0, 0);
            }
            Integer isCountable = tx.getIsCountable();
            if (isCountable == null || isCountable != 1) {
                return new SummaryContribution(null, null, null, 0.0, 0.0, 0.0, 0);
            }

            Long accountId = tx.getAccountId();
            Date date = tx.getDate();
            if (accountId == null || date == null) {
                return new SummaryContribution(null, null, null, 0.0, 0.0, 0.0, 0);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;

            double amount = tx.getAmount() != null ? tx.getAmount() : 0.0;

            double income = 0.0;
            double expense = 0.0;

            Integer type = tx.getTransactionType();
            if (type != null && type == 2) {
                income = amount;
            } else if (type != null && type == 1) {
                expense = amount;
            } else {
                return new SummaryContribution(accountId, year, month, 0.0, 0.0, 0.0, 0);
            }

            return new SummaryContribution(accountId, year, month, income, expense, income - expense, 1);
        }

        // True when this contribution has no effect on any summary row.
        boolean isZero() {
            return income == 0.0 && expense == 0.0 && net == 0.0 && count == 0;
        }
    }

    // ========== TRANSFER OPERATIONS ==========

    /**
     * Creates a regular transaction for the given user.
     * 
     * @param userId authenticated user id
     * @param request transaction request
     * @return created transaction
     */
    @Transactional
    public Transaction createTransaction(String userId, TransactionRequest request) {
        logger.info("Creating regular transaction for user {} on account {}", userId, request.accountId());

        // Validate account ownership
        if (request.accountId() == null) {
            throw new IllegalArgumentException("Transaction requires accountId");
        }
        Account acc = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new NotFoundException("Account not found: " + request.accountId()));
        
        if (!userId.equals(acc.getUserId())) {
            throw new AuthorizationException("User does not own account: " + request.accountId());
        }

        // Validate category ownership
        if (request.categoryId() == null) {
            throw new IllegalArgumentException("Transaction requires categoryId");
        }
        Category cat = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.categoryId()));
        
        if (!userId.equals(cat.getUserId())) {
            throw new AuthorizationException("User does not own category: " + request.categoryId());
        }

        // Convert request to Transaction entity
        Transaction transaction = new Transaction();
        transaction.setAccountId(request.accountId());
        transaction.setCategoryId(request.categoryId());
        transaction.setTransactionType(request.transactionType());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setName(request.name());
        transaction.setComments(request.comments());
        transaction.setIsCountable(request.isCountable());
        transaction.setOriginalCurrency(request.originalCurrency());
        transaction.setOriginalAmount(request.originalAmount());
        transaction.setExchangeRate(request.exchangeRate());
        transaction.setLinkedTransactionId(request.linkedTransactionId());

        return saveForUser(userId, transaction);
    }

    /**
     * Creates a transfer between two accounts atomically.
     * 
     * @param userId authenticated user id
     * @param fromAccountId source account
     * @param toAccountId destination account
     * @param date transfer date (applied to both sides). If null, defaults to current date.
     * @param amount transfer amount
     * @param name optional transfer name/description
     * @param comments optional comments
     * @return array containing [debitTransaction, creditTransaction]
     */
    @Transactional
    public Transaction[] createTransfer(String userId, Long fromAccountId, Long toAccountId, 
                                       Date date, Double amount, String name, String comments) {
        logger.info("Creating transfer: {} from account {} to account {} for user {}", 
                   amount, fromAccountId, toAccountId, userId);

        // Validation
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("fromAccountId and toAccountId cannot be same");
        }

        // Validate accounts ownership
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromAccountId));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + toAccountId));

        if (!userId.equals(fromAccount.getUserId())) {
            throw new AuthorizationException("User does not own source account: " + fromAccountId);
        }
        if (!userId.equals(toAccount.getUserId())) {
            throw new AuthorizationException("User does not own destination account: " + toAccountId);
        }

        // Find or create TRANSFER category
        Long transferCategoryId = getOrCreateTransferCategory(userId);

        // Use same date for both transactions (ensures consistency)
        Date transferDate = (date != null) ? date : new Date();

        // Create DEBIT transaction (money out from source account)
        Transaction debit = new Transaction();
        debit.setAccountId(fromAccountId);
        debit.setCategoryId(transferCategoryId);
        debit.setTransactionType(TYPE_DEBIT);
        debit.setAmount(amount);
        debit.setDate(transferDate);
        debit.setIsCountable(0);  // Transfers don't count as income/expense
        debit.setName(name != null ? name : "Transfer Out");
        debit.setComments(comments);

        Transaction savedDebit = saveForUser(userId, debit);

        // Validation: ensure debit was saved
        if (savedDebit == null || savedDebit.getId() == null) {
            logger.error("Failed to save debit transaction for transfer from account {} to {}", 
                        fromAccountId, toAccountId);
            throw new RuntimeException("Failed to create debit transaction");
        }

        // Create CREDIT transaction (money in to destination account)
        Transaction credit = new Transaction();
        credit.setAccountId(toAccountId);
        credit.setCategoryId(transferCategoryId);
        credit.setTransactionType(TYPE_CREDIT);
        credit.setAmount(amount);
        credit.setDate(transferDate);
        credit.setIsCountable(0);  // Transfers don't count as income/expense
        credit.setName(name != null ? name : "Transfer In");
        credit.setComments(comments);
        credit.setLinkedTransactionId(savedDebit.getId());  // Link to debit transaction

        Transaction savedCredit = saveForUser(userId, credit);

        // Validation: ensure credit was saved
        if (savedCredit == null || savedCredit.getId() == null) {
            logger.error("Failed to save credit transaction for transfer from account {} to {}", 
                        fromAccountId, toAccountId);
            throw new RuntimeException("Failed to create credit transaction");
        }

        // Update debit to link back to credit (bidirectional link)
        savedDebit.setLinkedTransactionId(savedCredit.getId());
        saveForUser(userId, savedDebit);

        logger.info("Transfer created successfully: {} from account {} to account {} (debit: {}, credit: {})",
                   amount, fromAccountId, toAccountId, savedDebit.getId(), savedCredit.getId());

        return new Transaction[]{savedDebit, savedCredit};
    }

    /**
     * Deletes a transfer (both linked transactions) atomically.
     * 
     * @param userId authenticated user id
     * @param transactionId either the debit or credit transaction id
     */
    @Transactional
    public void deleteTransfer(String userId, Long transactionId) {
        logger.info("Deleting transfer containing transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        // Verify this is actually a transfer
        if (transaction.getLinkedTransactionId() == null) {
            throw new IllegalArgumentException("Transaction " + transactionId + " is not part of a transfer");
        }

        Long linkedId = transaction.getLinkedTransactionId();
        Transaction linkedTx = transactionRepository.findById(linkedId)
                .orElseThrow(() -> new NotFoundException("Linked transaction not found: " + linkedId));

        // Verify ownership of both transactions
        Account account = accountRepository.findById(transaction.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + transaction.getAccountId()));
        Account linkedAccount = accountRepository.findById(linkedTx.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + linkedTx.getAccountId()));

        if (!userId.equals(account.getUserId())) {
            throw new AuthorizationException("User does not own account: " + account.getId());
        }
        if (!userId.equals(linkedAccount.getUserId())) {
            throw new AuthorizationException("User does not own linked account: " + linkedAccount.getId());
        }

        // Delete both transactions atomically
        logger.info("Deleting transfer: transaction {} and linked transaction {}", transactionId, linkedId);
        deleteForUser(userId, transactionId);
        deleteForUser(userId, linkedId);

        logger.info("Transfer deleted successfully: transactions {} and {}", transactionId, linkedId);
    }

    /**
     * Updates an existing transaction (regular or transfer).
     * Validates ownership and delegates to appropriate update method.
     * 
     * @param userId authenticated user id
     * @param transaction the transaction to update
     * @return updated transaction
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Transaction updateTransaction(String userId, Transaction transaction) {
        if (transaction.getId() == null) {
            throw new IllegalArgumentException("Transaction ID is required for update");
        }

        Transaction existing = transactionRepository.findById(transaction.getId())
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transaction.getId()));

        // Verify ownership of existing transaction
        Account existingAccount = accountRepository.findById(existing.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + existing.getAccountId()));
        
        if (!userId.equals(existingAccount.getUserId())) {
            throw new AuthorizationException("User does not own this transaction");
        }

        // Check if this is a transfer
        if (existing.getLinkedTransactionId() != null) {
            // This is a TRANSFER - handle both sides atomically
            logger.info("Updating transfer transaction {}", transaction.getId());
            
            Transaction linkedTx = transactionRepository.findById(existing.getLinkedTransactionId())
                    .orElseThrow(() -> new IllegalArgumentException("Linked transaction not found: " + existing.getLinkedTransactionId()));

            // Determine which is debit and which is credit
            boolean isDebit = existing.getTransactionType() != null && existing.getTransactionType() == 1;
            Long fromAccountId = isDebit ? transaction.getAccountId() : linkedTx.getAccountId();
            Long toAccountId = isDebit ? linkedTx.getAccountId() : transaction.getAccountId();

            // Update the transfer
            Transaction[] result = updateTransfer(
                userId,
                transaction.getId(),
                fromAccountId,
                toAccountId,
                transaction.getDate(),
                transaction.getAmount(),
                transaction.getName(),
                transaction.getComments()
            );

            // Return the transaction that was requested
            return result[0].getId().equals(transaction.getId()) ? result[0] : result[1];
        } else {
            // This is a REGULAR TRANSACTION
            logger.info("Updating regular transaction {}", transaction.getId());
            
            // Verify new account ownership
            Account newAccount = accountRepository.findById(transaction.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + transaction.getAccountId()));
            
            if (!userId.equals(newAccount.getUserId())) {
                throw new AuthorizationException("User does not own the target account: " + transaction.getAccountId());
            }

            // Verify new category ownership
            if (transaction.getCategoryId() == null) {
                throw new IllegalArgumentException("Category is required");
            }
            
            var categories = categoryRepository.findById(transaction.getCategoryId());
            if (categories.isEmpty()) {
                throw new IllegalArgumentException("Category not found: " + transaction.getCategoryId());
            }
            
            Category category = categories.get();
            if (!userId.equals(category.getUserId())) {
                throw new AuthorizationException("User does not own the category: " + transaction.getCategoryId());
            }

            // Save the transaction
            return saveForUser(userId, transaction);
        }
    }

    /**
     * Updates a transfer between two accounts atomically.
     * This updates both the debit and credit sides of the transfer.
     * 
     * @param userId authenticated user id
     * @param transactionId id of either the debit or credit transaction
     * @param fromAccountId new source account (or null to keep existing)
     * @param toAccountId new destination account (or null to keep existing)
     * @param date new transfer date (or null to keep existing)
     * @param amount new transfer amount (or null to keep existing)
     * @param name new transfer name/description (or null to keep existing)
     * @param comments new comments (or null to keep existing)
     * @return array containing updated [debitTransaction, creditTransaction]
     */
    @Transactional
    public Transaction[] updateTransfer(String userId, Long transactionId, 
                                       Long fromAccountId, Long toAccountId,
                                       Date date,
                                       Double amount, String name, String comments) {
        logger.info("Updating transfer containing transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Verify this is actually a transfer
        if (transaction.getLinkedTransactionId() == null) {
            throw new IllegalArgumentException("Transaction " + transactionId + " is not part of a transfer");
        }

        Long linkedId = transaction.getLinkedTransactionId();
        Transaction linkedTx = transactionRepository.findById(linkedId)
                .orElseThrow(() -> new IllegalArgumentException("Linked transaction not found: " + linkedId));

        // Determine which is debit and which is credit
        Transaction debit = transaction.getTransactionType() == TYPE_DEBIT ? transaction : linkedTx;
        Transaction credit = transaction.getTransactionType() == TYPE_CREDIT ? transaction : linkedTx;

        // Verify ownership of both accounts
        Account debitAccount = accountRepository.findById(debit.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + debit.getAccountId()));
        Account creditAccount = accountRepository.findById(credit.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + credit.getAccountId()));

        if (!userId.equals(debitAccount.getUserId()) || !userId.equals(creditAccount.getUserId())) {
            throw new AuthorizationException("User does not own one or both accounts in the transfer");
        }

        // Update fields if provided
        Date newDate = (date != null) ? date : transaction.getDate(); // Keep same date for both sides

        if (date != null) {
            debit.setDate(newDate);
            credit.setDate(newDate);
        }
        
        if (amount != null && amount > 0) {
            debit.setAmount(amount);
            credit.setAmount(amount);
        }
        
        if (name != null) {
            debit.setName(name);
            credit.setName(name);
        }
        
        if (comments != null) {
            debit.setComments(comments);
            credit.setComments(comments);
        }

        // Handle account changes if provided
        Long newDebitAccountId = debit.getAccountId();
        if (fromAccountId != null && !fromAccountId.equals(debit.getAccountId())) {
            Account newFromAccount = accountRepository.findById(fromAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromAccountId));
            if (!userId.equals(newFromAccount.getUserId())) {
                throw new AuthorizationException("User does not own source account: " + fromAccountId);
            }
            newDebitAccountId = fromAccountId;
        }
        
        Long newCreditAccountId = credit.getAccountId();
        if (toAccountId != null && !toAccountId.equals(credit.getAccountId())) {
            Account newToAccount = accountRepository.findById(toAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + toAccountId));
            if (!userId.equals(newToAccount.getUserId())) {
                throw new AuthorizationException("User does not own destination account: " + toAccountId);
            }
            newCreditAccountId = toAccountId;
        }

        // Validate accounts are different
        if (newDebitAccountId.equals(newCreditAccountId)) {
            throw new IllegalArgumentException("Transfer source and destination accounts cannot be the same");
        }

        // Apply account changes
        if (fromAccountId != null && !fromAccountId.equals(debit.getAccountId())) {
            debit.setAccountId(fromAccountId);
        }
        if (toAccountId != null && !toAccountId.equals(credit.getAccountId())) {
            credit.setAccountId(toAccountId);
        }

        // Save both transactions
        Transaction updatedDebit = saveForUser(userId, debit);
        Transaction updatedCredit = saveForUser(userId, credit);

        logger.info("Transfer updated successfully: transactions {} and {}", updatedDebit.getId(), updatedCredit.getId());

        return new Transaction[]{updatedDebit, updatedCredit};
    }

    /**
     * Checks if a transaction is part of a transfer.
     */
    public boolean isTransfer(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .map(tx -> tx.getLinkedTransactionId() != null)
                .orElse(false);
    }

    /**
     * Gets or creates the TRANSFER category for a user.
     */
    private Long getOrCreateTransferCategory(String userId) {
        var catList = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (!catList.isEmpty()) {
            return catList.get(0).getId();
        }

        logger.info("Auto-creating TRANSFER category for user: {}", userId);
        com.trako.entities.Category category = new com.trako.entities.Category();
        category.setName("TRANSFER");
        category.setUserId(userId);
        com.trako.entities.Category saved = categoryRepository.save(category);
        return saved.getId();
    }
}
