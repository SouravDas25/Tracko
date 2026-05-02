package com.trako.integration.transaction;

import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.transactions.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GET /api/transactions/search endpoint.
 * Validates: Requirements 1.1, 1.5, 4.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TransactionSearchEndpointIT extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser();
        bearerToken = generateBearerToken(testUser);

        testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);
    }

    private Transaction createTransaction(String name, double amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionDbType.DEBIT);
        tx.setName(name);
        tx.setOriginalAmount(amount);
        tx.setOriginalCurrency("INR");
        tx.setExchangeRate(1.0);
        tx.setDate(new Date());
        tx.setAccountId(testAccount.getId());
        tx.setCategoryId(testCategory.getId());
        return transactionWriteService.saveForUser(testUser.getId(), tx);
    }

    @Test
    public void testSuccessfulSearch_returns200WithResults() throws Exception {
        createTransaction("coffee", 5.00);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.result.results").isArray())
                .andExpect(jsonPath("$.result.query").value("coffee"));
    }

    @Test
    public void testEmptyQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "")
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testQueryTooLong_returns400() throws Exception {
        String longQuery = "a".repeat(201);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", longQuery)
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidDateRange_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee")
                        .param("startDate", "2024-12-31")
                        .param("endDate", "2024-01-01")
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUnauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testPaginationParameters_workCorrectly() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createTransaction("coffee item " + i, i * 10.0);
        }

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(5))
                .andExpect(jsonPath("$.result.results", hasSize(2)))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(true));
    }
}
