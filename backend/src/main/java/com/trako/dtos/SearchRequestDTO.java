package com.trako.dtos;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

public class SearchRequestDTO {
    @NotBlank(message = "Search query cannot be blank")
    @Size(min = 1, max = 200, message = "Search query must be between 1 and 200 characters")
    private String query;

    @PositiveOrZero(message = "Page must be zero or positive")
    private Integer page = 0;

    @Positive(message = "Page size must be positive")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 20;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    @DecimalMin(value = "0.0", message = "Minimum amount cannot be negative")
    private Double minAmount;

    @DecimalMin(value = "0.0", message = "Maximum amount cannot be negative")
    private Double maxAmount;

    private List<Long> accountIds;

    private Long categoryId;

    @DecimalMin(value = "0.0", message = "Fuzzy threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Fuzzy threshold must be between 0.0 and 1.0")
    private Double fuzzyThreshold = 0.7;

    private Boolean expand = false;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Double getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(Double minAmount) {
        this.minAmount = minAmount;
    }

    public Double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(Double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public List<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Double getFuzzyThreshold() {
        return fuzzyThreshold;
    }

    public void setFuzzyThreshold(Double fuzzyThreshold) {
        this.fuzzyThreshold = fuzzyThreshold;
    }

    public Boolean getExpand() {
        return expand;
    }

    public void setExpand(Boolean expand) {
        this.expand = expand;
    }
}