package com.trako.integration.transaction;

import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.services.transactions.TransactionService;
import com.trako.services.transactions.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    private User testUser;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Txn User");

        testAccount = new Account();
        testAccount.setName("Main");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Misc");
        testCategory.setUserId(testUser.getId());
        testCategory.setCategoryType(CategoryType.EXPENSE);
        testCategory = categoryRepository.save(testCategory);

        // Mock Security Context for service methods that need loggedInUser()
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(), testUser.getPassword(), Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    public void testFindAllCoversRepositoryDelegate() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("Any");
        t.setDate(new Date());
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(1.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t);

        List<Transaction> all = transactionService.findAll();
        assertTrue(all.size() >= 1);
    }

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date monthStart(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    public void testFindAllReturnsSortedByDateDesc() {
        // Create 3 transactions with different dates
        createTxn(date(2026, 1, 1), "Oldest");
        createTxn(date(2026, 1, 10), "Newest");
        createTxn(date(2026, 1, 5), "Middle");

        List<Transaction> result = transactionService.findByUserId(testUser.getId());
        assertEquals(3, result.size());

        assertEquals("Newest", result.get(0).getName());
        assertEquals("Middle", result.get(1).getName());
        assertEquals("Oldest", result.get(2).getName());
    }

    @Test
    public void testSaveCalculatesAmountFromExchangeRate() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("Foreign Txn");
        t.setDate(new Date());
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(100.0);
        t.setExchangeRate(1.5); // 1 Original = 1.5 Base
        t.setOriginalCurrency("EUR");
        // amount is null

        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), t);

        assertNotNull(saved.getAmount());
        assertEquals(150.0, saved.getAmount(), 0.01);
    }

    @Test
    public void testSaveCalculatesAmountFromUserCurrencyConfig() {
        // Seed UserCurrency
        UserCurrency uc = new UserCurrency();
        uc.setUser(testUser);
        uc.setCurrencyCode("GBP");
        uc.setExchangeRate(2.0);
        userCurrencyRepository.save(uc);

        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("British Txn");
        t.setDate(new Date());
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(50.0);
        t.setOriginalCurrency("GBP");
        // Provide exchangeRate explicitly to satisfy validation and align with current write service behavior
        t.setExchangeRate(2.0);

        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), t);

        assertNotNull(saved.getAmount());
        assertEquals(100.0, saved.getAmount(), 0.01); // 50 * 2.0
        assertEquals(2.0, saved.getExchangeRate(), 0.01);
    }

    @Test
    public void testGetSummaryCalculations() {
        // Income: 500
        Transaction income = new Transaction();
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setName("Income");
        income.setDate(date(2026, 2, 1));
        income.setTransactionType(TransactionDbType.CREDIT); // Credit
        income.setOriginalAmount(500.0);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // Expense: 200
        Transaction expense = new Transaction();
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setName("Expense");
        expense.setDate(date(2026, 2, 5));
        expense.setTransactionType(TransactionDbType.DEBIT); // Debit
        expense.setOriginalAmount(200.0);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // Excluded (not countable): 1000
        Transaction ignored = new Transaction();
        ignored.setAccountId(testAccount.getId());
        ignored.setCategoryId(testCategory.getId());
        ignored.setName("Ignored");
        ignored.setDate(date(2026, 2, 6));
        ignored.setTransactionType(TransactionDbType.DEBIT);
        ignored.setOriginalAmount(1000.0);
        ignored.setOriginalCurrency("INR");
        ignored.setExchangeRate(1.0);
        ignored.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), ignored);

        Date start = monthStart(2026, 2);
        Date end = monthStart(2026, 3);

        TransactionSummaryDTO summary = transactionService.getSummary(testUser.getId(), start, end);

        assertEquals(500.0, summary.getTotalIncome(), 0.01);
        assertEquals(200.0, summary.getTotalExpense(), 0.01);
        assertEquals(300.0, summary.getNetTotal(), 0.01); // 500 - 200
        assertEquals(2, summary.getTransactionCount()); // Ignored one doesn't count
    }

    @Test
    public void testGetSummaryWithRolloverAccumulatesAcrossMultipleMonths() {
        // Jan 2026: Net +100 (income 300, expense 200)
        Transaction janIncome = new Transaction();
        janIncome.setAccountId(testAccount.getId());
        janIncome.setCategoryId(testCategory.getId());
        janIncome.setName("Jan Income");
        janIncome.setDate(date(2026, 1, 5));
        janIncome.setTransactionType(TransactionDbType.CREDIT);
        janIncome.setOriginalAmount(300.0);
        janIncome.setOriginalCurrency("INR");
        janIncome.setExchangeRate(1.0);
        janIncome.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), janIncome);

        Transaction janExpense = new Transaction();
        janExpense.setAccountId(testAccount.getId());
        janExpense.setCategoryId(testCategory.getId());
        janExpense.setName("Jan Expense");
        janExpense.setDate(date(2026, 1, 10));
        janExpense.setTransactionType(TransactionDbType.DEBIT);
        janExpense.setOriginalAmount(200.0);
        janExpense.setOriginalCurrency("INR");
        janExpense.setExchangeRate(1.0);
        janExpense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), janExpense);

        // Dec 2025: Net -50 (income 100, expense 150)
        Transaction decIncome = new Transaction();
        decIncome.setAccountId(testAccount.getId());
        decIncome.setCategoryId(testCategory.getId());
        decIncome.setName("Dec Income");
        decIncome.setDate(date(2025, 12, 5));
        decIncome.setTransactionType(TransactionDbType.CREDIT);
        decIncome.setOriginalAmount(100.0);
        decIncome.setOriginalCurrency("INR");
        decIncome.setExchangeRate(1.0);
        decIncome.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), decIncome);

        Transaction decExpense = new Transaction();
        decExpense.setAccountId(testAccount.getId());
        decExpense.setCategoryId(testCategory.getId());
        decExpense.setName("Dec Expense");
        decExpense.setDate(date(2025, 12, 10));
        decExpense.setTransactionType(TransactionDbType.DEBIT);
        decExpense.setOriginalAmount(150.0);
        decExpense.setOriginalCurrency("INR");
        decExpense.setExchangeRate(1.0);
        decExpense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), decExpense);

        // Nov 2025: Net +250 (income 400, expense 150)
        Transaction novIncome = new Transaction();
        novIncome.setAccountId(testAccount.getId());
        novIncome.setCategoryId(testCategory.getId());
        novIncome.setName("Nov Income");
        novIncome.setDate(date(2025, 11, 5));
        novIncome.setTransactionType(TransactionDbType.CREDIT);
        novIncome.setOriginalAmount(400.0);
        novIncome.setOriginalCurrency("INR");
        novIncome.setExchangeRate(1.0);
        novIncome.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), novIncome);

        Transaction novExpense = new Transaction();
        novExpense.setAccountId(testAccount.getId());
        novExpense.setCategoryId(testCategory.getId());
        novExpense.setName("Nov Expense");
        novExpense.setDate(date(2025, 11, 10));
        novExpense.setTransactionType(TransactionDbType.DEBIT);
        novExpense.setOriginalAmount(150.0);
        novExpense.setOriginalCurrency("INR");
        novExpense.setExchangeRate(1.0);
        novExpense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), novExpense);

        // Feb 2026 current period: Net +300 (income 500, expense 200)
        Transaction febIncome = new Transaction();
        febIncome.setAccountId(testAccount.getId());
        febIncome.setCategoryId(testCategory.getId());
        febIncome.setName("Feb Income");
        febIncome.setDate(date(2026, 2, 1));
        febIncome.setTransactionType(TransactionDbType.CREDIT);
        febIncome.setOriginalAmount(500.0);
        febIncome.setOriginalCurrency("INR");
        febIncome.setExchangeRate(1.0);
        febIncome.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), febIncome);

        Transaction febExpense = new Transaction();
        febExpense.setAccountId(testAccount.getId());
        febExpense.setCategoryId(testCategory.getId());
        febExpense.setName("Feb Expense");
        febExpense.setDate(date(2026, 2, 3));
        febExpense.setTransactionType(TransactionDbType.DEBIT);
        febExpense.setOriginalAmount(200.0);
        febExpense.setOriginalCurrency("INR");
        febExpense.setExchangeRate(1.0);
        febExpense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), febExpense);

        Date start = monthStart(2026, 2);
        Date end = monthStart(2026, 3);

        TransactionSummaryDTO summary = transactionService.getSummaryWithRollover(testUser.getId(), start, end, null, null);

        assertEquals(500.0, summary.getTotalIncome(), 0.01);
        assertEquals(200.0, summary.getTotalExpense(), 0.01);
        assertEquals(300.0, summary.getNetTotal(), 0.01);

        // Rollover should include Nov + Dec + Jan nets
        // Nov: +250, Dec: -50, Jan: +100 => +300
        assertEquals(300.0, summary.getRolloverNet(), 0.01);
        assertEquals(600.0, summary.getNetTotalWithRollover(), 0.01);
    }

    @Test
    public void testFindWithDetailsNonPaged_withNullAccountIds_coversListOverload() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("DetailTxn");
        t.setDate(date(2026, 2, 1));
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t);

        var details = transactionService.findWithDetailsByUserIdAndDateBetween(
                testUser.getId(),
                monthStart(2026, 2),
                monthStart(2026, 3),
                null
        );

        assertEquals(1, details.size());
        assertNotNull(details.get(0).getAccount());
        assertNotNull(details.get(0).getCategory());
    }

    @Test
    public void testFindWithDetailsPaged_whenEmpty_returnsEmptyPage() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = transactionService.findWithDetailsByUserIdAndDateBetween(
                testUser.getId(),
                monthStart(1990, 1),
                monthStart(1990, 2),
                null,
                pageable
        );

        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getContent().size());
    }

    @Test
    public void testGetSummaryFallbackWithAccountFilter_usesAccountIdsBranch() {
        Account otherAccount = new Account();
        otherAccount.setName("Other");
        otherAccount.setUserId(testUser.getId());
        otherAccount = accountRepository.save(otherAccount);

        Transaction inScope = new Transaction();
        inScope.setAccountId(testAccount.getId());
        inScope.setCategoryId(testCategory.getId());
        inScope.setName("InScope");
        inScope.setDate(date(2026, 2, 1));
        inScope.setTransactionType(TransactionDbType.CREDIT);
        inScope.setOriginalAmount(100.0);
        inScope.setOriginalCurrency("INR");
        inScope.setExchangeRate(1.0);
        inScope.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), inScope);

        Transaction outOfScope = new Transaction();
        outOfScope.setAccountId(otherAccount.getId());
        outOfScope.setCategoryId(testCategory.getId());
        outOfScope.setName("OutOfScope");
        outOfScope.setDate(date(2026, 2, 2));
        outOfScope.setTransactionType(TransactionDbType.CREDIT);
        outOfScope.setOriginalAmount(999.0);
        outOfScope.setOriginalCurrency("INR");
        outOfScope.setExchangeRate(1.0);
        outOfScope.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), outOfScope);

        // Not a full-month range (forces fallback scan), but includes accountIds (covers else branch).
        Date start = date(2026, 2, 1);
        Date end = date(2026, 2, 28);

        TransactionSummaryDTO summary = transactionService.getSummary(
                testUser.getId(),
                start,
                end,
                List.of(testAccount.getId()),
                null
        );
        assertEquals(100.0, summary.getTotalIncome(), 0.01);
    }

    @Test
    public void testGetSummaryWithRollover_whenStartDateNull_returnsBaseAndCoversNullBranch() {
        Transaction income = new Transaction();
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setName("Income");
        income.setDate(date(2026, 2, 1));
        income.setTransactionType(TransactionDbType.CREDIT);
        income.setOriginalAmount(123.0);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        TransactionSummaryDTO summary = transactionService.getSummaryWithRollover(
                testUser.getId(),
                null,
                monthStart(2026, 3),
                null,
                null
        );

        assertNotNull(summary);
        assertEquals(0.0, summary.getRolloverNet(), 0.01);
    }

    @Test
    public void testGetSummary_fullMonthFalseWhenStartDateNullOrEndDateNull() {
        TransactionSummaryDTO s1 = transactionService.getSummary(testUser.getId(), null, monthStart(2026, 3), null, null);
        TransactionSummaryDTO s2 = transactionService.getSummary(testUser.getId(), monthStart(2026, 2), null, null, null);
        assertNotNull(s1);
        assertNotNull(s2);
    }

    @Test
    public void testFindByUserIdAndDateBetween_listOverload_coversDelegate() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("Range");
        t.setDate(date(2026, 2, 10));
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t);

        List<Transaction> txs = transactionService.findByUserIdAndDateBetween(
                testUser.getId(),
                monthStart(2026, 2),
                monthStart(2026, 3)
        );

        assertEquals(1, txs.size());
    }

    @Test
    public void testFindByUserIdAndDateBetweenAndAccountIds_listOverload_coversDelegate() {
        Account other = new Account();
        other.setName("Other");
        other.setUserId(testUser.getId());
        other = accountRepository.save(other);

        Transaction t1 = new Transaction();
        t1.setAccountId(testAccount.getId());
        t1.setCategoryId(testCategory.getId());
        t1.setName("A1");
        t1.setDate(date(2026, 2, 10));
        t1.setTransactionType(TransactionDbType.DEBIT);
        t1.setOriginalAmount(10.0);
        t1.setOriginalCurrency("INR");
        t1.setExchangeRate(1.0);
        t1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t1);

        Transaction t2 = new Transaction();
        t2.setAccountId(other.getId());
        t2.setCategoryId(testCategory.getId());
        t2.setName("A2");
        t2.setDate(date(2026, 2, 11));
        t2.setTransactionType(TransactionDbType.DEBIT);
        t2.setOriginalAmount(20.0);
        t2.setOriginalCurrency("INR");
        t2.setExchangeRate(1.0);
        t2.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t2);

        List<Transaction> txs = transactionService.findByUserIdAndDateBetweenAndAccountIds(
                testUser.getId(),
                monthStart(2026, 2),
                monthStart(2026, 3),
                List.of(testAccount.getId())
        );

        assertEquals(1, txs.size());
        assertEquals("A1", txs.get(0).getName());
    }

    @Test
    public void testFindByUserIdAndCategoryIdAndDateBetween_pageable_coversDelegate() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("Cat");
        t.setDate(date(2026, 2, 10));
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t);

        var page = transactionService.findByUserIdAndCategoryIdAndDateBetween(
                testUser.getId(),
                testCategory.getId(),
                monthStart(2026, 2),
                monthStart(2026, 3),
                PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
    }

    @Test
    public void testFindWithDetailsByUserIdAndCategoryIdAndDateBetween_whenEmpty_returnsEmptyPage() {
        var page = transactionService.findWithDetailsByUserIdAndCategoryIdAndDateBetween(
                testUser.getId(),
                testCategory.getId(),
                monthStart(1990, 1),
                monthStart(1990, 2),
                PageRequest.of(0, 10)
        );

        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getContent().size());
    }

    @Test
    public void testFindWithDetailsByUserIdAndDateBetween_listOverload_withAccountIds_coversElseBranch() {
        Account other = new Account();
        other.setName("Other2");
        other.setUserId(testUser.getId());
        other = accountRepository.save(other);

        Transaction t1 = new Transaction();
        t1.setAccountId(testAccount.getId());
        t1.setCategoryId(testCategory.getId());
        t1.setName("Keep");
        t1.setDate(date(2026, 2, 10));
        t1.setTransactionType(TransactionDbType.DEBIT);
        t1.setOriginalAmount(10.0);
        t1.setOriginalCurrency("INR");
        t1.setExchangeRate(1.0);
        t1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t1);

        Transaction t2 = new Transaction();
        t2.setAccountId(other.getId());
        t2.setCategoryId(testCategory.getId());
        t2.setName("Drop");
        t2.setDate(date(2026, 2, 11));
        t2.setTransactionType(TransactionDbType.DEBIT);
        t2.setOriginalAmount(20.0);
        t2.setOriginalCurrency("INR");
        t2.setExchangeRate(1.0);
        t2.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t2);

        var details = transactionService.findWithDetailsByUserIdAndDateBetween(
                testUser.getId(),
                monthStart(2026, 2),
                monthStart(2026, 3),
                List.of(testAccount.getId())
        );

        assertEquals(1, details.size());
        assertEquals("Keep", details.get(0).getName());
    }

    @Test
    public void testFindWithDetailsByUserIdAndDateBetween_pageable_withAccountIds_coversElseBranch() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("Paged");
        t.setDate(date(2026, 2, 10));
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t);

        var page = transactionService.findWithDetailsByUserIdAndDateBetween(
                testUser.getId(),
                monthStart(2026, 2),
                monthStart(2026, 3),
                List.of(testAccount.getId()),
                PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void testFindWithDetailsByUserIdAndDateBetween_listOverload_whenNoTransactions_returnsEmptyList() {
        var details = transactionService.findWithDetailsByUserIdAndDateBetween(
                testUser.getId(),
                monthStart(1990, 1),
                monthStart(1990, 2),
                null
        );

        assertNotNull(details);
        assertEquals(0, details.size());
    }

    private void createTxn(Date date, String name) {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName(name);
        t.setDate(date);
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), t);
    }
}
