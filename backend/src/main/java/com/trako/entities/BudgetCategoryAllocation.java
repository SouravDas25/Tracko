package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "budget_category_allocations")
@Getter
@Setter
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

    public void setAllocatedAmount(Double allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
        recalculateRemaining();
    }

    public void setActualSpent(Double actualSpent) {
        this.actualSpent = actualSpent;
        recalculateRemaining();
    }

    private void recalculateRemaining() {
        if (this.allocatedAmount != null && this.actualSpent != null) {
            this.remainingBalance = this.allocatedAmount - this.actualSpent;
        }
    }
}
