package com.trako.models.request;

import com.trako.entities.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategorySaveRequest {

    @NotBlank
    @Size(max = 250)
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
