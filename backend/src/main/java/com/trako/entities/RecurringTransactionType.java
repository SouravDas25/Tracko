package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RecurringTransactionType {
    DEBIT(1),
    CREDIT(2),
    TRANSFER(3);

    private final int value;

    RecurringTransactionType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static RecurringTransactionType fromValue(int value) {
        return Arrays.stream(RecurringTransactionType.values())
                .filter(t -> t.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RecurringTransactionType value: " + value));
    }
}
