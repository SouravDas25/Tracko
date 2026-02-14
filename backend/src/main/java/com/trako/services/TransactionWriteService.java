package com.trako.services;

import com.trako.entities.Account;
import com.trako.entities.AccountMonthlySummary;
import com.trako.entities.Transaction;
import com.trako.entities.UserCurrency;
import com.trako.repositories.AccountMonthSummaryRepository;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UserCurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

/**
 * Write-oriented service for {@link Transaction} mutations.
 *
 * <p>This is the single supported write-path for creating/updating/deleting transactions.
 * It is responsible for maintaining the pre-aggregated {@code account_month_summary} table
 * (via {@link AccountMonthSummaryRepository}) so read-side queries (month summary and rollover)
 * can avoid scanning the raw {@code transactions} table.
 */
@Service
public class TransactionWriteService {

    // Single write-path for Transaction mutations.
    // This service is responsible for keeping the pre-aggregated account_month_summary table
    // consistent, which allows TransactionService to answer month summaries + rollover queries
    // without scanning raw transactions.

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMonthSummaryRepository accountMonthSummaryRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    @Transactional
    /**
     * Creates or updates a transaction for the given user.
     *
     * <p>On update, the existing transaction (if any) is loaded so we can apply an incremental
     * (old -&gt; new) delta to {@code account_month_summary}.
     *
     * <p>If {@link Transaction#getAmount()} is null, the amount is derived from the original
     * currency fields ({@code originalAmount/originalCurrency/exchangeRate}) before persistence.
     *
     * @param userId      authenticated user id
     * @param transaction transaction payload
     * @return persisted transaction
     */
    public Transaction saveForUser(String userId, Transaction transaction) {
        // For updates we need the previous state so we can apply a (old -> new) delta to the summary table.
        Transaction existing = null;
        if (transaction.getId() != null) {
            existing = transactionRepository.findById(transaction.getId()).orElse(null);
        }

        // Ensure Transaction.amount is populated (supports multi-currency inputs via originalAmount/currency/rate).
        computeAmountIfMissing(userId, transaction);

        Transaction saved = transactionRepository.save(transaction);

        // Summary maintenance: apply delta using (old -> new)
        applyDelta(userId, existing, saved);

        return saved;
    }

    @Transactional
    /**
     * Deletes a transaction for the given user and reverses its contribution from
     * {@code account_month_summary}.
     *
     * @param userId        authenticated user id
     * @param transactionId id of the transaction to delete
     */
    public void deleteForUser(String userId, Long transactionId) {
        // On delete we apply a delta of (old -> null) to reverse the summary contribution.
        Transaction existing = transactionRepository.findById(transactionId).orElse(null);
        if (existing == null) {
            return;
        }

        transactionRepository.deleteById(transactionId);
        applyDelta(userId, existing, null);
    }

    private void computeAmountIfMissing(String userId, Transaction transaction) {
        // Normalizes foreign-currency transactions into the user's base currency.
        // If amount is provided, we trust it; otherwise we derive it from originalAmount and either:
        // - exchangeRate (explicit), or
        // - originalCurrency (lookup in user's configured secondary currencies).
        if (transaction.getAmount() == null) {
            if (transaction.getOriginalAmount() != null && transaction.getExchangeRate() != null) {
                double calculatedAmount = transaction.getOriginalAmount() * transaction.getExchangeRate();
                transaction.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
            } else if (transaction.getOriginalAmount() != null && transaction.getOriginalCurrency() != null) {
                String uid = userId;
                String currencyCode = transaction.getOriginalCurrency().toUpperCase();
                UserCurrency uc = userCurrencyRepository.findByUserIdAndCurrencyCode(uid, currencyCode);
                if (uc != null && uc.getExchangeRate() != null) {
                    double calculatedAmount = transaction.getOriginalAmount() * uc.getExchangeRate();
                    transaction.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
                    transaction.setExchangeRate(uc.getExchangeRate());
                } else {
                    throw new IllegalArgumentException("No exchange rate configured for currency: " + currencyCode);
                }
            } else {
                throw new IllegalArgumentException(
                        "Amount cannot be null unless originalAmount and either exchangeRate or originalCurrency are provided");
            }
        }
    }

