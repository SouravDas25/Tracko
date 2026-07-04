package com.trako.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateRequest {
    private String name;

    @Email
    @Size(max = 150)
    private String email;

    private String profilePic;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "must be a 3-letter currency code")
    private String baseCurrency;

    @Override
    public String toString() {
        return "UserProfileUpdateRequest{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", baseCurrency='" + baseCurrency + '\'' +
                '}';
    }
}
