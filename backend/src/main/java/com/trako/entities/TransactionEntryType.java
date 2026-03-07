package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TransactionEntryType {
    DEBIT(1),
    CREDIT(2);

    public static final int TRANSFER_RENDERING_VALUE = 3; // Virtual type for frontend rendering

    private final int value;

    TransactionEntryType(int value) {
        this.value = value;
    }

    @JsonCreator
    public static TransactionEntryType fromValue(int value) {
        return Arrays.stream(TransactionEntryType.values())
                .filter(t -> t.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TransactionEntryType value: " + value));
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}
