package com.trako.services.transactions;

import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.TransactionRepository;
import com.trako.services.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-oriented service for {@link Transaction} mutations.
 *
 * <p>This is the single supported write-path for creating/updating/deleting transactions.
 * It acts as a facade, delegating to specific services based on transaction type.
 */
@Service
public class TransactionWriteService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionWriteService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private CreditTransactionService creditTransactionService;

    @Autowired
    private DebitTransactionService debitTransactionService;

    @Autowired
    private TransactionValidationService validationService;

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
            return createTransfer(userId, request);
        } else {
            // REGULAR TRANSACTION
            if (request.transactionType() == TransactionType.CREDIT) {
                return creditTransactionService.createCreditTransaction(userId, request);
            } else {
                // Default to DEBIT if not specified or explicitly DEBIT
                return debitTransactionService.createDebitTransaction(userId, request);
            }
        }
    }

    private Transaction createTransfer(String userId, TransactionRequest request) {
        logger.info("Processing transfer request from account {} to account {}",
                request.getSourceAccountId(), request.toAccountId());

        // Validate required fields for transfer wrapper
        Long fromAccountId = request.getSourceAccountId();
        if (fromAccountId == null) {
            throw new IllegalArgumentException("Transfer requires fromAccountId or accountId");
        }
        if (request.toAccountId() == null) {
            throw new IllegalArgumentException("Transfer requires toAccountId");
        }
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
        validationService.validateAccountOwnership(userId, existing.getAccountId());

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
        // If a new transactionType is provided, it controls which path we take (DEBIT vs CREDIT).
        TransactionType targetType = request.transactionType() != null
                ? request.transactionType()
                : existing.getTransactionType();

        if (targetType == TransactionType.CREDIT) {
            return creditTransactionService.updateCreditTransaction(userId, existing, request);
        } else {
            // Default to DEBIT when null or explicitly DEBIT
            return debitTransactionService.updateDebitTransaction(userId, existing, request);
        }
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

    /**
     * Unified entry point for deleting a transaction or transfer.
     * Handles checking if it's a transfer and delegating accordingly.
     */
    @Transactional
    public void deleteUnifiedTransaction(String userId, Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // We delegate to specific methods which will re-fetch and verify ownership/locking.
        if (tx.getLinkedTransactionId() != null) {
            transferService.deleteTransfer(userId, id);
        } else {
            deleteForUser(userId, id);
        }
    }

    @Transactional
    public void deleteForUser(String userId, Long transactionId) {
        Transaction transaction = validationService.validateTransactionOwnership(userId, transactionId);
        transactionRepository.delete(transaction);
    }

    /**
     * Creates a regular transaction for the given user.
     * Retained for backward compatibility.
     *
     * @param userId  authenticated user id
     * @param request transaction request
     * @return created transaction
     */
    @Transactional
    public Transaction createTransaction(String userId, TransactionRequest request) {
        if (request.transactionType() == TransactionType.CREDIT) {
            return creditTransactionService.createCreditTransaction(userId, request);
        } else {
            return debitTransactionService.createDebitTransaction(userId, request);
        }
    }

    /**
     * Saves a transaction for the given user.
     * Retained for backward compatibility and internal use by tests.
     */
    @Transactional
    public Transaction saveForUser(String userId, Transaction transaction) {
        if (transaction.getAccountId() != null) {
            validationService.validateAccountOwnership(userId, transaction.getAccountId());
        }
        Transaction persisted = transactionRepository.saveAndFlush(transaction);
        return transactionRepository.findById(persisted.getId()).orElse(persisted);
    }
}
