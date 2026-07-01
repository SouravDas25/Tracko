package com.trako.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BudgetResponseDTO {
    private Integer month;
    private Integer year;
    private Double totalBudget;
    private Double totalIncome;
    private Double totalSpent;
    private Double rolloverAmount;
    private Double availableToAssign;
    private Boolean isClosed;
    private List<BudgetCategoryDTO> categories;
}
