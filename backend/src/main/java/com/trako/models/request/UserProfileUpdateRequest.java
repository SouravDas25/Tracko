package com.trako.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    private String name;

    @Email
    @Size(max = 150)
    private String email;

    private String profilePic;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "must be a 3-letter currency code")
    private String baseCurrency;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

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
