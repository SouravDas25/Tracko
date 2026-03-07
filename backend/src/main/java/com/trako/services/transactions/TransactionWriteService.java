package com.trako.services.transactions;

import com.trako.dtos.TransferResult;
import com.trako.entities.Transaction;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
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
    private TransferConversionService transferConversionService;

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
        if (request.transactionType() == null) {
            throw new IllegalArgumentException("transactionType cannot be null");
        }
        if (request.transactionType() == TransactionType.TRANSFER) {
            return createTransfer(userId, request);
        } else {
            return createRegularTransaction(userId, request);
        }
    }

    private Transaction createRegularTransaction(String userId, TransactionRequest request) {
        validationService.validateCreateTransaction(request);
        if (request.transactionType() == TransactionType.CREDIT) {
            return creditTransactionService.createCreditTransaction(userId, request);
        } else {
            return debitTransactionService.createDebitTransaction(userId, request);
        }
    }

    private Transaction createTransfer(String userId, TransactionRequest request) {
        validationService.validateCreateTransfer(request);

        logger.info("Processing transfer request from account {} to account {}",
                request.getSourceAccountId(), request.toAccountId());

        Double exchangeRate = currencyService.resolveExchangeRate(userId, request.originalCurrency(), request.exchangeRate());

        // Delegate to internal transfer creation
        TransferResult result = transferService.createTransfer(
                userId,
                request.getSourceAccountId(),
                request.toAccountId(),
                request.date(),
                request.originalAmount(),
                request.originalCurrency(),
                exchangeRate,
                request.name(),
                request.comments()
        );

        // Return the debit side
        return result.debit();
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
        validationService.validateAccountOwnership(userId, existing.getAccountId());

        boolean isExistingTransfer = existing.getLinkedTransactionId() != null;
        TransactionType requestedType = request.transactionType();

        // Null transactionType means "no type change" — resolve from existing
        if (requestedType == null) {
            if (isExistingTransfer) {
                requestedType = TransactionType.TRANSFER;
            } else {
                requestedType = TransactionType.fromValue(existing.getTransactionType().getValue());
            }
        }

        if (isExistingTransfer && requestedType == TransactionType.TRANSFER) {
            // Transfer stays a transfer
            validationService.validateUpdateTransfer(request);
            return handleTransferUpdate(userId, existing, request);
        } else if (isExistingTransfer) {
            // Transfer → Regular (DEBIT or CREDIT)
            validationService.validateConvertToTransaction(request, existing);
            return transferConversionService.convertTransferToRegular(userId, id, request);
        } else if (requestedType == TransactionType.TRANSFER) {
            // Regular → Transfer
            validationService.validateConvertToTransfer(request, existing);
            return transferConversionService.convertRegularToTransfer(userId, id, request).debit();
        } else {
            // Regular stays/changes to DEBIT or CREDIT
            validationService.validateUpdateTransaction(request);
            if (requestedType == TransactionType.CREDIT) {
                return creditTransactionService.updateCreditTransaction(userId, existing, request);
            } else {
                return debitTransactionService.updateDebitTransaction(userId, existing, request);
            }
        }
    }

    private Transaction handleTransferUpdate(String userId, Transaction existing, TransactionRequest request) {
        boolean isDebitSide = existing.getTransactionType() == TransactionDbType.DEBIT;
        boolean isCreditSide = existing.getTransactionType() == TransactionDbType.CREDIT;

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

        TransferResult result = transferService.updateTransfer(
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

        return result.getById(existing.getId());
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
