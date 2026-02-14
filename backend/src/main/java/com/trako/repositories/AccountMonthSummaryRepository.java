package com.trako.repositories;

import com.trako.entities.AccountMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountMonthSummaryRepository extends JpaRepository<AccountMonthlySummary, Long> {

    Optional<AccountMonthlySummary> findByUserIdAndAccountIdAndYearAndMonth(String userId, Long accountId, Integer year, Integer month);

    @Query("SELECT COALESCE(SUM(s.netTotal), 0) FROM AccountMonthlySummary s " +
            "WHERE s.userId = :userId " +
            "AND (s.year < :year OR (s.year = :year AND s.month < :month))")
    Double sumNetBeforeMonth(
            @Param("userId") String userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query("SELECT COALESCE(SUM(s.netTotal), 0) FROM AccountMonthlySummary s " +
            "WHERE s.userId = :userId " +
            "AND s.accountId IN :accountIds " +
            "AND (s.year < :year OR (s.year = :year AND s.month < :month))")
    Double sumNetBeforeMonthForAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query("SELECT COALESCE(SUM(s.incomeTotal), 0), COALESCE(SUM(s.expenseTotal), 0), COALESCE(SUM(s.netTotal), 0), COALESCE(SUM(s.countCountable), 0) " +
            "FROM AccountMonthlySummary s " +
            "WHERE s.userId = :userId AND s.year = :year AND s.month = :month")
    Object[] sumMonthTotals(
            @Param("userId") String userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query("SELECT COALESCE(SUM(s.incomeTotal), 0), COALESCE(SUM(s.expenseTotal), 0), COALESCE(SUM(s.netTotal), 0), COALESCE(SUM(s.countCountable), 0) " +
            "FROM AccountMonthlySummary s " +
            "WHERE s.userId = :userId AND s.accountId IN :accountIds AND s.year = :year AND s.month = :month")
    Object[] sumMonthTotalsForAccounts(
            @Param("userId") String userId,
            @Param("accountIds") List<Long> accountIds,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
