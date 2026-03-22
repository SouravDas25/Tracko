package com.trako.services;

import com.trako.dtos.CategoryStatDTO;
import com.trako.dtos.CategoryStatsResponseDTO;
import com.trako.dtos.StatsPointDTO;
import com.trako.dtos.StatsResponseDTO;
import com.trako.entities.Category;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.trako.util.DateUtil.*;

@Service
public class StatsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private List<StatsPointDTO> buildSeriesFromDb(String userId, Range range, TransactionType transactionType, Long accountId, Date anchorDate, Date currentStart, Date currentEnd) {
        List<Object[]> aggs = transactionRepository.sumAmountsByDate(userId, TransactionDbType.from(transactionType), accountId, null);
        return buildSeriesFromAggs(range, anchorDate, aggs, currentStart, currentEnd);
    }

    private List<StatsPointDTO> buildSeriesFromDbCategory(String userId, Range range, TransactionType transactionType, Long accountId, Long categoryId, Date anchorDate, Date currentStart, Date currentEnd) {
        List<Object[]> aggs = transactionRepository.sumAmountsByDate(userId, TransactionDbType.from(transactionType), accountId, categoryId);
        return buildSeriesFromAggs(range, anchorDate, aggs, currentStart, currentEnd);
    }

    private List<StatsPointDTO> buildSeriesFromAggs(Range range, Date anchorDate, List<Object[]> aggs, Date currentStart, Date currentEnd) {
        if (aggs == null || aggs.isEmpty()) {
            return new ArrayList<>();
        }

        Date anchor = (anchorDate == null) ? new Date() : anchorDate;

        if (range == Range.custom) {
            long diff = currentEnd.getTime() - currentStart.getTime();
            long days = diff / (24 * 60 * 60 * 1000);

            if (days > 62) {
                // Use Monthly Granularity for large ranges (> 2 months)
                TreeMap<Integer, Double> byMonth = new TreeMap<>();
                for (Object[] row : aggs) {
                    Date d = (Date) row[0];
                    if (d.before(currentStart) || !d.before(currentEnd)) continue;
                    Double amt = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    int key = toYearMonthKey(d);
                    byMonth.put(key, byMonth.getOrDefault(key, 0.0) + amt);
                }

                List<StatsPointDTO> out = new ArrayList<>();
                int startKey = toYearMonthKey(currentStart);
                int endKey = toYearMonthKey(addDays(currentEnd, -1));

                int spanMonths = monthsBetweenKeys(startKey, endKey);
                for (int i = 0; i <= spanMonths; i++) {
                    int key = addMonthsToYearMonthKey(startKey, i);
                    int year = key / 100;
                    int month = key % 100;
                    out.add(new StatsPointDTO(monthLabel(month) + " " + year, byMonth.getOrDefault(key, 0.0)));
                }
                return out;
            } else {
                // Use Daily Granularity for short ranges
                TreeMap<Long, Double> byDay = new TreeMap<>();
                for (Object[] row : aggs) {
                    Date d = (Date) row[0];
                    if (d.before(currentStart) || !d.before(currentEnd)) continue;
                    Double amt = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    long key = startOfDay(d).getTime();
                    byDay.put(key, byDay.getOrDefault(key, 0.0) + amt);
                }
                List<StatsPointDTO> out = new ArrayList<>();
                long startMs = startOfDay(currentStart).getTime();
                long endMs = startOfDay(addDays(currentEnd, -1)).getTime();
                long oneDayMs = 24L * 60L * 60L * 1000L;

                for (long ts = startMs; ts <= endMs; ts += oneDayMs) {
                    out.add(new StatsPointDTO(DATE_FMT.format(new Date(ts)), byDay.getOrDefault(ts, 0.0)));
                }
                return out;
            }
        }

        if (range == Range.yearly || range == Range.fiveYearly || range == Range.tenYearly) {
            TreeMap<Integer, Double> byYear = new TreeMap<>();
            for (Object[] row : aggs) {
                Date d = (Date) row[0];
                Double amt = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                int year = toYear(d);
                byYear.put(year, byYear.getOrDefault(year, 0.0) + amt);
            }
            List<StatsPointDTO> out = new ArrayList<>();
            if (byYear.isEmpty()) return out;

            int minYear = byYear.firstKey();
            int maxYear = Math.max(byYear.lastKey(), toYear(anchor));
            for (int y = minYear; y <= maxYear; y++) {
                out.add(new StatsPointDTO(String.valueOf(y), byYear.getOrDefault(y, 0.0)));
            }
            return out;
        }

        if (range == Range.monthly) {
            TreeMap<Integer, Double> byMonth = new TreeMap<>();
            for (Object[] row : aggs) {
                Date d = (Date) row[0];
                Double amt = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                int key = toYearMonthKey(d);
                byMonth.put(key, byMonth.getOrDefault(key, 0.0) + amt);
            }
            List<StatsPointDTO> out = new ArrayList<>();
            if (byMonth.isEmpty()) return out;

            int minKey = byMonth.firstKey();
            int anchorKey = toYearMonthKey(anchor);
            int maxKey = Math.max(byMonth.lastKey(), anchorKey);

            int spanMonths = monthsBetweenKeys(minKey, maxKey);
            for (int i = 0; i <= spanMonths; i++) {
                int key = addMonthsToYearMonthKey(minKey, i);
                int year = key / 100;
                int month = key % 100;
                out.add(new StatsPointDTO(monthLabel(month) + " " + year, byMonth.getOrDefault(key, 0.0)));
            }
            return out;
        }

        // weekly
        TreeMap<Long, Double> byWeekStart = new TreeMap<>();
        for (Object[] row : aggs) {
            Date d = (Date) row[0];
            Double amt = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            Date weekStart = startOfWeek(d);
            long key = weekStart.getTime();
            byWeekStart.put(key, byWeekStart.getOrDefault(key, 0.0) + amt);
        }
        List<StatsPointDTO> out = new ArrayList<>();
        if (byWeekStart.isEmpty()) return out;

        long minWeekStart = byWeekStart.firstKey();
        long maxWeekStart = Math.max(byWeekStart.lastKey(), startOfWeek(anchor).getTime());
        long oneWeekMs = 7L * 24L * 60L * 60L * 1000L;

        for (long ts = minWeekStart; ts <= maxWeekStart; ts += oneWeekMs) {
            out.add(new StatsPointDTO(DATE_FMT.format(new Date(ts)), byWeekStart.getOrDefault(ts, 0.0)));
        }
        return out;
    }

    public StatsResponseDTO getStats(String userId, Range range, TransactionType transactionType, Long accountId, Date anchorDate, Date customStartDate, Date customEndDate) {
        Date now = (anchorDate == null) ? new Date() : anchorDate;
        Date currentStart;
        Date currentEnd;
        switch (range) {
            case custom:
                currentStart = (customStartDate != null) ? startOfDay(customStartDate) : startOfDay(now);
                currentEnd = (customEndDate != null) ? addDays(startOfDay(customEndDate), 1) : addDays(currentStart, 1);
                break;
            case weekly:
                currentStart = startOfWeek(now);
                currentEnd = addDays(currentStart, 7);
                break;
            case monthly:
                currentStart = startOfMonth(now);
                currentEnd = nextMonth(currentStart);
                break;
            case fiveYearly:
                currentStart = startOfYear(addYears(now, -4));
                currentEnd = nextYear(startOfYear(now));
                break;
            case tenYearly:
                currentStart = startOfYear(addYears(now, -9));
                currentEnd = nextYear(startOfYear(now));
                break;
            case yearly:
            default:
                currentStart = startOfYear(now);
                currentEnd = nextYear(currentStart);
                break;
        }

        System.out.println("[StatsService] computed period range=" + range
                + " accountId=" + accountId
                + " anchor=" + now
                + " start=" + DATE_FMT.format(currentStart)
                + " end=" + DATE_FMT.format(addDays(currentEnd, -1)));

        List<StatsPointDTO> series = buildSeriesFromDb(userId, range, transactionType, accountId, now, currentStart, currentEnd);

        Map<Long, String> categoryNames = new HashMap<>();
        List<Category> cats = categoryRepository.findByUserId(userId);
        for (Category c : cats) {
            categoryNames.put(c.getId(), c.getName());
        }

        List<Object[]> catAggs = transactionRepository.sumAmountsByCategoryForUserInRange(
                userId, TransactionDbType.from(transactionType), accountId, currentStart, currentEnd);

        List<CategoryStatDTO> catStats = new ArrayList<>();
        double total = 0.0;

        for (Object[] row : catAggs) {
            Long cid = row[0] != null ? ((Number) row[0]).longValue() : null;
            if (cid == null || cid == 0) continue;

            Double amt = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            if (amt <= 0) continue;

            total += amt;
            catStats.add(new CategoryStatDTO(cid, categoryNames.getOrDefault(cid, "Category " + cid), amt));
        }

        catStats.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        return new StatsResponseDTO(
                range.name(),
                transactionType,
                DATE_FMT.format(currentStart),
                DATE_FMT.format(addDays(currentEnd, -1)),
                total,
                series,
                catStats
        );
    }

    public CategoryStatsResponseDTO getCategoryStats(String userId, Range range, TransactionType transactionType, Long accountId, Date anchorDate, Long categoryId, Date customStartDate, Date customEndDate) {
        Date now = (anchorDate == null) ? new Date() : anchorDate;
        Date currentStart;
        Date currentEnd;
        switch (range) {
            case custom:
                currentStart = (customStartDate != null) ? startOfDay(customStartDate) : startOfDay(now);
                currentEnd = (customEndDate != null) ? addDays(startOfDay(customEndDate), 1) : addDays(currentStart, 1);
                break;
            case weekly:
                currentStart = startOfWeek(now);
                currentEnd = addDays(currentStart, 7);
                break;
            case monthly:
                currentStart = startOfMonth(now);
                currentEnd = nextMonth(currentStart);
                break;
            case fiveYearly:
                currentStart = startOfYear(addYears(now, -4));
                currentEnd = nextYear(startOfYear(now));
                break;
            case tenYearly:
                currentStart = startOfYear(addYears(now, -9));
                currentEnd = nextYear(startOfYear(now));
                break;
            case yearly:
            default:
                currentStart = startOfYear(now);
                currentEnd = nextYear(currentStart);
                break;
        }

        List<StatsPointDTO> series = buildSeriesFromDbCategory(userId, range, transactionType, accountId, categoryId, now, currentStart, currentEnd);

        Double sum = transactionRepository.sumAmountForCategoryInRange(
                userId, TransactionDbType.from(transactionType), accountId, categoryId, currentStart, currentEnd);
        double total = sum != null ? sum : 0.0;

        return new CategoryStatsResponseDTO(
                range.name(),
                transactionType,
                categoryId,
                DATE_FMT.format(currentStart),
                DATE_FMT.format(addDays(currentEnd, -1)),
                total,
                series
        );
    }

    public enum Range {
        weekly,
        monthly,
        yearly,
        fiveYearly,
        tenYearly,
        custom
    }
}
