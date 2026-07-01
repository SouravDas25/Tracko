package com.trako.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransactionSummaryDTO {
    private Double totalIncome;
    private Double totalExpense;
    private Double netTotal;
    private Double rolloverNet;
    private Double netTotalWithRollover;
    private Integer transactionCount;

    public TransactionSummaryDTO(Double totalIncome, Double totalExpense, Double netTotal, Integer transactionCount) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netTotal = netTotal;
        this.rolloverNet = 0.0;
        this.netTotalWithRollover = netTotal;
        this.transactionCount = transactionCount;
    }

    public TransactionSummaryDTO(Double totalIncome, Double totalExpense, Double netTotal, Double rolloverNet, Double netTotalWithRollover, Integer transactionCount) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netTotal = netTotal;
        this.rolloverNet = rolloverNet;
        this.netTotalWithRollover = netTotalWithRollover;
        this.transactionCount = transactionCount;
    }
}
