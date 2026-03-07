package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionPutAmountTest {

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
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(), testUser.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        testAccount = new Account();
        testAccount.setName("A1");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    public void testUpdateTransaction_UsesAmountField_WhenOriginalAmountMissing() throws Exception {
        // Create a valid transaction first (amount = originalAmount * exchangeRate)
        Transaction t = new Transaction();
        t.setTransactionType(TransactionType.DEBIT);
        t.setName("Original Name");
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setIsCountable(1);
        t = transactionWriteService.saveForUser(testUser.getId(), t);

        // Update providing only originalAmount (no legacy 'amount' support)
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", testAccount.getId());
        payload.put("originalAmount", 25.0);
        // currency/rate omitted -> keep existing (INR, 1.0)

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                // With exchangeRate 1.0, amount should equal originalAmount
                .andExpect(jsonPath("$.result.amount").value(25.0))
                .andExpect(jsonPath("$.result.originalAmount").value(25.0));
    }
}
