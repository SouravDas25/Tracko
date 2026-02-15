package com.trako.services;

import com.trako.dtos.SplitDetailDTO;
import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-oriented service for {@link Transaction} queries.
 *
 * <p><b>Important:</b> this service is intentionally read-only. All transaction mutations must go
 * through {@link TransactionWriteService} so {@code account_month_summary} remains consistent.
 */
@Service
public class TransactionService {

    // Read-only service.
    // All transaction mutations must go through TransactionWriteService so that
    // account_month_summary stays consistent (used for fast summary + rollover queries).

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountSummaryReadOnlyService accountSummaryReadOnlyService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private SplitRepository splitRepository;

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> findByUserId(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> findByUserIdAndDateBetween(String userId, Date startDate, Date endDate) {
        return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    public Page<Transaction> findByUserIdAndDateBetween(String userId, Date startDate, Date endDate, Pageable pageable) {
        return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate, pageable);
    }

    public List<Transaction> findByUserIdAndDateBetweenAndAccountIds(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        return transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
    }

    public Page<Transaction> findByUserIdAndDateBetweenAndAccountIds(String userId, Date startDate, Date endDate, List<Long> accountIds, Pageable pageable) {
        return transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds, pageable);
    }

    public Page<Transaction> findByUserIdAndCategoryIdAndDateBetween(String userId, Long categoryId, Date startDate, Date endDate, Pageable pageable) {
        return transactionRepository.findByUserIdAndCategoryIdAndDateBetween(userId, categoryId, startDate, endDate, pageable);
    }

    public Page<TransactionDetailDTO> findWithDetailsByUserIdAndCategoryIdAndDateBetween(String userId, Long categoryId, Date startDate, Date endDate, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findByUserIdAndCategoryIdAndDateBetween(userId, categoryId, startDate, endDate, pageable);

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<TransactionDetailDTO> dtos = fetchDetailsForTransactions(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public List<TransactionDetailDTO> findWithDetailsByUserIdAndDateBetween(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        List<Transaction> transactions;
        if (accountIds == null || accountIds.isEmpty()) {
            transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        } else {
            transactions = transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
        }

        return fetchDetailsForTransactions(transactions);
    }

    public Page<TransactionDetailDTO> findWithDetailsByUserIdAndDateBetween(String userId, Date startDate, Date endDate, List<Long> accountIds, Pageable pageable) {
        Page<Transaction> page;
        if (accountIds == null || accountIds.isEmpty()) {
            page = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate, pageable);
        } else {
            page = transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds, pageable);
        }

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<TransactionDetailDTO> dtos = fetchDetailsForTransactions(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private List<TransactionDetailDTO> fetchDetailsForTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect IDs
        Set<Long> acctIds = new HashSet<>();
        Set<Long> catIds = new HashSet<>();
        List<Long> txIds = new ArrayList<>();

        for (Transaction t : transactions) {
            acctIds.add(t.getAccountId());
            catIds.add(t.getCategoryId());
            txIds.add(t.getId());
        }

        // Batch Fetch
        List<Account> accounts = accountRepository.findAllById(acctIds);
        Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));

        List<Category> categories = categoryRepository.findAllById(catIds);
        Map<Long, Category> categoryMap = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Split> splits = splitRepository.findByTransactionIdIn(txIds);
        Map<Long, List<Split>> splitsByTxId = splits.stream().collect(Collectors.groupingBy(Split::getTransactionId));

