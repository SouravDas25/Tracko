package com.trako.services;

import com.trako.exceptions.NotFoundException;
import com.trako.models.external.ExchangeRateApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    @Value("${exchange-rate.api-url:https://open.er-api.com/v6/latest/}")
    private String apiUrl;

    public ExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ExchangeRateApiResponse getRates(String baseCurrency) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl + baseCurrency,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new NotFoundException("Could not fetch exchange rates for: " + baseCurrency);
        }

        Map<String, Object> body = response.getBody();

        // External API typically uses "base_code" for the base currency key
        Object base = body.get("base_code");
        String resolvedBase = (base instanceof String) ? (String) base : baseCurrency;

        @SuppressWarnings("unchecked")
        Map<String, Double> rates = (Map<String, Double>) body.get("rates");

        if (rates == null || rates.isEmpty()) {
            throw new NotFoundException("No exchange rates available for base currency: " + resolvedBase);
        }

        return new ExchangeRateApiResponse(resolvedBase, rates);
    }
}
