package com.trako.models.request;

import jakarta.validation.constraints.NotNull;

public class UserCurrencyRequest {
    @NotNull
    private String currencyCode;
    @NotNull
    private Double exchangeRate;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}
