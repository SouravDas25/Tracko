package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.models.request.TransferRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        // Create user and auth token
        testUser = new User();
        testUser.setName("Transfer User");
        testUser.setPhoneNo("9990001111");
        testUser.setEmail("transfer@example.com");
        testUser.setFireBaseId("xfer_pass");
        testUser = usersRepository.save(testUser);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        // Two accounts owned by the same user
        fromAccount = new Account();
        fromAccount.setName("Wallet");
        fromAccount.setUserId(testUser.getId());
        fromAccount = accountRepository.save(fromAccount);

        toAccount = new Account();
        toAccount.setName("Bank");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);
    }

    @Test
    public void shouldCreateTransferBetweenOwnedAccounts() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccount.getId());
        req.setToAccountId(toAccount.getId());
        req.setAmount(100.0);
        req.setComments("Moving funds");

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer created successfully"));

        // Verify two transactions created with proper attributes
        List<Transaction> fromTxns = transactionRepository.findByAccountId(fromAccount.getId());
        List<Transaction> toTxns = transactionRepository.findByAccountId(toAccount.getId());

        // One debit on source, one credit on destination
        org.junit.jupiter.api.Assertions.assertEquals(1, fromTxns.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, toTxns.size());

        Transaction debit = fromTxns.get(0);
        Transaction credit = toTxns.get(0);

        org.junit.jupiter.api.Assertions.assertEquals(1, debit.getTransactionType());
        org.junit.jupiter.api.Assertions.assertEquals(2, credit.getTransactionType());
        org.junit.jupiter.api.Assertions.assertEquals(100.0, debit.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(100.0, credit.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(0, debit.getIsCountable());
        org.junit.jupiter.api.Assertions.assertEquals(0, credit.getIsCountable());
        org.junit.jupiter.api.Assertions.assertNotNull(debit.getDate());
        org.junit.jupiter.api.Assertions.assertNotNull(credit.getDate());

        // TRANSFER category should exist and be set on both
        List<Category> cats = categoryRepository.findByUserIdAndName(testUser.getId(), "TRANSFER");
        org.junit.jupiter.api.Assertions.assertEquals(1, cats.size());
        Long transferCatId = cats.get(0).getId();
        org.junit.jupiter.api.Assertions.assertEquals(transferCatId, debit.getCategoryId());
        org.junit.jupiter.api.Assertions.assertEquals(transferCatId, credit.getCategoryId());
    }

    @Test
    public void shouldReuseExistingTransferCategory() throws Exception {
        // Pre-create TRANSFER category
        Category transfer = new Category();
        transfer.setName("TRANSFER");
        transfer.setUserId(testUser.getId());
        transfer = categoryRepository.save(transfer);

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccount.getId());
        req.setToAccountId(toAccount.getId());
        req.setAmount(50.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Ensure no duplicate category created
        List<Category> cats = categoryRepository.findByUserIdAndName(testUser.getId(), "TRANSFER");
        org.junit.jupiter.api.Assertions.assertEquals(1, cats.size());

        // Both transactions should use the existing category id
        List<Transaction> allFrom = transactionRepository.findByAccountId(fromAccount.getId());
        List<Transaction> allTo = transactionRepository.findByAccountId(toAccount.getId());
        org.junit.jupiter.api.Assertions.assertEquals(transfer.getId(), allFrom.get(0).getCategoryId());
        org.junit.jupiter.api.Assertions.assertEquals(transfer.getId(), allTo.get(0).getCategoryId());
    }

    @Test
    public void shouldRejectSameAccountTransfer() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccount.getId());
        req.setToAccountId(fromAccount.getId());
        req.setAmount(10.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromAccountId and toAccountId cannot be same"));
    }

    @Test
    public void shouldValidateBeanConstraints() throws Exception {
        // amount < 1 should be rejected by @Min(1)
        TransferRequest invalid = new TransferRequest();
        invalid.setFromAccountId(fromAccount.getId());
        invalid.setToAccountId(toAccount.getId());
        invalid.setAmount(0.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        // missing fromAccountId
        TransferRequest missingFrom = new TransferRequest();
        missingFrom.setToAccountId(toAccount.getId());
        missingFrom.setAmount(5.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingFrom)))
                .andExpect(status().isBadRequest());

        // missing toAccountId
        TransferRequest missingTo = new TransferRequest();
        missingTo.setFromAccountId(fromAccount.getId());
        missingTo.setAmount(5.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingTo)))
                .andExpect(status().isBadRequest());

        // negative amount
        TransferRequest negativeAmount = new TransferRequest();
        negativeAmount.setFromAccountId(fromAccount.getId());
        negativeAmount.setToAccountId(toAccount.getId());
        negativeAmount.setAmount(-10.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeAmount)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectWhenAccountNotOwned() throws Exception {
        // Another user's account
        User other = new User();
        other.setName("Other");
        other.setPhoneNo("1112223333");
        other.setEmail("other@example.com");
        other.setFireBaseId("other_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("Foreign");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccount.getId());
        req.setToAccountId(foreign.getId());
        req.setAmount(20.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));

        // Ensure no transactions were created on either account
        org.junit.jupiter.api.Assertions.assertTrue(transactionRepository.findByAccountId(fromAccount.getId()).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(transactionRepository.findByAccountId(foreign.getId()).isEmpty());
    }

    @Test
    public void shouldRejectWithoutAuth() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccount.getId());
        req.setToAccountId(toAccount.getId());
        req.setAmount(10.0);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldRejectWhenOnlyFromOwned() throws Exception {
        // Create another user's destination account
        User other = new User();
        other.setName("Other2");
        other.setPhoneNo("2223334444");
        other.setEmail("other2@example.com");
        other.setFireBaseId("other2_pass");
        other = usersRepository.save(other);

        Account othersDest = new Account();
        othersDest.setName("Other To");
        othersDest.setCurrency("USD");
        othersDest.setUserId(other.getId());
        othersDest = accountRepository.save(othersDest);

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccount.getId()); // owned
        req.setToAccountId(othersDest.getId());    // not owned
        req.setAmount(10.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldRejectWhenOnlyToOwned() throws Exception {
        // Create another user's source account
        User other = new User();
        other.setName("Other3");
        other.setPhoneNo("3334445555");
        other.setEmail("other3@example.com");
        other.setFireBaseId("other3_pass");
        other = usersRepository.save(other);

        Account othersSource = new Account();
        othersSource.setName("Other From");
        othersSource.setCurrency("USD");
        othersSource.setUserId(other.getId());
        othersSource = accountRepository.save(othersSource);

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(othersSource.getId()); // not owned
        req.setToAccountId(toAccount.getId());       // owned
        req.setAmount(15.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldReturnUnauthorizedForNonexistentAccountIds() throws Exception {
        // Controller checks ownership by user accounts list; nonexistent IDs are not owned -> 401
        long fakeFromId = 999999L;
        long fakeToId = 888888L;

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fakeFromId);
        req.setToAccountId(fakeToId);
        req.setAmount(20.0);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
