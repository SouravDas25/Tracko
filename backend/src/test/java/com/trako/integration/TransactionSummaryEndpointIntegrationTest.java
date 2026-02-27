package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionSummaryEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;
    private Account account1;
    private Account account2;
    private Category category;

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
        testUser.setPassword("password");
        testUser = usersRepository.save(testUser);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        account1 = new Account();
        account1.setName("Acc 1");
        account1.setUserId(testUser.getId());
        account1 = accountRepository.save(account1);

        account2 = new Account();
        account2.setName("Acc 2");
        account2.setUserId(testUser.getId());
        account2 = accountRepository.save(account2);

        category = new Category();
        category.setName("Cat 1");
        category.setUserId(testUser.getId());
        category = categoryRepository.save(category);
    }

    @Test
    public void testGetMonthlySummaries_AllAccounts() throws Exception {
        // Acc 1: Jan +1000
        createTransaction(account1.getId(), category.getId(), 2024, Calendar.JANUARY, 1000.0, 2);
        // Acc 2: Jan -200
        createTransaction(account2.getId(), category.getId(), 2024, Calendar.JANUARY, 200.0, 1);
        // Total Jan: +800

        // Acc 1: Feb +500
        createTransaction(account1.getId(), category.getId(), 2024, Calendar.FEBRUARY, 500.0, 2);
        // Total Feb: +500

        mockMvc.perform(get("/api/transactions/summary/monthly")
                        .param("year", "2024")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[?(@.month == 1)].netTotal").value(800.0))
                .andExpect(jsonPath("$.result[?(@.month == 2)].netTotal").value(500.0));
    }

    @Test
    public void testGetYearlySummaries_AllAccounts() throws Exception {
        // 2023: Acc 1 +1000
        createTransaction(account1.getId(), category.getId(), 2023, Calendar.DECEMBER, 1000.0, 2);

        // 2024: Acc 1 +2000, Acc 2 -500
        createTransaction(account1.getId(), category.getId(), 2024, Calendar.JANUARY, 2000.0, 2);
        createTransaction(account2.getId(), category.getId(), 2024, Calendar.FEBRUARY, 500.0, 1);
        // Net 2024: +1500

        mockMvc.perform(get("/api/transactions/summary/yearly")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[?(@.year == 2024)].netTotal").value(1500.0))
                .andExpect(jsonPath("$.result[?(@.year == 2023)].netTotal").value(1000.0));
    }

    private void createTransaction(Long accountId, Long categoryId, int year, int month, double amount, int type) {
        Transaction t = new Transaction();
        t.setAccountId(accountId);
        t.setCategoryId(categoryId);
        t.setTransactionType(type);
        t.setAmount(amount);
        t.setIsCountable(1);
        t.setName("Txn");
        t.setDate(new GregorianCalendar(year, month, 15).getTime());
        transactionWriteService.saveForUser(testUser.getId(), t);
    }
}