        // Fetch Contacts for Splits
        Set<Long> contactIds = splits.stream()
                .map(Split::getContactId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Contact> contacts = contactRepository.findAllById(contactIds);
        Map<Long, Contact> contactMap = contacts.stream().collect(Collectors.toMap(Contact::getId, Function.identity()));

        // Assemble DTOs
        List<TransactionDetailDTO> dtos = new ArrayList<>();
        for (Transaction t : transactions) {
            Account acct = accountMap.get(t.getAccountId());
            Category cat = categoryMap.get(t.getCategoryId());
            List<Split> txSplits = splitsByTxId.getOrDefault(t.getId(), Collections.emptyList());

            List<SplitDetailDTO> splitdtos = txSplits.stream().map(s -> {
                Contact c = (s.getContactId() != null) ? contactMap.get(s.getContactId()) : null;
                return new SplitDetailDTO(s, c);
            }).collect(Collectors.toList());

            dtos.add(new TransactionDetailDTO(t, cat, acct, splitdtos));
        }

        return dtos;
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> findByCategoryId(Long categoryId) {
        return transactionRepository.findByCategoryId(categoryId);
    }

    public TransactionSummaryDTO getSummary(String userId, Date startDate, Date endDate) {
        return getSummary(userId, startDate, endDate, null);
    }

    /**
     * Returns a transaction summary for the given range and also includes a rollover value.
     *
     * <p>Rollover is defined as the sum of net totals for all months strictly before the month of
     * {@code startDate}. When {@code startDate} cannot be mapped to a month, this returns the base
     * summary with no rollover.
     *
     * @param userId     user id
     * @param startDate  range start (inclusive)
     * @param endDate    range end (exclusive)
     * @param accountIds optional account filter
     * @return summary DTO including rollover fields
     */
    public TransactionSummaryDTO getSummaryWithRollover(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        // Summary for the requested range (month fast-path when possible).
        TransactionSummaryDTO base = getSummary(userId, startDate, endDate, accountIds);

        YearMonthKey ym = toYearMonthKey(startDate);
        if (ym == null) {
            return base;
        }

        Double rolloverNet;
        // Rollover is defined as the sum of net totals for all months strictly before the start month.
        rolloverNet = accountSummaryReadOnlyService.getRolloverNetBeforeMonth(userId, ym.year, ym.month, accountIds);

        double withRollover = safe(base.getNetTotal()) + safe(rolloverNet);
        return new TransactionSummaryDTO(
                safe(base.getTotalIncome()),
                safe(base.getTotalExpense()),
                safe(base.getNetTotal()),
                safe(rolloverNet),
                withRollover,
                base.getTransactionCount()
        );
    }

    /**
     * Returns a transaction summary for the given date range.
     *
     * <p>Fast path: if the range is exactly a full calendar month (start at first-day midnight,
     * end at the start of the next month), the result is computed from the pre-aggregated
     * {@code account_month_summary} table.
     *
     * <p>Fallback: otherwise, the result is computed by scanning raw transactions in the range.
     *
     * @param userId     user id
     * @param startDate  range start (inclusive)
     * @param endDate    range end (exclusive)
     * @param accountIds optional account filter
     * @return summary DTO
     */
    public TransactionSummaryDTO getSummary(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        YearMonthKey ym = toYearMonthKey(startDate);
        boolean fullMonth = isFullMonthRange(startDate, endDate);

        if (ym != null && fullMonth) {
            // Fast-path: when the requested range is exactly a full calendar month, use the
            // pre-aggregated account_month_summary table instead of scanning raw transactions.
            AccountSummaryReadOnlyService.MonthTotals totals =
                    accountSummaryReadOnlyService.getMonthTotals(userId, ym.year, ym.month, accountIds);
            return new TransactionSummaryDTO(totals.income(), totals.expense(), totals.net(), totals.count());
        }

        // Fallback for arbitrary date ranges (preserve old behavior): scan raw transactions
        // since account_month_summary is only maintained at month granularity.
        List<Transaction> transactions;
        if (accountIds == null || accountIds.isEmpty()) {
            transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        } else {
            transactions = transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
        }

        double totalIncome = 0.0;
        double totalExpense = 0.0;
        int count = 0;

        for (Transaction t : transactions) {
            if (t.getIsCountable() == 1) {
                count++;
                if (t.getTransactionType() == 2) {
                    totalIncome += t.getAmount();
                } else if (t.getTransactionType() == 1) {
                    totalExpense += t.getAmount();
                }
            }
        }

        double netTotal = totalIncome - totalExpense;
        return new TransactionSummaryDTO(totalIncome, totalExpense, netTotal, count);
    }

    private static class YearMonthKey {
        final int year;
        final int month;

        YearMonthKey(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

    private YearMonthKey toYearMonthKey(Date date) {
        if (date == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        return new YearMonthKey(year, month);
    }

    private boolean isFullMonthRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) return false;

        Calendar s = Calendar.getInstance();
        s.setTime(startDate);

        Calendar e = Calendar.getInstance();
        e.setTime(endDate);

        boolean startIsFirstDayMidnight =
                s.get(Calendar.DAY_OF_MONTH) == 1 &&
                        s.get(Calendar.HOUR_OF_DAY) == 0 &&
                        s.get(Calendar.MINUTE) == 0 &&
                        s.get(Calendar.SECOND) == 0;

        if (!startIsFirstDayMidnight) return false;

        Calendar expectedEnd = (Calendar) s.clone();
        expectedEnd.add(Calendar.MONTH, 1);

        // End is treated as an exclusive boundary (start of next month at the same time as startDate).

        return expectedEnd.get(Calendar.YEAR) == e.get(Calendar.YEAR)
                && expectedEnd.get(Calendar.MONTH) == e.get(Calendar.MONTH)
                && expectedEnd.get(Calendar.DAY_OF_MONTH) == e.get(Calendar.DAY_OF_MONTH);
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    public Double getTotalIncome(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == 2) // CREDIT = income
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Double getTotalExpense(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == 1) // DEBIT = expense
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
}
