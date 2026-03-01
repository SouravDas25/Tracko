package com.trako.services;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.UserCurrency;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UserCurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Write-oriented service for {@link Transaction} mutations.
 *
 * <p>This is the single supported write-path for creating/updating/deleting transactions.
 */
@Service
public class TransactionWriteService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionWriteService.class);

    // Single write-path for Transaction mutations.

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

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
     * <p>Validates that the target account belongs to the user before persistence.
     *
     * @param userId      authenticated user id
     * @param transaction transaction payload
     * @return persisted transaction
     * @throws AuthorizationException if user doesn't own the target account
     */
    public Transaction saveForUser(String userId, Transaction transaction) {
        if (transaction.getAccountId() != null) {
            Account acc = accountRepository.findById(transaction.getAccountId())
                    .orElseThrow(() -> new NotFoundException("Account not found: " + transaction.getAccountId()));
            if (!userId.equals(acc.getUserId())) {
                throw new AuthorizationException("User does not own account: " + transaction.getAccountId());
            }
        }
        Transaction persisted = transactionRepository.saveAndFlush(transaction);
        // Reload to ensure DB-computed columns like 'amount' are populated
        return transactionRepository.findById(persisted.getId()).orElse(persisted);
    }

    @Transactional
    /**
     * Deletes a transaction for the given user.
     *
     * @param userId        authenticated user id
     * @param transactionId id of the transaction to delete
     * @throws AuthorizationException if user doesn't own the transaction
     */
    public void deleteForUser(String userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
        
        Account acc = accountRepository.findById(transaction.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found: " + transaction.getAccountId()));
        
        if (!userId.equals(acc.getUserId())) {
            throw new AuthorizationException("User does not own transaction: " + transactionId);
        }
        
        transactionRepository.delete(transaction);
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

        // Validate currency fields
        if (request.originalCurrency() == null) {
            throw new IllegalArgumentException("Original currency is required");
        }
        if (request.originalAmount() == null) {
            throw new IllegalArgumentException("Original amount is required");
        }
        if (request.exchangeRate() == null) {
            throw new IllegalArgumentException("Exchange rate is required");
        }

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
        // Amount is computed by DB
        transaction.setDate(request.date());
        transaction.setName(request.name());
        transaction.setComments(request.comments());
        transaction.setIsCountable(request.isCountable());
        
        // Handle case where request might have amount instead of originalAmount (for backward compatibility in tests)
        Double originalAmount = request.originalAmount() != null ? request.originalAmount() : request.amount();
        transaction.setOriginalAmount(originalAmount);
        transaction.setOriginalCurrency(request.originalCurrency());
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
     * @param originalAmount transfer amount
     * @param name optional transfer name/description
     * @param comments optional comments
     * @return array containing [debitTransaction, creditTransaction]
     */
    @Transactional
    public Transaction[] createTransfer(String userId, Long fromAccountId, Long toAccountId, 
                                       Date date, Double originalAmount, String originalCurrency, Double exchangeRate, String name, String comments) {
        logger.info("Creating transfer: {} {} from account {} to account {} for user {}", 
                   originalAmount, originalCurrency, fromAccountId, toAccountId, userId);

        // Validate currency fields
        if (originalCurrency == null) {
            throw new IllegalArgumentException("Original currency is required for transfer");
        }
        if (originalAmount == null) {
            throw new IllegalArgumentException("Original amount is required for transfer");
        }
        if (exchangeRate == null) {
            throw new IllegalArgumentException("Exchange rate is required for transfer");
        }

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
        debit.setOriginalAmount(originalAmount);
        debit.setOriginalCurrency(originalCurrency);
        debit.setExchangeRate(exchangeRate);
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
        credit.setOriginalAmount(originalAmount);
        credit.setOriginalCurrency(originalCurrency);
        credit.setExchangeRate(exchangeRate);
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

        logger.info("Transfer created successfully: {} {} from account {} to account {} (debit: {}, credit: {})",
                   originalAmount, originalCurrency, fromAccountId, toAccountId, savedDebit.getId(), savedCredit.getId());

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
     * Validates ownership and delegates to the appropriate update method.
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

        // Verify ownership of an existing transaction
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
                transaction.getOriginalAmount(),
                transaction.getOriginalCurrency(),
                transaction.getExchangeRate(),
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
     * @param originalAmount new transfer amount (or null to keep existing)
     * @param name new transfer name/description (or null to keep existing)
     * @param comments new comments (or null to keep existing)
     * @return array containing updated [debitTransaction, creditTransaction]
     */
    @Transactional
    public Transaction[] updateTransfer(String userId, Long transactionId, 
                                       Long fromAccountId, Long toAccountId,
                                       Date date,
                                       Double originalAmount, String originalCurrency, Double exchangeRate, String name, String comments) {
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
        Transaction debit = transaction.getTransactionType() != null && transaction.getTransactionType() == TYPE_DEBIT ? transaction : linkedTx;
        Transaction credit = transaction.getTransactionType() != null && transaction.getTransactionType() == TYPE_CREDIT ? transaction : linkedTx;

        // Verify ownership of both accounts
        Account debitAccount = accountRepository.findById(debit.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + debit.getAccountId()));
        Account creditAccount = accountRepository.findById(credit.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + credit.getAccountId()));

        if (!userId.equals(debitAccount.getUserId()) || !userId.equals(creditAccount.getUserId())) {
            throw new AuthorizationException("User does not own one or both accounts in the transfer");
        }

        // Update fields if provided
        if (date != null) {
            debit.setDate(date);
            credit.setDate(date);
        }
        
        if (originalAmount != null && originalAmount > 0) {
            debit.setOriginalAmount(originalAmount);
            credit.setOriginalAmount(originalAmount);
        }
        
        if (originalCurrency != null) {
            debit.setOriginalCurrency(originalCurrency);
            credit.setOriginalCurrency(originalCurrency);
        }
        
        if (exchangeRate != null && exchangeRate > 0) {
            debit.setExchangeRate(exchangeRate);
            credit.setExchangeRate(exchangeRate);
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
     * 
     * <p>This method is synchronized at the user-level to prevent duplicate creation
     * during concurrent transfer requests.
     */
    private synchronized Long getOrCreateTransferCategory(String userId) {
        var catList = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (!catList.isEmpty()) {
            return catList.get(0).getId();
        }

        logger.info("Auto-creating TRANSFER category for user: {}", userId);
        Category category = new Category();
        category.setName("TRANSFER");
        category.setUserId(userId);
        Category saved = categoryRepository.save(category);
        return saved.getId();
    }
}
