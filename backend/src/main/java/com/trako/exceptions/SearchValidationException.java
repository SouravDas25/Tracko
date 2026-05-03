package com.trako.exceptions;

public class SearchValidationException extends RuntimeException {

    private final String field;

    public SearchValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
