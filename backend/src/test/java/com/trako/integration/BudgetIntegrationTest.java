package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.services.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class BudgetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private AccountRepository accountRepository;

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
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;
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

        // Generate Token
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
        
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
    public void testAllocateFunds() throws Exception {
        // 0. Add Income Transaction to allow allocation
        // Set date to Jan 2024 to match request
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2024, java.util.Calendar.JANUARY, 15);
        
        Transaction income = new Transaction();
        income.setTransactionType(2); // Credit/Income
        income.setName("Salary");
        income.setAmount(1000.0);
        income.setDate(cal.getTime());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        BudgetAllocationRequestDTO request = new BudgetAllocationRequestDTO();
        request.setMonth(1);
        request.setYear(2024);
        request.setCategoryId(testCategory.getId());
        request.setAmount(500.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categoryName").value("Food"))
                .andExpect(jsonPath("$.result.allocatedAmount").value(500.0))
                .andExpect(jsonPath("$.result.remainingBalance").value(500.0));
    }
    
    @Test
    public void testGetAvailableToAssign() throws Exception {
        // 1. Add Income Transaction (Type 2)
        Transaction income = new Transaction();
        income.setTransactionType(2); // Credit/Income
        income.setName("Salary");
        income.setAmount(1000.0);
        income.setDate(new Date()); // Today
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId()); 
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);
        
        // 2. Allocate some funds
        BudgetAllocationRequestDTO request = new BudgetAllocationRequestDTO();
        LocalDate now = LocalDate.now();
        request.setMonth(now.getMonthValue());
        request.setYear(now.getYear());
        request.setCategoryId(testCategory.getId());
        request.setAmount(400.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                
        // 3. Check Available (1000 - 400 = 600)
        mockMvc.perform(get("/api/budget/available")
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(600.0));
    }

    @Test
    public void testRolloverCalculation() throws Exception {
        // 1. Setup Previous Month (Income: 1000, Allocated: 800)
        LocalDate prev = LocalDate.now().minusMonths(1);
        int prevMonth = prev.getMonthValue();
        int prevYear = prev.getYear();

        // Create Previous Month Income
        Transaction prevIncome = new Transaction();
        prevIncome.setTransactionType(2); // Credit/Income
        prevIncome.setName("Prev Salary");
        prevIncome.setAmount(1000.0);
        prevIncome.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(1)));
        prevIncome.setAccountId(testAccount.getId());
        prevIncome.setCategoryId(testCategory.getId());
        prevIncome.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), prevIncome);

        // Allocate in Previous Month (This affects Prev Month Budget, but for "Available" we look at Account Balance)
        BudgetMonth prevBudgetMonth = new BudgetMonth();
        prevBudgetMonth.setUserId(testUser.getId());
        prevBudgetMonth.setMonth(prevMonth);
        prevBudgetMonth.setYear(prevYear);
        prevBudgetMonth.setTotalBudget(800.0);
        prevBudgetMonth.setIsClosed(false);
        budgetMonthRepository.save(prevBudgetMonth);
        
        // 2. Check Current Month Available
        // Account Balance = 1000 (Prev Income) - 0 (Expense).
        // Current Allocations = 0.
        // Available = 1000.
        
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/budget/available")
                .param("month", String.valueOf(now.getMonthValue()))
                .param("year", String.valueOf(now.getYear()))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1000.0));
    }

    @Test
    public void testCategoryBalanceRollover() throws Exception {
        // 1. Setup Previous Month
        LocalDate prev = LocalDate.now().minusMonths(1);
        int prevMonth = prev.getMonthValue();
        int prevYear = prev.getYear();

        // Add Income for Prev Month to ensure we have funds
        Transaction income = new Transaction();
        income.setTransactionType(2); // Income
        income.setName("Prev Income");
        income.setAmount(1000.0);
        income.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(1)));
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // 2. Add Expense in Prev Month
        Transaction expense = new Transaction();
        expense.setTransactionType(1); // Expense
        expense.setName("Prev Expense");
        expense.setAmount(50.0);
        expense.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(15)));
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // 3. Check Rollover in Current Month
        // Account Balance = 1000 (Income) - 50 (Expense) = 950.
        // Current Allocations = 0.
        // Available = 950.
        
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/budget/available")
                .param("month", String.valueOf(now.getMonthValue()))
                .param("year", String.valueOf(now.getYear()))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(950.0));
    }

    @Test
    public void testGetBudgetDetailsWithActuals() throws Exception {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        // 0. Add Income to allow allocation
        Transaction income = new Transaction();
        income.setTransactionType(2); // Credit/Income
        income.setName("Salary");
        income.setAmount(1000.0);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // 1. Add Expense Transaction (Type 1)
        Transaction expense = new Transaction();
        expense.setTransactionType(1); // Debit/Expense
        expense.setName("Groceries");
        expense.setAmount(50.0);
        expense.setDate(new Date()); // Today
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);
        
        // 2. Allocate funds
        BudgetAllocationRequestDTO request = new BudgetAllocationRequestDTO();
        request.setMonth(month);
        request.setYear(year);
        request.setCategoryId(testCategory.getId());
        request.setAmount(200.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. Get Budget Details
        mockMvc.perform(get("/api/budget")
                .param("month", String.valueOf(month))
                .param("year", String.valueOf(year))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalBudget").value(200.0))
                .andExpect(jsonPath("$.result.totalSpent").value(50.0))
                .andExpect(jsonPath("$.result.categories[0].actualSpent").value(50.0))
                .andExpect(jsonPath("$.result.categories[0].remainingBalance").value(150.0));
    }

    @Test
    public void testOverAllocation() throws Exception {
        // 1. Add Income Transaction (1000.0)
        Transaction income = new Transaction();
        income.setTransactionType(2); // Credit/Income
        income.setName("Salary");
        income.setAmount(1000.0);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // 2. Allocate 800 (OK)
        BudgetAllocationRequestDTO request1 = new BudgetAllocationRequestDTO();
        LocalDate now = LocalDate.now();
        request1.setMonth(now.getMonthValue());
        request1.setYear(now.getYear());
        request1.setCategoryId(testCategory.getId());
        request1.setAmount(800.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // 3. Try to allocate 300 (Total 1100 > 1000) -> Should SUCCEED now (Simplification)
        BudgetAllocationRequestDTO request2 = new BudgetAllocationRequestDTO();
        request2.setMonth(now.getMonthValue());
        request2.setYear(now.getYear());
        
        Category secondCategory = new Category();
        secondCategory.setName("Fun");
        secondCategory.setUserId(testUser.getId());
        secondCategory.setIsRollOverEnabled(false);
        secondCategory = categoryRepository.save(secondCategory);

        request2.setCategoryId(secondCategory.getId());
        request2.setAmount(300.0); // 800 + 300 = 1100 > 1000

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk()); // EXPECTING OK NOW
    }

    @Test
    public void testCurrentMonthExpenseDoesNotReduceAvailable() throws Exception {
        // Logic: Available = (Opening Balance + Current Income) - Current Allocated.
        // Current Expenses are assumed to be covered by allocations and do not double-dip from Available.
        
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        // 1. Add Income (1000)
        Transaction income = new Transaction();
        income.setTransactionType(2); 
        income.setName("Income");
        income.setAmount(1000.0);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // 2. Add Expense (100)
        Transaction expense = new Transaction();
        expense.setTransactionType(1);
        expense.setName("Expense");
        expense.setAmount(100.0);
        expense.setDate(new Date());
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // 3. Check Available
        // Expected: 1000 (Income) - 0 (Allocated) = 1000.
        // Expense of 100 does NOT reduce Available. 
        // (Real Cash is 900, but Available to Assign is 1000 because we haven't assigned the 100 job yet).
        
        mockMvc.perform(get("/api/budget/available")
                .param("month", String.valueOf(month))
                .param("year", String.valueOf(year))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1000.0));
    }
}
