package com.trako.dtos;

public class TransactionSummaryDTO {
    private Double totalIncome;
    private Double totalExpense;
    private Double netTotal;
    private Integer transactionCount;

    public TransactionSummaryDTO() {
    }

    public TransactionSummaryDTO(Double totalIncome, Double totalExpense, Double netTotal, Integer transactionCount) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netTotal = netTotal;
        this.transactionCount = transactionCount;
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(Double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }
}
