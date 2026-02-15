package com.trako.repositories;

import com.trako.entities.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
           "(SELECT a.id FROM Account a WHERE a.userId = :userId) " +
           "AND t.date >= :startDate AND t.date < :endDate")
    Page<Transaction> findByUserIdAndDateBetween(
        @Param("userId") String userId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        Pageable pageable
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

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
           "AND t.date >= :startDate AND t.date < :endDate")
    Page<Transaction> findByUserIdAndDateBetweenAndAccountIds(
        @Param("userId") String userId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("accountIds") List<Long> accountIds,
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId) " +
           "AND t.categoryId = :categoryId " +
           "AND t.date >= :startDate AND t.date < :endDate " +
           "ORDER BY t.date DESC")
    List<Transaction> findByUserIdAndCategoryIdAndDateBetween(
        @Param("userId") String userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
           "AND t.categoryId = :categoryId " +
           "AND t.date >= :startDate AND t.date < :endDate " +
           "ORDER BY t.date DESC")
    List<Transaction> findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(
        @Param("userId") String userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("accountIds") List<Long> accountIds
    );

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId) " +
           "AND t.categoryId = :categoryId " +
           "AND t.date >= :startDate AND t.date < :endDate")
    Page<Transaction> findByUserIdAndCategoryIdAndDateBetween(
        @Param("userId") String userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN " +
           "(SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
           "AND t.categoryId = :categoryId " +
           "AND t.date >= :startDate AND t.date < :endDate")
    Page<Transaction> findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(
        @Param("userId") String userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("accountIds") List<Long> accountIds,
        Pageable pageable
    );

    @Query("SELECT t.accountId AS accountId, " +
            "SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END) AS balance " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND (t.isCountable = 1 OR (:transferCategoryId IS NOT NULL AND t.categoryId = :transferCategoryId)) " +
            "GROUP BY t.accountId")
    List<Map<String, Object>> findAccountBalancesByUserId(
            @Param("userId") String userId,
            @Param("transferCategoryId") Long transferCategoryId
    );

    @Query("SELECT " +
            "SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END) " +
            "FROM Transaction t " +
            "WHERE t.accountId = :accountId " +
            "AND (t.isCountable = 1 OR (:transferCategoryId IS NOT NULL AND t.categoryId = :transferCategoryId))")
    Double findBalanceByAccountId(
            @Param("accountId") Long accountId,
            @Param("transferCategoryId") Long transferCategoryId
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId = :accountId " +
            "AND t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.linkedTransactionId IS NOT NULL")
    Double sumTransferDeltaForAccount(
            @Param("userId") String userId,
            @Param("accountId") Long accountId
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId = :accountId " +
            "AND t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "AND t.linkedTransactionId IS NOT NULL")
    Double sumTransferDeltaForAccountInRange(
            @Param("userId") String userId,
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId = :accountId " +
            "AND t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND (t.isCountable = 1 OR t.linkedTransactionId IS NOT NULL)")
    Double sumBalanceForAccountFromTransactions(
            @Param("userId") String userId,
            @Param("accountId") Long accountId
    );

    @Query("SELECT t.accountId AS accountId, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END), 0) AS balance " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND (t.isCountable = 1 OR t.linkedTransactionId IS NOT NULL) " +
            "GROUP BY t.accountId")
    List<Map<String, Object>> sumBalancesByAccountForUserFromTransactions(
            @Param("userId") String userId
    );

    @Query("SELECT t.accountId AS accountId, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 2 THEN t.amount " +
            "         WHEN t.transactionType = 1 THEN -t.amount " +
            "         ELSE 0 END), 0) AS transferDelta " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.linkedTransactionId IS NOT NULL " +
            "GROUP BY t.accountId")
    List<Map<String, Object>> sumTransferDeltasByAccountForUser(
            @Param("userId") String userId
    );
    
    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByCategoryId(Long categoryId);
}
