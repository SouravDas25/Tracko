package com.trako.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TransactionDbType {
    DEBIT(1),
    CREDIT(2);

    public static final int TRANSFER_RENDERING_VALUE = 3; // Virtual type for frontend rendering

    private final int value;

    TransactionDbType(int value) {
        this.value = value;
    }

    @JsonCreator
    public static TransactionDbType fromValue(int value) {
        return Arrays.stream(TransactionDbType.values())
                .filter(t -> t.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TransactionDbType value: " + value));
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}
