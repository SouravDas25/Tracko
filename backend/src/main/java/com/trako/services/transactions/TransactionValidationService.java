package com.trako.services.transactions;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.BadRequestException;
import com.trako.exceptions.NotFoundException;
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
}
