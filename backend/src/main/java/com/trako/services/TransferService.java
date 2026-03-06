package com.trako.services;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.BadRequestException;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private void validateAccountOwnership(String userId, Long accountId) {
        if (accountId == null) return;
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Account not found: " + accountId));
        if (!userId.equals(account.getUserId())) {
            throw new AuthorizationException("User does not own account: " + accountId);
        }
    }

    private Transaction saveTransaction(String userId, Transaction transaction) {
        if (transaction.getAccountId() != null) {
            validateAccountOwnership(userId, transaction.getAccountId());
        }
        Transaction persisted = transactionRepository.saveAndFlush(transaction);
        return transactionRepository.findById(persisted.getId()).orElse(persisted);
    }

    private void deleteTransaction(String userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
        validateAccountOwnership(userId, transaction.getAccountId());
        transactionRepository.delete(transaction);
    }

    private Long getOrCreateTransferCategory(String userId) {
        var catList = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (!catList.isEmpty()) {
            return catList.get(0).getId();
        }

        synchronized (userId.intern()) {
            catList = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
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

    public boolean isTransfer(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .map(tx -> tx.getLinkedTransactionId() != null)
                .orElse(false);
    }

    @Transactional
    public Transaction[] createTransfer(String userId, Long fromAccountId, Long toAccountId, 
                                       Date date, Double originalAmount, String originalCurrency, Double exchangeRate, String name, String comments) {
        logger.info("Creating transfer: {} {} from account {} to account {} for user {}", 
                   originalAmount, originalCurrency, fromAccountId, toAccountId, userId);

        if (originalCurrency == null) {
            throw new IllegalArgumentException("Original currency is required for transfer");
        }
        if (originalAmount == null) {
            throw new IllegalArgumentException("Original amount is required for transfer");
        }
        if (exchangeRate == null) {
            throw new IllegalArgumentException("Exchange rate is required for transfer");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("fromAccountId and toAccountId cannot be same");
        }

        validateAccountOwnership(userId, fromAccountId);
        validateAccountOwnership(userId, toAccountId);

        Long transferCategoryId = getOrCreateTransferCategory(userId);
        Date transferDate = (date != null) ? date : new Date();

        Transaction debit = new Transaction();
        debit.setAccountId(fromAccountId);
        debit.setCategoryId(transferCategoryId);
        debit.setTransactionType(TransactionType.DEBIT);
        debit.setOriginalAmount(originalAmount);
        debit.setOriginalCurrency(originalCurrency);
        debit.setExchangeRate(exchangeRate);
        debit.setDate(transferDate);
        debit.setIsCountable(0);
        debit.setName(name != null ? name : "Transfer Out");
        debit.setComments(comments);

        Transaction savedDebit = saveTransaction(userId, debit);

        if (savedDebit == null || savedDebit.getId() == null) {
            throw new RuntimeException("Failed to create debit transaction");
        }

        Transaction credit = new Transaction();
        credit.setAccountId(toAccountId);
        credit.setCategoryId(transferCategoryId);
        credit.setTransactionType(TransactionType.CREDIT);
        credit.setOriginalAmount(originalAmount);
        credit.setOriginalCurrency(originalCurrency);
        credit.setExchangeRate(exchangeRate);
        credit.setDate(transferDate);
        credit.setIsCountable(0);
        credit.setName(name != null ? name : "Transfer In");
        credit.setComments(comments);
        credit.setLinkedTransactionId(savedDebit.getId());

        Transaction savedCredit = saveTransaction(userId, credit);

        if (savedCredit == null || savedCredit.getId() == null) {
            throw new RuntimeException("Failed to create credit transaction");
        }

        savedDebit.setLinkedTransactionId(savedCredit.getId());
        saveTransaction(userId, savedDebit);

        return new Transaction[]{savedDebit, savedCredit};
    }

    @Transactional
    public void deleteTransfer(String userId, Long transactionId) {
        logger.info("Deleting transfer containing transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (transaction.getLinkedTransactionId() == null) {
            throw new IllegalArgumentException("Transaction " + transactionId + " is not part of a transfer");
        }

        Long linkedId = transaction.getLinkedTransactionId();
        Transaction linkedTx = transactionRepository.findById(linkedId)
                .orElseThrow(() -> new NotFoundException("Linked transaction not found: " + linkedId));

        validateAccountOwnership(userId, transaction.getAccountId());
        validateAccountOwnership(userId, linkedTx.getAccountId());

        deleteTransaction(userId, transactionId);
        deleteTransaction(userId, linkedId);
    }

    @Transactional
    public Transaction[] updateTransfer(String userId, Long transactionId, 
                                       Long fromAccountId, Long toAccountId,
                                       Date date,
                                       Double originalAmount, String originalCurrency, Double exchangeRate, String name, String comments) {
        logger.info("Updating transfer containing transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getLinkedTransactionId() == null) {
            throw new IllegalArgumentException("Transaction " + transactionId + " is not part of a transfer");
        }

        Long linkedId = transaction.getLinkedTransactionId();
        Transaction linkedTx = transactionRepository.findById(linkedId)
                .orElseThrow(() -> new IllegalArgumentException("Linked transaction not found: " + linkedId));

        Transaction debit = transaction.getTransactionType() != null && transaction.getTransactionType() == TransactionType.DEBIT ? transaction : linkedTx;
        Transaction credit = transaction.getTransactionType() != null && transaction.getTransactionType() == TransactionType.CREDIT ? transaction : linkedTx;

        validateAccountOwnership(userId, debit.getAccountId());
        validateAccountOwnership(userId, credit.getAccountId());

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

        if (newDebitAccountId.equals(newCreditAccountId)) {
            throw new IllegalArgumentException("Transfer source and destination accounts cannot be the same");
        }

        if (fromAccountId != null && !fromAccountId.equals(debit.getAccountId())) {
            debit.setAccountId(fromAccountId);
        }
        if (toAccountId != null && !toAccountId.equals(credit.getAccountId())) {
            credit.setAccountId(toAccountId);
        }

        Transaction updatedDebit = saveTransaction(userId, debit);
        Transaction updatedCredit = saveTransaction(userId, credit);

        return new Transaction[]{updatedDebit, updatedCredit};
    }

    @Transactional
    public Transaction[] convertRegularToTransfer(String userId, Long transactionId, TransactionRequest request) {
        Long toAccountId = request.toAccountId();
        logger.info("Converting regular transaction {} to transfer (to account {}) for user {}", transactionId, toAccountId, userId);

        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (existing.getLinkedTransactionId() != null) {
            throw new IllegalArgumentException("Transaction is already a transfer");
        }

        validateAccountOwnership(userId, existing.getAccountId());
        validateAccountOwnership(userId, toAccountId);
        
        if (existing.getAccountId().equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        if (request.date() != null) existing.setDate(request.date());
        if (request.name() != null) existing.setName(request.name());
        if (request.comments() != null) existing.setComments(request.comments());
        if (request.originalAmount() != null) existing.setOriginalAmount(request.originalAmount());
        if (request.originalCurrency() != null) existing.setOriginalCurrency(request.originalCurrency());
        if (request.exchangeRate() != null) existing.setExchangeRate(request.exchangeRate());

        Long transferCategoryId = getOrCreateTransferCategory(userId);

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

        return new Transaction[]{savedDebit, savedCredit};
    }

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

        validateAccountOwnership(userId, existing.getAccountId());

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
}
