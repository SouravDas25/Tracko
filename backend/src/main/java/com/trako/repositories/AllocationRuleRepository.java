package com.trako.repositories;

import com.trako.entities.AllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationRuleRepository extends JpaRepository<AllocationRule, Long> {
    List<AllocationRule> findByUserId(String userId);

    List<AllocationRule> findByUserIdAndIsActiveTrue(String userId);

    @Modifying
    @Query("DELETE FROM AllocationRule a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
