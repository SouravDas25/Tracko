package com.trako.integration.transaction;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.transactions.TransactionWriteService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionUpdateRegularEdgeCasesTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    private String tokenA;
    private User userA;
    private Account accA1;
    private Account accA2;
    private Category catA1;

    @BeforeEach
    public void setup() {
        userA = createUniqueUser("UserA");
        tokenA = generateBearerToken(userA);

        accA1 = new Account();
        accA1.setName("A1");
        accA1.setUserId(userA.getId());
        accA1 = accountRepository.save(accA1);
        accA2 = new Account();
        accA2.setName("A2");
        accA2.setUserId(userA.getId());
        accA2 = accountRepository.save(accA2);

        catA1 = new Category();
        catA1.setName("Food");
        catA1.setUserId(userA.getId());
        catA1 = categoryRepository.save(catA1);
    }

    private Transaction createRegular() {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setName("Orig");
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(accA1.getId());
        t.setCategoryId(catA1.getId());
        t.setIsCountable(1);
        return transactionRepository.save(t);
    }

    @Test
    public void updateRegular_invalidCategory_returnsBadRequest() throws Exception {
        Transaction t = createRegular();
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                null,                    // accountId
                null,                    // date
                null,                    // name
                null,                    // comments
                999999L,                 // categoryId (non-existent)
                TransactionType.DEBIT,   // transactionType
                null,                    // isCountable
                null,                    // originalCurrency
                null,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateRegular_unauthorizedAccount_returnsUnauthorized() throws Exception {
        Transaction t = createRegular();

        // Create other user and account
        User userB = createUniqueUser("UserB");
        Account accB = new Account();
        accB.setName("B1");
        accB.setUserId(userB.getId());
        accB = accountRepository.save(accB);

        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                accB.getId(),             // accountId (unauthorized)
                null,                    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.DEBIT,   // transactionType
                null,                    // isCountable
                null,                    // originalCurrency
                null,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );
        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateRegular_partialFields_success() throws Exception {
        Transaction t = createRegular();
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                accA2.getId(),            // accountId
                new Date(),              // date
                "NewName",               // name
                "Cmt",                   // comments
                catA1.getId(),            // categoryId
                TransactionType.DEBIT,   // transactionType
                0,                       // isCountable
                null,                    // originalCurrency
                null,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        Transaction updated = transactionRepository.findById(t.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("NewName");
        assertThat(updated.getComments()).isEqualTo("Cmt");
        assertThat(updated.getIsCountable()).isEqualTo(0);
        assertThat(updated.getAccountId()).isEqualTo(accA2.getId());
    }
}
