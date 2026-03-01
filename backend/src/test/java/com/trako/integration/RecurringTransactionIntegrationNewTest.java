package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.RecurringTransactionRepository;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class RecurringTransactionIntegrationNewTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UsersRepository usersRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private RecurringTransactionRepository recurringTransactionRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;
    private Account accA1;
    private Category catA1;

    @BeforeEach
    public void setup() {
        recurringTransactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        userA = new User(); userA.setName("A"); userA.setPhoneNo("7000000001"); userA.setEmail("a@x.com"); userA.setPassword("p"); userA = usersRepository.save(userA);
        userB = new User(); userB.setName("B"); userB.setPhoneNo("7000000002"); userB.setEmail("b@x.com"); userB.setPassword("p"); userB = usersRepository.save(userB);

        var pA = new org.springframework.security.core.userdetails.User(userA.getPhoneNo(), userA.getPassword(), Collections.emptyList());
        var pB = new org.springframework.security.core.userdetails.User(userB.getPhoneNo(), userB.getPassword(), Collections.emptyList());
        tokenA = "Bearer " + jwtTokenUtil.generateToken(pA);
        tokenB = "Bearer " + jwtTokenUtil.generateToken(pB);

        accA1 = new Account(); accA1.setName("A1"); accA1.setUserId(userA.getId()); accA1 = accountRepository.save(accA1);
        catA1 = new Category(); catA1.setName("Food"); catA1.setUserId(userA.getId()); catA1 = categoryRepository.save(catA1);
    }

    private Map<String, Object> basePayload() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "Sub");
        m.put("accountId", accA1.getId());
        m.put("categoryId", catA1.getId());
        m.put("transactionType", "DEBIT");
        m.put("frequency", "MONTHLY");
        Date now = new Date();
        m.put("startDate", now);
        m.put("nextRunDate", now);
        m.put("isActive", true);
        return m;
    }

    @Test
    public void createRecurring_withZeroOriginalAmount_returnsBadRequest() throws Exception {
        Map<String, Object> p = basePayload();
        p.put("originalCurrency", "INR");
        p.put("originalAmount", 0.0); // @Positive -> 400
        p.put("exchangeRate", 1.0);
        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createRecurring_withZeroExchangeRate_returnsBadRequest() throws Exception {
        Map<String, Object> p = basePayload();
        p.put("originalCurrency", "INR");
        p.put("originalAmount", 10.0);
        p.put("exchangeRate", 0.0); // @Positive -> 400
        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateRecurring_setNegativeOriginalAmount_returnsBadRequest() throws Exception {
        // Create a minimal valid recurring
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(userA.getId());
        rt.setName("R");
        rt.setAccountId(accA1.getId());
        rt.setCategoryId(catA1.getId());
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        Date now = new Date();
        rt.setStartDate(now);
        rt.setNextRunDate(now);
        rt = recurringTransactionRepository.save(rt);

        Map<String, Object> updates = new HashMap<>();
        updates.put("originalAmount", -5.0); // @Positive -> 400
        mockMvc.perform(put("/api/recurring-transactions/" + rt.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteRecurring_unauthorized_returnsUnauthorized() throws Exception {
        // Create under userA
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(userA.getId());
        rt.setName("R");
        rt.setAccountId(accA1.getId());
        rt.setCategoryId(catA1.getId());
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        Date now = new Date();
        rt.setStartDate(now);
        rt.setNextRunDate(now);
        rt = recurringTransactionRepository.save(rt);

        mockMvc.perform(delete("/api/recurring-transactions/" + rt.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isUnauthorized());
    }
}
