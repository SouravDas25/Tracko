package com.trako.services;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

@Service
public class ExchangeRateService {

    private final String API_URL = "https://open.er-api.com/v6/latest/";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getRates(String baseCurrency) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(API_URL + baseCurrency, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        return null;
    }
}
