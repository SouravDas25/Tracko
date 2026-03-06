package com.trako.models.external;

import java.util.Map;

/**
 * Simple representation of the external exchange-rate API response
 * with only the fields we currently need.
 */
public class ExchangeRateApiResponse {

    private String baseCode;
    private Map<String, Double> rates;

    public ExchangeRateApiResponse(String baseCode, Map<String, Double> rates) {
        this.baseCode = baseCode;
        this.rates = rates;
    }

    public String getBaseCode() {
        return baseCode;
    }

    public void setBaseCode(String baseCode) {
        this.baseCode = baseCode;
    }

    public Map<String, Double> getRates() {
        return rates;
    }

    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }
}
