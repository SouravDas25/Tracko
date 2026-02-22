package com.trako.repositories;

import com.trako.entities.AllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationRuleRepository extends JpaRepository<AllocationRule, Long> {
    List<AllocationRule> findByUserId(String userId);
    List<AllocationRule> findByUserIdAndIsActiveTrue(String userId);
    void deleteByUserId(String userId);
}
