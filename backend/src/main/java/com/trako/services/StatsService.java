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
        yearly
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    private Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date addDays(Date d, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }

    private Date startOfWeek(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.setFirstDayOfWeek(Calendar.MONDAY);
        int current = c.get(Calendar.DAY_OF_WEEK);
        int delta = (current - Calendar.MONDAY + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, -delta);
        return c.getTime();
    }

    private Date startOfMonth(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    private Date startOfYear(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.set(Calendar.DAY_OF_YEAR, 1);
        return c.getTime();
    }

    private Date nextMonth(Date startOfMonth) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfMonth);
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    private Date nextYear(Date startOfYear) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfYear);
        c.add(Calendar.YEAR, 1);
        return c.getTime();
    }

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

    private List<Transaction> filterKindCountable(List<Transaction> txs, int transactionType) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : txs) {
            if (t.getIsCountable() == null || t.getIsCountable() != 1) continue;
            if (t.getTransactionType() == null || t.getTransactionType() != transactionType) continue;
            out.add(t);
        }
        return out;
    }

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

    private List<StatsPointDTO> buildSeriesAllData(Range range, List<Transaction> kindTxs) {
        if (kindTxs == null || kindTxs.isEmpty()) {
            return new ArrayList<>();
        }

        if (range == Range.yearly) {
            TreeMap<Integer, Double> byYear = new TreeMap<>();
            for (Transaction t : kindTxs) {
                Calendar c = Calendar.getInstance();
                c.setTime(t.getDate());
                int year = c.get(Calendar.YEAR);
                byYear.put(year, byYear.getOrDefault(year, 0.0) + (t.getAmount() == null ? 0.0 : t.getAmount()));
            }
            List<StatsPointDTO> out = new ArrayList<>();
            for (var e : byYear.entrySet()) {
                out.add(new StatsPointDTO(String.valueOf(e.getKey()), e.getValue()));
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
            for (var e : byMonth.entrySet()) {
                int year = e.getKey() / 100;
                int month = e.getKey() % 100;
                out.add(new StatsPointDTO(monthLabel(month) + " " + year, e.getValue()));
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
        for (var e : byWeekStart.entrySet()) {
            out.add(new StatsPointDTO(DATE_FMT.format(new Date(e.getKey())), e.getValue()));
        }
        return out;
    }

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
        List<StatsPointDTO> series = buildSeriesAllData(range, kindTxs);

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

        // Series over all time (same behavior as main stats graph)
        List<StatsPointDTO> series = buildSeriesAllData(range, catTxs);

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
