package com.trako.services;

import com.trako.dtos.*;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.enums.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.trako.util.DateUtil.*;

@Service
public class AnalyticsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Main entry point for the analytics chart endpoint.
     * Converts inclusive date range to exclusive, delegates to the appropriate grouping
     * strategy based on the groupBy parameter, and computes the total across all series.
     * Returns a uniform AnalyticsResponseDTO with a groupedSeries list.
     */
    public AnalyticsResponseDTO getChartData(String userId, TransactionType transactionType,
                                             Date startDate, Date endDate,
                                             AnalyticsGranularity granularity, AnalyticsGroupBy groupBy,
                                             List<Long> accountIds, List<Long> categoryIds) {
        if (granularity == null) granularity = AnalyticsGranularity.MONTHLY;

        Date currentStart = startOfDay(startDate);
        Date currentEnd = addDays(startOfDay(endDate), 1);
        TransactionDbType dbType = TransactionDbType.from(transactionType);

        List<NamedSeriesDTO> groupedSeries;
        if (groupBy == AnalyticsGroupBy.CATEGORY && (categoryIds == null || categoryIds.isEmpty())) {
            groupedSeries = buildGroupedByCategory(userId, dbType, granularity, accountIds, currentStart, currentEnd);
        } else if (groupBy == AnalyticsGroupBy.ACCOUNT && (accountIds == null || accountIds.isEmpty())) {
            groupedSeries = buildGroupedByAccount(userId, dbType, granularity, categoryIds, currentStart, currentEnd);
        } else {
            groupedSeries = buildUngrouped(userId, dbType, granularity, accountIds, categoryIds, currentStart, currentEnd);
        }

        double total = groupedSeries.stream()
                .flatMap(ns -> ns.getSeries().stream())
                .mapToDouble(pt -> pt.getValue() != null ? pt.getValue() : 0.0)
                .sum();

        return new AnalyticsResponseDTO(
                granularity.name().toLowerCase(),
                transactionType,
                DATE_FMT.format(currentStart),
                DATE_FMT.format(addDays(currentEnd, -1)),
                total,
                groupedSeries
        );
    }

    // ── Series building ──

    /**
     * Dispatches to the appropriate granularity-specific series builder.
     * Returns an empty series with zero-valued buckets if no data rows exist,
     * ensuring the chart always has the correct number of data points.
     */
    private List<StatsPointDTO> buildSeries(AnalyticsGranularity granularity, List<DateAmountRow> rows,
                                            Date currentStart, Date currentEnd) {
        if (rows == null || rows.isEmpty()) {
            return buildEmptySeries(granularity, currentStart, currentEnd);
        }
        return switch (granularity) {
            case YEARLY -> buildYearlySeries(rows, currentStart, currentEnd);
            case WEEKLY -> buildWeeklySeries(rows, currentStart, currentEnd);
            default -> buildMonthlySeries(rows, currentStart, currentEnd);
        };
    }

    /**
     * Generates a zero-filled series covering the full date range.
     * Produces one StatsPointDTO per bucket (year, month, or week) so the
     * chart renders a complete axis even when there are no transactions.
     */
    private List<StatsPointDTO> buildEmptySeries(AnalyticsGranularity granularity, Date currentStart, Date currentEnd) {
        List<StatsPointDTO> out = new ArrayList<>();
        switch (granularity) {
            case YEARLY: {
                int startYear = toYear(currentStart);
                int endYear = toYear(addDays(currentEnd, -1));
                for (int y = startYear; y <= endYear; y++)
                    out.add(new StatsPointDTO(String.valueOf(y), 0.0));
                break;
            }
            case WEEKLY: {
                long startMs = startOfWeek(currentStart).getTime();
                long endMs = startOfWeek(addDays(currentEnd, -1)).getTime();
                long oneWeekMs = 7L * 24 * 60 * 60 * 1000;
                for (long ts = startMs; ts <= endMs; ts += oneWeekMs)
                    out.add(new StatsPointDTO(DATE_FMT.format(new Date(ts)), 0.0));
                break;
            }
            default: {
                int startKey = toYearMonthKey(currentStart);
                int endKey = toYearMonthKey(addDays(currentEnd, -1));
                int spanMonths = monthsBetweenKeys(startKey, endKey);
                for (int i = 0; i <= spanMonths; i++) {
                    int key = addMonthsToYearMonthKey(startKey, i);
                    out.add(new StatsPointDTO(monthLabel(key % 100) + " " + key / 100, 0.0));
                }
                break;
            }
        }
        return out;
    }

    /**
     * Buckets transaction amounts by calendar year within [currentStart, currentEnd).
     * Filters out rows outside the range, sums amounts per year, and fills
     * gaps with 0.0 so every year in the span has a data point.
     */
    private List<StatsPointDTO> buildYearlySeries(List<DateAmountRow> rows, Date currentStart, Date currentEnd) {
        TreeMap<Integer, Double> byYear = new TreeMap<>();
        for (DateAmountRow row : rows) {
            if (row.date().before(currentStart) || !row.date().before(currentEnd)) continue;
            byYear.merge(toYear(row.date()), row.amount(), Double::sum);
        }
        List<StatsPointDTO> out = new ArrayList<>();
        int startYear = toYear(currentStart);
        int endYear = toYear(addDays(currentEnd, -1));
        for (int y = startYear; y <= endYear; y++)
            out.add(new StatsPointDTO(String.valueOf(y), byYear.getOrDefault(y, 0.0)));
        return out;
    }

    /**
     * Buckets transaction amounts by year-month within [currentStart, currentEnd).
     * Uses a composite YYYYMM key for grouping, then iterates through every
     * month in the span to produce a continuous series with zero-filled gaps.
     */
    private List<StatsPointDTO> buildMonthlySeries(List<DateAmountRow> rows, Date currentStart, Date currentEnd) {
        TreeMap<Integer, Double> byMonth = new TreeMap<>();
        for (DateAmountRow row : rows) {
            if (row.date().before(currentStart) || !row.date().before(currentEnd)) continue;
            byMonth.merge(toYearMonthKey(row.date()), row.amount(), Double::sum);
        }
        List<StatsPointDTO> out = new ArrayList<>();
        int startKey = toYearMonthKey(currentStart);
        int endKey = toYearMonthKey(addDays(currentEnd, -1));
        int spanMonths = monthsBetweenKeys(startKey, endKey);
        for (int i = 0; i <= spanMonths; i++) {
            int key = addMonthsToYearMonthKey(startKey, i);
            out.add(new StatsPointDTO(monthLabel(key % 100) + " " + key / 100, byMonth.getOrDefault(key, 0.0)));
        }
        return out;
    }

    /**
     * Buckets transaction amounts by ISO week start within [currentStart, currentEnd).
     * Aligns each transaction to its Monday, sums per week, and iterates in
     * 7-day steps to produce a continuous weekly series with zero-filled gaps.
     */
    private List<StatsPointDTO> buildWeeklySeries(List<DateAmountRow> rows, Date currentStart, Date currentEnd) {
        TreeMap<Long, Double> byWeekStart = new TreeMap<>();
        for (DateAmountRow row : rows) {
            if (row.date().before(currentStart) || !row.date().before(currentEnd)) continue;
            byWeekStart.merge(startOfWeek(row.date()).getTime(), row.amount(), Double::sum);
        }
        List<StatsPointDTO> out = new ArrayList<>();
        long startMs = startOfWeek(currentStart).getTime();
        long endMs = startOfWeek(addDays(currentEnd, -1)).getTime();
        long oneWeekMs = 7L * 24 * 60 * 60 * 1000;
        for (long ts = startMs; ts <= endMs; ts += oneWeekMs)
            out.add(new StatsPointDTO(DATE_FMT.format(new Date(ts)), byWeekStart.getOrDefault(ts, 0.0)));
        return out;
    }

    // ── Grouping strategies ──

    /**
     * Builds a single aggregated series across all matching transactions.
     * If exactly one category is selected, the series is named after that category;
     * otherwise it is named "All". Returns a singleton list for uniform response shape.
     */
    private List<NamedSeriesDTO> buildUngrouped(String userId, TransactionDbType dbType,
                                                AnalyticsGranularity granularity,
                                                List<Long> accountIds, List<Long> categoryIds,
                                                Date currentStart, Date currentEnd) {
        List<DateAmountRow> rows = transactionRepository
                .sumAmountsByDateFiltered(userId, dbType, accountIds, categoryIds);
        List<StatsPointDTO> series = buildSeries(granularity, rows, currentStart, currentEnd);

        String name = "All";
        if (categoryIds != null && categoryIds.size() == 1) {
            Category cat = categoryRepository.findById(categoryIds.get(0)).orElse(null);
            if (cat != null) name = cat.getName();
        }
        return Collections.singletonList(new NamedSeriesDTO(name, series));
    }

    /**
     * Partitions grouped query rows by entity ID and builds one NamedSeriesDTO per entity.
     * Resolves entity names from the provided nameMap, falling back to "prefix + id".
     * Skips entities whose series is entirely zero to keep the chart clean.
     */
    private List<NamedSeriesDTO> buildGroupedSeries(List<GroupedDateAmountRow> rows,
                                                    Map<Long, String> nameMap, String fallbackPrefix,
                                                    AnalyticsGranularity granularity,
                                                    Date currentStart, Date currentEnd) {
        LinkedHashMap<Long, List<DateAmountRow>> byEntity = new LinkedHashMap<>();
        for (GroupedDateAmountRow row : rows) {
            if (row.entityId() == null || row.entityId() == 0) continue;
            byEntity.computeIfAbsent(row.entityId(), k -> new ArrayList<>())
                    .add(new DateAmountRow(row.date(), row.amount()));
        }

        List<NamedSeriesDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<DateAmountRow>> entry : byEntity.entrySet()) {
            String name = nameMap.getOrDefault(entry.getKey(), fallbackPrefix + " " + entry.getKey());
            List<StatsPointDTO> series = buildSeries(granularity, entry.getValue(), currentStart, currentEnd);
            if (series.stream().anyMatch(pt -> pt.getValue() != null && pt.getValue() > 0)) {
                result.add(new NamedSeriesDTO(name, series));
            }
        }
        return result;
    }

    /**
     * Groups chart data by category — one line series per category.
     * Only called when no category filter is active (checked in getChartData).
     */
    private List<NamedSeriesDTO> buildGroupedByCategory(String userId, TransactionDbType dbType,
                                                        AnalyticsGranularity granularity,
                                                        List<Long> accountIds,
                                                        Date currentStart, Date currentEnd) {
        List<GroupedDateAmountRow> rows = transactionRepository
                .sumAmountsByDateGroupedByCategoryFiltered(userId, dbType, accountIds, currentStart, currentEnd);

        Map<Long, String> nameMap = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return buildGroupedSeries(rows, nameMap, "Category", granularity, currentStart, currentEnd);
    }

    /**
     * Groups chart data by account — one line series per account.
     * Only called when no account filter is active (checked in getChartData).
     */
    private List<NamedSeriesDTO> buildGroupedByAccount(String userId, TransactionDbType dbType,
                                                       AnalyticsGranularity granularity,
                                                       List<Long> categoryIds,
                                                       Date currentStart, Date currentEnd) {
        List<GroupedDateAmountRow> rows = transactionRepository
                .sumAmountsByDateGroupedByAccountFiltered(userId, dbType, categoryIds, currentStart, currentEnd);

        Map<Long, String> nameMap = accountRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));

        return buildGroupedSeries(rows, nameMap, "Account", granularity, currentStart, currentEnd);
    }
}
