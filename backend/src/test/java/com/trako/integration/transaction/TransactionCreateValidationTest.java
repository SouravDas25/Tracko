package com.trako.integration.transaction;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.enums.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionCreateValidationTest extends BaseIntegrationTest {

    private String bearerToken;
    private User testUser;
    private Account acc;
    private Account acc2;
    private Category cat;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("U1");
        bearerToken = generateBearerToken(testUser);

        acc = new Account();
        acc.setName("A1");
        acc.setUserId(testUser.getId());
        acc = accountRepository.save(acc);
        acc2 = new Account();
        acc2.setName("A2");
        acc2.setUserId(testUser.getId());
        acc2 = accountRepository.save(acc2);

        cat = new Category();
        cat.setName("Food");
        cat.setUserId(testUser.getId());
        cat = categoryRepository.save(cat);
    }

    // Transfer validations
    @Test
    public void createTransfer_missingFromAccount_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (missing)
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                acc2.getId(),            // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_missingToAccount_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId (missing)
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_sameAccounts_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                acc.getId(),             // toAccountId (same as accountId)
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_missingCurrency_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                null,                    // originalCurrency (missing)
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                acc2.getId(),            // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_missingOrNonPositiveAmount_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                null,                    // originalAmount (missing)
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                acc2.getId(),            // toAccountId
                null                     // fromAccountId
        );
        // missing amount
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        
        // zero amount
        TransactionRequest zeroPayload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                0.0,                     // originalAmount (zero)
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                acc2.getId(),            // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_currencyNotConfigured_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                "USD",                   // originalCurrency (not configured)
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                acc2.getId(),            // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // Regular transaction validations
    @Test
    public void createTransaction_missingCurrency_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                cat.getId(),              // categoryId
                TransactionType.DEBIT,   // transactionType
                null,                    // isCountable
                null,                    // originalCurrency (missing)
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransaction_missingOrNonPositiveAmount_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                cat.getId(),              // categoryId
                TransactionType.DEBIT,   // transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                null,                    // originalAmount (missing)
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );
        // missing amount
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        
        // non-positive
        TransactionRequest zeroPayload = new TransactionRequest(
                null,                    // id
                acc.getId(),              // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                cat.getId(),              // categoryId
                TransactionType.DEBIT,   // transactionType
                null,                    // isCountable
                "INR",                   // originalCurrency
                0.0,                     // originalAmount (zero)
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPayload)))
                .andExpect(status().isBadRequest());
    }
}
