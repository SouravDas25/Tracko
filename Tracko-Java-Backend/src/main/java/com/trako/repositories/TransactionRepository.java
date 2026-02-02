package com.trako.repositories;

import com.trako.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId) " +
           "ORDER BY t.date DESC")
    List<Transaction> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId) " +
           "AND t.date >= :startDate AND t.date < :endDate " +
           "ORDER BY t.date DESC")
    List<Transaction> findByUserIdAndDateBetween(
        @Param("userId") String userId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
           "AND t.date >= :startDate AND t.date < :endDate " +
           "ORDER BY t.date DESC")
    List<Transaction> findByUserIdAndDateBetweenAndAccountIds(
        @Param("userId") String userId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("accountIds") List<Long> accountIds
    );
    
    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByCategoryId(Long categoryId);
}
