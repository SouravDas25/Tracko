package com.trako.dtos;

public class BudgetCategoryDTO {
    private Long categoryId;
    private String categoryName;
    private Double allocatedAmount;
    private Double actualSpent;
    private Double remainingBalance;
    private Boolean isRollOverEnabled;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(Double allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public Double getActualSpent() {
        return actualSpent;
    }

    public void setActualSpent(Double actualSpent) {
        this.actualSpent = actualSpent;
    }

    public Double getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(Double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public Boolean getIsRollOverEnabled() {
        return isRollOverEnabled;
    }

    public void setIsRollOverEnabled(Boolean rollOverEnabled) {
        isRollOverEnabled = rollOverEnabled;
    }
}
