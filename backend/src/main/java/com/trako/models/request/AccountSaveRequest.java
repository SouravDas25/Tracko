package com.trako.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountSaveRequest {
    @NotBlank
    @Size(max = 250)
    private String name;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "must be a 3-letter currency code")
    private String currency;
}
