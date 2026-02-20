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
import java.util.Date;
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
public class AccountSummaryIntegrationTest {

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
    private Account account;
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
        testUser.setFireBaseId("password");
        testUser = usersRepository.save(testUser);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        account = new Account();
        account.setName("Test Account");
        account.setUserId(testUser.getId());
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Test Category");
        category.setUserId(testUser.getId());
        category = categoryRepository.save(category);
    }

    @Test
    public void testGetAccountMonthlySummaries() throws Exception {
        // Add transactions in different months of 2024
        createTransaction(account.getId(), category.getId(), 2024, Calendar.JANUARY, 1000.0, 2); // Income
        createTransaction(account.getId(), category.getId(), 2024, Calendar.JANUARY, 200.0, 1);  // Expense
        // Net Jan: +800

        createTransaction(account.getId(), category.getId(), 2024, Calendar.FEBRUARY, 500.0, 2); // Income
        // Net Feb: +500

        mockMvc.perform(get("/api/accounts/" + account.getId() + "/summary/monthly")
                        .param("year", "2024")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].month").value(2)) // Descending order usually? Or check content
                .andExpect(jsonPath("$.result[0].netTotal").value(500.0))
                .andExpect(jsonPath("$.result[1].month").value(1))
                .andExpect(jsonPath("$.result[1].netTotal").value(800.0));
    }

    @Test
    public void testGetAccountYearlySummaries() throws Exception {
        // 2023
        createTransaction(account.getId(), category.getId(), 2023, Calendar.DECEMBER, 1000.0, 2); // +1000

        // 2024
        createTransaction(account.getId(), category.getId(), 2024, Calendar.JANUARY, 2000.0, 2); // +2000
        createTransaction(account.getId(), category.getId(), 2024, Calendar.FEBRUARY, 500.0, 1); // -500
        // Net 2024: +1500

        mockMvc.perform(get("/api/accounts/" + account.getId() + "/summary/yearly")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].year").value(2024))
                .andExpect(jsonPath("$.result[0].netTotal").value(1500.0))
                .andExpect(jsonPath("$.result[1].year").value(2023))
                .andExpect(jsonPath("$.result[1].netTotal").value(1000.0));
    }

    @Test
    public void testGetAccountSummary_Unauthorized() throws Exception {
        User otherUser = new User();
        otherUser.setName("Other");
        otherUser.setPhoneNo("0987654321");
        otherUser.setFireBaseId("other");
        otherUser = usersRepository.save(otherUser);

        Account otherAccount = new Account();
        otherAccount.setName("Other Acc");
        otherAccount.setUserId(otherUser.getId());
        otherAccount = accountRepository.save(otherAccount);

        mockMvc.perform(get("/api/accounts/" + otherAccount.getId() + "/summary/monthly")
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
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
