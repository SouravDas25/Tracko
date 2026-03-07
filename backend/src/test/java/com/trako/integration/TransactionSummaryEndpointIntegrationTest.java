package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.services.transactions.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionSummaryEndpointIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    private User testUser;
    private String bearerToken;
    private Account account1;
    private Account account2;
    private Category category;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);

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
        createTransaction(account1.getId(), category.getId(), 2024, Calendar.JANUARY, 1000.0, TransactionEntryType.CREDIT);
        // Acc 2: Jan -200
        createTransaction(account2.getId(), category.getId(), 2024, Calendar.JANUARY, 200.0, TransactionEntryType.DEBIT);
        // Total Jan: +800

        // Acc 1: Feb +500
        createTransaction(account1.getId(), category.getId(), 2024, Calendar.FEBRUARY, 500.0, TransactionEntryType.CREDIT);
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
        createTransaction(account1.getId(), category.getId(), 2023, Calendar.DECEMBER, 1000.0, TransactionEntryType.CREDIT);

        // 2024: Acc 1 +2000, Acc 2 -500
        createTransaction(account1.getId(), category.getId(), 2024, Calendar.JANUARY, 2000.0, TransactionEntryType.CREDIT);
        createTransaction(account2.getId(), category.getId(), 2024, Calendar.FEBRUARY, 500.0, TransactionEntryType.DEBIT);
        // Net 2024: +1500

        mockMvc.perform(get("/api/transactions/summary/yearly")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[?(@.year == 2024)].netTotal").value(1500.0))
                .andExpect(jsonPath("$.result[?(@.year == 2023)].netTotal").value(1000.0));
    }

    private void createTransaction(Long accountId, Long categoryId, int year, int month, double amount, TransactionEntryType type) {
        Transaction t = new Transaction();
        t.setAccountId(accountId);
        t.setCategoryId(categoryId);
        t.setTransactionType(type);
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);
        t.setName("Txn");
        t.setDate(new GregorianCalendar(year, month, 15).getTime());
        transactionWriteService.saveForUser(testUser.getId(), t);
    }
}
