package com.trako.services.transactions;

import com.trako.dtos.SplitDetailDTO;
import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionPeriodSummaryDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.util.NumberUtil;
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
 * through {@link TransactionWriteService} to preserve read-side consistency.
 */
@Service
public class TransactionService {

    // Read-only service.
    // All transaction mutations must go through TransactionWriteService.

    @Autowired
    private TransactionRepository transactionRepository;

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

    public Page<Transaction> findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(String userId, Long categoryId, Date startDate, Date endDate, List<Long> accountIds, Pageable pageable) {
        return transactionRepository.findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(userId, categoryId, startDate, endDate, accountIds, pageable);
    }

    public Page<TransactionDetailDTO> findWithDetailsByUserIdAndCategoryIdAndDateBetween(String userId, Long categoryId, Date startDate, Date endDate, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findByUserIdAndCategoryIdAndDateBetween(userId, categoryId, startDate, endDate, pageable);

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<TransactionDetailDTO> dtos = fetchDetailsForTransactions(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public Page<TransactionDetailDTO> findWithDetailsByUserIdAndCategoryIdAndDateBetweenAndAccountIds(String userId, Long categoryId, Date startDate, Date endDate, List<Long> accountIds, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(userId, categoryId, startDate, endDate, accountIds, pageable);

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

        Date monthStart = toMonthStart(ym.year, ym.month);
        Double rolloverNet;
        // Rollover is defined as the sum of net totals strictly before the start month.
        if (accountIds == null || accountIds.isEmpty()) {
            rolloverNet = transactionRepository.sumNetBeforeDateForUser(userId, monthStart);
        } else {
            rolloverNet = transactionRepository.sumNetBeforeDateForUserAndAccounts(userId, accountIds, monthStart);
        }

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
     * end at the start of the next month), the result is computed by aggregating countable
     * transactions directly in SQL.
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
            // Fast-path: aggregate countable transactions directly for full calendar months.
            Object[] row;
            if (accountIds == null || accountIds.isEmpty()) {
                row = transactionRepository.sumCountableTotalsForUserInRange(userId, startDate, endDate);
            } else {
                row = transactionRepository.sumCountableTotalsForUserInRangeAndAccounts(userId, accountIds, startDate, endDate);
            }
            return summaryFromAggregateRow(row);
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
                if (t.getTransactionType() == TransactionType.CREDIT) {
                    totalIncome += t.getAmount();
                } else if (t.getTransactionType() == TransactionType.DEBIT) {
                    totalExpense += t.getAmount();
                }
            }
        }

        double netTotal = totalIncome - totalExpense;
        return new TransactionSummaryDTO(totalIncome, totalExpense, netTotal, count);
    }

    public TransactionSummaryDTO getAccountSummary(String userId, Long accountId, Date startDate, Date endDate) {
        TransactionSummaryDTO base = getSummary(userId, startDate, endDate, Collections.singletonList(accountId));
        Double transferDelta = transactionRepository.sumTransferDeltaForAccountInRange(userId, accountId, startDate, endDate);
        double net = safe(base.getNetTotal()) + safe(transferDelta);
        return new TransactionSummaryDTO(
                safe(base.getTotalIncome()),
                safe(base.getTotalExpense()),
                net,
                base.getTransactionCount()
        );
    }

    public TransactionSummaryDTO getAccountSummaryWithRollover(String userId, Long accountId, Date startDate, Date endDate) {
        TransactionSummaryDTO base = getAccountSummary(userId, accountId, startDate, endDate);

        YearMonthKey ym = toYearMonthKey(startDate);
        if (ym == null) {
            return base;
        }

        Date monthStart = toMonthStart(ym.year, ym.month);
        Double rolloverNet = transactionRepository.sumNetBeforeDateForUserAndAccounts(
                userId,
                Collections.singletonList(accountId),
                monthStart
        );

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

    private YearMonthKey toYearMonthKey(Date date) {
        if (date == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        return new YearMonthKey(year, month);
    }

    private Date toMonthStart(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
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

    private TransactionSummaryDTO summaryFromAggregateRow(Object[] row) {
        row = normalizeAggregateRow(row);
        double income = NumberUtil.asDouble(row[0]);
        double expense = NumberUtil.asDouble(row[1]);
        double net = NumberUtil.asDouble(row[2]);
        int count = NumberUtil.asInt(row[3]);
        return new TransactionSummaryDTO(income, expense, net, count);
    }

    private Object[] normalizeAggregateRow(Object[] row) {
        if (row == null) return new Object[]{0.0, 0.0, 0.0, 0};
        if (row.length == 1 && row[0] instanceof Object[]) {
            return (Object[]) row[0];
        }
        return row;
    }

    public Double getTotalIncome(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == TransactionType.CREDIT) // CREDIT = income
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Double getTotalExpense(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == TransactionType.DEBIT) // DEBIT = expense
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public List<TransactionPeriodSummaryDTO> getMonthlySummaries(String userId, int year, List<Long> accountIds) {
        List<Object[]> rows;
        if (accountIds == null || accountIds.isEmpty()) {
            rows = transactionRepository.findMonthlySummariesForUserAndYear(userId, year);
        } else {
            rows = transactionRepository.findMonthlySummariesForUserAndYearAndAccounts(userId, year, accountIds);
        }

        return rows.stream().map(row -> {
            row = normalizeAggregateRow(row);
            int y = NumberUtil.asInt(row[0]);
            int m = NumberUtil.asInt(row[1]);
            double income = NumberUtil.asDouble(row[2]);
            double expense = NumberUtil.asDouble(row[3]);
            double net = NumberUtil.asDouble(row[4]);
            int count = NumberUtil.asInt(row[5]);
            return new TransactionPeriodSummaryDTO(income, expense, net, count, y, m);
        }).collect(Collectors.toList());
    }

    public List<TransactionPeriodSummaryDTO> getYearlySummaries(String userId, List<Long> accountIds) {
        List<Object[]> rows;
        if (accountIds == null || accountIds.isEmpty()) {
            rows = transactionRepository.findYearlySummariesForUser(userId);
        } else {
            rows = transactionRepository.findYearlySummariesForUserAndAccounts(userId, accountIds);
        }

        return rows.stream().map(row -> {
            row = normalizeAggregateRow(row);
            int y = NumberUtil.asInt(row[0]);
            double income = NumberUtil.asDouble(row[1]);
            double expense = NumberUtil.asDouble(row[2]);
            double net = NumberUtil.asDouble(row[3]);
            int count = NumberUtil.asInt(row[4]);
            return new TransactionPeriodSummaryDTO(income, expense, net, count, y, null);
        }).collect(Collectors.toList());
    }

    private record YearMonthKey(int year, int month) {
    }
}
