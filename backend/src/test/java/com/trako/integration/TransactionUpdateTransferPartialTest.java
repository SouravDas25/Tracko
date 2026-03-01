package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
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
import java.util.List;
import java.util.Map;

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
public class TransactionUpdateTransferPartialTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsersRepository usersRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String bearerToken;
    private User user;
    private Account a1;
    private Account a2;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        user = new User();
        user.setName("U1");
        user.setPhoneNo("7777777777");
        user.setEmail("u1@example.com");
        user.setPassword("pass");
        user = usersRepository.save(user);

        var principal = new org.springframework.security.core.userdetails.User(
                user.getPhoneNo(), user.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        a1 = new Account(); a1.setName("A1"); a1.setUserId(user.getId()); a1 = accountRepository.save(a1);
        a2 = new Account(); a2.setName("A2"); a2.setUserId(user.getId()); a2 = accountRepository.save(a2);
    }

    private Transaction createTransfer(double originalAmount, String currency, Double exchangeRate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", a1.getId());
        payload.put("toAccountId", a2.getId());
        payload.put("originalAmount", originalAmount);
        payload.put("originalCurrency", currency);
        if (exchangeRate != null) payload.put("exchangeRate", exchangeRate);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        return transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst().orElseThrow();
    }

    @Test
    public void updateTransfer_exchangeRateOnly_updatesBothSides() throws Exception {
        Transaction debit = createTransfer(10.0, "INR", 1.0);
        Long debitId = debit.getId();
        Long creditId = debit.getLinkedTransactionId();

        Map<String, Object> update = new HashMap<>();
        update.put("exchangeRate", 2.0);
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

        Map<String, Object> update = new HashMap<>();
        update.put("fromAccountId", a1.getId());
        update.put("toAccountId", a1.getId()); // same -> should 400

        mockMvc.perform(put("/api/transactions/" + debitId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }
}
