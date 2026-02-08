package com.trako.models.request;

import jakarta.validation.constraints.NotNull;

public class AccountSaveRequest {

    @NotNull
    private String name;

    private String currency;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
