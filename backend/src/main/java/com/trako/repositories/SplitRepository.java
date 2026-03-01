package com.trako.repositories;

import com.trako.entities.Split;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SplitRepository extends JpaRepository<Split, Long>, JpaSpecificationExecutor<Split> {

    List<Split> findByTransactionId(Long transactionId);

    List<Split> findByTransactionIdIn(List<Long> transactionIds);
    
    List<Split> findByUserId(String userId);
    
    List<Split> findByUserIdAndIsSettled(String userId, Integer isSettled);

    List<Split> findByContactId(Long contactId);

    List<Split> findByContactIdAndIsSettled(Long contactId, Integer isSettled);

    void deleteByUserId(String userId);
    void deleteByTransactionIdIn(List<Long> transactionIds);

    boolean existsByContactId(Long contactId);
    
    @Modifying
    @Query("UPDATE Split s SET s.isSettled = 1, s.settledAt = CURRENT_TIMESTAMP WHERE s.id = ?1")
    void settleSplit(Long splitId);

    @Modifying
    @Query("UPDATE Split s SET s.isSettled = 0, s.settledAt = NULL WHERE s.id = ?1")
    void unsettleSplit(Long splitId);

}
