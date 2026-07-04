package com.trako.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetCategoryDTO {
    private Long categoryId;
    private String categoryName;
    private Double allocatedAmount;
    private Double actualSpent;
    private Double remainingBalance;
}
