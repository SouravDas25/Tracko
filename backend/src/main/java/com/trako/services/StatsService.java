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
import java.util.*;

@Service
public class StatsService {

    public enum Range {
        weekly,
        monthly,
        yearly,
        custom
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    // private static final SimpleDateFormat DOW_FMT = new SimpleDateFormat("EEE", Locale.ENGLISH);

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
    // private double sumInRange(List<Transaction> txs, Date start, Date endExclusive, int transactionType) {
    //     double total = 0.0;
    //     for (Transaction t : txs) {
    //         if (t.getIsCountable() == null || t.getIsCountable() != 1) continue;
    //         if (t.getTransactionType() == null || t.getTransactionType() != transactionType) continue;
    //         Date d = t.getDate();
    //         if (d.before(start) || !d.before(endExclusive)) continue;
    //         total += (t.getAmount() == null ? 0.0 : t.getAmount());
    //     }
    //     return total;
    // }


    private List<StatsPointDTO> buildSeriesFromDb(String userId, Range range, int transactionType, Long accountId, Date anchorDate, Date currentStart, Date currentEnd) {
        List<Object[]> aggs = transactionRepository.sumAmountsByDateForUser(userId, transactionType, accountId);
        return buildSeriesFromAggs(range, anchorDate, aggs, currentStart, currentEnd);
    }
    
    private List<StatsPointDTO> buildSeriesFromDbCategory(String userId, Range range, int transactionType, Long accountId, Long categoryId, Date anchorDate, Date currentStart, Date currentEnd) {
        List<Object[]> aggs = transactionRepository.sumAmountsByDateForCategory(userId, transactionType, accountId, categoryId);
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
                // Even if empty, we want to generate the zero-filled series for the range
                
                int startKey = toYearMonthKey(currentStart);
                // For endKey, since currentEnd is exclusive (start of next day), 
                // we should look at the last inclusive day (currentEnd - 1ms)
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
                // Generate contiguous days
                long startMs = startOfDay(currentStart).getTime();
                long endMs = startOfDay(addDays(currentEnd, -1)).getTime();
                long oneDayMs = 24L * 60L * 60L * 1000L;

                for (long ts = startMs; ts <= endMs; ts += oneDayMs) {
                    out.add(new StatsPointDTO(DATE_FMT.format(new Date(ts)), byDay.getOrDefault(ts, 0.0)));
                }
                return out;
            }
        }
        
        if (range == Range.yearly) {
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


    /**
     * Returns stats summary for the authenticated user:
     * - current-period total and category breakdown
     * - contiguous historical series by requested granularity
     */
    public StatsResponseDTO getStats(String userId, Range range, int transactionType, Long accountId, Date anchorDate, Date customStartDate, Date customEndDate) {
        // Current period
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

        // Use new database aggregations instead of loading all transactions into memory
        List<StatsPointDTO> series = buildSeriesFromDb(userId, range, transactionType, accountId, now, currentStart, currentEnd);

        // Category breakdown for current period
        Map<Long, String> categoryNames = new HashMap<>();
        List<Category> cats = categoryRepository.findByUserId(userId);
        for (Category c : cats) {
            categoryNames.put(c.getId(), c.getName());
        }

        List<Object[]> catAggs = transactionRepository.sumAmountsByCategoryForUserInRange(
                userId, transactionType, accountId, currentStart, currentEnd);

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

    /**
     * Returns category-scoped stats summary for the authenticated user:
     * - current-period total for category
     * - contiguous historical series for that category by requested granularity
     */
    public CategoryStatsResponseDTO getCategoryStats(String userId, Range range, int transactionType, Long accountId, Date anchorDate, Long categoryId, Date customStartDate, Date customEndDate) {
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
            case yearly:
            default:
                currentStart = startOfYear(now);
                currentEnd = nextYear(currentStart);
                break;
        }

        // Series by requested granularity over available category data using DB aggregations
        List<StatsPointDTO> series = buildSeriesFromDbCategory(userId, range, transactionType, accountId, categoryId, now, currentStart, currentEnd);

        // Total for current period only
        Double sum = transactionRepository.sumAmountForCategoryInRange(
                userId, transactionType, accountId, categoryId, currentStart, currentEnd);
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
