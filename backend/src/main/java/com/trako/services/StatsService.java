package com.trako.services;

import com.trako.dtos.CategoryStatDTO;
import com.trako.dtos.CategoryStatsResponseDTO;
import com.trako.dtos.StatsPointDTO;
import com.trako.dtos.StatsResponseDTO;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.*;

@Service
public class StatsService {

    public enum Range {
        weekly,
        monthly,
        yearly
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DOW_FMT = new SimpleDateFormat("EEE", Locale.ENGLISH);

    /**
     * Normalizes a Date to 00:00:00.000 in the server timezone.
     */
    private Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Adds days to a date and returns the resulting Date.
     */
    private Date addDays(Date d, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }

    /**
     * Returns Monday 00:00:00.000 for the week that contains the input date.
     */
    private Date startOfWeek(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.setFirstDayOfWeek(Calendar.MONDAY);
        int current = c.get(Calendar.DAY_OF_WEEK);
        int delta = (current - Calendar.MONDAY + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, -delta);
        return c.getTime();
    }

    /**
     * Returns the first day of the month at 00:00:00.000 for the input date.
     */
    private Date startOfMonth(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    /**
     * Returns the first day of the year at 00:00:00.000 for the input date.
     */
    private Date startOfYear(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.set(Calendar.DAY_OF_YEAR, 1);
        return c.getTime();
    }

    /**
     * Returns the first day of the next month from a month-start date.
     */
    private Date nextMonth(Date startOfMonth) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfMonth);
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    /**
     * Returns the first day of the next year from a year-start date.
     */
    private Date nextYear(Date startOfYear) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfYear);
        c.add(Calendar.YEAR, 1);
        return c.getTime();
    }

    /**
     * Extracts year from the given date.
     */
    private int toYear(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.YEAR);
    }

    /**
     * Converts a date to an integer key in the form YYYYMM.
     */
    private int toYearMonthKey(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        return year * 100 + month;
    }

    /**
     * Adds months to a YYYYMM key and returns the resulting YYYYMM key.
     */
    private int addMonthsToYearMonthKey(int yearMonthKey, int deltaMonths) {
        int year = yearMonthKey / 100;
        int month = yearMonthKey % 100;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, deltaMonths);
        int newYear = c.get(Calendar.YEAR);
        int newMonth = c.get(Calendar.MONTH) + 1;
        return newYear * 100 + newMonth;
    }

    /**
     * Returns the month distance between two YYYYMM keys.
     */
    private int monthsBetweenKeys(int startKey, int endKey) {
        int startYear = startKey / 100;
        int startMonth = startKey % 100;
        int endYear = endKey / 100;
        int endMonth = endKey % 100;
        return (endYear - startYear) * 12 + (endMonth - startMonth);
    }

    /**
     * Sums transaction amounts for a transaction type inside [start, endExclusive).
     */
    private double sumInRange(List<Transaction> txs, Date start, Date endExclusive, int transactionType) {
        double total = 0.0;
        for (Transaction t : txs) {
            if (t.getIsCountable() == null || t.getIsCountable() != 1) continue;
            if (t.getTransactionType() == null || t.getTransactionType() != transactionType) continue;
            Date d = t.getDate();
            if (d.before(start) || !d.before(endExclusive)) continue;
            total += (t.getAmount() == null ? 0.0 : t.getAmount());
        }
        return total;
    }

    /**
     * Filters transactions by countable=1 and the requested transaction type.
     */
    private List<Transaction> filterKindCountable(List<Transaction> txs, int transactionType) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : txs) {
            if (t.getIsCountable() == null || t.getIsCountable() != 1) continue;
            if (t.getTransactionType() == null || t.getTransactionType() != transactionType) continue;
            out.add(t);
        }
        return out;
    }

    /**
     * Builds fixed-size current-period buckets for charts.
     * yearly -> 12 months, monthly -> days in month, weekly -> 7 days.
     */
    private List<StatsPointDTO> buildSeriesForPeriod(Range range, List<Transaction> kindTxs, Date start, Date endExclusive) {
        List<StatsPointDTO> out = new ArrayList<>();
        if (start == null || endExclusive == null) return out;

        if (range == Range.yearly) {
            // 12 months: Jan..Dec
            double[] buckets = new double[12];
            if (kindTxs != null) {
                for (Transaction t : kindTxs) {
                    Date d = t.getDate();
                    if (d == null) continue;
                    if (d.before(start) || !d.before(endExclusive)) continue;
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    int m = c.get(Calendar.MONTH); // 0-11
                    buckets[m] += (t.getAmount() == null ? 0.0 : t.getAmount());
                }
            }
            for (int m = 0; m < 12; m++) {
                out.add(new StatsPointDTO(monthLabel(m + 1), buckets[m]));
            }
            return out;
        }

        if (range == Range.monthly) {
            // Days of month: 1..N
            Calendar c = Calendar.getInstance();
            c.setTime(start);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            double[] buckets = new double[daysInMonth];
            if (kindTxs != null) {
                for (Transaction t : kindTxs) {
                    Date d = t.getDate();
                    if (d == null) continue;
                    if (d.before(start) || !d.before(endExclusive)) continue;
                    Calendar tc = Calendar.getInstance();
                    tc.setTime(d);
                    int dom = tc.get(Calendar.DAY_OF_MONTH); // 1..N
                    if (dom >= 1 && dom <= daysInMonth) {
                        buckets[dom - 1] += (t.getAmount() == null ? 0.0 : t.getAmount());
                    }
                }
            }
            for (int i = 1; i <= daysInMonth; i++) {
                out.add(new StatsPointDTO(String.valueOf(i), buckets[i - 1]));
            }
            return out;
        }

        // weekly: 7 days starting from computed week start (Mon)
        double[] buckets = new double[7];
        if (kindTxs != null) {
            for (Transaction t : kindTxs) {
                Date d = t.getDate();
                if (d == null) continue;
                if (d.before(start) || !d.before(endExclusive)) continue;
                long diffMs = d.getTime() - start.getTime();
                int idx = (int) (diffMs / (24L * 60L * 60L * 1000L));
                if (idx >= 0 && idx < 7) {
                    buckets[idx] += (t.getAmount() == null ? 0.0 : t.getAmount());
                }
            }
        }
        for (int i = 0; i < 7; i++) {
            Date d = addDays(start, i);
            out.add(new StatsPointDTO(DOW_FMT.format(d), buckets[i]));
        }
        return out;
    }

    /**
     * Filters transactions by a specific category id.
     */
    private List<Transaction> filterCategory(List<Transaction> txs, Long categoryId) {
        List<Transaction> out = new ArrayList<>();
        if (txs == null || txs.isEmpty() || categoryId == null) return out;
        for (Transaction t : txs) {
            if (t.getCategoryId() == null) continue;
            if (!categoryId.equals(t.getCategoryId())) continue;
            out.add(t);
        }
        return out;
    }

    /**
     * Builds a time-series aggregated over the user's available transaction history using the requested granularity.
     *
     * Feature: contiguous bucket series (zero-filled)
     * - The backend returns a contiguous series so the UI does not need to infer or "fill gaps".
     * - We find the first bucket that contains any data and then create buckets up to the bucket containing the
     *   provided anchor date (usually "today" or a user-selected reference date).
     * - Any missing bucket between first..anchor is returned with value 0.
     *
     * Semantics (labels):
     * - yearly: bucket per year, label = "YYYY" (e.g., "2025").
     * - monthly: bucket per month across years, label = "Mon YYYY" (e.g., "Jan 2026").
     * - weekly: bucket per week (week start), label = "yyyy-MM-dd" (e.g., "2026-01-05").
     */
    private List<StatsPointDTO> buildSeriesAllData(Range range, List<Transaction> kindTxs, Date anchorDate) {
        if (kindTxs == null || kindTxs.isEmpty()) {
            return new ArrayList<>();
        }

        Date anchor = (anchorDate == null) ? new Date() : anchorDate;

        if (range == Range.yearly) {
            TreeMap<Integer, Double> byYear = new TreeMap<>();
            for (Transaction t : kindTxs) {
                Calendar c = Calendar.getInstance();
                c.setTime(t.getDate());
                int year = c.get(Calendar.YEAR);
                byYear.put(year, byYear.getOrDefault(year, 0.0) + (t.getAmount() == null ? 0.0 : t.getAmount()));
            }
            List<StatsPointDTO> out = new ArrayList<>();

            int minYear = byYear.firstKey();
            int maxYear = Math.max(byYear.lastKey(), toYear(anchor));
            for (int y = minYear; y <= maxYear; y++) {
                out.add(new StatsPointDTO(String.valueOf(y), byYear.getOrDefault(y, 0.0)));
            }
            return out;
        }

        if (range == Range.monthly) {
            TreeMap<Integer, Double> byMonth = new TreeMap<>();
            for (Transaction t : kindTxs) {
                Calendar c = Calendar.getInstance();
                c.setTime(t.getDate());
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH) + 1;
                int key = year * 100 + month;
                byMonth.put(key, byMonth.getOrDefault(key, 0.0) + (t.getAmount() == null ? 0.0 : t.getAmount()));
            }
            List<StatsPointDTO> out = new ArrayList<>();

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
        for (Transaction t : kindTxs) {
            Date weekStart = startOfWeek(t.getDate());
            long key = weekStart.getTime();
            byWeekStart.put(key, byWeekStart.getOrDefault(key, 0.0) + (t.getAmount() == null ? 0.0 : t.getAmount()));
        }
        List<StatsPointDTO> out = new ArrayList<>();

        long minWeekStart = byWeekStart.firstKey();
        long maxWeekStart = Math.max(byWeekStart.lastKey(), startOfWeek(anchor).getTime());
        long oneWeekMs = 7L * 24L * 60L * 60L * 1000L;

        for (long ts = minWeekStart; ts <= maxWeekStart; ts += oneWeekMs) {
            out.add(new StatsPointDTO(DATE_FMT.format(new Date(ts)), byWeekStart.getOrDefault(ts, 0.0)));
        }
        return out;
    }

    /**
     * Returns stats summary for the authenticated user:
     * - current-period total and category breakdown
     * - contiguous historical series by requested granularity
     */
    public StatsResponseDTO getStats(String userId, Range range, int transactionType, Date anchorDate) {
        // Current period
        Date now = (anchorDate == null) ? new Date() : anchorDate;
        Date currentStart;
        Date currentEnd;
        switch (range) {
            case weekly:
                currentStart = startOfWeek(now);
                currentEnd = addDays(currentStart, 7);
                break;
            case monthly:
                currentStart = startOfMonth(now);
                currentEnd = nextMonth(currentStart);
                break;
            case yearly:
            default:
                currentStart = startOfYear(now);
                currentEnd = nextYear(currentStart);
                break;
        }

        System.out.println("[StatsService] computed period range=" + range
                + " anchor=" + now
                + " start=" + DATE_FMT.format(currentStart)
                + " end=" + DATE_FMT.format(addDays(currentEnd, -1)));

        // Fetch all user transactions once and do aggregation in backend.
        // (Requirement: graph should show all periods that have data)
        List<Transaction> allTxs = transactionRepository.findByUserId(userId);
        List<Transaction> kindTxs = filterKindCountable(allTxs, transactionType);
        List<StatsPointDTO> series = buildSeriesAllData(range, kindTxs, now);

        // Category breakdown for current period
        Map<Long, String> categoryNames = new HashMap<>();
        List<Category> cats = categoryRepository.findByUserId(userId);
        for (Category c : cats) {
            categoryNames.put(c.getId(), c.getName());
        }

        Map<Long, Double> byCategory = new HashMap<>();
        for (Transaction t : kindTxs) {
            Date d = t.getDate();
            if (d.before(currentStart) || !d.before(currentEnd)) continue;
            Long cid = t.getCategoryId();
            if (cid == null || cid == 0) continue;
            double amt = (t.getAmount() == null ? 0.0 : t.getAmount());
            byCategory.put(cid, (byCategory.getOrDefault(cid, 0.0)) + amt);
        }

        List<CategoryStatDTO> catStats = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<Long, Double> e : byCategory.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            total += e.getValue();
            catStats.add(new CategoryStatDTO(e.getKey(), categoryNames.getOrDefault(e.getKey(), "Category " + e.getKey()), e.getValue()));
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

    /**
     * Returns category-scoped stats summary for the authenticated user:
     * - current-period total for category
     * - contiguous historical series for that category by requested granularity
     */
    public CategoryStatsResponseDTO getCategoryStats(String userId, Range range, int transactionType, Date anchorDate, Long categoryId) {
        Date now = (anchorDate == null) ? new Date() : anchorDate;
        Date currentStart;
        Date currentEnd;
        switch (range) {
            case weekly:
                currentStart = startOfWeek(now);
                currentEnd = addDays(currentStart, 7);
                break;
            case monthly:
                currentStart = startOfMonth(now);
                currentEnd = nextMonth(currentStart);
                break;
            case yearly:
            default:
                currentStart = startOfYear(now);
                currentEnd = nextYear(currentStart);
                break;
        }

        List<Transaction> allTxs = transactionRepository.findByUserId(userId);
        List<Transaction> kindTxs = filterKindCountable(allTxs, transactionType);
        List<Transaction> catTxs = filterCategory(kindTxs, categoryId);

        // Series by requested granularity over available category data
        List<StatsPointDTO> series = buildSeriesAllData(range, catTxs, now);

        // Total for current period only
        double total = 0.0;
        for (Transaction t : catTxs) {
            Date d = t.getDate();
            if (d == null) continue;
            if (d.before(currentStart) || !d.before(currentEnd)) continue;
            total += (t.getAmount() == null ? 0.0 : t.getAmount());
        }

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

    /**
     * Maps month number (1..12) to English short month name.
     */
    private String monthLabel(int month) {
        switch (month) {
            case 1:
                return "Jan";
            case 2:
                return "Feb";
            case 3:
                return "Mar";
            case 4:
                return "Apr";
            case 5:
                return "May";
            case 6:
                return "Jun";
            case 7:
                return "Jul";
            case 8:
                return "Aug";
            case 9:
                return "Sep";
            case 10:
                return "Oct";
            case 11:
                return "Nov";
            case 12:
                return "Dec";
            default:
                return "";
        }
    }
}
