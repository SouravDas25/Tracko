package com.trako.dtos;

public class TransactionPeriodSummaryDTO extends TransactionSummaryDTO {
    private Integer year;
    private Integer month;

    public TransactionPeriodSummaryDTO(Double totalIncome, Double totalExpense, Double netTotal, Integer transactionCount, Integer year, Integer month) {
        super(totalIncome, totalExpense, netTotal, transactionCount);
        this.year = year;
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
