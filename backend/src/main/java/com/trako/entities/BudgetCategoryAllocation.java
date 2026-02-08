package com.trako.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "budget_category_allocations")
public class BudgetCategoryAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "budget_month_id")
    private Long budgetMonthId;

    @NotNull
    @Column(name = "category_id")
    private Long categoryId;

    @NotNull
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "allocated_amount")
    private Double allocatedAmount = 0.0;

    @Column(name = "actual_spent")
    private Double actualSpent = 0.0;

    @Column(name = "remaining_balance")
    private Double remainingBalance = 0.0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_month_id", insertable = false, updatable = false)
    private BudgetMonth budgetMonth;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBudgetMonthId() {
        return budgetMonthId;
    }

    public void setBudgetMonthId(Long budgetMonthId) {
        this.budgetMonthId = budgetMonthId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(Double allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
        recalculateRemaining();
    }

    public Double getActualSpent() {
        return actualSpent;
    }

    public void setActualSpent(Double actualSpent) {
        this.actualSpent = actualSpent;
        recalculateRemaining();
    }

    public Double getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(Double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public BudgetMonth getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(BudgetMonth budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    private void recalculateRemaining() {
        if (this.allocatedAmount != null && this.actualSpent != null) {
            this.remainingBalance = this.allocatedAmount - this.actualSpent;
        }
    }
}
