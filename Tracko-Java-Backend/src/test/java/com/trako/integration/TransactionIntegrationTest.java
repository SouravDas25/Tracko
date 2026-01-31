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
}
