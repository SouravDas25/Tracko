package com.trako.models.request;

import com.trako.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategorySaveRequest {
    @NotBlank
    @Size(max = 250)
    private String name;

    private CategoryType categoryType;
}
