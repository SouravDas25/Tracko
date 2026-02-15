package com.trako.dtos;

import java.util.List;

public class StatsResponseDTO {
    private String range;
    private Integer transactionType;
    private String periodStart;
    private String periodEnd;
    private Double total;
    private List<StatsPointDTO> series;
    private List<CategoryStatDTO> categories;

    public StatsResponseDTO() {
    }

    public StatsResponseDTO(String range, Integer transactionType, String periodStart, String periodEnd, Double total,
                            List<StatsPointDTO> series, List<CategoryStatDTO> categories) {
        this.range = range;
        this.transactionType = transactionType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.total = total;
        this.series = series;
        this.categories = categories;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
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

    public List<StatsPointDTO> getSeries() {
        return series;
    }

    public void setSeries(List<StatsPointDTO> series) {
        this.series = series;
    }

    public List<CategoryStatDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryStatDTO> categories) {
        this.categories = categories;
    }
}
