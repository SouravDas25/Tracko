package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UserCurrencyRepository;
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

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionCreateAutoResolveRateTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UsersRepository usersRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserCurrencyRepository userCurrencyRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String bearerToken;
    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        userCurrencyRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        user = new User();
        user.setName("U1");
        user.setPhoneNo("8000000000");
        user.setEmail("u1@example.com");
        user.setPassword("pass");
        // baseCurrency defaults to INR from changelog; leave as default or set explicitly if needed
        user = usersRepository.save(user);

        var principal = new org.springframework.security.core.userdetails.User(
                user.getPhoneNo(), user.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        account = new Account(); account.setName("A1"); account.setUserId(user.getId()); account = accountRepository.save(account);
        category = new Category(); category.setName("Food"); category.setUserId(user.getId()); category = categoryRepository.save(category);

        // Configure a secondary currency USD with rate 2.0
        UserCurrency uc = new UserCurrency();
        uc.setUser(user);
        uc.setCurrencyCode("USD");
        uc.setExchangeRate(2.0);
        userCurrencyRepository.save(uc);
    }

    @Test
    public void createRegular_autoResolvesExchangeRate_whenOmitted() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", account.getId());
        payload.put("categoryId", category.getId());
        payload.put("transactionType", 1); // DEBIT
        payload.put("name", "Groceries");
        payload.put("date", new java.util.Date());
        payload.put("originalCurrency", "USD");
        payload.put("originalAmount", 10.0);
        // No exchangeRate provided -> should auto-resolve to 2.0

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.originalAmount").value(10.0))
                .andExpect(jsonPath("$.result.exchangeRate").value(2.0))
                .andExpect(jsonPath("$.result.amount").value(20.0));
    }
}
