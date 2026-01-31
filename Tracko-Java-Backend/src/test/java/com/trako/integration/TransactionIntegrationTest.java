package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UsersRepository usersRepository;

    private User testUser;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser = usersRepository.save(testUser);

        testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    public void testCreateTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Lunch");
        transaction.setAmount(25.50);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transaction.setComments("Pizza");

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Lunch"))
                .andExpect(jsonPath("$.result.amount").value(25.50))
                .andExpect(jsonPath("$.result.comments").value("Pizza"));
    }

    @Test
    public void testGetAllTransactions() throws Exception {
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionType(1);
        transaction1.setName("Lunch");
        transaction1.setAmount(25.50);
        transaction1.setDate(new Date());
        transaction1.setAccountId(testAccount.getId());
        transaction1.setCategoryId(testCategory.getId());
        transactionRepository.save(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionType(1);
        transaction2.setName("Dinner");
        transaction2.setAmount(35.00);
        transaction2.setDate(new Date());
        transaction2.setAccountId(testAccount.getId());
        transaction2.setCategoryId(testCategory.getId());
        transactionRepository.save(transaction2);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].name", containsInAnyOrder("Lunch", "Dinner")));
    }

    @Test
    public void testGetTransactionById() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Coffee");
        transaction.setAmount(5.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionRepository.save(transaction);

        mockMvc.perform(get("/api/transactions/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Coffee"))
                .andExpect(jsonPath("$.result.amount").value(5.00));
    }

    @Test
    public void testGetTransactionsByUserId() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Groceries");
        transaction.setAmount(100.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transactionRepository.save(transaction);

        mockMvc.perform(get("/api/transactions/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("Groceries"));
    }

    @Test
    public void testGetTransactionsByAccountId() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("ATM Withdrawal");
        transaction.setAmount(200.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transactionRepository.save(transaction);

        mockMvc.perform(get("/api/transactions/account/" + testAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("ATM Withdrawal"));
    }

    @Test
    public void testGetTransactionsByCategoryId() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Restaurant");
        transaction.setAmount(50.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transactionRepository.save(transaction);

        mockMvc.perform(get("/api/transactions/category/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("Restaurant"));
    }

    @Test
    public void testUpdateTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Old Name");
        transaction.setAmount(10.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionRepository.save(transaction);

        saved.setName("Updated Name");
        saved.setAmount(15.00);

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Name"))
                .andExpect(jsonPath("$.result.amount").value(15.00));
    }

    @Test
    public void testDeleteTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("To Delete");
        transaction.setAmount(1.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionRepository.save(transaction);

        mockMvc.perform(delete("/api/transactions/" + saved.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetSummary() throws Exception {
        // Create income transaction
        Transaction income = new Transaction();
        income.setTransactionType(2); // CREDIT = income
        income.setName("Salary");
        income.setAmount(1000.00);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionRepository.save(income);

        // Create expense transaction
        Transaction expense = new Transaction();
        expense.setTransactionType(1); // DEBIT = expense
        expense.setName("Groceries");
        expense.setAmount(200.00);
        expense.setDate(new Date());
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionRepository.save(expense);

        // Create non-countable transaction (should be excluded)
        Transaction nonCountable = new Transaction();
        nonCountable.setTransactionType(1); // DEBIT = expense
        nonCountable.setName("Transfer");
        nonCountable.setAmount(50.00);
        nonCountable.setDate(new Date());
        nonCountable.setAccountId(testAccount.getId());
        nonCountable.setCategoryId(testCategory.getId());
        nonCountable.setIsCountable(0);
        transactionRepository.save(nonCountable);

        mockMvc.perform(get("/api/transactions/user/" + testUser.getId() + "/summary")
                .param("startDate", "2020-01-01")
                .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.result.totalExpense").value(200.00))
                .andExpect(jsonPath("$.result.netTotal").value(800.00))
                .andExpect(jsonPath("$.result.transactionCount").value(2));
    }

    @Test
    public void testGetTotalIncome() throws Exception {
        Transaction income1 = new Transaction();
        income1.setTransactionType(2); // CREDIT = income
        income1.setName("Salary");
        income1.setAmount(1000.00);
        income1.setDate(new Date());
        income1.setAccountId(testAccount.getId());
        income1.setCategoryId(testCategory.getId());
        income1.setIsCountable(1);
        transactionRepository.save(income1);

        Transaction income2 = new Transaction();
        income2.setTransactionType(2); // CREDIT = income
        income2.setName("Bonus");
        income2.setAmount(500.00);
        income2.setDate(new Date());
        income2.setAccountId(testAccount.getId());
        income2.setCategoryId(testCategory.getId());
        income2.setIsCountable(1);
        transactionRepository.save(income2);

        mockMvc.perform(get("/api/transactions/user/" + testUser.getId() + "/total-income")
                .param("startDate", "2020-01-01")
                .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1500.00));
    }

    @Test
    public void testGetTotalExpense() throws Exception {
        Transaction expense1 = new Transaction();
        expense1.setTransactionType(1); // DEBIT = expense
        expense1.setName("Groceries");
        expense1.setAmount(200.00);
        expense1.setDate(new Date());
        expense1.setAccountId(testAccount.getId());
        expense1.setCategoryId(testCategory.getId());
        expense1.setIsCountable(1);
        transactionRepository.save(expense1);

        Transaction expense2 = new Transaction();
        expense2.setTransactionType(1); // DEBIT = expense
        expense2.setName("Utilities");
        expense2.setAmount(150.00);
        expense2.setDate(new Date());
        expense2.setAccountId(testAccount.getId());
        expense2.setCategoryId(testCategory.getId());
        expense2.setIsCountable(1);
        transactionRepository.save(expense2);

        mockMvc.perform(get("/api/transactions/user/" + testUser.getId() + "/total-expense")
                .param("startDate", "2020-01-01")
                .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(350.00));
    }

    @Test
    public void testGetSummaryExcludesNonCountable() throws Exception {
        // Only non-countable transactions
        Transaction nonCountable1 = new Transaction();
        nonCountable1.setTransactionType(2); // CREDIT = income
        nonCountable1.setName("Transfer In");
        nonCountable1.setAmount(500.00);
        nonCountable1.setDate(new Date());
        nonCountable1.setAccountId(testAccount.getId());
        nonCountable1.setCategoryId(testCategory.getId());
        nonCountable1.setIsCountable(0);
        transactionRepository.save(nonCountable1);

        Transaction nonCountable2 = new Transaction();
        nonCountable2.setTransactionType(1); // DEBIT = expense
        nonCountable2.setName("Transfer Out");
        nonCountable2.setAmount(300.00);
        nonCountable2.setDate(new Date());
        nonCountable2.setAccountId(testAccount.getId());
        nonCountable2.setCategoryId(testCategory.getId());
        nonCountable2.setIsCountable(0);
        transactionRepository.save(nonCountable2);

        mockMvc.perform(get("/api/transactions/user/" + testUser.getId() + "/summary")
                .param("startDate", "2020-01-01")
                .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(0.00))
                .andExpect(jsonPath("$.result.totalExpense").value(0.00))
                .andExpect(jsonPath("$.result.netTotal").value(0.00))
                .andExpect(jsonPath("$.result.transactionCount").value(0));
    }
}
