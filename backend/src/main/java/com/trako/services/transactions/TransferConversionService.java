package com.trako.services.transactions;

import com.trako.dtos.TransferResult;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for converting transactions between regular and transfer types.
 *
 * <p>This service handles the conversion logic:
 * <ul>
 *   <li>Regular Transaction → Transfer (creates linked credit transaction)</li>
 *   <li>Transfer → Regular Transaction (removes linked transaction)</li>
 * </ul>
 */
@Service
public class TransferConversionService {

    private static final Logger logger = LoggerFactory.getLogger(TransferConversionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionValidationService validationService;

    @Autowired
    private TransferService transferService;

    /**
     * Converts a regular transaction to a transfer by:
     * 1. Updating the existing transaction to be a DEBIT with TRANSFER category
     * 2. Creating a linked CREDIT transaction in the destination account
     *
     * @param userId        the authenticated user id
     * @param transactionId the id of the transaction to convert
     * @param request       contains toAccountId and optional field overrides
     * @return a {@link TransferResult} containing both debit and credit transactions
     */
    @Transactional
    public TransferResult convertRegularToTransfer(String userId, Long transactionId, TransactionRequest request) {
        Long toAccountId = request.toAccountId();
        logger.info("Converting regular transaction {} to transfer (to account {}) for user {}", transactionId, toAccountId, userId);

        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (existing.getLinkedTransactionId() != null) {
            throw new IllegalArgumentException("Transaction is already a transfer");
        }

        validationService.validateAccountOwnership(userId, existing.getAccountId());
        validationService.validateAccountOwnership(userId, toAccountId);

        if (existing.getAccountId().equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        if (request.date() != null) existing.setDate(request.date());
        if (request.name() != null) existing.setName(request.name());
        if (request.comments() != null) existing.setComments(request.comments());
        if (request.originalAmount() != null) existing.setOriginalAmount(request.originalAmount());
        if (request.originalCurrency() != null) existing.setOriginalCurrency(request.originalCurrency());
        if (request.exchangeRate() != null) existing.setExchangeRate(request.exchangeRate());

        Long transferCategoryId = transferService.getOrCreateTransferCategory(userId);

        existing.setCategoryId(transferCategoryId);
        existing.setTransactionType(TransactionType.DEBIT);
        existing.setIsCountable(0);

        Transaction credit = new Transaction();
        credit.setAccountId(toAccountId);
        credit.setCategoryId(transferCategoryId);
        credit.setTransactionType(TransactionType.CREDIT);
        credit.setOriginalAmount(existing.getOriginalAmount());
        credit.setOriginalCurrency(existing.getOriginalCurrency());
        credit.setExchangeRate(existing.getExchangeRate());
        credit.setDate(existing.getDate());
        credit.setIsCountable(0);
        credit.setName(existing.getName());
        credit.setComments(existing.getComments());

        Transaction savedCredit = saveTransaction(userId, credit);

        existing.setLinkedTransactionId(savedCredit.getId());
        Transaction savedDebit = saveTransaction(userId, existing);

        savedCredit.setLinkedTransactionId(savedDebit.getId());
        saveTransaction(userId, savedCredit);

        return new TransferResult(savedDebit, savedCredit);
    }

    /**
     * Converts a transfer transaction back to a regular transaction by:
     * 1. Removing the linked transaction
     * 2. Updating the existing transaction with the new category and type
     * 3. Setting isCountable back to 1
     *
     * @param userId        the authenticated user id
     * @param transactionId the id of the transaction to convert
     * @param request       contains new categoryId, transactionType, and optional fields
     * @return the updated transaction now as a regular transaction
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

        if (!transactionRepository.existsById(linkedId)) {
            throw new NotFoundException("Linked transaction not found: " + linkedId);
        }

        validationService.validateAccountOwnership(userId, existing.getAccountId());

        existing.setLinkedTransactionId(null);

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
            existing.setIsCountable(1);
        }

        Transaction saved = saveTransaction(userId, existing);
        deleteTransaction(userId, linkedId);

        return saved;
    }

    private Transaction saveTransaction(String userId, Transaction transaction) {
        if (transaction.getAccountId() != null) {
            validationService.validateAccountOwnership(userId, transaction.getAccountId());
        }
        Transaction persisted = transactionRepository.saveAndFlush(transaction);
        return transactionRepository.findById(persisted.getId()).orElse(persisted);
    }

    private void deleteTransaction(String userId, Long transactionId) {
        Transaction transaction = validationService.validateTransactionOwnership(userId, transactionId);
        transactionRepository.delete(transaction);
    }
}
