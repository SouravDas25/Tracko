package com.trako.repositories;

import com.trako.entities.Transaction;
import com.trako.entities.TransactionEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
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

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
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
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0) AS balance " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND (t.isCountable = 1 OR t.linkedTransactionId IS NOT NULL) " +
            "GROUP BY t.accountId")
    List<Map<String, Object>> sumBalancesByAccountForUserFromTransactions(
            @Param("userId") String userId
    );

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.date >= :startDate AND t.date < :endDate")
    Object[] sumCountableTotalsForUserInRange(
            @Param("userId") String userId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND t.date >= :startDate AND t.date < :endDate")
    Object[] sumCountableTotalsForUserInRangeAndAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.date < :startDate")
    Double sumNetBeforeDateForUser(
            @Param("userId") String userId,
            @Param("startDate") Date startDate
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND t.date < :startDate")
    Double sumNetBeforeDateForUserAndAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") Date startDate
    );

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdIn(List<Long> accountIds);

    List<Transaction> findByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);

    void deleteByAccountIdIn(List<Long> accountIds);

    @Query("SELECT YEAR(t.date) as y, MONTH(t.date) as m, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND YEAR(t.date) = :year " +
            "GROUP BY YEAR(t.date), MONTH(t.date) " +
            "ORDER BY y DESC, m DESC")
    List<Object[]> findMonthlySummariesForUserAndYear(
            @Param("userId") String userId,
            @Param("year") int year
    );

    @Query("SELECT YEAR(t.date) as y, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "GROUP BY YEAR(t.date) " +
            "ORDER BY y DESC")
    List<Object[]> findYearlySummariesForUser(
            @Param("userId") String userId
    );

    @Query("SELECT YEAR(t.date) as y, MONTH(t.date) as m, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND YEAR(t.date) = :year " +
            "GROUP BY YEAR(t.date), MONTH(t.date) " +
            "ORDER BY y DESC, m DESC")
    List<Object[]> findMonthlySummariesForUserAndYearAndAccounts(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("accountIds") List<Long> accountIds
    );

    @Query("SELECT YEAR(t.date) as y, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.entities.TransactionEntryType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.entities.TransactionEntryType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "GROUP BY YEAR(t.date) " +
            "ORDER BY y DESC")
    List<Object[]> findYearlySummariesForUserAndAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds
    );

    @Query("SELECT t.date, SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "GROUP BY t.date " +
            "ORDER BY t.date ASC")
    List<Object[]> sumAmountsByDateForUser(
            @Param("userId") String userId,
            @Param("transactionType") TransactionEntryType transactionType,
            @Param("accountId") Long accountId
    );

    @Query("SELECT t.date, SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "AND t.categoryId = :categoryId " +
            "GROUP BY t.date " +
            "ORDER BY t.date ASC")
    List<Object[]> sumAmountsByDateForCategory(
            @Param("userId") String userId,
            @Param("transactionType") TransactionEntryType transactionType,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT t.categoryId, SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.categoryId")
    List<Object[]> sumAmountsByCategoryForUserInRange(
            @Param("userId") String userId,
            @Param("transactionType") TransactionEntryType transactionType,
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "AND t.categoryId = :categoryId " +
            "AND t.date >= :startDate AND t.date < :endDate")
    Double sumAmountForCategoryInRange(
            @Param("userId") String userId,
            @Param("transactionType") TransactionEntryType transactionType,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId)")
    void deleteByUserId(@Param("userId") String userId);
}
