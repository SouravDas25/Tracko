package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class SplitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User testUser;
    private User otherUser;
    private Transaction testTransaction;
    private String bearerToken;
    private String otherBearerToken;
    private Contact testContact;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);

        otherUser = createUniqueUser("Other User");
        otherBearerToken = generateBearerToken(otherUser);

        Account testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        Category testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);

        testTransaction = new Transaction();
        testTransaction.setTransactionType(TransactionEntryType.DEBIT);
        testTransaction.setName("Dinner");
        testTransaction.setOriginalAmount(100.00);
        testTransaction.setOriginalCurrency("INR");
        testTransaction.setExchangeRate(1.0);
        testTransaction.setDate(new Date());
        testTransaction.setAccountId(testAccount.getId());
        testTransaction.setCategoryId(testCategory.getId());
        testTransaction = transactionWriteService.saveForUser(testUser.getId(), testTransaction);

        testContact = new Contact();
        testContact.setUserId(testUser.getId());
        testContact.setName("C1");
        testContact.setPhoneNo(generateUniquePhone());
        testContact = contactRepository.save(testContact);
    }

    @Test
    public void testCreateSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);

        mockMvc.perform(post("/api/splits")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(split)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(50.00))
                .andExpect(jsonPath("$.result.isSettled").value(0));
    }

    @Test
    public void testCreateSplit_missingTransactionId_returnsBadRequest() throws Exception {
        Split split = new Split();
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);

        mockMvc.perform(post("/api/splits")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(split)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateSplit_zeroAmount_returnsBadRequest() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(0.0);

        mockMvc.perform(post("/api/splits")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(split)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateSplit_negativeAmount_returnsBadRequest() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(-10.0);

        mockMvc.perform(post("/api/splits")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(split)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetAllSplits() throws Exception {
        Split split1 = new Split();
        split1.setTransactionId(testTransaction.getId());
        split1.setUserId(testUser.getId());
        split1.setAmount(50.00);
        splitRepository.save(split1);

        Split split2 = new Split();
        split2.setTransactionId(testTransaction.getId());
        split2.setUserId(testUser.getId());
        split2.setAmount(50.00);
        splitRepository.save(split2);

        mockMvc.perform(get("/api/splits")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)));
    }

    @Test
    public void testGetSplitById() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(75.00);
        Split saved = splitRepository.save(split);

        mockMvc.perform(get("/api/splits/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(75.00));
    }

    @Test
    public void testGetSplitsByTransactionId() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(100.00);
        splitRepository.save(split);

        mockMvc.perform(get("/api/splits/transaction/" + testTransaction.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].transactionId").value(testTransaction.getId().intValue()));
    }

    @Test
    public void testGetSplitsByUserId() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        splitRepository.save(split);

        mockMvc.perform(get("/api/splits")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].userId").value(testUser.getId()));
    }

    @Test
    public void testGetUnsettledSplitsByUserId() throws Exception {
        Split split1 = new Split();
        split1.setTransactionId(testTransaction.getId());
        split1.setUserId(testUser.getId());
        split1.setAmount(50.00);
        split1.setIsSettled(0);
        splitRepository.save(split1);

        Split split2 = new Split();
        split2.setTransactionId(testTransaction.getId());
        split2.setUserId(testUser.getId());
        split2.setAmount(50.00);
        split2.setIsSettled(1);
        splitRepository.save(split2);

        mockMvc.perform(get("/api/splits/unsettled")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].isSettled").value(0));
    }

    @Test
    public void testGetSplitById_whenOwnedByAnotherUser_returnsUnauthorized() throws Exception {
        Split foreign = new Split();
        foreign.setTransactionId(testTransaction.getId());
        foreign.setUserId(testUser.getId());
        foreign.setAmount(50.00);
        foreign = splitRepository.save(foreign);

        mockMvc.perform(get("/api/splits/" + foreign.getId())
                        .header("Authorization", otherBearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetSplitsByContactId() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setContactId(testContact.getId());
        split.setAmount(25.00);
        split.setIsSettled(0);
        splitRepository.save(split);

        mockMvc.perform(get("/api/splits/contact/" + testContact.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].contactId").value(testContact.getId().intValue()));
    }

    @Test
    public void testGetSplitsByContactId_whenContactOwnedByAnotherUser_returnsUnauthorized() throws Exception {
        Contact otherContact = new Contact();
        otherContact.setUserId(otherUser.getId());
        otherContact.setName("OtherContact");
        otherContact = contactRepository.save(otherContact);

        mockMvc.perform(get("/api/splits/contact/" + otherContact.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetUnsettledSplitsByContactId_filtersSettled() throws Exception {
        Split unsettled = new Split();
        unsettled.setTransactionId(testTransaction.getId());
        unsettled.setUserId(testUser.getId());
        unsettled.setContactId(testContact.getId());
        unsettled.setAmount(10.00);
        unsettled.setIsSettled(0);
        splitRepository.save(unsettled);

        Split settled = new Split();
        settled.setTransactionId(testTransaction.getId());
        settled.setUserId(testUser.getId());
        settled.setContactId(testContact.getId());
        settled.setAmount(10.00);
        settled.setIsSettled(1);
        splitRepository.save(settled);

        mockMvc.perform(get("/api/splits/contact/" + testContact.getId() + "/unsettled")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].isSettled").value(0));
    }

    @Test
    public void testSettleSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);
        Split saved = splitRepository.save(split);

        mockMvc.perform(patch("/api/splits/settle/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Split settled = splitRepository.findById(saved.getId()).orElse(null);
        assertThat(settled).isNotNull();
        assertThat(settled.getIsSettled()).isEqualTo(1);
        assertThat(settled.getSettledAt()).isNotNull();
    }

    @Test
    public void testUnsettleSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(1);
        split.setSettledAt(new Date());
        Split saved = splitRepository.save(split);

        mockMvc.perform(patch("/api/splits/unsettle/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Split unsettled = splitRepository.findById(saved.getId()).orElse(null);
        assertThat(unsettled).isNotNull();
        assertThat(unsettled.getIsSettled()).isEqualTo(0);
        assertThat(unsettled.getSettledAt()).isNull();
    }

    @Test
    public void testDeleteSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        Split saved = splitRepository.save(split);

        mockMvc.perform(delete("/api/splits/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/splits/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies the split settlement workflow.
     *
     * <p>When a split is settled (e.g., a contact pays back their share), the system updates the
     * split status to 'settled'. However, it does NOT automatically create a corresponding
     * transaction record. This is because the repayment details (source, destination account, etc.)
     * are unknown.
     *
     * <p>This test confirms:
     * 1. Settling a split updates its status but does not change the transaction count.
     * 2. The user can manually create a transaction to record the repayment.
     */
    @Test
    public void testSettleSplit_doesNotCreateTransaction_and_allowsManualTransactionCreation() throws Exception {
        // 1. Create a split
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setContactId(testContact.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);
        Split savedSplit = splitRepository.save(split);

        long initialTransactionCount = transactionRepository.count();

        // 2. Settle the split
        // This simulates the user marking the split as "paid" in the UI.
        mockMvc.perform(patch("/api/splits/settle/" + savedSplit.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // 3. Verify split is settled
        Split settledSplit = splitRepository.findById(savedSplit.getId()).orElse(null);
        assertThat(settledSplit).isNotNull();
        assertThat(settledSplit.getIsSettled()).isEqualTo(1);

        // 4. Verify NO new transaction was automatically created
        // The system should not guess where the money went.
        long transactionCountAfterSettle = transactionRepository.count();
        assertThat(transactionCountAfterSettle).isEqualTo(initialTransactionCount);

        // 5. Manually create a transaction for the settlement (Income)
        // This simulates the user explicitly recording the repayment transaction.
        Transaction settlementTransaction = new Transaction();
        settlementTransaction.setTransactionType(TransactionEntryType.CREDIT); // Credit/Income
        settlementTransaction.setName("Settlement from " + testContact.getName());
        settlementTransaction.setOriginalAmount(savedSplit.getAmount());
        settlementTransaction.setOriginalCurrency("INR");
        settlementTransaction.setExchangeRate(1.0);
        settlementTransaction.setDate(new Date());
        settlementTransaction.setAccountId(testTransaction.getAccountId());
        settlementTransaction.setCategoryId(testTransaction.getCategoryId());

        Transaction manualTx = transactionWriteService.saveForUser(testUser.getId(), settlementTransaction);

        // 6. Verify the new transaction exists
        assertThat(manualTx.getId()).isNotNull();
        assertThat(transactionRepository.count()).isEqualTo(initialTransactionCount + 1);
    }
}
