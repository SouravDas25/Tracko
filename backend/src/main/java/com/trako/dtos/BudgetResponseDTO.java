package com.trako.dtos;

import java.util.List;

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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(Double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Double getRolloverAmount() {
        return rolloverAmount;
    }

    public void setRolloverAmount(Double rolloverAmount) {
        this.rolloverAmount = rolloverAmount;
    }

    public Double getAvailableToAssign() {
        return availableToAssign;
    }

    public void setAvailableToAssign(Double availableToAssign) {
        this.availableToAssign = availableToAssign;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean closed) {
        isClosed = closed;
    }

    public List<BudgetCategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<BudgetCategoryDTO> categories) {
        this.categories = categories;
    }
}
