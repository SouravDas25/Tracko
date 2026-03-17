package com.trako.enums;

public enum AnalyticsGranularity {
    WEEKLY, MONTHLY, YEARLY;

    public static AnalyticsGranularity fromString(String value) {
        if (value == null || value.isEmpty()) return MONTHLY;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
