package com.trako.controllers;

import com.trako.services.ExchangeRateService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
public class ExchangeRateController {

    @Autowired
    ExchangeRateService exchangeRateService;

    @GetMapping("/{baseCurrency}")
    public ResponseEntity<?> getRates(@PathVariable String baseCurrency) {
        Map<String, Object> data = exchangeRateService.getRates(baseCurrency);
        if (data != null) {
            return Response.ok(data);
        } else {
            return Response.notFound("Could not fetch exchange rates");
        }
    }
}
