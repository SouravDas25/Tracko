package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
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
        testUser.setFireBaseId("password");
        testUser = usersRepository.save(testUser);

        // Generate Token
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
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
        transactionRepository.save(income);

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
        transactionRepository.save(income);
        
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
        // 1. Setup Previous Month (Income: 1000, Allocated: 800, Unallocated: 200)
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
        transactionRepository.save(prevIncome);

        // Allocate in Previous Month
        BudgetMonth prevBudgetMonth = new BudgetMonth();
        prevBudgetMonth.setUserId(testUser.getId());
        prevBudgetMonth.setMonth(prevMonth);
        prevBudgetMonth.setYear(prevYear);
        prevBudgetMonth.setTotalBudget(800.0); // Manually setting total budget as if allocated
        prevBudgetMonth.setIsClosed(false);
        budgetMonthRepository.save(prevBudgetMonth);
        
        // We also need to create the allocation record to match the total budget logic if strictly enforced,
        // but the service calculates rollover based on (Total Income - Total Budget) of prev month.
        // Also it checks category rollovers. Let's stick to unallocated funds first.
        // Rollover = (Income 1000) - (Allocated 800) = 200.

        // 2. Check Current Month Available
        // Current Income = 0
        // Expected Available = Current Income (0) + Rollover (200) = 200
        
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/budget/available")
                .param("month", String.valueOf(now.getMonthValue()))
                .param("year", String.valueOf(now.getYear()))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(200.0));
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
        transactionRepository.save(income);

        // 1. Add Expense Transaction (Type 1)
        Transaction expense = new Transaction();
        expense.setTransactionType(1); // Debit/Expense
        expense.setName("Groceries");
        expense.setAmount(50.0);
        expense.setDate(new Date()); // Today
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionRepository.save(expense);
        
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
        transactionRepository.save(income);

        // 2. Allocate Funds in Prev Month
        BudgetAllocationRequestDTO allocRequest = new BudgetAllocationRequestDTO();
        allocRequest.setMonth(prevMonth);
        allocRequest.setYear(prevYear);
        allocRequest.setCategoryId(testCategory.getId());
        allocRequest.setAmount(200.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allocRequest)))
                .andExpect(status().isOk());

        // 3. Add Expense in Prev Month
        Transaction expense = new Transaction();
        expense.setTransactionType(1); // Expense
        expense.setName("Prev Expense");
        expense.setAmount(50.0);
        expense.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(15)));
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionRepository.save(expense);

        // 4. Trigger Actuals Update by fetching Prev Month Budget
        mockMvc.perform(get("/api/budget")
                .param("month", String.valueOf(prevMonth))
                .param("year", String.valueOf(prevYear))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categories[0].actualSpent").value(50.0))
                .andExpect(jsonPath("$.result.categories[0].remainingBalance").value(150.0));

        // 5. Check Rollover in Current Month
        // Unallocated from Prev: 1000 - 200 = 800
        // Category Rollover: 150
        // Total Rollover Expected: 950
        
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/budget/available")
                .param("month", String.valueOf(now.getMonthValue()))
                .param("year", String.valueOf(now.getYear()))
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(950.0));
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
        transactionRepository.save(income);

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

        // 3. Try to allocate 300 (Total 1100 > 1000) -> Should Fail
        BudgetAllocationRequestDTO request2 = new BudgetAllocationRequestDTO();
        request2.setMonth(now.getMonthValue());
        request2.setYear(now.getYear());
        request2.setCategoryId(testCategory.getId()); // Same category, updating amount?
        // If I use the same category and set amount to 300, that is updating the allocation to 300.
        // That would be VALID (Total 300 < 1000).
        // I want to ADD another allocation or UPDATE to 1100.
        
        // Let's create a second category for clarity
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
                .andExpect(status().isBadRequest()); // Expecting 400
    }

    @Test
    public void testOverAllocationWithRollover() throws Exception {
        // Disable rollover for this category to strictly test "Unallocated Funds" rollover (Income - Allocated)
        // Otherwise, the 800 allocated in prev month would also roll over, making total available 1000.
        testCategory.setIsRollOverEnabled(false);
        categoryRepository.save(testCategory);

        // 1. Setup Previous Month (Income: 1000, Allocated: 800) -> Unallocated = 200
        LocalDate prev = LocalDate.now().minusMonths(1);
        int prevMonth = prev.getMonthValue();
        int prevYear = prev.getYear();

        // Previous Month Income
        Transaction prevIncome = new Transaction();
        prevIncome.setTransactionType(2); // Income
        prevIncome.setName("Prev Salary");
        prevIncome.setAmount(1000.0);
        prevIncome.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(1)));
        prevIncome.setAccountId(testAccount.getId());
        prevIncome.setCategoryId(testCategory.getId());
        prevIncome.setIsCountable(1);
        transactionRepository.save(prevIncome);

        // Previous Month Allocation
        BudgetAllocationRequestDTO prevAlloc = new BudgetAllocationRequestDTO();
        prevAlloc.setMonth(prevMonth);
        prevAlloc.setYear(prevYear);
        prevAlloc.setCategoryId(testCategory.getId());
        prevAlloc.setAmount(800.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prevAlloc)))
                .andExpect(status().isOk());

        // 2. Current Month (Income: 0)
        // Available should be 200 (Rollover)
        LocalDate now = LocalDate.now();
        
        // Try to allocate 200 (Should Pass)
        BudgetAllocationRequestDTO currentAllocOk = new BudgetAllocationRequestDTO();
        currentAllocOk.setMonth(now.getMonthValue());
        currentAllocOk.setYear(now.getYear());
        currentAllocOk.setCategoryId(testCategory.getId());
        currentAllocOk.setAmount(200.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(currentAllocOk)))
                .andExpect(status().isOk());

        // Try to allocate 201 (Should Fail, Total > 200)
        // Note: We need to use a different category or update the existing one to > 200.
        // Let's update the existing one to 250.
        currentAllocOk.setAmount(250.0);

        mockMvc.perform(post("/api/budget/allocate")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(currentAllocOk)))
                .andExpect(status().isBadRequest());
    }
}
