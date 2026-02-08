package com.trako.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
public class ExchangeRateService {

    private final String API_URL = "https://open.er-api.com/v6/latest/";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getRates(String baseCurrency) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(API_URL + baseCurrency, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