    private void applyDelta(String userId, Transaction oldTx, Transaction newTx) {
        // We only ever update summaries based on the difference between the old and new contribution.
        // Contribution rules:
        // - only isCountable == 1 contributes
        // - month bucket = transaction.date's (year, month)
        // - CREDIT adds to income, DEBIT adds to expense; net = income - expense
        SummaryContribution oldC = SummaryContribution.from(oldTx);
        SummaryContribution newC = SummaryContribution.from(newTx);

        if (oldC.isZero() && newC.isZero()) {
            return;
        }

        // Subtract old
        if (!oldC.isZero()) {
            updateSummaryRow(userId, oldC, -1);
        }

        // Add new
        if (!newC.isZero()) {
            updateSummaryRow(userId, newC, +1);
        }
    }

    // Applies a signed delta from a SummaryContribution to the
    // corresponding row in account_month_summary.
    //
    // - userId: caller user id (used to enforce ownership of the account).
    // - c: per-transaction contribution for a specific (accountId, year, month).
    // - sign: +1 when adding a new contribution, -1 when removing a previous one.
    private void updateSummaryRow(String userId, SummaryContribution c, int sign) {
        if (c.accountId == null || c.year == null || c.month == null) {
            return;
        }

        // Ensure an account exists and belongs to the caller user before mutating summaries.
        Account acc = accountRepository.findById(c.accountId).orElse(null);
        if (acc == null) {
            return;
        }
        if (userId != null && !userId.equals(acc.getUserId())) {
            return;
        }

        // Upsert the month summary row, then apply the signed delta.
        AccountMonthlySummary row = accountMonthSummaryRepository
                .findByUserIdAndAccountIdAndYearAndMonth(acc.getUserId(), c.accountId, c.year, c.month)
                .orElseGet(() -> {
                    AccountMonthlySummary s = new AccountMonthlySummary();
                    s.setUserId(acc.getUserId());
                    s.setAccountId(c.accountId);
                    s.setYear(c.year);
                    s.setMonth(c.month);
                    s.setIncomeTotal(0.0);
                    s.setExpenseTotal(0.0);
                    s.setNetTotal(0.0);
                    s.setCountCountable(0);
                    return s;
                });

        double income = safe(row.getIncomeTotal());
        double expense = safe(row.getExpenseTotal());
        double net = safe(row.getNetTotal());
        int cnt = row.getCountCountable() != null ? row.getCountCountable() : 0;

        income += sign * c.income;
        expense += sign * c.expense;
        net += sign * c.net;
        cnt += sign * c.count;

        row.setIncomeTotal(income);
        row.setExpenseTotal(expense);
        row.setNetTotal(net);
        row.setCountCountable(cnt);

        accountMonthSummaryRepository.save(row);
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    // Value object describing how a single Transaction affects
    // a particular (accountId, year, month) row in account_month_summary.
    private record SummaryContribution(Long accountId, Integer year, Integer month, double income, double expense,
                                       double net, int count) {

        // Derive the monthly summary contribution for a single Transaction.
        // Returns a "zero" contribution when the transaction should not
        // affect summaries (e.g. not countable / missing data / unknown type).
        static SummaryContribution from(Transaction tx) {
            if (tx == null) {
                return new SummaryContribution(null, null, null, 0.0, 0.0, 0.0, 0);
            }
            Integer isCountable = tx.getIsCountable();
            if (isCountable == null || isCountable != 1) {
                return new SummaryContribution(null, null, null, 0.0, 0.0, 0.0, 0);
            }

            Long accountId = tx.getAccountId();
            Date date = tx.getDate();
            if (accountId == null || date == null) {
                return new SummaryContribution(null, null, null, 0.0, 0.0, 0.0, 0);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;

            double amount = tx.getAmount() != null ? tx.getAmount() : 0.0;

            double income = 0.0;
            double expense = 0.0;

            Integer type = tx.getTransactionType();
            if (type != null && type == 2) {
                income = amount;
            } else if (type != null && type == 1) {
                expense = amount;
            } else {
                return new SummaryContribution(accountId, year, month, 0.0, 0.0, 0.0, 0);
            }

            return new SummaryContribution(accountId, year, month, income, expense, income - expense, 1);
        }

        // True when this contribution has no effect on any summary row.
        boolean isZero() {
            return income == 0.0 && expense == 0.0 && net == 0.0 && count == 0;
        }
    }
}
