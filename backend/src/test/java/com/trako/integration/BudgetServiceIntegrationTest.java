package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.dtos.BudgetResponseDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.services.BudgetCalculationService;
import com.trako.services.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class BudgetServiceIntegrationTest {

    @Autowired
    private BudgetCalculationService budgetCalculationService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private BudgetMonthRepository budgetMonthRepository;

    @Autowired
    private BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser;
    private Category expenseCategory;
    private Category incomeCategory;
    private Account testAccount;

    @BeforeEach
    public void setup() {
        // Clean up
        budgetCategoryAllocationRepository.deleteAll();
        budgetMonthRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        // Seed User
        testUser = new User();
        testUser.setName("Budget User");
        testUser.setPhoneNo("5551234567");
        testUser.setEmail("budget@example.com");
        testUser.setPassword("pass");
        testUser = usersRepository.save(testUser);

        // Seed Account
        testAccount = new Account();
        testAccount.setName("Checking");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        // Seed Expense Category (budgetable)
        expenseCategory = new Category();
        expenseCategory.setName("Groceries");
        expenseCategory.setUserId(testUser.getId());
        expenseCategory.setCategoryType(CategoryType.EXPENSE);
        expenseCategory.setIsRollOverEnabled(true);
        expenseCategory = categoryRepository.save(expenseCategory);

        // Seed Income Category (Transaction.categoryId is @NotNull)
        incomeCategory = new Category();
        incomeCategory.setName("Income");
        incomeCategory.setUserId(testUser.getId());
        incomeCategory.setCategoryType(CategoryType.INCOME);
        incomeCategory = categoryRepository.save(incomeCategory);
    }

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    public void testAvailableToAssignCalculation() {
        // 1. Add Income for Jan 2026
        Transaction income = new Transaction();
        // income.setUserId(testUser.getId());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(incomeCategory.getId());
        income.setOriginalAmount(1000.0);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setTransactionType(TransactionType.CREDIT); // CREDIT/INCOME
        income.setIsCountable(1);
        income.setDate(date(2026, 1, 5));
        income.setName("Salary");
        transactionWriteService.saveForUser(testUser.getId(), income);

        // 2. Add Expense (should NOT affect Available to Assign directly, only Actual Spent)
        // Available = (Income + Rollover) - Total Allocated
        Transaction expense = new Transaction();
        // expense.setUserId(testUser.getId());
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(expenseCategory.getId());
        expense.setOriginalAmount(50.0);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setTransactionType(TransactionType.DEBIT); // DEBIT/EXPENSE
        expense.setIsCountable(1);
        expense.setDate(date(2026, 1, 10));
        expense.setName("Grocery Run");
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // 3. Verify Initial State (No allocations yet)
        // Available should be 1000 (Income) - 0 (Allocated) = 1000
        Double available = budgetCalculationService.calculateAvailableToAssign(testUser.getId(), 1, 2026);
        assertEquals(1000.0, available, 0.001);

        // 4. Allocate Funds
        BudgetAllocationRequestDTO req = new BudgetAllocationRequestDTO();
        req.setCategoryId(expenseCategory.getId());
        req.setAmount(400.0);
        req.setMonth(1);
        req.setYear(2026);
        budgetCalculationService.allocateFunds(testUser.getId(), req);

        // 5. Verify New State
        // Available = 1000 - 400 = 600
        available = budgetCalculationService.calculateAvailableToAssign(testUser.getId(), 1, 2026);
        assertEquals(600.0, available, 0.001);

        // Verify Budget Details
        BudgetResponseDTO details = budgetCalculationService.getBudgetDetails(testUser.getId(), 1, 2026, true, null);
        assertEquals(1000.0, details.getTotalIncome(), 0.001);
        assertEquals(400.0, details.getTotalBudget(), 0.001); // Total Allocated
        assertEquals(50.0, details.getTotalSpent(), 0.001);    // Actual Spent
        assertEquals(600.0, details.getAvailableToAssign(), 0.001);
    }

    @Test
    public void testRolloverLogic() {
        // --- Month 1: Jan 2026 ---
        // Income: 1000
        Transaction incomeJan = new Transaction();
        // incomeJan.setUserId(testUser.getId());
        incomeJan.setAccountId(testAccount.getId());
        incomeJan.setCategoryId(incomeCategory.getId());
        incomeJan.setOriginalAmount(1000.0);
        incomeJan.setOriginalCurrency("INR");
        incomeJan.setExchangeRate(1.0);
        incomeJan.setTransactionType(TransactionType.CREDIT);
        incomeJan.setIsCountable(1);
        incomeJan.setDate(date(2026, 1, 5));
        incomeJan.setName("Jan Salary");
        transactionWriteService.saveForUser(testUser.getId(), incomeJan);

        // Actual Spent: 100 (create before allocation so allocateFunds can compute actualSpent)
        Transaction expenseJan = new Transaction();
        // expenseJan.setUserId(testUser.getId());
        expenseJan.setAccountId(testAccount.getId());
        expenseJan.setCategoryId(expenseCategory.getId());
        expenseJan.setOriginalAmount(100.0);
        expenseJan.setOriginalCurrency("INR");
        expenseJan.setExchangeRate(1.0);
        expenseJan.setTransactionType(TransactionType.DEBIT);
        expenseJan.setIsCountable(1);
        expenseJan.setDate(date(2026, 1, 10));
        expenseJan.setName("Jan Expense");
        transactionWriteService.saveForUser(testUser.getId(), expenseJan);

        // Allocation: 400 to Groceries (Rollover Enabled)
        BudgetAllocationRequestDTO reqJan = new BudgetAllocationRequestDTO();
        reqJan.setCategoryId(expenseCategory.getId());
        reqJan.setAmount(400.0);
        reqJan.setMonth(1);
        reqJan.setYear(2026);
        budgetCalculationService.allocateFunds(testUser.getId(), reqJan);

        // End of Month 1:
        // Income = 1000, Allocated = 400, Unallocated (Global Rollover) = 600
        // Category Remaining = 400 - 100 = 300 (Category Rollover)

        // --- Month 2: Feb 2026 ---
        // Verify Rollover Calculation
        // Total Rollover = Unallocated Prev Month (600) + Rollover Enabled Categories Remaining (300) = 900

        BudgetResponseDTO febDetails = budgetCalculationService.getBudgetDetails(testUser.getId(), 2, 2026, false, null);
        
        // Expected Rollover: 600 (Global) + 300 (Category) = 900
        assertEquals(900.0, febDetails.getRolloverAmount(), 0.001);
        
        // Available to Assign in Feb = Feb Income (0) + Rollover (900) - Feb Allocated (0) = 900
        assertEquals(900.0, febDetails.getAvailableToAssign(), 0.001);
    }

    @Test
    public void testOverAllocationAllowedNow() {
        // Income: 100
        Transaction income = new Transaction();
        // income.setUserId(testUser.getId());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(incomeCategory.getId());
        income.setOriginalAmount(100.0);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setTransactionType(TransactionType.CREDIT);
        income.setIsCountable(1);
        income.setDate(date(2026, 1, 1));
        income.setName("Small Salary");
        transactionWriteService.saveForUser(testUser.getId(), income);

        // Try to allocate 150
        BudgetAllocationRequestDTO req = new BudgetAllocationRequestDTO();
        req.setCategoryId(expenseCategory.getId());
        req.setAmount(150.0);
        req.setMonth(1);
        req.setYear(2026);

        // Previously threw exception; now allowed. Ensure no exception thrown.
        budgetCalculationService.allocateFunds(testUser.getId(), req);
    }
}
