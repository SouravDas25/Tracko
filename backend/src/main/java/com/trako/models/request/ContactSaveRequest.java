package com.trako.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactSaveRequest {
    @NotBlank
    @Size(max = 250)
    private String name;

    @Size(max = 32)
    private String phoneNo;

    @Size(max = 150)
    private String email;
}
