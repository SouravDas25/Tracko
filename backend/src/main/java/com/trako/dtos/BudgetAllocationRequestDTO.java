package com.trako.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetAllocationRequestDTO {
    @NotNull
    private Integer month;

    @NotNull
    private Integer year;

    @NotNull
    private Long categoryId;

    @NotNull
    @Positive
    private Double amount;
}
