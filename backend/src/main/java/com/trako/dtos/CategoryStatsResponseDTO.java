package com.trako.dtos;

import com.trako.entities.TransactionType;

import java.util.List;

public class CategoryStatsResponseDTO {
    private String range;
    private TransactionType transactionType;
    private Long categoryId;
    private String periodStart;
    private String periodEnd;
    private Double total;
    private List<StatsPointDTO> series;

    public CategoryStatsResponseDTO() {
    }

    public CategoryStatsResponseDTO(String range,
                                    TransactionType transactionType,
                                    Long categoryId,
                                    String periodStart,
                                    String periodEnd,
                                    Double total,
                                    List<StatsPointDTO> series) {
        this.range = range;
        this.transactionType = transactionType;
        this.categoryId = categoryId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.total = total;
        this.series = series;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<StatsPointDTO> getSeries() {
        return series;
    }

    public void setSeries(List<StatsPointDTO> series) {
        this.series = series;
    }
}
