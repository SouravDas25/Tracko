package com.trako.integration.transaction;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.enums.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.integration.BaseIntegrationTest;
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
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                account.getId(),          // accountId
                new java.util.Date(),    // date
                "Groceries",             // name
                null,                    // comments
                category.getId(),         // categoryId
                TransactionType.DEBIT,   // transactionType
                null,                    // isCountable
                "USD",                   // originalCurrency
                10.0,                    // originalAmount
                null,                    // exchangeRate (auto-resolve)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

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
