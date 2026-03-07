package com.trako.integration.transaction;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.TransferResult;
import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.services.transactions.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionAmountValidationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private TransferService transferService;

    private User testUser;
    private String bearerToken;
    private Account testAccount;
    private Account secondAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);

        testAccount = new Account();
        testAccount.setName("A1");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        secondAccount = new Account();
        secondAccount.setName("A2");
        secondAccount.setUserId(testUser.getId());
        secondAccount = accountRepository.save(secondAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    public void createTransaction_zeroAmount_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", TransactionType.DEBIT.name());
        payload.put("originalAmount", 0.0);
        payload.put("accountId", testAccount.getId());
        payload.put("categoryId", testCategory.getId());
        payload.put("name", "T1");
        payload.put("date", new Date());
        payload.put("originalCurrency", "INR");
        payload.put("exchangeRate", 1.0);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_negativeAmount_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", "TRANSFER");
        payload.put("originalAmount", -5.0);
        payload.put("fromAccountId", testAccount.getId());
        payload.put("toAccountId", secondAccount.getId());
        payload.put("name", "TF");
        payload.put("date", new Date());
        payload.put("originalCurrency", "INR");
        payload.put("exchangeRate", 1.0);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateTransaction_setZeroAmount_returnsBadRequest() throws Exception {
        // create valid transaction first
        Transaction t = new Transaction();
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setName("T2");
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t = transactionWriteService.saveForUser(testUser.getId(), t);

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", TransactionType.DEBIT);
        payload.put("originalAmount", 0.0);

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateTransfer_setNegativeAmount_returnsBadRequest() throws Exception {
        // create valid transfer via service for setup
        TransferResult pair = transferService.createTransfer(
                testUser.getId(),
                testAccount.getId(),
                secondAccount.getId(),
                new Date(),
                10.0,
                "INR",
                1.0,
                "TF Valid",
                null
        );

        Long anySideId = pair.debit().getId();

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", TransactionType.TRANSFER);
        payload.put("originalAmount", -1.0);
        payload.put("toAccountId", secondAccount.getId());

        mockMvc.perform(put("/api/transactions/" + anySideId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }
}
