package com.trako.services;

import com.trako.repositories.AccountMonthSummaryRepository;
import com.trako.util.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only wrapper around {@link AccountMonthSummaryRepository}.
 *
 * <p>Encapsulates the pre-aggregated {@code account_month_summary} querying semantics used by
 * {@link TransactionService} (month totals and rollover calculations).
 */
@Service
public class AccountSummaryReadOnlyService {

    /**
     * Typed aggregate result for a (user, month) summary.
     *
     * @param income total income
     * @param expense total expense
     * @param net net total (income - expense)
     * @param count count of countable transactions
     */
    public record MonthTotals(double income, double expense, double net, int count) {
    }

    @Autowired
    private AccountMonthSummaryRepository accountMonthSummaryRepository;

    /**
     * Returns month totals (income, expense, net, count) for the given user and month.
     *
     * @param userId     user id
     * @param year       calendar year
     * @param month      calendar month (1-12)
     * @param accountIds optional account filter
     * @return typed totals
     */
    public MonthTotals getMonthTotals(String userId, int year, int month, List<Long> accountIds) {
        Object[] row;
        if (accountIds == null || accountIds.isEmpty()) {
            row = accountMonthSummaryRepository.sumMonthTotals(userId, year, month);
        } else {
            row = accountMonthSummaryRepository.sumMonthTotalsForAccounts(userId, accountIds, year, month);
        }

        row = normalizeAggregateRow(row);
        return new MonthTotals(
                NumberUtil.asDouble(row[0]),
                NumberUtil.asDouble(row[1]),
                NumberUtil.asDouble(row[2]),
                NumberUtil.asInt(row[3])
        );
    }

    private Object[] normalizeAggregateRow(Object[] row) {
        if (row == null) return new Object[]{0.0, 0.0, 0.0, 0};
        if (row.length == 1 && row[0] instanceof Object[]) {
            return (Object[]) row[0];
        }
        return row;
    }

    /**
     * Returns the net rollover (sum of net totals) for all months strictly before the given year/month.
     *
     * @param userId     user id
     * @param year       calendar year
     * @param month      calendar month (1-12)
     * @param accountIds optional account filter
     * @return rollover net (may be null depending on database)
     */
    public Double getRolloverNetBeforeMonth(String userId, int year, int month, List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return accountMonthSummaryRepository.sumNetBeforeMonth(userId, year, month);
        }
        return accountMonthSummaryRepository.sumNetBeforeMonthForAccounts(userId, accountIds, year, month);
    }
}
