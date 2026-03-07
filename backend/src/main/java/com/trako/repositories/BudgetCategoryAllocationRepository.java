package com.trako.repositories;

import com.trako.entities.BudgetCategoryAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryAllocationRepository extends JpaRepository<BudgetCategoryAllocation, Long> {
    List<BudgetCategoryAllocation> findByBudgetMonthId(Long budgetMonthId);

    Optional<BudgetCategoryAllocation> findByBudgetMonthIdAndCategoryId(Long budgetMonthId, Long categoryId);

    List<BudgetCategoryAllocation> findByUserIdAndBudgetMonthId(String userId, Long budgetMonthId);

    void deleteByUserId(String userId);

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE BudgetCategoryAllocation b SET b.actualSpent = 0.0, b.remainingBalance = b.allocatedAmount WHERE b.userId = :userId")
    void resetActualSpentByUserId(@Param("userId") String userId);
}
