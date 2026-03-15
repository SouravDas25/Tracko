package com.trako.integration.transaction;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TransactionCreateValidationTest extends BaseIntegrationTest {

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

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

    // ==================== Transfer create validations ====================

    @Test
    public void createTransfer_missingFromAccount_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (missing)
                new Date(),              // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
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
                acc.getId(),             // accountId
                new Date(),              // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
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
                acc.getId(),             // accountId
                new Date(),              // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
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
                acc.getId(),             // accountId
                new Date(),              // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
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
        // missing amount
        TransactionRequest payload = new TransactionRequest(
                null, acc.getId(), new Date(), null, null, null,
                TransactionType.TRANSFER, "INR", null, null, null, acc2.getId(), null
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        // zero amount
        TransactionRequest zeroPayload = new TransactionRequest(
                null, acc.getId(), new Date(), null, null, null,
                TransactionType.TRANSFER, "INR", 0.0, null, null, acc2.getId(), null
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPayload)))
                .andExpect(status().isBadRequest());

        // negative amount
        TransactionRequest negativePayload = new TransactionRequest(
                null, acc.getId(), new Date(), null, null, null,
                TransactionType.TRANSFER, "INR", -5.0, null, null, acc2.getId(), null
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativePayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransfer_currencyNotConfigured_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),             // accountId
                new Date(),              // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
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

    // ==================== Regular transaction create validations ====================

    @Test
    public void createTransaction_missingCurrency_returnsBadRequest() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),             // accountId
                new Date(),              // date
                null,                    // name
                null,                    // comments
                cat.getId(),             // categoryId
                TransactionType.DEBIT,   // transactionType
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
        // missing amount
        TransactionRequest payload = new TransactionRequest(
                null, acc.getId(), new Date(), null, null, cat.getId(),
                TransactionType.DEBIT, "INR", null, null, null, null, null
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        // zero amount
        TransactionRequest zeroPayload = new TransactionRequest(
                null, acc.getId(), new Date(), null, null, cat.getId(),
                TransactionType.DEBIT, "INR", 0.0, null, null, null, null
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createRegular_autoResolvesExchangeRate_whenOmitted() throws Exception {
        // Configure USD with a known exchange rate
        UserCurrency uc = new UserCurrency();
        uc.setUser(testUser);
        uc.setCurrencyCode("USD");
        uc.setExchangeRate(2.0);
        userCurrencyRepository.save(uc);

        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                acc.getId(),             // accountId
                new Date(),              // date
                "Groceries",             // name
                null,                    // comments
                cat.getId(),             // categoryId
                TransactionType.DEBIT,   // transactionType
                "USD",                   // originalCurrency
                10.0,                    // originalAmount
                null,                    // exchangeRate (omitted — should auto-resolve to 2.0)
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
