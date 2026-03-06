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

    @Autowired
    private TransferService transferService;

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
            return transferService.convertRegularToTransfer(userId, id, request)[0];
        }

        // CASE 2: Convert Transfer -> Regular Transaction
        // If it's a transfer and we are changing the category, we assume conversion to regular.
        if (isExistingTransfer && request.categoryId() != null) {
            return transferService.convertTransferToRegular(userId, id, request);
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

        Transaction[] result = transferService.updateTransfer(
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
            Transaction[] result = transferService.createTransfer(
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
             transferService.deleteTransfer(userId, id);
        } else {
             deleteForUser(userId, id);
        }
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
}
