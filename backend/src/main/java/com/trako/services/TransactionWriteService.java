package com.trako.services;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.BadRequestException;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.*;
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
    private CategoryRepository categoryRepository;

    @Autowired
    private CurrencyService currencyService;

    private void validateAccountOwnership(String userId, Long accountId) {
        if (accountId == null) return;
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Account not found: " + accountId));
        if (!userId.equals(account.getUserId())) {
            throw new AuthorizationException("User does not own account: " + accountId);
        }
    }

    private void validateCategoryOwnership(String userId, Long categoryId) {
        if (categoryId == null) return;
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Category not found: " + categoryId));
        if (!userId.equals(category.getUserId())) {
            throw new AuthorizationException("User does not own category: " + categoryId);
        }
    }

    /**
     * Unified entry point for updating a transaction or transfer.
     * Handles all branching logic for conversions and partial updates.
     */
    @Transactional
    public Transaction updateTransaction(String userId, Long id, TransactionRequest request) {
        Transaction existing = transactionRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Transaction not found")
        );

        // Verify basic ownership of the transaction via its account
        validateAccountOwnership(userId, existing.getAccountId());

        boolean isExistingTransfer = existing.getLinkedTransactionId() != null;
        
        // CASE 1: Convert Regular Transaction -> Transfer
        if (!isExistingTransfer && request.toAccountId() != null) {
            return convertRegularToTransfer(userId, id, request)[0];
        }

        // CASE 2: Convert Transfer -> Regular Transaction
        // If it's a transfer and we are changing the category, we assume conversion to regular.
        if (isExistingTransfer && request.categoryId() != null) {
            return convertTransferToRegular(userId, id, request);
        }

        // CASE 3: Updating a TRANSFER (that stays a transfer)
        if (isExistingTransfer || request.isTransfer()) {
            return handleTransferUpdate(userId, existing, request);
        }

        // CASE 4: Updating a REGULAR TRANSACTION
        return handleRegularTransactionUpdate(userId, existing, request);
    }

    private Transaction handleTransferUpdate(String userId, Transaction existing, TransactionRequest request) {
        boolean isDebitSide = existing.getTransactionType() == TransactionType.DEBIT;
        boolean isCreditSide = existing.getTransactionType() == TransactionType.CREDIT;

        Long resolvedFromAccountId = request.fromAccountId();
        Long resolvedToAccountId = request.toAccountId();

        if (resolvedFromAccountId == null && isDebitSide) {
            resolvedFromAccountId = request.accountId();
        }
        if (resolvedToAccountId == null && isCreditSide) {
            resolvedToAccountId = request.accountId();
        }

        String currency = request.originalCurrency() != null ? request.originalCurrency() : existing.getOriginalCurrency();
        Double rate = request.exchangeRate();
        
        if (request.originalCurrency() != null) {
             rate = currencyService.resolveExchangeRate(userId, currency, request.exchangeRate());
        } else {
             if (rate == null) {
                 rate = existing.getExchangeRate();
             }
        }

        if (request.originalAmount() != null && request.originalAmount() <= 0) {
            throw new IllegalArgumentException("Original amount must be greater than 0");
        }

        Transaction[] result = updateTransfer(
            userId,
            existing.getId(),
            resolvedFromAccountId,
            resolvedToAccountId,
            request.date(),
            request.originalAmount(),
            currency,
            rate,
            request.name(),
            request.comments()
        );

        return result[0].getId().equals(existing.getId()) ? result[0] : result[1];
    }

    private Transaction handleRegularTransactionUpdate(String userId, Transaction existing, TransactionRequest request) {
        Transaction txToUpdate = new Transaction();
        txToUpdate.setId(existing.getId());
        
        // Apply updates or keep existing
        txToUpdate.setAccountId(request.accountId() != null ? request.accountId() : existing.getAccountId());
        txToUpdate.setCategoryId(request.categoryId() != null ? request.categoryId() : existing.getCategoryId());
        txToUpdate.setTransactionType(request.transactionType() != null ? request.transactionType() : existing.getTransactionType());
        
        // Currency resolution
        String newCurrency = request.originalCurrency();
        Double newRate = request.exchangeRate();
        
        if (newCurrency != null) {
             newRate = currencyService.resolveExchangeRate(userId, newCurrency, newRate);
        } else {
            newCurrency = existing.getOriginalCurrency();
            if (newRate == null) {
                newRate = existing.getExchangeRate();
            }
        }
        
        txToUpdate.setOriginalCurrency(newCurrency);
        txToUpdate.setExchangeRate(newRate);
        
        if (request.originalAmount() != null) {
            if (request.originalAmount() <= 0) {
                throw new IllegalArgumentException("Original amount must be greater than 0");
            }
            txToUpdate.setOriginalAmount(request.originalAmount());
        } else {
            txToUpdate.setOriginalAmount(existing.getOriginalAmount());
        }

        txToUpdate.setName(request.name() != null ? request.name() : existing.getName());
        txToUpdate.setComments(request.comments() != null ? request.comments() : existing.getComments());
        txToUpdate.setDate(request.date() != null ? request.date() : existing.getDate());
        txToUpdate.setIsCountable(request.isCountable() != null ? request.isCountable() : existing.getIsCountable());
        txToUpdate.setLinkedTransactionId(existing.getLinkedTransactionId());

        return performRegularTransactionUpdate(userId, txToUpdate);
    }


    // private static final int TYPE_DEBIT = 1;
    // private static final int TYPE_CREDIT = 2;

    @Transactional
    public Transaction saveForUser(String userId, Transaction transaction) {
        if (transaction.getAccountId() != null) {
            validateAccountOwnership(userId, transaction.getAccountId());
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
        
        validateAccountOwnership(userId, transaction.getAccountId());
        
        transactionRepository.delete(transaction);
    }


    // ========== TRANSFER OPERATIONS ==========

    /**
     * Unified entry point for creating a transaction or transfer.
     * Handles all branching logic based on request data.
     * 
     * @return The created Transaction (or the debit side if it's a transfer)
     */
    @Transactional
    public Transaction createUnifiedTransaction(String userId, TransactionRequest request) {
        // Check if this is a transfer request (has toAccountId field)
        if (request.isTransfer()) {
            // This is a TRANSFER request
            logger.info("Processing transfer request from account {} to account {}", 
                    request.getSourceAccountId(), request.toAccountId());
            
            // Validate required fields
            Long fromAccountId = request.getSourceAccountId();
            if (fromAccountId == null) {
                throw new IllegalArgumentException("Transfer requires fromAccountId or accountId");
            }
            if (request.toAccountId() == null) {
                throw new IllegalArgumentException("Transfer requires toAccountId");
            }
            // Validate same account
            if (fromAccountId.equals(request.toAccountId())) {
                throw new IllegalArgumentException("fromAccountId and toAccountId cannot be same");
            }
            
            // Validate Currency and Amount
            if (request.originalCurrency() == null) {
                throw new IllegalArgumentException("Original currency is required");
            }
            if (request.originalAmount() == null || request.originalAmount() <= 0) {
                 throw new IllegalArgumentException("Original amount must be greater than 0");
            }

            Double exchangeRate = currencyService.resolveExchangeRate(userId, request.originalCurrency(), request.exchangeRate());

            // Delegate to internal transfer creation
            Transaction[] result = createTransfer(
                userId,
                fromAccountId,
                request.toAccountId(),
                request.date(),
                request.originalAmount(),
                request.originalCurrency(),
                exchangeRate,
                request.name(),
                request.comments()
            );
            
            // Return the debit side
            return result[0];
            
        } else {
            // This is a REGULAR TRANSACTION request
            if (request.originalCurrency() == null) {
                throw new IllegalArgumentException("Original currency is required");
            }
            if (request.originalAmount() == null) {
                 throw new IllegalArgumentException("Original amount is required");
            }
            if (request.originalAmount() <= 0) {
                 throw new IllegalArgumentException("Original amount must be greater than 0");
            }
            
            Double exchangeRate = currencyService.resolveExchangeRate(userId, request.originalCurrency(), request.exchangeRate());

            // Create enriched request with resolved exchange rate
            TransactionRequest enrichedRequest = new TransactionRequest(
                request.id(),
                request.accountId(),
                request.date(),
                request.name(),
                request.comments(),
                request.categoryId(),
                request.transactionType(),
                request.isCountable(),
                request.originalCurrency(),
                request.originalAmount(),
                exchangeRate,
                request.linkedTransactionId(),
                request.toAccountId(),
                request.fromAccountId()
            );

            return createTransaction(userId, enrichedRequest);
        }
    }

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
        validateAccountOwnership(userId, request.accountId());

        // Validate category ownership
        if (request.categoryId() == null) {
            throw new IllegalArgumentException("Transaction requires categoryId");
        }
        validateCategoryOwnership(userId, request.categoryId());

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
        
        // Only support originalAmount; no fallback to legacy 'amount'
        transaction.setOriginalAmount(request.originalAmount());
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
        validateAccountOwnership(userId, fromAccountId);
        validateAccountOwnership(userId, toAccountId);

        // Find or create TRANSFER category
        Long transferCategoryId = getOrCreateTransferCategory(userId);

        // Use same date for both transactions (ensures consistency)
        Date transferDate = (date != null) ? date : new Date();

        // Create DEBIT transaction (money out from source account)
        Transaction debit = new Transaction();
        debit.setAccountId(fromAccountId);
        debit.setCategoryId(transferCategoryId);
        debit.setTransactionType(TransactionType.DEBIT);
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
        credit.setTransactionType(TransactionType.CREDIT);
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
     * Unified entry point for deleting a transaction or transfer.
     * Handles checking if it's a transfer and delegating accordingly.
     */
    @Transactional
    public void deleteUnifiedTransaction(String userId, Long id) {
        Transaction tx = transactionRepository.findById(id)
             .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // We delegate to specific methods which will re-fetch and verify ownership/locking.
        // This is slightly inefficient (double fetch) but safe and consistent with existing granular methods.
        if (tx.getLinkedTransactionId() != null) {
             deleteTransfer(userId, id);
        } else {
             deleteForUser(userId, id);
        }
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
        validateAccountOwnership(userId, transaction.getAccountId());
        validateAccountOwnership(userId, linkedTx.getAccountId());

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
    public Transaction performRegularTransactionUpdate(String userId, Transaction transaction) {
        if (transaction.getId() == null) {
            throw new IllegalArgumentException("Transaction ID is required for update");
        }

        // We assume transaction.getId() exists and verify ownership
        // Note: The caller (handleRegularTransactionUpdate) constructs 'transaction' with the ID of an existing one.
        // But we should re-verify if we want to be safe, or assume caller did it.
        // For safety, let's fetch the original to ensure it exists and check ownership of the *target* account.
        
        // Verify new account ownership
        validateAccountOwnership(userId, transaction.getAccountId());

        // Verify new category ownership
        if (transaction.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        validateCategoryOwnership(userId, transaction.getCategoryId());

        // Save the transaction
        return saveForUser(userId, transaction);
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
        Transaction debit = transaction.getTransactionType() != null && transaction.getTransactionType() == TransactionType.DEBIT ? transaction : linkedTx;
        Transaction credit = transaction.getTransactionType() != null && transaction.getTransactionType() == TransactionType.CREDIT ? transaction : linkedTx;

        // Verify ownership of both accounts
        validateAccountOwnership(userId, debit.getAccountId());
        validateAccountOwnership(userId, credit.getAccountId());

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
            validateAccountOwnership(userId, fromAccountId);
            newDebitAccountId = fromAccountId;
        }
        
        Long newCreditAccountId = credit.getAccountId();
        if (toAccountId != null && !toAccountId.equals(credit.getAccountId())) {
            validateAccountOwnership(userId, toAccountId);
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
     * Converts a regular transaction into a transfer.
     * 
     * @param userId authenticated user id
     * @param transactionId id of the regular transaction
     * @param request the update request containing toAccountId and other field updates
     * @return array containing [debit, credit]
     */
    @Transactional
    public Transaction[] convertRegularToTransfer(String userId, Long transactionId, TransactionRequest request) {
        Long toAccountId = request.toAccountId();
        logger.info("Converting regular transaction {} to transfer (to account {}) for user {}", transactionId, toAccountId, userId);

        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (existing.getLinkedTransactionId() != null) {
            throw new IllegalArgumentException("Transaction is already a transfer");
        }

        // Validate account ownership
        validateAccountOwnership(userId, existing.getAccountId());
        validateAccountOwnership(userId, toAccountId);
        
        if (existing.getAccountId().equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        // Apply updates from request to the "existing" transaction (which becomes DEBIT/Source side)
        if (request.date() != null) existing.setDate(request.date());
        if (request.name() != null) existing.setName(request.name());
        if (request.comments() != null) existing.setComments(request.comments());
        if (request.originalAmount() != null) existing.setOriginalAmount(request.originalAmount());
        if (request.originalCurrency() != null) existing.setOriginalCurrency(request.originalCurrency());
        if (request.exchangeRate() != null) existing.setExchangeRate(request.exchangeRate());

        // Determine Transfer Category
        Long transferCategoryId = getOrCreateTransferCategory(userId);

        // Update the existing transaction to be the DEBIT side of the transfer
        existing.setCategoryId(transferCategoryId);
        existing.setTransactionType(TransactionType.DEBIT);
        existing.setIsCountable(0); // Transfers are not countable

        // Create mate (CREDIT side)
        Transaction credit = new Transaction();
        credit.setAccountId(toAccountId);
        credit.setCategoryId(transferCategoryId);
        credit.setTransactionType(TransactionType.CREDIT);
        // Copy fields from updated existing
        credit.setOriginalAmount(existing.getOriginalAmount());
        credit.setOriginalCurrency(existing.getOriginalCurrency());
        credit.setExchangeRate(existing.getExchangeRate());
        credit.setDate(existing.getDate());
        credit.setIsCountable(0);
        credit.setName(existing.getName());
        credit.setComments(existing.getComments());
        
        Transaction savedCredit = saveForUser(userId, credit); // Save first to get ID
        
        // Link them
        existing.setLinkedTransactionId(savedCredit.getId());
        Transaction savedDebit = saveForUser(userId, existing);
        
        savedCredit.setLinkedTransactionId(savedDebit.getId());
        saveForUser(userId, savedCredit);

        return new Transaction[]{savedDebit, savedCredit};
    }

    /**
     * Converts a transfer transaction into a regular transaction.
     * The linked transaction (mate) is deleted.
     * 
     * @param userId authenticated user id
     * @param transactionId id of the transaction to keep (converted to regular)
     * @param request updates to apply during conversion (category, type, etc.)
     * @return updated regular transaction
     */
    @Transactional
    public Transaction convertTransferToRegular(String userId, Long transactionId, TransactionRequest request) {
        logger.info("Converting transfer transaction {} to regular for user {}", transactionId, userId);

        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (existing.getLinkedTransactionId() == null) {
            throw new IllegalArgumentException("Transaction is not a transfer");
        }

        Long linkedId = existing.getLinkedTransactionId();
        Transaction linkedTx = transactionRepository.findById(linkedId)
                .orElseThrow(() -> new NotFoundException("Linked transaction not found: " + linkedId));

        // Verify ownership
        validateAccountOwnership(userId, existing.getAccountId());

        // Unlink existing
        existing.setLinkedTransactionId(null);
        
        // Apply updates from request
        if (request.categoryId() != null) {
            existing.setCategoryId(request.categoryId());
        }
        if (request.transactionType() != null) {
            existing.setTransactionType(request.transactionType());
        }
        if (request.name() != null) {
            existing.setName(request.name());
        }
        if (request.isCountable() != null) {
            existing.setIsCountable(request.isCountable());
        } else {
            // Default to countable if not specified when converting to regular?
            existing.setIsCountable(1); 
        }
        
        // Save the one we keep
        Transaction saved = saveForUser(userId, existing);

        // Delete the mate
        deleteForUser(userId, linkedId);

        return saved;
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
