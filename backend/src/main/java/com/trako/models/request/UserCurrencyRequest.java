package com.trako.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCurrencyRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code")
    private String currencyCode;

    @NotNull
    @Positive
    private Double exchangeRate;
}
