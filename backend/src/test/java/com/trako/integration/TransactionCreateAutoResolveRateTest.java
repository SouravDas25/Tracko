package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.repositories.UserCurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionCreateAutoResolveRateTest extends BaseIntegrationTest {

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    private String bearerToken;
    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("U1");
        bearerToken = generateBearerToken(user);

        account = new Account();
        account.setName("A1");
        account.setUserId(user.getId());
        account = accountRepository.save(account);
        category = new Category();
        category.setName("Food");
        category.setUserId(user.getId());
        category = categoryRepository.save(category);

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
