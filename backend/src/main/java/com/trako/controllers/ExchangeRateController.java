package com.trako.controllers;

import com.trako.models.external.ExchangeRateApiResponse;
import com.trako.services.ExchangeRateService;
import com.trako.util.Response;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Exchange Rates", description = "Fetch live exchange rates")
@RestController
@RequestMapping("/api/exchange-rates")
@Validated
public class ExchangeRateController {

    @Autowired
    ExchangeRateService exchangeRateService;

    @Operation(summary = "Get exchange rates for a base currency (e.g. USD)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ExchangeRateApiResponse.class)))
    @GetMapping("/{baseCurrency}")
    public ResponseEntity<?> getRates(
            @PathVariable
            @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code")
            String baseCurrency) {
        ExchangeRateApiResponse data = exchangeRateService.getRates(baseCurrency);
        return Response.ok(data);
    }
}
