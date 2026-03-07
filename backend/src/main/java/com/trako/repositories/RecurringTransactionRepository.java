package com.trako.repositories;

import com.trako.entities.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserId(String userId);

    List<RecurringTransaction> findByNextRunDateBeforeAndIsActiveTrue(Date date);

    boolean existsByAccountId(Long accountId);

    boolean existsByToAccountId(Long toAccountId);

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("DELETE FROM RecurringTransaction rt WHERE rt.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
