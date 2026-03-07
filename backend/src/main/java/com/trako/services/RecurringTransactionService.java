package com.trako.services;

import com.trako.enums.Frequency;
import com.trako.entities.RecurringTransaction;
import com.trako.exceptions.AuthorizationException;
import com.trako.enums.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.RecurringTransactionRepository;
import com.trako.services.transactions.TransactionValidationService;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.services.transactions.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class RecurringTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(RecurringTransactionService.class);

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TransactionValidationService validationService;

    // Self-injection so that processSingleTransaction's REQUIRES_NEW propagation
    // is honoured through the Spring proxy (intra-bean calls bypass the proxy).
    @Autowired
    @Lazy
    private RecurringTransactionService self;

    public List<RecurringTransaction> getAll(String userId) {
        return recurringTransactionRepository.findByUserId(userId);
    }

    public Optional<RecurringTransaction> getById(Long id) {
        return recurringTransactionRepository.findById(id);
    }

    @Transactional
    public RecurringTransaction create(String userId, RecurringTransaction recurringTransaction) {
        validationService.validateAccountOwnership(userId, recurringTransaction.getAccountId());
        validationService.validateCategoryOwnership(userId, recurringTransaction.getCategoryId());
        validationService.validateAccountOwnership(userId, recurringTransaction.getToAccountId());

        recurringTransaction.setUserId(userId);

        // Ensure nextRunDate is set. If not, default to startDate
        if (recurringTransaction.getNextRunDate() == null) {
            recurringTransaction.setNextRunDate(recurringTransaction.getStartDate());
        }

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

        // Immediately backfill all past-due entries so the user sees transactions right away.
        // Direct call (not self.) so it runs in the same transaction — if processing fails,
        // the entire create rolls back.
        processSingleTransaction(saved);

        return recurringTransactionRepository.findById(saved.getId()).orElse(saved);
    }

    @Transactional
    public RecurringTransaction update(String userId, Long id, RecurringTransaction updates) {
        RecurringTransaction existing = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found"));

        if (!existing.getUserId().equals(userId)) {
            throw new AuthorizationException("User does not own this recurring transaction: " + id);
        }

        if (updates.getAccountId() != null) {
            validationService.validateAccountOwnership(userId, updates.getAccountId());
        }
        if (updates.getCategoryId() != null) {
            validationService.validateCategoryOwnership(userId, updates.getCategoryId());
        }
        if (updates.getToAccountId() != null) {
            validationService.validateAccountOwnership(userId, updates.getToAccountId());
        }

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getAccountId() != null) existing.setAccountId(updates.getAccountId());
        if (updates.getCategoryId() != null) existing.setCategoryId(updates.getCategoryId());
        if (updates.getToAccountId() != null) existing.setToAccountId(updates.getToAccountId());
        if (updates.getTransactionType() != null) existing.setTransactionType(updates.getTransactionType());
        if (updates.getFrequency() != null) existing.setFrequency(updates.getFrequency());
        if (updates.getStartDate() != null) existing.setStartDate(updates.getStartDate());
        if (updates.getNextRunDate() != null) existing.setNextRunDate(updates.getNextRunDate());
        if (updates.getEndDate() != null) existing.setEndDate(updates.getEndDate());
        if (updates.getIsActive() != null) existing.setIsActive(updates.getIsActive());

        // Currency fields
        if (updates.getOriginalCurrency() != null) {
            if (updates.getOriginalCurrency().isEmpty()) {
                existing.setOriginalCurrency(null);
                existing.setOriginalAmount(null);
                existing.setExchangeRate(null);
            } else {
                existing.setOriginalCurrency(updates.getOriginalCurrency());
            }
        }
        if (updates.getOriginalAmount() != null) existing.setOriginalAmount(updates.getOriginalAmount());
        if (updates.getExchangeRate() != null) existing.setExchangeRate(updates.getExchangeRate());

        return recurringTransactionRepository.save(existing);
    }

    @Transactional
    public void delete(String userId, Long id) {
        RecurringTransaction existing = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found"));

        if (!existing.getUserId().equals(userId)) {
            throw new AuthorizationException("User does not own this recurring transaction: " + id);
        }

        recurringTransactionRepository.delete(existing);
    }

    public void processDueTransactions() {
        Date now = new Date();
        List<RecurringTransaction> dueTransactions = recurringTransactionRepository.findByNextRunDateBeforeAndIsActiveTrue(now);

        logger.info("Found {} recurring transactions due for processing", dueTransactions.size());

        for (RecurringTransaction rt : dueTransactions) {
            try {
                self.processSingleTransaction(rt);
            } catch (Exception e) {
                logger.error("Failed to process recurring transaction ID: {}", rt.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleTransaction(RecurringTransaction rt) {
        Date now = new Date();

        // Already past end date — deactivate without creating anything
        if (rt.getEndDate() != null && rt.getNextRunDate().after(rt.getEndDate())) {
            rt.setIsActive(false);
            recurringTransactionRepository.save(rt);
            return;
        }

        logger.info("Processing recurring transaction: {} (ID: {})", rt.getName(), rt.getId());

        // Catch-up loop: backfill one entry per missed period until nextRunDate is in the future.
        // Each entry is dated to the period it belongs to, keeping budgets/summaries accurate.
        int created = 0;
        while (!rt.getNextRunDate().after(now)) {
            if (rt.getEndDate() != null && rt.getNextRunDate().after(rt.getEndDate())) {
                rt.setIsActive(false);
                break;
            }

            createTransactionEntry(rt);
            created++;

            rt.setLastRunDate(rt.getNextRunDate());
            Date nextDate = calculateNextRunDate(rt.getNextRunDate(), rt.getFrequency());
            rt.setNextRunDate(nextDate);

            if (rt.getEndDate() != null && nextDate.after(rt.getEndDate())) {
                rt.setIsActive(false);
                break;
            }
        }

        if (created > 1) {
            logger.info("Caught up {} missed entries for recurring transaction ID: {}", created, rt.getId());
        }

        recurringTransactionRepository.save(rt);
    }

    private void createTransactionEntry(RecurringTransaction rt) {
        boolean isTransferType = rt.getTransactionType() == TransactionType.TRANSFER;
        boolean hasDistinctToAccount = rt.getToAccountId() != null
                && !rt.getToAccountId().equals(rt.getAccountId());

        if (isTransferType && hasDistinctToAccount) {
            transferService.createTransfer(
                    rt.getUserId(),
                    rt.getAccountId(),
                    rt.getToAccountId(),
                    rt.getNextRunDate(),
                    rt.getOriginalAmount(),
                    rt.getOriginalCurrency(),
                    rt.getExchangeRate(),
                    rt.getName(),
                    "Recurring Transfer: " + rt.getFrequency()
            );
        } else {
            TransactionType txType;
            if (rt.getTransactionType() == TransactionType.DEBIT) {
                txType = TransactionType.DEBIT;
            } else if (rt.getTransactionType() == TransactionType.CREDIT) {
                txType = TransactionType.CREDIT;
            } else {
                txType = TransactionType.DEBIT;
            }

            TransactionRequest request = new TransactionRequest(
                    null,                                           // id
                    rt.getAccountId(),                              // accountId
                    rt.getNextRunDate(),                            // date
                    rt.getName(),                                   // name
                    "Recurring Transaction: " + rt.getFrequency(),  // comments
                    rt.getCategoryId(),                             // categoryId
                    txType,                                         // transactionType
                    1,                                              // isCountable
                    rt.getOriginalCurrency(),                       // originalCurrency
                    rt.getOriginalAmount(),                         // originalAmount
                    rt.getExchangeRate(),                           // exchangeRate
                    null,                                           // linkedTransactionId
                    null,                                           // toAccountId
                    null                                            // fromAccountId
            );
            transactionWriteService.createTransaction(rt.getUserId(), request);
        }
    }

    private Date calculateNextRunDate(Date current, Frequency frequency) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(current);

        switch (frequency) {
            case DAILY:
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case WEEKLY:
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTHLY:
                cal.add(Calendar.MONTH, 1);
                break;
            case YEARLY:
                cal.add(Calendar.YEAR, 1);
                break;
        }
        return cal.getTime();
    }
}
