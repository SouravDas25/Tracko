package com.trako.models.request;

import jakarta.validation.constraints.NotNull;

public class AccountSaveRequest {

    @NotNull
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
