package com.trako.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthicationRequest {
    @NotBlank
    @Size(max = 32)
    String phoneNo;

    @NotBlank
    @Size(max = 250)
    String password;
}
