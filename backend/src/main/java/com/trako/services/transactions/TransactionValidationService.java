package com.trako.services.transactions;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.BadRequestException;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionValidationService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // ==================== Ownership Validators ====================

    public void validateAccountOwnership(String userId, Long accountId) {
        if (accountId == null) return;
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Account not found: " + accountId));
        if (!userId.equals(account.getUserId())) {
            throw new AuthorizationException("User does not own account: " + accountId);
        }
    }

    public void validateCategoryOwnership(String userId, Long categoryId) {
        if (categoryId == null) return;
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Category not found: " + categoryId));
        if (!userId.equals(category.getUserId())) {
            throw new AuthorizationException("User does not own category: " + categoryId);
        }
    }

    public Transaction validateTransactionOwnership(String userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        validateAccountOwnership(userId, transaction.getAccountId());
        return transaction;
    }

    // ==================== Create Validators ====================

    /**
     * Validates a request to create a DEBIT or CREDIT transaction.
     * Required: transactionType, accountId, categoryId, originalCurrency, originalAmount.
     */
    public void validateCreateTransaction(TransactionRequest request) {
        if (request.transactionType() == null) {
            throw new BadRequestException("transactionType is required");
        }
        if (request.accountId() == null) {
            throw new BadRequestException("accountId is required");
        }
        if (request.categoryId() == null) {
            throw new BadRequestException("categoryId is required");
        }
        if (request.originalCurrency() == null) {
            throw new BadRequestException("originalCurrency is required");
        }
        if (request.originalAmount() == null) {
            throw new BadRequestException("originalAmount is required");
        }
    }

    /**
     * Validates a request to create a TRANSFER.
     * Required: transactionType, fromAccountId/accountId, toAccountId, originalCurrency, originalAmount (>0).
     */
    public void validateCreateTransfer(TransactionRequest request) {
        if (request.transactionType() == null) {
            throw new BadRequestException("transactionType is required");
        }
        Long fromAccountId = request.getSourceAccountId();
        if (fromAccountId == null) {
            throw new BadRequestException("Transfer requires fromAccountId or accountId");
        }
        if (request.toAccountId() == null) {
            throw new BadRequestException("Transfer requires toAccountId");
        }
        if (fromAccountId.equals(request.toAccountId())) {
            throw new BadRequestException("fromAccountId and toAccountId cannot be the same");
        }
        if (request.originalCurrency() == null) {
            throw new BadRequestException("originalCurrency is required");
        }
        if (request.originalAmount() == null || request.originalAmount() <= 0) {
            throw new BadRequestException("originalAmount must be greater than 0");
        }
    }

    // ==================== Update Validators ====================

    /**
     * Validates a request to update a regular transaction (stays DEBIT or CREDIT, or switches between them).
     * transactionType may be null (means "no type change"). All fields are optional (partial update).
     * If originalAmount is provided, it must be > 0.
     */
    public void validateUpdateTransaction(TransactionRequest request) {
        if (request.originalAmount() != null && request.originalAmount() <= 0) {
            throw new BadRequestException("originalAmount must be greater than 0");
        }
    }

    /**
     * Validates a request to update a transfer (stays a transfer).
     * transactionType may be null (means "no type change"). All fields are optional (partial update).
     * If originalAmount is provided, it must be > 0.
     */
    public void validateUpdateTransfer(TransactionRequest request) {
        if (request.originalAmount() != null && request.originalAmount() <= 0) {
            throw new BadRequestException("originalAmount must be greater than 0");
        }
        Long resolvedFrom = request.fromAccountId() != null ? request.fromAccountId() : request.accountId();
        if (resolvedFrom != null && resolvedFrom.equals(request.toAccountId())) {
            throw new BadRequestException("Source and destination accounts cannot be the same");
        }
    }

    /**
     * Validates a request to convert a regular transaction into a transfer.
     * Required: transactionType, toAccountId.
     */
    public void validateConvertToTransfer(TransactionRequest request, Transaction existing) {
        if (request.transactionType() == null) {
            throw new BadRequestException("transactionType is required");
        }
        if (request.toAccountId() == null) {
            throw new BadRequestException("toAccountId is required when converting to a transfer");
        }
        if (existing.getAccountId().equals(request.toAccountId())) {
            throw new BadRequestException("Source and destination accounts cannot be the same");
        }
    }

    /**
     * Validates a request to convert a transfer back into a regular transaction.
     * Required: transactionType, categoryId.
     * The linked transaction must exist.
     */
    public void validateConvertToTransaction(TransactionRequest request, Transaction existing) {
        if (request.transactionType() == null) {
            throw new BadRequestException("transactionType is required");
        }
        if (request.categoryId() == null) {
            throw new BadRequestException("categoryId is required when converting a transfer to a regular transaction");
        }
        if (existing.getLinkedTransactionId() == null) {
            throw new BadRequestException("Transaction is not a transfer");
        }
        if (!transactionRepository.existsById(existing.getLinkedTransactionId())) {
            throw new NotFoundException("Linked transaction not found: " + existing.getLinkedTransactionId());
        }
    }
}
