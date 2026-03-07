package com.trako.services;

import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.dtos.BudgetCategoryDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.services.transactions.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BudgetRolloverDebugTest {

    @Autowired
    private BudgetCalculationService budgetCalculationService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    private BudgetMonthRepository budgetMonthRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        // Clear data
        budgetCategoryAllocationRepository.deleteAll();
        budgetMonthRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        // Create User
        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = usersRepository.save(testUser);

        // Create Account
        testAccount = new Account();
        testAccount.setName("Test Account");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        // Create Category
        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory.setIsRollOverEnabled(true);
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    public void debugRolloverIssue() {
        LocalDate prev = LocalDate.now().minusMonths(1);
        int prevMonth = prev.getMonthValue();
        int prevYear = prev.getYear();

        // 1. Add Income for Previous Month
        Transaction prevIncome = new Transaction();
        prevIncome.setTransactionType(TransactionType.CREDIT); // Income
        prevIncome.setName("Prev Salary");
        prevIncome.setOriginalAmount(1000.0);
        prevIncome.setOriginalCurrency("INR");
        prevIncome.setExchangeRate(1.0);
        prevIncome.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(15)));
        prevIncome.setAccountId(testAccount.getId());
        prevIncome.setCategoryId(testCategory.getId());
        prevIncome.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), prevIncome);

        // 2. Allocate Funds in Previous Month
        BudgetAllocationRequestDTO allocRequest = new BudgetAllocationRequestDTO();
        allocRequest.setMonth(prevMonth);
        allocRequest.setYear(prevYear);
        allocRequest.setCategoryId(testCategory.getId());
        allocRequest.setAmount(200.0);
        BudgetCategoryDTO allocationResult = budgetCalculationService.allocateFunds(testUser.getId(), allocRequest);
        assertNotNull(allocationResult);
        assertEquals(200.0, allocationResult.getAllocatedAmount());

        // 3. Add Expense in Previous Month
        Transaction expense = new Transaction();
        expense.setTransactionType(TransactionType.DEBIT); // Expense
        expense.setName("Prev Expense");
        expense.setOriginalAmount(50.0);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(20)));
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // 4. Check BudgetCategoryAllocation state BEFORE fetching budget details
        BudgetMonth prevBudgetMonth = budgetMonthRepository.findByUserIdAndMonthAndYear(
                testUser.getId(), prevMonth, prevYear).orElse(null);
        assertNotNull(prevBudgetMonth);
        System.out.println("Prev Budget Month: " + prevBudgetMonth);

        BudgetCategoryAllocation prevAllocation = budgetCategoryAllocationRepository
                .findByBudgetMonthIdAndCategoryId(prevBudgetMonth.getId(), testCategory.getId()).orElse(null);
        assertNotNull(prevAllocation);
        System.out.println("Prev Allocation BEFORE fetching budget details: " + prevAllocation);
        System.out.println("Prev Allocation Remaining Balance: " + prevAllocation.getRemainingBalance());

        // 5. Get Budget Details for Previous Month (this should update actual spent and remaining balance)
        budgetCalculationService.getBudgetDetails(testUser.getId(), prevMonth, prevYear, true, null);

        // 6. Check BudgetCategoryAllocation state AFTER fetching budget details
        prevAllocation = budgetCategoryAllocationRepository
                .findByBudgetMonthIdAndCategoryId(prevBudgetMonth.getId(), testCategory.getId()).orElse(null);
        assertNotNull(prevAllocation);
        System.out.println("Prev Allocation AFTER fetching budget details: " + prevAllocation);
        System.out.println("Prev Allocation Remaining Balance: " + prevAllocation.getRemainingBalance());

        // 7. Calculate Rollover for Current Month by getting available funds
        LocalDate now = LocalDate.now();
        Double availableToAssign = budgetCalculationService.calculateAvailableToAssign(testUser.getId(), now.getMonthValue(), now.getYear());
        System.out.println("Available to Assign: " + availableToAssign);

        // Expected: Unallocated (1000 - 200) + Category Rollover (150) = 950
        // Available to assign should be 950 (rollover) since current month has no income
        assertEquals(950.0, availableToAssign);
    }
}
