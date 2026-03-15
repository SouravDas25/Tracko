package com.trako.enums;

public enum AnalyticsGroupBy {
    CATEGORY, ACCOUNT;

    public static AnalyticsGroupBy fromString(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
