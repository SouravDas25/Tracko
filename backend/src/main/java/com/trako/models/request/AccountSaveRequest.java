package com.trako.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountSaveRequest {

    @NotBlank
    @Size(max = 250)
    private String name;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "must be a 3-letter currency code")
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
