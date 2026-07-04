package com.trako.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionPeriodSummaryDTO extends TransactionSummaryDTO {
    private Integer year;
    private Integer month;

    public TransactionPeriodSummaryDTO(Double totalIncome, Double totalExpense, Double netTotal, Integer transactionCount, Integer year, Integer month) {
        super(totalIncome, totalExpense, netTotal, transactionCount);
        this.year = year;
        this.month = month;
    }
}
