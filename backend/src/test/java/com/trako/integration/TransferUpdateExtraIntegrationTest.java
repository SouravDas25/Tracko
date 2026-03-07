package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionEntryType;
import com.trako.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransferUpdateExtraIntegrationTest extends BaseIntegrationTest {

    private User userA;
    private String tokenA;
    private Account acc1;
    private Account acc2;
    private Account acc3;

    @BeforeEach
    public void setup() {
        userA = createUniqueUser("UserA");
        tokenA = generateBearerToken(userA);

        acc1 = new Account();
        acc1.setName("A1");
        acc1.setUserId(userA.getId());
        acc1 = accountRepository.save(acc1);
        acc2 = new Account();
        acc2.setName("A2");
        acc2.setUserId(userA.getId());
        acc2 = accountRepository.save(acc2);
        acc3 = new Account();
        acc3.setName("A3");
        acc3.setUserId(userA.getId());
        acc3 = accountRepository.save(acc3);
    }

    @Test
    public void testUpdateTransferChangeToAccount_Success() throws Exception {
        // Create initial transfer acc1 -> acc2
        Map<String, Object> create = new HashMap<>();
        create.put("accountId", acc1.getId());
        create.put("toAccountId", acc2.getId());
        create.put("originalAmount", 100.0);
        create.put("originalCurrency", "INR");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        // Fetch transactions and pick debit side
        List<Transaction> all = transactionRepository.findAll();
        Transaction debit = all.stream().filter(t -> t.getTransactionType() == TransactionEntryType.DEBIT).findFirst().orElseThrow();
        Long creditId = debit.getLinkedTransactionId();

        // Update toAccountId to acc3
        Map<String, Object> update = new HashMap<>();
        update.put("toAccountId", acc3.getId());
        update.put("originalAmount", 150.0);
        update.put("originalCurrency", "INR");

        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();
        assertThat(updatedDebit.getAmount()).isEqualTo(150.0);
        assertThat(updatedCredit.getAmount()).isEqualTo(150.0);
        assertThat(updatedDebit.getAccountId()).isEqualTo(acc1.getId());
        assertThat(updatedCredit.getAccountId()).isEqualTo(acc3.getId());
    }

    @Test
    public void testUpdateTransfer_Unauthorized() throws Exception {
        // Create another user and its transfer
        User userB = new User();
        userB.setName("UserB");
        userB.setPhoneNo("7000000002");
        userB.setEmail("b@example.com");
        userB.setPassword("pass");
        userB = usersRepository.save(userB);

        Account b1 = new Account();
        b1.setName("B1");
        b1.setUserId(userB.getId());
        b1 = accountRepository.save(b1);
        Account b2 = new Account();
        b2.setName("B2");
        b2.setUserId(userB.getId());
        b2 = accountRepository.save(b2);

        var principalB = new org.springframework.security.core.userdetails.User(
                userB.getPhoneNo(), userB.getPassword(), Collections.emptyList());
        String tokenB = "Bearer " + jwtTokenUtil.generateToken(principalB);

        Map<String, Object> create = new HashMap<>();
        create.put("accountId", b1.getId());
        create.put("toAccountId", b2.getId());
        create.put("originalAmount", 50.0);
        create.put("originalCurrency", "INR");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        // Get B's debit id
        List<Transaction> all = transactionRepository.findAll();
        Transaction bDebit = all.stream().filter(t -> t.getTransactionType() == TransactionEntryType.DEBIT).findFirst().orElseThrow();

        // Try to update B's transfer with A's token -> 401
        Map<String, Object> update = new HashMap<>();
        update.put("originalAmount", 60.0);
        update.put("originalCurrency", "INR");

        mockMvc.perform(put("/api/transactions/" + bDebit.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnauthorized());
    }
}
