package com.trako.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSaveRequest {
    private String name;

    @NotBlank
    @Size(max = 32)
    private String phoneNo;

    @Email
    @Size(max = 150)
    private String email;

    private String profilePic;

    @NotBlank
    @Size(min = 4, max = 250)
    @JsonProperty("password")
    private String password;

    private Integer isShadow;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "must be a 3-letter currency code")
    private String baseCurrency;

    public boolean isShadow() {
        return isShadow != null && isShadow == 1;
    }

    @Override
    public String toString() {
        return "User{" +
                ", name='" + name + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
