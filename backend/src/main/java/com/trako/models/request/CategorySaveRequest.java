package com.trako.models.request;

import com.trako.entities.CategoryType;
import jakarta.validation.constraints.NotNull;

public class CategorySaveRequest {

    @NotNull
    private String name;

    private CategoryType categoryType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(CategoryType categoryType) {
        this.categoryType = categoryType;
    }
}
