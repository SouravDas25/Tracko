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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransferUpdateHappyPathIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsersRepository usersRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String token;
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
        user.setPhoneNo("6000000000");
        user.setEmail("u1@example.com");
        user.setPassword("pass");
        user = usersRepository.save(user);

        var principal = new org.springframework.security.core.userdetails.User(
                user.getPhoneNo(), user.getPassword(), Collections.emptyList());
        token = "Bearer " + jwtTokenUtil.generateToken(principal);

        a1 = new Account(); a1.setName("A1"); a1.setUserId(user.getId()); a1 = accountRepository.save(a1);
        a2 = new Account(); a2.setName("A2"); a2.setUserId(user.getId()); a2 = accountRepository.save(a2);
    }

    @Test
    public void updateTransfer_happyPath_updatesAmountCurrencyDateAndName() throws Exception {
        // Create transfer a1 -> a2 (10.0 INR)
        Map<String, Object> create = new HashMap<>();
        create.put("accountId", a1.getId());
        create.put("toAccountId", a2.getId());
        create.put("originalAmount", 10.0);
        create.put("originalCurrency", "INR");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        // Grab debit/credit IDs
        List<Transaction> txs = transactionRepository.findAll();
        Transaction debit = txs.stream().filter(t -> t.getTransactionType() == TransactionType.DEBIT).findFirst().orElseThrow();
        Long creditId = debit.getLinkedTransactionId();

        // Update fields
        Map<String, Object> update = new HashMap<>();
        update.put("originalAmount", 20.0);
        update.put("originalCurrency", "INR");
        update.put("exchangeRate", 1.0);
        update.put("name", "Updated Transfer");
        update.put("date", new Date());

        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("updated successfully")));

        // Verify both sides got updated
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();
        assertThat(updatedDebit.getOriginalAmount()).isEqualTo(20.0);
        assertThat(updatedCredit.getOriginalAmount()).isEqualTo(20.0);
        assertThat(updatedDebit.getName()).isEqualTo("Updated Transfer");
        assertThat(updatedCredit.getName()).isEqualTo("Updated Transfer");
        assertThat(updatedDebit.getDate()).isNotNull();
        assertThat(updatedCredit.getDate()).isNotNull();
    }
}
