package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UsersRepository;
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
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionCreateValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsersRepository usersRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String bearerToken;
    private User testUser;
    private Account acc;
    private Account acc2;
    private Category cat;

    @BeforeEach
    public void setup() {
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("U1");
        testUser.setPhoneNo("1111111111");
        testUser.setEmail("u1@example.com");
        testUser.setPassword("pass");
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(), testUser.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        acc = new Account(); acc.setName("A1"); acc.setUserId(testUser.getId()); acc = accountRepository.save(acc);
        acc2 = new Account(); acc2.setName("A2"); acc2.setUserId(testUser.getId()); acc2 = accountRepository.save(acc2);

        cat = new Category(); cat.setName("Food"); cat.setUserId(testUser.getId()); cat = categoryRepository.save(cat);
    }

    // Transfer validations
    @Test
    public void createTransfer_missingFromAccount_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toAccountId", acc2.getId());
        payload.put("originalAmount", 10.0);
        payload.put("originalCurrency", "INR");
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_missingToAccount_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("originalAmount", 10.0);
        payload.put("originalCurrency", "INR");
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_sameAccounts_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("toAccountId", acc.getId());
        payload.put("originalAmount", 10.0);
        payload.put("originalCurrency", "INR");
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_missingCurrency_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("toAccountId", acc2.getId());
        payload.put("originalAmount", 10.0);
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_missingOrNonPositiveAmount_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("toAccountId", acc2.getId());
        payload.put("originalCurrency", "INR");
        // missing amount
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        // zero amount
        payload.put("originalAmount", 0.0);
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_currencyNotConfigured_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("toAccountId", acc2.getId());
        payload.put("originalAmount", 10.0);
        payload.put("originalCurrency", "USD"); // not configured, base currency likely null/different
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // Regular transaction validations
    @Test
    public void createTransaction_missingCurrency_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("categoryId", cat.getId());
        payload.put("transactionType", "DEBIT");
        payload.put("originalAmount", 10.0);
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransaction_missingOrNonPositiveAmount_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", acc.getId());
        payload.put("categoryId", cat.getId());
        payload.put("transactionType", "DEBIT");
        payload.put("originalCurrency", "INR");
        // missing amount
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        // non-positive
        payload.put("originalAmount", 0.0);
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }
}
