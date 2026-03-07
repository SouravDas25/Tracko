package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionUpdateIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    private User testUser;
    private String bearerToken;
    private Account account1;
    private Account account2;
    private Category category1;
    private Category category2;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("U1");
        bearerToken = generateBearerToken(testUser);

        account1 = new Account();
        account1.setName("A1");
        account1.setUserId(testUser.getId());
        account1 = accountRepository.save(account1);

        account2 = new Account();
        account2.setName("A2");
        account2.setUserId(testUser.getId());
        account2 = accountRepository.save(account2);

        category1 = new Category();
        category1.setName("Food");
        category1.setUserId(testUser.getId());
        category1 = categoryRepository.save(category1);

        category2 = new Category();
        category2.setName("Travel");
        category2.setUserId(testUser.getId());
        category2 = categoryRepository.save(category2);
    }

    @Test
    public void updateRegularTransactionFields_Success() throws Exception {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionType.DEBIT);
        t.setName("Old");
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(account1.getId());
        t.setCategoryId(category1.getId());
        t.setIsCountable(1);
        t = transactionWriteService.saveForUser(testUser.getId(), t);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "NewName");
        payload.put("comments", "Updated");
        payload.put("date", new Date());
        payload.put("accountId", account2.getId());
        payload.put("categoryId", category2.getId());
        payload.put("isCountable", 0);
        payload.put("originalCurrency", "INR");
        payload.put("exchangeRate", 1.0);

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        Transaction updated = transactionRepository.findById(t.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("NewName");
        assertThat(updated.getComments()).isEqualTo("Updated");
        assertThat(updated.getAccountId()).isEqualTo(account2.getId());
        assertThat(updated.getCategoryId()).isEqualTo(category2.getId());
        assertThat(updated.getIsCountable()).isEqualTo(0);
    }

    @Test
    public void updateRegularTransaction_Unauthorized() throws Exception {
        // Create transaction for another user
        User other = createUniqueUser("Other");

        Account otherAcc = new Account();
        otherAcc.setName("OtherAcc");
        otherAcc.setUserId(other.getId());
        otherAcc = accountRepository.save(otherAcc);

        Category otherCat = new Category();
        otherCat.setName("OtherCat");
        otherCat.setUserId(other.getId());
        otherCat = categoryRepository.save(otherCat);

        Transaction otherTx = new Transaction();
        otherTx.setTransactionType(TransactionType.DEBIT);
        otherTx.setName("OtherTx");
        otherTx.setOriginalAmount(5.0);
        otherTx.setOriginalCurrency("INR");
        otherTx.setExchangeRate(1.0);
        otherTx.setDate(new Date());
        otherTx.setAccountId(otherAcc.getId());
        otherTx.setCategoryId(otherCat.getId());
        otherTx.setIsCountable(1);
        otherTx = transactionRepository.save(otherTx);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Hack");

        mockMvc.perform(put("/api/transactions/" + otherTx.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }
}
