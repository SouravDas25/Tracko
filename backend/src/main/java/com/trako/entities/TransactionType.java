package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TransactionType {
    DEBIT(1),
    CREDIT(2),
    TRANSFER(3);

    private final int value;

    TransactionType(int value) {
        this.value = value;
    }

    @JsonCreator
    public static TransactionType fromValue(int value) {
        return Arrays.stream(TransactionType.values())
                .filter(t -> t.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TransactionType value: " + value));
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}
