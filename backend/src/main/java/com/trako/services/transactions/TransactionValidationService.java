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

    public void validateTransactionCreateRequest(TransactionRequest request) {
        if (request.accountId() == null) {
            throw new BadRequestException("Transaction requires accountId");
        }
        if (request.categoryId() == null) {
            throw new BadRequestException("Transaction requires categoryId");
        }
        if (request.originalCurrency() == null) {
            throw new BadRequestException("Original currency is required");
        }
        if (request.originalAmount() == null) {
            throw new BadRequestException("Original amount is required");
        }
    }

    public void validateTransferCreateRequest(TransactionRequest request) {
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
            throw new BadRequestException("Original currency is required");
        }
        if (request.originalAmount() == null || request.originalAmount() <= 0) {
            throw new BadRequestException("Original amount must be greater than 0");
        }
    }

    public void validateToAccountId(Long toAccountId) {
        if (toAccountId == null) {
            throw new BadRequestException("toAccountId is required when converting to a transfer");
        }
    }

    public void validatePositiveAmount(Double amount) {
        if (amount != null && amount <= 0) {
            throw new BadRequestException("Original amount must be greater than 0");
        }
    }

    public void validateIsTransfer(Transaction existing) {
        if (existing.getLinkedTransactionId() == null) {
            throw new BadRequestException("Transaction is not a transfer");
        }
    }

    public void validateIsNotTransfer(Transaction existing) {
        if (existing.getLinkedTransactionId() != null) {
            throw new BadRequestException("Transaction is already a transfer");
        }
    }

    public void validateLinkedTransactionExists(Long linkedId) {
        if (!transactionRepository.existsById(linkedId)) {
            throw new NotFoundException("Linked transaction not found: " + linkedId);
        }
    }

    public void validateNotSameAccount(Long accountId1, Long accountId2) {
        if (accountId1 != null && accountId1.equals(accountId2)) {
            throw new BadRequestException("Source and destination accounts cannot be the same");
        }
    }
}
