package com.trako.models.request;

import jakarta.validation.constraints.NotNull;

public class CategorySaveRequest {

    @NotNull
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
