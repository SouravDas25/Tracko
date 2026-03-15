package com.trako.dtos;

import com.trako.enums.TransactionType;

import java.util.List;

public class AnalyticsResponseDTO {
    private String granularity;
    private TransactionType transactionType;
    private String periodStart;
    private String periodEnd;
    private Double total;
    private List<NamedSeriesDTO> groupedSeries;

    public AnalyticsResponseDTO() {
    }

    public AnalyticsResponseDTO(String granularity, TransactionType transactionType, String periodStart,
                                String periodEnd, Double total, List<NamedSeriesDTO> groupedSeries) {
        this.granularity = granularity;
        this.transactionType = transactionType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.total = total;
        this.groupedSeries = groupedSeries;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
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

    public List<NamedSeriesDTO> getGroupedSeries() {
        return groupedSeries;
    }

    public void setGroupedSeries(List<NamedSeriesDTO> groupedSeries) {
        this.groupedSeries = groupedSeries;
    }
}
