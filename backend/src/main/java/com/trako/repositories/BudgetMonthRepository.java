package com.trako.repositories;

import com.trako.entities.BudgetMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetMonthRepository extends JpaRepository<BudgetMonth, Long> {
    Optional<BudgetMonth> findByUserIdAndMonthAndYear(String userId, Integer month, Integer year);
    
    // Find the latest closed budget month for a user (for rollover calculation)
    Optional<BudgetMonth> findFirstByUserIdAndIsClosedTrueOrderByYearDescMonthDesc(String userId);
    
    void deleteByUserId(String userId);
}
