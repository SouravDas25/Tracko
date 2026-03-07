package com.trako.controllers;

import com.trako.models.external.ExchangeRateApiResponse;
import com.trako.services.ExchangeRateService;
import com.trako.util.Response;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rates")
@Validated
public class ExchangeRateController {

    @Autowired
    ExchangeRateService exchangeRateService;

    @GetMapping("/{baseCurrency}")
    public ResponseEntity<?> getRates(
            @PathVariable
            @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code")
            String baseCurrency) {
        ExchangeRateApiResponse data = exchangeRateService.getRates(baseCurrency);
        return Response.ok(data);
    }
}
