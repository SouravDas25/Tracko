package com.trako.repositories;

import com.trako.entities.BudgetCategoryAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryAllocationRepository extends JpaRepository<BudgetCategoryAllocation, Long> {
    List<BudgetCategoryAllocation> findByBudgetMonthId(Long budgetMonthId);
    
    Optional<BudgetCategoryAllocation> findByBudgetMonthIdAndCategoryId(Long budgetMonthId, Long categoryId);
    
    List<BudgetCategoryAllocation> findByUserIdAndBudgetMonthId(String userId, Long budgetMonthId);
}
