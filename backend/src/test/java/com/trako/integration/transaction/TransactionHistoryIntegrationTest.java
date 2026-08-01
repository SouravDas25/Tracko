package com.trako.integration.transaction;

import com.trako.dtos.TransferResult;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Contact;
import com.trako.entities.Split;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.models.external.ExchangeRateApiResponse;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.ContactRepository;
import com.trako.repositories.SplitRepository;
import com.trako.services.ExchangeRateService;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.services.transactions.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TransactionHistoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private ContactRepository contactRepository;

    @MockBean
    private ExchangeRateService exchangeRateService;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser();
        bearerToken = generateBearerToken(testUser);

        testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);

        // The provider quotes target-per-base, so an INR-based response carries GBP-per-INR and
        // USD-per-INR. Intent is 1 GBP = 1.2 INR and 1 USD = 80 INR — the reciprocals of these.
        ExchangeRateApiResponse mockResponse =
                new ExchangeRateApiResponse("INR", Map.of("GBP", 1.0 / 1.2, "USD", 1.0 / 80.0));
        when(exchangeRateService.getRates(anyString())).thenReturn(mockResponse);
    }

    private Transaction persistTransaction(String name, double amount) {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setName(name);
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        return transactionWriteService.saveForUser(testUser.getId(), t);
    }

    private long firstHistoryId(String responseJson) throws Exception {
        var root = objectMapper.readTree(responseJson);
        var arr = root.path("result");
        assertTrue(arr.isArray() && arr.size() > 0, "expected at least one history entry");
        return arr.get(0).path("id").asLong();
    }

    @Test
    public void editThenRevert_restoresOldValues() throws Exception {
        Transaction saved = persistTransaction("Old Name", 10.00);

        // Edit via the API — records an UPDATE snapshot of the old values.
        TransactionRequest update = new TransactionRequest(
                null, testAccount.getId(), saved.getDate(), "New Name", "changed",
                testCategory.getId(), TransactionType.DEBIT, "INR", 99.00, 1.0, null, null, null);

        mockMvc.perform(put("/api/transactions/{id}", saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("New Name"))
                .andExpect(jsonPath("$.result.amount").value(99.00));

        // The timeline has the UPDATE entry.
        String historyJson = mockMvc.perform(get("/api/transactions/{id}/history", saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].operation").value("UPDATE"))
                .andReturn().getResponse().getContentAsString();

        long historyId = firstHistoryId(historyJson);

        // Revert rolls the edit back.
        mockMvc.perform(post("/api/transactions/history/{historyId}/revert", historyId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        Transaction reverted = transactionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Old Name", reverted.getName());
        assertEquals(10.00, reverted.getOriginalAmount(), 0.0001);
    }

    @Test
    public void deleteThenRestore_recreatesTransactionWithSameIdAndSplits() throws Exception {
        Transaction saved = persistTransaction("To Delete", 40.00);

        Contact contact = new Contact();
        contact.setUserId(testUser.getId());
        contact.setName("Alice");
        contact.setPhoneNo("9990001111");
        contact = contactRepository.save(contact);

        Split split = new Split();
        split.setTransactionId(saved.getId());
        split.setUserId(testUser.getId());
        split.setContactId(contact.getId());
        split.setAmount(15.00);
        split.setIsSettled(0);
        splitRepository.save(split);

        Long id = saved.getId();

        // Delete → moves to the recycle bin.
        mockMvc.perform(delete("/api/transactions/{id}", id)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions/{id}", id)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());

        // It shows up in the trash view.
        String trashJson = mockMvc.perform(get("/api/transactions/trash")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].operation").value("DELETE"))
                .andExpect(jsonPath("$.result[0].transactionId").value(id.intValue()))
                .andReturn().getResponse().getContentAsString();

        long historyId = firstHistoryId(trashJson);

        // Restore → transaction re-created with the same id, and its split restored.
        mockMvc.perform(post("/api/transactions/history/{historyId}/revert", historyId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        Transaction restored = transactionRepository.findById(id).orElseThrow();
        assertEquals("To Delete", restored.getName());
        assertEquals(40.00, restored.getOriginalAmount(), 0.0001);

        List<Split> restoredSplits = splitRepository.findByTransactionId(id);
        assertEquals(1, restoredSplits.size());
        assertEquals(15.00, restoredSplits.get(0).getAmount(), 0.0001);
        assertEquals(contact.getId(), restoredSplits.get(0).getContactId());
    }

    @Test
    public void deleteThenRestore_transfer_restoresBothLinkedSides() throws Exception {
        Account toAccount = new Account();
        toAccount.setName("Checking");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);

        TransferResult created = transferService.createTransfer(
                testUser.getId(), testAccount.getId(), toAccount.getId(),
                new Date(), 50.00, "INR", 1.0, "Xfer", "note");

        Long debitId = created.debit().getId();
        Long creditId = created.debit().getLinkedTransactionId();
        assertNotNull(creditId);

        // Deleting one side deletes the whole transfer.
        mockMvc.perform(delete("/api/transactions/{id}", debitId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        assertTrue(transactionRepository.findById(debitId).isEmpty());
        assertTrue(transactionRepository.findById(creditId).isEmpty());

        String trashJson = mockMvc.perform(get("/api/transactions/trash")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long historyId = firstHistoryId(trashJson);

        // Restore brings back both sides, still linked to each other.
        mockMvc.perform(post("/api/transactions/history/{historyId}/revert", historyId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        Transaction restoredDebit = transactionRepository.findById(debitId).orElseThrow();
        Transaction restoredCredit = transactionRepository.findById(creditId).orElseThrow();
        assertEquals(creditId, restoredDebit.getLinkedTransactionId());
        assertEquals(debitId, restoredCredit.getLinkedTransactionId());
    }

    /** Creates a regular DEBIT transaction through the API (so a CREATE entry is recorded) and returns its id. */
    private long createTransactionViaApi(String name, double amount) throws Exception {
        TransactionRequest req = new TransactionRequest(
                null, testAccount.getId(), new Date(), name, "note",
                testCategory.getId(), TransactionType.DEBIT, "INR", amount, 1.0, null, null, null);
        String json = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).path("result").path("id").asLong();
    }

    @Test
    public void listAllHistory_returnsEveryOperationAcrossAllTransactions() throws Exception {
        // Transaction A: created, then edited, then deleted → CREATE, UPDATE, DELETE.
        long aId = createTransactionViaApi("Alpha", 10.00);
        TransactionRequest editA = new TransactionRequest(
                null, testAccount.getId(), new Date(), "Alpha Edited", "note",
                testCategory.getId(), TransactionType.DEBIT, "INR", 12.00, 1.0, null, null, null);
        mockMvc.perform(put("/api/transactions/{id}", aId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editA)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/transactions/{id}", aId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        // Transaction B: just created → CREATE.
        long bId = createTransactionViaApi("Beta", 20.00);

        // The global history spans both transactions and every operation. Timestamps can tie at
        // millisecond resolution, so assert the multiset of operations/ids rather than strict order.
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(4))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.history.length()").value(4))
                .andExpect(jsonPath("$.result.history[*].operation", hasItems("CREATE", "UPDATE", "DELETE")))
                .andExpect(jsonPath("$.result.history[*].transactionId",
                        hasItems((int) aId, (int) bId)));
    }

    @Test
    public void listHistory_isPagedAndNewestFirst() throws Exception {
        // Three creates → three CREATE entries, oldest to newest by id.
        long firstId = createTransactionViaApi("One", 1.00);
        createTransactionViaApi("Two", 2.00);
        long lastId = createTransactionViaApi("Three", 3.00);

        // Page 0 (size 2): the two newest, most recent first (id desc breaks any timestamp tie).
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", bearerToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.hasPrevious").value(false))
                .andExpect(jsonPath("$.result.history.length()").value(2))
                .andExpect(jsonPath("$.result.history[0].transactionId").value((int) lastId));

        // Page 1: the remaining (oldest) entry.
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", bearerToken)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.hasPrevious").value(true))
                .andExpect(jsonPath("$.result.history.length()").value(1))
                .andExpect(jsonPath("$.result.history[0].transactionId").value((int) firstId));
    }

    @Test
    public void listHistory_filtersByOperation() throws Exception {
        long aId = createTransactionViaApi("Deleted one", 10.00);
        mockMvc.perform(delete("/api/transactions/{id}", aId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());
        createTransactionViaApi("Kept one", 20.00);

        // operation=DELETE is the recycle-bin view: only the delete of A.
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", bearerToken)
                        .param("operation", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.history[0].operation").value("DELETE"))
                .andExpect(jsonPath("$.result.history[0].transactionId").value((int) aId));

        // Two creates recorded (A and the kept one).
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", bearerToken)
                        .param("operation", "create")) // case-insensitive
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2));
    }

    @Test
    public void listHistory_rejectsUnknownOperation() throws Exception {
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", bearerToken)
                        .param("operation", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transactionHistory_showsFieldDiffIncludingType() throws Exception {
        long id = createTransactionViaApi("Diffy", 10.00); // DEBIT, 10 INR

        // One edit that changes the type (DEBIT → CREDIT) and the amount (10 → 25).
        TransactionRequest edit = new TransactionRequest(
                null, testAccount.getId(), new Date(), "Diffy", "note",
                testCategory.getId(), TransactionType.CREDIT, "INR", 25.00, 1.0, null, null, null);
        mockMvc.perform(put("/api/transactions/{id}", id)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isOk());

        // The newest timeline entry is the UPDATE, and it carries a before→after diff of what changed.
        mockMvc.perform(get("/api/transactions/{id}/history", id)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].operation").value("UPDATE"))
                .andExpect(jsonPath("$.result[0].changes[?(@.field == 'Type')].before").value(hasItem("DEBIT")))
                .andExpect(jsonPath("$.result[0].changes[?(@.field == 'Type')].after").value(hasItem("CREDIT")))
                .andExpect(jsonPath("$.result[0].changes[?(@.field == 'Amount')].before").value(hasItem("10")))
                .andExpect(jsonPath("$.result[0].changes[?(@.field == 'Amount')].after").value(hasItem("25")))
                // The CREATE entry underneath is not an edit, so it has no diff rows.
                .andExpect(jsonPath("$.result[1].operation").value("CREATE"))
                .andExpect(jsonPath("$.result[1].changes[*]").isEmpty());
    }

    @Test
    public void listAllHistory_isScopedToTheOwner() throws Exception {
        // Owner accumulates history...
        createTransactionViaApi("Mine", 5.00);

        // ...but another user's global history stays empty.
        User other = createUniqueUser("Stranger");
        String otherToken = generateBearerToken(other);

        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0))
                .andExpect(jsonPath("$.result.history.length()").value(0));
    }

    @Test
    public void revert_otherUsersHistory_isNotFound() throws Exception {
        Transaction saved = persistTransaction("Mine", 5.00);
        mockMvc.perform(delete("/api/transactions/{id}", saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        String trashJson = mockMvc.perform(get("/api/transactions/trash")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long historyId = firstHistoryId(trashJson);

        User other = createUniqueUser("Intruder");
        String otherToken = generateBearerToken(other);

        mockMvc.perform(post("/api/transactions/history/{historyId}/revert", historyId)
                        .header("Authorization", otherToken))
                .andExpect(status().isNotFound());
    }
}
