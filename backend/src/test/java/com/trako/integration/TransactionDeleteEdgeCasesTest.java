package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionDeleteEdgeCasesTest extends BaseIntegrationTest {

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;
    private Account accA;
    private Category catA;

    @BeforeEach
    public void setup() {
        userA = createUniqueUser("A");
        tokenA = generateBearerToken(userA);

        userB = createUniqueUser("B");
        tokenB = generateBearerToken(userB);

        accA = new Account();
        accA.setName("A1");
        accA.setUserId(userA.getId());
        accA = accountRepository.save(accA);
        catA = new Category();
        catA.setName("Food");
        catA.setUserId(userA.getId());
        catA = categoryRepository.save(catA);
    }

    private Transaction createTransactionForUserA() {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionEntryType.DEBIT);
        t.setName("T");
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(accA.getId());
        t.setCategoryId(catA.getId());
        t.setIsCountable(1);
        return transactionRepository.save(t);
    }

    @Test
    public void delete_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/transactions/999999")
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    public void delete_unauthorized_returnsUnauthorized() throws Exception {
        Transaction t = createTransactionForUserA();
        mockMvc.perform(delete("/api/transactions/" + t.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isUnauthorized());
    }
}
