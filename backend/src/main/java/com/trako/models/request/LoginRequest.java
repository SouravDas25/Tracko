package com.trako.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank
    @Size(max = 150)
    private String username;

    @NotBlank
    @Size(max = 250)
    private String password;
}
