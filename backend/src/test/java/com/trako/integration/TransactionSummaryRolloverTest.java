package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.services.BudgetCalculationService;
import com.trako.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionSummaryRolloverTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetCalculationService budgetCalculationService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetMonthRepository budgetMonthRepository;
    
    @Autowired
    private BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    private User testUser;
    private Account accountA;
    private Account accountB;
    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    public void setup() {
        // Cleanup
        budgetCategoryAllocationRepository.deleteAll();
        budgetMonthRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        // User
        testUser = new User();
        testUser.setName("Summary Test User");
        testUser.setPhoneNo("9998887777");
        testUser.setEmail("summary@test.com");
        testUser.setFireBaseId("summary_pass");
        testUser = usersRepository.save(testUser);

        // Accounts
        accountA = new Account();
        accountA.setName("Bank A");
        accountA.setUserId(testUser.getId());
        accountA = accountRepository.save(accountA);

        accountB = new Account();
        accountB.setName("Bank B");
        accountB.setUserId(testUser.getId());
        accountB = accountRepository.save(accountB);

        // Categories
        incomeCategory = new Category();
        incomeCategory.setName("Income");
        incomeCategory.setUserId(testUser.getId());
        incomeCategory.setCategoryType(CategoryType.INCOME);
        incomeCategory = categoryRepository.save(incomeCategory);

        expenseCategory = new Category();
        expenseCategory.setName("Expense");
        expenseCategory.setUserId(testUser.getId());
        expenseCategory.setCategoryType(CategoryType.EXPENSE);
        expenseCategory.setIsRollOverEnabled(true);
        expenseCategory = categoryRepository.save(expenseCategory);
    }

    private Date getDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date getMonthStart(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date getMonthEnd(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, 1, 0, 0, 0);
        c.add(Calendar.MONTH, 1); // Start of next month
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    public void testPreviousMonthSummaryAcrossAllAccounts() {
        // Scenario:
        // Previous Month (Jan 2025):
        // - Account A: Income 1000
        // - Account B: Income 500
        // - Account A: Expense 200
        // - Account B: Expense 100
        // Total Income: 1500, Total Expense: 300, Net: 1200

        int prevYear = 2025;
        int prevMonth = 1;

        // 1. Create Transactions
        createTransaction(accountA, incomeCategory, 1000.0, 2, getDate(prevYear, prevMonth, 5));
        createTransaction(accountB, incomeCategory, 500.0, 2, getDate(prevYear, prevMonth, 10));
        createTransaction(accountA, expenseCategory, 200.0, 1, getDate(prevYear, prevMonth, 15));
        createTransaction(accountB, expenseCategory, 100.0, 1, getDate(prevYear, prevMonth, 20));

        // 2. Verify Summary for Previous Month (All Accounts)
        Date start = getMonthStart(prevYear, prevMonth);
        Date end = getMonthEnd(prevYear, prevMonth);
        
        // Pass null for accountIds to get summary for all accounts
        TransactionSummaryDTO summary = transactionService.getSummary(testUser.getId(), start, end, null);

        assertEquals(1500.0, summary.getTotalIncome(), 0.001, "Total Income should include both accounts");
        assertEquals(300.0, summary.getTotalExpense(), 0.001, "Total Expense should include both accounts");
        assertEquals(1200.0, summary.getNetTotal(), 0.001, "Net Total should be correct");
        assertEquals(4, summary.getTransactionCount(), "Should count all transactions");
    }

    @Test
    public void testBudgetRolloverUsesPreviousMonthSummaryCorrectly() {
        // Scenario:
        // Previous Month (Jan 2025):
        // - Income across accounts: 1500
        // - Allocated Budget: 1000
        // - Expected Unallocated Rollover: 1500 - 1000 = 500
        
        int prevYear = 2025;
        int prevMonth = 1;
        int currYear = 2025;
        int currMonth = 2;

        // 1. Create Transactions in Previous Month
        createTransaction(accountA, incomeCategory, 1000.0, 2, getDate(prevYear, prevMonth, 5));
        createTransaction(accountB, incomeCategory, 500.0, 2, getDate(prevYear, prevMonth, 10));
        // Consuming the budget so category rollover is 0, ensuring we test unallocated rollover (Income based)
        createTransaction(accountA, expenseCategory, 1000.0, 1, getDate(prevYear, prevMonth, 15));

        // 2. Allocate Budget in Previous Month (triggers BudgetMonth creation)
        BudgetAllocationRequestDTO allocReq = new BudgetAllocationRequestDTO();
        allocReq.setCategoryId(expenseCategory.getId());
        allocReq.setAmount(1000.0);
        allocReq.setMonth(prevMonth);
        allocReq.setYear(prevYear);
        budgetCalculationService.allocateFunds(testUser.getId(), allocReq);

        // 3. Verify Budget Details for Previous Month
        // Total Income should be 1500 (from summary of all accounts)
        // Total Budget should be 1000
        TransactionSummaryDTO prevSummary = transactionService.getSummary(testUser.getId(), 
                getMonthStart(prevYear, prevMonth), 
                getMonthEnd(prevYear, prevMonth), 
                null);
        assertEquals(1500.0, prevSummary.getTotalIncome(), 0.001);

        // 4. Calculate Available to Assign for CURRENT Month (Feb 2025)
        // Should include rollover from Jan 2025
        // Rollover = Jan Income (1500) - Jan Allocated (1000) = 500
        // Current Income = 0
        // Available = 0 + 500 = 500
        
        Double available = budgetCalculationService.calculateAvailableToAssign(testUser.getId(), currMonth, currYear);
        
        assertEquals(500.0, available, 0.001, "Available to assign should include unallocated funds from previous month");
    }

    @Test
    public void testBudgetRolloverAccumulatesUnusedFundsAcrossMultipleMonths() {
        int yearA = 2024;
        int monthA = 11;
        int yearB = 2024;
        int monthB = 12;
        int yearC = 2025;
        int monthC = 1;
        int currYear = 2025;
        int currMonth = 2;

        createTransaction(accountA, incomeCategory, 1000.0, 2, getDate(yearA, monthA, 5));
        createTransaction(accountA, expenseCategory, 400.0, 1, getDate(yearA, monthA, 10));
        BudgetAllocationRequestDTO allocA = new BudgetAllocationRequestDTO();
        allocA.setCategoryId(expenseCategory.getId());
        allocA.setAmount(400.0);
        allocA.setMonth(monthA);
        allocA.setYear(yearA);
        budgetCalculationService.allocateFunds(testUser.getId(), allocA);

        createTransaction(accountA, incomeCategory, 500.0, 2, getDate(yearB, monthB, 5));
        createTransaction(accountA, expenseCategory, 100.0, 1, getDate(yearB, monthB, 10));
        BudgetAllocationRequestDTO allocB = new BudgetAllocationRequestDTO();
        allocB.setCategoryId(expenseCategory.getId());
        allocB.setAmount(100.0);
        allocB.setMonth(monthB);
        allocB.setYear(yearB);
        budgetCalculationService.allocateFunds(testUser.getId(), allocB);

        createTransaction(accountA, incomeCategory, 800.0, 2, getDate(yearC, monthC, 5));
        createTransaction(accountA, expenseCategory, 300.0, 1, getDate(yearC, monthC, 10));
        BudgetAllocationRequestDTO allocC = new BudgetAllocationRequestDTO();
        allocC.setCategoryId(expenseCategory.getId());
        allocC.setAmount(300.0);
        allocC.setMonth(monthC);
        allocC.setYear(yearC);
        budgetCalculationService.allocateFunds(testUser.getId(), allocC);

        Double available = budgetCalculationService.calculateAvailableToAssign(testUser.getId(), currMonth, currYear);
        assertEquals(1500.0, available, 0.001);
    }

    private void createTransaction(Account account, Category category, Double amount, Integer type, Date date) {
        Transaction t = new Transaction();
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setAmount(amount);
        t.setTransactionType(type);
        t.setDate(date);
        t.setName("Test Txn");
        t.setIsCountable(1);
        transactionRepository.save(t);
    }
}
