package com.trako.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Shared date utility methods used by StatsService and AnalyticsService
 * for date manipulation, bucketing keys, and label formatting.
 */
public final class DateUtil {

    /** Standard date format (yyyy-MM-dd) used for API responses and series labels. */
    public static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    private DateUtil() {}

    /** Truncates a date to midnight (00:00:00.000) of the same day. */
    public static Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** Returns a new date shifted by the given number of days (positive or negative). */
    public static Date addDays(Date d, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }

    /**
     * Returns the Monday at midnight for the week containing the given date.
     * Uses ISO week convention (Monday = first day of week).
     */
    public static Date startOfWeek(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.setFirstDayOfWeek(Calendar.MONDAY);
        int current = c.get(Calendar.DAY_OF_WEEK);
        int delta = (current - Calendar.MONDAY + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, -delta);
        return c.getTime();
    }

    /** Returns the first day (midnight) of the month containing the given date. */
    public static Date startOfMonth(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    /** Returns January 1st (midnight) of the year containing the given date. */
    public static Date startOfYear(Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(now));
        c.set(Calendar.DAY_OF_YEAR, 1);
        return c.getTime();
    }

    /** Returns the first day of the next month from the given start-of-month date. */
    public static Date nextMonth(Date startOfMonth) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfMonth);
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    /** Returns January 1st of the next year from the given start-of-year date. */
    public static Date nextYear(Date startOfYear) {
        Calendar c = Calendar.getInstance();
        c.setTime(startOfYear);
        c.add(Calendar.YEAR, 1);
        return c.getTime();
    }

    /** Extracts the 4-digit year from a date. */
    public static int toYear(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.YEAR);
    }

    /**
     * Converts a date to a composite YYYYMM integer key (e.g. 202503 for March 2025).
     * Used for monthly bucketing — allows simple integer comparison and iteration.
     */
    public static int toYearMonthKey(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        return year * 100 + month;
    }

    /**
     * Adds deltaMonths to a YYYYMM key and returns the resulting YYYYMM key.
     * Handles year rollover (e.g. 202512 + 1 = 202601).
     */
    public static int addMonthsToYearMonthKey(int yearMonthKey, int deltaMonths) {
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
     * Returns the number of months between two YYYYMM keys (inclusive difference).
     * E.g. monthsBetweenKeys(202501, 202503) = 2.
     */
    public static int monthsBetweenKeys(int startKey, int endKey) {
        int startYear = startKey / 100;
        int startMonth = startKey % 100;
        int endYear = endKey / 100;
        int endMonth = endKey % 100;
        return (endYear - startYear) * 12 + (endMonth - startMonth);
    }

    /** Returns the 3-letter abbreviation for a 1-based month number (1=Jan, 12=Dec). */
    public static String monthLabel(int month) {
        switch (month) {
            case 1: return "Jan";
            case 2: return "Feb";
            case 3: return "Mar";
            case 4: return "Apr";
            case 5: return "May";
            case 6: return "Jun";
            case 7: return "Jul";
            case 8: return "Aug";
            case 9: return "Sep";
            case 10: return "Oct";
            case 11: return "Nov";
            case 12: return "Dec";
            default: return "";
        }
    }
}
