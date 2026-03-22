package com.trako.repositories;

import com.trako.dtos.DateAmountRow;
import com.trako.dtos.GroupedDateAmountRow;
import com.trako.dtos.NamedDateAmountRow;
import com.trako.entities.Transaction;
import com.trako.enums.TransactionDbType;
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

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
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

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
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
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0) AS balance " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND (t.isCountable = 1 OR t.linkedTransactionId IS NOT NULL) " +
            "GROUP BY t.accountId")
    List<Map<String, Object>> sumBalancesByAccountForUserFromTransactions(
            @Param("userId") String userId
    );

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "AND t.date >= :startDate AND t.date < :endDate")
    Object[] sumCountableTotalsForUserInRange(
            @Param("userId") String userId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "AND t.date >= :startDate AND t.date < :endDate")
    Object[] sumCountableTotalsForUserInRangeAndAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "AND t.date < :startDate")
    Double sumNetBeforeDateForUser(
            @Param("userId") String userId,
            @Param("startDate") Date startDate,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "AND t.date < :startDate")
    Double sumNetBeforeDateForUserAndAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") Date startDate,
            @Param("categoryId") Long categoryId
    );

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdIn(List<Long> accountIds);

    List<Transaction> findByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);

    void deleteByAccountIdIn(List<Long> accountIds);

    @Query("SELECT YEAR(t.date) as y, MONTH(t.date) as m, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND YEAR(t.date) = :year " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "GROUP BY YEAR(t.date), MONTH(t.date) " +
            "ORDER BY y DESC, m DESC")
    List<Object[]> findMonthlySummariesForUserAndYear(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT YEAR(t.date) as y, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "GROUP BY YEAR(t.date) " +
            "ORDER BY y DESC")
    List<Object[]> findYearlySummariesForUser(
            @Param("userId") String userId,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT YEAR(t.date) as y, MONTH(t.date) as m, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND YEAR(t.date) = :year " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "GROUP BY YEAR(t.date), MONTH(t.date) " +
            "ORDER BY y DESC, m DESC")
    List<Object[]> findMonthlySummariesForUserAndYearAndAccounts(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("accountIds") List<Long> accountIds,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT YEAR(t.date) as y, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = com.trako.enums.TransactionDbType.CREDIT THEN t.amount " +
            "         WHEN t.transactionType = com.trako.enums.TransactionDbType.DEBIT THEN -t.amount " +
            "         ELSE 0 END), 0), " +
            "COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId AND a.id IN :accountIds) " +
            "AND t.isCountable = 1 " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "GROUP BY YEAR(t.date) " +
            "ORDER BY y DESC")
    List<Object[]> findYearlySummariesForUserAndAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("categoryId") Long categoryId
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
            @Param("transactionType") TransactionDbType transactionType,
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
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId
    );

    // ── Unified query: optional categoryId ──

    @Query("SELECT t.date, SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "GROUP BY t.date " +
            "ORDER BY t.date ASC")
    List<Object[]> sumAmountsByDate(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId
    );

    // ── Grouped queries for analytics (single query, no N+1) ──

    @Query("SELECT t.categoryId, t.date, SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.categoryId, t.date " +
            "ORDER BY t.categoryId, t.date ASC")
    List<Object[]> sumAmountsByDateGroupedByCategory(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT t.accountId, t.date, SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:accountId IS NULL OR t.accountId = :accountId) " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.accountId, t.date " +
            "ORDER BY t.accountId, t.date ASC")
    List<Object[]> sumAmountsByDateGroupedByAccount(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
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
            @Param("transactionType") TransactionDbType transactionType,
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
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );


    // ── Analytics queries: multi-account and multi-category filtering ──

    @Query("SELECT new com.trako.dtos.DateAmountRow(t.date, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:#{#accountIds == null || #accountIds.isEmpty()} = true OR t.accountId IN :accountIds) " +
            "AND (:#{#categoryIds == null || #categoryIds.isEmpty()} = true OR t.categoryId IN :categoryIds) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.date " +
            "ORDER BY t.date ASC")
    List<DateAmountRow> sumAmountsByDateFiltered(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountIds") List<Long> accountIds,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT new com.trako.dtos.GroupedDateAmountRow(t.categoryId, t.date, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:#{#accountIds == null || #accountIds.isEmpty()} = true OR t.accountId IN :accountIds) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.categoryId, t.date " +
            "ORDER BY t.categoryId, t.date ASC")
    List<GroupedDateAmountRow> sumAmountsByDateGroupedByCategoryFiltered(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT new com.trako.dtos.GroupedDateAmountRow(t.accountId, t.date, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:#{#categoryIds == null || #categoryIds.isEmpty()} = true OR t.categoryId IN :categoryIds) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.accountId, t.date " +
            "ORDER BY t.accountId, t.date ASC")
    List<GroupedDateAmountRow> sumAmountsByDateGroupedByAccountFiltered(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT new com.trako.dtos.NamedDateAmountRow(t.name, t.date, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId) " +
            "AND t.isCountable = 1 " +
            "AND t.transactionType = :transactionType " +
            "AND (:#{#accountIds == null || #accountIds.isEmpty()} = true OR t.accountId IN :accountIds) " +
            "AND (:#{#categoryIds == null || #categoryIds.isEmpty()} = true OR t.categoryId IN :categoryIds) " +
            "AND t.date >= :startDate AND t.date < :endDate " +
            "GROUP BY t.name, t.date " +
            "ORDER BY t.name, t.date ASC")
    List<NamedDateAmountRow> sumAmountsByDateGroupedByNameFiltered(
            @Param("userId") String userId,
            @Param("transactionType") TransactionDbType transactionType,
            @Param("accountIds") List<Long> accountIds,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.accountId IN (SELECT a.id FROM Account a WHERE a.userId = :userId)")
    void deleteByUserId(@Param("userId") String userId);
}
