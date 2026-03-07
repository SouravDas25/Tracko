package com.trako.integration.transaction;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionUpdateTransferPartialTest extends BaseIntegrationTest {

    private String bearerToken;
    private User user;
    private Account a1;
    private Account a2;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("U1");
        bearerToken = generateBearerToken(user);

        a1 = new Account();
        a1.setName("A1");
        a1.setUserId(user.getId());
        a1 = accountRepository.save(a1);
        a2 = new Account();
        a2.setName("A2");
        a2.setUserId(user.getId());
        a2 = accountRepository.save(a2);
    }

    private Transaction createTransfer(double originalAmount, String currency, Double exchangeRate) throws Exception {
        TransactionRequest request = new TransactionRequest(
                null,                    // id
                a1.getId(),              // accountId (source account)
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId (TRANSFER category auto-handled)
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                currency,                // originalCurrency
                originalAmount,          // originalAmount
                exchangeRate,            // exchangeRate (maybe null)
                null,                    // linkedTransactionId
                a2.getId(),              // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        return transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
                .findFirst().orElseThrow();
    }

    @Test
    public void updateTransfer_exchangeRateOnly_updatesBothSides() throws Exception {
        Transaction debit = createTransfer(10.0, "INR", 1.0);
        Long debitId = debit.getId();
        Long creditId = debit.getLinkedTransactionId();

        TransactionRequest update = new TransactionRequest(
                null,                    // id
                null,                    // accountId
                null,                    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                null,                    // originalCurrency
                null,                    // originalAmount
                2.0,                     // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        // Keep currency same (not provided), amount unchanged, only rate should change

        mockMvc.perform(put("/api/transactions/" + debitId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("updated successfully")));

        Transaction updatedDebit = transactionRepository.findById(debitId).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();
        assertThat(updatedDebit.getExchangeRate()).isEqualTo(2.0);
        assertThat(updatedCredit.getExchangeRate()).isEqualTo(2.0);
    }

    @Test
    public void updateTransfer_sameAccounts_rejected() throws Exception {
        Transaction debit = createTransfer(10.0, "INR", 1.0);
        Long debitId = debit.getId();

        TransactionRequest update = new TransactionRequest(
                null,                    // id
                null,                    // accountId
                null,                    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // isCountable
                null,                    // originalCurrency
                null,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                a1.getId(),              // toAccountId
                a1.getId()               // fromAccountId (same as to -> should 400)
        );

        mockMvc.perform(put("/api/transactions/" + debitId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }
}
