package com.trako.repositories;

import com.trako.entities.Split;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SplitRepository extends JpaRepository<Split, String>, JpaSpecificationExecutor<Split> {

    List<Split> findBySourceUserId(String sourceUserId);

    @Query("SELECT s from Split s WHERE s.dueUserId = ?1 AND s.sourceUserId = ?2 ORDER BY s.created_at")
    List<Split> findByDueUserIdAndSourceUserId(String dueUserId, String sourceUserId);

    @Query("SELECT DISTINCT dueUserId FROM Split WHERE sourceUserId = ?1")
    List<String> findAllDueUserId(String sourceUserId);

    @Modifying
    @Query("UPDATE Split SET settledAmount = ?2 where id = ?1 ")
    void settleSplit(String splitId, Double settleAmount);

}
