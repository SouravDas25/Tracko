package com.trako.services.transactions;

import com.trako.entities.Transaction;
import com.trako.entities.TransactionEntryType;
import com.trako.entities.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.TransactionRepository;
import com.trako.services.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditTransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TransactionValidationService validationService;

    @Transactional
    public Transaction createCreditTransaction(String userId, TransactionRequest request) {
        validationService.validateTransactionCreateRequest(request);
        validationService.validateAccountOwnership(userId, request.accountId());
        validationService.validateCategoryOwnership(userId, request.categoryId());

        Double exchangeRate = currencyService.resolveExchangeRate(userId, request.originalCurrency(), request.exchangeRate());

        Transaction transaction = new Transaction();
        transaction.setAccountId(request.accountId());
        transaction.setCategoryId(request.categoryId());
        transaction.setTransactionType(TransactionEntryType.CREDIT);
        transaction.setDate(request.date());
        transaction.setName(request.name());
        transaction.setComments(request.comments());
        transaction.setIsCountable(request.isCountable());
        transaction.setOriginalAmount(request.originalAmount());
        transaction.setOriginalCurrency(request.originalCurrency());
        transaction.setExchangeRate(exchangeRate);
        transaction.setLinkedTransactionId(request.linkedTransactionId());

        return saveForUser(userId, transaction);
    }

    @Transactional
    public Transaction updateCreditTransaction(String userId, Transaction existing, TransactionRequest request) {
        // Apply updates or keep existing
        Long newAccountId = request.accountId() != null ? request.accountId() : existing.getAccountId();
        Long newCategoryId = request.categoryId() != null ? request.categoryId() : existing.getCategoryId();

        // Ownership checks
        validationService.validateAccountOwnership(userId, newAccountId);
        validationService.validateCategoryOwnership(userId, newCategoryId);

        existing.setAccountId(newAccountId);
        existing.setCategoryId(newCategoryId);
        existing.setTransactionType(TransactionEntryType.CREDIT);

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

        existing.setOriginalCurrency(newCurrency);
        existing.setExchangeRate(newRate);

        validationService.validatePositiveAmount(request.originalAmount());
        if (request.originalAmount() != null) existing.setOriginalAmount(request.originalAmount());

        if (request.name() != null) existing.setName(request.name());
        if (request.comments() != null) existing.setComments(request.comments());
        if (request.date() != null) existing.setDate(request.date());
        if (request.isCountable() != null) existing.setIsCountable(request.isCountable());

        return saveForUser(userId, existing);
    }

    private Transaction saveForUser(String userId, Transaction transaction) {
        // Redundant check but safe
        validationService.validateAccountOwnership(userId, transaction.getAccountId());
        Transaction persisted = transactionRepository.saveAndFlush(transaction);
        return transactionRepository.findById(persisted.getId()).orElse(persisted);
    }
}
